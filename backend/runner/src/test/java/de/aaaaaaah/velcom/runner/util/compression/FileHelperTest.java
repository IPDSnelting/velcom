package de.aaaaaaah.velcom.runner.util.compression;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FileHelperTest {

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