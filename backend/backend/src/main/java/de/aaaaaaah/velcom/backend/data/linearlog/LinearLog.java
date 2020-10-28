package de.aaaaaaah.velcom.backend.data.linearlog;

import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.util.Collection;
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
	 * @return a stream representing a linear log of commits. The stream must be closed manually once
	 * 	it is no longer used
	 * @throws LinearLogException if anything goes wrong (for example, the underlying jgit repo may
	 * 	close unexpectedly)
	 */
	Stream<Commit> walk(RepoId repo, Collection<BranchName> branches) throws LinearLogException;

	/**
	 * A helper function for {@link #walk(RepoId, Collection)} that takes {@link Branch}es instead of
	 * {@link BranchName}s.
	 *
	 * <p>Note that the returned stream must be closed after it is no longer being used.</p>
	 *
	 * @param repo the repo to take the commits from
	 * @param branches the branches to restrict the commits to
	 * @return a stream representing a linear log of commits. The stream must be closed manually once
	 * 	it is no longer used
	 * @throws LinearLogException if anything goes wrong (for example, the underlying jgit repo may
	 * 	close unexpectedly)
	 */
	default Stream<Commit> walkBranches(RepoId repo, Collection<Branch> branches)
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
	 * @return the previous commit, if it could be found, and {@link Optional#empty()} otherwise. Also
	 * 	returns {@link Optional#empty()} if a {@link LinearLogException} occurred while trying to find
	 * 	the previous commit
	 */
	Optional<Commit> getPreviousCommit(Commit commit);

	Pair<Optional<Commit>, Optional<Commit>> getPrevNextCommits(Commit commit);

}
