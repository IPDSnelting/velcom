package de.aaaaaaah.velcom.backend.access.dimensionaccess.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DimensionTest {

	@SuppressWarnings("EqualsWithItself")
	@Test
	void compareDimensions() {
		Dimension dimAA = new Dimension("a", "a");
		Dimension dimAB = new Dimension("a", "b");
		Dimension dimBA = new Dimension("b", "a");
		Dimension dimBB = new Dimension("b", "b");

		assertThat(dimAA.compareTo(dimAA)).isEqualTo(0);
		assertThat(dimAA).isLessThan(dimAB);
		assertThat(dimAA).isLessThan(dimBA);
		assertThat(dimAA).isLessThan(dimBB);

		assertThat(dimAB).isGreaterThan(dimAA);
		assertThat(dimAB.compareTo(dimAB)).isEqualTo(0);
		assertThat(dimAB).isLessThan(dimBA);
		assertThat(dimAB).isLessThan(dimBB);

		assertThat(dimBA).isGreaterThan(dimAA);
		assertThat(dimBA).isGreaterThan(dimAB);
		assertThat(dimBA.compareTo(dimBA)).isEqualTo(0);
		assertThat(dimBA).isLessThan(dimBB);

		assertThat(dimBB).isGreaterThan(dimAA);
		assertThat(dimBB).isGreaterThan(dimAB);
		assertThat(dimBB).isGreaterThan(dimBA);
		assertThat(dimBB.compareTo(dimBB)).isEqualTo(0);
	}
}
