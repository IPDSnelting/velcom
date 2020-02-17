package de.aaaaaaah.velcom.runner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.FutureProgramResult;
import de.aaaaaaah.velcom.runner.shared.ProgramExecutor.ProgramResult;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgramExecutorTest {

	private ProgramExecutor programExecutor;

	@BeforeEach
	void setUp() {
		programExecutor = new ProgramExecutor(100);
	}

	@Test
	void capturesExitCode() throws InterruptedException {
		ProgramResult result = programExecutor.execute("/usr/bin/env", "bash", "-c", "exit 120")
			.get();
		assertThat(result.getExitCode()).isEqualTo(120);
	}

	@Test
	void capturesStandardOutput() throws InterruptedException {
		String output = "HEY you there\nMy äöß‡friend";
		ProgramResult result = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "echo -ne '" + output + "'"
		)
			.get();
		assertThat(result.getStdOut()).isEqualTo(output);
		assertThat(result.getStdErr()).isEmpty();
	}

	@Test
	void capturesStandardError() throws InterruptedException {
		String output = "HEY you there\nMy äöß‡friend";
		ProgramResult result = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "echo -ne '" + output + "' 1>&2"
		)
			.get();
		assertThat(result.getStdErr()).isEqualTo(output);
		assertThat(result.getStdOut()).isEmpty();
	}

	@Test
	void drainsStandardOut() throws InterruptedException {
		String output = "Hello".repeat(10_000);
		ProgramResult result = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "echo -ne '" + output + "'"
		)
			.get();
		assertThat(result.getStdOut()).isEqualTo(output);
		assertThat(result.getStdErr()).isEmpty();
	}

	@Test
	void callInvalidProgram() {
		assertThatThrownBy(
			() -> programExecutor.execute("hello world how are you").get()
		);
	}

	@Test
	void gracefullyStopProgram() throws InterruptedException {
		FutureProgramResult future = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "stuff() {\n"
				+ "    while true ; do\n"
				+ "        /usr/bin/sleep 1\n"
				+ "    done\n"
				+ "}\n"
				+ "\n"
				+ "stuff\n"
		);
		Thread.sleep(100);

		future.cancel();
		assertThatThrownBy(future::get).isInstanceOf(CancellationException.class);
	}

	@Test
	void forcefullyStopProgram() throws InterruptedException {
		FutureProgramResult future = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "stuff() {\n"
				+ "    while true ; do\n"
				+ "        /usr/bin/sleep 1\n"
				+ "    done\n"
				+ "}\n"
				+ "\n"
				+ "trap \"stuff\" SIGTERM\n"
				+ "\n"
				+ "stuff\n"
		);

		Thread.sleep(100);
		future.cancel();

		assertThatThrownBy(future::get).isInstanceOf(CancellationException.class);
	}

}