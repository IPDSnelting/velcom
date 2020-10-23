package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.Objects.requireNonNull;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import java.util.List;

public class RepoGraphData {

	private final RepoId repoId;
	private final List<GraphEntry> entries;

	public RepoGraphData(RepoId repoId, List<GraphEntry> entries) {
		this.repoId = requireNonNull(repoId);
		this.entries = requireNonNull(entries);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	/**
	 * @return a list containing all entries for this repository sorted by the author date of the
	 * 	commits.
	 */
	public List<GraphEntry> getEntries() {
		return entries;
	}

	@Override
	public String toString() {
		return "RepoGraphData{" +
			"repoId=" + repoId +
			'}';
	}

}
