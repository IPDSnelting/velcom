package de.aaaaaaah.velcom.shared.util;

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
