package de.aaaaaaah.velcom.backend.storage.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.aaaaaaah.velcom.backend.GlobalConfig;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Provides access to a database.
 */
public class DatabaseStorage {

	private final HikariDataSource dataSource;

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param config the config used to get the connection information for the database from
	 */
	public DatabaseStorage(GlobalConfig config) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(config.getJdbcUrl());

		config.getJdbcUsername().ifPresent(hikariConfig::setUsername);
		config.getJdbcPassword().ifPresent(hikariConfig::setPassword);

		dataSource = new HikariDataSource(hikariConfig);

		migrate();
	}

	/**
	 * Initializes the database storage.
	 *
	 * <p>
	 * Also performs database migrations, if necessary.
	 *
	 * @param jdbcUrl the jdbc url used to connect to the database
	 */
	public DatabaseStorage(String jdbcUrl) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(jdbcUrl);

		dataSource = new HikariDataSource(hikariConfig);

		migrate();
	}

	private void migrate() {
		Flyway flyway = Flyway.configure()
			.dataSource(this.dataSource)
			.load();

		flyway.migrate();
	}

	/**
	 * @return a {@link DataSource} instance providing access to the database
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * @return a {@link DSLContext} instance providing jooq functionality along with a connection to
	 * 	the database
	 */
	public DSLContext acquireContext() {
		return DSL.using(dataSource, SQLDialect.SQLITE);
	}

	/**
	 * Closes the database storage.
	 */
	public void close() {
		this.dataSource.close();
	}

}
