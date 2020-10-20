package de.aaaaaaah.velcom.backend.newaccess.dimensionaccess;

import static org.jooq.codegen.db.tables.Measurement.MEASUREMENT;
import static org.jooq.codegen.db.tables.Run.RUN;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DimensionReadAccess {

	protected final DatabaseStorage databaseStorage;

	public DimensionReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	public Set<Dimension> getAvailableDimensions(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectDistinct(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.isNotNull())
				.stream()
				.map(record -> new Dimension(record.value1(), record.value2()))
				.collect(Collectors.toSet());
		}
	}

	public Map<RepoId, Set<Dimension>> getAvailableDimensions(Collection<RepoId> repoIds) {
		Set<String> repoIdStrings = repoIds.stream()
			.map(RepoId::getIdAsString)
			.collect(Collectors.toSet());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectDistinct(RUN.REPO_ID, MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(RUN.REPO_ID.in(repoIdStrings))
				.and(RUN.COMMIT_HASH.isNotNull())
				.stream()
				.collect(Collectors.groupingBy(
					record -> RepoId.fromString(record.value1()),
					Collectors.mapping(
						record -> new Dimension(record.value2(), record.value3()),
						Collectors.toSet()
					)
				));
		}
	}
}
