package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class Task {

	public static final int DEFAULT_PRIORITY = 2;

	private final TaskId id;
	private final String author;
	private final int priority;
	private final Instant insertTime;
	private final Instant updateTime;
	private final Either<CommitSource, TarSource> source;

	public Task(String author, CommitSource source) {
		this(new TaskId(), author, DEFAULT_PRIORITY, Instant.now(), Instant.now(),
			Either.ofLeft(source));
	}

	public Task(String author, TarSource source) {
		this(new TaskId(), author, DEFAULT_PRIORITY, Instant.now(), Instant.now(),
			Either.ofRight(source));
	}

	public Task(TaskId id, String author, int priority, Instant insertTime, Instant updateTime,
		CommitSource source) {
		this(id, author, priority, insertTime, updateTime, Either.ofLeft(source));
	}

	public Task(TaskId id, String author, int priority, Instant insertTime, Instant updateTime,
		TarSource source) {
		this(id, author, priority, insertTime, updateTime, Either.ofRight(source));
	}

	private Task(TaskId id, String author, int priority, Instant insertTime, Instant updateTime,
		Either<CommitSource, TarSource> source) {
		this.id = Objects.requireNonNull(id);
		this.author = Objects.requireNonNull(author);
		this.priority = priority;
		this.insertTime = Objects.requireNonNull(insertTime);
		this.updateTime = Objects.requireNonNull(updateTime);
		this.source = source;
	}

	public TaskId getId() {
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

	public Either<CommitSource, TarSource> getSource() {
		return source;
	}

	public Optional<RepoId> getRepoId() {
		if (getSource().isLeft()) {
			return Optional.of(getSource().getLeft().get().getRepoId());
		} else {
			return getSource().getRight().get().getRepoId();
		}
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
			'}';
	}

}
