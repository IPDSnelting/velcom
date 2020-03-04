package de.aaaaaaah.velcom.backend.data.recentbenchmarks;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import de.aaaaaaah.velcom.backend.newaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ParentMapper {

	private final RepoReadAccess repoAccess;
	private final LinearLog linearLog;
	private final Map<RepoId, Map<CommitHash, CommitHash>> globalParentMap;

	public ParentMapper(RepoReadAccess repoAccess, LinearLog linearLog) {
		this.repoAccess = repoAccess;
		this.linearLog = linearLog;
		this.globalParentMap = new HashMap<>();
	}

	public Optional<CommitHash> getParent(RepoId repoId, CommitHash hash)
		throws LinearLogException {

		Map<CommitHash, CommitHash> parentMap = globalParentMap.get(repoId);

		if (parentMap == null) {
			long start = System.currentTimeMillis();

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

			long end = System.currentTimeMillis();
			//System.err.println("ParentMapper: Generated in " + (end-start) + " ms for: " + repoId);
		}

		return Optional.ofNullable(parentMap.get(hash));
	}

}
