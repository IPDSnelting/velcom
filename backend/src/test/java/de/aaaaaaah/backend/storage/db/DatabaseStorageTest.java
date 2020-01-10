package de.aaaaaaah.backend.storage.db;

import static org.jooq.codegen.db.Tables.KNOWN_COMMIT;
import static org.jooq.codegen.db.Tables.REPOSITORY;
import static org.jooq.codegen.db.Tables.REPO_TOKEN;
import static org.jooq.codegen.db.Tables.RUN;
import static org.jooq.codegen.db.Tables.RUN_MEASUREMENT;
import static org.jooq.codegen.db.Tables.RUN_MEASUREMENT_VALUE;
import static org.jooq.codegen.db.Tables.TRACKED_BRANCH;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.codegen.db.tables.records.RepoTokenRecord;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.jooq.codegen.db.tables.records.RunMeasurementRecord;
import org.jooq.codegen.db.tables.records.RunMeasurementValueRecord;
import org.jooq.codegen.db.tables.records.RunRecord;
import org.jooq.codegen.db.tables.records.TrackedBranchRecord;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DatabaseStorageTest {

	private static final Path DB_FILE_PATH = Paths.get("data/test_data.db");
	private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE_PATH.toString();

	DatabaseStorage databaseStorage;

	@BeforeEach
	void setUp() {
		databaseStorage = new DatabaseStorage(JDBC_URL);
	}

	@AfterEach
	void tearDown() throws IOException {
		databaseStorage.close();
		Files.deleteIfExists(DB_FILE_PATH);
	}

	@Test
	@Disabled
	public void testSelect() {
		DSLContext db = DSL.using(databaseStorage.getDataSource(), SQLDialect.SQLITE);

		db.select().from(REPOSITORY).where(REPOSITORY.ID.greaterThan("abc")).fetch();
	}

	@Test
	@Disabled
	public void testInsert() {
		try (DSLContext db = DSL.using(databaseStorage.getDataSource(), SQLDialect.SQLITE)) {
			String repoId = UUID.randomUUID().toString();

			RepositoryRecord repoRecord = db.newRecord(REPOSITORY);
			repoRecord.setId(repoId);
			repoRecord.setName("Velcom");
			repoRecord.setRemoteUrl("bla");
			repoRecord.insert();

			KnownCommitRecord knownCommitRecord = db.newRecord(KNOWN_COMMIT);
			knownCommitRecord.setRepoId(repoId);
			knownCommitRecord.setHash("f3c12144172b81880e39c274f74daa53d76797a5");
			knownCommitRecord.insert();

			TrackedBranchRecord trackedBranchRecord = db.newRecord(TRACKED_BRANCH);
			trackedBranchRecord.setRepoId(repoId);
			trackedBranchRecord.setBranchName("master");

			RunRecord benchmarkRunRecord = db.newRecord(RUN);
			benchmarkRunRecord.setRepoId(repoId);
			benchmarkRunRecord.setCommitHash("f3c12144172b81880e39c274f74daa53d76797a5");
			benchmarkRunRecord.setStartTime(Timestamp.from(Instant.now()));
			benchmarkRunRecord.setEndTime(Timestamp.from(Instant.now()));
			benchmarkRunRecord.insert();

			assertNotNull(benchmarkRunRecord.getId());

			RunMeasurementRecord runMeasurementRecord = db.newRecord(RUN_MEASUREMENT);
			runMeasurementRecord.setRunId(benchmarkRunRecord.getId());
			runMeasurementRecord.setBenchmark("compile");
			runMeasurementRecord.setMetric("speed");
			runMeasurementRecord.setUnit("cm");
			runMeasurementRecord.setInterpretation("LIB");
			runMeasurementRecord.insert();

			assertNotNull(runMeasurementRecord.getId());

			RunMeasurementValueRecord runValueRecord = db.newRecord(RUN_MEASUREMENT_VALUE);
			runValueRecord.setMeasurementId(runMeasurementRecord.getId());
			runValueRecord.setValue(1.2);
			runValueRecord.insert();
		}
	}

	@Test
	@Disabled
	public void testArgon() {
		Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

		char[] pw = "12345".toCharArray();

		int memoryInKiB = 488281; // = 500MB
		int parallelism = Runtime.getRuntime().availableProcessors();
		long maxMillisecs = 2000;

		int iterations = Argon2Helper.findIterations(
			argon2,
			maxMillisecs,
			memoryInKiB,
			parallelism
		);

		String hash = argon2.hash(iterations, memoryInKiB, parallelism, pw);

		boolean success = argon2.verify(hash, pw);
		assertTrue(success);

		argon2.wipeArray(pw);

		try (DSLContext db = DSL.using(databaseStorage.getDataSource(), SQLDialect.SQLITE)) {
			RepoTokenRecord repoTokenRecord = db.newRecord(REPO_TOKEN);
			repoTokenRecord.setRepoId(UUID.randomUUID().toString());
			repoTokenRecord.setToken(hash);
			repoTokenRecord.setHashAlgo(1);
			repoTokenRecord.insert();
		}
	}

	@Test
	@Disabled
	public void testStream() {
		try (DSLContext db = DSL.using(databaseStorage.getDataSource(), SQLDialect.SQLITE)) {
			String repoId = UUID.randomUUID().toString();
			String commitHash = "f3c12144172b81880e39c274f74daa53d76797a5";

			for (int i = 0; i < 100; i++) {
				RunRecord benchmarkRunRecord = db.newRecord(RUN);
				benchmarkRunRecord.setRepoId(repoId);
				benchmarkRunRecord.setCommitHash(commitHash);
				benchmarkRunRecord.setStartTime(Timestamp.from(Instant.now()));
				benchmarkRunRecord.setEndTime(Timestamp.from(Instant.now()));
				benchmarkRunRecord.insert();
			}

			db.select().from(RUN)
				.where(RUN.REPO_ID.eq(repoId).and(RUN.COMMIT_HASH.eq(commitHash)))
				.stream()
				.limit(10)
				.forEach(System.out::println);
		}
	}

}
