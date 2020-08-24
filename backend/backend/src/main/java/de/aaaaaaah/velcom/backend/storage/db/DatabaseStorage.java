package de.aaaaaaah.velcom.backend.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.ServerMain;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteDataSource;

/**
 * Provides access to a database.
 */
public class DatabaseStorage {

	private final DSLContext context;
	private final Lock writeLock = new ReentrantLock();

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param config the config used to get the connection information for the database from
	 */
	public DatabaseStorage(GlobalConfig config) {
		this(config.getJdbcUrl());
	}

	/**
	 * Initializes the database storage.
	 *
	 * <p> Also performs database migrations, if necessary.
	 *
	 * @param jdbcUrl the jdbc url used to connect to the database
	 */
	public DatabaseStorage(String jdbcUrl) {
		migrate(jdbcUrl);

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		sqliteConfig.setJournalMode(JournalMode.WAL);

		SQLiteDataSource sqLiteDataSource = new SQLiteDataSource(sqliteConfig);

		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDataSource(sqLiteDataSource);
		hikariConfig.setPoolName("velcom-db-pool");
		hikariConfig.setMetricRegistry(ServerMain.getMetricRegistry());

		HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

		this.context = DSL.using(hikariDataSource, SQLDialect.SQLITE);
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

	/**
	 * Acquires read access to the database.
	 * @return a way to interact with the database in a read only way
	 */
	public DBReadAccess acquireReadAccess() {
		return new DBReadAccess(acquireContext());
	}

	/**
	 * Acquires read and write access to the database.
	 * @return a way to interact with the database
	 */
	public DBWriteAccess acquireWriteAccess() {
		return new DBWriteAccess(acquireContext(), this.writeLock);
	}

	/**
	 * Acquires a transaction that can only read from the database.
	 * @param handler the handler that is executed within the context of the transaction
	 */
	public void acquireReadTransaction(CheckedConsumer<DBReadAccess, Throwable> handler) {
		try (final DBReadAccess db = acquireReadAccess()) {
			db.dsl().transaction(cfg -> {
				try (DBReadAccess inTransactionDB = new DBReadAccess(cfg.dsl())) {
					handler.accept(inTransactionDB);
				}
			});
		}
	}

	/**
	 * Acquires a transaction that can read and write to the database.
	 * @param handler the handler that is executed within the context of the transaction
	 */
	public void acquireWriteTransaction(CheckedConsumer<DBWriteAccess, Throwable> handler) {
		try (final DBWriteAccess db = acquireWriteAccess()) {
			db.dsl().transaction(cfg -> {
				try (DBWriteAccess inTransactionDB = new DBWriteAccess(cfg.dsl(), this.writeLock)) {
					handler.accept(inTransactionDB);
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

}
