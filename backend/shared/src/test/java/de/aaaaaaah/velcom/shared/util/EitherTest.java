package de.aaaaaaah.velcom.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Test
	void mapLeft() {
		var mappedLeft = Either.ofLeft(20).mapLeft(it -> it + 40);
		assertThat(mappedLeft.isLeft()).isTrue();
		assertThat(mappedLeft.getLeft()).isPresent();
		assertThat(mappedLeft.getRight()).isEmpty();
		assertThat(mappedLeft.getLeft()).isPresent().contains(60);

		var mappedRight = Either.ofRight(20).mapLeft(it -> "d");
		assertThat(mappedRight.isRight()).isTrue();
		assertThat(mappedRight.getLeft()).isEmpty();
		assertThat(mappedRight.getRight()).isPresent().contains(20);
	}

	@Test
	void mapRight() {
		var mappedRight = Either.ofRight(20).mapRight(it -> it + 40);
		assertThat(mappedRight.isRight()).isTrue();
		assertThat(mappedRight.getRight()).isPresent();
		assertThat(mappedRight.getLeft()).isEmpty();
		assertThat(mappedRight.getRight()).isPresent().contains(60);

		var mappedLeft = Either.ofLeft(20).mapRight(it -> "d");
		assertThat(mappedLeft.isLeft()).isTrue();
		assertThat(mappedLeft.getRight()).isEmpty();
		assertThat(mappedLeft.getLeft()).isPresent().contains(20);
	}

	@Test
	void mapBoth() {
		Either<Integer, String> lefty = Either.ofLeft(20).mapBoth(
			i -> i + 40,
			s -> s + "hey"
		);

		assertThat(lefty.getRight()).isEmpty();
		assertThat(lefty.getLeft()).isPresent().contains(60);

		var righty = Either.<Integer, String>ofRight("you ").mapBoth(
			i -> i + 40,
			s -> s + "hey"
		);

		assertThat(righty.getLeft()).isEmpty();
		assertThat(righty.getRight()).isPresent().contains("you hey");
	}
}
