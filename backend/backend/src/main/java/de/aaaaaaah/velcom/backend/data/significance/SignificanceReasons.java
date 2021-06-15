package de.aaaaaaah.velcom.backend.data.significance;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import java.util.List;

public class SignificanceReasons {

	private final List<DimensionDifference> significantDifferences;
	private final List<Dimension> significantFailedDimensions;
	private final boolean entireRunFailed;

	public SignificanceReasons(List<DimensionDifference> significantDifferences,
		List<Dimension> significantFailedDimensions, boolean entireRunFailed) {

		this.significantDifferences = significantDifferences;
		this.significantFailedDimensions = significantFailedDimensions;
		this.entireRunFailed = entireRunFailed;
	}

	public List<DimensionDifference> getSignificantDifferences() {
		return significantDifferences;
	}

	public List<Dimension> getSignificantFailedDimensions() {
		return significantFailedDimensions;
	}

	public boolean isEntireRunFailed() {
		return entireRunFailed;
	}
}
