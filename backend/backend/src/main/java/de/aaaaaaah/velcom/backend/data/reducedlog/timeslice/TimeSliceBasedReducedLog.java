package de.aaaaaaah.velcom.backend.data.reducedlog.timeslice;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.data.reducedlog.ReducedLog;
import de.aaaaaaah.velcom.backend.util.Either;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This reduction strategy groups the commits into slices of time (e.g. hours or days) and selects
 * the best performing commit for each time slice. Drops all commits that don't have a successful
 * measurement of the specified name.
 */
public class TimeSliceBasedReducedLog implements ReducedLog {

	private final BenchmarkAccess benchmarkAccess;
	private final CommitGrouper<Long> commitGrouper;

	public TimeSliceBasedReducedLog(BenchmarkAccess benchmarkAccess,
		CommitGrouper<Long> commitGrouper) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitGrouper = commitGrouper;
	}

	private Optional<Measurement> findMeasurementOfName(Collection<Measurement> measurements,
		MeasurementName measurementName) {

		for (Measurement measurement : measurements) {
			if (measurement.getMeasurementName().equals(measurementName)) {
				return Optional.of(measurement);
			}
		}
		return Optional.empty();
	}

	public Optional<Run> getBestRunOf(List<Run> runs, MeasurementName measurementName) {
		double maxValue = 0;
		int indexOfMaxValue = -1;
		Optional<Run> runWithMaxValue = Optional.empty();

		for (int i = 0; i < runs.size(); i++) {
			final Run run = runs.get(i);
			// Hey, we can make a monad out of this :P
			final Optional<MeasurementValues> maybeValues = run.getMeasurements()
				.flatMap(measurements -> findMeasurementOfName(measurements, measurementName))
				.map(Measurement::getContent)
				.flatMap(Either::getRight);
			if (maybeValues.isEmpty()) {
				continue;
			}
			final MeasurementValues values = maybeValues.get();

			boolean indexIsNegative = indexOfMaxValue < 0;
			boolean moreIsBetter = values.getInterpretation().equals(Interpretation.MORE_IS_BETTER);
			boolean interpretationApplies = moreIsBetter
				? values.getValue() > maxValue
				: values.getValue() < maxValue;

			if (indexIsNegative || interpretationApplies) {
				indexOfMaxValue = i;
				maxValue = values.getValue();
				runWithMaxValue = Optional.of(run);
			}
		}

		return runWithMaxValue;
	}

	@Override
	public List<Run> reduce(Collection<Commit> originalCommits, MeasurementName measurementName) {
		final Map<Long, List<Commit>> grouped = originalCommits.stream()
			.collect(Collectors.groupingBy(commit -> commitGrouper.getGroup(
				commit.getAuthorDate().atZone(ZoneOffset.UTC)
			)));

		return grouped.keySet().stream()
			.sorted()
			.map(grouped::get)
			.map(commits -> commits.stream()
				.map(benchmarkAccess::getLatestRunOf)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toUnmodifiableList())
			)
			.map(commits -> getBestRunOf(commits, measurementName))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toUnmodifiableList());
	}

}
