package de.aaaaaaah.velcom.backend.access.benchmarkaccess;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.codegen.db.tables.Dimension.DIMENSION;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewMeasurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkWriteAccessTest {

	private static final RepoId REPO_ID = new RepoId();
	private static final CommitHash COMMIT_HASH =
		new CommitHash("4fe01fb5246edddd5b1454b96ecf597bad006666");
	private static final Dimension DIM1 = new Dimension("test", "ing");
	private static final Dimension DIM2 = new Dimension("hello", "world");

	private DatabaseStorage databaseStorage;
	private AvailableDimensionsCache availableDimensionsCache;
	private LatestRunCache latestRunCache;
	private BenchmarkWriteAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		testDb.addRepo(REPO_ID);
		testDb.addCommit(REPO_ID, COMMIT_HASH);
		testDb.addDimension(DIM1);
		testDb.addDimension(DIM2);

		databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		availableDimensionsCache = mock(AvailableDimensionsCache.class);
		latestRunCache = mock(LatestRunCache.class);
		access = new BenchmarkWriteAccess(databaseStorage, availableDimensionsCache, latestRunCache);
	}

	@Test
	void insertTarRun() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofRight(new TarSource("description", null)),
			new RunError("errorMessage", RunErrorType.VELCOM_ERROR));

		access.insertRun(newRun);

		Run run = access.getRun(runId);
		assertThat(run.getId()).isEqualTo(runId);
		assertThat(run.getAuthor()).isEqualTo("author");
		assertThat(run.getRunnerName()).isEqualTo("runnerName");
		assertThat(run.getRunnerInfo()).isEqualTo("runnerInfo");
		assertThat(run.getStartTime()).isEqualTo(startTime);
		assertThat(run.getStopTime()).isEqualTo(stopTime);
		assertThat(run.getSource()).isEqualTo(Either.ofRight(new TarSource("description", null)));
		assertThat(run.getRepoId()).isEqualTo(Optional.empty());
		assertThat(run.getResult())
			.isEqualTo(Either.ofLeft(new RunError("errorMessage", RunErrorType.VELCOM_ERROR)));
	}

	@Test
	void insertTarRunAttachedToRepo() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofRight(new TarSource("description", REPO_ID)),
			new RunError("errorMessage", RunErrorType.VELCOM_ERROR));

		access.insertRun(newRun);

		Run run = access.getRun(runId);
		assertThat(run.getId()).isEqualTo(runId);
		assertThat(run.getAuthor()).isEqualTo("author");
		assertThat(run.getRunnerName()).isEqualTo("runnerName");
		assertThat(run.getRunnerInfo()).isEqualTo("runnerInfo");
		assertThat(run.getStartTime()).isEqualTo(startTime);
		assertThat(run.getStopTime()).isEqualTo(stopTime);
		assertThat(run.getSource()).isEqualTo(Either.ofRight(new TarSource("description", REPO_ID)));
		assertThat(run.getRepoId()).isEqualTo(Optional.of(REPO_ID));
		assertThat(run.getResult())
			.isEqualTo(Either.ofLeft(new RunError("errorMessage", RunErrorType.VELCOM_ERROR)));
	}

	@Test
	void insertCommitRun() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)),
			new RunError("errorMessage", RunErrorType.VELCOM_ERROR));

		access.insertRun(newRun);

		Run run = access.getRun(runId);
		assertThat(run.getId()).isEqualTo(runId);
		assertThat(run.getAuthor()).isEqualTo("author");
		assertThat(run.getRunnerName()).isEqualTo("runnerName");
		assertThat(run.getRunnerInfo()).isEqualTo("runnerInfo");
		assertThat(run.getStartTime()).isEqualTo(startTime);
		assertThat(run.getStopTime()).isEqualTo(stopTime);
		assertThat(run.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)));
		assertThat(run.getRepoId()).isEqualTo(Optional.of(REPO_ID));
		assertThat(run.getResult())
			.isEqualTo(Either.ofLeft(new RunError("errorMessage", RunErrorType.VELCOM_ERROR)));
	}

	@Test
	void insertSuccessfulRun() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)), List.of(
			new NewMeasurement(runId, DIM1, null, null, new MeasurementValues(List.of(1d, 2d, 3d))),
			new NewMeasurement(runId, DIM2, null, null, new MeasurementValues(List.of(4d)))
		));

		access.insertRun(newRun);

		Run run = access.getRun(runId);
		assertThat(run.getId()).isEqualTo(runId);
		assertThat(run.getAuthor()).isEqualTo("author");
		assertThat(run.getRunnerName()).isEqualTo("runnerName");
		assertThat(run.getRunnerInfo()).isEqualTo("runnerInfo");
		assertThat(run.getStartTime()).isEqualTo(startTime);
		assertThat(run.getStopTime()).isEqualTo(stopTime);
		assertThat(run.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)));
		assertThat(run.getRepoId()).isEqualTo(Optional.of(REPO_ID));
		assertThat(run.getResult().getRight()).isPresent();
		assertThat(run.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(runId, DIM1, Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d)))),
			new Measurement(runId, DIM2, Either.ofRight(new MeasurementValues(List.of(4d))))
		);
	}

	@Test
	void insertPartlySuccessfulRun() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)), List.of(
			new NewMeasurement(runId, DIM1, null, null, new MeasurementValues(List.of(1d, 2d, 3d))),
			new NewMeasurement(runId, DIM2, null, null, new MeasurementError("errorMessage"))
		));

		access.insertRun(newRun);

		Run run = access.getRun(runId);
		assertThat(run.getId()).isEqualTo(runId);
		assertThat(run.getAuthor()).isEqualTo("author");
		assertThat(run.getRunnerName()).isEqualTo("runnerName");
		assertThat(run.getRunnerInfo()).isEqualTo("runnerInfo");
		assertThat(run.getStartTime()).isEqualTo(startTime);
		assertThat(run.getStopTime()).isEqualTo(stopTime);
		assertThat(run.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)));
		assertThat(run.getRepoId()).isEqualTo(Optional.of(REPO_ID));
		assertThat(run.getResult().getRight()).isPresent();
		assertThat(run.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(runId, DIM1, Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d)))),
			new Measurement(runId, DIM2, Either.ofLeft(new MeasurementError("errorMessage")))
		);
	}

	@Test
	void insertingRunUpdatesDimensions() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);
		Dimension dim3 = new Dimension("a", "b");
		Dimension dim4 = new Dimension("c", "d");

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)), List.of(
			new NewMeasurement(runId, DIM1, new Unit("asdf"), null,
				new MeasurementValues(List.of(1d, 2d, 3d))),
			new NewMeasurement(runId, DIM2, null, Interpretation.MORE_IS_BETTER,
				new MeasurementError("errorMessage")),
			new NewMeasurement(runId, dim3, null, null, new MeasurementValues(List.of(4d))),
			new NewMeasurement(runId, dim4, new Unit("foo"), Interpretation.LESS_IS_BETTER,
				new MeasurementValues(List.of(5d)))
		));

		access.insertRun(newRun);

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Map<Dimension, DimensionRecord> dimMap = db.selectFrom(DIMENSION).stream()
				.collect(toMap(
					it -> new Dimension(it.getBenchmark(), it.getMetric()),
					it -> it
				));

			assertThat(dimMap).containsOnlyKeys(DIM1, DIM2, dim3, dim4);

			DimensionRecord dim1Record = dimMap.get(DIM1);
			assertThat(dim1Record.getUnit()).isEqualTo(new Unit("asdf").getName());
			assertThat(dim1Record.getInterpretation())
				.isEqualTo(Interpretation.DEFAULT.getTextualRepresentation());

			DimensionRecord dim2Record = dimMap.get(DIM2);
			assertThat(dim2Record.getUnit()).isEqualTo(Unit.DEFAULT.getName());
			assertThat(dim2Record.getInterpretation())
				.isEqualTo(Interpretation.MORE_IS_BETTER.getTextualRepresentation());

			DimensionRecord dim3Record = dimMap.get(dim3);
			assertThat(dim3Record.getUnit()).isEqualTo(Unit.DEFAULT.getName());
			assertThat(dim3Record.getInterpretation())
				.isEqualTo(Interpretation.DEFAULT.getTextualRepresentation());

			DimensionRecord dim4Record = dimMap.get(dim4);
			assertThat(dim4Record.getUnit()).isEqualTo(new Unit("foo").getName());
			assertThat(dim4Record.getInterpretation())
				.isEqualTo(Interpretation.LESS_IS_BETTER.getTextualRepresentation());
		}
	}

	@Test
	void insertingRunInvalidatesCaches() {
		RunId runId = new RunId();
		Instant startTime = Instant.ofEpochSecond(1600010001);
		Instant stopTime = Instant.ofEpochSecond(1600010006);

		NewRun newRun = new NewRun(runId, "author", "runnerName", "runnerInfo", startTime, stopTime,
			Either.ofLeft(new CommitSource(REPO_ID, COMMIT_HASH)),
			new RunError("errorMessage", RunErrorType.VELCOM_ERROR));

		access.insertRun(newRun);

		verify(availableDimensionsCache, atLeastOnce()).invalidate(REPO_ID);
		verify(latestRunCache, atLeastOnce()).invalidate(REPO_ID, COMMIT_HASH);
	}
}
