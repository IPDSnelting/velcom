package de.aaaaaaah.velcom.backend.newaccess.hashalgorithm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.newaccess.entities.AuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Argon2AlgorithmTest {

	private static final int MEMORY_IN_KIB = 5120;
	private static final int ITERATIONS = 50;
	private static final AuthToken TOKEN = new AuthToken("*****");

	private final Argon2Algorithm algo = new Argon2Algorithm(MEMORY_IN_KIB, ITERATIONS);

	@Test
	void testGenerateAndMatches() {
		String hash = algo.generateHash(TOKEN);

		assertNotEquals("*****", hash);

		assertTrue(algo.matches(hash, TOKEN));
	}

	@ParameterizedTest
	@ValueSource(strings = {"12345", " *****", "***** ", "", " ", "123"})
	void testSomeTokens(String tokenStr) {
		AuthToken tokenToTest = new AuthToken(tokenStr);

		assertFalse(algo.matches(algo.generateHash(TOKEN), tokenToTest));
		assertFalse(algo.matches(algo.generateHash(tokenToTest), TOKEN));
		assertTrue(algo.matches(algo.generateHash(tokenToTest), tokenToTest));
	}

}