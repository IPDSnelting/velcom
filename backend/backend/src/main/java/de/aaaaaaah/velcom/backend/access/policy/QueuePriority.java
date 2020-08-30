package de.aaaaaaah.velcom.backend.access.policy;

/**
 * The priority of a queue task.
 */
public enum QueuePriority {
	NORMAL(10),
	LOW(20),
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
