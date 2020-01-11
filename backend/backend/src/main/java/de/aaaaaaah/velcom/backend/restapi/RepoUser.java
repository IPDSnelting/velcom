package de.aaaaaaah.velcom.backend.restapi;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
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

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static boolean mayAccessRepo(Optional<RepoUser> user, RepoId repoId) {
		// If this user has no repo id, they're an admin and thus have access to all repos.
		return user.map(repoUser -> repoUser.getRepoId().map(repoId::equals).orElse(true))
			.orElse(false);
	}

	/**
	 * Helper function for use in API endpoints to quickly check whether a user is allowed to access
	 * a repo.
	 *
	 * @param user the user
	 * @param repoId the repo the user is trying to access
	 * @throws ClientErrorException if the user is not allowed to access the repo
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static void guardRepoAccess(Optional<RepoUser> user, RepoId repoId)
		throws ClientErrorException {

		if (!mayAccessRepo(user, repoId)) {
			throw new ClientErrorException(Status.UNAUTHORIZED);
		}
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static boolean mayAccessAdmin(Optional<RepoUser> user) {
		// If this user has no repo id, they're an admin and thus have access to all repos.
		return user.map(repoUser -> repoUser.getRepoId().isEmpty()).orElse(false);
	}

	/**
	 * Helper function for use in API endpoints to quickly check whether a user has admin
	 * permissions.
	 *
	 * @param user the user
	 * @throws ClientErrorException if the user doesn't have admin permissions
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static void guardAdminAccess(Optional<RepoUser> user) throws ClientErrorException {
		if (!mayAccessAdmin(user)) {
			throw new ClientErrorException(Status.UNAUTHORIZED);
		}
	}

	@Override
	public String getName() {
		return getRepoId().map(RepoId::toString).orElse("");
	}

	public Optional<RepoId> getRepoId() {
		return Optional.ofNullable(repoId);
	}
}
