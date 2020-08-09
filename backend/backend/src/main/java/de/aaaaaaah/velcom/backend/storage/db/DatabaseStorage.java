package de.aaaaaaah.velcom.backend.storage.db;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
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

	/**
	 * @return a {@link DSLContext} instance providing jooq functionality along with a connection to
	 * 	the database
	 */
	public DSLContext acquireContext() {
		return this.context;
	}

	/**
	 * Acquires a transaction and passes the {@link DSLContext} that is associated with the
	 * transaction to the given handler. If an exception occurs during execution of the handler, the
	 * transaction will be cancelled and the exception is passed back to the caller of this method.
	 *
	 * @param handler the handler used to pass queries to the transaction
	 */
	public void acquireTransaction(CheckedConsumer<DSLContext, Throwable> handler) {
		Objects.requireNonNull(handler);

		try (DSLContext dslContext = this.acquireContext()) {
			dslContext.transaction(configuration -> {
				try (DSLContext transactionContext = DSL.using(configuration)) {
					handler.accept(transactionContext);
				}
			});
		}
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
