package de.aaaaaaah.velcom.runner.states;

import de.aaaaaaah.velcom.runner.Connection;
import de.aaaaaaah.velcom.runner.TeleBackend;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This state is entered if the backend replied that it had a task repo for the runner. After
 * downloading the task repo, this state transitions into {@link Idle}.
 */
public class AwaitingRun extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingRun.class);

	private final CompletableFuture<RequestRunReply> replyFuture;
	private final RequestRunReply reply;

	private OutputStream tmpFile;

	public AwaitingRun(TeleBackend teleBackend, Connection connection,
		RequestRunReply reply, CompletableFuture<RequestRunReply> replyFuture) {

		super(teleBackend, connection);

		this.replyFuture = replyFuture;
		this.reply = reply;
	}

	@Override
	public void onEnter() {
		LOGGER.info("{} - Receiving task repo for task {}", teleBackend.getAddress(),
			reply.getRunId().get());

		try {
			Path taskRepoTmpPath = teleBackend.getTaskRepoTmpPath();
			Files.createDirectories(taskRepoTmpPath.getParent());
			tmpFile = Files.newOutputStream(taskRepoTmpPath);
		} catch (IOException e) {
			LOGGER.warn("{} - Could not open stream to task repo tmp file", teleBackend.getAddress(), e);
		}
	}

	@Override
	public RunnerState onBinary(ByteBuffer data, boolean last) {
		if (tmpFile != null) {
			byte[] bytes = new byte[data.remaining()];
			data.get(bytes);
			try {
				tmpFile.write(bytes);
			} catch (IOException e) {
				LOGGER.warn("{} - Could not stream to task repo tmp file", teleBackend.getAddress(), e);
				tmpFile = null;
			}
		}

		if (!last) {
			return this;
		} else if (tmpFile == null) {
			return new Idle(teleBackend, connection);
		} else {
			replyFuture.complete(reply);
			return new Idle(teleBackend, connection);
		}
	}

	@Override
	public void onExit() {
		if (tmpFile != null) {
			try {
				tmpFile.close();
			} catch (IOException e) {
				LOGGER
					.warn("{} - Could not close stream to task repo tmp file", teleBackend.getAddress(), e);
			}
		}

		replyFuture.cancel(true);
	}
}
