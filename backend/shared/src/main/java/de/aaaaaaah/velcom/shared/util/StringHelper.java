package de.aaaaaaah.velcom.shared.util;

public class StringHelper {

	public static String escape(String string) {
		String escaped = string
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\t", "\\t")
			.replace("\b", "\\b");

		return "\"" + escaped + "\"";
	}
}
