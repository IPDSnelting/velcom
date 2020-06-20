package de.aaaaaaah.velcom.runner.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import net.jbock.Command;
import net.jbock.Param;

/**
 * The runner for VelCom that executes benchmarks.
 *
 * Made by Aaaaaaah!
 */
@Command(value = "VelCom-Runner")
@SuppressWarnings("CheckStyle")
public abstract class RunnerCliSpec {

	/**
	 * The path to the config file
	 */
	@Param(value = 1, mappedBy = ConfigFilePathMapper.class)
	public abstract Path configFileLocation();

	/**
	 * A mapper for the config file location.
	 */
	public static class ConfigFilePathMapper implements Function<String, Path> {

		@Override
		public Path apply(String name) {
			Path path = Paths.get(name);
			if (Files.notExists(path)) {
				throw new IllegalArgumentException(
					"The given path (" + path.toAbsolutePath() + ") does not exist!"
				);
			}
			if (!Files.isRegularFile(path)) {
				throw new IllegalArgumentException(
					"The given path (" + path.toAbsolutePath() + ") is no regular file!"
				);
			}
			if (!Files.isReadable(path)) {
				throw new IllegalArgumentException(
					"The given path (" + path.toAbsolutePath() + ") is not readable by me!"
				);
			}
			return path.toAbsolutePath();
		}
	}
}
