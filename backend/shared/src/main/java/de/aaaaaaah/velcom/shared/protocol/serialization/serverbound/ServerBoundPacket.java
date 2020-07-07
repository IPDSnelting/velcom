package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * A packet that can be sent to the backend by the runner.
 */
public class ServerBoundPacket {

	private final ServerBoundPacketType type;
	private final JsonNode data;

	@JsonCreator
	public ServerBoundPacket(
		@JsonProperty(required = true) ServerBoundPacketType type,
		@JsonProperty(required = true) JsonNode data
	) {
		this.type = type;
		this.data = data;
	}

	public ServerBoundPacketType getType() {
		return type;
	}

	public JsonNode getData() {
		return data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServerBoundPacket that = (ServerBoundPacket) o;
		return type == that.type &&
			data.equals(that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, data);
	}
}
