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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes the benchmark script and parses the result.
 */
public class Benchmarker {

	private final AtomicReference<BenchResult> result; // nullable inside the reference

	private final Thread thread;
	private final BenchRequest benchRequest;
	private boolean aborted;

	/**
	 * Creates a new Benchmarker <em>and starts the benchmark.</em>
	 *
	 * @param benchRequest the benchmark request
	 */
	public Benchmarker(BenchRequest benchRequest) {
		this.benchRequest = benchRequest;

		result = new AtomicReference<>();

		thread = new Thread(this::runBenchmark);
		thread.start();
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
	public synchronized void abort() {
		// The synchronization requirements are detailed in the runBenchmark method.
		aborted = true;
		thread.interrupt();
	}

	private void runBenchmark() {
		BenchmarkFailureInformation information = new BenchmarkFailureInformation();
		information.addToGeneral("Runner name", '"' + benchRequest.getRunnerName() + '"');
		information.addMachineInfo(benchRequest.getSystemInfo());
		information.addToGeneral("Bench-Repo Hash", benchRequest.getBenchRepoHash());
		information.addToGeneral("User", System.getProperty("user.name"));

		Path benchScriptPath = benchRequest.getBenchRepoPath().resolve("bench");
		if (!Files.isExecutable(benchScriptPath)) {
			information.addSection("Setup error", "`bench` script is not executable");
			this.result.set(new BenchResult(getRunId(), false, null, information.toString()));
			return;
		}

		Future<ProgramResult> work = startBenchExecution(information, benchScriptPath);

		try {
			// This part is synchronized with the abort method. This ensures the following scenario can
			// not happen:
			// 1. The worker thread is up and at the aborted check
			// 2. The worker thread passes it
			// 3. the abort method is invoked and interrupts the worker
			// 4. the worker thread does not check the flag and calls work.get, blocking for the result
			// ==> The abort is lost!
			// Synchronizing here ensures that the worker is either already waiting and receives the
			// interrupted exception or aborted is set to true before the wait starts and the worker bails
			// out and cancels the work.
			synchronized (this) {
				// Ensure we do not wait if the benchmark was cancelled before this thread was ready
				if (aborted) {
					work.cancel(true);
					this.result.set(null);
					return;
				}
				ProgramResult programResult = work.get();

				addProgramOutput(information, programResult);

				this.result.set(interpretResult(information, programResult));
			}
		} catch (ExecutionException e) {
			this.result.set(interpretExecutionException(information, e));
		} catch (InterruptedException e) {
			work.cancel(true);
			this.result.set(null);
		} catch (CancellationException e) {
			this.result.set(null);
		}
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
			bareResult.getError() == null,
			new Result(bareResult.getBenchmarks(), bareResult.getError()),
			bareResult.getError()
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
