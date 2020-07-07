package de.aaaaaaah.velcom.runner.entity;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.shared.util.StringOutputStream;
import de.aaaaaaah.velcom.shared.util.FileHelper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TempFileBenchmarkRepoOrganizerTest {

	private TempFileBenchmarkRepoOrganizer organizer;

	@BeforeEach
	void setUp() throws IOException {
		organizer = new TempFileBenchmarkRepoOrganizer();
	}

	@AfterEach
	void tearDown() throws IOException {
		Path tempDir = organizer.getPathToRepo().getParent();
		if (Files.exists(tempDir)) {
			FileHelper.deleteDirectoryOrFile(tempDir);
		}
	}

	@Test
	void headHashIsInitiallyEmpty() {
		assertThat(organizer.getHeadHash()).isEmpty();
	}

	@Test
	void copySetsHeadHash() throws URISyntaxException, IOException {
		organizer.copyToYourself(
			Path.of(
				TempFileBenchmarkRepoOrganizerTest.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI()
			),
			"Test"
		);
		assertThat(organizer.getHeadHash()).hasValue("Test");
	}

	@Test
	void copiedFile() throws URISyntaxException, IOException {
		organizer.copyToYourself(
			Path.of(
				TempFileBenchmarkRepoOrganizerTest.class
					.getResource("/benchmark-script/sample-bench-dir")
					.toURI()
			),
			"Test"
		);
		assertThat(organizer.getHeadHash()).hasValue("Test");
		assertThat(
			Files.list(organizer.getPathToRepo()).collect(toList())
		)
			.containsExactly(organizer.getPathToRepo().resolve("bench"));
		String readScript = Files.readString(organizer.getBenchmarkScript());
		String realScript = readResource("/benchmark-script/sample-bench-dir/bench");

		assertThat(readScript).isEqualTo(realScript);
	}

	@Test
	void copyTwiceOverwrites() throws URISyntaxException, IOException {
		organizer.copyToYourself(
			Path.of(
				TempFileBenchmarkRepoOrganizerTest.class
					.getResource("/benchmark-script/sample-bench-dir")
					.toURI()
			),
			"Test"
		);
		organizer.copyToYourself(
			Path.of(
				TempFileBenchmarkRepoOrganizerTest.class
					.getResource("/benchmark-script/sample-bench-dir-2")
					.toURI()
			),
			"Test2"
		);
		assertThat(organizer.getHeadHash()).hasValue("Test2");
		assertThat(
			Files.list(organizer.getPathToRepo()).collect(toList())
		)
			.containsExactly(organizer.getPathToRepo().resolve("bench"));
		String readScript = Files.readString(organizer.getBenchmarkScript());
		String realScript = readResource("/benchmark-script/sample-bench-dir-2/bench");

		assertThat(readScript).isEqualTo(realScript);
	}

	private static String readResource(String path) throws IOException {
		StringOutputStream stringOutputStream = new StringOutputStream();
		try (InputStream inputStream = TempFileBenchmarkRepoOrganizerTest.class
			.getResourceAsStream(path)) {
			inputStream.transferTo(stringOutputStream);
		}
		return stringOutputStream.getString();
	}
}