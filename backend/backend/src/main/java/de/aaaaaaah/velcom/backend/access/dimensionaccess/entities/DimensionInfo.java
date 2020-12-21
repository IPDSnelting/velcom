package de.aaaaaaah.velcom.backend.access.dimensionaccess.entities;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Information associated globally with a dimension.
 */
public class DimensionInfo {

	private static final boolean DEFAULT_SIGNIFICANCE = true;

	private final Dimension dimension;
	private final Unit unit;
	private final Interpretation interpretation;
	private final boolean significant;

	public DimensionInfo(Dimension dimension, Unit unit, Interpretation interpretation,
		boolean significant) {

		this.dimension = Objects.requireNonNull(dimension);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
		this.significant = significant;
	}

	public DimensionInfo(Dimension dimension, @Nullable Unit unit,
		@Nullable Interpretation interpretation) {

		this(
			dimension,
			Optional.ofNullable(unit).orElse(Unit.DEFAULT),
			Optional.ofNullable(interpretation).orElse(Interpretation.DEFAULT),
			DEFAULT_SIGNIFICANCE
		);
	}

	/**
	 * Create a {@link DimensionInfo} with default unit and interpretation.
	 *
	 * @param dimension the dimension
	 */
	public DimensionInfo(Dimension dimension) {
		this(dimension, Unit.DEFAULT, Interpretation.DEFAULT, DEFAULT_SIGNIFICANCE);
	}

	public Dimension getDimension() {
		return dimension;
	}

	public Unit getUnit() {
		return unit;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	public boolean isSignificant() {
		return significant;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DimensionInfo dimensionInfo = (DimensionInfo) o;
		return dimension.equals(dimensionInfo.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dimension);
	}

	@Override
	public String toString() {
		return "DimensionInfo{" +
			"dimension=" + dimension +
			", unit=" + unit +
			", interpretation=" + interpretation +
			'}';
	}
}
