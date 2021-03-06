package de.aaaaaaah.velcom.shared.util.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
	void drainsStandardErr() throws ExecutionException, InterruptedException {
		String output = "Hello".repeat(200_000);
		Future<ProgramResult> future = programExecutor.execute(
			"/usr/bin/env", "bash", "-c",
			"for i in {1..200000}; do echo -ne 'Hello' ; done | tee /dev/stderr"
		);

		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(TimeUnit.SECONDS.toMillis(20));
				future.cancel(true);
			} catch (InterruptedException ignored) {
				// Reached when everything works
			}
		});
		thread.start();

		ProgramResult result;
		try {
			result = future.get();
		} catch (CancellationException e) {
			fail("Likely needed to be killed", e);
			return;
		}

		thread.interrupt();
		assertThat(result.getStdErr()).isEqualTo(output);
		assertThat(result.getStdOut()).isEqualTo(output);
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

	@Test
	void hasSomewhatCorrectDuration() throws ExecutionException, InterruptedException {
		ProgramExecutor executor = new ProgramExecutor();
		ProgramResult result = executor.execute("/usr/bin/env", "bash", "-c", "sleep 3")
			.get();

		assertThat(result.getRuntime().toSeconds()).isBetween(3L, 6L);
	}

	@Test
	void streamsStandardOutAndError() throws InterruptedException, ExecutionException {
		ProgramExecutor executor = new ProgramExecutor();
		StreamsProcessOutput<ProgramResult> processOutput = executor.execute("/usr/bin/env", "bash",
			"-c",
			"echo 'Hello world'\n"
				+ "echo 'Hello world error' >&2\n"
				+ "\n"
				+ "sleep 2\n"
				+ "\n"
				+ "echo -n ' after!'\n"
				+ "echo -n ' after error!' >&2\n"
		);

		Thread.sleep(1000);

		assertThat(processOutput.getCurrentStdOut()).isEqualTo("Hello world\n");
		assertThat(processOutput.getCurrentStdErr()).isEqualTo("Hello world error\n");

		// If the build server is slow, this can take a few seconds. Give it 6 and bail out early if
		// it is faster
		for (int i = 0; i < 12; i++) {
			Thread.sleep(500);

			if (!processOutput.getCurrentStdOut().contains("after")) {
				continue;
			}
			if (!processOutput.getCurrentStdErr().contains("after")) {
				continue;
			}
			break;
		}

		assertThat(processOutput.getCurrentStdOut()).isEqualTo("Hello world\n after!");
		assertThat(processOutput.getCurrentStdErr()).isEqualTo("Hello world error\n after error!");

		assertThat(processOutput.isDone());
		assertThat(processOutput.get().getExitCode()).isEqualTo(0);
	}
}
