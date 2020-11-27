package de.aaaaaaah.velcom.backend.access.hashalgorithm;

import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

/**
 * Uses the argon library to provide a password hashing algorithm.
 */
public class Argon2Algorithm implements HashAlgorithm {

	private final Argon2 argon;

	private final int iterations;
	private final int memory; // in KiB
	private final int parallelism;

	public Argon2Algorithm(Argon2Types type, int iterations, int memory, int parallelism) {
		this.argon = Argon2Factory.create(type);
		this.iterations = iterations;
		this.memory = memory;
		this.parallelism = parallelism;
	}

	@Override
	public String generateHash(AuthToken token) {
		char[] tokenChars = token.getToken().toCharArray();
		return argon.hash(iterations, memory, parallelism, tokenChars);
	}

	@Override
	public boolean matches(String hash, AuthToken token) {
		return argon.verify(hash, token.getToken().toCharArray());
	}
}
