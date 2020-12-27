package de.aaaaaaah.velcom.backend.access.dimensionaccess.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import org.junit.jupiter.api.Test;

class InterpretationTest {

	@Test
	void fromTextualRepresentation() {
		assertThat(Interpretation.fromTextualRepresentation("LESS_IS_BETTER"))
			.isEqualTo(Interpretation.LESS_IS_BETTER);
		assertThat(Interpretation.fromTextualRepresentation("MORE_IS_BETTER"))
			.isEqualTo(Interpretation.MORE_IS_BETTER);
		assertThat(Interpretation.fromTextualRepresentation("NEUTRAL"))
			.isEqualTo(Interpretation.NEUTRAL);

		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation("blubb"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation("less_is_better"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation("LESS_IS_BETTER_"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation("neutral"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation("NEUTRAL "))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation(" NEUTRAL"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Interpretation.fromTextualRepresentation(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fromSharedRepresentation() {
		assertThat(Interpretation.fromSharedRepresentation(Result.Interpretation.LESS_IS_BETTER))
			.isEqualTo(Interpretation.LESS_IS_BETTER);
		assertThat(Interpretation.fromSharedRepresentation(Result.Interpretation.MORE_IS_BETTER))
			.isEqualTo(Interpretation.MORE_IS_BETTER);
		assertThat(Interpretation.fromSharedRepresentation(Result.Interpretation.NEUTRAL))
			.isEqualTo(Interpretation.NEUTRAL);
	}
}
