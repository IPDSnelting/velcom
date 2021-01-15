package de.aaaaaaah.velcom.backend.access.committaccess.entities;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommitTest {

	private Commit commitWithMessage(String message) {
		return new Commit(
			new RepoId(),
			new CommitHash("8853e6aaf9b6721205d388285fb42bff89b6ec38"),
			true,
			true,
			"author",
			Instant.now(),
			"committer",
			Instant.now(),
			message
		);
	}

	@Test
	void parseMessageCorrectly() {
		// Single- and multiline messages with only a summary

		Commit commit = commitWithMessage("hello");
		assertThat(commit.getMessage()).isEqualTo("hello");
		assertThat(commit.getSummary()).isEqualTo("hello");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.empty());

		commit = commitWithMessage("hello\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.empty());

		commit = commitWithMessage("hello\nworld");
		assertThat(commit.getMessage()).isEqualTo("hello\nworld");
		assertThat(commit.getSummary()).isEqualTo("hello\nworld");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.empty());

		commit = commitWithMessage("hello\nworld\n");
		assertThat(commit.getMessage()).isEqualTo("hello\nworld\n");
		assertThat(commit.getSummary()).isEqualTo("hello\nworld\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.empty());

		// Single- and multiline messages with existing but empty message body

		commit = commitWithMessage("hello\nworld\n\n");
		assertThat(commit.getMessage()).isEqualTo("hello\nworld\n\n");
		assertThat(commit.getSummary()).isEqualTo("hello\nworld\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of(""));

		commit = commitWithMessage("hello\n\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of(""));

		// Messages with summary and body
		// This section is mainly about preserving newlines

		commit = commitWithMessage("hello\n\nworld");
		assertThat(commit.getMessage()).isEqualTo("hello\n\nworld");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("world"));

		commit = commitWithMessage("hello\n\nworld\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\nworld\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("world\n"));

		commit = commitWithMessage("hello\n\nworld\n\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\nworld\n\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("world\n\n"));

		commit = commitWithMessage("hello\n\n\nworld");
		assertThat(commit.getMessage()).isEqualTo("hello\n\n\nworld");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("\nworld"));

		commit = commitWithMessage("hello\n\n\nworld\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\n\nworld\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("\nworld\n"));

		commit = commitWithMessage("hello\n\n\nworld\n\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\n\nworld\n\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary()).isEqualTo(Optional.of("\nworld\n\n"));

		// Multiline summaries and bodies

		commit = commitWithMessage("hello\n\nworld\nmultiline\nmessage");
		assertThat(commit.getMessage()).isEqualTo("hello\n\nworld\nmultiline\nmessage");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary())
			.isEqualTo(Optional.of("world\nmultiline\nmessage"));

		commit = commitWithMessage("hello\n\nworld\nmultiline\nmessage\n");
		assertThat(commit.getMessage()).isEqualTo("hello\n\nworld\nmultiline\nmessage\n");
		assertThat(commit.getSummary()).isEqualTo("hello\n");
		assertThat(commit.getMessageWithoutSummary())
			.isEqualTo(Optional.of("world\nmultiline\nmessage\n"));

		commit = commitWithMessage("hello\nmultiline\nmessage\n\nworld");
		assertThat(commit.getMessage()).isEqualTo("hello\nmultiline\nmessage\n\nworld");
		assertThat(commit.getSummary()).isEqualTo("hello\nmultiline\nmessage\n");
		assertThat(commit.getMessageWithoutSummary())
			.isEqualTo(Optional.of("world"));

		commit = commitWithMessage("hello\nmultiline\nmessage\n\nworld\n");
		assertThat(commit.getMessage()).isEqualTo("hello\nmultiline\nmessage\n\nworld\n");
		assertThat(commit.getSummary()).isEqualTo("hello\nmultiline\nmessage\n");
		assertThat(commit.getMessageWithoutSummary())
			.isEqualTo(Optional.of("world\n"));
	}
}
