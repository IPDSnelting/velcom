package de.aaaaaaah.velcom.backend.storage.db;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.codegen.db.tables.Repo.REPO;

import de.aaaaaaah.velcom.backend.TestDb;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.nio.file.Path;
import java.util.Map;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseStorageTest {

	private TestDb testDb;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		testDb = new TestDb(tempDir);
	}

	@Test
	void canReadExistingInfo(@TempDir Path tempDir) {
		String repoId = new RepoId().getIdAsString();
		String repoName = "test repo";
		String repoRemoteUrl = "https://github.com/IPDSnelting/velcom.git";

		testDb.db().batchInsert(new RepoRecord(repoId, repoName, repoRemoteUrl)).execute();
		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());

		// Read without explicit transaction
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			RepoRecord record = db.dsl().selectFrom(REPO).fetchSingle();
			assertThat(record.getId()).isEqualTo(repoId);
			assertThat(record.getName()).isEqualTo(repoName);
			assertThat(record.getRemoteUrl()).isEqualTo(repoRemoteUrl);
		}

		// Read transaction, no return value
		databaseStorage.acquireReadTransaction(db -> {
			RepoRecord record = db.dsl().selectFrom(REPO).fetchSingle();
			assertThat(record.getId()).isEqualTo(repoId);
			assertThat(record.getName()).isEqualTo(repoName);
			assertThat(record.getRemoteUrl()).isEqualTo(repoRemoteUrl);
		});

		// Read transaction with return value
		RepoRecord record = databaseStorage.acquireReadTransaction(db -> {
			return db.dsl().selectFrom(REPO).fetchSingle();
		});
		assertThat(record.getId()).isEqualTo(repoId);
		assertThat(record.getName()).isEqualTo(repoName);
		assertThat(record.getRemoteUrl()).isEqualTo(repoRemoteUrl);
	}

	@Test
	void canWriteNewInfo(@TempDir Path tempDir) {
		String repoId1 = new RepoId().getIdAsString();
		String repoId2 = new RepoId().getIdAsString();
		String repoId3 = new RepoId().getIdAsString();
		String repoName1 = "test repo 1";
		String repoName2 = "test repo 2";
		String repoName3 = "test repo 3";
		String repoRemoteUrl = "https://github.com/IPDSnelting/velcom.git";

		TestDb testDb = new TestDb(tempDir);
		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());

		// Write without explicit transaction
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl().batchInsert(new RepoRecord(repoId1, repoName1, repoRemoteUrl)).execute();
		}

		// Write transaction, no return value
		databaseStorage.acquireWriteTransaction(db -> {
			db.dsl().batchInsert(new RepoRecord(repoId2, repoName2, repoRemoteUrl)).execute();
		});

		// Write transaction with return value
		databaseStorage.acquireWriteTransaction(db -> {
			db.dsl().batchInsert(new RepoRecord(repoId3, repoName3, repoRemoteUrl)).execute();
			return true;
		});

		// Check if all three repos have been added correctly
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Map<String, RepoRecord> records = db.dsl()
				.selectFrom(REPO)
				.stream()
				.collect(toMap(RepoRecord::getId, it -> it));

			assertThat(records).containsOnlyKeys(repoId1, repoId2, repoId3);
			assertThat(records.get(repoId1).getName()).isEqualTo(repoName1);
			assertThat(records.get(repoId2).getName()).isEqualTo(repoName2);
			assertThat(records.get(repoId3).getName()).isEqualTo(repoName3);
		}
	}

	@Test
	void transactionsPassThroughReturnValue() {
		DatabaseStorage databaseStorage = new DatabaseStorage(testDb.closeAndGetJdbcUrl());

		boolean returned;

		returned = databaseStorage.acquireReadTransaction(db -> true);
		assertThat(returned).isTrue();
		returned = databaseStorage.acquireReadTransaction(db -> false);
		assertThat(returned).isFalse();

		returned = databaseStorage.acquireWriteTransaction(db -> true);
		assertThat(returned).isTrue();
		returned = databaseStorage.acquireWriteTransaction(db -> false);
		assertThat(returned).isFalse();
	}
}
