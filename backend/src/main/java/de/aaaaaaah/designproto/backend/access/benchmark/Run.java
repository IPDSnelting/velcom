package de.aaaaaaah.designproto.backend.access.benchmark;

import de.aaaaaaah.designproto.backend.access.commit.Commit;
import de.aaaaaaah.designproto.backend.access.commit.CommitAccess;
import de.aaaaaaah.designproto.backend.access.commit.CommitHash;
import de.aaaaaaah.designproto.backend.access.repo.Repo;
import de.aaaaaaah.designproto.backend.access.repo.RepoAccess;
import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A run is a single execution of the benchmark script.
 *
 * <p> It can either be successful, in which case it may contain multiple successful and failed
 * {@link Measurement}s, or it can be failed, in which case it only contains an error message and no
 * {@link Measurement}s. Specifically, a successful {@link Run} can still contain failed {@link
 * Measurement}s.
 */
public class Run {

	private final BenchmarkAccess benchmarkAccess;
	private final CommitAccess commitAccess;
	private final RepoAccess repoAccess;

	private final RunId id;
	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant startTime;
	private final Instant stopTime;
	@Nullable
	private final String errorMessage;

	public Run(BenchmarkAccess benchmarkAccess, CommitAccess commitAccess, RepoAccess repoAccess,
		RunId id, RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
		@Nullable String errorMessage) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;

		this.id = id;
		this.repoId = repoId;
		this.commitHash = commitHash;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.errorMessage = errorMessage;
	}

	public RunId getId() {
		return id;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public Repo getRepo() {
		return repoAccess.getRepo(repoId);
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public Commit getCommit() {
		return commitAccess.getCommit(repoId, commitHash);
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	public Optional<Collection<Measurement>> getMeasurements() {
		if (errorMessage == null) {
			return Optional.of(benchmarkAccess.getMeasurements(id));
		} else {
			return Optional.empty();
		}
	}

	// TODO: 27.12.19 hasError method?

	public Optional<String> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}

}
