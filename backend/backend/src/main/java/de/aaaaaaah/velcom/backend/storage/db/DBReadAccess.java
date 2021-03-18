package de.aaaaaaah.velcom.backend.storage.db;

import java.io.Closeable;
import java.util.Objects;
import org.jooq.DSLContext;

/**
 * Allows read only access to a database.
 */
public class DBReadAccess implements Closeable {

	protected final DSLContext ctx;

	public DBReadAccess(DSLContext ctx) {
		this.ctx = Objects.requireNonNull(ctx);
	}

	@Override
	public void close() {
		ctx.close();
	}

	public DSLContext dsl() {
		return ctx;
	}
}
