package de.aaaaaaah.velcom.shared.util;

/**
 * Check whether this program is running on a lame OS.
 */
public final class OSCheck {

	private OSCheck() {
		throw new UnsupportedOperationException("No instantiation");
	}

	public static boolean isStupidWindows() {
		return System.getProperty("os.name", "generic")
			.toLowerCase()
			.contains("windows");
	}

}
