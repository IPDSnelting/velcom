package de.aaaaaaah.velcom.backend.access;

import static de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus.BENCHMARK_REQUIRED;
import static de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY;
import static de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus.NO_BENCHMARK_REQUIRED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.Repository.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class KnownCommitAccessTest {

	private static final int REPO_COUNT = 2;
	private static final int COMMITS_PER_REPO = 10;

	@TempDir
	Path testDir;
	DatabaseStorage dbStorage;

	KnownCommitWriteAccess knownCommitAccess;
	RepoId firstId = new RepoId(), secondId = new RepoId();
	List<CommitHash> allHashes = new ArrayList<>();
	List<CommitHash> firstHashes = new ArrayList<>();
	List<CommitHash> secondHashes = new ArrayList<>();
	CommitHash firstHash;

	@BeforeEach
	void setUp() throws SQLException {
		dbStorage = new DatabaseStorage("jdbc:sqlite:file:" + testDir.resolve("data.db"));
		knownCommitAccess = new KnownCommitWriteAccess(dbStorage);

		// Insert repo into db
		try (DSLContext db = dbStorage.acquireContext()) {
			RepositoryRecord record = db.newRecord(REPOSITORY);
			record.setId(firstId.getId().toString());
			record.setName("bla");
			record.setRemoteUrl("bla");
			record.insert();

			record.setId(secondId.getId().toString());
			record.insert();
		}

		for (int i = 0; i < COMMITS_PER_REPO; i++) {
			CommitHash hash = new CommitHash(UUID.randomUUID().toString());
			firstHashes.add(hash);
			allHashes.add(hash);

			if (i == 0) {
				firstHash = hash;
			}

			hash = new CommitHash(UUID.randomUUID().toString());
			secondHashes.add(hash);
			allHashes.add(hash);
		}
	}

	@AfterEach
	void breakDown() {
		if (dbStorage != null) {
			dbStorage.close();
		}
	}

	@Test
	public void testStatusOfASingleCommit() {
		// Try each status on a different commit
		for (int i = 0; i < BenchmarkStatus.values().length; i++) {
			BenchmarkStatus status = BenchmarkStatus.values()[i];
			CommitHash hash = firstHashes.get(i);

			assertFalse(knownCommitAccess.isKnown(firstId, hash));
			assertFalse(knownCommitAccess.isKnown(secondId, hash));
			knownCommitAccess.setBenchmarkStatus(firstId, hash, status);
			assertTrue(knownCommitAccess.isKnown(firstId, hash));
			assertFalse(knownCommitAccess.isKnown(secondId, hash)); // wrong repo
			assertEquals(status, knownCommitAccess.getBenchmarkStatus(firstId, hash));
		}

		// Try another status on same commit again (firstHash should have status with idx 0)
		BenchmarkStatus newStatus = BenchmarkStatus.values()[1];

		knownCommitAccess.setBenchmarkStatus(firstId, firstHash, newStatus);
		assertTrue(knownCommitAccess.isKnown(firstId, firstHash));
		assertEquals(newStatus, knownCommitAccess.getBenchmarkStatus(firstId, firstHash));
	}

	@ParameterizedTest
	@EnumSource(BenchmarkStatus.class)
	public void testSetStatusOnMultipleCommit(BenchmarkStatus status) {
		List<CommitHash> knownList = firstHashes.stream()
			.limit(COMMITS_PER_REPO / 2)
			.collect(toList());

		List<CommitHash> unknownList = firstHashes.stream()
			.skip(COMMITS_PER_REPO / 2)
			.collect(toList());

		knownCommitAccess.setBenchmarkStatus(firstId, knownList, status);

		for (CommitHash knownHash : knownList) {
			assertTrue(knownCommitAccess.isKnown(firstId, knownHash));
			assertFalse(knownCommitAccess.isKnown(secondId, knownHash)); // wrong repo
			assertEquals(status,
				knownCommitAccess.getBenchmarkStatus(firstId, knownHash));
		}

		for (CommitHash unknownHash : unknownList) {
			assertFalse(knownCommitAccess.isKnown(firstId, unknownHash));
			assertFalse(knownCommitAccess.isKnown(secondId, unknownHash));
		}

		for (CommitHash hashOnOtherRepo : secondHashes) {
			assertFalse(knownCommitAccess.isKnown(firstId, hashOnOtherRepo));
			assertFalse(knownCommitAccess.isKnown(secondId, hashOnOtherRepo));
		}
	}

	@ParameterizedTest
	@EnumSource(BenchmarkStatus.class)
	public void testHasKnownCommits(BenchmarkStatus status) {
		assertFalse(knownCommitAccess.hasKnownCommits(firstId));
		assertFalse(knownCommitAccess.hasKnownCommits(secondId));
		knownCommitAccess.setBenchmarkStatus(firstId, firstHash, status);
		assertTrue(knownCommitAccess.hasKnownCommits(firstId));
		assertFalse(knownCommitAccess.hasKnownCommits(secondId));
	}

	@Test
	public void testAllCommitsRequiringBenchmark() {
		Set<CommitHash> commitsThatRequireBenchmark = new HashSet<>(secondHashes);
		commitsThatRequireBenchmark.addAll(
			firstHashes.stream().skip(COMMITS_PER_REPO / 2).collect(toList())
		);

		// First half of first repo is NO_BENCHMARK_REQUIRED
		firstHashes.stream().limit(COMMITS_PER_REPO / 2).forEach(
			hash -> knownCommitAccess.setBenchmarkStatus(firstId, hash, NO_BENCHMARK_REQUIRED)
		);

		// Second half of first repo is BENCHMARK_REQUIRED
		firstHashes.stream().skip(COMMITS_PER_REPO / 2).forEach(
			hash -> knownCommitAccess.setBenchmarkStatus(firstId, hash, BENCHMARK_REQUIRED)
		);

		// First half of SECOND repo is BENCHMARK_REQUIRED
		secondHashes.stream().limit(COMMITS_PER_REPO / 2).forEach(
			hash -> knownCommitAccess.setBenchmarkStatus(secondId, hash, BENCHMARK_REQUIRED)
		);

		// Second half of SECOND repo is BENCHMARK_REQUIRED_MANUALLY
		secondHashes.stream().skip(COMMITS_PER_REPO / 2).forEach(
			hash -> knownCommitAccess.setBenchmarkStatus(secondId, hash,
				BENCHMARK_REQUIRED_MANUAL_PRIORITY)
		);

		// Now let's check
		Set<Pair<RepoId, CommitHash>> result = knownCommitAccess.getAllCommitsRequiringBenchmark();
		Set<CommitHash> resultHashes = result.stream().map(Pair::getSecond).collect(toSet());
		assertEquals(commitsThatRequireBenchmark, resultHashes);
	}

}
