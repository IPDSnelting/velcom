package de.aaaaaaah.velcom.backend.newaccess;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
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

	private static Commit commitFromRevCommit(RepoId repoId, RevCommit revCommit) {
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
	 * @param repoId the repo to searc
	 * @param commitHash the commit to retrieve
	 * @return The commit if it could be found. Returns empty if the repo doesn't exist or the
	 * 	commit could not be found.
	 */
	public Optional<Commit> getCommit(RepoId repoId, CommitHash commitHash) {
		List<Commit> commits = getCommits(repoId, List.of(commitHash));

		if (!commits.isEmpty()) {
			return Optional.of(commits.get(0));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Fetch {@link Commit}s for a list of {@link CommitHash}es.
	 *
	 * @param repoId the repo to search
	 * @param commitHashes the commits to retrieve
	 * @return Those commits that could be found. If the repo could not be found, returns an empty
	 * 	list. If a commit could not be found, doesn't return that commit in the return value.
	 * 	Preserves ordering of commits (and duplicate commits) from the input commit hash
	 * 	collection.
	 */
	public List<Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes) {
		List<Commit> commits = new ArrayList<>();

		try (
			Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName());
			RevWalk walk = new RevWalk(repo)
		) {
			for (CommitHash hash : commitHashes) {
				try {
					ObjectId commitPtr = repo.resolve(hash.getHash());
					RevCommit revCommit = walk.parseCommit(commitPtr);
					commits.add(commitFromRevCommit(repoId, revCommit));
				} catch (IOException ignored) {
					// See javadoc
				}
			}
		} catch (RepositoryAcquisitionException ignored) {
			// See javadoc
		}

		return commits;
	}

}
