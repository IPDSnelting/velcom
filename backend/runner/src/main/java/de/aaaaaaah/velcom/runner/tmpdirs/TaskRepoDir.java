package de.aaaaaaah.velcom.runner.tmpdirs;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This class manages the directory where the task repo is stored, as well as a temporary file used
 * while downloading and unpacking a new bench repo.
 */
public class TaskRepoDir {

	private final Path dirPath;
	private final Path tmpFilePath;

	public TaskRepoDir(Path dirPath) {
		this.dirPath = dirPath;
		tmpFilePath = dirPath.getParent().resolve(dirPath.getFileName() + ".tmp");
	}

	/**
	 * Delete the directory.
	 *
	 * @throws IOException if something io-related goes wrong during the deletion
	 */
	public void clear() throws IOException {
		FileHelper.deleteDirectoryOrFile(dirPath);
	}

	public Path getDirPath() {
		return dirPath;
	}

	public Path getTmpFilePath() {
		return tmpFilePath;
	}
}
