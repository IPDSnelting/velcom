package de.aaaaaaah.velcom.backend.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryRemoverTest {

	@TempDir
	Path tempFolder;

	@Test
	void canDeleteEmptyDir() throws IOException {
		Path dirPath = tempFolder.resolve("test");
		Files.createDirectory(dirPath);

		DirectoryRemover.deleteDirectoryRecursive(dirPath);

		assertFalse(
			Files.exists(dirPath), "Dir not deleted"
		);
	}

	@Test
	void canDeleteDirWithFiles() throws IOException {
		Path dirPath = tempFolder.resolve("test");
		Files.createDirectory(dirPath);

		for (int i = 0; i < 10; i++) {
			Files.writeString(dirPath.resolve("hey" + i), "Test " + i);
		}

		DirectoryRemover.deleteDirectoryRecursive(dirPath);

		assertFalse(
			Files.exists(dirPath), "Dir not deleted"
		);
	}

	@Test
	void canDeleteDirWithReadOnlyFiles() throws IOException {
		Path dirPath = tempFolder.resolve("test");
		Files.createDirectory(dirPath);

		for (int i = 0; i < 10; i++) {
			Path file = dirPath.resolve("hey" + i);
			Files.writeString(file, "Test " + i);
			file.toFile().setWritable(false);
		}

		DirectoryRemover.deleteDirectoryRecursive(dirPath);

		assertFalse(
			Files.exists(dirPath), "Dir not deleted"
		);
	}

	@Test
	void canDeleteDirWithNestedDir() throws IOException {
		Path dirPath = tempFolder.resolve("test");
		Files.createDirectory(dirPath);

		for (int i = 0; i < 10; i++) {
			Path subDir = dirPath.resolve("hey" + i);
			Files.createDirectory(subDir);
			Files.writeString(subDir.resolve("hey" + i), "Test " + i);
		}

		DirectoryRemover.deleteDirectoryRecursive(dirPath);

		assertFalse(
			Files.exists(dirPath), "Dir not deleted"
		);
	}

}