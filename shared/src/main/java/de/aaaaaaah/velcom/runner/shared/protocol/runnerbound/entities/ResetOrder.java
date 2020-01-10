package de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;

/**
 * Orders the client to abort all work and return to the idle stage.
 */
public class ResetOrder implements SentEntity {

	private String reason;

	/**
	 * Creates a new reset order.
	 *
	 * @param reason the reason for the reset
	 */
	@JsonCreator
	public ResetOrder(String reason) {
		this.reason = reason;
	}

	/**
	 * Returns the reason for the abortion.
	 *
	 * @return the reason for the abortion.
	 */
	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return "ResetOrder{" +
			"reason='" + reason + '\'' +
			'}';
	}
}
