package de.aaaaaaah.velcom.backend.data.linearlog;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A linear log consists of a repo's ordered commits. A subset of the repo's branches may be
 * selected, and only commits from the selected branches are included in the linear log.
 *
 * <p> There are different strategies for bringing commits into a linear order. Those strategies
 * can be expressed as implementations of this interface.
 */
public interface LinearLog {

	/**
	 * A repo's linear log, expressed as a stream. The stream must be closed manually once it is no
	 * longer used.
	 *
	 * @param repo the repo to take the commits from
	 * @param branches the branches to restrict the commits to
	 * @return a stream representing a linear log of commits. The stream must be closed manually
	 * 	once it is no longer used
	 * @throws LinearLogException if anything goes wrong (for example, the underlying jgit repo may
	 * 	close unexpectedly)
	 */
	Stream<Commit> walk(Repo repo, Collection<BranchName> branches) throws LinearLogException;

	/**
	 * A helper function for {@link #walk(Repo, Collection)} that takes {@link Branch}es instead of
	 * {@link BranchName}s.
	 *
	 * <p>Note that the returned stream must be closed after it is no longer being used.</p>
	 *
	 * @param repo the repo to take the commits from
	 * @param branches the branches to restrict the commits to
	 * @return a stream representing a linear log of commits. The stream must be closed manually
	 * 	once it is no longer used
	 * @throws LinearLogException if anything goes wrong (for example, the underlying jgit repo may
	 * 	close unexpectedly)
	 */
	default Stream<Commit> walkBranches(Repo repo, Collection<Branch> branches)
		throws LinearLogException {

		List<BranchName> branchNames = branches.stream()
			.map(Branch::getName)
			.collect(Collectors.toUnmodifiableList());

		return walk(repo, branchNames);
	}

	/**
	 * Attempt to find the commit immediately previous to a specified commit in the linear log.
	 * Usually, this commit would be one of the specified commit's parents.
	 *
	 * @param commit the commit whose predecessor to find
	 * @return the previous commit, if it could be found, and {@link Optional#empty()} otherwise.
	 * 	Also returns {@link Optional#empty()} if a {@link LinearLogException} occurred while trying
	 * 	to find the previous commit
	 */
	default Optional<Commit> getPreviousCommit(Commit commit) {
		Repo repo = commit.getRepo();

		try (Stream<Commit> walk = walkBranches(repo, repo.getTrackedBranches())) {
			Iterator<Commit> remainingCommits = walk
				.dropWhile(c -> !commit.equals(c))
				.skip(1)
				.iterator();

			return remainingCommits.hasNext()
				? Optional.of(remainingCommits.next())
				: Optional.empty();
		} catch (LinearLogException e) {
			return Optional.empty();
		}
	}

	default Pair<Optional<Commit>, Optional<Commit>> getPrevNextCommits(Commit commit) {
		Repo repo = commit.getRepo();
		try (Stream<Commit> walk = walkBranches(repo, repo.getTrackedBranches())) {
			Iterator<Commit> iterator = walk.iterator();

			boolean foundCommit = false;
			Commit nextCommit = null;
			while (iterator.hasNext()) {
				Commit currentCommit = iterator.next();
				if (currentCommit.getHash().equals(commit.getHash())) {
					foundCommit = true;
					break;
				} else {
					nextCommit = currentCommit;
				}
			}

			if (!foundCommit) {
				return new Pair<>(Optional.empty(), Optional.empty());
			}

			Commit prevCommit = null;
			if (iterator.hasNext()) {
				prevCommit = iterator.next();
			}

			return new Pair<>(Optional.ofNullable(prevCommit), Optional.ofNullable(nextCommit));
		} catch (LinearLogException e) {
			return new Pair<>(Optional.empty(), Optional.empty());
		}
	}

}
