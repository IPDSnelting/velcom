package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;

/**
 * The types of packets that can be sent to the runner by the backend.
 */
public enum ClientBoundPacketType {
	ABORT_RUN("abort_run", AbortRun.class),
	CLEAR_RESULT("clear_result", ClearResult.class),
	GET_RESULT("get_result", GetResult.class),
	GET_STATUS("get_status", GetStatus.class),
	REQUEST_RUN_REPLY("request_run_reply", RequestRunReply.class);

	private final String type;
	private final Class<?> dataClass;

	ClientBoundPacketType(String type, Class<?> dataClass) {
		this.type = type;
		this.dataClass = dataClass;
	}

	static Optional<ClientBoundPacketType> ofType(String type) {
		for (ClientBoundPacketType value : ClientBoundPacketType.values()) {
			if (value.type.equals(type)) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}

	static Optional<ClientBoundPacketType> ofClass(Class<?> theClass) {
		for (ClientBoundPacketType value : ClientBoundPacketType.values()) {
			if (value.dataClass.equals(theClass)) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}

	@JsonValue
	public String getType() {
		return type;
	}

	public Class<?> getDataClass() {
		return dataClass;
	}
}
