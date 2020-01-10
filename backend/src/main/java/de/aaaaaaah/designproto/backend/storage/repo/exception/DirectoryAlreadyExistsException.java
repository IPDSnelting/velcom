package de.aaaaaaah.designproto.backend.storage.repo.exception;

import java.nio.file.Path;
import java.util.Objects;

/**
 * An exception that is thrown when a certain directory already exists.
 */
public class DirectoryAlreadyExistsException extends Exception {

	private final Path directory;

	/**
	 * Constructs a new {@link DirectoryAlreadyExistsException}.
	 *
	 * @param directory the directory that already exists
	 */
	public DirectoryAlreadyExistsException(Path directory) {
		this.directory = Objects.requireNonNull(directory);
	}

	/**
	 * @return Returns the directry that already exists
	 */
	public Path getDirectory() {
		return directory;
	}

}
