package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.shared.protocol.serverbound.entities.RunnerInformation;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Contains information about a single runner, how to reach it and its state machine.
 */
public class ActiveRunnerInformation {

	private final RunnerConnectionManager connectionManager;
	private final ServerRunnerStateMachine runnerStateMachine;

	private RunnerInformation runnerInformation;

	private Consumer<RunnerInformation> runnerInformationListener;
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

		this.disconnectedListener = (statusCode) -> {
		};
		this.runnerInformationListener = (information) -> {

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

	public void setRunnerInformationListener(Consumer<RunnerInformation> runnerInformationListener) {
		this.runnerInformationListener = runnerInformationListener;
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
		return runnerInformation.getRunnerState();
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
	 * Sets the listener for disconnections.
	 *
	 * @param listener the listener. Receives the status code as a parameter.
	 */
	public void setOnDisconnected(IntConsumer listener) {
		this.disconnectedListener = listener;
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
