package de.aaaaaaah.velcom.runner.state;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;

class RunnerStateMachineTest {

	private RunnerStateMachine runnerStateMachine;
	private RunnerConfiguration configuration;
	private SocketConnectionManager socketConnectionManager;

	@BeforeEach
	void setUp() {
		runnerStateMachine = new RunnerStateMachine();

		configuration = mock(RunnerConfiguration.class);
		socketConnectionManager = mock(SocketConnectionManager.class);
		BenchmarkRepoOrganizer benchmarkRepoOrganizer = mock(BenchmarkRepoOrganizer.class);

		when(configuration.getConnectionManager()).thenReturn(socketConnectionManager);
		when(configuration.getBenchmarkRepoOrganizer()).thenReturn(benchmarkRepoOrganizer);

		when(benchmarkRepoOrganizer.getHeadHash()).thenReturn(Optional.empty());
	}

	@Test
	void openSendsRunnerInformation() throws IOException {
		runnerStateMachine.onConnectionEstablished(configuration);

		verify(socketConnectionManager).sendEntity(Matchers.isA(RunnerInformation.class));
	}

}