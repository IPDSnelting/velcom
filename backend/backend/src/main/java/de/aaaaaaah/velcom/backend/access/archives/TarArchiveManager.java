package de.aaaaaaah.velcom.backend.access.archives;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages archives for tar files.
 */
public class TarArchiveManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TarArchiveManager.class);

	private final Path rootDir;

	public TarArchiveManager(Path rootDir) throws IOException {
		this.rootDir = rootDir;
		Files.createDirectories(rootDir);
	}

	public Path create(/* tar file */) {
		return getArchivePath();
	}

	public Path getArchivePath(/* tar details */) {
		throw new UnsupportedOperationException();
	}

	public void deleteArchive(/* tar details */) {
		throw new UnsupportedOperationException();
	}

	public void deleteAll() throws IOException {
		FileHelper.deleteDirectoryOrFile(rootDir);
		Files.createDirectories(rootDir);
	}

}
