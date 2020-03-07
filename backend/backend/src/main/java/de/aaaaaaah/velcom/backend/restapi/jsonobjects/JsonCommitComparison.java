package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import java.util.Collection;
import java.util.Optional;
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
		final Optional<Commit> maybeFirstCommit = commitComparison.getFirstCommit();

		if (maybeFirstCommit.isPresent()) {
			firstCommit = new JsonCommit(maybeFirstCommit.get());
			firstRun = commitComparison.getFirstRun()
				.map(run -> new JsonRun(run, maybeFirstCommit.get()))
				.orElse(null);
		} else {
			firstCommit = null;
			// There can't be a run without a commit, so the run would be null anyways
			firstRun = null;
		}

		secondCommit = new JsonCommit(commitComparison.getSecondCommit());
		secondRun = commitComparison.getSecondRun()
			.map(run -> new JsonRun(run, commitComparison.getSecondCommit()))
			.orElse(null);

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
