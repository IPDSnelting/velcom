package de.aaaaaaah.velcom.backend.access.token;

import static org.jooq.codegen.db.tables.RepoToken.REPO_TOKEN;
import static org.jooq.impl.DSL.selectFrom;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.token.hashalgorithm.Argon2Algorithm;
import de.aaaaaaah.velcom.backend.access.token.hashalgorithm.HashAlgorithm;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RepoTokenRecord;

/**
 * This class abstracts away access to the {@link AuthToken}s which are stored in the db.
 *
 * <p> TODO add more details on how tokens are not stored in plain text etc.
 */
public class TokenAccess {

	private final HashAlgorithm currentHashAlgorithm;
	private final int currentHashAlgorithmId;
	private final Map<Integer, HashAlgorithm> hashAlgorithms;

	private final DatabaseStorage databaseStorage;

	private final String adminTokenHash;

	/**
	 * This constructor also registers the {@link CommitAccess} in the accessLayer.
	 *
	 * @param config the global configuration file
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 * @param adminToken the admin token from the config
	 */
	public TokenAccess(GlobalConfig config, AccessLayer accessLayer,
		DatabaseStorage databaseStorage, AuthToken adminToken) {

		// Argon2
		currentHashAlgorithmId = 1;
		currentHashAlgorithm =
			new Argon2Algorithm(config.getHashMemory(), config.getHashIterations());

		hashAlgorithms = new HashMap<>();
		hashAlgorithms.put(currentHashAlgorithmId, currentHashAlgorithm);

		this.databaseStorage = databaseStorage;
		this.adminTokenHash = currentHashAlgorithm.generateHash(adminToken);

		accessLayer.registerTokenAccess(this);
	}

	private HashAlgorithm getAlgorithmById(int id) {
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
				HashAlgorithm usedHashAlgo = getAlgorithmById(usedAlgoId);

				// Check if provided token is even correct
				if (!usedHashAlgo.matches(hash, token)) {
					return false; // Can't migrate if provided token is incorrect
				}

				// Overwrite hash with newer (currently used) hash algorithm
				setToken(repoId, token);

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
		return currentHashAlgorithm.matches(adminTokenHash, token);
	}

	// Modifying tokens

	public void setToken(RepoId id, @Nullable AuthToken authToken) {
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

	/**
	 * @param id a repo's id
	 * @return whether the repo has a token associated with it
	 */
	public boolean hasToken(RepoId id) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetchExists(selectFrom(REPO_TOKEN)
				.where(REPO_TOKEN.REPO_ID.eq(id.getId().toString()))
			);
		}
	}
}
