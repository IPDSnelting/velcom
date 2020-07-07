package de.aaaaaaah.velcom.shared.protocol;

/**
 * An entity that can be sent over the network between the runner and server.
 */
public interface SentEntity {

	/**
	 * A unique string that identifies this packet type.
	 *
	 * @return a unique name for this packet entity
	 * @apiNote the default implementation is equivalent to {@code getClass().getSimpleName()}
	 */
	default String identifier() {
		return getClass().getSimpleName();
	}

}
