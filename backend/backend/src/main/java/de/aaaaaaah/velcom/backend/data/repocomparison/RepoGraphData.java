package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.Objects.requireNonNull;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.List;
import java.util.Set;

public class RepoGraphData {

	private final RepoId repoId;
	private final Set<BranchName> includedBranches;
	private final List<GraphEntry> entries;

	public RepoGraphData(RepoId repoId, Set<BranchName> includedBranches, List<GraphEntry> entries) {
		this.repoId = requireNonNull(repoId);
		this.includedBranches = requireNonNull(includedBranches);
		this.entries = requireNonNull(entries);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public Set<BranchName> getIncludedBranches() {
		return includedBranches;
	}

	/**
	 * @return a list containing all entries for this repository sorted by the author date of the
	 * commits.
	 */
	public List<GraphEntry> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		return "RepoGraphData{" +
			"repoId=" + repoId +
			", includedBranches=" + includedBranches +
			'}';
	}

}
