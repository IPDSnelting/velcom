package de.aaaaaaah.velcom.backend;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.codegen.db.tables.records.BranchRecord;
import org.jooq.codegen.db.tables.records.CommitRelationshipRecord;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.MeasurementValueRecord;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.jooq.codegen.db.tables.records.RunRecord;
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
			remoteUrl.getUrl(),
			null,
			null
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
		boolean everTracked, String author, Instant authorDate, String committer, Instant committerDate,
		String message) {

		dslContext.batchInsert(new KnownCommitRecord(
			repoId.getIdAsString(),
			commitHash.getHash(),
			reachable,
			tracked,
			everTracked,
			author,
			authorDate,
			committer,
			committerDate,
			message
		)).execute();
	}

	public void addCommit(RepoId repoId, CommitHash commitHash, boolean reachable, boolean tracked,
		boolean everTracked, String message, Instant authorDate, Instant committerDate) {

		addCommit(repoId, commitHash, reachable, tracked, everTracked, "author", authorDate,
			"committer", committerDate, message);
	}

	public void addCommit(RepoId repoId, CommitHash commitHash) {

		addCommit(repoId, commitHash, true, true, true, "author", Instant.now(), "committer",
			Instant.now(), "message");
	}

	public void addCommitRel(RepoId repoId, CommitHash parent, CommitHash child) {
		dslContext.batchInsert(new CommitRelationshipRecord(
			repoId.getIdAsString(),
			parent.getHash(),
			child.getHash()
		)).execute();
	}

	public void addDimension(DimensionInfo dimensionInfo) {
		dslContext.batchInsert(new DimensionRecord(
			dimensionInfo.getDimension().getBenchmark(),
			dimensionInfo.getDimension().getMetric(),
			dimensionInfo.getUnit().getName(),
			dimensionInfo.getInterpretation().getTextualRepresentation(),
			dimensionInfo.isSignificant()
		)).execute();
	}

	public void addDimension(Dimension dimension) {
		addDimension(new DimensionInfo(dimension));
	}

	public void addRun(RunId runId, String author, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime, Either<CommitSource, TarSource> source,
		@Nullable RunError error) {

		dslContext.batchInsert(new RunRecord(
			runId.getIdAsString(),
			author,
			runnerName,
			runnerInfo,
			startTime,
			stopTime,
			source.consume(cs -> Optional.of(cs.getRepoId()), TarSource::getRepoId)
				.map(RepoId::getIdAsString)
				.orElse(null),
			source.getLeft()
				.map(CommitSource::getHash)
				.map(CommitHash::getHash)
				.orElse(null),
			source.getRight()
				.map(TarSource::getDescription)
				.orElse(null),
			Optional.ofNullable(error)
				.map(RunError::getType)
				.map(RunErrorType::getTextualRepresentation)
				.orElse(null),
			Optional.ofNullable(error)
				.map(RunError::getMessage)
				.orElse(null)
		)).execute();
	}

	public void addRun(RunId runId, Either<CommitSource, TarSource> source) {
		addRun(runId, "author", "runnerName", "runnerInfo", Instant.now(), Instant.now(), source, null);
	}

	public UUID addMeasurement(RunId runId, Dimension dimension, @Nullable Unit unit,
		@Nullable Interpretation interpretation, Either<MeasurementError, MeasurementValues> result) {

		UUID measurementId = UUID.randomUUID();

		dslContext.batchInsert(new MeasurementRecord(
			measurementId.toString(),
			runId.getIdAsString(),
			dimension.getBenchmark(),
			dimension.getMetric(),
			Optional.ofNullable(unit)
				.map(Unit::getName)
				.orElse(null),
			Optional.ofNullable(interpretation)
				.map(Interpretation::getTextualRepresentation)
				.orElse(null),
			result.getLeft()
				.map(MeasurementError::getErrorMessage)
				.orElse(null)
		)).execute();

		result.getRight().ifPresent(values -> dslContext
			.batchInsert(values.getValues().stream()
				.map(value -> new MeasurementValueRecord(measurementId.toString(), value))
				.collect(toList()))
			.execute());

		return measurementId;
	}

	public UUID addMeasurement(RunId runId, Dimension dimension) {
		return addMeasurement(runId, dimension, Unit.DEFAULT, Interpretation.DEFAULT,
			Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d))));
	}
}
