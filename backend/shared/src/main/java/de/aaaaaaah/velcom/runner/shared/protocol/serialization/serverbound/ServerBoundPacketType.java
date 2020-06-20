package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;

/**
 * The types of packets that can be sent to the backend by the runner.
 */
public enum ServerBoundPacketType {
	CLEAR_RESULT_REPLY("clear_result_reply", ClearResultReply.class),
	GET_RESULT_REPLY("get_result_reply", GetResultReply.class),
	GET_STATUS_REPLY("get_status_reply", GetStatusReply.class),
	REQUEST_RUN("request_run", RequestRun.class);

	private final String type;
	private final Class<?> dataClass;

	ServerBoundPacketType(String type, Class<?> dataClass) {
		this.type = type;
		this.dataClass = dataClass;
	}

	static Optional<ServerBoundPacketType> ofType(String type) {
		for (ServerBoundPacketType value : ServerBoundPacketType.values()) {
			if (value.type.equals(type)) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}

	static Optional<ServerBoundPacketType> ofClass(Class<?> theClass) {
		for (ServerBoundPacketType value : ServerBoundPacketType.values()) {
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
