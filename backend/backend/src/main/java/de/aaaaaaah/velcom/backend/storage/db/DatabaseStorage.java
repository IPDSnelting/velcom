package de.aaaaaaah.velcom.backend.storage.db;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
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

	private final Connection connection;
	private final DSLContext context;

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param config the config used to get the connection information for the database from
	 */
	public DatabaseStorage(GlobalConfig config) throws SQLException {
		this(config.getJdbcUrl());
	}

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param jdbcUrl the jdbc url used to connect to the database
	 */
	public DatabaseStorage(String jdbcUrl) throws SQLException {
		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		sqliteConfig.setJournalMode(JournalMode.WAL);

		SQLiteDataSource dataSource = new SQLiteDataSource(sqliteConfig);
		dataSource.setUrl(jdbcUrl);

		this.connection = dataSource.getConnection();

		this.context = DSL.using(this.connection, SQLDialect.SQLITE);

		migrate(dataSource);
	}

	private void migrate(DataSource dataSource) {
		Flyway flyway = Flyway.configure()
			.dataSource(dataSource)
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
	 * Closes the database storage.
	 */
	public void close() {
		try {
			this.connection.close();
		} catch (SQLException ignore) {
		}
	}

}
