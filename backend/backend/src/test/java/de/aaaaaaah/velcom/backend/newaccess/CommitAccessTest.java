package de.aaaaaaah.velcom.backend.newaccess;

import static java.time.Duration.ofHours;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.aaaaaaah.velcom.backend.TestCommit;
import de.aaaaaaah.velcom.backend.TestRepo;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import javax.annotation.Nullable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileSnapshot;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

public class CommitAccessTest {

	private static Instant timer = Instant.now();

	private static Instant time() {
		timer = timer.plus(ofHours(1));
		timer = timer.truncatedTo(ChronoUnit.SECONDS); // jgit is not that precise
		return timer;
	}

	private static final List<BranchName> BRANCHES = List.of(
		BranchName.fromName("master"),
		BranchName.fromName("otherbranch"),
		BranchName.fromName("thirdbranch")
	);

	private static final List<TestCommit> COMMITS = List.of(
		new TestCommit("Initial commit", time()),
		new TestCommit("Second commit", time()),
		new TestCommit("Began working on some feature", "otherbranch", time()),
		new TestCommit("Some bugfixes", "master", time()),
		new TestCommit("Finished feature", "otherbranch", time()),
		new TestCommit("abc", "thirdbranch", time()),
		new TestCommit("final commit", "master", time())
	);

	@TempDir
	Path testDir;
	Path repoStoragePath;

	RepoStorage repoStorage;
	RepoId repoId = new RepoId();
	List<CommitHash> hashes = new ArrayList<>();

	TestRepo testRepo;

	CommitReadAccess commitAccess;

	@BeforeEach
	public void setUp() throws IOException, GitAPIException, AddRepositoryException {
		((Logger) LoggerFactory.getLogger(FileSnapshot.class)).setLevel(Level.OFF); // Too much spam

		repoStoragePath = testDir.resolve("data").resolve("repos");
		repoStorage = new RepoStorage(repoStoragePath);

		// Create test repository
		testRepo = new TestRepo(testDir.resolve("test_repo"));

		for (TestCommit COMMIT : COMMITS) {
			RevCommit revCommit = testRepo.commit(COMMIT);
			hashes.add(new CommitHash(revCommit.getId().getName()));
		}

		// Add test repository to repo storage
		repoStorage.addRepository(repoId.getDirectoryName(),
			"file://" + testDir.resolve("test_repo"));

		commitAccess = new CommitReadAccess(repoStorage);
	}

	@Test
	public void testGetCommit() {
		for (int i = 0; i < hashes.size(); i++) {
			Commit commit = commitAccess.getCommit(repoId, hashes.get(i));
			RevCommit revCommit = testRepo.getRevCommit(COMMITS.get(i)).orElseThrow();

			assertEquals(commit.getRepoId(), repoId);
			assertCommitEquals(commit, revCommit);
		}
	}

	@Test
	public void testGetCommits() {
		List<CommitHash> toTest = hashes.subList(0, hashes.size() / 2);

		List<Commit> resultCommits = commitAccess.getCommits(repoId, toTest);
		List<CommitHash> resultHashes = resultCommits.stream()
			.map(Commit::getHash).collect(toList());

		assertThat(resultHashes).containsExactlyInAnyOrderElementsOf(toTest);

		for (Commit commit : resultCommits) {
			RevCommit revCommit = testRepo.getRevCommit(commit.getHash().getHash()).orElseThrow();

			assertCommitEquals(commit, revCommit);
		}

		assertTrue(commitAccess.getCommits(repoId, Collections.emptyList()).isEmpty());
	}

	@Test
	public void testGetCommitWalk() throws IOException {
		TestCommit startTestCommit = COMMITS.get(5);
		var startHash = new CommitHash(testRepo.getCommitHash(startTestCommit).orElseThrow());
		Commit start = commitAccess.getCommit(repoId, startHash);
		Commit next;
		Collection<Commit> parents;

		try (CommitWalk walk = commitAccess.getCommitWalk(start)) {
			Commit sameStart = walk.getStart();
			assertCommitEquals(start, sameStart);

			parents = walk.getParents(start);
			assertEquals(1, parents.size());
			next = parents.iterator().next();
			assertCommitEquals(next, COMMITS.get(4));

			parents = walk.getParents(next);
			assertEquals(1, parents.size());
			next = parents.iterator().next();
			assertCommitEquals(next, COMMITS.get(2));

			parents = walk.getParents(next);
			assertEquals(1, parents.size());
			next = parents.iterator().next();
			assertCommitEquals(next, COMMITS.get(1));

			parents = walk.getParents(next);
			assertEquals(1, parents.size());
			next = parents.iterator().next();
			assertCommitEquals(next, COMMITS.get(0));
		}
	}

	@ParameterizedTest
	@MethodSource("instantProvider")
	public void testGetCommitsBetween(@Nullable Instant start, @Nullable Instant end) {
		// First, calculate which commits should be included
		List<String> includedHashes = new ArrayList<>();

		for (TestCommit commit : COMMITS) {
			Instant time = commit.getAuthorDate().orElseThrow();
			if ((start == null || time.isAfter(start))
				&& (end == null || time.isBefore(end))) {

				includedHashes.add(testRepo.getCommitHash(commit).orElseThrow());
			}
		}

		// Now, call getCommitsBetween
		Map<CommitHash, Commit> commits = commitAccess.getCommitsBetween(
			repoId, BRANCHES, start, end
		);

		List<String> resultHashes = commits.keySet().stream().map(CommitHash::getHash)
			.collect(toList());

		assertThat(resultHashes).containsExactlyInAnyOrderElementsOf(includedHashes);
	}

	@Test
	public void testGetCommitsBetweenWithReversedInstants() {
		assertThrows(IllegalArgumentException.class,
			() -> commitAccess.getCommitsBetween(repoId, BRANCHES,
				COMMITS.get(3).getAuthorDate().orElseThrow(),
				COMMITS.get(1).getAuthorDate().orElseThrow()
			)
		);
	}

	@Test
	public void testCommitLog() {
		try (Stream<Commit> commitLog = commitAccess.getCommitLog(repoId, BRANCHES)) {
			List<CommitHash> resultHashes = commitLog.map(Commit::getHash).collect(toList());
			assertThat(resultHashes).containsExactlyInAnyOrderElementsOf(hashes);
		}

		List<BranchName> branchList = BRANCHES.subList(2, 3);

		try (Stream<Commit> commitLog = commitAccess.getCommitLog(repoId, branchList)) {
			List<CommitHash> resultHashes = commitLog.map(Commit::getHash).collect(toList());
			assertThat(resultHashes).containsExactlyInAnyOrder(
				hashes.get(0), hashes.get(1), hashes.get(2), hashes.get(4), hashes.get(5)
			);
		}
	}

	private static Stream<Arguments> instantProvider() {
		Builder<Arguments> builder = Stream.builder();

		for (int i = 0; i < COMMITS.size(); i++) {
			Instant start = COMMITS.get(i).getAuthorDate().orElseThrow();
			builder.accept(Arguments.of(start, null));
			builder.accept(Arguments.of(null, start));

			for (int j = i; j < COMMITS.size(); j++) {
				Instant end = COMMITS.get(j).getAuthorDate().orElseThrow();
				builder.accept(Arguments.of(start, end));
			}
		}

		builder.accept(Arguments.of(null, null));

		return builder.build();
	}

	private void assertCommitEquals(Commit commit, RevCommit revCommit) {
		assertEquals(revCommit.getId().getName(), commit.getHash().getHash());
		assertEquals(revCommit.getFullMessage(), commit.getMessage());

		PersonIdent authorId = revCommit.getAuthorIdent();
		String revAuthor = authorId.getName() + " <" + authorId.getEmailAddress() + ">";
		assertEquals(revAuthor, commit.getAuthor());

		assertEquals(revCommit.getAuthorIdent().getWhen().toInstant(), commit.getAuthorDate());

		PersonIdent committerId = revCommit.getCommitterIdent();
		String revCommitter = committerId.getName() + " <" + committerId.getEmailAddress() + ">";
		assertEquals(revCommitter, commit.getCommitter());

		assertEquals(revCommit.getCommitterIdent().getWhen().toInstant(),
			commit.getCommitterDate());

		Collection<CommitHash> parentHashes = commit.getParentHashes();
		List<CommitHash> revCommitParentHashes = new ArrayList<>();

		for (RevCommit parent : revCommit.getParents()) {
			revCommitParentHashes.add(new CommitHash(parent.getId().getName()));
		}

		assertThat(parentHashes).containsExactlyInAnyOrderElementsOf(revCommitParentHashes);
	}

	private void assertCommitEquals(Commit commit, Commit other) {
		assertEquals(commit.getRepoId(), other.getRepoId());
		assertEquals(commit.getHash(), other.getHash());
		assertEquals(commit.getParentHashes(), other.getParentHashes());
		assertEquals(commit.getAuthor(), other.getAuthor());
		assertEquals(commit.getAuthorDate(), other.getAuthorDate());
		assertEquals(commit.getCommitter(), other.getCommitter());
		assertEquals(commit.getCommitterDate(), other.getCommitterDate());
		assertEquals(commit.getMessage(), other.getMessage());
	}

	private void assertCommitEquals(Commit commit, TestCommit testCommit) {
		RevCommit revCommit = testRepo.getRevCommit(testCommit).orElseThrow();
		assertCommitEquals(commit, revCommit);
	}

}
