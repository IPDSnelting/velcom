package de.aaaaaaah.velcom.runner.benchmarking;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("not found");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("readable");
			});
	}

	@Test
	void testBenchNotExecutable() throws Exception {
		Path benchScriptPath = benchRepoPath.resolve("bench");

		Files.createFile(benchScriptPath);

		doWithResult(
			result -> {
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("executable");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("invalid");
				assertThat(error).containsIgnoringCase("output");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("exit code");
				assertThat(error).containsIgnoringCase("1");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getLeft()).isPresent();
				String error = result.getResult().getLeft().get();
				assertThat(error).containsIgnoringCase("exit code");
				assertThat(error).containsIgnoringCase("130");
				assertThat(error).containsIgnoringCase("SIGINT");
				assertThat(error).containsIgnoringCase("signal");
				assertThat(error).containsIgnoringCase("Terminal interrupt");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getRight()).isPresent();
				Result success = result.getResult().getRight().get();
				assertThat(success.getBenchmarks()).isEmpty();
				assertThat(success.getError()).isPresent();
				assertThat(success.getError().get()).isEqualTo("Halloooo");
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getRight()).isPresent();
				Result success = result.getResult().getRight().get();
				assertThat(success.getBenchmarks()).isPresent();
				assertThat(success.getBenchmarks().get()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", "20", null, null, null)
					)
				));
				assertThat(success.getError()).isEmpty();
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
				assertThat(result.getRunId()).isEqualTo(taskId);

				assertThat(result.getResult().getRight()).isPresent();
				Result success = result.getResult().getRight().get();
				assertThat(success.getBenchmarks()).isPresent();
				assertThat(success.getBenchmarks().get()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", null, "cats", Interpretation.NEUTRAL, List.of(20d, 5d))
					)
				));
				assertThat(success.getError()).isEmpty();
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

		assertThat(finishFuture).succeedsWithin(Duration.ofSeconds(3));

		assertThat(benchmarker.getResult()).isNotEmpty();
		assertThat(benchmarker.getTaskId()).isEqualTo(taskId);
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

		assertThat(finishFuture).succeedsWithin(Duration.ofSeconds(3));

		assertThat(benchmarker.getResult()).isNotEmpty();
		assertThat(benchmarker.getTaskId()).isEqualTo(taskId);
	}

	private void doWithResult(Consumer<BenchResult> resultConsumer)
		throws InterruptedException, ExecutionException, TimeoutException {

		Benchmarker benchmarker = new Benchmarker(finishFuture, taskId, workPath, BENCH_REPO_HASH,
			benchRepoPath, startTime, RUNNER_NAME, systemInfo);

		finishFuture.get(20, TimeUnit.SECONDS);

		Optional<BenchResult> resultOptional = benchmarker.getResult();

		assertThat(resultOptional).isPresent();

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
