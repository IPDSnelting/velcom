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
	 * @return the integer priority. Lower is more important
	 */
	public int getAsInt() {
		return priority;
	}
}
