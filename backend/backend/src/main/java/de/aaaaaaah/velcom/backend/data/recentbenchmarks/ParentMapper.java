package de.aaaaaaah.velcom.backend.data.recentbenchmarks;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Transforms the complete linear history of a repository into a parent map which maps each commit's
 * hash to their respective previous commit's hash.
 *
 * <p>This class is <b>not</b> thread safe.</p>
 */
public class ParentMapper {

	private final RepoReadAccess repoAccess;
	private final LinearLog linearLog;
	private final Map<RepoId, Map<CommitHash, CommitHash>> globalParentMap;

	public ParentMapper(RepoReadAccess repoAccess, LinearLog linearLog) {
		this.repoAccess = repoAccess;
		this.linearLog = linearLog;
		this.globalParentMap = new HashMap<>();
	}

	/**
	 * Get the hash of the commit that is one step before the commit with the specified {@code
	 * hash}.
	 *
	 * <p>
	 * <b>Beware</b> that, if this is the first time that this method is called with the
	 * given {@code repoId}, the complete linear log of that repository will be read into memory and
	 * transformed into the parent map. This can be a quite expensive operation
	 * </p>
	 *
	 * @param repoId the repository of the commit
	 * @param hash the hash of the commit
	 * @return returns the hash of the previous commit, if it exists
	 * @throws LinearLogException if the linear log could not be read from the repository
	 */
	public Optional<CommitHash> getParent(RepoId repoId, CommitHash hash)
		throws LinearLogException {

		Map<CommitHash, CommitHash> parentMap = globalParentMap.get(repoId);

		if (parentMap == null) {
			// Need to generate the parent map for this repository
			parentMap = new HashMap<>();
			globalParentMap.put(repoId, parentMap);

			// Load complete linear log into memory
			List<BranchName> branches = repoAccess.getTrackedBranches(repoId).stream()
				.map(Branch::getName)
				.collect(toList());

			try (Stream<Commit> walk = linearLog.walk(repoId, branches)) {
				Iterator<Commit> iterator = walk.iterator();

				Commit currentCommit = iterator.next();

				while (iterator.hasNext()) {
					Commit nextCommit = iterator.next();

					parentMap.put(currentCommit.getHash(), nextCommit.getHash());

					currentCommit = nextCommit;
				}
			}
		}

		return Optional.ofNullable(parentMap.get(hash));
	}

}
