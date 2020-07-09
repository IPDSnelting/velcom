package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.runner_new.Dispatcher;
import de.aaaaaaah.velcom.backend.runner_new.KnownRunner;
import de.aaaaaaah.velcom.backend.runner_new.single.state.AwaitAbortRunReply;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.AbortRun;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side class for a runner.
 */
public class TeleRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(TeleRunner.class);

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
	 * Aborts the current benchmark. This is a very rudimentary implementation at the moment.
	 * <br>
	 * The abort will do nothing, if
	 * <ul>
	 *   <li>The runner is not connected</li>
	 *   <li>The send fails</li>
	 *   <li>The runner reconnects and has results for that run</li>
	 *   <li>The server sends a get_status before the abort and the runner has results</li>
	 * </ul>
	 * These limitations might be lifted in the future.
	 */
	public void abort() {
		if (!hasConnection()) {
			LOGGER.info("Tried to abort commit but was not connected with a runner: {}", getRunnerName());
			return;
		}

		try {
			connection.getStateMachine().switchFromRestingState(new AwaitAbortRunReply(this, connection));
			connection.send(new AbortRun().asPacket(serializer));
		} catch (InterruptedException e) {
			LOGGER.warn("Abort failed, I was interrupted {}", getRunnerName());
		}
	}

	public void sendAvailableWork() {
		Optional<Task> workOptional = dispatcher.getWork(this);

		if (workOptional.isEmpty()) {
			connection.send(
				new RequestRunReply(false, null, false, null).asPacket(serializer)
			);
			return;
		}
		Task task = workOptional.get();

		boolean benchRepoUpToDate = runnerInformation.get().getBenchHash()
			.map(it -> it.equals("current"))
			.orElse(false);
		connection.send(
			new RequestRunReply(!benchRepoUpToDate, "current", true, task.getId().getId())
				.asPacket(serializer)
		);

		if (!benchRepoUpToDate) {
			// TODO: Send bench repo
		}

		try (OutputStream outputStream = connection.createBinaryOutputStream()) {
			dispatcher.getQueue().transferTask(task.getId(), outputStream);
		} catch (PrepareTransferException e) {
			LOGGER.info("Failed to transfer repo to runner: Archiving failed", e);
			// This task is corrupted, we can not benchmark it.
			// TODO: 09.07.20 Mark this task as done and attach an error, somehow 
		} catch (TransferException | IOException | NoSuchTaskException e) {
			LOGGER.info("Failed to transfer repo to runner: Sending failed", e);
			// TODO: 09.07.20 Reinsert task to queue
			connection.close(StatusCode.TRANSFER_FAILED);
		}
	}
}
