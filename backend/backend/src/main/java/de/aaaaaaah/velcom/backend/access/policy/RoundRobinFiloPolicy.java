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
 * repo associated tasks by ordering them in a round robin type of way while also allowing tasks to
 * be manually prioritized over all other tasks.
 *
 * <p>This queue policy recognizes three different priority levels:</p>
 * <p>
 * Priority 0: Tasks that are prioritized at priority 0 are considered manual tasks and are regarded
 * as the most important tasks. Comparing two tasks with priority 0 results in the task that got
 * updated most recently being prioritized more.
 * </p>
 * <p>
 * Priority 1: Tasks that are prioritized at priority 1 are tasks that are associated with a tar
 * file. Comparing two tasks with priority 1 results in the task that got updated most recently
 * being prioritized more.
 * </p>
 * <p>
 * Priority 2: Only tasks associated with a repository are allowed to have priority 2. All tasks
 * under this priority are subject to the round robin ordering so that repositories take turns when
 * iterating over the tasks from front to back such that one representative of each repository is
 * iterated over, until another task is found with the same repository of a previous task. The
 * ordering of the repositories is lexicographical.
 * </p>
 */
public class RoundRobinFiloPolicy implements QueuePolicy {

	/**
	 * The priority at and below which tasks will be subject to the round robin ordering (below as in
	 * less important, not a lower priority value).
	 */
	private static final QueuePriority ROUND_ROBIN_PRIORITY = QueuePriority.LISTENER;

	private DatabaseStorage databaseStorage;
	private RepoId lastRepo;

	public RoundRobinFiloPolicy(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	@Override
	public synchronized Optional<Task> fetchNextTask() {
		AtomicReference<Task> result = new AtomicReference<>(null);

		databaseStorage.acquireWriteTransaction(db -> {
			// 1.) Find manual or tar task with highest priority
			Optional<TaskRecord> mostImportantRecord = db.selectFrom(TASK)
				.where(
					TASK.PRIORITY.lessThan(ROUND_ROBIN_PRIORITY.asInt())
						.and(TASK.IN_PROCESS.eq(false))
				)
				.orderBy(
					TASK.PRIORITY.asc(), TASK.UPDATE_TIME.desc()
				)
				.limit(1)
				.fetchOptional();

			if (mostImportantRecord.isPresent()) {
				// Mark task as "in process"
				db.update(TASK)
					.set(TASK.IN_PROCESS, true)
					.where(TASK.ID.eq(mostImportantRecord.get().getId()))
					.execute();

				result.set(taskFromRecord(mostImportantRecord.get()));
				return;
			}

			// 2.) Get next task that has the default priority (round robin happens here)
			if (lastRepo == null) {
				// there was no last repo => just take the first repo available
				Optional<TaskRecord> repoTaskRecord = db.selectFrom(TASK)
					.where(TASK.IN_PROCESS.eq(false))
					.orderBy(TASK.INSERT_TIME.desc())
					.limit(1)
					.fetchOptional();

				if (repoTaskRecord.isPresent()) {
					Task task = taskFromRecord(repoTaskRecord.get());

					lastRepo = task.getSource().getLeft().orElseThrow().getRepoId();

					// Mark task as "in process"
					db.update(TASK)
						.set(TASK.IN_PROCESS, true)
						.where(TASK.ID.eq(repoTaskRecord.get().getId()))
						.execute();

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
					.where(TASK.IN_PROCESS.eq(false))
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
					.where(
						TASK.REPO_ID.eq(nextRepoId.getId().toString())
							.and(TASK.IN_PROCESS.eq(false))
					)
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

		databaseStorage.acquireReadTransaction(db -> {
			// 1.) Get manual tasks
			db.selectFrom(TASK)
				.where(TASK.PRIORITY.lessThan(ROUND_ROBIN_PRIORITY.asInt()))
				.orderBy(TASK.PRIORITY.asc(), TASK.UPDATE_TIME.desc())
				.forEach(record -> completeTaskList.addLast(taskFromRecord(record)));

			// 2.) Get default repo tasks that have to be ordered in a round robin way
			Map<RepoId, LinkedList<Task>> groupMap = new HashMap<>();
			db.selectFrom(TASK)
				.where(TASK.REPO_ID.isNotNull())
				.and(TASK.PRIORITY.ge(ROUND_ROBIN_PRIORITY.asInt()))
				.orderBy(TASK.INSERT_TIME.desc())
				.forEach(record -> {
					Task task = taskFromRecord(record);
					RepoId repoId = task.getSource().getLeft().orElseThrow().getRepoId();

					if (!groupMap.containsKey(repoId)) {
						groupMap.put(repoId, new LinkedList<>());
					}

					groupMap.get(repoId).addLast(task);
				});

			if (groupMap.isEmpty()) {
				return; // we are done here
			}

			List<RepoId> repoIds = new ArrayList<>(groupMap.keySet());
			if (lastRepo != null && !repoIds.contains(lastRepo)) {
				// The repo of the next task is determined starting in order from the repo whose task was
				// last executed. If that repo is not in the repoIds list, we add it here so we can
				// determine the correct starting index later.
				repoIds.add(lastRepo);
			}
			repoIds.sort(Comparator.comparing(repoId -> repoId.getId().toString()));

			int startingIndex = (lastRepo == null) ? 0 : (repoIds.indexOf(lastRepo) + 1) % repoIds.size();

			for (int i = startingIndex; !groupMap.isEmpty(); i = (i + 1) % repoIds.size()) {
				RepoId currentRepoId = repoIds.get(i);
				LinkedList<Task> tasks = groupMap.get(currentRepoId);

				// If we have  exhausted the tasks for the current repo, its task list entry will have been
				// removed already.
				if (tasks == null) {
					continue;
				}

				completeTaskList.addLast(tasks.pollFirst());
				if (tasks.isEmpty()) {
					groupMap.remove(currentRepoId);
				}
			}
		});

		return completeTaskList;
	}

}
