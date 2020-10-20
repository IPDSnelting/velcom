package de.aaaaaaah.velcom.backend.listener.dbupdate;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommit;
import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommitWalk;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.jooq.codegen.db.tables.records.BranchRecord;
import org.jooq.codegen.db.tables.records.CommitRelationshipRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For a single repo, update the db to mirror the actual git repo's branches and commits.
 */
public class DbUpdater {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbUpdater.class);

	private final Repo repo;
	private final Repository jgitRepo;
	private final DBWriteAccess db;

	private final String repoIdStr;

	public DbUpdater(Repo repo, Repository jgitRepo, DBWriteAccess db) {
		this.repo = repo;
		this.jgitRepo = jgitRepo;
		this.db = db;

		repoIdStr = repo.getId().getIdAsString();
	}

	/**
	 * Perform the update (see class level javadoc).
	 *
	 * @param toBeQueued This is expected to be an empty, mutable list. This function will add the
	 * 	hashes of all commits that need to be entered into the queue. These may include commits that
	 * 	are already in the queue.
	 */
	public void update(List<CommitHash> toBeQueued) throws DbUpdateException {
		if (anyUnmigratedBranchesOrCommits()) {
			LOGGER.info("Migrating repo {}", repo);

			try {
				insertAllUnknownCommits();
				migrateCommits();
				updateBranches();
				updateTrackedFlags();
			} catch (GitAPIException | IOException e) {
				throw new DbUpdateException("Failed to migrate repo " + repo, e);
			}
		} else {
			LOGGER.debug("Updating repo {}", repo);

			try {
				// Checking this now since #insertAllUnknownCommits() will change the result
				boolean anyCommits = anyCommits();

				insertAllUnknownCommits();
				updateBranches();

				if (anyCommits) {
					LOGGER.debug("Detected non-empty repository");
					findNewTasks(toBeQueued);
				} else {
					LOGGER.debug("Detected empty (i. e. new) repository");
					useHeadsOfTrackedBranchesAsTasks(toBeQueued);
				}

				updateTrackedFlags();
			} catch (GitAPIException | IOException e) {
				throw new DbUpdateException("Failed to update repo " + repo, e);
			}
		}
	}

	private KnownCommitRecord jgitCommitToKnownCommitRecord(JgitCommit jgitCommit) {
		KnownCommitRecord record = KNOWN_COMMIT.newRecord();
		record.setMigrated(true);
		record.setRepoId(repoIdStr);
		record.setHash(jgitCommit.getHashAsString());
		record.setTracked(false);
		record.setAuthor(jgitCommit.getAuthor());
		record.setAuthorDate(jgitCommit.getAuthorDate());
		record.setCommitter(jgitCommit.getCommitter());
		record.setCommitterDate(jgitCommit.getCommitterDate());
		record.setMessage(jgitCommit.getMessage());
		return record;
	}

	private List<CommitRelationshipRecord> jgitCommitToCommRelRecords(JgitCommit jgitCommit) {
		return jgitCommit.getParentHashes().stream()
			.map(parentHash -> {
				CommitRelationshipRecord record = COMMIT_RELATIONSHIP.newRecord();
				record.setRepoId(repoIdStr);
				record.setChildHash(jgitCommit.getHashAsString());
				record.setParentHash(parentHash.getHash());
				return record;
			})
			.collect(toList());
	}

	private boolean anyUnmigratedBranchesOrCommits() {
		boolean unmigratedBranches = db.selectFrom(BRANCH)
			.where(BRANCH.REPO_ID.eq(repoIdStr))
			.andNot(BRANCH.MIGRATED)
			.limit(1)
			.fetchOptional()
			.isPresent();

		boolean unmigratedCommits = db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.andNot(KNOWN_COMMIT.MIGRATED)
			.limit(1)
			.fetchOptional()
			.isPresent();

		return unmigratedBranches || unmigratedCommits;
	}

	private boolean anyCommits() {
		return db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.limit(1)
			.fetchOptional()
			.isPresent();
	}

	/**
	 * Search through all commits in the jgit repo and add all those to the db that don't already
	 * exist (including parent relationships). Un-migrated commits are left alone.
	 *
	 * @throws IOException if jgit does (not sure when that happens)
	 * @throws GitAPIException if jgit does (not sure when that happens)
	 */
	// TODO: 20.10.20 Update comment after migration
	private void insertAllUnknownCommits() throws IOException, GitAPIException {
		LOGGER.debug("Inserting all unknown commits");

		// See comment in #insertNewCommits()
		Set<String> knownCommits = db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.fetchSet(KNOWN_COMMIT.HASH);

		List<KnownCommitRecord> recordsToInsert = new ArrayList<>();
		List<CommitRelationshipRecord> relationshipsToInsert = new ArrayList<>();

		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			walk.getAllCommits()
				.filter(commit -> !knownCommits.contains(commit.getHashAsString()))
				.forEach(jgitCommit -> {
					recordsToInsert.add(jgitCommitToKnownCommitRecord(jgitCommit));
					relationshipsToInsert.addAll(jgitCommitToCommRelRecords(jgitCommit));
				});
		}

		// Inserting the commits first so we don't violate any foreign key constraints
		db.dsl().batchInsert(recordsToInsert).execute();
		db.dsl().batchInsert(relationshipsToInsert).execute();
	}

	/**
	 * Migrate all commits that haven't been migrated yet by looking them up in the jgit repo. After
	 * this function is called, there should be no more un-migrated commits for this repo. Commits
	 * that could not be found in the jgit repo are deleted.
	 */
	private void migrateCommits() {
		LOGGER.debug("Migrating commits");

		List<KnownCommitRecord> toBeDeleted = new ArrayList<>();
		List<KnownCommitRecord> toBeUpdated = new ArrayList<>();
		List<CommitRelationshipRecord> relationships = new ArrayList<>();

		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
				.andNot(KNOWN_COMMIT.MIGRATED)
				.forEach(record -> {
					CommitHash hash = new CommitHash(record.getHash());
					Optional<JgitCommit> optionalCommit = walk.getCommit(hash);

					if (optionalCommit.isPresent()) {
						JgitCommit commit = optionalCommit.get();

						// Update the main record
						record.setMigrated(true);
						record.setAuthor(commit.getAuthor());
						record.setAuthorDate(commit.getAuthorDate());
						record.setCommitter(commit.getCommitter());
						record.setCommitterDate(commit.getCommitterDate());
						record.setMessage(commit.getMessage());
						toBeUpdated.add(record);

						// Add all parent relationships
						commit.getParentHashes().stream()
							.map(parentHash -> {
								CommitRelationshipRecord relRecord = COMMIT_RELATIONSHIP.newRecord();
								relRecord.setRepoId(repoIdStr);
								relRecord.setChildHash(hash.getHash());
								relRecord.setParentHash(parentHash.getHash());
								return relRecord;
							})
							.forEach(relationships::add);
					} else {
						toBeDeleted.add(record);
					}
				});
		}

		LOGGER.info("Deleting {} commits, updating {} commits, inserting {} relationships",
			toBeDeleted.size(), toBeUpdated.size(), relationships.size());

		db.dsl().batchDelete(toBeDeleted).execute();
		db.dsl().batchUpdate(toBeUpdated).execute();
		db.dsl().batchInsert(relationships).execute();
	}

	/**
	 * Update the "tracked" field for all commits. It is set to true if the commit is reachable from a
	 * tracked branch, false otherwise. This is accomplished with a nifty recursive query that
	 * hopefully works as intended and is not too resource intensive.
	 */
	private void updateTrackedFlags() {
		LOGGER.debug("Updating tracked flags");

		String query = ""
			+ "WITH RECURSIVE reachable(r_hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash\n"
			+ "  FROM branch\n"
			+ "  WHERE branch.repo_id = ?\n" // <-- Binding #1
			+ "  AND branch.tracked\n"
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN reachable\n"
			+ "    ON reachable.r_hash = commit_relationship.child_hash\n"
			+ ")\n"
			+ "\n"
			+ "UPDATE known_commit\n"
			+ "SET tracked = (known_commit.hash IN reachable)\n"
			+ "WHERE known_commit.repo_id = ?\n" // <-- Binding #2
			+ "";

		db.dsl().execute(query, repoIdStr, repoIdStr);
	}

	/**
	 * Crawl the jgit repo (breadth-first) and insert all previously unseen commits into the db (with
	 * "tracked" set to false).
	 */
	private void insertNewCommits() throws GitAPIException {
		LOGGER.debug("Inserting new commits");

		// Breadth-first search through jgit repo.
		// TODO: 20.10.20 See if bfs could be replaced by simple walk of all jgit commits

		// All commits that should not be added to the queue. Keeping them all in memory should be fine
		// for pretty much all sizes of repos we're working with.
		Set<String> knownHashes = new HashSet<>(db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.fetchSet(KNOWN_COMMIT.HASH));

		Queue<String> queue = new Git(jgitRepo).branchList().call().stream()
			.map(ref -> ref.getObjectId().getName())
			// Careful not to initialize the queue with any unwanted commits
			.filter(hash -> !knownHashes.contains(hash))
			.collect(toCollection(ArrayDeque::new));

		// Of course the commits already in the queue should not be added to the queue again
		knownHashes.addAll(queue);

		List<JgitCommit> commitsFound = new ArrayList<>();

		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			while (!queue.isEmpty()) {
				CommitHash hash = new CommitHash(queue.poll());
				// If we can't get the commit out of jgit (which shouldn't normally happen), we just treat
				// it as if it didn't exist
				walk.getCommit(hash).ifPresent(commit -> {
					// Since this commit comes from the queue, it should also be part of our results since it
					// was previously not known.
					commitsFound.add(commit);

					List<String> interestingParentHashes = commit.getParentHashes().stream()
						.map(CommitHash::getHash)
						// Again, careful not to add known commits to the queue
						.filter(parentHash -> !knownHashes.contains(parentHash))
						.collect(toList());
					knownHashes.addAll(interestingParentHashes);
					queue.addAll(interestingParentHashes);
				});
			}
		}

		// And finally some transforming required by jOOQ for its batch inserts

		List<KnownCommitRecord> newCommits = commitsFound.stream()
			.map(this::jgitCommitToKnownCommitRecord)
			.collect(toList());

		List<CommitRelationshipRecord> newRelationships = commitsFound.stream()
			.flatMap(commit -> jgitCommitToCommRelRecords(commit).stream())
			.collect(toList());

		db.dsl().batchInsert(newCommits).execute();
		db.dsl().batchInsert(newRelationships).execute();
	}

	/**
	 * Update the "branch" table with the repo's current branches.
	 * <p>
	 * This function should also work fine on un-migrated branches.
	 *
	 * @throws GitAPIException if jgit fails somehow
	 */
	// TODO: 19.10.20 Update this comment after migration
	private void updateBranches() throws GitAPIException {
		LOGGER.debug("Updating branches");

		Set<BranchName> trackedBranchNames = db.selectFrom(BRANCH)
			.where(BRANCH.REPO_ID.eq(repoIdStr))
			.and(BRANCH.TRACKED)
			.stream()
			.map(record -> BranchName.fromName(record.getName()))
			.collect(toSet());

		List<BranchRecord> branchRecords = new Git(jgitRepo).branchList().call().stream()
			.map(ref -> {
				BranchName name = BranchName.fromFullName(ref.getName());
				String latestCommitHash = ref.getObjectId().getName();

				BranchRecord record = BRANCH.newRecord();
				record.setMigrated(true);
				record.setRepoId(repoIdStr);
				record.setName(name.getName());
				record.setLatestCommitHash(latestCommitHash);
				record.setTracked(trackedBranchNames.contains(name));
				return record;
			})
			.collect(toList());

		db.deleteFrom(BRANCH).where(BRANCH.REPO_ID.eq(repoIdStr)).execute();
		db.dsl().batchInsert(branchRecords).execute();
	}

	/**
	 * Find all commits that should be tracked (via a recursive query) and add them to the queue (if
	 * they haven't already been benchmarked yet).
	 *
	 * @param toBeQueued This is expected to be an empty, mutable list. This function will add the
	 * 	hashes of all commits that need to be entered into the queue. These may include commits that
	 * 	are already in the queue.
	 */
	private void findNewTasks(List<CommitHash> toBeQueued) {
		LOGGER.debug("Finding new tasks");

		String query = ""
			+ "WITH RECURSIVE\n"
			+ "\n"
			+ "untracked(hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash\n"
			+ "  FROM branch\n"
			+ "  JOIN known_commit\n"
			+ "    ON known_commit.repo_id = branch.repo_id\n"
			+ "    AND known_commit.hash = branch.latest_commit_hash\n"
			+ "  WHERE branch.repo_id = ?\n" // <-- Binding #1
			+ "  AND branch.tracked\n"
			+ "  AND NOT known_commit.tracked\n"
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT known_commit.hash\n"
			+ "  FROM known_commit\n"
			+ "  JOIN commit_relationship\n"
			+ "    ON commit_relationship.parent_hash = known_commit.hash\n"
			+ "  JOIN untracked\n"
			+ "    ON untracked.hash = commit_relationship.child_hash\n"
			+ "  WHERE known_commit.repo_id = ?\n" // <-- Binding #2
			+ "  AND NOT known_commit.tracked\n"
			+ "),\n"
			+ "\n"
			+ "has_result(hash) AS (\n"
			+ "  SELECT DISTINCT run.commit_hash\n"
			+ "  FROM run\n"
			+ "  WHERE run.repo_id = ?\n" // <-- Binding #3
			+ "  AND run.commit_hash IS NOT NULL\n"
			+ "),\n"
			+ "\n"
			+ "in_queue(hash) AS (\n"
			+ "  SELECT DISTINCT task.commit_hash\n"
			+ "  FROM task\n"
			+ "  WHERE task.repo_id = ?\n" // <-- Binding #4
			+ "  AND task.commit_hash IS NOT NULL\n"
			+ ")\n"
			+ "\n"
			+ "SELECT DISTINCT untracked.hash\n"
			+ "FROM untracked\n"
			+ "WHERE untracked.hash NOT IN has_result\n"
			+ "AND untracked.hash NOT IN in_queue\n"
			+ "";

		db.dsl().fetchLazy(query, repoIdStr, repoIdStr, repoIdStr, repoIdStr)
			.stream()
			.map(record -> (String) record.getValue(0))
			.map(CommitHash::new)
			.forEach(toBeQueued::add);
	}

	/**
	 * @param toBeQueued This is expected to be an empty, mutable list. This function will add the
	 * 	hashes of all commits that need to be entered into the queue. These may include commits that
	 * 	are already in the queue.
	 */
	private void useHeadsOfTrackedBranchesAsTasks(List<CommitHash> toBeQueued) {
		// TODO: 20.10.20 Filter out commits which have already been benchmarked
		db.selectFrom(BRANCH)
			.where(BRANCH.REPO_ID.eq(repoIdStr))
			.and(BRANCH.TRACKED)
			.stream()
			.map(BranchRecord::getLatestCommitHash)
			.map(CommitHash::new)
			.forEach(toBeQueued::add);
	}
}
