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
	private final JsonRun first;
	@Nullable
	private final JsonRun second;
	private final Collection<JsonDifference> differences;

	public JsonCommitComparison(CommitComparison commitComparison) {
		first = commitComparison.getFirstRun().map(JsonRun::new).orElse(null);
		second = commitComparison.getSecondRun().map(JsonRun::new).orElse(null);

		differences = commitComparison.getDifferences().stream()
			.map(JsonDifference::new)
			.collect(Collectors.toUnmodifiableList());
	}

	@Nullable
	public JsonRun getFirst() {
		return first;
	}

	@Nullable
	public JsonRun getSecond() {
		return second;
	}

	public Collection<JsonDifference> getDifferences() {
		return differences;
	}

}
