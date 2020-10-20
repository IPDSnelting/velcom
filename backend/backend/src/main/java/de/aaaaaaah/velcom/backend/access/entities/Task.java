package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class Task {

	private final TaskId id;
	private final String author;
	private final QueuePriority priority;
	private final Instant insertTime;
	private final Instant updateTime;
	private final Either<CommitSource, TarSource> source;

	public Task(String author, QueuePriority priority, CommitSource source) {
		this(new TaskId(), author, priority, Instant.now(), Instant.now(),
			Either.ofLeft(source));
	}

	public Task(String author, QueuePriority priority, TarSource source) {
		this(new TaskId(), author, priority, Instant.now(), Instant.now(),
			Either.ofRight(source));
	}

	public Task(TaskId id, String author, QueuePriority priority, Instant insertTime,
		Instant updateTime, CommitSource source) {
		this(id, author, priority, insertTime, updateTime, Either.ofLeft(source));
	}

	public Task(TaskId id, String author, QueuePriority priority, Instant insertTime,
		Instant updateTime, TarSource source) {
		this(id, author, priority, insertTime, updateTime, Either.ofRight(source));
	}

	private Task(TaskId id, String author, QueuePriority priority, Instant insertTime,
		Instant updateTime, Either<CommitSource, TarSource> source) {

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

	public QueuePriority getPriority() {
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
		return getSource().consume(
			commitSource -> Optional.of(commitSource.getRepoId()),
			TarSource::getRepoId
		);
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
