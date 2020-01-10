package de.aaaaaaah.designproto.backend.access;

import de.aaaaaaah.designproto.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.designproto.backend.access.commit.CommitAccess;
import de.aaaaaaah.designproto.backend.access.queue.QueueAccess;
import de.aaaaaaah.designproto.backend.access.repo.RepoAccess;
import de.aaaaaaah.designproto.backend.access.token.TokenAccess;

/**
 * The various access classes need instances of each other to pass their respective entities when
 * creating them. This class keeps track of the access classes and lets them access each other.
 */
public class AccessLayer {

	// TODO slim down this class to only the necessary access classes?

	private BenchmarkAccess benchmarkAccess;
	private CommitAccess commitAccess;
	private QueueAccess queueAccess;
	private RepoAccess repoAccess;
	private TokenAccess tokenAccess;

	/**
	 * Add a {@link BenchmarkAccess} to this layer.
	 *
	 * @param benchmarkAccess the access class
	 */
	public void registerBenchmarkAccess(BenchmarkAccess benchmarkAccess) {
		this.benchmarkAccess = benchmarkAccess;
	}

	/**
	 * Add a {@link CommitAccess} to this layer.
	 *
	 * @param commitAccess the access class
	 */
	public void registerCommitAccess(CommitAccess commitAccess) {
		this.commitAccess = commitAccess;
	}

	/**
	 * Add a {@link QueueAccess} to this layer.
	 *
	 * @param queueAccess the access class
	 */
	public void registerQueueAccess(QueueAccess queueAccess) {
		this.queueAccess = queueAccess;
	}

	/**
	 * Add a {@link RepoAccess} to this layer.
	 *
	 * @param repoAccess the access class
	 */
	public void registerRepoAccess(RepoAccess repoAccess) {
		this.repoAccess = repoAccess;
	}

	/**
	 * Add a {@link TokenAccess} to this layer.
	 *
	 * @param tokenAccess the access class
	 */
	public void registerTokenAccess(TokenAccess tokenAccess) {
		this.tokenAccess = tokenAccess;
	}

	public BenchmarkAccess getBenchmarkAccess() {
		return benchmarkAccess;
	}

	public CommitAccess getCommitAccess() {
		return commitAccess;
	}

	public QueueAccess getQueueAccess() {
		return queueAccess;
	}

	public RepoAccess getRepoAccess() {
		return repoAccess;
	}

	public TokenAccess getTokenAccess() {
		return tokenAccess;
	}
}
