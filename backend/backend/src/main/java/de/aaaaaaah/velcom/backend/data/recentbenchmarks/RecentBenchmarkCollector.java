package de.aaaaaaah.velcom.backend.data.recentbenchmarks;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLogException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects the most recent benchmarked commits and compares them against their previous commits.
 */
public class RecentBenchmarkCollector {

	private static final int BATCH_SIZE = 100;
	private static final int MAX_ROUNDS = 100;

	private final RepoReadAccess repoAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final LinearLog linearLog;
	private final CommitComparer comparer;

	public RecentBenchmarkCollector(RepoReadAccess repoAccess,
		BenchmarkReadAccess benchmarkAccess,
		CommitReadAccess commitAccess, LinearLog linearLog,
		CommitComparer comparer) {

		this.repoAccess = repoAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.linearLog = linearLog;
		this.comparer = comparer;
	}

	/**
	 * Collects the most recent benchmarked commits and compares them against their previous
	 * commits.
	 *
	 * @param amount the amount of commits to collect
	 * @param onlySignificant whether or not to only include commits which brought some significant
	 * 	change compared to its previous commit.
	 * @return an ordered list of collected commits, where the first commit is the one with the most
	 * 	recent run
	 * @throws BenchmarkCollectorException if an error occurs while trying to collect commits
	 */
	public List<CommitComparison> collect(int amount, boolean onlySignificant)
		throws BenchmarkCollectorException {

		List<CommitComparison> resultList = new ArrayList<>();
		ParentMapper parentMapper = new ParentMapper(repoAccess, linearLog);

		try {
			for (int i = 0; i < MAX_ROUNDS && resultList.size() < amount; i++) {
				processBatch(resultList, i * BATCH_SIZE, parentMapper, onlySignificant);
			}
		} catch (Exception e) {
			throw new BenchmarkCollectorException(e);
		}

		return resultList.subList(0, Math.min(amount, resultList.size()));
	}

	private void processBatch(List<CommitComparison> resultList, int skip,
		ParentMapper parentMapper, boolean onlySignificant) throws LinearLogException {

		// 0.) Get runs for this batch
		List<Run> runs = benchmarkAccess.getRecentRuns(skip, BATCH_SIZE);

		// 0.1.) Filter out runs that are not related to any repository
		// TODO: Remove this by completely reworking the recent benchmark collector
		// so that it also works with tars
		runs = runs.stream()
			.filter(run -> run.getSource().isLeft())
			.collect(Collectors.toList());

		// 1.) Filter out runs that belong to the same commit
		Map<RepoId, Map<CommitHash, Run>> commitToRunMap = new HashMap<>();

		Iterator<Run> iterator = runs.iterator();
		while (iterator.hasNext()) {
			Run run = iterator.next();
			RepoId repoId = run.getSource().getLeft().get().getRepoId();
			CommitHash hash = run.getSource().getLeft().get().getHash();

			if (!commitToRunMap.containsKey(repoId)) {
				commitToRunMap.put(repoId, new HashMap<>());
			}

			if (commitToRunMap.get(repoId).containsKey(hash)) {
				// There is already an earlier run (more recent based on the lists ordering)
				// for this commit => older run can be discarded
				iterator.remove();
			} else {
				commitToRunMap.get(repoId).put(hash, run);
			}
		}

		// 2.) Find out which parent runs need to be loaded from database
		Map<RepoId, List<CommitHash>> missingParentRuns = new HashMap<>();

		iterator = runs.iterator();

		while (iterator.hasNext()) {
			Run run = iterator.next();
			RepoId repoId = run.getSource().getLeft().get().getRepoId();
			CommitHash hash = run.getSource().getLeft().get().getHash();

			missingParentRuns.computeIfAbsent(repoId, i -> new ArrayList<>());

			CommitHash parentHash = parentMapper.getParent(repoId, hash)
				.orElse(null);

			if (parentHash == null && onlySignificant) {
				// parent commit does not exist for this run/commit => no comparison can be made
				// => if onlySignificant is true this run can never be significant => remove run
				iterator.remove();
				commitToRunMap.get(repoId).remove(hash);
			} else if (parentHash != null) {
				// Only insert into missingParentRuns if parent commit actually exists
				// (since if it does not exist, no run for that commit can exist => not missing)

				// Check if run of parent commit is already in memory
				if (!commitToRunMap.get(repoId).containsKey(parentHash)) {
					// sadly, parent run either does not exist or is not in memory => need to load
					missingParentRuns.get(repoId).add(parentHash);
				}
			}
		}

		// 3.) Load missing parent runs
		if (!missingParentRuns.isEmpty()) {
			// Maybe a universal identifier for (repoid+commithash pair) would be nice
			// so we don't have to do this for each repository...
			missingParentRuns.forEach((repoId, runHashes) -> {
				Map<CommitHash, Run> latestRuns = benchmarkAccess.getLatestRuns(repoId, runHashes);

				for (Run run : latestRuns.values()) {
					RepoId runRepoId = run.getSource().getLeft().get().getRepoId();
					CommitHash runHash = run.getSource().getLeft().get().getHash();
					commitToRunMap.get(runRepoId).put(runHash, run);
				}
			});
		}

		// 4.) Load commit data
		Map<RepoId, Map<CommitHash, Commit>> commitDataMap = new HashMap<>();

		commitToRunMap.forEach((repoId, hashToRunMap) -> {
			Set<CommitHash> commitHashes = hashToRunMap.keySet();

			List<Commit> commits = commitAccess.getCommits(repoId, commitHashes);

			for (Commit commit : commits) {
				commitDataMap.computeIfAbsent(repoId, i -> new HashMap<>());
				commitDataMap.get(repoId).put(commit.getHash(), commit);
			}
		});

		// 5.) Do the actual comparisons
		iterator = runs.iterator();
		while (iterator.hasNext()) {
			Run run = iterator.next();
			RepoId repoId = run.getSource().getLeft().get().getRepoId();
			CommitHash commitHash = run.getSource().getLeft().get().getHash();

			CommitHash parentHash = parentMapper.getParent(repoId, commitHash)
				.orElse(null);
			Commit commit = commitDataMap.get(repoId).get(commitHash);

			if (commit == null) {
				// No commit data available => no comparison possible
				iterator.remove();
				commitToRunMap.get(repoId).remove(commitHash);
			} else {
				CommitComparison comparison;

				if (parentHash == null) {
					// Parent not available, but since this run still exists, it is okay
					comparison = comparer.compare(null, null, commit, run);
				} else {
					// Parent is available => load it and its run
					Commit parent = commitDataMap.get(repoId).get(parentHash);
					Run parentRun = commitToRunMap.get(repoId).get(parentHash);

					if (parent == null) {
						// parent commit data not available => no comparison can be made with it
						comparison = comparer.compare(null, null, commit, run);
					} else if (parentRun == null) {
						// parent run not available
						comparison = comparer.compare(parent, null, commit, run);
					} else {
						// parent run & parent commit data available
						comparison = comparer.compare(parent, parentRun, commit, run);
					}
				}

				if (!onlySignificant || comparison.isSignificant()) {
					resultList.add(comparison);
				}
			}
		}
	}

}
