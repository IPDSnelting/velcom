package de.aaaaaaah.velcom.backend.storage.db;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows read and write access to a database.
 */
public class DBWriteAccess extends DBReadAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(DBWriteAccess.class);

	private final Lock lock;

	public DBWriteAccess(DSLContext ctx, Lock lock) {
		super(ctx);
		this.lock = Objects.requireNonNull(lock);
		this.lock.lock();
	}

	@Override
	public void close() {
		try {
			super.close();
		} finally {
			lock.unlock();
		}
	}

	public void vacuum() {
		LOGGER.info("Vacuuming database...");
		ctx.execute("VACUUM");
		LOGGER.info("Vacuuming completed.");
	}
}
