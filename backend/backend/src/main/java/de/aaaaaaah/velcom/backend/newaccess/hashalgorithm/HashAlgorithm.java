package de.aaaaaaah.velcom.backend.newaccess.hashalgorithm;


import de.aaaaaaah.velcom.backend.newaccess.entities.AuthToken;

/**
 * Represents a certain hashing algorithm used for storing tokens.
 */
public interface HashAlgorithm {

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
