package de.aaaaaaah.velcom.runner.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamedSectionsTest {

	private NamedSections sections;

	@BeforeEach
	void setUp() {
		sections = new NamedSections();
	}

	@Test
	void sectionNamesOfDifferentLength() {
		sections.addSection("hello", "world");
		sections.addSection("northrop", "grumman");

		String expected = ""
			+ "###########\n"
			+ "## hello ##\n"
			+ "###########\n"
			+ "world\n"
			+ "\n"
			+ "##############\n"
			+ "## northrop ##\n"
			+ "##############\n"
			+ "grumman\n";

		assertThat(sections.format()).isEqualTo(expected);
	}

	@Test
	void rowModificationsAfterAddingRow() {
		NamedRows rows = new NamedRows();
		rows.add("hello", "world");
		sections.addSection("rows", rows);
		sections.addSection("not rows", "believe me");

		String expected = ""
			+ "##########\n"
			+ "## rows ##\n"
			+ "##########\n"
			+ "hello: world\n"
			+ "\n"
			+ "##############\n"
			+ "## not rows ##\n"
			+ "##############\n"
			+ "believe me\n";

		assertThat(sections.format()).isEqualTo(expected);

		rows.add("keep", "turning");

		expected = ""
			+ "##########\n"
			+ "## rows ##\n"
			+ "##########\n"
			+ "hello: world\n"
			+ "keep:  turning\n"
			+ "\n"
			+ "##############\n"
			+ "## not rows ##\n"
			+ "##############\n"
			+ "believe me\n";

		assertThat(sections.format()).isEqualTo(expected);
	}

	@Test
	void addToFront() {
		sections.addSection("hello", "world");

		String expected = ""
			+ "###########\n"
			+ "## hello ##\n"
			+ "###########\n"
			+ "world\n";

		assertThat(sections.format()).isEqualTo(expected);

		sections.addSectionInFront("front!", "I am priority");
		NamedRows rows = new NamedRows();
		rows.add("hello", "world");
		sections.addSectionInFront("frontier", rows);

		expected = ""
			+ "##############\n"
			+ "## frontier ##\n"
			+ "##############\n"
			+ "hello: world\n"
			+ "\n"
			+ "############\n"
			+ "## front! ##\n"
			+ "############\n"
			+ "I am priority\n"
			+ "\n"
			+ expected;

		assertThat(sections.format()).isEqualTo(expected);
	}

	@Test
	void automaticallyAddNewlineWhenNecessary() {
		sections.addSection("with newline", "see?\n");
		sections.addSection("without newline", "see?");
		sections.addSection("last section",
			"to test if sections are\nalways separated\nby exactly two\nnewlines\n");

		String expected = ""
			+ "##################\n"
			+ "## with newline ##\n"
			+ "##################\n"
			+ "see?\n"
			+ "\n"
			+ "#####################\n"
			+ "## without newline ##\n"
			+ "#####################\n"
			+ "see?\n"
			+ "\n"
			+ "##################\n"
			+ "## last section ##\n"
			+ "##################\n"
			+ "to test if sections are\n"
			+ "always separated\n"
			+ "by exactly two\n"
			+ "newlines\n";

		assertThat(sections.format()).isEqualTo(expected);
	}
}
