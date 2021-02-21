package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.List;
import java.util.UUID;

public class JsonRepo {

	private final UUID id;
	private final String name;
	private final String remoteUrl;
	private final List<JsonBranch> branches;
	private final List<JsonDimension> dimensions;

	public JsonRepo(UUID id, String name, String remoteUrl, List<JsonBranch> branches,
		List<JsonDimension> dimensions) {

		this.id = id;
		this.name = name;
		this.remoteUrl = remoteUrl;
		this.branches = branches;
		this.dimensions = dimensions;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public List<JsonBranch> getBranches() {
		return branches;
	}

	public List<JsonDimension> getDimensions() {
		return dimensions;
	}
}
