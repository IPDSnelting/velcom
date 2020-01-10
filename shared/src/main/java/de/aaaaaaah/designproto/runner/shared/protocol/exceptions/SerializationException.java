package de.aaaaaaah.designproto.runner.shared.protocol.exceptions;

/**
 * Indicates an error occured when (de)serializing some sent message.
 */
public class SerializationException extends RuntimeException {

	/**
	 * Creates a new serialization exception.
	 *
	 * @param message the detail error message
	 */
	public SerializationException(String message) {
		super(message);
	}

	/**
	 * Creates a new serialization exception.
	 *
	 * @param message the detail error message
	 * @param cause the underlying cause
	 */
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
