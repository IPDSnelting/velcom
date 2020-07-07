package de.aaaaaaah.velcom.runner.revision;

// TODO update comment once backend is implemented

import de.aaaaaaah.velcom.runner.revision.benchmarking.BenchResult;
import de.aaaaaaah.velcom.runner.revision.states.AwaitingRequestRunReply;
import de.aaaaaaah.velcom.runner.revision.states.RunnerState;
import de.aaaaaaah.velcom.runner.revision.tmpdirs.BenchRepoDir;
import de.aaaaaaah.velcom.runner.revision.tmpdirs.TaskRepoDir;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.RequestRun;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * This class contains all the data and objects which are valid during the life of a backend.
 */
public class TeleBackend {

	private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);

	private final AtomicReference<Status> globalStatus;

	private final URI address;
	private final String token;
	private final Path path;

	private final BenchRepoDir benchRepoDir;
	private final TaskRepoDir taskRepoDir;

	// TODO add info for current run4

	public TeleBackend(AtomicReference<Status> globalStatus, URI address, String token, Path path) {
		this.globalStatus = globalStatus;

		this.address = address;
		this.token = token;
		this.path = path;

		// RunnerMain.die will stop the runner but java doesn't know that, so I need to do this
		// little dance here...
		BenchRepoDir tmpBenchRepoDir = null;
		try {
			tmpBenchRepoDir = new BenchRepoDir(path.resolve("bench_repo"));
		} catch (IOException e) {
			// Dying here is still okay since the main thread starts the TeleBackend threads only
			// when all TeleBackends have been initialized successfully. That's why we don't need
			// to close any open connections or stop running benchmarks here.
			RunnerMain.die(e, "Could not load hash file of bench repo");
		}
		benchRepoDir = tmpBenchRepoDir;

		taskRepoDir = new TaskRepoDir(path.resolve("task_repo"));
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

	public RunnerInfo getInfo() {
		// TODO add more info
		return RunnerInfo.fromSystemInfo();
	}

	public Status getStatus() {
		return globalStatus.get();
	}

	public Optional<String> getBenchHash() {
		return benchRepoDir.getHash();
	}

	public Optional<BenchResult> getBenchResult() {
		// TODO implement
		return Optional.empty();
	}

	public void clearBenchResult() {
		// TODO implement
	}

	public void abortCurrentRun() {
		// TODO implement
	}

	public Optional<UUID> getCurrentRunId() {
		// TODO implement
		return Optional.empty();
	}
}
