package de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.util.Objects;

/**
 * Indicates that the runner has received the work binaries and the server can go on freeing them.
 */
public class WorkReceived implements SentEntity {

	private RunnerWorkOrder workOrder;

	/**
	 * Creates a new {@link WorkReceived} instance.
	 *
	 * @param workOrder the work order whose file was received
	 */
	@JsonCreator
	public WorkReceived(RunnerWorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	/**
	 * Returns the work order whose file was reeived.
	 *
	 * @return the work order whose file was reeived
	 */
	public RunnerWorkOrder getWorkOrder() {
		return workOrder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WorkReceived that = (WorkReceived) o;
		return Objects.equals(workOrder, that.workOrder);
	}

	@Override
	public int hashCode() {
		return Objects.hash(workOrder);
	}

	@Override
	public String toString() {
		return "WorkReceived{" +
			"workOrder=" + workOrder +
			'}';
	}
}
