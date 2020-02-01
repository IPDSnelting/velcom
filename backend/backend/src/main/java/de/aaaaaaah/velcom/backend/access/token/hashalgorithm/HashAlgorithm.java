package de.aaaaaaah.velcom.backend.access.token.hashalgorithm;

import de.aaaaaaah.velcom.backend.access.token.AuthToken;

/**
 * Represents a certain hashing algorithm used for storing tokens.
 */
public interface HashAlgorithm {

//	/**
//	 * The password hashing algorithm Argon2id. See https://github.com/P-H-C/phc-winner-argon2
//	 */
//	ARGON2ID(1) {
//		private final Argon2Algorithm argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
//
//		private final int memoryInKiB = 5 * 1024; // = 5MiB
//		private final int parallelism = Runtime.getRuntime().availableProcessors();
//		private final long maxMillis = 500;
//		private final int iterations = Math.max(1, Argon2Helper.findIterations(
//			argon, maxMillis, memoryInKiB, parallelism
//		));
//
//		@Override
//		public String generateHash(AuthToken token) {
//			char[] tokenChars = token.getToken().toCharArray();
//			return argon.hash(iterations, memoryInKiB, parallelism, tokenChars);
//		}
//
//		@Override
//		public boolean matches(String hash, AuthToken token) {
//			return argon.verify(hash, token.getToken().toCharArray());
//		}
//	};
//
//	private final int id;
//
//	HashAlgorithm(int id) {
//		this.id = id;
//	}

	/**
	 * Generates the appropriate hash for the given token
	 *
	 * @param token the token
	 * @return the hash
	 */
	 String generateHash(AuthToken token);

	/**
	 * Checks whether nor not the provided token verifies against the provided hash.
	 *
	 * @param hash the hash
	 * @param token the token
	 * @return returns true if the token verifies against the hash
	 */
	 boolean matches(String hash, AuthToken token);

}
