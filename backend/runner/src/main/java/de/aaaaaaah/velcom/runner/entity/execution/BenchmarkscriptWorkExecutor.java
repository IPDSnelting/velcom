package de.aaaaaaah.velcom.runner.entity.execution;

import de.aaaaaaah.velcom.runner.entity.BenchmarkFailureInformation;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.ProgramResult;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.util.ExceptionHelper;
import de.aaaaaaah.velcom.runner.util.compression.FileHelper;
import de.aaaaaaah.velcom.runner.util.compression.TarHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes work based on the benchmark script specification.
 */
public class BenchmarkscriptWorkExecutor implements WorkExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkscriptWorkExecutor.class);

	private final BenchmarkScriptOutputParser benchmarkScriptOutputParser;
	private Future<ProgramResult> programResult;

	public BenchmarkscriptWorkExecutor() {
		benchmarkScriptOutputParser = new BenchmarkScriptOutputParser();
	}

	@Override
	public void abortExecution() {
		if (programResult != null && !programResult.isDone()) {
			programResult.cancel(true);
		}
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
					"The Benchmark-Script terminated with a non-zero exit code"
						+ " (" + result.getExitCode() + ")!"
				);
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
				failureInformation.addSection("Benchmark-Script error", error);
				failureInformation.addSection(
					"Reason",
					"The Benchmark-Script terminated successfully but returned an error message!"
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
		} catch (IOException | ExecutionException | InterruptedException e) {
			LOGGER.info("Error executing some work for {}", workOrder);
			LOGGER.info("Stacktrace:", e);

			failureInformation.addSection("Stacktrace", ExceptionHelper.getStackTrace(e));
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection(
				"Reason",
				"Most likely an internal runner error. Rebenchmarking might solve the problem!"
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
		} catch (OutputParseException e) {
			LOGGER.info("Benchmark script returned invalid data: '{}'", e.getMessage());
			failureInformation.addSection("End time", Instant.now().toString());
			failureInformation.addSection("Invalid output", e.getMessage());
			failureInformation.addSection(
				"Reason",
				"The Benchmark-Script returned invalid output!"
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
