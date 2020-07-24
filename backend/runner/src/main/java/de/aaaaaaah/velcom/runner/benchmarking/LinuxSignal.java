package de.aaaaaaah.velcom.runner.benchmarking;

import java.util.Arrays;
import java.util.Optional;

/**
 * Common linux (posix) signals ans their meanings.
 */
public enum LinuxSignal {
	SIGHUP(1, "Hangup"),
	SIGINT(2, "Terminal interrupt signal"),
	SIGQUIT(3, "Terminal quit signal"),
	SIGILL(4, "Illegal instruction"),
	SIGTRAP(5, "Trace/breakpoint trap"),
	SIGABRT(6, "Process abort signal"),
	SIGBUS(7, "Access to an undefined portion of a memory object"),
	SIGFPE(8, "Erroneous arithmetic operation"),
	SIGKILL(9, "Kill (cannot be caught or ignored"),
	SIGUSR1(10, "User-defined signal 1"),
	SIGSEGV(11, "Invalid memory reference"),
	SIGUSR2(12, "User-defined signal 2"),
	SIGPIPE(13, "Write on a pipe with no one to read it"),
	SIGALRM(14, "Alarm clock"),
	SIGTERM(15, "Termination signal"),
	SIGSTKFLT(16, "Coprocessor experienced a stack fault"),
	SIGCHLD(17, "Child process terminated, stopped, or continued"),
	SIGCONT(18, "Continue executing, if stopped"),
	SIGSTOP(19, "Stop executing (cannot be caught or ignored"),
	SIGTSTP(20, "Terminal stop signal"),
	SIGTTIN(21, "Background process attempting read"),
	SIGTTOU(22, "Background process attempting write"),
	SIGURG(23, "Out-of-band data is available at a socket"),
	SIGXCPU(24, "CPU time limit exceeded"),
	SIGXFSZ(25, "File size limit exceeded"),
	SIGVTALRM(26, "Virtual timer expired"),
	SIGPROF(27, "Profiling timer expired"),
	SIGWINCH(28, "Terminal window size changed"),
	SIGPOLL(29, "Pollable event"),
	SIGPWR(30, "Power failure"),
	SIGSYS(31, "Bad system call");

	private final int number;
	private final String explanation;

	LinuxSignal(int number, String explanation) {
		this.number = number;
		this.explanation = explanation;
	}

	/**
	 * @return the signal number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return a short explanation for this signal
	 */
	public String getExplanation() {
		return explanation;
	}

	/**
	 * Returns the signal for a given number.
	 *
	 * @param number the signal number
	 * @return the signal or empty if none
	 */
	public static Optional<LinuxSignal> forSignalNumber(int number) {
		return Arrays.stream(values()).filter(signal -> signal.getNumber() == number).findFirst();
	}

	/**
	 * Returns the signal for a given exit code, if any.
	 *
	 * @param exitCode the exit code
	 * @return the signal or empty if none
	 */
	public static Optional<LinuxSignal> forExitCode(int exitCode) {
		if (exitCode < 127) {
			return Optional.empty();
		}
		return forSignalNumber(exitCode - 128);
	}
}
