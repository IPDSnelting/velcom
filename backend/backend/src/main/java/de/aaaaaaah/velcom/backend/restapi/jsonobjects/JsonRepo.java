package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import java.util.Collection;
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

	public JsonRepo(Repo repo) {
		id = repo.getId().getId();
		name = repo.getName();

		branches = repo.getBranches().stream()
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toUnmodifiableList());

		trackedBranches = repo.getBranches().stream()
			.filter(Branch::isTracked)
			.map(Branch::getName)
			.map(BranchName::getName)
			.collect(Collectors.toUnmodifiableList());

		measurements = repo.getAvailableMeasurements().stream()
			.map(JsonMeasurementName::new)
			.collect(Collectors.toUnmodifiableList());

		remoteUrl = repo.getRemoteUrl().getUrl();
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
}
