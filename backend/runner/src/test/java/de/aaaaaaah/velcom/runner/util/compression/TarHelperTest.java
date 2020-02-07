package de.aaaaaaah.velcom.runner.util.compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.runner.shared.util.compression.PermissionsHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TarHelperTest {

	@TempDir
	Path rootDir;
	Path resultDir;
	Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		resultDir = rootDir.resolve("res");
		tempDir = rootDir.resolve("temp");
		Files.createDirectory(tempDir);
		Files.createDirectory(resultDir);
	}

	@Test
	void testUntarInvalid() {
		assertThatThrownBy(() -> TarHelper.untar(Path.of(""), resultDir));
	}

	@Test
	void testUntar() throws IOException {
		Path test = tempDir.resolve("Test");
		Files.createFile(test);
		Files.setPosixFilePermissions(
			test,
			Set.of(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_READ
			)
		);
		Files.createDirectory(tempDir.resolve("test2"));
		Files.createFile(tempDir.resolve("test2").resolve("nested"));
		Files.createDirectory(tempDir.resolve("test3"));
		Files.createFile(tempDir.resolve("test3").resolve("nested3"));

		tarTemp();

		TarHelper.untar(tempDir.resolve("output"), resultDir);
		Files.deleteIfExists(tempDir.resolve("output"));

		List<Path> paths = Files.walk(tempDir).collect(Collectors.toList());
		for (Path path : paths) {
			Path other = resultDir.resolve(tempDir.relativize(path));
			assertThat(Files.exists(other)).isTrue();
			assertThat(Files.size(other)).isEqualTo(Files.size(path));
			assertThat(Files.getPosixFilePermissions(other))
				.containsExactlyElementsOf(
					Files.getPosixFilePermissions(path)
				);
		}
	}

	private void tarTemp() throws IOException {
		TarArchiveOutputStream output = new TarArchiveOutputStream(
			Files.newOutputStream(tempDir.resolve("output"))
		);
		int dirCounter = 0;
		try (output) {
			List<Path> files = Files.walk(tempDir)
				.filter(Predicate.not(tempDir.resolve("output")::equals))
				.collect(Collectors.toList());
			for (Path it : files) {
				String fileName = tempDir.relativize(it).toString();
				TarArchiveEntry entry = new TarArchiveEntry(
					it.toFile(),
					fileName
				);

				if (Files.isDirectory(it)) {
					// Also contain files/directories with no parent
					if (dirCounter++ % 2 == 0) {
						output.putArchiveEntry(entry);
						output.closeArchiveEntry();
					}
					continue;
				}

				entry.setMode(PermissionsHelper.toOctal(Files.getPosixFilePermissions(it)));

				try (InputStream inputStream = Files.newInputStream(it)) {
					output.putArchiveEntry(entry);
					IOUtils.copy(inputStream, output);
					output.closeArchiveEntry();
				}
			}
		}
	}
}