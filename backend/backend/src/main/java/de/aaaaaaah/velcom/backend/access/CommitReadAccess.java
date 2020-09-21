package de.aaaaaaah.velcom.backend.access;


import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.exceptions.CommitAccessException;
import de.aaaaaaah.velcom.backend.access.exceptions.CommitLogException;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.access.filter.AuthorTimeRevFilter;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotCommitList;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revplot.PlotWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class CommitReadAccess {

	private final RepoStorage repoStorage;

	public CommitReadAccess(RepoStorage repoStorage) {
		this.repoStorage = repoStorage;
	}

	private static String formatPersonIdent(PersonIdent ident) {
		final String name = ident.getName();

		if (name.isEmpty()) {
			return "<" + ident.getEmailAddress() + ">";
		} else {
			return name + " <" + ident.getEmailAddress() + ">";
		}
	}

	static Commit commitFromRevCommit(RepoId repoId, RevCommit revCommit) {
		CommitHash ownHash = new CommitHash(revCommit.getId().getName());

		List<CommitHash> parentHashes = List.of(revCommit.getParents()).stream()
			.map(RevCommit::getId)
			.map(AnyObjectId::getName)
			.map(CommitHash::new)
			.collect(Collectors.toUnmodifiableList());

		PersonIdent author = revCommit.getAuthorIdent();
		PersonIdent committer = revCommit.getCommitterIdent();

		return new Commit(
			repoId,
			ownHash,
			parentHashes,
			formatPersonIdent(author),
			author.getWhen().toInstant(),
			formatPersonIdent(committer),
			committer.getWhen().toInstant(),
			revCommit.getFullMessage()
		);
	}

	/**
	 * Fetch a single {@link Commit}.
	 *
	 * @param repoId the repo to search
	 * @param commitHash the commit to retrieve
	 * @return The commit if it could be found. Returns empty if the repo doesn't exist or the commit
	 * 	could not be found.
	 */
	public Commit getCommit(RepoId repoId, CommitHash commitHash) {
		Map<CommitHash, Commit> commits = getCommits(repoId, List.of(commitHash));

		return Optional.ofNullable(commits.get(commitHash))
			.orElseThrow(() -> new NoSuchCommitException(repoId, commitHash));
	}

	/**
	 * Fetch {@link Commit}s for a list of {@link CommitHash}es.
	 *
	 * @param repoId the repo to search
	 * @param commitHashes the commits to retrieve
	 * @return Those commits that could be found. If the repo could not be found, returns an empty
	 * 	list. If a commit could not be found, doesn't return that commit in the return value.
	 */
	public Map<CommitHash, Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes) {
		Objects.requireNonNull(repoId);
		Objects.requireNonNull(commitHashes);

		if (commitHashes.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<CommitHash, Commit> commits = new HashMap<>();

		try (
			Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName());
			RevWalk walk = new RevWalk(repo)
		) {
			for (CommitHash hash : commitHashes) {
				try {
					ObjectId commitPtr = repo.resolve(hash.getHash());
					RevCommit revCommit = walk.parseCommit(commitPtr);
					commits.put(hash, commitFromRevCommit(repoId, revCommit));
				} catch (IOException | NullPointerException ignored) {
					// See javadoc
				}
			}
		} catch (RepositoryAcquisitionException ignored) {
			// See javadoc
		}

		return commits;
	}

	/**
	 * Find the children of a given commit.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit's hash
	 * @return the commit's children
	 */
	public Collection<CommitHash> getChildren(RepoId repoId, CommitHash commitHash) {
		try (
			Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName());
			PlotWalk plotWalk = new PlotWalk(repo);
		) {
			ObjectId targetId = repo.resolve(commitHash.getHash());

			List<RevCommit> startPoints = new ArrayList<>();
			for (Ref ref : repo.getRefDatabase().getRefs()) {
				ObjectId objectId = ref.getObjectId();
				RevCommit revCommit = plotWalk.parseCommit(objectId);
				startPoints.add(revCommit);
			}
			plotWalk.markStart(startPoints);

			PlotCommitList<PlotLane> plotCommitList = new PlotCommitList<>();
			plotCommitList.source(plotWalk);
			plotCommitList.fillTo(Integer.MAX_VALUE);

			PlotCommit<PlotLane> commit = plotCommitList.stream()
				.filter(targetId::equals)
				.findAny()
				.orElseThrow(() -> new NoSuchCommitException(repoId, commitHash));

			List<PlotCommit<PlotLane>> children = new ArrayList<>();
			for (int i = 0; i < commit.getChildCount(); i++) {
				children.add(commit.getChild(i));
			}

			return children.stream()
				.map(AnyObjectId::getName)
				.map(CommitHash::new)
				.collect(Collectors.toList());
		} catch (Exception e) {
			throw new CommitAccessException("Failed to find children", e, repoId, commitHash);
		}
	}

	/**
	 * Constructs a new commit walk instance starting at the specified commit.
	 *
	 * @param startCommit the commit to start at
	 * @return the commit walk instance
	 */
	public CommitWalk getCommitWalk(Commit startCommit) {
		RepoId repoId = startCommit.getRepoId();
		Repository repo = null;
		RevWalk walk = null;

		try {
			repo = repoStorage.acquireRepository(repoId.getDirectoryName());
			walk = new RevWalk(repo);

			return new CommitWalk(repoId, repo, walk, startCommit);
		} catch (Exception e) {
			if (walk != null) {
				walk.close();
			}
			if (repo != null) {
				repo.close();
			}
			throw new CommitAccessException("Failed to create commit walk", e,
				startCommit.getRepoId(), startCommit.getHash());
		}
	}

	/**
	 * Collects all commits from the specified repository that 1.) were authored between the given
	 * startTime and stopTime and 2.) are reachable from the given branches
	 *
	 * <p>If no startTime or no stopTime are specified, the author date will not be limited
	 * in that regard.</p>
	 *
	 * @param repoId the id of the repository
	 * @param branches what branches to consider
	 * @param startTime the start time
	 * @param stopTime the stop time
	 * @return a map with each commit hash pointing to its respective commit
	 * @throws IllegalArgumentException if startTime is after stopTime
	 */
	public Map<CommitHash, Commit> getCommitsBetween(RepoId repoId,
		Collection<BranchName> branches, @Nullable Instant startTime, @Nullable Instant stopTime) {

		Objects.requireNonNull(repoId);
		Objects.requireNonNull(branches);

		if (startTime != null && stopTime != null && startTime.isAfter(stopTime)) {
			throw new IllegalArgumentException(
				"start time is after stop time: " + startTime + " > " + stopTime
			);
		}

		if (branches.isEmpty()) {
			return Collections.emptyMap();
		}

		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			RevWalk walk = new RevWalk(repo);
			Git git = Git.wrap(repo);

			// Start the walk from the specified branches
			Map<CommitHash, Commit> commitMap = new HashMap<>();

			for (Ref branchRef : git.branchList().call()) {
				BranchName branchName = BranchName.fromFullName(branchRef.getName());

				if (branches.contains(branchName)) {
					ObjectId branchObjectId = branchRef.getObjectId();
					RevCommit revCommit = walk.parseCommit(branchObjectId); // Doesn't load body

					walk.markStart(revCommit);
				}
			}

			// Restrict the search results
			if (startTime != null && stopTime != null) {
				walk.setRevFilter(AuthorTimeRevFilter.between(startTime, stopTime));
			} else if (startTime != null) {
				walk.setRevFilter(AuthorTimeRevFilter.after(startTime));
			} else if (stopTime != null) {
				walk.setRevFilter(AuthorTimeRevFilter.before(stopTime));
			}

			for (RevCommit revCommit : walk) {
				Commit commit = commitFromRevCommit(repoId, revCommit);
				commitMap.put(commit.getHash(), commit);
			}

			return commitMap;
		} catch (RepositoryAcquisitionException | GitAPIException | IOException e) {
			throw new RepoAccessException(repoId, e);
		}
	}

	public Stream<Commit> getCommitLog(RepoId repoId, Collection<BranchName> branches)
		throws CommitLogException {

		// Step -1: Check arguments
		Objects.requireNonNull(repoId);
		Objects.requireNonNull(branches);
		if (branches.isEmpty()) {
			return Stream.empty();
		}

		// Step 0: Sort branches so that the outcome is deterministic
		List<BranchName> sortedBranches = new ArrayList<>(branches);
		Collections.sort(sortedBranches);

		// Step 1: Acquire repository
		Repository jgitRepo;

		try {
			String directoryName = repoId.getDirectoryName();
			jgitRepo = repoStorage.acquireRepository(directoryName);
		} catch (RepositoryAcquisitionException e) {
			throw new CommitLogException(repoId, branches, e);
		}

		try {
			// Step 2: Run log command
			LogCommand logCommand = Git.wrap(jgitRepo).log();

			for (BranchName branchName : sortedBranches) {
				ObjectId branchId = jgitRepo.resolve(branchName.getFullName());
				logCommand.add(branchId);
			}

			// Step 3: Prepare stream
			Spliterator<RevCommit> commitSpliterator = Spliterators.spliteratorUnknownSize(
				logCommand.call().iterator(), 0);

			return StreamSupport.stream(commitSpliterator, false)
				.map(revCommit -> commitFromRevCommit(repoId, revCommit))
				.onClose(jgitRepo::close);
		} catch (Exception e) {
			jgitRepo.close(); // Release repo storage lock if this fails
			throw new CommitLogException(repoId, branches, e);
		}
	}

}
