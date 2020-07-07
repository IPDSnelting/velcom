package de.aaaaaaah.velcom.runner.revision;

// TODO update comment once backend is implemented

import de.aaaaaaah.velcom.runner.revision.states.AwaitingRequestRunReply;
import de.aaaaaaah.velcom.runner.revision.states.RunnerState;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound.RequestRun;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/**
 * This class contains all the data and objects which are valid during the life of a backend.
 */
public class TeleBackend {

	private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);

	private final URI address;
	private final String token;

	public TeleBackend(URI address, String token) {
		this.address = address;
		this.token = token;
	}

	@Nullable
	private volatile Connection connection;

	public void run() {
		//noinspection InfiniteLoopStatement
		while (true) {
			try {
				Connection conn = new Connection(this, address, token);
				connection = conn;
				conn.getClosedFuture().get();
			} catch (ExecutionException | InterruptedException e) {
				try {
					//noinspection BusyWait
					Thread.sleep(RECONNECT_DELAY.toMillis());
				} catch (InterruptedException ignore) {
				}
			}
		}
	}

	@Nullable
	public Future<Void> maybePerformBenchmark() {
		Connection conn = connection;
		if (conn == null) {
			return null;
		}

		CompletableFuture<Boolean> receivedData = new CompletableFuture<>();

		try {
			RunnerState newState = new AwaitingRequestRunReply(this, conn, receivedData);
			conn.getStateMachine().switchFromRestingState(newState);
			conn.sendPacket(new RequestRun().asPacket(conn.getSerializer()));

			boolean needToPerformBenchmark = receivedData.get();
			if (!needToPerformBenchmark) {
				return null;
			}
		} catch (InterruptedException | ExecutionException e) {
			return null;
		}

		CompletableFuture<Void> benchmarkPerformed = new CompletableFuture<>();
		benchmarkPerformed.complete(null); // TODO implement actual benchmarking
		return benchmarkPerformed;
	}

}
