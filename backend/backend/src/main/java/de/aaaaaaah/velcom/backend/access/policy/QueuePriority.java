package de.aaaaaaah.velcom.backend.access.policy;

/**
 * The priority of a queue task.
 */
public enum QueuePriority {
	LISTENER(2),
	TAR(1),
	MANUAL(0);

	private final int priority;

	QueuePriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Convert an int to a {@link QueuePriority}. Integers that don't correspond to a particular
	 * priority are interpreted as the closest available priority.
	 *
	 * @param priority the int to convert
	 * @return the matching priority
	 */
	public static QueuePriority fromInt(int priority) {
		if (priority <= 0) {
			return MANUAL;
		} else if (priority >= 2) {
			return LISTENER;
		} else {
			return TAR;
		}
	}

	/**
	 * @return the integer priority. Lower is more important
	 */
	public int asInt() {
		return priority;
	}
}
