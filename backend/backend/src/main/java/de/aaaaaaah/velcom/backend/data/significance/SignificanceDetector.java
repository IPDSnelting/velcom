package de.aaaaaaah.velcom.backend.data.significance;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SignificanceDetector {

	private final SignificanceFactors significanceFactors;
	private final RunComparator runComparator;

	public SignificanceDetector(SignificanceFactors significanceFactors,
		RunComparator runComparator) {

		this.significanceFactors = significanceFactors;
		this.runComparator = runComparator;
	}

	/**
	 * Check whether a run is significant when compared to multiple other runs.
	 *
	 * @param run the run whose significance to determine
	 * @param comparedTo the other runs to compare against
	 * @param significantDimensions the currently significant dimensions
	 * @return a {@link SignificanceReasons} instance detailing why the run is significant, or {@code
	 * 	Optional.empty()} if the run is not significant.
	 */
	public Optional<SignificanceReasons> getSignificance(Run run, Collection<Run> comparedTo,
		Set<Dimension> significantDimensions) {

		if (run.getResult().getRight().isEmpty()) {
			return Optional.of(new SignificanceReasons(List.of(), List.of(), true));
		}

		List<Dimension> significantFailedDimensions = run.getResult()
			.getRight()
			.get()
			.stream()
			.filter(measurement -> measurement.getContent().isLeft())
			.map(Measurement::getDimension)
			.filter(significantDimensions::contains)
			.sorted()
			.collect(toList());

		List<DimensionDifference> significantDifferences = comparedTo.stream()
			.map(first -> runComparator.compare(first, run))
			.flatMap(comparison -> comparison.getDifferences().stream())
			.filter(difference -> significantDimensions.contains(difference.getDimension()))
			.filter(this::isSignificantDifference)
			.sorted(Comparator.comparing(DimensionDifference::getDimension))
			.collect(toList());

		if (!significantDifferences.isEmpty() || !significantFailedDimensions.isEmpty()) {
			return Optional
				.of(new SignificanceReasons(significantDifferences, significantFailedDimensions, false));
		} else {
			return Optional.empty();
		}
	}

	private boolean isSignificantDifference(DimensionDifference diff) {
		if (diff.getDimension().getBenchmark().startsWith("~"))
			// 1B instructions
			return Math.abs(diff.getDiff()) >= 10_000_000_000L;

		boolean relSignificant = diff.getReldiff()
			.map(reldiff -> Math.abs(reldiff) >= significanceFactors.getRelativeThreshold())
			// There is no reldiff if the first value is 0. But if the second value is also zero, that
			// hardly constitutes a significant difference. Otherwise, it is a move away from 0, which is
			// always significant.
			.orElse(diff.getFirst() != diff.getSecond());

		boolean stddevSignificant = diff.getSecondStddev()
			.map(stddev -> Math.abs(diff.getDiff()) >= significanceFactors.getStddevThreshold() * stddev)
			// If there is no stddev, this check should not prevent differences from being significant
			.orElse(true);

		return relSignificant && stddevSignificant;
	}
}
