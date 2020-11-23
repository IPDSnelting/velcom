package de.aaaaaaah.velcom.backend.storage.tar;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tar file storage can store tar files associated to an identifier (usually a task/run id).
 */
public class TarFileStorage {

	private final static Logger LOGGER = LoggerFactory.getLogger(TarFileStorage.class);

	private final Path rootDir;

	/**
	 * Initialize a new tar file storage.
	 *
	 * @param rootDir the directory where the tar files will be stored in
	 * @throws IOException if the directory could not be created
	 */
	public TarFileStorage(Path rootDir) throws IOException {
		this.rootDir = rootDir;
		Files.createDirectories(rootDir);
	}

	private Path getPathOfIdentifier(String identifier) {
		return rootDir.resolve(identifier + ".tar");
	}

	private String getIdentifierOfPath(Path path) {
		String name = path.getFileName().toString();

		if (name.endsWith(".tar")) {
			return name.substring(0, name.length() - 4);
		} else {
			return name;
		}
	}

	/**
	 * Add a new tar file to the storage.
	 *
	 * @param identifier the identifier (must be a valid file name)
	 * @param inputStream the stream containing the tar file
	 * @throws IOException if such a tar file already exists for the identifier or some file
	 * 	operation went wrong
	 */
	public void storeTarFile(String identifier, InputStream inputStream) throws IOException {
		Files.copy(
			inputStream,
			getPathOfIdentifier(identifier)
		);
	}

	/**
	 * Remove a tar file from the storage if it exists. Does nothing if the file doesn't exist.
	 *
	 * @param identifier the identifier (must be a valid file name)
	 * @throws IOException if some file operation went wrong
	 */
	public void removeTarFile(String identifier) throws IOException {
		FileHelper.deleteDirectoryOrFile(getPathOfIdentifier(identifier));
	}

	/**
	 * Retrieve a tar file from the storage.
	 *
	 * @param identifier the identifier (must be a valid file name)
	 * @param outputStream the stream the tar file is written to
	 * @throws IOException if no tar file exists for the identifier or some file operation went
	 * 	wrong
	 */
	public void retrieveTarFile(String identifier, OutputStream outputStream) throws IOException {
		Files.copy(
			getPathOfIdentifier(identifier),
			outputStream
		);
	}

	/**
	 * Remove all files with unknown identifiers.
	 *
	 * @param knownIdentifiers a set of all known identifiers
	 * @throws IOException if some file operation went wrong
	 */
	public void removeUnknownFiles(Set<String> knownIdentifiers) throws IOException {
		LOGGER.debug("Removing files with unknown identifiers");

		List<Path> children = Files.list(rootDir).collect(Collectors.toList());
		for (Path child : children) {
			String identifier = getIdentifierOfPath(child);

			if (!knownIdentifiers.contains(identifier)) {
				LOGGER.info("Removing unknown file {}", child);
				FileHelper.deleteDirectoryOrFile(child);
			}
		}
	}

	/**
	 * Remove all currently stored tar files.
	 *
	 * @throws IOException if some file operation went wrong
	 */
	public void removeAllFiles() throws IOException {
		LOGGER.debug("Removing all files");

		List<Path> children = Files.list(rootDir).collect(Collectors.toList());
		for (Path child : children) {
			String identifier = getIdentifierOfPath(child);
			LOGGER.info("Removing file {}", child);
			FileHelper.deleteDirectoryOrFile(child);
		}
	}
}
