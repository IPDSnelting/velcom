package de.aaaaaaah.velcom.runner.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.nio.file.Path;

public class RunnerMain {

	public static void main(String[] args) {
		RunnerCliSpec cliSpec = new RunnerCliSpec_Parser().parseOrExit(args);
		RunnerConfig config = loadConfig(cliSpec.configFileLocation());
		System.out.println(config);
	}

	private static RunnerConfig loadConfig(Path configFilePath) {
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule());

		try {
			return objectMapper.readValue(configFilePath.toFile(), RunnerConfig.class);
		} catch (IOException e) {
			die(e, "Could not load config file at path " + configFilePath);
			// never reached
			return null;
		}
	}

	private static void die(Throwable e, String message) {
		System.out.println(message);
		System.out.println(e.toString());
		System.exit(1);
	}

}
