package de.aaaaaaah.velcom.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EitherTest {

	@Test
	void testRight() {
		Either<Integer, Boolean> either = Either.ofRight(true);
		assertTrue(either.isRight());
		assertFalse(either.isLeft());
		assertEquals(Optional.of(true), either.getRight());
		assertEquals(Optional.empty(), either.getLeft());
	}

	@Test
	void testLeft() {
		Either<Integer, Boolean> either = Either.ofLeft(33);
		assertFalse(either.isRight());
		assertTrue(either.isLeft());
		assertEquals(Optional.empty(), either.getRight());
		assertEquals(Optional.of(33), either.getLeft());
	}

	@Test
	void testEquals() {
		Either<Integer, Boolean> left = Either.ofLeft(24);
		Either<Integer, Boolean> right = Either.ofRight(false);
		Either<Boolean, Integer> weird = Either.ofRight(24);

		assertEquals(Either.ofLeft(24), left);
		assertNotEquals(Either.ofLeft(23), left);

		assertEquals(Either.ofRight(false), right);
		assertNotEquals(Either.ofRight(true), right);

		assertEquals(Either.ofRight(24), weird);
		assertNotEquals(Either.ofRight(12), weird);

		assertNotEquals(left, right);
		assertNotEquals(left, weird);
		assertNotEquals(right, weird);
	}

}