package de.aaaaaaah.velcom.runner.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamedRowsTest {

	private NamedRows rows;

	@BeforeEach
	void setUp() {
		rows = new NamedRows();
	}

	@Test
	void alignsRowsByLongestName() {
		rows.add("hello", "world");
		rows.add("goodbye", "and good night");

		String expected = ""
			+ "hello:   world\n"
			+ "goodbye: and good night\n";

		assertThat(rows.format()).isEqualTo(expected);

		rows.add("a semi-long name", "short text");

		expected = ""
			+ "hello:            world\n"
			+ "goodbye:          and good night\n"
			+ "a semi-long name: short text\n";

		assertThat(rows.format()).isEqualTo(expected);
	}

	@Test
	void multipleRowsWithTheSameName() {
		rows.add("hello", "world");
		rows.add("hello", "there");
		rows.add("bye", "world");
		rows.add("hello", "australia");

		String expected = ""
			+ "hello: world\n"
			+ "hello: there\n"
			+ "bye:   world\n"
			+ "hello: australia\n";

		assertThat(rows.format()).isEqualTo(expected);
	}

	@Test
	void escapingStringsAndArrays() {
		rows.addEscaped("String", "foo\bar\n");
		rows.addEscapedArray("Array", new String[]{
			"\"hello\" you there\n\t\n",
			"HEY! \n\""
		});

		String expected = ""
			+ "String: \"foo\\bar\\n\"\n"
			+ "Array:  [\"\\\"hello\\\" you there\\n\\t\\n\", \"HEY! \\n\\\"\"]\n";

		assertThat(rows.format()).isEqualTo(expected);
	}
}
