package de.aaaaaaah.velcom.backend.access.benchmarkaccess;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkReadAccessTest {

	/*
	 * This test db setup is at follows:
	 *
	 * - Run 1 (Tar)
	 * - Run 2 (Tar, bench error)
	 * - Repo 1
	 *   - Run 3 (Tar)
	 *   - Run 4 (Tar, velcom error)
	 *   - Commit 1
	 *     - Run 5
	 *     - Run 6
	 *     - Run 7 (bench error)
	 *   - Commit 2
	 *     - Run 8
	 *   - Commit 3
	 * - Repo 2
	 *   - Run 9 (Tar)
	 *   - Commit 4
	 *     - Run 10
	 *
	 * The runs occurred in the order 1, 5, 6, 3, 9, 10, 4, 8, 2, 7
	 * Each task is as many seconds long as its number.
	 *
	 * Run 5 contains successful measurements for the dimensions hello.world and test.this.
	 * Run 6 contains a successful measurement for hello.world and a failed measurement for test.this.
	 * Run 7 contains a bench script error.
	 * Run 4 contains a velcom error.
	 *
	 * That setup should ensure that a wide variety of edge cases can be covered.
	 */

	private static final RepoId REPO1_ID = new RepoId();
	private static final RepoId REPO2_ID = new RepoId();

	private static final CommitHash COMMIT1_HASH =
		new CommitHash("bc42262ad5c504587bc4ccecab9f3f701a474047");
	private static final CommitHash COMMIT2_HASH =
		new CommitHash("809170709d5f73cb273d5512a10895c10d6dc539");
	private static final CommitHash COMMIT3_HASH =
		new CommitHash("cf3b1b977b01f255c6b79aee55f1d0bb77f8417b");
	private static final CommitHash COMMIT4_HASH =
		new CommitHash("2aa75b3d92046e3d9c080ee8397cd3fbafea0021");

	private static final RunId RUN1_ID = RunId.fromString("99ff038a-cf26-4a5f-8d0f-5bc9673e6e19");
	private static final RunId RUN2_ID = RunId.fromString("1862ce3a-0eb2-45ee-8793-e017baee0888");
	private static final RunId RUN3_ID = RunId.fromString("0fca4c5b-97c1-422e-acff-365cb0c9a608");
	private static final RunId RUN4_ID = RunId.fromString("577a7d6e-e38f-4123-a7c3-e1e5255019af");
	private static final RunId RUN5_ID = RunId.fromString("5414e886-3196-44ee-b80b-9e3146e9e903");
	private static final RunId RUN6_ID = RunId.fromString("2de7c555-6327-4b50-a826-6c26ebd8cfc3");
	private static final RunId RUN7_ID = RunId.fromString("4b5e1f15-770e-4c06-8828-14a7080346d2");
	private static final RunId RUN8_ID = RunId.fromString("e19a269b-c664-4313-a592-b20de69bccb1");
	private static final RunId RUN9_ID = RunId.fromString("6726d82b-1012-43e3-b2e1-1a1dd56915d1");
	private static final RunId RUN10_ID = RunId.fromString("d505ff5f-7591-4208-a491-ee807621ddd7");
	private static final Instant RUN1_START = Instant.ofEpochSecond(1600010001);
	private static final Instant RUN5_START = Instant.ofEpochSecond(1600020001);
	private static final Instant RUN6_START = Instant.ofEpochSecond(1600030001);
	private static final Instant RUN3_START = Instant.ofEpochSecond(1600040001);
	private static final Instant RUN9_START = Instant.ofEpochSecond(1600050001);
	private static final Instant RUN10_START = Instant.ofEpochSecond(1600060001);
	private static final Instant RUN4_START = Instant.ofEpochSecond(1600070001);
	private static final Instant RUN8_START = Instant.ofEpochSecond(1600080001);
	private static final Instant RUN2_START = Instant.ofEpochSecond(1600090001);
	private static final Instant RUN7_START = Instant.ofEpochSecond(1600100001);

	private static final Dimension DIM_HW = new Dimension("hello", "world");
	private static final Dimension DIM_TT = new Dimension("test", "this");

	private BenchmarkReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		testDb.addRepo(REPO1_ID);
		testDb.addRepo(REPO2_ID);
		testDb.addCommit(REPO1_ID, COMMIT1_HASH, true, true, true, "blad9bla",
			Instant.ofEpochSecond(1), Instant.ofEpochSecond(1));
		testDb.addCommit(REPO1_ID, COMMIT2_HASH, true, true, true, "m2",
			Instant.ofEpochSecond(2), Instant.ofEpochSecond(2));
		testDb.addCommit(REPO1_ID, COMMIT3_HASH, true, true, true, "m3",
			Instant.ofEpochSecond(3), Instant.ofEpochSecond(3));
		testDb.addCommit(REPO2_ID, COMMIT4_HASH, true, true, true, "m4",
			Instant.ofEpochSecond(4), Instant.ofEpochSecond(4));

		testDb.addRun(RUN1_ID, "a1", "rn1", "ri1", RUN1_START, RUN1_START.plusSeconds(1),
			Either.ofRight(new TarSource("td1", null)), null);
		testDb.addRun(RUN2_ID, "a2", "rn2", "ri2", RUN2_START, RUN2_START.plusSeconds(2),
			Either.ofRight(new TarSource("td2", null)),
			new RunError("em2", RunErrorType.BENCH_SCRIPT_ERROR));
		testDb.addRun(RUN3_ID, "a3", "rn3", "ri3", RUN3_START, RUN3_START.plusSeconds(3),
			Either.ofRight(new TarSource("td3", REPO1_ID)), null);
		testDb.addRun(RUN4_ID, "a4", "rn4", "ri4", RUN4_START, RUN4_START.plusSeconds(4),
			Either.ofRight(new TarSource("td4", REPO1_ID)),
			new RunError("em4", RunErrorType.VELCOM_ERROR));
		testDb.addRun(RUN5_ID, "a5", "rn5", "ri5", RUN5_START, RUN5_START.plusSeconds(5),
			Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)), null);
		testDb.addRun(RUN6_ID, "a6", "rn6", "ri6", RUN6_START, RUN6_START.plusSeconds(6),
			Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)), null);
		testDb.addRun(RUN7_ID, "a7", "rn7", "ri7", RUN7_START, RUN7_START.plusSeconds(7),
			Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)),
			new RunError("em7", RunErrorType.BENCH_SCRIPT_ERROR));
		testDb.addRun(RUN8_ID, "a8", "rn8", "ri8", RUN8_START, RUN8_START.plusSeconds(8),
			Either.ofLeft(new CommitSource(REPO1_ID, COMMIT2_HASH)), null);
		testDb.addRun(RUN9_ID, "a9", "rn9", "ri9", RUN9_START, RUN9_START.plusSeconds(9),
			Either.ofRight(new TarSource("td9", REPO2_ID)), null);
		testDb.addRun(RUN10_ID, "a10", "rn10", "ri10", RUN10_START, RUN10_START.plusSeconds(10),
			Either.ofLeft(new CommitSource(REPO2_ID, COMMIT4_HASH)), null);

		// Required for foreign key constraints
		testDb.addDimension(DIM_HW);
		testDb.addDimension(DIM_TT);

		testDb.addMeasurement(RUN5_ID, DIM_HW, null, Interpretation.LESS_IS_BETTER,
			Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d, 4d, 5d))));
		testDb.addMeasurement(RUN5_ID, DIM_TT, new Unit("asdf"), null,
			Either.ofRight(new MeasurementValues(List.of(6d, 7d, 8d, 9d, 10d, 11d, 12d, 13d, 14d, 15d))));
		testDb.addMeasurement(RUN6_ID, DIM_HW, null, null,
			Either.ofRight(new MeasurementValues(List.of(16d))));
		testDb.addMeasurement(RUN6_ID, DIM_TT, new Unit("xyz"), Interpretation.MORE_IS_BETTER,
			Either.ofLeft(new MeasurementError("blargh")));

		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		access = new BenchmarkReadAccess(databaseStorage);
	}

	@Test
	void getAllRunIds() {
		assertThat(access.getAllRunIds(REPO1_ID, COMMIT1_HASH))
			.containsExactlyInAnyOrder(RUN5_ID, RUN6_ID, RUN7_ID);
		assertThat(access.getAllRunIds(REPO1_ID, COMMIT2_HASH)).containsExactlyInAnyOrder(RUN8_ID);
		assertThat(access.getAllRunIds(REPO1_ID, COMMIT3_HASH)).isEmpty();
		assertThat(access.getAllRunIds(REPO2_ID, COMMIT4_HASH)).containsExactlyInAnyOrder(RUN10_ID);

		assertThat(access.getAllRunIds(REPO1_ID, COMMIT4_HASH)).isEmpty();
		assertThat(access.getAllRunIds(REPO2_ID, COMMIT1_HASH)).isEmpty();
	}

	@Test
	void getRecentRuns() {
		// Run order from old to new: 1, 5, 6, 3, 9, 10, 4, 8, 2, 7
		// Run order from new to old: 7, 2, 8, 4, 10, 9, 3, 6, 5, 1
		// Excluding tar runs: 7, 8, 10, 6, 5
		// Grouped by commits: 10, 8, 7, 6, 5

		assertThat(access.getRecentRunIds(0, 10))
			.containsExactly(RUN10_ID, RUN8_ID, RUN7_ID, RUN6_ID, RUN5_ID);

		// Get varying amounts at offset 0

		assertThat(access.getRecentRunIds(0, 20))
			.containsExactly(RUN10_ID, RUN8_ID, RUN7_ID, RUN6_ID, RUN5_ID);
		assertThat(access.getRecentRunIds(0, 3))
			.containsExactly(RUN10_ID, RUN8_ID, RUN7_ID);
		assertThat(access.getRecentRunIds(0, 1)).containsExactly(RUN10_ID);
		assertThat(access.getRecentRunIds(0, 0)).isEmpty();

		// Get varying amounts at varying offsets

		assertThat(access.getRecentRunIds(2, 4)).containsExactly(RUN7_ID, RUN6_ID, RUN5_ID);
		assertThat(access.getRecentRunIds(4, 3)).containsExactly(RUN5_ID);
		assertThat(access.getRecentRunIds(5, 3)).isEmpty();

		// Illegal arguments

		assertThatThrownBy(() -> access.getRecentRunIds(-1, 10))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> access.getRecentRunIds(0, -1))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void getLatestRuns() {
		assertThat(access.getLatestRunId(REPO1_ID, COMMIT1_HASH)).isEqualTo(Optional.of(RUN7_ID));
		assertThat(access.getLatestRunId(REPO1_ID, COMMIT2_HASH)).isEqualTo(Optional.of(RUN8_ID));
		assertThat(access.getLatestRunId(REPO1_ID, COMMIT3_HASH)).isEqualTo(Optional.empty());
		assertThat(access.getLatestRunId(REPO2_ID, COMMIT4_HASH)).isEqualTo(Optional.of(RUN10_ID));

		assertThat(access.getLatestRunId(REPO2_ID, COMMIT1_HASH)).isEqualTo(Optional.empty());
		assertThat(access.getLatestRunId(REPO2_ID, COMMIT2_HASH)).isEqualTo(Optional.empty());
		assertThat(access.getLatestRunId(REPO2_ID, COMMIT3_HASH)).isEqualTo(Optional.empty());
		assertThat(access.getLatestRunId(REPO1_ID, COMMIT4_HASH)).isEqualTo(Optional.empty());

		Map<CommitHash, RunId> latestRunIds = access
			.getLatestRunIds(REPO1_ID, List.of(COMMIT1_HASH, COMMIT3_HASH, COMMIT4_HASH));

		assertThat(latestRunIds).containsOnlyKeys(COMMIT1_HASH);
		assertThat(latestRunIds.get(COMMIT1_HASH)).isEqualTo(RUN7_ID);

		latestRunIds = access
			.getLatestRunIds(REPO1_ID, List.of(COMMIT1_HASH, COMMIT2_HASH, COMMIT3_HASH));

		assertThat(latestRunIds).containsOnlyKeys(COMMIT1_HASH, COMMIT2_HASH);
		assertThat(latestRunIds.get(COMMIT1_HASH)).isEqualTo(RUN7_ID);
		assertThat(latestRunIds.get(COMMIT2_HASH)).isEqualTo(RUN8_ID);
	}

	@Test
	void getRuns() {
		Run run4 = access.getRun(RUN4_ID);
		assertThat(run4.getId()).isEqualTo(RUN4_ID);
		assertThat(run4.getAuthor()).isEqualTo("a4");
		assertThat(run4.getRunnerName()).isEqualTo("rn4");
		assertThat(run4.getRunnerInfo()).isEqualTo("ri4");
		assertThat(run4.getStartTime()).isEqualTo(RUN4_START);
		assertThat(run4.getStopTime()).isEqualTo(RUN4_START.plusSeconds(4));
		assertThat(run4.getSource()).isEqualTo(Either.ofRight(new TarSource("td4", REPO1_ID)));
		assertThat(run4.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run4.getResult())
			.isEqualTo(Either.ofLeft(new RunError("em4", RunErrorType.VELCOM_ERROR)));
		assertThat(run4.getAllDimensionsUsed()).isEmpty();

		Run run5 = access.getRun(RUN5_ID);
		assertThat(run5.getId()).isEqualTo(RUN5_ID);
		assertThat(run5.getAuthor()).isEqualTo("a5");
		assertThat(run5.getRunnerName()).isEqualTo("rn5");
		assertThat(run5.getRunnerInfo()).isEqualTo("ri5");
		assertThat(run5.getStartTime()).isEqualTo(RUN5_START);
		assertThat(run5.getStopTime()).isEqualTo(RUN5_START.plusSeconds(5));
		assertThat(run5.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run5.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run5.getResult().getRight()).isPresent();
		assertThat(run5.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(RUN5_ID, DIM_HW,
				Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d, 4d, 5d)))),
			new Measurement(RUN5_ID, DIM_TT, Either
				.ofRight(new MeasurementValues(List.of(6d, 7d, 8d, 9d, 10d, 11d, 12d, 13d, 14d, 15d))))
		);
		assertThat(run5.getAllDimensionsUsed()).containsExactlyInAnyOrder(DIM_HW, DIM_TT);

		Run run6 = access.getRun(RUN6_ID);
		assertThat(run6.getId()).isEqualTo(RUN6_ID);
		assertThat(run6.getAuthor()).isEqualTo("a6");
		assertThat(run6.getRunnerName()).isEqualTo("rn6");
		assertThat(run6.getRunnerInfo()).isEqualTo("ri6");
		assertThat(run6.getStartTime()).isEqualTo(RUN6_START);
		assertThat(run6.getStopTime()).isEqualTo(RUN6_START.plusSeconds(6));
		assertThat(run6.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run6.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run6.getResult().getRight()).isPresent();
		assertThat(run6.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(RUN6_ID, DIM_HW, Either.ofRight(new MeasurementValues(List.of(16d)))),
			new Measurement(RUN6_ID, DIM_TT, Either.ofLeft(new MeasurementError("blargh")))
		);
		assertThat(run6.getAllDimensionsUsed()).containsExactlyInAnyOrder(DIM_HW, DIM_TT);

		Run run7 = access.getRun(RUN7_ID);
		assertThat(run7.getId()).isEqualTo(RUN7_ID);
		assertThat(run7.getAuthor()).isEqualTo("a7");
		assertThat(run7.getRunnerName()).isEqualTo("rn7");
		assertThat(run7.getRunnerInfo()).isEqualTo("ri7");
		assertThat(run7.getStartTime()).isEqualTo(RUN7_START);
		assertThat(run7.getStopTime()).isEqualTo(RUN7_START.plusSeconds(7));
		assertThat(run7.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run7.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run7.getResult())
			.isEqualTo(Either.ofLeft(new RunError("em7", RunErrorType.BENCH_SCRIPT_ERROR)));
		assertThat(run7.getAllDimensionsUsed()).isEmpty();

		RunId nonexistentId = new RunId();

		assertThatThrownBy(() -> access.getRun(nonexistentId))
			.isInstanceOf(NoSuchRunException.class)
			.extracting("invalidSource")
			.isEqualTo(Either.ofLeft(nonexistentId));

		List<Run> runs = access.getRuns(List.of(RUN4_ID, RUN5_ID, RUN6_ID, RUN7_ID, nonexistentId));
		Map<RunId, Run> runMap = runs.stream()
			.collect(toMap(Run::getId, it -> it));
		assertThat(runMap).containsOnlyKeys(RUN4_ID, RUN5_ID, RUN6_ID, RUN7_ID);

		run4 = runMap.get(RUN4_ID);
		assertThat(run4.getId()).isEqualTo(RUN4_ID);
		assertThat(run4.getAuthor()).isEqualTo("a4");
		assertThat(run4.getRunnerName()).isEqualTo("rn4");
		assertThat(run4.getRunnerInfo()).isEqualTo("ri4");
		assertThat(run4.getStartTime()).isEqualTo(RUN4_START);
		assertThat(run4.getStopTime()).isEqualTo(RUN4_START.plusSeconds(4));
		assertThat(run4.getSource()).isEqualTo(Either.ofRight(new TarSource("td4", REPO1_ID)));
		assertThat(run4.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run4.getResult())
			.isEqualTo(Either.ofLeft(new RunError("em4", RunErrorType.VELCOM_ERROR)));
		assertThat(run4.getAllDimensionsUsed()).isEmpty();

		run5 = runMap.get(RUN5_ID);
		assertThat(run5.getId()).isEqualTo(RUN5_ID);
		assertThat(run5.getAuthor()).isEqualTo("a5");
		assertThat(run5.getRunnerName()).isEqualTo("rn5");
		assertThat(run5.getRunnerInfo()).isEqualTo("ri5");
		assertThat(run5.getStartTime()).isEqualTo(RUN5_START);
		assertThat(run5.getStopTime()).isEqualTo(RUN5_START.plusSeconds(5));
		assertThat(run5.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run5.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run5.getResult().getRight()).isPresent();
		assertThat(run5.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(RUN5_ID, DIM_HW,
				Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d, 4d, 5d)))),
			new Measurement(RUN5_ID, DIM_TT, Either
				.ofRight(new MeasurementValues(List.of(6d, 7d, 8d, 9d, 10d, 11d, 12d, 13d, 14d, 15d))))
		);
		assertThat(run5.getAllDimensionsUsed()).containsExactlyInAnyOrder(DIM_HW, DIM_TT);

		run6 = runMap.get(RUN6_ID);
		assertThat(run6.getId()).isEqualTo(RUN6_ID);
		assertThat(run6.getAuthor()).isEqualTo("a6");
		assertThat(run6.getRunnerName()).isEqualTo("rn6");
		assertThat(run6.getRunnerInfo()).isEqualTo("ri6");
		assertThat(run6.getStartTime()).isEqualTo(RUN6_START);
		assertThat(run6.getStopTime()).isEqualTo(RUN6_START.plusSeconds(6));
		assertThat(run6.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run6.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run6.getResult().getRight()).isPresent();
		assertThat(run6.getResult().getRight().get()).containsExactlyInAnyOrder(
			new Measurement(RUN6_ID, DIM_HW, Either.ofRight(new MeasurementValues(List.of(16d)))),
			new Measurement(RUN6_ID, DIM_TT, Either.ofLeft(new MeasurementError("blargh")))
		);
		assertThat(run6.getAllDimensionsUsed()).containsExactlyInAnyOrder(DIM_HW, DIM_TT);

		run7 = runMap.get(RUN7_ID);
		assertThat(run7.getId()).isEqualTo(RUN7_ID);
		assertThat(run7.getAuthor()).isEqualTo("a7");
		assertThat(run7.getRunnerName()).isEqualTo("rn7");
		assertThat(run7.getRunnerInfo()).isEqualTo("ri7");
		assertThat(run7.getStartTime()).isEqualTo(RUN7_START);
		assertThat(run7.getStopTime()).isEqualTo(RUN7_START.plusSeconds(7));
		assertThat(run7.getSource()).isEqualTo(Either.ofLeft(new CommitSource(REPO1_ID, COMMIT1_HASH)));
		assertThat(run7.getRepoId()).isEqualTo(Optional.of(REPO1_ID));
		assertThat(run7.getResult())
			.isEqualTo(Either.ofLeft(new RunError("em7", RunErrorType.BENCH_SCRIPT_ERROR)));
		assertThat(run7.getAllDimensionsUsed()).isEmpty();
	}

	@Test
	void searchRuns() {
		// Search for all runs ("a" appears in every run's author)

		assertThat(access.searchRuns(100, null, "a")
			.stream()
			.map(Pair::getFirst)
			.map(ShortRunDescription::getId))
			.containsExactly(RUN7_ID, RUN2_ID, RUN8_ID, RUN4_ID, RUN10_ID, RUN9_ID, RUN3_ID, RUN6_ID,
				RUN5_ID, RUN1_ID);

		// Restricted to repos

		assertThat(access.searchRuns(100, REPO1_ID, "a")
			.stream()
			.map(Pair::getFirst)
			.map(ShortRunDescription::getId))
			.containsExactly(RUN7_ID, RUN8_ID, RUN4_ID, RUN3_ID, RUN6_ID, RUN5_ID);

		assertThat(access.searchRuns(100, REPO2_ID, "a")
			.stream()
			.map(Pair::getFirst)
			.map(ShortRunDescription::getId))
			.containsExactly(RUN10_ID, RUN9_ID);

		// Commit description and hash are ignored

		assertThat(access.searchRuns(100, null, "d9")
			.stream()
			.map(Pair::getFirst)
			.map(ShortRunDescription::getId))
			.containsExactly(RUN9_ID);

		assertThat(access.searchRuns(100, null, COMMIT1_HASH.getHash()))
			.isEmpty();

		// Limit is respected

		assertThat(access.searchRuns(4, null, "a")
			.stream()
			.map(Pair::getFirst)
			.map(ShortRunDescription::getId))
			.containsExactly(RUN7_ID, RUN2_ID, RUN8_ID, RUN4_ID);
	}

	@Test
	void getShortRunDescription() {
		assertThat(access.getShortRunDescription(RUN1_ID))
			.isEqualTo(new ShortRunDescription(RUN1_ID, null, null, "td1"));
		assertThat(access.getShortRunDescription(RUN2_ID))
			.isEqualTo(new ShortRunDescription(RUN2_ID, null, null, "td2"));
		assertThat(access.getShortRunDescription(RUN3_ID))
			.isEqualTo(new ShortRunDescription(RUN3_ID, null, null, "td3"));
		assertThat(access.getShortRunDescription(RUN4_ID))
			.isEqualTo(new ShortRunDescription(RUN4_ID, null, null, "td4"));
		assertThat(access.getShortRunDescription(RUN5_ID))
			.isEqualTo(new ShortRunDescription(RUN5_ID, COMMIT1_HASH.getHash(), "blad9bla", null));
		assertThat(access.getShortRunDescription(RUN6_ID))
			.isEqualTo(new ShortRunDescription(RUN6_ID, COMMIT1_HASH.getHash(), "blad9bla", null));
		assertThat(access.getShortRunDescription(RUN7_ID))
			.isEqualTo(new ShortRunDescription(RUN7_ID, COMMIT1_HASH.getHash(), "blad9bla", null));
		assertThat(access.getShortRunDescription(RUN8_ID))
			.isEqualTo(new ShortRunDescription(RUN8_ID, COMMIT2_HASH.getHash(), "m2", null));
		assertThat(access.getShortRunDescription(RUN9_ID))
			.isEqualTo(new ShortRunDescription(RUN9_ID, null, null, "td9"));
		assertThat(access.getShortRunDescription(RUN10_ID))
			.isEqualTo(new ShortRunDescription(RUN10_ID, COMMIT4_HASH.getHash(), "m4", null));
	}
}
