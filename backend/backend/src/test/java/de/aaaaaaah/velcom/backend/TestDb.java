package de.aaaaaaah.velcom.backend;

import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteDataSource;

/**
 * This class sets up a sqlite db that can then be used to test access classes without mocking the
 * {@link de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage}.
 */
public class TestDb {

	private static final String TEST_DB_NAME = "test.db";

	private final String jdbcUrl;
	private final DSLContext dslContext;

	/**
	 * Create a new sqlite db. This class can choose its own names for the db files, but they must be
	 * placed in the specified directory. The sqlite db is created with WAL. Foreign key checks are
	 * enabled.
	 *
	 * @param tmpDir the directory the db files are created in
	 */
	public TestDb(Path tmpDir) {
		jdbcUrl = "jdbc:sqlite:file:" + (tmpDir.resolve(TEST_DB_NAME).toAbsolutePath());

		// Migrate to get a useful schema

		Flyway flyway = Flyway.configure()
			.dataSource(jdbcUrl, "", "")
			.load();
		flyway.migrate();

		// Connect to db with correct settings

		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		sqliteConfig.setJournalMode(JournalMode.WAL);

		SQLiteDataSource sqLiteDataSource = new SQLiteDataSource(sqliteConfig);
		sqLiteDataSource.setUrl(jdbcUrl);

		dslContext = DSL.using(sqLiteDataSource, SQLDialect.SQLITE);
	}

	/**
	 * @return a {@link DSLContext} that can be used to interact with the database
	 */
	public DSLContext db() {
		return dslContext;
	}

	/**
	 * Close the database connection and return a jdbc url that can be used to connect to the file.
	 * WARNING: After this function is called, calling other functions on this object is forbidden.
	 *
	 * @return a jdbc url that can be used to connect to the database
	 */
	public String closeAndGetJdbcUrl() {
		dslContext.close();
		return jdbcUrl;
	}
}
