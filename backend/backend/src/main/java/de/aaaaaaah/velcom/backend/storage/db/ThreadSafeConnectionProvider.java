package de.aaaaaaah.velcom.backend.storage.db;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

/**
 * A thread safe implementation of {@link org.jooq.impl.DefaultConnectionProvider} using a simple
 * locking mechanism.
 */
public class ThreadSafeConnectionProvider implements ConnectionProvider {

	private final Connection connection;
	private final Lock lock = new ReentrantLock();

	public ThreadSafeConnectionProvider(Connection connection) {
		this.connection = Objects.requireNonNull(connection);
	}

	@Override
	public Connection acquire() throws DataAccessException {
		this.lock.lock();
		return this.connection;
	}

	@Override
	public void release(Connection connection) throws DataAccessException {
		this.lock.unlock();
	}

}
