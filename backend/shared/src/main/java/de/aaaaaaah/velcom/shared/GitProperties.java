package de.aaaaaaah.velcom.shared;

import java.io.IOException;
import java.util.Properties;

/**
 * Provides some build information such as the build time, commit hash or version.
 *
 * <p> When changing this, also update the sections marked with the comment "Current commit hash
 * available to jars" in backend/pom.xml and runner/pom.xml.
 */
public class GitProperties {

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		try {
			PROPERTIES.load(GitProperties.class.getResourceAsStream("/git.properties"));
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize git properties", e);
		}
	}

	public static String getBuildTime() {
		return PROPERTIES.getProperty("git.build.time");
	}

	public static String getVersion() {
		return PROPERTIES.getProperty("git.build.version");
	}

	public static String getHash() {
		return PROPERTIES.getProperty("git.commit.id.full");
	}

	public static String getHashAbbrev() {
		return PROPERTIES.getProperty("git.commit.id.abbrev");
	}
}
