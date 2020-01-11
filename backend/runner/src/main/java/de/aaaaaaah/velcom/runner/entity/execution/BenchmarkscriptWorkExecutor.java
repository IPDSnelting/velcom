package de.aaaaaaah.velcom.runner.entity.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.WorkExecutor;
import de.aaaaaaah.velcom.runner.entity.execution.ProgramExecutor.ProgramResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.util.ExceptionHelper;
import de.aaaaaaah.velcom.runner.util.compression.FileHelper;
import de.aaaaaaah.velcom.runner.util.compression.TarHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Executes work based on the benchmark script specification.
 */
public class BenchmarkscriptWorkExecutor implements WorkExecutor {

	private final BenchmarkScriptOutputParser benchmarkScriptOutputParser;
	private Future<ProgramResult> programResult;
	private final ObjectMapper objectMapper;

	public BenchmarkscriptWorkExecutor() {
		benchmarkScriptOutputParser = new BenchmarkScriptOutputParser();
		objectMapper = new ObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.registerModule(new ParameterNamesModule());
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
		try (var execEnv = new ExecutionEnv(workPath, configuration.getBenchmarkRepoOrganizer())) {
			programResult = new ProgramExecutor().execute(
				execEnv.getExecutablePath().toAbsolutePath().toString(),
				execEnv.unpack(workOrder).toAbsolutePath().toString()
			);

			ProgramResult result = programResult.get();

			if (result.getExitCode() != 0) {
				configuration.getRunnerStateMachine().onWorkDone(
					new BenchmarkResults(
						workOrder,
						"Non-zero exit code: " + result.getExitCode()
							+ "\nOutput       : " + result.getStdOut()
							+ "\nError Output : " + result.getStdErr()
							+ "\nRan for      : " + result.getRuntime()
					),
					configuration
				);
				return;
			}

			JsonNode resultTree = objectMapper.readTree(result.getStdOut());

			BenchmarkResults results = benchmarkScriptOutputParser.parse(workOrder, resultTree);

			configuration.getRunnerStateMachine().onWorkDone(results, configuration);
		} catch (IOException | ExecutionException | InterruptedException e) {
			System.err.println("Error executing some work (" + workOrder + ")");
			System.err.println("Message is: " + e.getMessage());
			System.err.println("Stacktrace:");
			e.printStackTrace();
			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(workOrder, ExceptionHelper.getStackTrace(e)),
				configuration
			);
		} catch (OutputParseException e) {
			System.err.println("Benchmark script returned invalid data: " + e.getMessage());
			e.printStackTrace();
			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(
					workOrder,
					"Error processing runner output: " + e.getMessage()
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
			unarchivedWorkPath = Files.createTempDirectory(workOrder.getRemoteUrlIdentifier());
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
