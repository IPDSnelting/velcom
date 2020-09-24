package de.aaaaaaah.velcom.runner.benchmarking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkerTest {

	public static final String BENCH_REPO_HASH = "hello";
	public static final String RUNNER_NAME = "runner name";
	@TempDir
	Path rootTempDir;
	Path benchRepoPath;
	Path workPath;
	private CompletableFuture<Void> finishFuture;

	private UUID taskId;
	private LinuxSystemInfo systemInfo;
	private Instant startTime;

	@BeforeEach
	void setUp() throws IOException {
		benchRepoPath = rootTempDir.resolve("benchrepo");
		workPath = rootTempDir.resolve("work");
		Files.createDirectory(benchRepoPath);
		Files.createDirectory(workPath);
		finishFuture = new CompletableFuture<>();

		taskId = UUID.randomUUID();
		systemInfo = LinuxSystemInfo.getCurrent();
		startTime = Instant.now();
	}

	@Test
	void testBenchDoesNotExist() throws Exception {
		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("not found");
			}
		);
	}

	@Test
	void testBenchNotReadable() throws Exception {
		Path benchScriptPath = benchRepoPath.resolve("bench");

		Files.createFile(benchScriptPath);
		EnumSet<PosixFilePermission> permissions = EnumSet.copyOf(
			Files.getPosixFilePermissions(benchScriptPath)
		);
		permissions.remove(PosixFilePermission.OTHERS_READ);
		permissions.remove(PosixFilePermission.OWNER_READ);
		permissions.remove(PosixFilePermission.GROUP_READ);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		Files.setPosixFilePermissions(benchScriptPath, permissions);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("readable");
			});
	}

	@Test
	void testBenchNotExecutable() throws Exception {
		Path benchScriptPath = benchRepoPath.resolve("bench");

		Files.createFile(benchScriptPath);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("executable");
			}
		);
	}

	@Test
	void testBenchScriptInvalidOutput() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"echo 'Hello'"
		);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("invalid");
				assertThat(result.getError()).containsIgnoringCase("output");
			}
		);
	}

	@Test
	void testBenchScriptExitCodeFailure() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"exit 1"
		);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("exit code");
				assertThat(result.getError()).containsIgnoringCase("1");
			}
		);
	}

	@Test
	void testBenchScriptSignalCodeInterpretation() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"exit 130"
		);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getError()).containsIgnoringCase("exit code");
				assertThat(result.getError()).containsIgnoringCase("130");
				assertThat(result.getError()).containsIgnoringCase("SIGINT");
				assertThat(result.getError()).containsIgnoringCase("signal");
				assertThat(result.getError()).containsIgnoringCase("Terminal interrupt");
			}
		);
	}

	@Test
	void testBenchScriptBenchmarkError() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"echo '{ \"error\": \"Halloooo\" }'"
		);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getError()).isNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getResult().getBenchmarks()).isEmpty();
				assertThat(result.getResult().getError()).isPresent();
				assertThat(result.getResult().getError().get()).isEqualTo("Halloooo");
			}
		);
	}

	@Test
	void testBenchScriptMetricError() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"echo '{ \"test\": { \"metric\": { \"error\": \"20\" } } }'"
		);

		doWithResult(
			result -> {
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getError()).isNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getResult().getBenchmarks()).isPresent();
				assertThat(result.getResult().getBenchmarks().get()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", "20", null, null, null)
					)
				));
				assertThat(result.getResult().getError()).isEmpty();
			}
		);
	}

	@Test
	void testBenchScriptMetricWithValue() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"echo '{ \"test\": { \"metric\": {"
				+ " \"unit\": \"cats\", \"resultInterpretation\": \"NEUTRAL\", \"results\": [20, 5]"
				+ " } } }'"
		);

		doWithResult(
			result -> {
				assertThat(result.getError()).isNull();
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(taskId);
				assertThat(result.getResult().getBenchmarks()).isPresent();
				assertThat(result.getResult().getBenchmarks().get()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", null, "cats", Interpretation.NEUTRAL, List.of(20d, 5d))
					)
				));
				assertThat(result.getResult().getError()).isEmpty();
			}
		);
	}

	@Test
	void testAbort() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"while true ; do sleep 1 ; done"
		);

		Benchmarker benchmarker = new Benchmarker(finishFuture, taskId, workPath, BENCH_REPO_HASH,
			benchRepoPath, startTime, RUNNER_NAME, systemInfo);

		Thread.sleep(400);
		benchmarker.abort();

		// Give it a few seconds to really cancel it (though ~500ms should be enough currently)
		for (int i = 0; i < 10; i++) {
			Thread.sleep(250);
			if (finishFuture.isDone()) {
				assertThat(benchmarker.getResult()).isNotEmpty();
				assertThat(benchmarker.getTaskId()).isEqualTo(taskId);
				return;
			}
		}
		fail("Cancel should have been done by now");
	}

	@Test
	void testAbortAfterLongTime() throws Exception {
		writeBenchScript(
			"#!/bin/sh",
			"while true ; do sleep 1 ; done"
		);

		Benchmarker benchmarker = new Benchmarker(finishFuture, taskId, workPath, BENCH_REPO_HASH,
			benchRepoPath, startTime, RUNNER_NAME, systemInfo);

		Thread.sleep(1000);
		benchmarker.abort();

		// Give it a few seconds to really cancel it (though ~500ms should be enough currently)
		for (int i = 0; i < 10; i++) {
			Thread.sleep(250);
			if (finishFuture.isDone()) {
				assertThat(benchmarker.getResult()).isNotEmpty();
				assertThat(benchmarker.getTaskId()).isEqualTo(taskId);
				return;
			}
		}
		fail("Cancel should have been done by now");
	}

	private void doWithResult(Consumer<BenchResult> resultConsumer)
		throws InterruptedException, ExecutionException, TimeoutException {

		Benchmarker benchmarker = new Benchmarker(finishFuture, taskId, workPath, BENCH_REPO_HASH,
			benchRepoPath, startTime, RUNNER_NAME, systemInfo);

		finishFuture.get(20, TimeUnit.SECONDS);

		Optional<BenchResult> resultOptional = benchmarker.getResult();

		Assertions.assertThat(resultOptional).isPresent();

		BenchResult result = resultOptional.get();
		resultConsumer.accept(result);
	}

	private void writeBenchScript(String... lines) throws IOException {
		Path path = benchRepoPath.resolve("bench");
		Files.write(path, Arrays.asList(lines));

		Set<PosixFilePermission> permissions = EnumSet.copyOf(
			Files.getPosixFilePermissions(path)
		);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);

		Files.setPosixFilePermissions(path, permissions);
	}
}
