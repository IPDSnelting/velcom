package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;
import static org.jooq.codegen.db.tables.Repository.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunBuilder;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BenchmarkAccessTest {

	static final RepoId[] REPO_IDS = {new RepoId(), new RepoId()};
	static final MeasurementName[][] MEASUREMENTS = {
		{ // Measurements for first repo:
			new MeasurementName("backend", "buildtime"),
			new MeasurementName("frontend", "buildtime")
		},
		{ // Measurements for second repo:
			new MeasurementName("backend", "buildtime"),
			new MeasurementName("frontend", "buildtime"),
			new MeasurementName("runner", "buildtime")
		},
	};
	static final int RUN_COUNT_PER_REPO = 6;
	static final int TOTAL_RUN_COUNT = RUN_COUNT_PER_REPO * REPO_IDS.length;

	@TempDir
	Path testDir;
	DatabaseStorage dbStorage;
	RepoReadAccess repoAccess;
	BenchmarkWriteAccess benchmarkAccess;

	// All run lists will be ordered from most recent to least recent
	List<Run> allRuns = new ArrayList<>();
	Map<Integer, List<Run>> repoRuns = new HashMap<>();

	List<CommitHash> allHashes = new ArrayList<>();
	Map<Integer, List<CommitHash>> repoHashes = new HashMap<>();

	@BeforeEach
	void setUp() throws SQLException {
		dbStorage = new DatabaseStorage("jdbc:sqlite:file:" + testDir.resolve("data.db"));

		repoAccess = mock(RepoReadAccess.class);
		when(repoAccess.getAllRepoIds()).thenReturn(List.of(REPO_IDS));

		benchmarkAccess = new BenchmarkWriteAccess(dbStorage, repoAccess);

		Instant time = Instant.now();

		// First of all, prepare the repositories
		for (int repoIndex = 0; repoIndex < REPO_IDS.length; repoIndex++) {
			// Prepare repo specific maps
			repoRuns.put(repoIndex, new ArrayList<>());
			repoHashes.put(repoIndex, new ArrayList<>());

			// Insert repo into database
			try (DSLContext db = dbStorage.acquireContext()) {
				RepositoryRecord record = db.newRecord(REPOSITORY);
				record.setId(REPO_IDS[repoIndex].getId().toString());
				record.setName("Bla");
				record.setRemoteUrl("bla");
				record.insert();
			}
		}

		// Now, generate runs (with each repository taking turns)
		for (int runIndex = 0; runIndex < TOTAL_RUN_COUNT; runIndex++) {
			int repoIndex = runIndex % REPO_IDS.length;

			// Create fake commit and insert into db
			CommitHash hash = new CommitHash(UUID.randomUUID().toString());
			repoHashes.get(repoIndex).add(hash);
			allHashes.add(hash);

			try (DSLContext db = dbStorage.acquireContext()) {
				KnownCommitRecord commitRecord = db.newRecord(KNOWN_COMMIT);
				commitRecord.setRepoId(REPO_IDS[repoIndex].getId().toString());
				commitRecord.setHash(hash.getHash());
				commitRecord.setStatus(0);
				commitRecord.setUpdateTime(Timestamp.from(Instant.now()));
				commitRecord.setInsertTime(Timestamp.from(Instant.now()));
				commitRecord.insert();
			}

			// Now create fake run
			Instant startTime = time;
			Instant stopTime = time.plus(Duration.ofMinutes(5));
			time = time.minus(Duration.ofMinutes(10));
			// ^ go back in time so that first run in list will be most recent

			RunBuilder runBuilder = RunBuilder.successful(
				REPO_IDS[repoIndex], hash, startTime, stopTime
			);

			for (MeasurementName mName : MEASUREMENTS[repoIndex]) {
				runBuilder.addSuccessfulMeasurement(
					mName,
					Interpretation.LESS_IS_BETTER,
					new Unit("cm"),
					List.of(0.0, 1.0, 1.0, 2.0, 3.0, 5.0, 8.0, 13.0, 1.0, 34.0)
				);
			}

			Run run = runBuilder.build();

			allRuns.add(run);
			repoRuns.get(repoIndex).add(run);
		}
	}

	@AfterEach
	void breakDown() {
		if (dbStorage != null) {
			dbStorage.close();
		}
	}

	@Test
	public void testInserts() {
		assertTrue(benchmarkAccess.getRecentRuns(0, TOTAL_RUN_COUNT).isEmpty());

		for (int i = 0; i < TOTAL_RUN_COUNT; i++) {
			Run run = allRuns.get(i);

			benchmarkAccess.insertRun(run);

			Run sameRun = benchmarkAccess.getLatestRun(run.getRepoId(), run.getCommitHash())
				.orElseThrow();

			assertRunEquals(run, sameRun);

			List<Run> recentRuns = benchmarkAccess.getRecentRuns(0, TOTAL_RUN_COUNT);
			List<Run> insertedRunsSoFar = allRuns.subList(0, i + 1);
			assertEquals(insertedRunsSoFar.size(), recentRuns.size());

			// Before doing a deep equals test, just check the run ids
			List<RunId> recentRunIds = recentRuns.stream().map(Run::getId).collect(toList());
			List<RunId> insertedIds = insertedRunsSoFar.stream().map(Run::getId).collect(toList());
			assertThat(recentRunIds).containsExactlyElementsOf(insertedIds);

			// Do deep equals test
			for (int runIndex = 0; runIndex < recentRuns.size(); runIndex++) {
				assertRunEquals(insertedRunsSoFar.get(runIndex), recentRuns.get(runIndex));
			}
		}
	}

	@Test
	public void testGetLatestRuns() {
		allRuns.forEach(benchmarkAccess::insertRun);

		RepoId repoId = REPO_IDS[0];
		List<CommitHash> repoHashList = this.repoHashes.get(0);

		Map<CommitHash, Run> resultMap = benchmarkAccess.getLatestRuns(repoId, allHashes);

		// Check that commit hashes match
		assertThat(resultMap.keySet()).containsExactlyInAnyOrderElementsOf(repoHashList);

		// Check that commit hashes are mapped to the right run instances
		// (and that each run is from correct repo while we're at it)
		resultMap.forEach((hash, run) -> {
			assertEquals(repoId, run.getRepoId());
			assertEquals(hash, run.getCommitHash());
		});
	}

	@Test
	public void testGetAvailableMeasurements() {
		allRuns.forEach(benchmarkAccess::insertRun);

		Collection<MeasurementName> result = benchmarkAccess.getAvailableMeasurements(REPO_IDS[0]);

		assertThat(result).containsExactlyInAnyOrder(MEASUREMENTS[0]);

		result = benchmarkAccess.getAvailableMeasurements(REPO_IDS[1]);

		assertThat(result).containsExactlyInAnyOrder(MEASUREMENTS[1]);
	}

	@Test
	public void testDeleteAllMeasurementsOfName() {
		allRuns.forEach(benchmarkAccess::insertRun);

		RepoId repoId = REPO_IDS[0];
		List<CommitHash> hashList = repoHashes.get(0);
		MeasurementName mName = MEASUREMENTS[0][0];

		benchmarkAccess.deleteAllMeasurementsOfName(repoId, mName);

		// Get updated runs from access and check if any of the runs still contains the mName
		Map<CommitHash, Run> runMap = benchmarkAccess.getLatestRuns(repoId, hashList);

		for (Run run : runMap.values()) {
			if (run.getMeasurements().isEmpty()) {
				continue;
			}

			Collection<Measurement> measurements = run.getMeasurements().get();

			List<MeasurementName> mNames = measurements.stream()
				.map(Measurement::getMeasurementName)
				.collect(toList());

			assertThat(mNames).doesNotContain(mName);
		}

		// Check getAvailableMeasurements
		Collection<MeasurementName> available = benchmarkAccess.getAvailableMeasurements(repoId);
		assertThat(available).doesNotContain(mName);

		// Check that the other repository still has backend|buildtime as measurement
		RepoId otherRepo = REPO_IDS[1];
		List<CommitHash> otherHashes = repoHashes.get(1);

		runMap = benchmarkAccess.getLatestRuns(otherRepo, otherHashes);

		for (Run run : runMap.values()) {
			if (run.getMeasurements().isEmpty()) {
				continue;
			}

			Collection<Measurement> measurements = run.getMeasurements().get();

			List<MeasurementName> mNames = measurements.stream()
				.map(Measurement::getMeasurementName)
				.collect(toList());

			assertThat(mNames).contains(mName);
		}

		available = benchmarkAccess.getAvailableMeasurements(otherRepo);
		assertThat(available).contains(mName);
	}

	private static void assertRunEquals(Run first, Run second) {
		assertEquals(first.getId(), second.getId());
		assertEquals(first.getRepoId(), second.getRepoId());
		assertEquals(first.getCommitHash(), second.getCommitHash());
		//assertEquals(first.getStartTime(), second.getStartTime()); time coming from db is a bit
		//assertEquals(first.getStopTime(), second.getStopTime());   less precise :(
		assertEquals(first.getErrorMessage(), second.getErrorMessage());
		assertEquals(first.getMeasurements().isPresent(), second.getMeasurements().isPresent());

		if (first.getMeasurements().isPresent()) {
			Collection<Measurement> firstMeasures = first.getMeasurements().get();
			Collection<Measurement> secondMeasures = second.getMeasurements().get();

			assertEquals(firstMeasures.size(), secondMeasures.size());

			Iterator<Measurement> firstIterator = firstMeasures.iterator();
			Iterator<Measurement> secondIterator = secondMeasures.iterator();

			while (firstIterator.hasNext()) {
				Measurement firstM = firstIterator.next();
				Measurement secondM = secondIterator.next();

				assertEquals(firstM.getRunId(), secondM.getRunId());
				assertEquals(firstM.getMeasurementName(), secondM.getMeasurementName());
				assertEquals(firstM.getContent(), secondM.getContent());
			}
		}
	}

}
