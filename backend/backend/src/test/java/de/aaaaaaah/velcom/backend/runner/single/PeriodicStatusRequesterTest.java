package de.aaaaaaah.velcom.backend.runner.single;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitClearResultReply;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitGetResultReply;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitGetStatusReply;
import de.aaaaaaah.velcom.backend.runner.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PeriodicStatusRequesterTest {

	private static final UUID RUN_ID = UUID.randomUUID();

	private PeriodicStatusRequester statusRequester;
	private StateMachine<TeleRunnerState> stateMachine;
	private TeleRunner teleRunner;
	private RunnerConnection runnerConnection;

	@BeforeEach
	void setUp() {
		//noinspection unchecked
		stateMachine = mock(StateMachine.class);
		teleRunner = mock(TeleRunner.class);
		runnerConnection = mock(RunnerConnection.class);
		when(runnerConnection.getSerializer()).thenReturn(new Serializer());

		this.statusRequester = new PeriodicStatusRequester(
			teleRunner,
			runnerConnection,
			stateMachine,
			Duration.ofSeconds(0)
		);
	}

	@AfterEach
	void tearDown() {
		this.statusRequester.cancel();
	}

	@Test
	void requestsStatus() throws InterruptedException {
		statusRequester.start();

		verify(stateMachine, timeout(1000)).switchFromRestingState(any(AwaitGetStatusReply.class));
		verify(runnerConnection, timeout(1000))
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.GET_STATUS));
	}

	@Test
	void doesNotRequestResultsIfNone() {
		statusRequester.start();

		verify(runnerConnection, after(1000).never())
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.GET_RESULT));
	}

	@Test
	void requestResultsIfPresent() throws InterruptedException {
		statusRequester.start();

		var captor = ArgumentCaptor.forClass(AwaitGetStatusReply.class);
		verify(stateMachine, timeout(1000)).switchFromRestingState(captor.capture());

		captor.getValue().getReplyFuture().complete(new GetStatusReply(
			"info", "version", "version", true, Status.IDLE, null, null
		));

		verify(runnerConnection, timeout(5000))
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.GET_RESULT));
	}

	@Test
	void clearResultsIfRunnerHasNoTask() throws InterruptedException {
		when(teleRunner.getCurrentTask()).thenReturn(Optional.empty());
		getResult();

		expectClearResultsAndConsumeIt();
	}

	@Test
	void clearResultsIfRunnerHasDifferentTask() throws InterruptedException {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(new TaskId(UUID.randomUUID()));
		when(teleRunner.getCurrentTask()).thenReturn(Optional.of(task));
		getResult();

		expectClearResultsAndConsumeIt();
		verify(runnerConnection, timeout(5000)).close(StatusCode.ILLEGAL_BEHAVIOUR);
	}

	@Test
	void savesResultIfRunnerHasSameTask() throws InterruptedException {
		Task task = mock(Task.class);
		when(task.getId()).thenReturn(new TaskId(RUN_ID));
		when(teleRunner.getCurrentTask()).thenReturn(Optional.of(task));
		GetResultReply result = getResult();

		expectClearResultsAndConsumeIt();

		verify(teleRunner, timeout(1000)).handleResults(result);
	}

	@Test
	void clearResultIfConnectionHasError() throws InterruptedException {
		getStatusReplyFuture().completeExceptionally(new RuntimeException("A!"));

		expectClearResultsAndConsumeIt();
	}

	@Test
	void clearResultIfSavingResultsHasError() throws InterruptedException {
		doThrow(new RuntimeException("A!")).when(teleRunner).handleResults(any());
		getResult();

		expectClearResultsAndConsumeIt();
	}

	@Test
	void disconnectIfClearAfterErrorFails() throws InterruptedException {
		doThrow(new RuntimeException("A!")).when(teleRunner).handleResults(any());
		doThrow(new RuntimeException("NOPE!")).when(runnerConnection)
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.CLEAR_RESULT));

		getResult();

		verify(runnerConnection, timeout(5000)).close(StatusCode.INTERNAL_ERROR);
	}

	private void expectClearResultsAndConsumeIt() throws InterruptedException {
		ArgumentCaptor<AwaitClearResultReply> captor;
		// This is wrong, the captor accepts other classes as well
		//noinspection ConstantConditions
		do {
			captor = ArgumentCaptor.forClass(AwaitClearResultReply.class);
			verify(stateMachine, timeout(5000).atLeastOnce()).switchFromRestingState(captor.capture());
		} while (!(captor.getValue() instanceof AwaitClearResultReply));

		verify(runnerConnection, timeout(5000))
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.CLEAR_RESULT));

		captor.getValue().getReplyFuture().complete(null);
	}

	private CompletableFuture<GetResultReply> getStatusReplyFuture() throws InterruptedException {
		statusRequester.start();

		var captorStatus = ArgumentCaptor.forClass(AwaitGetStatusReply.class);
		verify(stateMachine, timeout(1000)).switchFromRestingState(captorStatus.capture());

		captorStatus.getValue().getReplyFuture().complete(new GetStatusReply(
			"info", "version", "version", true, Status.IDLE, null, null
		));

		verify(runnerConnection, timeout(5000))
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.GET_RESULT));

		var captorResult = ArgumentCaptor.forClass(AwaitGetResultReply.class);
		verify(stateMachine, timeout(1000).atLeastOnce())
			.switchFromRestingState(captorResult.capture());

		return captorResult.getValue().getReplyFuture();
	}

	private GetResultReply getResult() throws InterruptedException {
		statusRequester.start();

		var captorStatus = ArgumentCaptor.forClass(AwaitGetStatusReply.class);
		verify(stateMachine, timeout(1000)).switchFromRestingState(captorStatus.capture());

		captorStatus.getValue().getReplyFuture().complete(new GetStatusReply(
			"info", "version", "version", true, Status.IDLE, null, null
		));

		verify(runnerConnection, timeout(5000))
			.send(argThat(argument -> argument.getType() == ClientBoundPacketType.GET_RESULT));

		var captorResult = ArgumentCaptor.forClass(AwaitGetResultReply.class);
		verify(stateMachine, timeout(1000).atLeastOnce())
			.switchFromRestingState(captorResult.capture());

		GetResultReply reply = new GetResultReply(
			RUN_ID, true, mock(Result.class), null, Instant.now(), Instant.now().minusSeconds(20)
		);
		captorResult.getValue().getReplyFuture().complete(reply);

		return reply;
	}
}
