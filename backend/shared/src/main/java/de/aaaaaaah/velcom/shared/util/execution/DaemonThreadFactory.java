package de.aaaaaaah.velcom.shared.util.execution;

import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

/**
 * A {@link ThreadFactory} that returns daemon threads.
 */
public class DaemonThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(@Nonnull Runnable r) {
		Thread thread = new Thread(r);
		thread.setDaemon(true);
		return thread;
	}
}
