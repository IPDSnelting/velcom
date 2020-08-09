package de.aaaaaaah.velcom.shared.util.execution;

import de.aaaaaaah.velcom.shared.util.StringOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small utility for executing programs.
 */
public class ProgramExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgramExecutor.class);

	// If multiple ProgramExecutors are used, could block the common fork join pool completely.
	// We occupy 2 threads per execution (draining stderr and stdio)
	// Using a cached thread pool allows the executor to scale, while also trimming down unused
	// tasks after some delay specified in the JDK method (currently 60 seconds).
	// As the threads are daemons, we do not need to shut down the pool at all. If nobody waits for
	// the result, the threads will die as well, otherwise they will be kept alive and things work
	// as expected.
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(
		new DaemonThreadFactory()
	);

	private final long timeToForceKillMillis;

	public ProgramExecutor() {
		this.timeToForceKillMillis = TimeUnit.SECONDS.toMillis(10);
	}

	public ProgramExecutor(long timeToForceKillMillis) {
		this.timeToForceKillMillis = timeToForceKillMillis;
	}

	/**
	 * Executes a program using the passed command. If the future is cancelled <em>with
	 * interruption</em>, the process is forcefully killed.
	 *
	 * @param command the command to execute
	 * @return a future representing the result
	 */
	public Future<ProgramResult> execute(String... command) {
		FutureTask<ProgramResult> futureTask = new FutureTask<>(() -> {
			Instant startTime = Instant.now();

			Process process = new ProcessBuilder(command).start();

			CompletableFuture<String> stdOut = readOutput(process::getInputStream);
			CompletableFuture<String> stdErr = readOutput(process::getErrorStream);

			try {
				int exitCode = process.waitFor();

				return new ProgramResult(
					exitCode,
					stdOut.get(),
					stdErr.get(),
					Duration.between(startTime, Instant.now())
				);
			} catch (InterruptedException e) {
				process.toHandle().destroy();

				try {
					process.waitFor(timeToForceKillMillis, TimeUnit.MILLISECONDS);
				} catch (InterruptedException ignored) {
					LOGGER.warn(
						"Interrupted while waiting for process to gracefully shutdown. Killing it");
				}
				LOGGER.debug("Waited " + timeToForceKillMillis + " killing it");

				process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
				process.toHandle().destroyForcibly();

				throw new CancellationException("Killed process");
			}
		});

		return new WaitingFutureTask<>(futureTask);
	}

	private CompletableFuture<String> readOutput(UncheckedSupplier<InputStream> input) {
		StringOutputStream outputStream = new StringOutputStream();
		return CompletableFuture
			.supplyAsync(asUnchecked(() -> {
					try (InputStream inputStream = input.get()) {
						inputStream.transferTo(outputStream);
					}
					return outputStream.getString();
				}),
				EXECUTOR
			)
			.exceptionally(throwable -> outputStream.getString());
	}

	private <T> Supplier<T> asUnchecked(UncheckedSupplier<T> supplier) {
		return () -> {
			try {
				return supplier.get();
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		};
	}

	private interface UncheckedSupplier<T> {

		T get() throws Exception;
	}

}
