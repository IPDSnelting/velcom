package de.aaaaaaah.velcom.runner.entity.execution;

import de.aaaaaaah.velcom.runner.util.StringOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A small utility for executing programs.
 */
public class ProgramExecutor {

	/**
	 * Executes a program using the passed command. If the future is cancelled <em>with
	 * interruption</em>, the process is forcefully killed.
	 *
	 * @param command the command to execute
	 * @return a future representing the result
	 */
	public CompletableFuture<ProgramResult> execute(String... command) {
		return CompletableFuture.supplyAsync(() -> {
				Instant startTime = Instant.now();

				Process process;
				try {
					process = new ProcessBuilder(command).start();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				ProcessHandle processHandle = process.toHandle();

				StringOutputStream stdOut = new StringOutputStream();
				StringOutputStream stdErr = new StringOutputStream();

				try (InputStream inputStream = process.getInputStream()) {
					inputStream.transferTo(stdOut);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				// TODO: 05.01.20 Does this drain stderr?
				try (InputStream errorStream = process.getErrorStream()) {
					errorStream.transferTo(stdErr);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				try {
					process.waitFor();
				} catch (InterruptedException e) {
					// We were cancelled
					processHandle.destroy();
					// Allow time for graceful shutdown
					try {
						process.waitFor(10, TimeUnit.SECONDS);
					} catch (InterruptedException ignored) {
					}
					processHandle.destroyForcibly();
				}
				return new ProgramResult(
					process.exitValue(),
					stdOut.getString(),
					stdErr.getString(),
					Duration.between(startTime, Instant.now())
				);
			},
			Executors.newSingleThreadExecutor()
		);
	}


	/**
	 * The result of executing a program.
	 */
	public static class ProgramResult {

		private final int exitCode;
		private final String stdOut;
		private final String stdErr;
		private final Duration runtime;

		public ProgramResult(int exitCode, String stdOut, String stdErr, Duration runtime) {
			this.exitCode = exitCode;
			this.stdOut = stdOut;
			this.stdErr = stdErr;
			this.runtime = runtime;
		}

		public int getExitCode() {
			return exitCode;
		}

		public String getStdOut() {
			return stdOut;
		}

		public String getStdErr() {
			return stdErr;
		}

		public Duration getRuntime() {
			return runtime;
		}
	}
}
