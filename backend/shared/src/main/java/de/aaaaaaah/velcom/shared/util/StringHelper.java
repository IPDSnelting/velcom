package de.aaaaaaah.velcom.shared.util;

/**
 * A utility class for manipulating strings.
 */
public class StringHelper {

	/**
	 * Wrap a string in quotation marks. Escape quotation marks and backslashes in the string to make
	 * the quotation unambiguous. Also escape some whitespace.
	 *
	 * @param string the string to wrap in quotationMarks
	 * @return the quoted (and escaped) string
	 */
	public static String quote(String string) {
		String escaped = string
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\t", "\\t")
			.replace("\b", "\\b");

		return "\"" + escaped + "\"";
	}
}
