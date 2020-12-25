package de.aaaaaaah.velcom.backend.access.dimensionaccess;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions.NoSuchDimensionException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
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
		new DimensionInfo(new Dimension("f", "g"), new Unit("h"), Interpretation.NEUTRAL, true)
	);

	private DimensionReadAccess access;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		TestDb testDb = new TestDb(tempDir);

		DIMENSIONS.forEach(testDb::addDimension);

		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());
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
				new Dimension("f", "g")
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

	// TODO: 2020-12-23 Add test for the getAvailableDimensions functions
}
