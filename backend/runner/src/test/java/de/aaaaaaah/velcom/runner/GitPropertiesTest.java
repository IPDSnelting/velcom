package de.aaaaaaah.velcom.runner;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.shared.GitProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class GitPropertiesTest {

	@Test
	void fieldsPresent() {
		Assertions.assertThat(GitProperties.getHash()).isNotBlank();
		assertThat(GitProperties.getHashAbbrev()).isNotBlank();
		assertThat(GitProperties.getBuildTime()).isNotBlank();
		assertThat(GitProperties.getVersion()).isNotBlank();
	}

	@Test
	void hasAbbreviationIsAbbreviation() {
		assertThat(GitProperties.getHash()).startsWith(GitProperties.getHashAbbrev());
	}
}
