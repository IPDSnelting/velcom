package de.aaaaaaah.velcom.shared.protocol.serverbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;

/**
 * The runner finished the benchmark.
 */
public class ReadyForWork implements SentEntity {

	private final String dummy;

	@JsonCreator
	public ReadyForWork(String dummy) {
		this.dummy = dummy;
	}

	public ReadyForWork() {
		this("dummy");
	}

	public String getDummy() {
		return dummy;
	}

}
