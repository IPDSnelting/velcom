package de.aaaaaaah.velcom.backend.newaccess.tokenaccess.hashalgorithm;

import de.aaaaaaah.velcom.backend.newaccess.tokenaccess.entities.AuthToken;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

/**
 * Uses the argon library to provide a password hashing algorithm.
 */
public class Argon2Algorithm implements HashAlgorithm {

	private static final Argon2 ARGON = Argon2Factory.create(Argon2Types.ARGON2i);

	private final int iterations;
	private final int memory; // in KiB
	private final int parallelism;

	public Argon2Algorithm(int iterations, int memory, int parallelism) {
		this.iterations = iterations;
		this.memory = memory;
		this.parallelism = parallelism;
	}

	@Override
	public String generateHash(AuthToken token) {
		char[] tokenChars = token.getToken().toCharArray();
		return ARGON.hash(iterations, memory, parallelism, tokenChars);
	}

	@Override
	public boolean matches(String hash, AuthToken token) {
		return ARGON.verify(hash, token.getToken().toCharArray());
	}
}
