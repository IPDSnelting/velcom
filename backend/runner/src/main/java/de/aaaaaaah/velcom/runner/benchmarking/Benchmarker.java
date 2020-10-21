package de.aaaaaaah.velcom.runner.benchmarking;

import de.aaaaaaah.velcom.runner.Delays;
import de.aaaaaaah.velcom.runner.benchmarking.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.benchmarking.output.OutputParseException;
import de.aaaaaaah.velcom.runner.formatting.NamedRows;
import de.aaaaaaah.velcom.runner.formatting.NamedSections;
import de.aaaaaaah.velcom.shared.GitProperties;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.ExceptionHelper;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import de.aaaaaaah.velcom.shared.util.execution.ProgramExecutor;
import de.aaaaaaah.velcom.shared.util.execution.ProgramResult;
import de.aaaaaaah.velcom.shared.util.execution.StreamsProcessOutput;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Executes the benchmark script and parses the result.
 */
public class Benchmarker {

	private final AtomicReference<BenchResult> result; // nullable inside the reference
	private final AtomicReference<Supplier<LinesWithOffset>> outputFetcher;

	private final CompletableFuture<Void> finishFuture;

	private final UUID taskId;
	private final Path taskRepoPath;
	@Nullable
	private final String benchRepoHash;
	private final Path benchRepoPath;

	private final Instant startTime;
	private final String runnerName;
	private final LinuxSystemInfo systemInfo;

	private volatile boolean aborted;
	private final Thread worker;

	/**
	 * Creates a new Benchmarker <em>and starts the benchmark</em>.
	 *
	 * @param finishFuture the future the benchmarker will complete after the benchmark is done
	 * 	(aborted, failed or completed successfully) and after a corresponding result has been set
	 * @param taskId the UUID of the task/run this benchmarker will perform
	 * @param taskRepoPath the path to the task repo directory
	 * @param benchRepoHash the current hash of the bench repo, or null if the backend has not yet
	 * 	sent us any bench repo.
	 * @param benchRepoPath the path to the bench repo directory
	 * @param startTime the time when the benchmark process started. When exactly this process starts
	 * 	is not up to the benchmarker.
	 * @param runnerName the name of this runner
	 * @param systemInfo the system information of this runner
	 */
	public Benchmarker(CompletableFuture<Void> finishFuture, UUID taskId, Path taskRepoPath,
		@Nullable String benchRepoHash, Path benchRepoPath, Instant startTime, String runnerName,
		LinuxSystemInfo systemInfo) {

		this.result = new AtomicReference<>();
		this.outputFetcher = new AtomicReference<>();

		this.finishFuture = finishFuture;

		this.taskId = taskId;
		this.taskRepoPath = taskRepoPath;
		this.benchRepoHash = benchRepoHash;
		this.benchRepoPath = benchRepoPath;

		this.startTime = startTime;
		this.runnerName = runnerName;
		this.systemInfo = systemInfo;

		aborted = false;
		worker = new Thread(this::runBenchmark);
		worker.start();
	}

	/**
	 * @return the id of the task this benchmarker is benchmarking
	 */
	public UUID getTaskId() {
		return taskId;
	}

	/**
	 * @return the last few lines of the output (stderr) or an empty String if none yet
	 */
	public LinesWithOffset getLastOutputLines() {
		Supplier<LinesWithOffset> supplier = outputFetcher.get();
		if (supplier == null) {
			return new LinesWithOffset(0, List.of());
		}
		return supplier.get();
	}

	/**
	 * @return the benchmark result.
	 */
	public Optional<BenchResult> getResult() {
		return Optional.ofNullable(result.get());
	}

	private NamedRows getBasicInfo() {
		NamedRows rows = new NamedRows();
		rows.add("Runner name", runnerName);
		rows.add("Hash", GitProperties.getHash());

		rows.add(
			"System",
			System.getProperty("os.name")
				+ " " + System.getProperty("os.arch")
				+ " " + System.getProperty("os.version")
		);

		rows.add("CPU", systemInfo.getCpuInfo().format());
		rows.add("Memory", systemInfo.getMemoryInfo().format());

		rows.add(
			"Java version",
			System.getProperty("java.version")
				+ " by " + System.getProperty("java.vendor")
		);

		rows.add("User", System.getProperty("user.name"));
		rows.add("Bench repo hash", Objects.requireNonNullElse(benchRepoHash, "none"));

		return rows;
	}

	/**
	 * Aborts the benchmark if it is running.
	 */
	public void abort() {
		aborted = true;
		worker.interrupt();
	}

	private void runBenchmark() {
		NamedRows generalInfo = getBasicInfo();
		NamedSections infoSections = new NamedSections();
		infoSections.addSection("General", generalInfo);

		Path benchScriptPath = benchRepoPath.resolve("bench");

		if (!Files.isReadable(benchScriptPath)) {
			infoSections.addSection("Setup error", "`bench` script not found or not readable");
			setResult(failedBenchResult(infoSections));
			return;
		}
		if (!Files.isExecutable(benchScriptPath)) {
			infoSections.addSection("Setup error", "`bench` script is not executable");
			setResult(failedBenchResult(infoSections));
			return;
		}

		StreamsProcessOutput<ProgramResult> work = startBenchExecution(generalInfo, benchScriptPath);

		outputFetcher.set(() -> {
			String stdErr = work.getCurrentStdErr();
			List<String> lines = stdErr.lines().collect(Collectors.toList());

			List<String> sublist = lines.subList(Math.max(lines.size() - 100, 0), lines.size());

			int indexFirstLine = lines.size() - sublist.size();

			return new LinesWithOffset(indexFirstLine, sublist);
		});

		try {
			// Ensure we do not wait if the benchmark was cancelled before this thread was ready
			if (aborted) {
				work.cancel(true);
			}
			ProgramResult programResult = work.get();

			addProgramOutput(generalInfo, infoSections, programResult);

			setResult(interpretResult(infoSections, programResult));
		} catch (ExecutionException e) {
			setResult(interpretExecutionException(infoSections, e));
		} catch (InterruptedException e) {
			work.cancel(true);
			infoSections.addSection("Failed", "The benchmark thread was interrupted");
			setResult(failedBenchResult(infoSections));
		} catch (CancellationException e) {
			infoSections.addSection("Failed", "The run was aborted");
			setResult(failedBenchResult(infoSections));
		}
	}

	/**
	 * Sets the result if it hasn't already be set.
	 *
	 * @param result the result
	 */
	private void setResult(@Nonnull BenchResult result) {
		this.result.compareAndSet(null, result);
		finishFuture.complete(null);
	}

	private StreamsProcessOutput<ProgramResult> startBenchExecution(NamedRows generalInfo,
		Path benchScriptPath) {
		Instant startTime = Instant.now();

		String[] calledCommand = {
			benchScriptPath.toAbsolutePath().toString(),
			taskRepoPath.toAbsolutePath().toString()
		};
		generalInfo.addEscapedArray("Executed command", calledCommand);
		generalInfo.add("Start time", startTime.toString());

		return new ProgramExecutor(Delays.TIME_TO_KILL.toMillis()).execute(calledCommand);
	}

	private void addProgramOutput(NamedRows generalInfo, NamedSections infoSections,
		ProgramResult programResult) {

		generalInfo.add("Stop time", Instant.now().toString());
		generalInfo.add("Execution time", programResult.getRuntime().toString());
		generalInfo.add("Exit code", programResult.getExitCode() + "");

		infoSections.addSection(
			"Stdout",
			programResult.getStdOut().isEmpty() ? "<empty>" : programResult.getStdOut()
		);
		infoSections.addSection(
			"Stderr",
			programResult.getStdErr().isEmpty() ? "<empty>" : programResult.getStdErr()
		);
	}

	private BenchResult interpretResult(NamedSections infoSections, ProgramResult programResult) {
		if (programResult.getExitCode() == 0) {
			return interpretZeroExitCode(infoSections, programResult);
		}
		return interpretFailingExitCode(infoSections, programResult);
	}

	private BenchResult interpretZeroExitCode(NamedSections infoSections,
		ProgramResult programResult) {

		Either<String, List<Benchmark>> bareResult;

		try {
			bareResult = new BenchmarkScriptOutputParser().parse(programResult.getStdOut());
		} catch (OutputParseException e) {
			infoSections.addSection("Invalid output", e.getMessage());
			infoSections.addSection(
				"Reason",
				"The benchmark script returned invalid output!"
			);
			return failedBenchResult(infoSections);
		}

		return successfulBenchResult(new Result(
			bareResult.getRight().orElse(null),
			bareResult.getLeft().orElse(null)
		));
	}

	private BenchResult interpretFailingExitCode(NamedSections infoSections,
		ProgramResult programResult) {

		infoSections.addSection(
			"Reason",
			"The benchmark script terminated with a non-zero exit code"
				+ " (" + programResult.getExitCode() + ")!"
		);

		LinuxSignal.forExitCode(programResult.getExitCode())
			.ifPresent(signal -> infoSections.addSection(
				"Exit code interpretation",
				"The exit code looks like the linux signal " + signal.name() + ".\n"
					+ "It's signal number is " + signal.getNumber() + " and it is caused by '"
					+ signal.getExplanation() + "'."
			));

		return failedBenchResult(infoSections);
	}

	private BenchResult interpretExecutionException(NamedSections infoSections,
		ExecutionException e) {
		infoSections.addSection("Stacktrace", ExceptionHelper.getStackTrace(e));
		infoSections.addSection("End time", Instant.now().toString());
		infoSections.addSection(
			"Reason",
			"Maybe an internal runner error. Rebenchmarking might solve the problem!"
		);
		return failedBenchResult(infoSections);
	}

	private BenchResult successfulBenchResult(Result result) {
		return BenchResult.successful(taskId, result, startTime, Instant.now());
	}

	private BenchResult failedBenchResult(NamedSections infoSections) {
		return BenchResult.failed(taskId, infoSections.format(), startTime, Instant.now());
	}

}
