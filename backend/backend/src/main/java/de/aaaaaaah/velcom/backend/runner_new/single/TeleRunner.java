package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.runner_new.Dispatcher;
import de.aaaaaaah.velcom.backend.runner_new.KnownRunner;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound.GetStatusReply;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Server side class for a runner.
 */
public class TeleRunner {

	private final AtomicReference<GetStatusReply> runnerInformation;
	private final String runnerName;
	private final Converter serializer;
	private final Dispatcher dispatcher;

	private RunnerConnection connection;

	public TeleRunner(String runnerName, Converter serializer, Dispatcher dispatcher) {
		this.runnerName = runnerName;
		this.serializer = serializer;
		this.dispatcher = dispatcher;
		this.runnerInformation = new AtomicReference<>();

		this.connection = createConnection();
	}

	/**
	 * Creates a new connection, if none exists.
	 *
	 * @return the created connection
	 * @throws IllegalStateException if this runner already has a connection
	 */
	public synchronized RunnerConnection createConnection() throws IllegalStateException {
		if (connection != null) {
			throw new IllegalStateException("I already have a connection");
		}
		connection = new RunnerConnection(serializer, this);
		connection.addCloseListener(this::disposeConnection);

		return connection;
	}

	private void disposeConnection() {
		synchronized (this) {
			this.connection = null;
		}
	}

	/**
	 * @return true if this runner has an active connection.
	 */
	public synchronized boolean hasConnection() {
		return connection != null;
	}

	/**
	 * Returns the unique name for this runner.
	 *
	 * @return the runner name
	 */
	public String getRunnerName() {
		return runnerName;
	}

	/**
	 * Returns the dispatcher this runner is assigned to.
	 *
	 * @return the dispatcher
	 */
	public Dispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Sets the runner information.
	 *
	 * @param reply the reply
	 */
	public void setRunnerInformation(GetStatusReply reply) {
		runnerInformation.set(reply);
	}

	/**
	 * Returns the runner information. This must always be non-null from the moment the runner is
	 * registered to the dispatcher. The value might be outdated.
	 */
	public KnownRunner getRunnerInformation() {
		GetStatusReply reply = runnerInformation.get();

		return new KnownRunner(getRunnerName(), reply.getInfo(), reply.getStatus());
	}

	/**
	 * Aborts the current benchmark. If the runner is not connected and reconnects with a working
	 * status and the same runId, the benchmark will be cancelled then. If the runner reconnects and
	 * has a result for the runID available, the result will be discarded.
	 */
	public void abort(UUID runId) {
		// TODO: 07.07.20 Implement
	}
}
