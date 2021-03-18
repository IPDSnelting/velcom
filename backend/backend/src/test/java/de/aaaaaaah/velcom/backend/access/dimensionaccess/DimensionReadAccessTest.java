package de.aaaaaaah.velcom.backend.access.dimensionaccess;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jooq.codegen.db.tables.Run.RUN;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions.NoSuchDimensionException;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DimensionReadAccessTest {

	private static final List<DimensionInfo> DIMENSIONS = List.of(
		new DimensionInfo(new Dimension("a", "b"), new Unit("c"), Interpretation.LESS_IS_BETTER, true),
		new DimensionInfo(new Dimension("a", "d"), new Unit("e"), Interpretation.MORE_IS_BETTER, false),
		new DimensionInfo(new Dimension("f", "g"), new Unit("h"), Interpretation.NEUTRAL, true),
		new DimensionInfo(new Dimension("i", "j"), new Unit("k"), Interpretation.NEUTRAL, false)
	);

	private static final RepoId REPO1_ID = new RepoId();
	private static final RepoId REPO2_ID = new RepoId();
	private static final RepoId REPO3_ID = new RepoId();
	private static final RepoId REPO4_ID = new RepoId();
	private static final RunId RUN1_ID = new RunId();
	private static final RunId RUN2_ID = new RunId();
	private static final RunId RUN3_ID = new RunId();
	private static final RunId RUN4_ID = new RunId();

	private DatabaseStorage databaseStorage;
	private DimensionReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		DIMENSIONS.forEach(testDb::addDimension);

		testDb.addRepo(REPO1_ID);
		testDb.addRepo(REPO2_ID);
		testDb.addRepo(REPO3_ID);
		testDb.addRepo(REPO4_ID);
		CommitHash run1Hash = new CommitHash("2acba5b560711dc4c8e53c356238862c07712eca");
		CommitHash run2Hash = new CommitHash("5dd4f1f6f0b3d5d5d830c6e4789d6f161496fa81");
		CommitHash run3Hash = new CommitHash("0bb703003ec44ac609760876a4668ff5a05b512b");
		CommitHash run4Hash = new CommitHash("a23577b29bfe6e384bba835b4453d0fc7f33855c");
		testDb.addCommit(REPO1_ID, run1Hash);
		testDb.addCommit(REPO2_ID, run2Hash);
		testDb.addCommit(REPO2_ID, run3Hash);
		testDb.addCommit(REPO3_ID, run4Hash);
		testDb.addRun(RUN1_ID, Either.ofLeft(new CommitSource(REPO1_ID, run1Hash)));
		testDb.addRun(RUN2_ID, Either.ofLeft(new CommitSource(REPO2_ID, run2Hash)));
		testDb.addRun(RUN3_ID, Either.ofLeft(new CommitSource(REPO2_ID, run3Hash)));
		testDb.addRun(RUN4_ID, Either.ofLeft(new CommitSource(REPO3_ID, run4Hash)));
		testDb.addMeasurement(RUN1_ID, new Dimension("a", "b"));
		testDb.addMeasurement(RUN1_ID, new Dimension("f", "g"));
		testDb.addMeasurement(RUN2_ID, new Dimension("a", "d"));
		testDb.addMeasurement(RUN3_ID, new Dimension("f", "g"));
		testDb.addMeasurement(RUN4_ID, new Dimension("i", "j"));

		databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
		access = new DimensionReadAccess(databaseStorage);
	}

	@Test
	void guardDimensionExists() {
		assertThatCode(() -> access.guardDimensionExists(new Dimension("a", "b")))
			.doesNotThrowAnyException();
		assertThatCode(() -> access.guardDimensionExists(new Dimension("a", "d")))
			.doesNotThrowAnyException();
		assertThatCode(() -> access.guardDimensionExists(new Dimension("f", "g")))
			.doesNotThrowAnyException();
		assertThatCode(() -> access.guardDimensionExists(new Dimension("i", "j")))
			.doesNotThrowAnyException();

		assertThatThrownBy(() -> access.guardDimensionExists(new Dimension("x", "y")))
			.isInstanceOf(NoSuchDimensionException.class)
			.extracting("invalidDimension")
			.isEqualTo(new Dimension("x", "y"));

		assertThatThrownBy(() -> access.guardDimensionExists(new Dimension("a", "g")))
			.isInstanceOf(NoSuchDimensionException.class)
			.extracting("invalidDimension")
			.isEqualTo(new Dimension("a", "g"));
	}

	@Test
	void getDimensions() {
		// All dimensions
		Set<DimensionInfo> allDimensions = access.getAllDimensions();
		assertThat(allDimensions.stream()
			.map(DimensionInfo::getDimension))
			.containsExactlyInAnyOrder(
				new Dimension("a", "b"),
				new Dimension("a", "d"),
				new Dimension("f", "g"),
				new Dimension("i", "j")
			);

		Map<Dimension, DimensionInfo> allDimensionsMap = allDimensions.stream()
			.collect(toMap(DimensionInfo::getDimension, it -> it));

		DimensionInfo infoAB = allDimensionsMap.get(new Dimension("a", "b"));
		assertThat(infoAB.getUnit()).isEqualTo(new Unit("c"));
		assertThat(infoAB.getInterpretation()).isEqualTo(Interpretation.LESS_IS_BETTER);
		assertThat(infoAB.isSignificant()).isTrue();

		DimensionInfo infoAD = allDimensionsMap.get(new Dimension("a", "d"));
		assertThat(infoAD.getUnit()).isEqualTo(new Unit("e"));
		assertThat(infoAD.getInterpretation()).isEqualTo(Interpretation.MORE_IS_BETTER);
		assertThat(infoAD.isSignificant()).isFalse();

		DimensionInfo infoFG = allDimensionsMap.get(new Dimension("f", "g"));
		assertThat(infoFG.getUnit()).isEqualTo(new Unit("h"));
		assertThat(infoFG.getInterpretation()).isEqualTo(Interpretation.NEUTRAL);
		assertThat(infoFG.isSignificant()).isTrue();

		DimensionInfo infoIJ = allDimensionsMap.get(new Dimension("i", "j"));
		assertThat(infoIJ.getUnit()).isEqualTo(new Unit("k"));
		assertThat(infoIJ.getInterpretation()).isEqualTo(Interpretation.NEUTRAL);
		assertThat(infoIJ.isSignificant()).isFalse();

		// Significant dimensions
		assertThat(access.getSignificantDimensions())
			.containsExactlyInAnyOrder(
				new Dimension("a", "b"),
				new Dimension("f", "g")
			);
	}

	@Test
	void getDimensionInfo() {
		DimensionInfo infoAB = access.getDimensionInfo(new Dimension("a", "b"));
		assertThat(infoAB.getUnit()).isEqualTo(new Unit("c"));
		assertThat(infoAB.getInterpretation()).isEqualTo(Interpretation.LESS_IS_BETTER);
		assertThat(infoAB.isSignificant()).isTrue();

		// Nonexistent info has default values
		DimensionInfo infoXY = access.getDimensionInfo(new Dimension("x", "y"));
		assertThat(infoXY.getUnit()).isEqualTo(Unit.DEFAULT);
		assertThat(infoXY.getInterpretation()).isEqualTo(Interpretation.DEFAULT);
		assertThat(infoXY.isSignificant()).isTrue(); // Default significance
	}

	@Test
	void getDimensionInfos() {
		Set<DimensionInfo> infos = access.getDimensionInfos(Set.of(new Dimension("a", "d")));
		assertThat(infos.stream()
			.map(DimensionInfo::getDimension))
			.containsExactly(new Dimension("a", "d"));

		DimensionInfo infoAD = new ArrayList<>(infos).get(0);
		assertThat(infoAD.getUnit()).isEqualTo(new Unit("e"));
		assertThat(infoAD.getInterpretation()).isEqualTo(Interpretation.MORE_IS_BETTER);
		assertThat(infoAD.isSignificant()).isFalse();

		Map<Dimension, DimensionInfo> infoMap = access
			.getDimensionInfoMap(Set.of(new Dimension("a", "b"), new Dimension("f", "g")));
		assertThat(infoMap).containsOnlyKeys(new Dimension("a", "b"), new Dimension("f", "g"));

		DimensionInfo infoAB = infoMap.get(new Dimension("a", "b"));
		assertThat(infoAB.getUnit()).isEqualTo(new Unit("c"));
		assertThat(infoAB.getInterpretation()).isEqualTo(Interpretation.LESS_IS_BETTER);
		assertThat(infoAB.isSignificant()).isTrue();

		DimensionInfo infoFG = infoMap.get(new Dimension("f", "g"));
		assertThat(infoFG.getUnit()).isEqualTo(new Unit("h"));
		assertThat(infoFG.getInterpretation()).isEqualTo(Interpretation.NEUTRAL);
		assertThat(infoFG.isSignificant()).isTrue();
	}

	@Test
	void getAvailableDimensions() {
		assertThat(access.getAvailableDimensions(REPO1_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "b"), new Dimension("f", "g"));
		assertThat(access.getAvailableDimensions(REPO2_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "d"), new Dimension("f", "g"));

		Map<RepoId, Set<Dimension>> dimensions = access
			.getAvailableDimensions(List.of(REPO1_ID, REPO2_ID));
		assertThat(dimensions)
			.containsOnlyKeys(REPO1_ID, REPO2_ID);
		assertThat(dimensions.get(REPO1_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "b"), new Dimension("f", "g"));
		assertThat(dimensions.get(REPO2_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "d"), new Dimension("f", "g"));

		// Change run3's source to a tar source
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl()
				.update(RUN)
				.set(RUN.COMMIT_HASH, (String) null)
				.set(RUN.TAR_DESC, "tarDesc")
				.where(RUN.ID.eq(RUN3_ID.getIdAsString()))
				.execute();
		}

		assertThat(access.getAvailableDimensions(REPO1_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "b"), new Dimension("f", "g"));
		assertThat(access.getAvailableDimensions(REPO2_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "d"));

		dimensions = access
			.getAvailableDimensions(List.of(REPO1_ID, REPO2_ID));
		assertThat(dimensions)
			.containsOnlyKeys(REPO1_ID, REPO2_ID);
		assertThat(dimensions.get(REPO1_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "b"), new Dimension("f", "g"));
		assertThat(dimensions.get(REPO2_ID))
			.containsExactlyInAnyOrder(new Dimension("a", "d"));
	}
}
