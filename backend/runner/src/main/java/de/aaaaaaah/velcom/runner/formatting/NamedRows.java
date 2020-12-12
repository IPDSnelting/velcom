package de.aaaaaaah.velcom.runner.formatting;

import de.aaaaaaah.velcom.shared.util.StringHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A way to pretty-print multiple rows of information with names.
 */
public class NamedRows {

	private final List<Row> rows;

	public NamedRows() {
		rows = new ArrayList<>();
	}

	/**
	 * Add a new row at the bottom.
	 *
	 * @param name the row's name
	 * @param value the row's value
	 */
	public void add(String name, String value) {
		rows.add(new Row(name, value));
	}

	/**
	 * Add a string as a new row at the bottom, wrapping it in quotes and escaping weird characters.
	 *
	 * @param name the row's name
	 * @param value the string to quote and escape
	 */
	public void addEscaped(String name, String value) {
		add(name, StringHelper.quote(value));
	}

	/**
	 * Add an array of strings as a new row at the bottom, wrapping the strings in quotes and escaping
	 * weird characters.
	 *
	 * @param name the row's name
	 * @param array the string array to quote and escape
	 */
	public void addEscapedArray(String name, String[] array) {
		String entries = Arrays.stream(array)
			.map(StringHelper::quote)
			.collect(Collectors.joining(", "));

		add(name, "[" + entries + "]");
	}

	/**
	 * Turn the rows into a string, aligning the values vertically.
	 *
	 * @return the rows formatted into a single multiline string
	 */
	public String format() {
		int maxNameLength = rows.stream()
			.map(Row::getName)
			.map(String::length)
			.max(Integer::compareTo)
			.orElse(0);

		return rows.stream()
			.map(row -> row.format(maxNameLength))
			.collect(Collectors.joining());
	}

	private static class Row {

		private final String name;
		private final String value;

		public Row(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public String format(int nameWidth) {
			// nameWidth + 1 to account for colon after name
			return String.format("%-" + (nameWidth + 1) + "s %s\n", (name + ":"), value);
		}
	}
}
