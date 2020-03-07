package de.aaaaaaah.velcom.backend.access;

import static org.jooq.codegen.db.tables.RepoToken.REPO_TOKEN;
import static org.jooq.impl.DSL.selectFrom;

import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.hashalgorithm.Argon2Algorithm;
import de.aaaaaaah.velcom.backend.access.hashalgorithm.HashAlgorithm;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RepoTokenRecord;

public class TokenReadAccess {

	protected final DatabaseStorage databaseStorage;

	protected final Map<Integer, HashAlgorithm> hashAlgorithms;
	protected final HashAlgorithm currentHashAlgorithm;
	protected final int currentHashAlgorithmId;

	protected final String adminTokenHash;

	public TokenReadAccess(DatabaseStorage databaseStorage, AuthToken adminToken, int hashMemory,
		int hashIterations) {

		hashAlgorithms = new HashMap<>();

		// Argon2
		currentHashAlgorithmId = 1;
		currentHashAlgorithm = new Argon2Algorithm(hashMemory, hashIterations);
		hashAlgorithms.put(currentHashAlgorithmId, currentHashAlgorithm);

		this.databaseStorage = databaseStorage;
		this.adminTokenHash = currentHashAlgorithm.generateHash(adminToken);
	}

	protected HashAlgorithm getAlgorithmById(int id) {
		return Objects.requireNonNull(hashAlgorithms.get(id));
	}

	// Querying tokens

	/**
	 * @param repoId a repo's id
	 * @param token a token to test
	 * @return whether the token is valid for the specified repo. Defaults to false if the repo does
	 * 	not exist or has no token
	 */
	public boolean isValidToken(RepoId repoId, AuthToken token) {
		Objects.requireNonNull(repoId);
		Objects.requireNonNull(token);

		try (DSLContext db = databaseStorage.acquireContext()) {
			RepoTokenRecord tokenRecord = db.selectFrom(REPO_TOKEN)
				.where(REPO_TOKEN.REPO_ID.eq(repoId.getId().toString()))
				.fetchOptional()
				.orElse(null);

			if (tokenRecord == null) {
				return false; // no entry in database => default to false
			}

			String hash = tokenRecord.getToken();
			int usedAlgoId = tokenRecord.getHashAlgo();

			if (usedAlgoId != currentHashAlgorithmId) {
				// Older hash algorithm was used for this token
				HashAlgorithm usedHashAlgo = getAlgorithmById(
					usedAlgoId);

				// Check if provided token is even correct
				if (!usedHashAlgo.matches(hash, token)) {
					return false; // Can't migrate if provided token is incorrect
				}

				// Overwrite hash with newer (currently used) hash algorithm
				setTokenProtected(repoId, token);

				// Since provided token was correct, return true
				return true;
			} else {
				// Current hash algorithm is used
				return currentHashAlgorithm.matches(hash, token);
			}
		}
	}

	/**
	 * @param token a token to test
	 * @return whether the token should give admin powers
	 */
	public boolean isValidAdminToken(AuthToken token) {
		Objects.requireNonNull(token);
		return currentHashAlgorithm.matches(adminTokenHash, token);
	}

	/**
	 * @param id a repo's id
	 * @return whether the repo has a token associated with it
	 */
	public boolean hasToken(RepoId id) {
		Objects.requireNonNull(id);
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetchExists(selectFrom(REPO_TOKEN)
				.where(REPO_TOKEN.REPO_ID.eq(id.getId().toString()))
			);
		}
	}

	// Modifying tokens

	protected void setTokenProtected(RepoId id, @Nullable AuthToken authToken) {
		Objects.requireNonNull(id);

		if (authToken == null) {
			// Remove from database
			try (DSLContext db = databaseStorage.acquireContext()) {
				db.deleteFrom(REPO_TOKEN)
					.where(REPO_TOKEN.REPO_ID.eq(id.getId().toString()))
					.execute();
			}
		} else {
			String hash = currentHashAlgorithm.generateHash(authToken);

			// Insert hash into database
			try (DSLContext db = databaseStorage.acquireContext()) {
				db.insertInto(REPO_TOKEN)
					.set(REPO_TOKEN.REPO_ID, id.getId().toString())
					.set(REPO_TOKEN.HASH_ALGO, currentHashAlgorithmId)
					.set(REPO_TOKEN.TOKEN, hash)
					.onDuplicateKeyUpdate()
					.set(REPO_TOKEN.HASH_ALGO, currentHashAlgorithmId)
					.set(REPO_TOKEN.TOKEN, hash)
					.execute();
			}
		}
	}
}
