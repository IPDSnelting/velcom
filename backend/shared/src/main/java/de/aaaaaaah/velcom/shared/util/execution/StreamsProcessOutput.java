package de.aaaaaaah.velcom.shared.util.execution;

import java.util.concurrent.Future;

/**
 * A future that also provides snapshots of the current standard out and standard error of the
 * process it waits on.
 */
public interface StreamsProcessOutput<T> extends Future<T> {

	/**
	 * @return a snapshot of the current standard output of the process
	 */
	String getCurrentStdOut();

	/**
	 * @return a snapshot of the current standard error
	 */
	String getCurrentStdErr();
}
