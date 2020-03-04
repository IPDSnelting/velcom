package de.aaaaaaah.velcom.backend.data.commitcomparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommitComparerTest {

	private Commit firstCommit;
	private Run firstRun;
	private Commit secondCommit;
	private Run secondRun;

	private Measurement successfulMeasurement1;
	private Measurement successfulMeasurement2;
	private Measurement successfulMeasurement3;
	private Measurement failedMeasurement2;
	private Measurement failedMeasurement3;

	private CommitComparer comparer;

	@BeforeEach
	void setup() {
		firstCommit = mock(Commit.class);
		firstRun = mock(Run.class);
		secondCommit = mock(Commit.class);
		secondRun = mock(Run.class);

		successfulMeasurement1 = mock(Measurement.class);
		when(successfulMeasurement1.getMeasurementName()).thenReturn(
			new MeasurementName("bench1", "metric1"));
		when(successfulMeasurement1.getContent()).thenReturn(Either.ofRight(
			new MeasurementValues(List.of(1d, 2d, 3d), new Unit("s"),
				Interpretation.LESS_IS_BETTER)));

		successfulMeasurement2 = mock(Measurement.class);
		when(successfulMeasurement2.getMeasurementName()).thenReturn(
			new MeasurementName("bench1", "metric2"));
		when(successfulMeasurement2.getContent()).thenReturn(Either.ofRight(
			new MeasurementValues(List.of(4d, 5d, 6d, 7d, 8d), new Unit("m"),
				Interpretation.MORE_IS_BETTER)));

		successfulMeasurement3 = mock(Measurement.class);
		when(successfulMeasurement3.getMeasurementName()).thenReturn(
			new MeasurementName("bench2", "metric1"));
		when(successfulMeasurement3.getContent()).thenReturn(Either.ofRight(
			new MeasurementValues(List.of(3d, -7d), new Unit("s"), Interpretation.LESS_IS_BETTER)));

		failedMeasurement2 = mock(Measurement.class);
		when(failedMeasurement2.getMeasurementName()).thenReturn(
			new MeasurementName("bench1", "metric2"));
		when(failedMeasurement2.getContent()).thenReturn(
			Either.ofLeft(new MeasurementError("measurement failed successfully")));

		failedMeasurement3 = mock(Measurement.class);
		when(failedMeasurement3.getMeasurementName()).thenReturn(
			new MeasurementName("bench2", "metric1"));
		when(failedMeasurement3.getContent()).thenReturn(
			Either.ofLeft(new MeasurementError("a rhombus is not a circle")));

		comparer = new CommitComparer(0.05);
	}

	@Test
	void mustHaveASecondCommit() {
		assertThrows(NullPointerException.class,
			() -> comparer.compare(firstCommit, firstRun, null, secondRun));
	}

	@Test
	void compareAsLittleAsPossible() {
		CommitComparison comparison = comparer.compare(null, null, secondCommit, null);

		assertEquals(Optional.empty(), comparison.getFirstCommit());
		assertEquals(Optional.empty(), comparison.getFirstRun());
		assertEquals(secondCommit, comparison.getSecondCommit());
		assertEquals(Optional.empty(), comparison.getSecondRun());

		assertTrue(comparison.getDifferences().isEmpty());
		assertFalse(comparison.isSignificant());
	}

	@Test
	void compareRunsWithMeasurements() {
		when(firstRun.getMeasurements()).thenReturn(
			Optional.of(
				List.of(successfulMeasurement1, successfulMeasurement2, failedMeasurement3)));
		when(secondRun.getMeasurements()).thenReturn(
			Optional.of(
				List.of(failedMeasurement2, successfulMeasurement1, successfulMeasurement3)));

		CommitComparison comparison = comparer.compare(firstCommit, firstRun, secondCommit,
			secondRun);

		assertEquals(1, comparison.getDifferences().size());
		CommitDifference difference = new ArrayList<>(comparison.getDifferences()).get(0);
		assertEquals(new MeasurementName("bench1", "metric1"), difference.getMeasurementName());
		assertEquals(2d, difference.getFirst(), 0.00001);
		assertEquals(2d, difference.getSecond(), 0.00001);
		assertEquals(0d, difference.getDifference(), 0.00001);
		assertFalse(comparison.isSignificant());
	}

}