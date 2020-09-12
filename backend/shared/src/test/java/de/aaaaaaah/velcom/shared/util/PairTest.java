package de.aaaaaaah.velcom.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PairTest {

	@Test
	void testContents() {
		Pair<Integer, Boolean> pair = new Pair<>(33, true);
		assertEquals(33, pair.getFirst());
		assertEquals(true, pair.getSecond());

		assertEquals(new Pair<>(33, true), pair);
		assertNotEquals(new Pair<>(33, false), pair);
		assertNotEquals(new Pair<>(34, true), pair);
	}

}
