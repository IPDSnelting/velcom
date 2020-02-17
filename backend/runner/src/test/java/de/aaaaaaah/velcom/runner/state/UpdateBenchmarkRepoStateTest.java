package de.aaaaaaah.velcom.runner.state;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateBenchmarkRepoStateTest {

	private UpdateBenchmarkRepoState updateBenchmarkRepoState;

	@BeforeEach
	void setUp() {
		updateBenchmarkRepoState = new UpdateBenchmarkRepoState("hash");
	}

	@Test
	void correctStatus() {
		assertThat(updateBenchmarkRepoState.getStatus()).isEqualTo(RunnerStatusEnum.IDLE);
	}

}