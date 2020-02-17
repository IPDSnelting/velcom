package de.aaaaaaah.velcom.runner.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.aaaaaaah.velcom.runner.cli.RunnerMain.SystemExiter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RunnerMainTest {

	@TempDir
	Path tempDir;

	private SystemExiter exiter;

	@BeforeEach
	void setUp() {
		exiter = mock(SystemExiter.class);
		RunnerMain.exiter = exiter;
	}

	@Test
	void failsIfNoArgumentsAreGiven() throws InterruptedException {
		RunnerMain.main(new String[0]);
		verify(exiter).exit(0);
	}

	@Test
	void failsIfPathToNoFileIsGiven() throws InterruptedException {
		RunnerMain.main(new String[]{tempDir.resolve("hey").toAbsolutePath().toString()});
		verify(exiter).exit(0);
	}

	@Test
	void failsIfPathToInvalidFileIsGiven() throws IOException {
		Path hey = tempDir.resolve("hey");
		Files.createFile(hey);

		assertThatThrownBy(() -> RunnerMain.main(new String[]{hey.toAbsolutePath().toString()}));
		verify(exiter).exit(1);
	}

	@ParameterizedTest
	@CsvSource(value = {
		"{}",
		"{ \"serverUrl\": \"Hello\" }",
		"{ \"serverUrl\": null, \"runnerToken\": \"Hello\", \"runnerName\": \"Test\" }",
	}, delimiter = '|')
	void failsIfInvalidConfigIsPassed(String config) throws IOException {
		Path hey = tempDir.resolve("hey");
		Files.createFile(hey);
		Files.writeString(hey, config);

		assertThatThrownBy(() -> RunnerMain.main(new String[]{hey.toAbsolutePath().toString()}));
		verify(exiter).exit(1);
	}
}