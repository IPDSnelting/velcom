package de.aaaaaaah.velcom.backend.listener.dbupdate;

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
import java.util.ArrayList;
import java.util.List;
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

		repoIdStr = repo.getIdAsString();
	}

	/**
	 * Perform the update (see class level javadoc).
	 *
	 * @return the hashes of all commits that need to be entered into the queue. These might (but
	 * 	should usually not) include commits that are already in the queue.
	 */
	// TODO: 21.10.20 Delete unreachable commits
	public List<CommitHash> update() throws DbUpdateException {
		try {
			// Checking this now since #insertAllUnknownCommits() will change the result
			boolean anyCommits = anyCommits();

			insertAllUnknownCommits();
			updateBranches();
			updateReachableFlags();

			List<CommitHash> toBeQueued;
			if (anyCommits) {
				LOGGER.debug("Detected non-empty repository");
				toBeQueued = findNewTasks();
			} else {
				LOGGER.debug("Detected empty (i. e. new) repository");
				toBeQueued = useHeadsOfTrackedBranchesAsTasks();
			}

			updateTrackedFlags();

			return toBeQueued;
		} catch (GitAPIException | IOException e) {
			throw new DbUpdateException("Failed to update repo " + repo, e);
		}
	}

	private KnownCommitRecord jgitCommitToKnownCommitRecord(JgitCommit jgitCommit) {
		KnownCommitRecord record = KNOWN_COMMIT.newRecord();
		record.setRepoId(repoIdStr);
		record.setHash(jgitCommit.getHashAsString());
		record.setReachable(false);
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

	private boolean anyCommits() {
		return db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.limit(1)
			.fetchOptional()
			.isPresent();
	}

	/**
	 * Search through all commits in the jgit repo and add all those to the db that don't already
	 * exist (including parent relationships).
	 *
	 * @throws IOException if jgit does (not sure when that happens)
	 * @throws GitAPIException if jgit does (not sure when that happens)
	 */
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
	 * Update the "reachable" field for all commits. It is set to true if the commit is reachable from
	 * any branch, false otherwise. Works similar to {@link #updateTrackedFlags()}.
	 */
	private void updateReachableFlags() {
		LOGGER.debug("Updating reachable flags");

		String query = ""
			+ "WITH RECURSIVE rec(hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash\n"
			+ "  FROM branch\n"
			+ "  WHERE branch.repo_id = ?\n" // <-- Binding #1
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN rec\n"
			+ "    ON rec.hash = commit_relationship.child_hash\n"
			+ ")\n"
			+ "\n"
			+ "UPDATE known_commit\n"
			+ "SET reachable = (known_commit.hash IN rec)\n"
			+ "WHERE known_commit.repo_id = ?\n" // <-- Binding #2
			+ "";

		db.dsl().execute(query, repoIdStr, repoIdStr);
	}

	/**
	 * Update the "tracked" field for all commits. It is set to true if the commit is reachable from a
	 * tracked branch, false otherwise. This is accomplished with a nifty recursive query that
	 * hopefully works as intended and is not too resource intensive.
	 */
	private void updateTrackedFlags() {
		LOGGER.debug("Updating tracked flags");

		String query = ""
			+ "WITH RECURSIVE rec(hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash\n"
			+ "  FROM branch\n"
			+ "  WHERE branch.repo_id = ?\n" // <-- Binding #1
			+ "  AND branch.tracked\n"
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN rec\n"
			+ "    ON rec.hash = commit_relationship.child_hash\n"
			+ ")\n"
			+ "\n"
			+ "UPDATE known_commit\n"
			+ "SET tracked = (known_commit.hash IN rec)\n"
			+ "WHERE known_commit.repo_id = ?\n" // <-- Binding #2
			+ "";

		db.dsl().execute(query, repoIdStr, repoIdStr);
	}

	/**
	 * Update the "branch" table with the repo's current branches.
	 *
	 * @throws GitAPIException if jgit fails somehow
	 */
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
	 * @return the hashes of all commits that need to be entered into the queue. These may include
	 * 	commits that are already in the queue.
	 */
	private List<CommitHash> findNewTasks() {
		LOGGER.debug("Finding new tasks");

		// The "untracked" CTE will only ever include reachable commits since we start at branches. This
		// means we don't have to check the "reachable" flag in the query.
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

		return db.dsl().fetchLazy(query, repoIdStr, repoIdStr, repoIdStr, repoIdStr)
			.stream()
			.map(record -> (String) record.getValue(0))
			.map(CommitHash::new)
			.collect(toList());
	}

	/**
	 * @return the hashes of all commits that need to be entered into the queue. In theory, these may
	 * 	include commits that are already in the queue and even commits that have already been
	 * 	benchmarked. In practice, this function should only be called for new repos, so the queue
	 * 	should not yet contain any of the repo's commits.
	 */
	private List<CommitHash> useHeadsOfTrackedBranchesAsTasks() {
		// Similar to #findNewTasks(), this function won't ever find unreachable commits.
		return db.selectFrom(BRANCH)
			.where(BRANCH.REPO_ID.eq(repoIdStr))
			.and(BRANCH.TRACKED)
			.stream()
			.map(BranchRecord::getLatestCommitHash)
			.map(CommitHash::new)
			.collect(toList());
	}
}
