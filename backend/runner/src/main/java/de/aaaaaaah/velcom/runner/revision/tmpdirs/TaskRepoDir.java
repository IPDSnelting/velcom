package de.aaaaaaah.velcom.runner.revision.tmpdirs;

import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.nio.file.Path;

public class TaskRepoDir {

	private final Path dirPath;

	public TaskRepoDir(Path dirPath) {
		this.dirPath = dirPath;
	}

	public void clear() throws IOException {
		FileHelper.deleteDirectoryOrFile(dirPath);
	}
}