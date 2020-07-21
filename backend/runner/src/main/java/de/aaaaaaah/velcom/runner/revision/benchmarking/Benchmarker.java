package de.aaaaaaah.velcom.runner.revision.benchmarking;

import de.aaaaaaah.velcom.runner.entity.BenchmarkFailureInformation;
import de.aaaaaaah.velcom.runner.entity.execution.LinuxSignal;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser;
import de.aaaaaaah.velcom.runner.entity.execution.output.BenchmarkScriptOutputParser.BareResult;
import de.aaaaaaah.velcom.runner.entity.execution.output.OutputParseException;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.util.ExceptionHelper;
import de.aaaaaaah.velcom.shared.util.execution.ProgramExecutor;
import de.aaaaaaah.velcom.shared.util.execution.ProgramResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Executes the benchmark script and parses the result.
 */
public class Benchmarker {

	private final AtomicReference<BenchResult> result; // nullable inside the reference

	private final BenchRequest benchRequest;
	private final CompletableFuture<Void> finishFuture;
	private volatile boolean aborted;
	private final Thread worker;

	/**
	 * Creates a new Benchmarker <em>and starts the benchmark.</em>
	 *
	 * @param benchRequest the benchmark request
	 * @param finishFuture the future the benchmarker completes when the benchmark is done (aborted,
	 * 	failed or completed successfully)
	 */
	public Benchmarker(BenchRequest benchRequest, CompletableFuture<Void> finishFuture) {
		this.benchRequest = benchRequest;
		this.finishFuture = finishFuture;

		this.result = new AtomicReference<>();

		this.worker = new Thread(this::runBenchmark);
		this.worker.start();
	}

	/**
	 * @return the run id this benchmarker is benchmarking
	 */
	public UUID getRunId() {
		return benchRequest.getRunId();
	}

	/**
	 * @return the benchmark result. Will be null forever if the benchmark was aborted
	 */
	public Optional<BenchResult> getResult() {
		return Optional.ofNullable(result.get());
	}

	/**
	 * Aborts the benchmark if it is running.
	 */
	public void abort() {
		aborted = true;
		worker.interrupt();
	}

	private void runBenchmark() {
		BenchmarkFailureInformation information = new BenchmarkFailureInformation();
		information.addToGeneral("Runner name", '"' + benchRequest.getRunnerName() + '"');
		information.addMachineInfo(benchRequest.getSystemInfo());
		information.addToGeneral("Bench-Repo Hash", benchRequest.getBenchRepoHash());
		information.addToGeneral("User", System.getProperty("user.name"));

		Path benchScriptPath = benchRequest.getBenchRepoPath().resolve("bench");

		if (!Files.isReadable(benchScriptPath)) {
			information.addSection("Setup error", "`bench` script not found or not readable");
			setResult(new BenchResult(getRunId(), false, null, information.toString()));
			return;
		}
		if (!Files.isExecutable(benchScriptPath)) {
			information.addSection("Setup error", "`bench` script is not executable");
			setResult(new BenchResult(getRunId(), false, null, information.toString()));
			return;
		}

		Future<ProgramResult> work = startBenchExecution(information, benchScriptPath);

		try {
			// Ensure we do not wait if the benchmark was cancelled before this thread was ready
			if (aborted) {
				work.cancel(true);
				setResult(null);
				return;
			}
			ProgramResult programResult = work.get();

			addProgramOutput(information, programResult);

			setResult(interpretResult(information, programResult));
		} catch (ExecutionException e) {
			setResult(interpretExecutionException(information, e));
		} catch (InterruptedException e) {
			work.cancel(true);
			setResult(null);
		} catch (CancellationException e) {
			setResult(null);
		}
	}

	private void setResult(@Nullable BenchResult result) {
		this.result.set(result);
		finishFuture.complete(null);
	}

	private Future<ProgramResult> startBenchExecution(BenchmarkFailureInformation information,
		Path benchScriptPath) {
		Instant startTime = Instant.now();

		String[] calledCommand = {
			benchScriptPath.toAbsolutePath().toString(),
			benchRequest.getWorkRepoPath().toAbsolutePath().toString()
		};
		information.addEscapedArrayToGeneral("Executed command", calledCommand);
		information.addToGeneral("Start time", startTime.toString());

		return new ProgramExecutor().execute(calledCommand);
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
			return new BenchResult(
				getRunId(),
				false,
				null,
				information.toString()
			);
		}

		return new BenchResult(
			getRunId(),
			true,
			new Result(bareResult.getBenchmarks(), bareResult.getError()),
			null
		);
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
		return new BenchResult(
			getRunId(),
			false,
			null,
			information.toString()
		);
	}

	private BenchResult interpretExecutionException(BenchmarkFailureInformation information,
		ExecutionException e) {
		information.addSection("Stacktrace", ExceptionHelper.getStackTrace(e));
		information.addSection("End time", Instant.now().toString());
		information.addSection(
			"Reason",
			"Maybe an internal runner error. Rebenchmarking might solve the problem!"
		);
		return new BenchResult(
			getRunId(),
			false,
			null,
			information.toString()
		);
	}

}
