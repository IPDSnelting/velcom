package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class JsonRepo {

	private final UUID id;
	private final String name;
	private final URI remoteUrl;
	private final List<String> untrackedBranches;
	private final List<String> trackedBranches;
	private final boolean hasToken;
	private final List<JsonDimension> dimensions;

	public JsonRepo(UUID id, String name, URI remoteUrl, List<String> untrackedBranches,
		List<String> trackedBranches, boolean hasToken, List<JsonDimension> dimensions) {

		this.id = id;
		this.name = name;
		this.remoteUrl = remoteUrl;
		this.untrackedBranches = untrackedBranches;
		this.trackedBranches = trackedBranches;
		this.hasToken = hasToken;
		this.dimensions = dimensions;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public URI getRemoteUrl() {
		return remoteUrl;
	}

	public List<String> getUntrackedBranches() {
		return untrackedBranches;
	}

	public List<String> getTrackedBranches() {
		return trackedBranches;
	}

	public boolean isHasToken() {
		return hasToken;
	}

	public List<JsonDimension> getDimensions() {
		return dimensions;
	}
}
