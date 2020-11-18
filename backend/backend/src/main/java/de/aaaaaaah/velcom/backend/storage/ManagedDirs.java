package de.aaaaaaah.velcom.backend.storage;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VelCom's files are split up across the {@code data/}, {@code cache/} and {@code tmp/}
 * directories, in addition to the config file. Files in these directories should never be modified
 * or deleted while VelCom is running.
 *
 * <p> The {@code data/} directory contains the current state of VelCom. Files in this directory
 * should never be deleted by an admin, only by VelCom itself. Only the config file and this
 * directory need to be included in backups.
 *
 * <p> The {@code cache/} directory contains longer-lived information that can be deleted without
 * loss of data but may be costly to create again.
 *
 * <p> The {@code tmp/} directory contains temporary files that VelCom deletes again when it
 * doesn't need them any more.
 */
public class ManagedDirs {

	private static final Logger LOGGER = LoggerFactory.getLogger(ManagedDirs.class);

	private final Path dataDir;
	private final Path cacheDir;
	private final Path tmpDir;

	public ManagedDirs(@Nullable Path dataDir, @Nullable Path cacheDir, @Nullable Path tmpDir) {
		if (dataDir != null) {
			this.dataDir = dataDir;
		} else {
			this.dataDir = Path.of("data/");
		}

		if (cacheDir != null) {
			this.cacheDir = cacheDir;
		} else {
			this.cacheDir = Path.of("cache/");
		}

		if (tmpDir != null) {
			this.tmpDir = tmpDir;
		} else {
			this.tmpDir = Path.of("tmp/");
		}

		LOGGER.info("Data dir:  " + this.dataDir.toAbsolutePath());
		LOGGER.info("Cache dir: " + this.cacheDir.toAbsolutePath());
		LOGGER.info("Tmp dir:   " + this.tmpDir.toAbsolutePath());
	}

	/**
	 * Create the directories as specified in the class docstring (if necessary) and make sure they
	 * only contain the allowed files and directories. Does <em>not</em> create any allowed files or
	 * directories that are absent.
	 *
	 * @throws IOException if the directories could not be properly created or cleaned
	 */
	public void createAndCleanDirs() throws IOException {
		// Clean up data dir
		onlyKeepAllowed(
			dataDir,
			Set.of("tars"),
			Set.of("data.db", "data.db-shm", "data.db-wal")
		);

		// Clean up cache dir
		onlyKeepAllowed(
			cacheDir,
			Set.of("repos"),
			Set.of()
		);

		// Clean up tmp dir
		onlyKeepAllowed(
			tmpDir,
			Set.of(),
			Set.of()
		);
	}

	private void onlyKeepAllowed(Path dir, Set<String> allowedTopLevelDirs,
		Set<String> allowedTopLevelFiles) throws IOException {

		LOGGER.debug("Cleaning up {}", dir);

		Files.createDirectories(dir);

		// Using this collection and foreach instead of using the stream directly because of the
		// possible IOExceptions inside the for loop.
		List<Path> children = Files.list(dir).collect(Collectors.toList());
		for (Path child : children) {
			String name = child.getFileName().toString();

			if (Files.isDirectory(child)) {
				if (!allowedTopLevelDirs.contains(name)) {
					LOGGER.info("Removing dir {}", child);
					FileHelper.deleteDirectoryOrFile(child);
				}
			} else {
				if (!allowedTopLevelFiles.contains(name)) {
					LOGGER.info("Removing file {}", child);
					FileHelper.deleteDirectoryOrFile(child);
				}
			}
		}
	}

	public String getJdbcUrl() {
		return "jdbc:sqlite:file:" + dataDir.resolve("data.db").toAbsolutePath();
	}

	public Path getTarsDir() {
		return dataDir.resolve("tars/");
	}

	public Path getReposDir() {
		return cacheDir.resolve("repos/");
	}

	public Path getArchivesDir() {
		return tmpDir.resolve("archives/");
	}
}
