package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectFrom;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommit;
import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommitWalk;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
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
import org.jooq.CommonTableExpression;
import org.jooq.Field;
import org.jooq.Record1;
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
	 */
	public void update() throws GitAPIException {
		migrateCommits(); // TODO: 20.10.20 Remove after migration
		updateTrackedFlags();

		// TODO: 20.10.20 Check if this is a new repo (i. e. no known commits)
		//insertNewCommits();

		// Needs to happen after insertNewCommits because of the foreign key from the "branch" table to
		// the "known_commit" table
		//updateBranches();

		// TODO: 20.10.20 Only insert last commits if this is a new repo
		//findNewTasks();
	}

	/**
	 * Migrate all commits that haven't been migrated yet by looking them up in the jgit repo. After
	 * this function is called, there should be no more unmigrated commits for this repo. Commits that
	 * could not be found in the jgit repo are deleted.
	 */
	private void migrateCommits() {
		LOGGER.debug("Migrating commits");

		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			List<KnownCommitRecord> toBeDeleted = new ArrayList<>();
			List<KnownCommitRecord> toBeUpdated = new ArrayList<>();
			List<CommitRelationshipRecord> relationships = new ArrayList<>();

			db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
				.and(KNOWN_COMMIT.MIGRATED.neg())
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

			db.dsl().batchDelete(toBeDeleted);
			db.dsl().batchUpdate(toBeUpdated);
			db.dsl().batchInsert(relationships);
		}
	}

	/**
	 * Update the "tracked" field for all commits. It is set to true if the commit is reachable from a
	 * tracked branch, false otherwise. This is accomplished with a nifty recursive query that
	 * hopefully works as intended and is not too resource intensive.
	 */
	private void updateTrackedFlags() {
		LOGGER.debug("Updating tracked flags");

		Field<String> r_hash = field(name("reachable", "r_hash"), String.class);

		db.dsl().withRecursive("reachable", "r_hash")
			.as(db.select(BRANCH.LATEST_COMMIT_HASH)
				.from(BRANCH)
				.where(BRANCH.REPO_ID.eq(repoIdStr))
				.and(BRANCH.TRACKED)
				.union(db.select(COMMIT_RELATIONSHIP.PARENT_HASH)
					.from(COMMIT_RELATIONSHIP)
					.join(name("reachable")).on(r_hash.eq(COMMIT_RELATIONSHIP.CHILD_HASH))
				)
			)
			.update(KNOWN_COMMIT)
			.set(KNOWN_COMMIT.TRACKED, field(KNOWN_COMMIT.HASH.in(r_hash)))
			.execute();
	}

	/**
	 * Crawl the jgit repo (breadth-first) and insert all previously unseen commits into the db (with
	 * "tracked" set to false).
	 */
	private void insertNewCommits() throws GitAPIException {
		LOGGER.debug("Inserting new commits");

		Set<String> knownHashes = new HashSet<>(db.selectFrom(KNOWN_COMMIT)
			.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
			.fetchSet(KNOWN_COMMIT.HASH));

		Queue<String> queue = new Git(jgitRepo).branchList().call().stream()
			.map(ref -> ref.getObjectId().getName())
			.filter(hash -> !knownHashes.contains(hash))
			.collect(toCollection(ArrayDeque::new));

		knownHashes.addAll(queue);

		List<JgitCommit> commitsFound = new ArrayList<>();

		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			while (!queue.isEmpty()) {
				CommitHash hash = new CommitHash(queue.poll());
				walk.getCommit(hash).ifPresent(commit -> {
					commitsFound.add(commit);

					List<String> interestingParentHashes = commit.getParentHashes().stream()
						.map(CommitHash::getHash)
						.filter(parentHash -> !knownHashes.contains(parentHash))
						.collect(toList());
					knownHashes.addAll(interestingParentHashes);
					queue.addAll(interestingParentHashes);
				});
			}
		}

		List<KnownCommitRecord> newCommits = commitsFound.stream()
			.map(commit -> {
				KnownCommitRecord record = KNOWN_COMMIT.newRecord();
				record.setMigrated(true);
				record.setRepoId(repoIdStr);
				record.setHash(commit.getHashAsString());
				record.setTracked(false);
				record.setAuthor(commit.getAuthor());
				record.setAuthorDate(commit.getAuthorDate());
				record.setCommitter(commit.getCommitter());
				record.setCommitterDate(commit.getCommitterDate());
				record.setMessage(commit.getMessage());
				return record;
			})
			.collect(toList());

		List<CommitRelationshipRecord> newRelationships = commitsFound.stream()
			.flatMap(commit -> commit.getParentHashes().stream()
				.map(parentHash -> {
					CommitRelationshipRecord record = COMMIT_RELATIONSHIP.newRecord();
					record.setRepoId(repoIdStr);
					record.setChildHash(commit.getHashAsString());
					record.setParentHash(parentHash.getHash());
					return record;
				}))
			.collect(toList());

		db.dsl().batchInsert(newCommits);
		db.dsl().batchInsert(newRelationships);
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

		db.deleteFrom(BRANCH).where(BRANCH.REPO_ID.eq(repoIdStr));
		db.dsl().batchInsert(branchRecords);
	}

	/**
	 * Find all commits that should be tracked (via a recursive query), set them to tracked and add
	 * them to the queue (if they haven't already been benchmarked yet).
	 */
	private void findNewTasks() {
		LOGGER.debug("Finding new tasks");

		// TODO: 20.10.20 Fix this recursive query
		Field<String> r_hash = field(name("reachable", "r_hash"), String.class);
		CommonTableExpression<Record1<String>> reachable = name("reachable").fields("r_hash")
			.as(select(BRANCH.LATEST_COMMIT_HASH)
				.from(BRANCH)
				.where(BRANCH.REPO_ID.eq(repoIdStr))
				.and(BRANCH.TRACKED)
				.union(db.select(COMMIT_RELATIONSHIP.PARENT_HASH)
					.from(COMMIT_RELATIONSHIP)
					.join("reachable").on(r_hash.eq(COMMIT_RELATIONSHIP.CHILD_HASH))
				)
			);

		CommonTableExpression<Record1<String>> runExists = name("run_exists").fields("run_hash")
			.as(select(RUN.COMMIT_HASH)
				.from(RUN)
				.where(RUN.REPO_ID.eq(repoIdStr))
				.and(RUN.COMMIT_HASH.isNotNull())
			);

		db.dsl()
			.withRecursive(reachable)
			.with(runExists)
			.select(
				KNOWN_COMMIT.HASH,
				field(KNOWN_COMMIT.HASH.in(selectFrom(runExists)))
			)
			.from(KNOWN_COMMIT);
		// TODO: 20.10.20 Do the things
	}
}
