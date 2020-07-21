package de.aaaaaaah.velcom.runner.revision.benchmarking;

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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkerTest {

	@TempDir
	Path rootTempDir;
	Path benchRepoPath;
	Path workPath;

	@BeforeEach
	void setUp() throws IOException {
		benchRepoPath = rootTempDir.resolve("benchrepo");
		workPath = rootTempDir.resolve("work");
		Files.createDirectory(benchRepoPath);
		Files.createDirectory(workPath);
	}

	@Test
	void testBenchDoesNotExist() throws InterruptedException {
		BenchRequest request = getBenchRequest();
		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("not found");
			}
		);
	}

	@Test
	void testBenchNotReadable() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();
		Path benchScriptPath = request.getBenchRepoPath().resolve("bench");

		Files.createFile(benchScriptPath);
		EnumSet<PosixFilePermission> permissions = EnumSet.copyOf(
			Files.getPosixFilePermissions(benchScriptPath)
		);
		permissions.remove(PosixFilePermission.OTHERS_READ);
		permissions.remove(PosixFilePermission.OWNER_READ);
		permissions.remove(PosixFilePermission.GROUP_READ);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		Files.setPosixFilePermissions(benchScriptPath, permissions);

		Benchmarker benchmarker = new Benchmarker(request);
		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("readable");
			});
	}

	@Test
	void testBenchNotExecutable() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();
		Path benchScriptPath = request.getBenchRepoPath().resolve("bench");

		Files.createFile(benchScriptPath);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("executable");
			}
		);
	}

	@Test
	void testBenchScriptInvalidOutput() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"echo 'Hello'"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("invalid");
				assertThat(result.getError()).containsIgnoringCase("output");
			}
		);
	}

	@Test
	void testBenchScriptExitCodeFailure() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"exit 1"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("exit code");
				assertThat(result.getError()).containsIgnoringCase("1");
			}
		);
	}

	@Test
	void testBenchScriptSignalCodeInterpretation() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"exit 130"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNull();
				assertThat(result.getError()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getError()).containsIgnoringCase("exit code");
				assertThat(result.getError()).containsIgnoringCase("130");
				assertThat(result.getError()).containsIgnoringCase("SIGINT");
				assertThat(result.getError()).containsIgnoringCase("signal");
				assertThat(result.getError()).containsIgnoringCase("Terminal interrupt");
			}
		);
	}

	@Test
	void testBenchScriptBenchmarkError() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"echo '{ \"error\": \"Halloooo\" }'"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getError()).isNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getResult().getBenchmarks()).isNull();
				assertThat(result.getResult().getError()).isEqualTo("Halloooo");
			}
		);
	}

	@Test
	void testBenchScriptMetricError() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"echo '{ \"test\": { \"metric\": { \"error\": \"20\" } } }'"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getError()).isNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getResult().getBenchmarks()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", "20", "", Interpretation.NEUTRAL, null)
					)
				));
				assertThat(result.getResult().getError()).isNull();
			}
		);
	}

	@Test
	void testBenchScriptMetricWithValue() throws InterruptedException, IOException {
		BenchRequest request = getBenchRequest();

		writeBenchScript(
			"#!/usr/bin/sh",
			"echo '{ \"test\": { \"metric\": {"
				+ " \"unit\": \"cats\", \"resultInterpretation\": \"NEUTRAL\", \"results\": [20, 5]"
				+ " } } }'"
		);

		Benchmarker benchmarker = new Benchmarker(request);

		doWithResult(
			benchmarker,
			result -> {
				assertThat(result.getError()).isNull();
				assertThat(result.getResult()).isNotNull();
				assertThat(result.getRunId()).isEqualTo(request.getRunId());
				assertThat(result.getResult().getBenchmarks()).containsExactly(new Benchmark(
					"test",
					List.of(
						new Metric("metric", null, "cats", Interpretation.NEUTRAL, List.of(20d, 5d))
					)
				));
				assertThat(result.getResult().getError()).isNull();
			}
		);
	}

	private void doWithResult(Benchmarker benchmarker, Consumer<BenchResult> resultConsumer)
		throws InterruptedException {
		// we have no way of knowing the benchmarker finished
		for (int i = 0; i < 10; i++) {
			Thread.sleep(250);
			Optional<BenchResult> resultOptional = benchmarker.getResult();
			if (resultOptional.isEmpty()) {
				continue;
			}
			BenchResult result = resultOptional.get();
			resultConsumer.accept(result);
			return;
		}

		fail("Timed out waiting for bench script result");
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

	private BenchRequest getBenchRequest() {
		return new BenchRequest(
			LinuxSystemInfo.getCurrent(),
			UUID.randomUUID(),
			"hello",
			benchRepoPath,
			workPath,
			"Runner"
		);
	}
}