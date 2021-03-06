package de.aaaaaaah.velcom.runner.formatting;

import de.aaaaaaah.velcom.shared.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A way to pretty-print multiple named sections, each consisting of {@link NamedRows} or a single
 * {@link String}.
 */
public class NamedSections {

	private final List<Section> sections;

	public NamedSections() {
		sections = new ArrayList<>();
	}


	/**
	 * Add a string as a new section after all other sections.
	 *
	 * @param name the name of the section
	 * @param content the content of the section
	 */
	public void addSection(String name, String content) {
		sections.add(new Section(name, Either.ofLeft(content)));
	}

	/**
	 * Add a {@link NamedRows} as a new section after all other sections. Any further changes to the
	 * {@link NamedRows} object will be reflected in the section contents.
	 *
	 * @param name the name of the section
	 * @param rows the content of the section
	 */
	public void addSection(String name, NamedRows rows) {
		sections.add(new Section(name, Either.ofRight(rows)));
	}

	/**
	 * Add a string as a new section in front of all all other sections.
	 *
	 * @param name the name of the section
	 * @param content the content of the section
	 */
	public void addSectionInFront(String name, String content) {
		sections.add(0, new Section(name, Either.ofLeft(content)));
	}

	/**
	 * Add a {@link NamedRows} as a new section in front of all all other sections. Any further
	 * changes to the {@link NamedRows} object will be reflected in the section contents.
	 *
	 * @param name the name of the section
	 * @param rows the content of the section
	 */
	public void addSectionInFront(String name, NamedRows rows) {
		sections.add(0, new Section(name, Either.ofRight(rows)));
	}

	/**
	 * Turn the sections into a string. Only at this point are any {@link NamedRows} formatted.
	 *
	 * @return the sections formatted into a single multiline string
	 */
	public String format() {
		return sections.stream()
			.map(Section::format)
			.collect(Collectors.joining("\n"));
	}

	private static class Section {

		private final String name;
		private final Either<String, NamedRows> content;

		public Section(String name, Either<String, NamedRows> content) {
			this.name = name;
			this.content = content;
		}

		public String getName() {
			return name;
		}

		public Either<String, NamedRows> getContent() {
			return content;
		}

		public String format() {
			StringBuilder builder = new StringBuilder();

			builder.append("#".repeat(name.length() + 6)).append("\n");
			builder.append("## ").append(name).append(" ##\n");
			builder.append("#".repeat(name.length() + 6)).append("\n");

			String contentString = content.consume(s -> s, NamedRows::format);
			builder.append(contentString);
			if (!contentString.endsWith("\n")) {
				builder.append("\n");
			}

			return builder.toString();
		}
	}
}
