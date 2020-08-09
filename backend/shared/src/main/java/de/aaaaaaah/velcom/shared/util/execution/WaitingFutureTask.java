package de.aaaaaaah.velcom.shared.util.execution;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future task that waits for the underlying worker to die before returning from get.
 *
 * @param <T> the type of the task
 */
class WaitingFutureTask<T> implements Future<T> {

	private final Thread worker;
	private final FutureTask<T> underlying;

	public WaitingFutureTask(FutureTask<T> task) {
		this.underlying = task;
		this.worker = new Thread(underlying, "MyFutureTask worker");
		this.worker.start();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return underlying.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return underlying.isCancelled();
	}

	@Override
	public boolean isDone() {
		return underlying.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (isCancelled()) {
			worker.join();
		}
		return underlying.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit)
		throws InterruptedException, ExecutionException, TimeoutException {
		if (isCancelled()) {
			worker.join(unit.toMillis(timeout));
			throw new CancellationException("Execution cancelled, thread died");
		}
		return underlying.get(timeout, unit);
	}
}
