package de.aaaaaaah.velcom.runner.tmpdirs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchRepoDirTest {

	@TempDir
	Path tempFolder;

	private Path benchRepoDir;
	private BenchRepoDir dir;

	@BeforeEach
	void setUp() throws IOException {
		benchRepoDir = tempFolder.resolve("benchrepo");
		dir = new BenchRepoDir(benchRepoDir);
	}

	@Test
	void readsNoHash() {
		assertThat(dir.getHash()).isEmpty();
	}

	@Test
	void writesCorrectHash() throws IOException {
		String hash = "12345";
		dir.setHash(hash);

		assertThat(dir.getHash()).isPresent().contains(hash);
	}

	@Test
	void readsCorrectHash() throws IOException {
		String hash = "12345";
		dir.setHash(hash);

		assertThat(new BenchRepoDir(benchRepoDir).getHash()).isPresent().contains(hash);
	}

	@Test
	void clearResetsHash() throws IOException {
		String hash = "12345";
		dir.setHash(hash);

		dir.clear();

		assertThat(dir.getHash()).isEmpty();
	}

	@Test
	void clearDeletesRepo() throws IOException {
		Files.createDirectory(benchRepoDir);

		Files.writeString(benchRepoDir.resolve("file"), "Hello world");
		Files.createDirectory(benchRepoDir.resolve("directory"));
		Files.createDirectory(benchRepoDir.resolve("non-empty-directory"));
		Files.writeString(
			benchRepoDir.resolve("non-empty-directory").resolve("test.tx"),
			"Hello world"
		);

		assertThat(benchRepoDir).isNotEmptyDirectory();

		dir.clear();

		assertThat(benchRepoDir).doesNotExist();
	}
}
