package de.aaaaaaah.velcom.backend.data.runcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RunComparison {

	private final Run first;
	private final Run second;
	private final List<DimensionDifference> differences;

	public RunComparison(Run first, Run second, List<DimensionDifference> differences) {
		this.first = first;
		this.second = second;
		this.differences = differences;
	}

	public Run getFirst() {
		return first;
	}

	public Run getSecond() {
		return second;
	}

	public List<DimensionDifference> getDifferences() {
		return differences;
	}

	public Set<Dimension> getDimensions() {
		return differences.stream()
			.map(DimensionDifference::getDimension)
			.collect(Collectors.toSet());
	}
}