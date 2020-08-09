package de.aaaaaaah.velcom.runner.tmpdirs;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Path;

public class TaskRepoDir {

	private final Path dirPath;
	private final Path tmpFilePath;

	public TaskRepoDir(Path dirPath) {
		this.dirPath = dirPath;
		tmpFilePath = dirPath.getParent().resolve(dirPath.getFileName() + ".tmp");
	}

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
