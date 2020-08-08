package de.aaaaaaah.velcom.backend.restapi.authentication;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.security.Principal;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response.Status;

public class RepoUser implements Principal {

	@Nullable
	private final RepoId repoId;

	public RepoUser(@Nullable RepoId repoId) {
		this.repoId = repoId;
	}

	@Override
	public String getName() {
		return getRepoId().map(RepoId::toString).orElse("");
	}

	public Optional<RepoId> getRepoId() {
		return Optional.ofNullable(repoId);
	}

	public boolean mayAccessRepo(RepoId repoId) {
		// If this user has no repo id, they're an admin and thus have access to all repos.
		return getRepoId().map(repoId::equals).orElse(true);
	}

	public boolean isAdmin() {
		return getRepoId().isEmpty();
	}

	/**
	 * Helper function for use in API endpoints to quickly check whether a user is allowed to access
	 * a repo.
	 *
	 * @param repoId the repo the user is trying to access
	 * @throws ClientErrorException if the user is not allowed to access the repo
	 */
	public void guardRepoAccess(RepoId repoId) throws ClientErrorException {
		if (!mayAccessRepo(repoId)) {
			throw new ClientErrorException(Status.UNAUTHORIZED);
		}
	}

	/**
	 * Helper function for use in API endpoints to quickly check whether a user has admin
	 * permissions.
	 *
	 * @throws ClientErrorException if the user doesn't have admin permissions
	 */
	public void guardAdminAccess() throws ClientErrorException {
		if (!isAdmin()) {
			throw new ClientErrorException(Status.UNAUTHORIZED);
		}
	}
}
