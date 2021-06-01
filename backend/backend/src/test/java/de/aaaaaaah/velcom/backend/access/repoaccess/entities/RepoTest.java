package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo.GithubInfo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RepoTest {

	@Test
	void testIllegalConstructorArguments() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new Repo(
			new RepoId(),
			"velcom",
			new RemoteUrl("https://github.com/IPDSnelting/velcom.git"),
			null,
			Instant.now()
		));

		Assertions.assertThrows(IllegalArgumentException.class, () -> new Repo(
			new RepoId(),
			"velcom",
			new RemoteUrl("https://github.com/IPDSnelting/velcom.git"),
			"bla",
			null
		));
	}

	@Test
	void testGhRepoName() {
		Repo repo = new Repo(new RepoId(), "velcom via https",
			new RemoteUrl("https://github.com/IPDSnelting/velcom.git"), null, null);
		assertThat(repo.getGithubRepoName()).isEqualTo(Optional.of("IPDSnelting/velcom"));

		repo = new Repo(new RepoId(), "velcom via ssh",
			new RemoteUrl("git@github.com:IPDSnelting/velcom.git"), null, null);
		assertThat(repo.getGithubRepoName()).isEqualTo(Optional.of("IPDSnelting/velcom"));

		repo = new Repo(new RepoId(), "velcom via gitlab(!?)",
			new RemoteUrl("https://gitlab.com/IPDSnelting/velcom.git"), null, null);
		assertThat(repo.getGithubRepoName()).isEmpty();
	}

	@Test
	void testGhInfo() {
		Instant now = Instant.now();

		Repo repo = new Repo(
			new RepoId(),
			"velcom",
			new RemoteUrl("https://github.com/IPDSnelting/velcom.git"),
			null,
			null
		);
		assertThat(repo.getGithubInfo()).isEmpty();

		repo = new Repo(
			new RepoId(),
			"velcom",
			new RemoteUrl("https://github.com/IPDSnelting/velcom.git"),
			"token",
			now
		);
		assertThat(repo.getGithubInfo())
			.isEqualTo(Optional.of(new GithubInfo("IPDSnelting/velcom", "token", now)));

		repo = new Repo(
			new RepoId(),
			"velcom via https",
			new RemoteUrl("https://gitlab.com/IPDSnelting/velcom.git"),
			"token",
			now
		);
		assertThat(repo.getGithubInfo()).isEmpty();
	}
}
