package de.aaaaaaah.velcom.shared.protocol;

import java.util.Arrays;
import java.util.Optional;

/**
 * An enum representing the websocket status codes used in the communication between a backend and a
 * runner.
 */
public enum StatusCode {
	NORMAL_CLOSURE(1000, ""),
	INTERNAL_ERROR(4000, "internal error"),
	ILLEGAL_BEHAVIOUR(4001, "illegal behaviour has occurred"),
	ILLEGAL_PACKET(4002, "invalid packet or packet of illegal type"),
	ILLEGAL_BINARY_PACKET(4003, "binary packet when none was allowed"),
	PING_TIMEOUT(4004, "ping timed out"),
	COMMAND_TIMEOUT(4005, "command reply timed out");

	private final int code;
	private final String description;

	StatusCode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	/**
	 * Try to interpret a websocket status code as {@link StatusCode}.
	 *
	 * @param code the websocket status code
	 * @return the {@link StatusCode}, if successful
	 */
	public static Optional<StatusCode> fromCode(int code) {
		return Arrays.stream(values())
			.filter(statusCode -> statusCode.getCode() == code)
			.findFirst();
	}

	public int getCode() {
		return code;
	}

	/**
	 * A human-readable description of the status code. Don't use this as the websocket close
	 * reason! Instead, use {@link #getDescriptionAsReason()}.
	 *
	 * @return the status code description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * A method for safely using the status code description as websocket close reason.
	 *
	 * @return the description returned by {@link #getDescription()} or an empty string if the
	 * 	description is too long
	 */
	public String getDescriptionAsReason() {
		if (description.getBytes().length > 123) {
			return "";
		} else {
			return description;
		}
	}
}
