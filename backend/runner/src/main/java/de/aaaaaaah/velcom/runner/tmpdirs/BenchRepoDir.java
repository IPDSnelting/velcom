package de.aaaaaaah.velcom.runner.tmpdirs;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the directory where the bench repo is stored, as well as the file containing
 * the bench repo's hash and a temporary file used while downloading and unpacking a new bench
 * repo.
 */
public class BenchRepoDir {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchRepoDir.class);

	private final Path dirPath;
	private final Path tmpFilePath;
	private final Path hashFilePath;

	@Nullable
	private String currentHash;

	public BenchRepoDir(Path dirPath) throws IOException {
		this.dirPath = dirPath;
		this.tmpFilePath = dirPath.getParent().resolve(dirPath.getFileName() + ".tmp");
		this.hashFilePath = dirPath.getParent().resolve(dirPath.getFileName() + ".hash");

		currentHash = readHash();
	}

	@Nullable
	private String readHash() throws IOException {
		try {
			LOGGER.debug("Trying to read hash from file {}", hashFilePath);
			FileReader reader = new FileReader(hashFilePath.toFile());
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			if (line == null) {
				LOGGER.debug("Could not read hash, file was empty");
				return null;
			}

			String hash = line.strip();
			LOGGER.debug("Read hash {}", hash);
			return hash;
		} catch (FileNotFoundException e) {
			LOGGER.debug("Could not read hash, file not found");
			return null;
		}
	}

	private void writeHash(@Nullable String hash) throws IOException {
		if (hash == null) {
			Files.deleteIfExists(hashFilePath);
			return;
		}
		FileWriter writer = new FileWriter(hashFilePath.toFile());
		writer.write(hash);
		writer.flush();
	}

	/**
	 * Delete the directory and hash file.
	 *
	 * @throws IOException if something io-related goes wrong during the deletion
	 */
	public void clear() throws IOException {
		setHash(null);
		FileHelper.deleteDirectoryOrFile(dirPath);
	}

	public Path getDirPath() {
		return dirPath;
	}

	public Path getTmpFilePath() {
		return tmpFilePath;
	}

	public void setHash(@Nullable String hash) throws IOException {
		currentHash = hash;
		writeHash(currentHash);
	}

	public Optional<String> getHash() {
		return Optional.ofNullable(currentHash);
	}

}
