package de.aaaaaaah.velcom.runner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * The runner's config file. Meant to be deserialized from JSON. Contains a comment section, some
 * values and one section for each backend the runner should connect to.
 */
@JsonIgnoreProperties("_comment")
public class RunnerConfig {

	private final String name;
	private final List<BackendEntry> backends;

	@JsonCreator
	public RunnerConfig(
		@JsonProperty(required = true) String name,
		@JsonProperty(required = true) List<BackendEntry> backends
	) {
		this.name = name;
		this.backends = backends;
	}

	public String getName() {
		return name;
	}

	public List<BackendEntry> getBackends() {
		return backends;
	}

	/**
	 * Section describing a single backend and how the runner should connect to it.
	 */
	public static class BackendEntry {

		private final URI address;
		private final String token;
		private final Path directory;

		@JsonCreator
		public BackendEntry(
			@JsonProperty(required = true) URI address,
			@JsonProperty(required = true) String token,
			@JsonProperty(required = true) Path directory
		) {
			this.address = address;
			this.token = token;
			this.directory = directory;
		}

		public URI getAddress() {
			return address;
		}

		public String getToken() {
			return token;
		}

		public Path getDirectory() {
			return directory;
		}
	}
}
