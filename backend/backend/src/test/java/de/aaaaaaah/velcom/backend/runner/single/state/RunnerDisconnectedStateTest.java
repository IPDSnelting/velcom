package de.aaaaaaah.velcom.backend.runner.single.state;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import org.junit.jupiter.api.Test;

class RunnerDisconnectedStateTest {

	@Test
	void statusCorrect() {
		assertThat(new RunnerDisconnectedState().getStatus())
			.isEqualTo(RunnerStatusEnum.DISCONNECTED);
	}
}