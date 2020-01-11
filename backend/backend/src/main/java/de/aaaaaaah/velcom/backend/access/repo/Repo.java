package de.aaaaaaah.velcom.backend.access.repo;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import java.util.Collection;
import java.util.Objects;

/**
 * A git repository that is being tracked and benchmarked by the system. A {@link Repo} has a list
 * of branches that are being tracked, meaning that new commits which appear on those branches will
 * automatically be benchmarked.
 *
 * <p> Multiple repositories with the same remote URL can be added to the system. Each of those
 * repositories can have a different branch configuration.
 */
public class Repo {

	private final RepoAccess repoAccess;
	private final TokenAccess tokenAccess;
	private final BenchmarkAccess benchmarkAccess;

	private final RepoId id;

	Repo(RepoAccess repoAccess, TokenAccess tokenAccess, BenchmarkAccess benchmarkAccess,
		RepoId id) {

		this.repoAccess = repoAccess;
		this.tokenAccess = tokenAccess;
		this.benchmarkAccess = benchmarkAccess;

		this.id = id;
	}

	// immutable
	public RepoId getId() {
		return id;
	}

	public String getName() {
		return repoAccess.getName(id);
	}

	public RemoteUrl getRemoteUrl() {
		return repoAccess.getRemoteUrl(id);
	}

	public Collection<Branch> getBranches() {
		return repoAccess.getBranches(id);
	}

	public Collection<Branch> getTrackedBranches() {
		return repoAccess.getTrackedBranches(id);
	}

	public Collection<MeasurementName> getAvailableMeasurements() {
		return benchmarkAccess.getAvailableMeasurements(id);
	}

	/**
	 * @param token the token to test
	 * @return whether the token is valid for (i. e. should grant access to) this repo
	 */
	public boolean isValidToken(AuthToken token) {
		return tokenAccess.isValidToken(id, token);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Repo repo = (Repo) o;
		return Objects.equals(id, repo.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Repo{" +
			"id=" + id +
			'}';
	}
}
