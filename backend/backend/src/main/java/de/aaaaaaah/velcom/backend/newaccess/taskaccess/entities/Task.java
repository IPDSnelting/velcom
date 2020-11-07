package de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Task {

	private final TaskId id;
	private final String author;
	private final TaskPriority priority;
	// TODO: 07.11.20 Get rid of one of these two times
	private final Instant insertTime;
	private final Instant updateTime;
	private final Either<CommitSource, TarSource> source;
	private final boolean inProcess; // TODO: 07.11.20 Rename to "inProgress"?

	public Task(TaskId id, String author, TaskPriority priority, Instant insertTime,
		Instant updateTime, Either<CommitSource, TarSource> source, boolean inProcess) {

		this.id = id;
		this.author = author;
		this.priority = priority;
		this.insertTime = insertTime;
		this.updateTime = updateTime;
		this.source = source;
		this.inProcess = inProcess;
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

	public boolean isInProcess() {
		return inProcess;
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
			", inProcess=" + inProcess +
			'}';
	}
}
