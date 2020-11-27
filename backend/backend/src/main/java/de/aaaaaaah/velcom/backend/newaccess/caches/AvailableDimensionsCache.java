package de.aaaaaaah.velcom.backend.newaccess.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Caches repos' available dimensions. Needs to be invalidated when runs are added or deleted.
 */
public class AvailableDimensionsCache {

	private static final int MAXIMUM_SIZE = 500;

	private final Cache<RepoId, Set<Dimension>> cache;

	public AvailableDimensionsCache() {
		cache = Caffeine.newBuilder()
			.maximumSize(MAXIMUM_SIZE)
			.build();
	}

	public Map<RepoId, Set<Dimension>> getAvailableDimensions(DimensionReadAccess dimensionAccess,
		Collection<RepoId> repoIds) {

		return cache.getAll(
			repoIds,
			missingIdsIt -> {
				List<RepoId> missingIds = new ArrayList<>();
				missingIdsIt.forEach(missingIds::add);
				return dimensionAccess.getAvailableDimensions(missingIds);
			}
		);
	}

	public Set<Dimension> getAvailableDimensionsFor(DimensionReadAccess dimensionReadAccess,
		RepoId repoId) {

		return cache.get(repoId, dimensionReadAccess::getAvailableDimensions);
	}

	/**
	 * Invalidate a specific repo's entry.
	 *
	 * @param repoId the id of the repo whose dimensions to remove from the cache
	 */
	public void invalidate(RepoId repoId) {
		cache.invalidate(repoId);
	}
}
