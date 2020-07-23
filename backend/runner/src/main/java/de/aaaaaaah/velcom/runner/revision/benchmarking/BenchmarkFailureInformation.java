package de.aaaaaaah.velcom.runner.revision.benchmarking;

import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Contains information about a benchmark script failure.
 */
public class BenchmarkFailureInformation {

	private final Section generalSection;
	private final List<Section> allSections;

	public BenchmarkFailureInformation() {
		this.generalSection = new Section("General");
		this.allSections = new ArrayList<>();

		allSections.add(generalSection);
	}

	/**
	 * Adds some property to the general section.
	 *
	 * @param name the name of the property
	 * @param value the value of the property
	 */
	public void addToGeneral(String name, String value) {
		this.generalSection.addRow(new Row(name, value));
	}

	/**
	 * Adds the machine info to the general block.
	 *
	 * @param systemInfo the linux system information
	 */
	public void addMachineInfo(LinuxSystemInfo systemInfo) {
		addToGeneral(
			"Machine Info",
			System.getProperty("os.name")
				+ " " + System.getProperty("os.arch")
				+ " " + System.getProperty("os.version")
		);
		addToGeneral(
			"CPU",
			systemInfo.getCpuInfo().toString()
		);
		addToGeneral(
			"Memory",
			systemInfo.getMemoryInfo().toString()
		);
		addToGeneral(
			"Java version",
			System.getProperty("java.version")
				+ " by " + System.getProperty("java.vendor")
		);
	}

	/**
	 * Adds an array property to the general section. The array text is kinda escaped and then
	 * rendered.
	 *
	 * @param name the header name
	 * @param value the value of the property
	 */
	public void addEscapedArrayToGeneral(String name, String[] value) {
		this.generalSection.addRow(new Row(
			name,
			Arrays.stream(value)
				.map(it -> it.replace("\\", "\\\\"))
				.map(it -> it.replace("\"", "\\\""))
				.map(it -> it.replace("\n", "\\n"))
				.map(it -> it.replace("\t", "\\t"))
				.map(it -> it.replace("\b", "\\b"))
				.map(it -> '"' + it + '"')
				.collect(Collectors.joining(", ", "[", "]"))
		));
	}

	/**
	 * Adds a new section with a header.
	 *
	 * @param name the header name
	 * @param content the section content
	 */
	public void addSection(String name, String content) {
		Section section = new Section(name);
		section.addRow(new Row(content));
		this.allSections.add(section);
	}

	@Override
	public String toString() {
		return allSections.stream()
			.filter(it -> !it.data.isEmpty())
			.map(Section::toString)
			.collect(Collectors.joining("\n\n"));
	}

	/**
	 * A single section.
	 */
	private static class Section {

		private final String name;
		private final List<Row> data;

		public Section(String name) {
			this.name = name;
			this.data = new ArrayList<>();
		}

		void addRow(Row row) {
			data.add(row);
		}

		@Override
		public String toString() {
			String header = formatHeader(name) + "\n";
			int maxNameLength = data.stream()
				.map(Row::getName)
				.filter(Objects::nonNull)
				.mapToInt(String::length)
				.max()
				.orElse(0);

			String rowString = data.stream()
				.map(row -> row.toString(maxNameLength))
				.collect(Collectors.joining("\n"));
			return header + rowString;
		}

		private static String formatHeader(String header) {
			String separator = "#".repeat(header.length() + "##  ##".length());

			return separator + "\n## " + header + " ##\n" + separator;
		}

	}

	private static class Row {

		private final String name;
		private final String value;

		public Row(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public Row(String content) {
			this(null, content);
		}

		public String getName() {
			return name;
		}

		public String toString(int width) {
			if (this.name == null) {
				return value;
			}
			return String.format("%-" + width + "s : %s", name, value);
		}
	}
}
