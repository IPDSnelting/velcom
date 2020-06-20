package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * A packet that can be sent to the runner by the backend.
 */
public class ClientBoundPacket {

	private final ClientBoundPacketType type;
	private final JsonNode data;

	@JsonCreator
	public ClientBoundPacket(
		@JsonProperty(required = true) ClientBoundPacketType type,
		@JsonProperty(required = true) JsonNode data
	) {
		this.type = type;
		this.data = data;
	}

	public ClientBoundPacketType getType() {
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
		ClientBoundPacket that = (ClientBoundPacket) o;
		return type == that.type &&
			data.equals(that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, data);
	}
}
