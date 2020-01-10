package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Contains information about a single runner, how to reach it and its state machine.
 */
public class ActiveRunnerInformation {

	private RunnerConnectionManager connectionManager;
	private ServerRunnerStateMachine runnerStateMachine;

	private RunnerInformation runnerInformation;
	private RunnerStatusEnum state;

	private List<Consumer<BenchmarkResults>> resultListeners;
	private List<Consumer<RunnerStatusEnum>> statusListeners;

	private BenchmarkResults results;
	private Commit currentCommit;

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

		this.statusListeners = new ArrayList<>();
		this.resultListeners = new ArrayList<>();
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
		if (callListeners) {
			this.statusListeners.forEach(it -> it.accept(state));
		}
	}

	/**
	 * Returns the last benchmark results, if any.
	 *
	 * @return the last benchmark results
	 */
	public Optional<BenchmarkResults> getResults() {
		return Optional.ofNullable(results);
	}

	/**
	 * Sets the last results.
	 *
	 * @param results the last results
	 */
	public void setResults(BenchmarkResults results) {
		this.results = results;
		this.resultListeners.forEach(it -> it.accept(results));
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
	 * Adds a listener for status changes. You may change the state from a listener!
	 *
	 * @param listener the listener
	 */
	public void addStatusListener(Consumer<RunnerStatusEnum> listener) {
		this.statusListeners.add(listener);
	}

	/**
	 * Adds a listener for results.
	 *
	 * @param listener the listener
	 */
	public void addResultListener(Consumer<BenchmarkResults> listener) {
		this.resultListeners.add(listener);
	}

	@Override
	public String toString() {
		return "ActiveRunnerInformation{" +
			"runnerInformation=" + runnerInformation +
			", state=" + state +
			", results=" + results +
			", connectionManager=" + connectionManager +
			", runnerStateMachine=" + runnerStateMachine +
			'}';
	}
}
