package de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A task is an item in the queue. When it is benchmarked by a runner, it turns into a run.
 */
public class Task {

	private final TaskId id;
	private final String author;
	private final TaskPriority priority;
	// TODO: 07.11.20 Get rid of one of these two times
	private final Instant insertTime;
	private final Instant updateTime;
	private final Either<CommitSource, TarSource> source;
	private final boolean inProgress;
	// TODO: 07.11.20 Rename "in_process" to "in_progress" in db

	public Task(TaskId id, String author, TaskPriority priority, Instant insertTime,
		Instant updateTime, Either<CommitSource, TarSource> source, boolean inProgress) {

		this.id = id;
		this.author = author;
		this.priority = priority;
		this.insertTime = insertTime;
		this.updateTime = updateTime;
		this.source = source;
		this.inProgress = inProgress;
	}

	public Task(String author, TaskPriority priority, Either<CommitSource, TarSource> source) {
		this.id = new TaskId();
		this.author = author;
		this.priority = priority;
		this.insertTime = Instant.now();
		this.updateTime = this.insertTime;
		this.source = source;
		this.inProgress = false;
	}

	public TaskId getId() {
		return id;
	}

	public UUID getIdAsUuid() {
		return id.getId();
	}

	public String getIdAsString() {
		return id.getIdAsString();
	}

	public String getAuthor() {
		return author;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public Instant getInsertTime() {
		return insertTime;
	}

	public Instant getUpdateTime() {
		return updateTime;
	}

	public Either<CommitSource, TarSource> getSource() {
		return source;
	}

	public Optional<RepoId> getRepoId() {
		return source.consume(it -> Optional.of(it.getRepoId()), TarSource::getRepoId);
	}

	public Optional<CommitHash> getCommitHash() {
		return source.consume(it -> Optional.of(it.getHash()), it -> Optional.empty());
	}

	public Optional<String> getTarDescription() {
		return source.consume(it -> Optional.empty(), it -> Optional.of(it.getDescription()));
	}

	public boolean isInProgress() {
		return inProgress;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Task task = (Task) o;
		return Objects.equals(id, task.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Task{" +
			"id=" + id +
			", author='" + author + '\'' +
			", priority=" + priority +
			", insertTime=" + insertTime +
			", updateTime=" + updateTime +
			", source=" + source +
			", inProgress=" + inProgress +
			'}';
	}
}
