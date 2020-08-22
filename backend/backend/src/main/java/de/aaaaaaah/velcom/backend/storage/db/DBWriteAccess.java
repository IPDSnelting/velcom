package de.aaaaaaah.velcom.backend.storage.db;

import java.util.concurrent.locks.Lock;
import org.jooq.DSLContext;
import org.jooq.DeleteWhereStep;
import org.jooq.InsertSetStep;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdateSetFirstStep;

public class DBWriteAccess extends DBReadAccess {

	public DBWriteAccess(DSLContext ctx, Lock lock) {
		super(ctx, lock);
	}

	public <R extends Record> InsertSetStep<R> insertInto(Table<R> table) {
		return ctx.insertInto(table);
	}

	public <R extends Record> DeleteWhereStep<R> deleteFrom(Table<R> table) {
		return ctx.deleteFrom(table);
	}

	public <R extends Record> UpdateSetFirstStep<R> update(Table<R> table) {
		return ctx.update(table);
	}

}
