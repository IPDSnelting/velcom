package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.shared.GitProperties;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for debugging. Not specified in the API and can change at any time.
 */
@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugEndpoint {

	@GET
	public GetReply get() {
		return new GetReply(
			GitProperties.getBuildTime(),
			GitProperties.getHash()
		);
	}

	private static class GetReply {

		private String buildTime;
		private String backendHash;

		public GetReply(String buildTime, String backendHash) {
			this.buildTime = buildTime;
			this.backendHash = backendHash;
		}

		public String getBuildTime() {
			return buildTime;
		}

		public String getBackendHash() {
			return backendHash;
		}
	}
}
