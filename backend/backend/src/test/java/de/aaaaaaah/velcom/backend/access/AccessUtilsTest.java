package de.aaaaaaah.velcom.backend.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import org.junit.jupiter.api.Test;

class AccessUtilsTest {

	@Test
	void readSource() {
		RepoId repoId = new RepoId();
		String repoIdStr = repoId.getIdAsString();
		CommitHash commitHash = new CommitHash("6121c30f8d73ae7f6a19bad8fd6bcb8379cf2287");
		String commitHashStr = commitHash.getHash();
		String tarDesc = "tarDesc";

		assertThatThrownBy(() -> AccessUtils.readSource(null, null, null))
			.isInstanceOf(IllegalArgumentException.class);

		assertThat(AccessUtils.readSource(null, null, tarDesc))
			.isEqualTo(Either.ofRight(new TarSource(tarDesc, null)));

		assertThatThrownBy(() -> AccessUtils.readSource(null, commitHashStr, null))
			.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AccessUtils.readSource(null, commitHashStr, tarDesc))
			.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AccessUtils.readSource(repoIdStr, null, null))
			.isInstanceOf(IllegalArgumentException.class);

		assertThat(AccessUtils.readSource(repoIdStr, null, tarDesc))
			.isEqualTo(Either.ofRight(new TarSource(tarDesc, repoId)));

		assertThat(AccessUtils.readSource(repoIdStr, commitHashStr, null))
			.isEqualTo(Either.ofLeft(new CommitSource(repoId, commitHash)));

		assertThatThrownBy(() -> AccessUtils.readSource(repoIdStr, commitHashStr, tarDesc))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
