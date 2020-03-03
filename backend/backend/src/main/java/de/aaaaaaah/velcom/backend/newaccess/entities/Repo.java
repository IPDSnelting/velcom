package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.util.Collection;

/**
 * A git repository that is being tracked and benchmarked by the system. A {@link Repo} has a list
 * of branches that are being tracked, meaning that new commits which appear on those branches will
 * automatically be benchmarked.
 *
 * <p> Multiple repositories with the same remote URL can be added to the system. Each of those
 * repositories can have a different branch configuration.
 */
public class Repo {

	private final RepoId repoId;
	private final String name;
	private final RemoteUrl remoteUrl;
	private final Collection<Branch> trackedBranches;

	public Repo(RepoId repoId, String name, RemoteUrl remoteUrl,
		Collection<Branch> trackedBranches) {

		this.repoId = repoId;
		this.name = name;
		this.remoteUrl = remoteUrl;
		this.trackedBranches = trackedBranches;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public String getName() {
		return name;
	}

	public RemoteUrl getRemoteUrl() {
		return remoteUrl;
	}

	public Collection<Branch> getTrackedBranches() {
		return trackedBranches;
	}

}