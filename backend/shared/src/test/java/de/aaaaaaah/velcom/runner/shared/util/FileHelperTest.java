package de.aaaaaaah.velcom.runner.shared.util;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHelperTest {

	@TempDir
	Path tempFolder;

	@Test
	void canDeleteEmptyDir() throws IOException {
		Path dirPath = tempFolder.resolve("test");
		Files.createDirectory(dirPath);

		FileHelper.deleteDirectoryOrFile(dirPath);

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

		FileHelper.deleteDirectoryOrFile(dirPath);

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

		FileHelper.deleteDirectoryOrFile(dirPath);

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

		FileHelper.deleteDirectoryOrFile(dirPath);

		assertFalse(
			Files.exists(dirPath), "Dir not deleted"
		);
	}

	@Test
	void deleteFile() throws IOException {
		Path path = Files.createTempFile("test", "suff");
		try {
			FileHelper.deleteDirectoryOrFile(path);
			assertThat(Files.notExists(path))
				.withFailMessage("Did not delete file!")
				.isTrue();
		} finally {
			Files.deleteIfExists(path);
		}
	}

	@Test
	void deleteEmptyDirectory() throws IOException {
		Path path = Files.createTempDirectory("test");
		try {
			FileHelper.deleteDirectoryOrFile(path);
			assertThat(Files.notExists(path))
				.withFailMessage("Did not delete directory!")
				.isTrue();
		} finally {
			Files.deleteIfExists(path);
		}
	}

	@Test
	void deleteNotEmptyDirectory() throws IOException {
		Path path = Files.createTempDirectory("test");
		Path innerFile = Files.createFile(path.resolve("hey"));
		try {
			FileHelper.deleteDirectoryOrFile(path);
			assertThat(Files.notExists(path))
				.withFailMessage("Did not delete directory!")
				.isTrue();
		} finally {
			Files.deleteIfExists(innerFile);
			Files.deleteIfExists(path);
		}
	}

	@Test
	void deleteNotExistingFile() throws IOException {
		Path path = Files.createTempFile("test", "hey");
		Files.deleteIfExists(path);
		FileHelper.deleteDirectoryOrFile(path);
	}

	@Test
	void uncheckedDeleteNotEmptyDirectory() throws IOException {
		Path path = Files.createTempDirectory("test");
		Path innerFile = Files.createFile(path.resolve("hey"));
		try {
			FileHelper.uncheckedDeleteDirectoryOrFile(path);
			assertThat(Files.notExists(path))
				.withFailMessage("Did not delete directory!")
				.isTrue();
		} finally {
			Files.deleteIfExists(innerFile);
			Files.deleteIfExists(path);
		}
	}

}