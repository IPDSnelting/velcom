package de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities;

/**
 * The priority a task can have in the queue. Lower means more important.
 */
public enum TaskPriority {
	/**
	 * The task has been manually prioritized. In the case of commits, manually adding them to the
	 * queue automatically prioritizes them.
	 */
	MANUAL(0),
	/**
	 * The task represents an uploaded tar file.
	 */
	TAR(1),
	/**
	 * The task represents a commit that has been automatically added to the queue by the listener.
	 */
	LISTENER(2);

	private final int priority;

	TaskPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Convert an int to a {@link TaskPriority}. Integers that don't correspond to a particular
	 * priority are interpreted as the closest available priority.
	 *
	 * @param priority the int to convert
	 * @return the matching priority
	 */
	public static TaskPriority fromInt(int priority) {
		if (priority <= 0) {
			return MANUAL;
		} else if (priority == 1) {
			return TAR;
		} else {
			return LISTENER;
		}
	}

	/**
	 * @return the priority as an integer
	 */
	public int asInt() {
		return priority;
	}

}
