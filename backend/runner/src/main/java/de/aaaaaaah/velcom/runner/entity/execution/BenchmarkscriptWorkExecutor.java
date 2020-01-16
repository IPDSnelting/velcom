package de.aaaaaaah.velcom.runner.entity.execution;

import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.entity.execution.ProgramExecutor.ProgramResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
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
		Instant startTime = Instant.now();

		try (var execEnv = new ExecutionEnv(workPath, configuration.getBenchmarkRepoOrganizer())) {
			// Delay a bit for testing
			Thread.sleep(5000);

			programResult = new ProgramExecutor().execute(
				execEnv.getExecutablePath().toAbsolutePath().toString(),
				execEnv.unpack(workOrder).toAbsolutePath().toString()
			);
			Instant endTime = Instant.now();

			ProgramResult result = programResult.get();

			if (result.getExitCode() != 0) {
				configuration.getRunnerStateMachine().onWorkDone(
					new BenchmarkResults(
						workOrder,
						"Non-zero exit code: " + result.getExitCode()
							+ "\n\nOutput       : " + result.getStdOut()
							+ "\n\nError Output : " + result.getStdErr()
							+ "\n\nRan for      : " + result.getRuntime(),
						startTime,
						endTime
					),
					configuration
				);
				return;
			}

			BareResult bareResult = benchmarkScriptOutputParser.parse(result.getStdOut());

			BenchmarkResults results = new BenchmarkResults(
				workOrder,
				bareResult.getBenchmarks(),
				bareResult.getError(),
				startTime, endTime
			);

			configuration.getRunnerStateMachine().onWorkDone(results, configuration);
		} catch (IOException | ExecutionException | InterruptedException e) {
			LOGGER.info("Error executing some work for {}", workOrder);
			LOGGER.info("Stacktrace:", e);
			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(workOrder, ExceptionHelper.getStackTrace(e),
					startTime,
					Instant.now()
				),
				configuration
			);
		} catch (OutputParseException e) {
			LOGGER.info("Benchmark script returned invalid data: '{}'", e.getMessage());
			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(
					workOrder,
					"Error processing runner output: " + e.getMessage(),
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
