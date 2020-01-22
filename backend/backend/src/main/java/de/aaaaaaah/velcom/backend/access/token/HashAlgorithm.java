package de.aaaaaaah.velcom.backend.access.token;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;

/**
 * Represents a certain hashing algorithm used for storing tokens.
 */
public enum HashAlgorithm {

	/**
	 * The password hashing algorithm Argon2id. See https://github.com/P-H-C/phc-winner-argon2
	 */
	ARGON2ID(1) {
		private final Argon2 argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

		private final int memoryInKiB = 488281; // = 500MB
		private final int parallelism = Runtime.getRuntime().availableProcessors();
		private final long maxMillis = 750;
		private final int iterations = Math.max(1, Argon2Helper.findIterations(
			argon, maxMillis, memoryInKiB, parallelism
		));

		@Override
		public String generateHash(AuthToken token) {
			char[] tokenChars = token.getToken().toCharArray();
			return argon.hash(iterations, memoryInKiB, parallelism, tokenChars);
		}

		@Override
		public boolean matches(String hash, AuthToken token) {
			return argon.verify(hash, token.getToken().toCharArray());
		}
	};

	private final int id;

	HashAlgorithm(int id) {
		this.id = id;
	}

	/**
	 * @return Returns the unique identifier of this hash algorithm
	 */
	public int getId() {
		return id;
	}

	/**
	 * Generates the appropriate hash for the given token
	 *
	 * @param token the token
	 * @return the hash
	 */
	public abstract String generateHash(AuthToken token);

	/**
	 * Checks whether nor not the provided token verifies against the provided hash.
	 *
	 * @param hash the hash
	 * @param token the token
	 * @return returns true if the token verifies against the hash
	 */
	public abstract boolean matches(String hash, AuthToken token);

	/**
	 * Tries to find the hash algorithm corresponding to the specified id
	 *
	 * @param hashAlgoId the id of the hash algorithm
	 * @return the hash algorithm, if found
	 * @throws IllegalArgumentException if no hash algorithm was found for the specified id
	 */
	public static HashAlgorithm fromId(int hashAlgoId) {
		for (HashAlgorithm algo : HashAlgorithm.values()) {
			if (algo.getId() == hashAlgoId) {
				return algo;
			}
		}

		throw new IllegalArgumentException("invalid hash algo id: " + hashAlgoId);
	}

}
