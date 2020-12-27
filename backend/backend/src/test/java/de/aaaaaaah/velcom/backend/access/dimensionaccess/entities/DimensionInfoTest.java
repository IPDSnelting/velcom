package de.aaaaaaah.velcom.backend.access.dimensionaccess.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DimensionInfoTest {

	private Dimension dimension;
	private Unit unit;
	private Interpretation interpretation;

	@BeforeEach
	void setUp() {
		dimension = new Dimension("test", "dimension");
		unit = new Unit("test unit");
		interpretation = Interpretation.LESS_IS_BETTER;
	}

	@Test
	void createDimInfo() {
		DimensionInfo info = new DimensionInfo(dimension);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(Unit.DEFAULT);
		assertThat(info.getInterpretation()).isEqualTo(Interpretation.DEFAULT);
		assertThat(info.isSignificant()).isTrue(); // Default significance

		info = new DimensionInfo(dimension, unit, interpretation, false);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(unit);
		assertThat(info.getInterpretation()).isEqualTo(interpretation);
		assertThat(info.isSignificant()).isFalse();

		info = new DimensionInfo(dimension, null, null);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(Unit.DEFAULT);
		assertThat(info.getInterpretation()).isEqualTo(Interpretation.DEFAULT);
		assertThat(info.isSignificant()).isTrue(); // Default significance

		info = new DimensionInfo(dimension, unit, null);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(unit);
		assertThat(info.getInterpretation()).isEqualTo(Interpretation.DEFAULT);
		assertThat(info.isSignificant()).isTrue(); // Default significance

		info = new DimensionInfo(dimension, null, interpretation);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(Unit.DEFAULT);
		assertThat(info.getInterpretation()).isEqualTo(interpretation);
		assertThat(info.isSignificant()).isTrue(); // Default significance

		info = new DimensionInfo(dimension, unit, interpretation);
		assertThat(info.getDimension()).isEqualTo(dimension);
		assertThat(info.getUnit()).isEqualTo(unit);
		assertThat(info.getInterpretation()).isEqualTo(interpretation);
		assertThat(info.isSignificant()).isTrue(); // Default significance
	}
}
