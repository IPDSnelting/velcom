package de.aaaaaaah.velcom.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;

/**
 * The server requests the results from the runner.
 */
public class RequestResults implements SentEntity {

	private final String dummy;

	@JsonCreator
	public RequestResults(String dummy) {
		this.dummy = dummy;
	}

	public RequestResults() {
		this("dummy");
	}

	public String getDummy() {
		return dummy;
	}
}
