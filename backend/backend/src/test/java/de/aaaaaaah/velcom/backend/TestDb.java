package de.aaaaaaah.velcom.backend;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.nio.file.Path;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.codegen.db.tables.records.BranchRecord;
import org.jooq.codegen.db.tables.records.CommitRelationshipRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.codegen.db.tables.records.RepoRecord;
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

	public void addRepo(RepoId repoId, String name, RemoteUrl remoteUrl) {
		dslContext.batchInsert(new RepoRecord(
			repoId.getIdAsString(),
			name,
			remoteUrl.getUrl()
		)).execute();
	}

	public void addRepo(RepoId repoId) {
		addRepo(repoId, "repo name", new RemoteUrl("https://foo.bar/baz.git"));
	}

	public void addBranch(RepoId repoId, BranchName name, CommitHash commitHash, boolean tracked) {
		dslContext.batchInsert(new BranchRecord(
			repoId.getIdAsString(),
			name.getName(),
			commitHash.getHash(),
			tracked
		)).execute();
	}

	public void addCommit(RepoId repoId, CommitHash commitHash, boolean reachable, boolean tracked,
		String author, Instant authorDate, String committer, Instant committerDate, String message) {

		dslContext.batchInsert(new KnownCommitRecord(
			repoId.getIdAsString(),
			commitHash.getHash(),
			reachable,
			tracked,
			author,
			authorDate,
			committer,
			committerDate,
			message
		)).execute();
	}

	public void addCommit(RepoId repoId, CommitHash commitHash, boolean reachable, boolean tracked,
		String message) {

		addCommit(repoId, commitHash, reachable, tracked, "author", Instant.now(), "committer",
			Instant.now(), message);
	}

	public void addCommitRel(RepoId repoId, CommitHash parent, CommitHash child) {
		dslContext.batchInsert(new CommitRelationshipRecord(
			repoId.getIdAsString(),
			parent.getHash(),
			child.getHash()
		)).execute();
	}
}
