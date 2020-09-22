package de.aaaaaaah.velcom.runner.formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NamedRows {

	private final List<Row> rows;

	public NamedRows() {
		rows = new ArrayList<>();
	}

	public static String escape(String string) {
		String escaped = string
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\t", "\\t")
			.replace("\b", "\\b");

		return "\"" + escaped + "\"";
	}

	public void add(String name, String value) {
		rows.add(new Row(name, value));
	}

	public void addEscaped(String name, String value) {
		add(name, escape(value));
	}

	public void addEscapedArray(String name, String[] array) {
		String entries = Arrays.stream(array)
			.map(NamedRows::escape)
			.collect(Collectors.joining(", "));

		add(name, "[" + entries + "]");
	}

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
