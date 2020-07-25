package de.aaaaaaah.velcom.runner.states;

import de.aaaaaaah.velcom.runner.Connection;
import de.aaaaaaah.velcom.runner.TeleBackend;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO implement
public class AwaitingBench extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingBench.class);

	private final RequestRunReply reply;
	private final CompletableFuture<RequestRunReply> replyFuture;
	private boolean owningReplyFuture;

	private OutputStream tmpFile;

	public AwaitingBench(TeleBackend teleBackend, Connection connection,
		RequestRunReply reply, CompletableFuture<RequestRunReply> replyFuture) {

		super(teleBackend, connection);

		this.replyFuture = replyFuture;
		owningReplyFuture = true;

		this.reply = reply;
	}

	@Override
	public void onEnter() {
		LOGGER.info("{}: Receiving bench repo", teleBackend);

		try {
			File file = teleBackend.getBenchRepoTmpPath().toFile();
			file.getParentFile().mkdirs();
			file.createNewFile();
			tmpFile = new FileOutputStream(file);
		} catch (IOException e) {
			LOGGER.warn("{}: Could not open stream to bench repo tmp file",  teleBackend, e);
		}
	}

	@Override
	public RunnerState onBinary(ByteBuffer data, boolean last) {
		if (tmpFile != null) {
			byte[] bytes;
			if (data.hasArray()) {
				bytes = data.array();
			} else {
				bytes = new byte[data.remaining()];
				data.get(bytes);
			}
			try {
				tmpFile.write(bytes);
			} catch (IOException e) {
				LOGGER.warn("{}: Could not stream to bench repo tmp file", teleBackend, e);
				tmpFile = null;
			}
		}

		if (!last) {
			return this;
		} else if (tmpFile == null) {
			return new Idle(teleBackend, connection);
		} else if (reply.hasRun()) {
			owningReplyFuture = false;
			return new AwaitingRun(teleBackend, connection, reply, replyFuture);
		} else {
			owningReplyFuture = false;
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
				LOGGER.warn("{}: Could not close stream to bench repo tmp file", teleBackend, e);
			}
		}

		if (owningReplyFuture) {
			replyFuture.cancel(true);
		}
	}
}
