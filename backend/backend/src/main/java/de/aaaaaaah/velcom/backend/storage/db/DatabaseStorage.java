package de.aaaaaaah.velcom.backend.storage.db;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.flywaydb.core.Flyway;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;

/**
 * Provides access to a database.
 */
public class DatabaseStorage {

	private final Connection connection;
	private final DSLContext context;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param config the config used to get the connection information for the database from
	 * @throws SQLException if sql goes wrong
	 */
	public DatabaseStorage(GlobalConfig config) throws SQLException {
		this(config.getJdbcUrl());
	}

	/**
	 * Initializes the database storage.
	 *
	 * <p> Also performs database migrations, if necessary.
	 *
	 * @param jdbcUrl the jdbc url used to connect to the database
	 * @throws SQLException if sql goes wrong
	 */
	public DatabaseStorage(String jdbcUrl) throws SQLException {
		migrate(jdbcUrl);

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		sqliteConfig.setJournalMode(JournalMode.WAL);

		connection = sqliteConfig.createConnection(jdbcUrl);

		Configuration jooqConfig = new DefaultConfiguration()
			.set(new ThreadSafeConnectionProvider(connection))
			.set(SQLDialect.SQLITE);

		context = DSL.using(jooqConfig);
	}

	/**
	 * By default, sqlite doesn't check for foreign key constraints. By opening a new db connection
	 * only based on the db url, flyway can take advantage of this and migrations become much more
	 * performant. This does however mean that each migration has to check foreign key consistency
	 * itself using {@code PRAGMA foreign_key_check}.
	 *
	 * @param jdbcUrl the url to the database
	 */
	private void migrate(String jdbcUrl) {
		Flyway flyway = Flyway.configure()
			.dataSource(jdbcUrl, "", "")
			.load();

		flyway.migrate();
	}

	public DBReadAccess acquireReadAccess() {
		return new DBReadAccess(acquireContext(), this.lock.readLock());
	}

	public DBWriteAccess acquireWriteAccess() {
		return new DBWriteAccess(acquireContext(), this.lock.writeLock());
	}

	public void acquireReadTransaction(CheckedConsumer<DBReadAccess, Throwable> handler) {
		try (final DBReadAccess db = acquireReadAccess()) {
			db.dsl().transaction(configuration -> {
				try (DSLContext inTransactionCTX = DSL.using(configuration)) {
					try (DBReadAccess inTransactionDB = new DBReadAccess(
						inTransactionCTX, this.lock.readLock()
					)) {
						handler.accept(inTransactionDB);
					}
				}
			});
		}
	}

	public void acquireWriteTransaction(CheckedConsumer<DBWriteAccess, Throwable> handler) {
		try (final DBWriteAccess db = acquireWriteAccess()) {
			db.dsl().transaction(configuration -> {
				try (DSLContext inTransactionCTX = DSL.using(configuration)) {
					try (DBWriteAccess inTransactionDB = new DBWriteAccess(
						inTransactionCTX, this.lock.writeLock()
					)) {
						handler.accept(inTransactionDB);
					}
				}
			});
		}
	}

	/**
	 * @return a {@link DSLContext} instance providing jooq functionality along with a connection to
	 * 	the database
	 */
	private DSLContext acquireContext() {
		return this.context;
	}

	/**
	 * Closes the database storage.
	 */
	public void close() {
		try {
			this.connection.close();
		} catch (SQLException ignore) {
		}
	}

}
