package de.aaaaaaah.velcom.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.shared.util.execution.ProgramExecutor;
import de.aaaaaaah.velcom.shared.util.execution.ProgramResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgramExecutorTest {

	private ProgramExecutor programExecutor;
	private final int timeToForceKillMillis = 2000;

	@BeforeEach
	void setUp() {
		programExecutor = new ProgramExecutor(timeToForceKillMillis);
	}

	@Test
	void capturesExitCode() throws ExecutionException, InterruptedException {
		ProgramResult result = programExecutor.execute("/usr/bin/env", "bash", "-c", "exit 120")
			.get();
		assertThat(result.getExitCode()).isEqualTo(120);
	}

	@Test
	void capturesStandardOutput() throws ExecutionException, InterruptedException {
		String output = "HEY you there\nMy äöß‡friend";
		ProgramResult result = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "echo -ne '" + output + "'"
		)
			.get();
		assertThat(result.getStdOut()).isEqualTo(output);
		assertThat(result.getStdErr()).isEmpty();
	}

	@Test
	void capturesStandardError() throws ExecutionException, InterruptedException {
		String output = "HEY you there\nMy äöß‡friend";
		ProgramResult result = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "echo -ne '" + output + "' 1>&2"
		)
			.get();
		assertThat(result.getStdErr()).isEqualTo(output);
		assertThat(result.getStdOut()).isEmpty();
	}

	@Test
	void drainsStandardOut() throws ExecutionException, InterruptedException {
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
		Future<ProgramResult> future = programExecutor.execute(
			"/usr/bin/env", "bash", "-c", "stuff() {\n"
				+ "    while true ; do\n"
				+ "        /usr/bin/sleep 1\n"
				+ "    done\n"
				+ "}\n"
				+ "\n"
				+ "stuff\n"
		);
		Thread.sleep(100);

		future.cancel(true);

		Instant start = Instant.now();

		assertThatThrownBy(future::get).isInstanceOf(CancellationException.class);

		// TODO: 21.06.20 Is this too flaky?
		assertThat(Duration.between(start, Instant.now()).toMillis())
			.isLessThan(timeToForceKillMillis);
	}

	@Test
	void forcefullyStopProgram() throws InterruptedException {
		Future<ProgramResult> future = programExecutor.execute(
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
		future.cancel(true);

		Instant start = Instant.now();

		assertThatThrownBy(future::get).isInstanceOf(CancellationException.class);

		assertThat(Duration.between(start, Instant.now()).toMillis())
			.isGreaterThanOrEqualTo(timeToForceKillMillis);
	}

}