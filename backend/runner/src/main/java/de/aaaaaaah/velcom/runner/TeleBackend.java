package de.aaaaaaah.velcom.runner;

// TODO update comment once backend is implemented

import de.aaaaaaah.velcom.runner.benchmarking.BenchResult;
import de.aaaaaaah.velcom.runner.benchmarking.Benchmarker;
import de.aaaaaaah.velcom.runner.states.AwaitingRequestRunReply;
import de.aaaaaaah.velcom.runner.states.RunnerState;
import de.aaaaaaah.velcom.runner.tmpdirs.BenchRepoDir;
import de.aaaaaaah.velcom.runner.tmpdirs.TaskRepoDir;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.RequestRun;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import de.aaaaaaah.velcom.shared.util.compression.TarHelper;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains all the data and objects which are valid during the life of a backend.
 */
public class TeleBackend {

	private static final Logger LOGGER = LoggerFactory.getLogger(TeleBackend.class);

	private final AtomicReference<Status> globalStatus;

	private final URI address;
	private final String name;
	private final String token;

	private final BenchRepoDir benchRepoDir;
	private final TaskRepoDir taskRepoDir;

	// Protects benchmarker
	private final Object benchmarkerLock;
	@Nullable
	private Benchmarker benchmarker;

	public TeleBackend(AtomicReference<Status> globalStatus, URI address, String name, String token,
		Path path) {

		this.globalStatus = globalStatus;

		this.address = address;
		this.name = name;
		this.token = token;

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

		benchmarkerLock = new Object();
		benchmarker = null;
	}

	@Nullable
	private volatile Connection connection;

	public void run() throws InterruptedException {
		//noinspection InfiniteLoopStatement
		while (true) {
			try {
				LOGGER.info("Connecting to " + address);
				Connection conn = new Connection(this, address, name, token);
				connection = conn;
				conn.getClosedFuture().get();
				LOGGER.info("Disconnected from " + address + ", reconnecting immediately");
			} catch (ExecutionException | InterruptedException e) {
				LOGGER.warn("Failed to connect to " + address + ", retrying soon");
				//noinspection BusyWait
				Thread.sleep(Delays.RECONNECT_AFTER_FAILED_CONNECTION.toMillis());
			}
		}
	}

	/**
	 * Performs a benchmark if possible, asking the backend in the process.
	 *
	 * <em>This function must always be called from the same thread!</em>
	 *
	 * @throws InterruptedException in an irrecoverable situation (runner must die)
	 * @throws ExecutionException in an irrecoverable situation (runner must die)
	 */
	public void maybePerformBenchmark() throws InterruptedException, ExecutionException {
		globalStatus.set(Status.RUN);

		Optional<BenchResult> benchResult = getBenchResult();
		if (benchResult.isPresent()) {
			// The runner protocol forbids us from asking the backend for a new task while we still
			// have a result, so we just ignore the request.
			return;
		}
		// Since this function is the only place where the benchmarker can be set to a non-null
		// value, and this function only returns after the benchmarker has a result (or is still
		// null in the first place), we now know that there is no benchmarker.

		Connection conn = connection;
		if (conn == null) {
			// We obviously can't ask the backend for a task if we aren't even connected to it.
			return;
		}

		try {
			clearTmpFiles();
		} catch (IOException e) {
			LOGGER.warn("Could not clear temporary files: ", e);
			return;
		}

		// This future will complete as soon as all tar files have been downloaded.
		CompletableFuture<RequestRunReply> replyFuture = sendRequestRun(conn);

		RequestRunReply reply = null;
		try {
			reply = replyFuture.get();
		} catch (ExecutionException | CancellationException e) {
			// Something went wrong while trying to receive files from the backend
			LOGGER.warn("Something went wrong while requesting run: ", e);
			return;
		}

		try {
			unpackTmpFiles(reply.hasBench(), reply.hasRun());
			if (reply.getBenchHash().isPresent()) {
				benchRepoDir.setHash(reply.getBenchHash().get());
			}
		} catch (IOException e) {
			LOGGER.warn("Could not unpack tar files: ", e);
			return;
		}
		try {
			clearTmpFiles();
		} catch (IOException e) {
			LOGGER.warn("Could not clear temporary files: ", e);
			return;
		}

		if (reply.getRunId().isPresent()) {
			startBenchmark(reply.getRunId().get()).get();
		}

		globalStatus.set(Status.IDLE);
	}

	private void clearTmpFiles() throws IOException {
		FileHelper.deleteDirectoryOrFile(benchRepoDir.getTmpFilePath());
		FileHelper.deleteDirectoryOrFile(taskRepoDir.getTmpFilePath());
	}

	private void unpackTmpFiles(boolean benchRepo, boolean taskRepo) throws IOException {
		if (benchRepo) {
			FileHelper.deleteDirectoryOrFile(benchRepoDir.getDirPath());
			TarHelper.untar(benchRepoDir.getTmpFilePath(), benchRepoDir.getDirPath());
		}

		if (taskRepo) {
			FileHelper.deleteDirectoryOrFile(taskRepoDir.getDirPath());
			TarHelper.untar(taskRepoDir.getTmpFilePath(), taskRepoDir.getDirPath());
		}
	}

	private CompletableFuture<RequestRunReply> sendRequestRun(Connection conn)
		throws InterruptedException {

		CompletableFuture<RequestRunReply> replyFuture = new CompletableFuture<>();

		RunnerState newState = new AwaitingRequestRunReply(this, conn, replyFuture);
		conn.getStateMachine().switchFromRestingState(newState);
		conn.sendPacket(new RequestRun().asPacket(conn.getSerializer()));

		return replyFuture;
	}

	private CompletableFuture<Void> startBenchmark(UUID runId) {
		// The benchmarker is guaranteed to be null. For more detail, see the comment at the
		// top of maybePerformBenchmark.

		CompletableFuture<Void> benchmarkFinished = new CompletableFuture<>();

		Benchmarker newBenchmarker = new Benchmarker(
			benchmarkFinished,
			runId,
			taskRepoDir.getDirPath(),
			benchRepoDir.getHash().orElse(null),
			benchRepoDir.getDirPath(),
			Instant.now(),
			name,
			LinuxSystemInfo.getCurrent()
		);

		synchronized (benchmarkerLock) {
			benchmarker = newBenchmarker;
		}

		return benchmarkFinished;
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
		return getBenchmarker().flatMap(Benchmarker::getResult);
	}

	/**
	 * Attempt to clear the benchmark result.
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean clearBenchResult() {
		synchronized (benchmarkerLock) {
			if (benchmarker != null && benchmarker.getResult().isPresent()) {
				benchmarker = null;
				return true;
			} else {
				return false;
			}
		}
	}

	public void abortCurrentRun() {
		getBenchmarker().ifPresent(Benchmarker::abort);
	}

	public Optional<UUID> getCurrentRunId() {
		return getBenchmarker().map(Benchmarker::getTaskId);
	}

	private Optional<Benchmarker> getBenchmarker() {
		synchronized (benchmarkerLock) {
			return Optional.ofNullable(benchmarker);
		}
	}

	public Path getBenchRepoTmpPath() {
		return benchRepoDir.getTmpFilePath();
	}

	public Path getTaskRepoTmpPath() {
		return taskRepoDir.getTmpFilePath();
	}

	@Override
	public String toString() {
		return "TeleBackend{" +
			"address=" + address +
			'}';
	}
}
