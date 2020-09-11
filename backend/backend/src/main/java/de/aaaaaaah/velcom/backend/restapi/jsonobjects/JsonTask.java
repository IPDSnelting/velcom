package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import java.util.UUID;

public class JsonTask {

	private final UUID id;
	private final String author;
	private final long since;
	private final JsonSource source;

	public JsonTask(UUID id, String author, long since, JsonSource source) {
		this.id = id;
		this.author = author;
		this.since = since;
		this.source = source;
	}

	public static JsonTask fromTask(Task task, CommitReadAccess commitAccess) {
		JsonSource source = task.getSource().consume(
			commitSource -> getCommitSource(commitAccess, commitSource),
			JsonTask::getTarSource
		);

		return new JsonTask(
			task.getId().getId(), task.getAuthor(), task.getInsertTime().getEpochSecond(), source
		);
	}

	private static JsonSource getTarSource(TarSource tarSource) {
		return JsonSource.fromUploadedTar(tarSource.getDescription(),
			tarSource.getRepoId().map(RepoId::getId).orElse(null));
	}

	private static JsonSource getCommitSource(CommitReadAccess commitAccess, CommitSource it) {
		Commit commit = commitAccess.getCommit(it.getRepoId(), it.getHash());

		return JsonSource.fromCommit(new JsonCommitDescription(
			it.getRepoId().getId(),
			it.getHash().getHash(),
			commit.getAuthor(),
			commit.getAuthorDate().getEpochSecond(),
			commit.getSummary()
		));
	}

	public UUID getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public long getSince() {
		return since;
	}

	public JsonSource getSource() {
		return source;
	}
}
