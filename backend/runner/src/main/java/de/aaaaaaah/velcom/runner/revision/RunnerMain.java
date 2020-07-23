package de.aaaaaaah.velcom.runner.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnerMain {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerMain.class);

	public static void main(String[] args) throws InterruptedException {
		RunnerCliSpec cliSpec = new RunnerCliSpec_Parser().parseOrExit(args);
		RunnerConfig config = loadConfig(cliSpec.configFileLocation());

		AtomicReference<Status> globalStatus = new AtomicReference<>(Status.IDLE);

		List<TeleBackend> backends = config.getBackends().stream()
			.map(entry -> new TeleBackend(
				globalStatus,
				entry.getAddress(),
				config.getName(),
				entry.getToken(),
				entry.getDirectory())
			)
			.collect(Collectors.toList());

		backends.forEach(backend -> new Thread(() -> {
			try {
				backend.run();
			} catch (InterruptedException e) {
				die(e, "Thread running " + backend + " was interrupted. This should never happen.");
			}
		}).start());

		//noinspection InfiniteLoopStatement
		while (true) {
			LOGGER.debug("Beginning new backend roundtrip");

			for (TeleBackend backend : backends) {
				LOGGER.debug("Asking " + backend + " for a benchmark");
				Future<Void> future = backend.maybePerformBenchmark();

				if (future != null) {
					LOGGER.info(backend + " has a benchmark and/or bench repo update");
					try {
						future.get();
					} catch (ExecutionException e) {
						LOGGER.warn("Error executing benchmark from " + backend + ":", e);
					}
				} else {
					LOGGER.debug(backend + " has no benchmark");
				}
			}

			//noinspection BusyWait
			LOGGER.debug("Waiting a bit before beginning new backend roundtrip");
			Thread.sleep(Delays.BACKEND_ROUNDTRIP.toMillis());
		}
	}

	@Nonnull
	private static RunnerConfig loadConfig(Path configFilePath) {
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule());

		try {
			return objectMapper.readValue(configFilePath.toFile(), RunnerConfig.class);
		} catch (IOException e) {
			die(e, "Could not load config file at path " + configFilePath);
			// never reached
			//noinspection ConstantConditions
			return null;
		}
	}

	/**
	 * Die with an error message and an exception. Always dies with exit code 1.
	 *
	 * @param e the exception to print
	 * @param message the message to print
	 */
	public static void die(Throwable e, String message) {
		System.out.println(message);
		System.out.println(e.toString());
		System.exit(1);
	}

}
