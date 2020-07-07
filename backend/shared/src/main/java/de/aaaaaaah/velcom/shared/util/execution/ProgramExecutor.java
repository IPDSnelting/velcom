package de.aaaaaaah.velcom.shared.util.execution;

import de.aaaaaaah.velcom.shared.util.StringOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A small utility for executing programs.
 */
public class ProgramExecutor {

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
				}

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
			}))
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
