package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Contains information about a single runner, how to reach it and its state machine.
 */
public class ActiveRunnerInformation {

	private RunnerConnectionManager connectionManager;
	private ServerRunnerStateMachine runnerStateMachine;

	private RunnerInformation runnerInformation;

	private Consumer<BenchmarkResults> resultListener;
	private Consumer<RunnerInformation> runnerInformationListener;
	private Runnable idleListener;
	private IntConsumer disconnectedListener;

	private Commit currentCommit;
	private Instant lastReceivedMessage;

	/**
	 * Creates a new runner information.
	 *
	 * @param connectionManager the connection manager
	 * @param runnerStateMachine the runner state machine
	 */
	public ActiveRunnerInformation(RunnerConnectionManager connectionManager,
		ServerRunnerStateMachine runnerStateMachine) {
		this.connectionManager = connectionManager;
		this.runnerStateMachine = runnerStateMachine;
		this.lastReceivedMessage = Instant.now();

		this.resultListener = it -> {
		};
		this.disconnectedListener = (statusCode) -> {
		};
		this.idleListener = () -> {
		};
		this.runnerInformationListener = ignored -> {
		};
	}

	/**
	 * Returns the runner's core information (os, name, etc).
	 *
	 * @return the runner's core information
	 */
	public Optional<RunnerInformation> getRunnerInformation() {
		return Optional.ofNullable(runnerInformation);
	}

	/**
	 * Sets the core runner information.
	 *
	 * @param runnerInformation the runner information
	 */
	public void setRunnerInformation(RunnerInformation runnerInformation) {
		this.runnerInformation = runnerInformation;
		this.runnerInformationListener.accept(runnerInformation);
	}

	/**
	 * @return the current runner state
	 */
	public RunnerStatusEnum getState() {
		return runnerStateMachine.getState().getStatus();
	}

	/**
	 * Returns the current commit, if any. Will be cleared when results arrive.
	 *
	 * @return the current commit
	 */
	public Optional<Commit> getCurrentCommit() {
		return Optional.ofNullable(currentCommit);
	}

	/**
	 * Returns the connection manager.
	 *
	 * @return the connection manager
	 */
	public RunnerConnectionManager getConnectionManager() {
		return connectionManager;
	}

	/**
	 * Returns the runner state machine.
	 *
	 * @return the runner state machine
	 */
	public ServerRunnerStateMachine getRunnerStateMachine() {
		return runnerStateMachine;
	}

	/**
	 * Marks the runner as idle.
	 */
	public void setIdle() {
		idleListener.run();
	}

	/**
	 * Sets the listener for disconnections.
	 *
	 * @param listener the listener. Receives the status code as a parameter.
	 */
	public void setOnDisconnected(IntConsumer listener) {
		this.disconnectedListener = listener;
	}

	/**
	 * Sets the listener to call when the runner switches to idle.
	 *
	 * @param idleListener the idle listener
	 */
	public void setOnIdle(Runnable idleListener) {
		this.idleListener = idleListener;
	}

	/**
	 * Sets the listener to call when the {@link RunnerInformation} are set or updated.
	 *
	 * @param listener the listener
	 */
	public void setOnRunnerInformation(Consumer<RunnerInformation> listener) {
		this.runnerInformationListener = listener;
	}

	/**
	 * Sets the listener for results.
	 *
	 * @param listener the listener
	 */
	public void setResultListener(Consumer<BenchmarkResults> listener) {
		this.resultListener = listener;
	}

	/**
	 * Returns the time the last runner message arrived.
	 *
	 * @return the time the last runner message arrived
	 */
	public Instant getLastReceivedMessage() {
		return lastReceivedMessage;
	}

	/**
	 * Sets the time the last runner message arrived.
	 *
	 * @param lastReceivedMessage the time the last runner message arrived
	 */
	public void setLastReceivedMessage(Instant lastReceivedMessage) {
		this.lastReceivedMessage = lastReceivedMessage;
	}

	/**
	 * Marks the runner as disconnected.
	 *
	 * @param statusCode the status code
	 */
	public void setDisconnected(int statusCode) {
		disconnectedListener.accept(statusCode);
	}

	/**
	 * Sets the last results.
	 *
	 * @param results the last results
	 */
	void setResults(BenchmarkResults results) {
		this.resultListener.accept(results);
		clearCurrentCommit();
	}

	/**
	 * Sets the current commit.
	 *
	 * @param currentCommit the current commit. May be null.
	 */
	void setCurrentCommit(Commit currentCommit) {
		this.currentCommit = Objects.requireNonNull(
			currentCommit, "currentCommit can not be null, use clearCurrentCommit!"
		);
	}

	/**
	 * Clears the current commit.
	 */
	public void clearCurrentCommit() {
		currentCommit = null;
	}

	@Override
	public String toString() {
		return "ActiveRunnerInformation{" +
			"runnerInformation=" + runnerInformation +
			", connectionManager=" + connectionManager +
			", runnerStateMachine=" + runnerStateMachine +
			'}';
	}
}
