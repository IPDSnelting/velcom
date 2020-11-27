package de.aaaaaaah.velcom.backend.access.hashalgorithm;

import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

/**
 * V1 of the hashing scheme using Argon2. No longer in use, do not attempt to generate hashes with
 * it - it is only able to verify them.
 */
public class V1Argon2Algorithm implements HashAlgorithm {

	private static final Argon2 ARGON = Argon2Factory.create(Argon2Types.ARGON2id);

	@Override
	public String generateHash(AuthToken token) {
		throw new IllegalStateException("This algorithm is no longer used!");
	}

	@Override
	public boolean matches(String hash, AuthToken token) {
		return ARGON.verify(hash, token.getToken().toCharArray());
	}
}
