package de.aaaaaaah.velcom.runner.revision;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for collecting and formatting runner information before sending it to the server.
 */
public class RunnerInfo {

	private final static int NAME_WIDTH = 12;

	private final Deque<String> names;
	private final Map<String, String> values;

	public RunnerInfo() {
		names = new ArrayDeque<>();
		values = new HashMap<>();
	}

	/**
	 * Create a {@link RunnerInfo} already containing a few bits of system information.
	 *
	 * @return the {@link RunnerInfo}
	 */
	public static RunnerInfo fromSystemInfo() {
		RunnerInfo info = new RunnerInfo();

		info.addInfoBottom("System",
			System.getProperty("os.name")
				+ " " + System.getProperty("os.arch")
				+ " " + System.getProperty("os.version"));

		info.addInfoBottom("Processors",
			String.valueOf(Runtime.getRuntime().availableProcessors()));

		info.addInfoBottom("Memory",
			String.valueOf(Runtime.getRuntime().maxMemory()));

		return info;
	}

	/**
	 * Add a new line at the top. If another line with the same name already exists, it is removed
	 * before this new line is added.
	 *
	 * @param name the label to the left
	 * @param value the text to the right
	 */
	public void addInfoTop(String name, String value) {
		if (!names.contains(name)) {
			names.addFirst(name);
		}
		values.put(name, value);
	}


	/**
	 * Add a new line at the bottom. If another line with the same name already exists, it is
	 * removed before this new line is added.
	 *
	 * @param name the label to the left
	 * @param value the text to the right
	 */
	public void addInfoBottom(String name, String value) {
		if (!names.contains(name)) {
			names.addLast(name);
		}
		values.put(name, value);
	}

	/**
	 * Convert the {@link RunnerInfo} into a string.
	 *
	 * @return the formatted result
	 */
	public String format() {
		StringBuilder builder = new StringBuilder();

		for (String name : names) {
			String value = values.get(name);

			builder.append(name);
			builder.append(": ");

			int delta = NAME_WIDTH - name.length();
			if (delta > 0) {
				builder.append("-".repeat(delta));
			}

			builder.append(value);
			builder.append("\n");
		}

		return builder.toString();
	}

}
