package de.aaaaaaah.velcom.runner.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.util.ExceptionHelper;
import de.aaaaaaah.velcom.runner.util.StringOutputStream;
import de.aaaaaaah.velcom.runner.util.compression.FileHelper;
import de.aaaaaaah.velcom.runner.util.compression.TarHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executes work based on the benchmark script specification.
 */
public class BenchmarkscriptWorkExecutor implements WorkExecutor {

	private ProcessHandle processHandle;

	@Override
	public void abortExecution() {
		if (processHandle != null && processHandle.isAlive()) {
			processHandle.destroy();
		}
	}

	@Override
	public void startExecution(Path workPath, RunnerWorkOrder workOrder,
		RunnerConfiguration configuration) {
		try {
			Path benchmarkScript = configuration.getBenchmarkRepoOrganizer().getBenchmarkScript();

			if (!Files.isExecutable(benchmarkScript)) {
				throw new RuntimeException(
					"Benchmark script file is not executable: " + benchmarkScript.toAbsolutePath()
				);
			}

			Path tempDir = Files.createTempDirectory(workOrder.getRemoteUrlIdentifier());
			TarHelper.untar(workPath, tempDir);

			Process process = new ProcessBuilder(
				benchmarkScript.toAbsolutePath().toString(),
				workPath.toAbsolutePath().toString()
			)
				.start();
			this.processHandle = process.toHandle();

			StringOutputStream stdOut = new StringOutputStream();
			StringOutputStream stdErr = new StringOutputStream();

			try (InputStream inputStream = process.getInputStream()) {
				inputStream.transferTo(stdOut);
			}

			// TODO: 05.01.20 Does this drain stderr?
			try (InputStream errorStream = process.getErrorStream()) {
				errorStream.transferTo(stdErr);
			}

			int exitCode = process.waitFor();

			processHandle = null;

			ObjectMapper objectMapper = new ObjectMapper()
				.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
				.registerModule(new ParameterNamesModule());
			BenchmarkResults results = objectMapper.readValue(
				stdOut.getString(),
				BenchmarkResults.class
			);

			System.out.println("Work done!");
			configuration.getRunnerStateMachine().onWorkDone(results, configuration);

			// Cleanup
			FileHelper.deleteDirectoryOrFile(workPath);
			FileHelper.deleteDirectoryOrFile(tempDir);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			// TODO: 16.12.19 Distinguish exception here from runner error 
			configuration.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(workOrder, ExceptionHelper.getStackTrace(e)),
				configuration
			);
		}
	}
}
