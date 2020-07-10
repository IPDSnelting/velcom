package de.aaaaaaah.velcom.backend.access.policy;

import static de.aaaaaaah.velcom.backend.access.TaskReadAccess.taskFromRecord;
import static org.jooq.codegen.db.Tables.TASK;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.Cursor;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.TaskRecord;

/**
 * A thread safe {@link QueuePolicy} that prioritizes tars and provides some kind of fairness to
 * repo associated tasks by ordering them in a round robin type of way.
 *
 * <p>The ordering is based on three key decisions:</p>
 * <p>
 * 1.) Tasks with a higher than default priority are prioritized the most. Between tasks that have
 * the same priority (but still higher than default), the task that got promoted to this priority
 * most recently is preferred.
 * </p>
 * <p>
 * 2.) Tasks with default priorities which are associated with tars are prioritized second most.
 * Between tar tasks with default priority, the task that was inserted most recently is preferred.
 * </p>
 * <p>
 * 3.) Tasks with default priorities which are associated with repositories are prioritized the
 * least. This is where the round robin policy comes into play. Tasks on this level of importance
 * are ordered in way that repositories take turns when iterating over the tasks from front to back
 * such that a representative of each repository is iterated over until another task is found with
 * the same repository of a previous task. The ordering of the repositories is lexicographical.
 * </p>
 */
public class RoundRobinFiloPolicy implements QueuePolicy {

	private DatabaseStorage databaseStorage;
	private RepoId lastRepo;

	public RoundRobinFiloPolicy(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	@Override
	public synchronized Optional<Task> fetchNextTask() {
		AtomicReference<Task> result = new AtomicReference<>(null);

		databaseStorage.acquireTransaction(db -> {
			// 1.) Find task with highest priority
			Optional<TaskRecord> mostImportantRecord = db.selectFrom(TASK)
				.where(TASK.PRIORITY.greaterThan(Task.DEFAULT_PRIORITY))
				.orderBy(
					TASK.PRIORITY.desc(), TASK.UPDATE_TIME.desc()
				)
				.limit(1)
				.fetchOptional();

			if (mostImportantRecord.isPresent()) {
				result.set(taskFromRecord(mostImportantRecord.get()));
				return;
			}

			// 2.) Check if there is a tar
			Optional<TaskRecord> tarRecord = db.selectFrom(TASK)
				.where(TASK.TAR_NAME.isNotNull())
				.orderBy(TASK.INSERT_TIME.desc())
				.limit(1)
				.fetchOptional();

			if (tarRecord.isPresent()) {
				result.set(taskFromRecord(tarRecord.get()));
				return;
			}

			// 3.) Get newest task that is associated with the next repo id
			if (lastRepo == null) {
				// there was no last repo => just take the first repo available
				Optional<TaskRecord> repoTaskRecord = db.selectFrom(TASK)
					.orderBy(TASK.INSERT_TIME.desc())
					.limit(1)
					.fetchOptional();

				if (repoTaskRecord.isPresent()) {
					Task task = taskFromRecord(repoTaskRecord.get());

					lastRepo = task.getSource().getLeft().orElseThrow().getRepoId();

					result.set(task);
					return;
				} else {
					// there are no repo tasks, no tar tasks and no higher priority tasks
					// => queue is completely empty
					return;
				}
			} else {
				// lastRepo is not null => we need to find out what the next repo is
				RepoId nextRepoId = null;

				try (Cursor<Record1<String>> cursor = db.selectDistinct(TASK.REPO_ID)
					.from(TASK)
					.orderBy(TASK.REPO_ID.asc())
					.fetchLazy()) {

					while (cursor.hasNext()) {
						Record1<String> record = cursor.fetchNext();
						RepoId currentRepoId = new RepoId(UUID.fromString(record.value1()));

						// Just in case there is no repo id that comes after lastRepo
						if (nextRepoId == null) {
							nextRepoId = currentRepoId;
						}

						if (currentRepoId.compareTo(lastRepo) > 0) {
							// currentRepoId comes after lastRepo and since the select was ordered
							// all other repos come after this one
							// => currentRepoId is next one
							nextRepoId = currentRepoId;
							break;
						}
					}
				}

				if (nextRepoId == null) {
					return; // there are no repos in queue => queue is empty
				}

				// Get newest task for next repo id
				TaskRecord taskRecord = db.selectFrom(TASK)
					.where(TASK.REPO_ID.eq(nextRepoId.getId().toString()))
					.orderBy(TASK.INSERT_TIME.desc())
					.limit(1)
					.fetchOne();

				// Mark task as "in process"
				db.update(TASK)
					.set(TASK.IN_PROCESS, true)
					.where(TASK.ID.eq(taskRecord.getId()))
					.execute();

				Task task = taskFromRecord(taskRecord);
				lastRepo = nextRepoId;
				result.set(task);
				return;
			}
		});

		return Optional.ofNullable(result.get());
	}

	@Override
	public List<Task> getTasksSorted() {
		// The final ordered list that represents the queue
		LinkedList<Task> completeTaskList = new LinkedList<>();

		databaseStorage.acquireTransaction(db -> {
			// 1.) Get manual tasks
			db.selectFrom(TASK)
				.where(TASK.PRIORITY.greaterThan(Task.DEFAULT_PRIORITY))
				.orderBy(TASK.PRIORITY.desc(), TASK.UPDATE_TIME.desc())
				.forEach(record -> completeTaskList.addLast(taskFromRecord(record)));

			// 2.) Get tar tasks
			db.selectFrom(TASK)
				.where(TASK.TAR_NAME.isNotNull())
				.orderBy(TASK.INSERT_TIME.desc())
				.forEach(record -> completeTaskList.addLast(taskFromRecord(record)));

			// 3.) Get default repo tasks that have to be ordered in a round robin way
			Map<RepoId, LinkedList<Task>> groupMap = new HashMap<>();
			List<RepoId> repoIds = new ArrayList<>();

			try (Cursor<TaskRecord> cursor = db.selectFrom(TASK)
				.where(TASK.REPO_ID.isNotNull())
				.orderBy(TASK.INSERT_TIME.desc())
				.fetchLazy()) {

				while (cursor.hasNext()) {
					Task task = taskFromRecord(cursor.fetchNext());
					RepoId repoId = task.getSource().getLeft().orElseThrow().getRepoId();

					if (!groupMap.containsKey(repoId)) {
						groupMap.put(repoId, new LinkedList<>());
						repoIds.add(repoId);
					}

					groupMap.get(repoId).addLast(task);
				}
			}

			if (groupMap.isEmpty()) {
				return; // we are done here
			}

			if (lastRepo != null && !repoIds.contains(lastRepo)) {
				// if lastRepo is not already in the repoIds list, then there
				// are no tasks associated with lastRepo currently in the queue
				// however we still need to begin the repo that comes right after
				// lastRepo (alphabetically) => add lastRepo to repoIds
				repoIds.add(lastRepo);
			}

			repoIds.sort(Comparator.comparing(repoId -> repoId.getId().toString()));

			int start = lastRepo == null ? 0 : (repoIds.indexOf(lastRepo) + 1) % repoIds.size();

			for (int i = start; !groupMap.isEmpty(); i = (i + 1) % repoIds.size()) {
				RepoId currentRepoId = repoIds.get(i);
				LinkedList<Task> tasks = groupMap.get(currentRepoId);

				completeTaskList.addLast(tasks.pollFirst());

				if (tasks.isEmpty()) {
					groupMap.remove(currentRepoId);
				}
			}
		});

		return completeTaskList;
	}

}
