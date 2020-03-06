package de.aaaaaaah.velcom.runner.entity.execution;

import de.aaaaaaah.velcom.runner.entity.BenchmarkFailureInformation;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.FutureProgramResult;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.ProgramResult;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.util.compression.FileHelper;
import de.aaaaaaah.velcom.runner.shared.util.compression.TarHelper;
import de.aaaaaaah.velcom.runner.util.ExceptionHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes work based on the benchmark script specification.
 */
public class BenchmarkscriptWorkExecutor implements WorkExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkscriptWorkExecutor.class);

	private final BenchmarkScriptOutputParser benchmarkScriptOutputParser;
	private final ReentrantLock cancelLock;
	private FutureProgramResult programResult;
	private volatile boolean cancelled;

	public BenchmarkscriptWorkExecutor() {
		benchmarkScriptOutputParser = new BenchmarkScriptOutputParser();
		cancelLock = new ReentrantLock();
	}

	@Override
	public AbortionResult abortExecution() {
		cancelLock.lock();
		cancelled = true;
		if (programResult != null && !programResult.isDone()) {
			programResult.cancel();
			cancelLock.unlock();
			return AbortionResult.CANCEL_IN_FUTURE;
		}
		cancelLock.unlock();
		return AbortionResult.CANCEL_RIGHT_NOW;
	}

	@Override
	public void startExecution(Path workPath, RunnerWorkOrder workOrder,
		RunnerConfiguration configuration) {
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

			cancelLock.lock();
			if (cancelled) {
				cancelLock.unlock();
				return;
			}
			programResult = new ProgramExecutor().execute(calledCommand);
			cancelLock.unlock();

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
		} catch (CancellationException e) {
			LOGGER.info("Program for order {} aborted!", workOrder);
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection(
				"Reason",
				"The benchmark process was manually aborted!"
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
			cancelLock.lock();
			cancelled = false;
			cancelLock.unlock();
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
		}

		public Path unpack(RunnerWorkOrder workOrder) throws IOException {
			unarchivedWorkPath = Files.createTempDirectory(workOrder.getRepoId().toString());
			TarHelper.untar(workBinaryPath, unarchivedWorkPath);

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
