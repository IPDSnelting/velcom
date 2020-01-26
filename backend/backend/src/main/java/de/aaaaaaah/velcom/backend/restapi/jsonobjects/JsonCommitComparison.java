package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;


/**
 * A helper class for serialization representing a comparison between two commits.
 */
public class JsonCommitComparison {

	@Nullable
	private final JsonCommit firstCommit;
	@Nullable
	private final JsonRun firstRun;
	private final JsonCommit secondCommit;
	@Nullable
	private final JsonRun secondRun;
	private final Collection<JsonDifference> differences;

	public JsonCommitComparison(CommitComparison commitComparison) {
		firstCommit = commitComparison.getFirstCommit().map(JsonCommit::new).orElse(null);
		firstRun = commitComparison.getFirstRun().map(JsonRun::new).orElse(null);
		secondCommit = new JsonCommit(commitComparison.getSecondCommit());
		secondRun = commitComparison.getSecondRun().map(JsonRun::new).orElse(null);

		differences = commitComparison.getDifferences().stream()
			.map(JsonDifference::new)
			.collect(Collectors.toUnmodifiableList());
	}

	@Nullable
	public JsonCommit getFirstCommit() {
		return firstCommit;
	}

	@Nullable
	public JsonRun getFirstRun() {
		return firstRun;
	}

	public JsonCommit getSecondCommit() {
		return secondCommit;
	}

	@Nullable
	public JsonRun getSecondRun() {
		return secondRun;
	}

	public Collection<JsonDifference> getDifferences() {
		return differences;
	}

}
