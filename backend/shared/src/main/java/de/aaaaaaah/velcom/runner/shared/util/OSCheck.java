package de.aaaaaaah.velcom.runner.shared.util;

public class OSCheck {

	public static boolean isStupidWindows() {
		return System.getProperty("os.name", "generic")
			.toLowerCase()
			.contains("windows");
	}

}
