package de.aaaaaaah.velcom.backend.access.token.hashalgorithm;

import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class Argon2Algorithm implements HashAlgorithm {

	public static final Argon2 argon = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
	public static final int parallelism = Runtime.getRuntime().availableProcessors();

	private final int memoryInKiB;
	private final int iterations;

	public Argon2Algorithm(int memoryInKiB, int iterations) {
		this.memoryInKiB = memoryInKiB;
		this.iterations = iterations;
	}

	@Override
	public String generateHash(AuthToken token) {
		char[] tokenChars = token.getToken().toCharArray();
		return argon.hash(iterations, memoryInKiB, parallelism, tokenChars);
	}

	@Override
	public boolean matches(String hash, AuthToken token) {
		return argon.verify(hash, token.getToken().toCharArray());
	}
}
