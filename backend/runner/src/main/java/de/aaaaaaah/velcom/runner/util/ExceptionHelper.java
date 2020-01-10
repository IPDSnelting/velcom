package de.aaaaaaah.velcom.runner.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Helps with common exception operations.
 */
public class ExceptionHelper {

	/**
	 * Returns the stacktrace of a {@link Throwable} as a String.
	 *
	 * @param throwable the throwable
	 * @return the stacktrace
	 */
	public static String getStackTrace(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stringWriter));

		return stringWriter.toString();
	}
}
