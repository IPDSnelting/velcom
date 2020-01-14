package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.time.Instant;
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
	private RunnerStatusEnum state;

	private Consumer<BenchmarkResults> resultListener;
	private Consumer<RunnerStatusEnum> statusListener;
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
		this.state = RunnerStatusEnum.DISCONNECTED;
		this.lastReceivedMessage = Instant.now();

		this.resultListener = it -> {
		};
		this.statusListener = it -> {
		};
		this.disconnectedListener = (statusCode) -> {
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
	}

	/**
	 * Returns the current runner state.
	 *
	 * @return the current runner state
	 */
	public RunnerStatusEnum getState() {
		return state;
	}

	/**
	 * Sets the current runner state.
	 *
	 * @param state the current runner state
	 */
	public void setState(RunnerStatusEnum state) {
		boolean callListeners = state != this.state;
		this.state = state;
		if (callListeners && state != RunnerStatusEnum.DISCONNECTED) {
			this.statusListener.accept(state);
		}
	}

	/**
	 * Marks the runner as disconnected.
	 *
	 * @param statusCode the status code
	 */
	public void setDisconnected(int statusCode) {
		this.setState(RunnerStatusEnum.DISCONNECTED);
		disconnectedListener.accept(statusCode);
	}

	/**
	 * Sets the last results.
	 *
	 * @param results the last results
	 */
	public void setResults(BenchmarkResults results) {
		this.resultListener.accept(results);
		setCurrentCommit(null);
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
	 * Sets the current commit.
	 *
	 * @param currentCommit the current commit. May be null.
	 */
	public void setCurrentCommit(Commit currentCommit) {
		this.currentCommit = currentCommit;
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
	 * Sets the listener for status changes.
	 *
	 * @param statusListener the status listener
	 */
	public void setStatusListener(Consumer<RunnerStatusEnum> statusListener) {
		this.statusListener = statusListener;
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

	@Override
	public String toString() {
		return "ActiveRunnerInformation{" +
			"runnerInformation=" + runnerInformation +
			", state=" + state +
			", connectionManager=" + connectionManager +
			", runnerStateMachine=" + runnerStateMachine +
			'}';
	}
}
