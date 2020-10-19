package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommit;
import de.aaaaaaah.velcom.backend.listener.jgitutils.JgitCommitWalk;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.jooq.Field;
import org.jooq.codegen.db.tables.records.BranchRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;

/**
 * For a single repo, update the db to mirror the actual git repo's branches and commits.
 */
public class DbUpdater {

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

	// TODO: 19.10.20 Update this comment after migration

	/**
	 * Update the "branch" table with the repo's current branches.
	 * <p>
	 * This function should also work fine on un-migrated branches.
	 *
	 * @throws GitAPIException if jgit fails somehow
	 */
	public void updateBranches() throws GitAPIException {
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

	public void updateCommits() {
		migrateCommits();
		// TODO: 19.10.20 Implement
	}

	private void migrateCommits() {
		try (JgitCommitWalk walk = new JgitCommitWalk(jgitRepo)) {
			List<KnownCommitRecord> toBeDeleted = new ArrayList<>();
			List<KnownCommitRecord> toBeUpdated = new ArrayList<>();

			db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoIdStr))
				.and(KNOWN_COMMIT.MIGRATED.neg())
				.forEach(record -> {
					CommitHash hash = new CommitHash(record.getHash());
					Optional<JgitCommit> optionalCommit = walk.getCommit(hash);

					if (optionalCommit.isPresent()) {
						JgitCommit commit = optionalCommit.get();

						record.setMigrated(true);
						record.setAuthor(commit.getAuthor());
						record.setAuthorDate(commit.getAuthorDate());
						record.setCommitter(commit.getCommitter());
						record.setCommitterDate(commit.getCommitterDate());
						record.setMessage(commit.getMessage());

						toBeUpdated.add(record);
					} else {
						toBeDeleted.add(record);
					}
				});

			db.dsl().batchDelete(toBeDeleted);
			db.dsl().batchUpdate(toBeUpdated);

			// All records already migrated?
			if (toBeDeleted.isEmpty() && toBeUpdated.isEmpty()) {
				return;
			}
		}

		// Initialize the "tracked" fields
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
}
