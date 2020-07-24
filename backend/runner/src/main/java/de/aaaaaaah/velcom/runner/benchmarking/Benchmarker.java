package de.aaaaaaah.velcom.runner.benchmarking;

import de.aaaaaaah.velcom.runner.Delays;
import de.aaaaaaah.velcom.runner.benchmarking.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.benchmarking.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.benchmarking.output.OutputParseException;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.util.ExceptionHelper;
import de.aaaaaaah.velcom.shared.util.execution.ProgramExecutor;
import de.aaaaaaah.velcom.shared.util.execution.ProgramResult;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Executes the benchmark script and parses the result.
 */
public class Benchmarker {

	private final AtomicReference<BenchResult> result; // nullable inside the reference

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
	 * @param startTime the time when the benchmark process started. When exactly this process
	 * 	starts is not up to the benchmarker.
	 * @param runnerName the name of this runner
	 * @param systemInfo the system information of this runner
	 */
	public Benchmarker(CompletableFuture<Void> finishFuture, UUID taskId, Path taskRepoPath,
		@Nullable String benchRepoHash, Path benchRepoPath, Instant startTime, String runnerName,
		LinuxSystemInfo systemInfo) {

		result = new AtomicReference<>();

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
	 * @return the benchmark result.
	 */
	public Optional<BenchResult> getResult() {
		return Optional.ofNullable(result.get());
	}

	private BenchmarkFailureInformation getBasicFailureInfo() {
		BenchmarkFailureInformation info = new BenchmarkFailureInformation();
		info.addToGeneral("Runner name", '"' + runnerName + '"');
		info.addMachineInfo(systemInfo);
		info.addToGeneral("User", System.getProperty("user.name"));
		info.addToGeneral("Bench repo hash", Objects.requireNonNullElse(benchRepoHash, "none"));
		return info;
	}

	/**
	 * Aborts the benchmark if it is running.
	 */
	public void abort() {
		aborted = true;
		worker.interrupt();
	}

	private void runBenchmark() {
		BenchmarkFailureInformation information = getBasicFailureInfo();

		Path benchScriptPath = benchRepoPath.resolve("bench");

		if (!Files.isReadable(benchScriptPath)) {
			information.addSection("Setup error", "`bench` script not found or not readable");
			setResult(failedBenchResult(information.toString()));
			return;
		}
		if (!Files.isExecutable(benchScriptPath)) {
			information.addSection("Setup error", "`bench` script is not executable");
			setResult(failedBenchResult(information.toString()));
			return;
		}

		Future<ProgramResult> work = startBenchExecution(information, benchScriptPath);

		try {
			// Ensure we do not wait if the benchmark was cancelled before this thread was ready
			if (aborted) {
				work.cancel(true);
			}
			ProgramResult programResult = work.get();

			addProgramOutput(information, programResult);

			setResult(interpretResult(information, programResult));
		} catch (ExecutionException e) {
			setResult(interpretExecutionException(information, e));
		} catch (InterruptedException e) {
			work.cancel(true);
			information.addSection("Failed", "The benchmark thread was interrupted");
			setResult(failedBenchResult(information.toString()));
		} catch (CancellationException e) {
			information.addSection("Failed", "The run was aborted");
			setResult(failedBenchResult(information.toString()));
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

	private Future<ProgramResult> startBenchExecution(BenchmarkFailureInformation information,
		Path benchScriptPath) {
		Instant startTime = Instant.now();

		String[] calledCommand = {
			benchScriptPath.toAbsolutePath().toString(),
			taskRepoPath.toAbsolutePath().toString()
		};
		information.addEscapedArrayToGeneral("Executed command", calledCommand);
		information.addToGeneral("Start time", startTime.toString());

		return new ProgramExecutor(Delays.TIME_TO_KILL.toMillis()).execute(calledCommand);
	}

	private void addProgramOutput(BenchmarkFailureInformation information,
		ProgramResult programResult) {
		information.addToGeneral("Stop time", Instant.now().toString());
		information.addToGeneral("Execution time", programResult.getRuntime().toString());
		information.addToGeneral("Exit code", programResult.getExitCode() + "");
		information.addSection(
			"Stdout",
			programResult.getStdOut().isEmpty() ? "<empty>" : programResult.getStdOut()
		);
		information.addSection(
			"Stderr",
			programResult.getStdErr().isEmpty() ? "<empty>" : programResult.getStdErr()
		);
	}

	private BenchResult interpretResult(BenchmarkFailureInformation information,
		ProgramResult programResult) {
		if (programResult.getExitCode() == 0) {
			return interpretZeroExitCode(information, programResult);
		}
		return interpretFailingExitCode(information, programResult);
	}

	private BenchResult interpretZeroExitCode(BenchmarkFailureInformation information,
		ProgramResult programResult) {
		BareResult bareResult;

		try {
			bareResult = new BenchmarkScriptOutputParser().parse(programResult.getStdOut());
		} catch (OutputParseException e) {
			information.addSection("Invalid output", e.getMessage());
			information.addSection(
				"Reason",
				"The benchmark script returned invalid output!"
			);
			return failedBenchResult(information.toString());
		}

		return successfulBenchResult(new Result(bareResult.getBenchmarks(), bareResult.getError()));
	}

	private BenchResult interpretFailingExitCode(BenchmarkFailureInformation information,
		ProgramResult programResult) {
		information.addSection(
			"Reason",
			"The benchmark script terminated with a non-zero exit code"
				+ " (" + programResult.getExitCode() + ")!"
		);
		LinuxSignal.forExitCode(programResult.getExitCode())
			.ifPresent(signal -> information.addSection(
				"Exit code interpretation",
				"The exit code looks like the linux signal " + signal.name() + ".\n"
					+ "It's signal number is " + signal.getNumber() + " and it is caused by '"
					+ signal.getExplanation() + "'."
			));
		return failedBenchResult(information.toString());
	}

	private BenchResult interpretExecutionException(BenchmarkFailureInformation information,
		ExecutionException e) {
		information.addSection("Stacktrace", ExceptionHelper.getStackTrace(e));
		information.addSection("End time", Instant.now().toString());
		information.addSection(
			"Reason",
			"Maybe an internal runner error. Rebenchmarking might solve the problem!"
		);
		return failedBenchResult(information.toString());
	}

	private BenchResult successfulBenchResult(Result result) {
		return new BenchResult(taskId, true, result, null, startTime, Instant.now());
	}

	private BenchResult failedBenchResult(String error) {
		return new BenchResult(taskId, false, null, error, startTime, Instant.now());
	}

}
