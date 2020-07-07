package de.aaaaaaah.velcom.runner.entity.execution;

import de.aaaaaaah.velcom.runner.entity.BenchmarkFailureInformation;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
import de.aaaaaaah.velcom.shared.util.execution.ProgramExecutor;
import de.aaaaaaah.velcom.shared.util.execution.ProgramResult;
import de.aaaaaaah.velcom.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.shared.protocol.exceptions.ProgramCancelledException;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import de.aaaaaaah.velcom.shared.util.compression.TarHelper;
import de.aaaaaaah.velcom.runner.util.ExceptionHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes work based on the benchmark script specification.
 */
public class BenchmarkscriptWorkExecutor implements WorkExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkscriptWorkExecutor.class);

	private final BenchmarkScriptOutputParser benchmarkScriptOutputParser;
	private Future<ProgramResult> programResult;
	private AtomicInteger currentWorkIdentifier;

	public BenchmarkscriptWorkExecutor() {
		benchmarkScriptOutputParser = new BenchmarkScriptOutputParser();
		currentWorkIdentifier = new AtomicInteger();
	}

	@Override
	public AbortionResult abortExecution(String reason) {
		currentWorkIdentifier.incrementAndGet();

		if (programResult != null && !programResult.isDone()) {
			programResult.cancel(true);
			return AbortionResult.CANCEL_IN_FUTURE;
		}
		return AbortionResult.CANCEL_RIGHT_NOW;
	}

	@Override
	public int getCancelNonce() {
		return currentWorkIdentifier.get();
	}

	@Override
	public void startExecution(Path workPath, RunnerWorkOrder workOrder,
		RunnerConfiguration configuration, int cancelNonce) {
		BenchmarkFailureInformation failureInformation = new BenchmarkFailureInformation();
		failureInformation.addToGeneral("Runner name", '"' + configuration.getRunnerName() + '"');
		failureInformation.addMachineInfo();
		failureInformation.addToGeneral("Bench-Repo Hash",
			configuration.getBenchmarkRepoOrganizer().getHeadHash().orElse("<unknown>")
		);
		failureInformation.addToGeneral("User", System.getProperty("user.name"));

		Instant startTime = Instant.now();

		try (var execEnv = new ExecutionEnv(workPath, configuration.getBenchmarkRepoOrganizer())) {
			String[] calledCommand = {
				execEnv.getExecutablePath().toAbsolutePath().toString(),
				execEnv.unpack(workOrder).toAbsolutePath().toString()
			};

			failureInformation.addEscapedArrayToGeneral("Executed command", calledCommand);
			failureInformation.addToGeneral("Start time", startTime.toString());

			if (cancelNonce != currentWorkIdentifier.get()) {
				LOGGER.info("Cancel nonce mismatch, skipping execution for {}", workOrder);
				configuration.getRunnerStateMachine().backToIdle(configuration);
				return;
			}

			programResult = new ProgramExecutor().execute(calledCommand);

			ProgramResult result = programResult.get();

			Instant endTime = Instant.now();

			failureInformation.addToGeneral("Stop time", endTime.toString());
			failureInformation.addToGeneral("Execution time", result.getRuntime().toString());
			failureInformation.addToGeneral("Exit code", result.getExitCode() + "");
			failureInformation.addSection(
				"Stdout",
				result.getStdOut().isEmpty() ? "<empty>" : result.getStdOut()
			);
			failureInformation.addSection(
				"Stderr",
				result.getStdErr().isEmpty() ? "<empty>" : result.getStdErr()
			);

			if (result.getExitCode() != 0) {
				failureInformation.addSection(
					"Reason",
					"The benchmark script terminated with a non-zero exit code"
						+ " (" + result.getExitCode() + ")!"
				);
				LinuxSignal.forExitCode(result.getExitCode()).ifPresent(signal -> {
					failureInformation.addSection(
						"Exit code interpretation",
						"The exit code looks like the linux signal " + signal.name() + ".\n"
							+ "It's signal number " + signal.getNumber() + " and it is caused by '"
							+ signal.getExplanation() + "'."
					);
				});
				configuration.getRunnerStateMachine().onWorkDone(
					new BenchmarkResults(
						workOrder,
						failureInformation.toString(),
						startTime,
						endTime
					),
					configuration
				);
				return;
			}

			BareResult bareResult = benchmarkScriptOutputParser.parse(result.getStdOut());

			String error = bareResult.getError();
			if (error != null) {
				failureInformation.addSection("Benchmark script error", error);
				failureInformation.addSection(
					"Reason",
					"The benchmark script terminated successfully but returned an error message!"
				);
				error = failureInformation.toString();
			}

			BenchmarkResults results = new BenchmarkResults(
				workOrder,
				bareResult.getBenchmarks(),
				error,
				startTime, endTime
			);

			configuration.getRunnerStateMachine().onWorkDone(results, configuration);
		} catch (OutputParseException e) {
			LOGGER.info("Benchmark script returned invalid data: '{}'", e.getMessage());
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection("Invalid output", e.getMessage());
			failureInformation.addSection(
				"Reason",
				"The benchmark script returned invalid output!"
			);

			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(
					workOrder,
					failureInformation.toString(),
					startTime,
					Instant.now()
				),
				configuration
			);
		} catch (ProgramCancelledException e) {
			LOGGER.info("Program for order {} aborted for reason '{}'!", workOrder, e.getReason());
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection(
				"Reason",
				"The benchmark process was explicitly aborted. (Reason " + e.getReason() + ")"
			);

			BenchmarkResults results = new BenchmarkResults(
				workOrder,
				failureInformation.toString(),
				startTime,
				Instant.now()
			);
			if (StatusCodeMappings.discardResults(e.getReason())) {
				results = null;
			}
			configuration.getRunnerStateMachine().onWorkDone(results, configuration);
		} catch (Exception e) {
			LOGGER.info("Error executing some work for {}", workOrder);
			LOGGER.info("Stacktrace:", e);

			failureInformation.addSection("Stacktrace", ExceptionHelper.getStackTrace(e));
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection(
				"Reason",
				"Maybe an internal runner error. Rebenchmarking might solve the problem!"
			);

			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(
					workOrder,
					failureInformation.toString(),
					startTime,
					Instant.now()
				),
				configuration
			);
		} finally {
			currentWorkIdentifier.incrementAndGet();
		}
	}

	private static class ExecutionEnv implements AutoCloseable {

		private Path workBinaryPath;
		private Path executablePath;
		private Path unarchivedWorkPath;

		private ExecutionEnv(Path workBinaryPath, BenchmarkRepoOrganizer benchmarkRepoOrganizer)
			throws IOException {
			this.workBinaryPath = workBinaryPath;
			this.executablePath = benchmarkRepoOrganizer.getBenchmarkScript();

			if (!Files.isExecutable(executablePath)) {
				throw new RuntimeException(
					"Benchmark script file is not executable: " + executablePath.toAbsolutePath()
				);
			}

			FileHelper.deleteOnExit(workBinaryPath);
		}

		public Path unpack(RunnerWorkOrder workOrder) throws IOException {
			unarchivedWorkPath = Files.createTempDirectory(workOrder.getRepoId().toString());
			TarHelper.untar(workBinaryPath, unarchivedWorkPath);
			FileHelper.deleteOnExit(unarchivedWorkPath);

			return unarchivedWorkPath;
		}

		public Path getExecutablePath() {
			return executablePath;
		}

		@Override
		public void close() throws IOException {
			FileHelper.deleteDirectoryOrFile(workBinaryPath);
			FileHelper.deleteDirectoryOrFile(unarchivedWorkPath);
		}
	}
}
