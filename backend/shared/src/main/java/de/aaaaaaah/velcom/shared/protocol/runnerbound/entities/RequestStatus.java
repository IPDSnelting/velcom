package de.aaaaaaah.velcom.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;

/**
 * The server requests the runner status.
 */
public class RequestStatus implements SentEntity {

	private final String dummy;

	@JsonCreator
	public RequestStatus(String dummy) {
		this.dummy = dummy;
	}

	public RequestStatus() {
		this("dummy");
	}

	public String getDummy() {
		return dummy;
	}

}
