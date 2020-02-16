package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A helper class for serialization representing a repository (not a local repository though).
 */
public class JsonRepo {

	private final UUID id;
	private final String name;
	private final Collection<String> branches;
	private final Collection<String> trackedBranches;
	private final Collection<JsonMeasurementName> measurements;
	private final String remoteUrl;
	private final boolean hasToken;

	public JsonRepo(Repo repo, Collection<Branch> branches,
		Collection<MeasurementName> measurements, boolean hasToken) {

		this.id = repo.getRepoId().getId();
		this.name = repo.getName();

		this.branches = branches.stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toUnmodifiableList());

		this.trackedBranches = repo.getTrackedBranches().stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toUnmodifiableList());

		this.measurements = measurements.stream()
			.map(JsonMeasurementName::new)
			.collect(Collectors.toUnmodifiableList());

		this.remoteUrl = repo.getRemoteUrl().getUrl();
		this.hasToken = hasToken;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Collection<String> getBranches() {
		return branches;
	}

	public Collection<String> getTrackedBranches() {
		return trackedBranches;
	}

	public Collection<JsonMeasurementName> getMeasurements() {
		return measurements;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public boolean getHasToken() {
		return hasToken;
	}
}
