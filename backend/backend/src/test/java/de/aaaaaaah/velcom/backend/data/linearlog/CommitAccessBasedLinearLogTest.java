package de.aaaaaaah.velcom.backend.data.linearlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.exceptions.CommitLogException;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommitAccessBasedLinearLogTest {

	private CommitReadAccess commitAccess;
	private RepoReadAccess repoAccess;
	private LinearLog log;

	private RepoId repoId;
	private Collection<BranchName> branchNames;
	private Collection<Branch> branches;

	private Commit c1;
	private Commit c2;
	private Commit c3;
	private List<Commit> commits;

	@BeforeEach
	void setup() {
		commitAccess = mock(CommitReadAccess.class);
		repoAccess = mock(RepoReadAccess.class);
		log = new CommitAccessBasedLinearLog(commitAccess, repoAccess);

		repoId = new RepoId(UUID.randomUUID());
		branchNames = Stream.of("foo", "bar", "baz")
			.map(BranchName::fromName)
			.collect(Collectors.toUnmodifiableList());
		branches = branchNames.stream()
			.map(branchName -> {
				Branch branch = mock(Branch.class);
				when(branch.getName()).thenReturn(branchName);
				return branch;
			})
			.collect(Collectors.toUnmodifiableList());

		c1 = new Commit(repoId, new CommitHash("hash1"), List.of(), "author", Instant.now(),
			"committer", Instant.now(), "message");
		c2 = new Commit(repoId, new CommitHash("hash2"), List.of(), "author", Instant.now(),
			"committer", Instant.now(), "message");
		c3 = new Commit(repoId, new CommitHash("hash3"), List.of(), "author", Instant.now(),
			"committer", Instant.now(), "message");
		commits = List.of(c1, c2, c3);
	}

	@Test
	void walkProperly() throws LinearLogException {
		when(commitAccess.getCommitLog(repoId, branchNames)).thenReturn(commits.stream());
		Stream<Commit> stream = log.walk(repoId, branchNames);
		verify(commitAccess).getCommitLog(repoId, branchNames);
		assertEquals(commits, stream.collect(Collectors.toUnmodifiableList()));
	}

	@Test
	void canHandleCommitLogExceptions() {
		when(commitAccess.getCommitLog(repoId, branchNames)).thenThrow(
			new CommitLogException(repoId, branchNames, null));
		assertThrows(LinearLogException.class, () -> log.walk(repoId, branchNames));
	}

	@Test
	void canFindPrevious() {
		when(repoAccess.getTrackedBranches(repoId)).thenReturn(branches);
		when(commitAccess.getCommitLog(repoId, branchNames)).then(ignore -> commits.stream());

		assertEquals(Optional.of(c2), log.getPreviousCommit(c1));
		assertEquals(Optional.of(c3), log.getPreviousCommit(c2));
		assertEquals(Optional.empty(), log.getPreviousCommit(c3));
	}

	@Test
	void canFindPreviousNext() {
		when(repoAccess.getTrackedBranches(repoId)).thenReturn(branches);
		when(commitAccess.getCommitLog(repoId, branchNames)).then(ignore -> commits.stream());

		assertEquals(new Pair<>(Optional.of(c2), Optional.empty()), log.getPrevNextCommits(c1));
		assertEquals(new Pair<>(Optional.of(c3), Optional.of(c1)), log.getPrevNextCommits(c2));
		assertEquals(new Pair<>(Optional.empty(), Optional.of(c2)), log.getPrevNextCommits(c3));
	}

}