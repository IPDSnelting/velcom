package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.RepoSource;
import de.aaaaaaah.velcom.backend.access.entities.TarSource;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonTask {

	private final UUID id;
	private final String author;
	private final int priority;
	private final Instant insertTime;
	private final Instant updateTime;

	@Nullable
	private final String tarName;
	@Nullable
	private final UUID repoId;
	@Nullable
	private final String commitHash;

	public JsonTask(Task task) {
		Objects.requireNonNull(task);
		this.id = task.getId().getId();
		this.author = task.getAuthor();
		this.priority = task.getPriority();
		this.insertTime = task.getInsertTime();
		this.updateTime = task.getUpdateTime();
		this.tarName = task.getSource().getRight().map(TarSource::getTarName).orElse(null);
		this.repoId = task.getSource()
			.getLeft()
			.map(RepoSource::getRepoId)
			.map(RepoId::getId)
			.orElse(null);
		this.commitHash = task.getSource()
			.getLeft()
			.map(RepoSource::getHash)
			.map(CommitHash::getHash)
			.orElse(null);
	}

	public UUID getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public int getPriority() {
		return priority;
	}

	public Instant getInsertTime() {
		return insertTime;
	}

	public Instant getUpdateTime() {
		return updateTime;
	}

	@Nullable
	public String getTarName() {
		return tarName;
	}

	@Nullable
	public UUID getRepoId() {
		return repoId;
	}

	@Nullable
	public String getCommitHash() {
		return commitHash;
	}

}
