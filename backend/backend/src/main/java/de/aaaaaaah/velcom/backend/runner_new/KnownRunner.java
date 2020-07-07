package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.State;
import java.util.Objects;

/**
 * A runner that is known to the dispatcher.
 */
// TODO: Rename me
public class KnownRunner {

	private final String name;
	private final String information;
	private final State state;

	/**
	 * Creates a new known runner.
	 *
	 * @param name the name of the runner
	 * @param information the runner information
	 * @param state the runner state
	 */
	public KnownRunner(String name, String information, State state) {
		this.name = Objects.requireNonNull(name, "name can not be null!");
		this.information = Objects.requireNonNull(information, "information can not be null!");
		this.state = Objects.requireNonNull(state, "state can not be null!");
	}

	public String getName() {
		return name;
	}

	public String getInformation() {
		return information;
	}

	public State getState() {
		return state;
	}
}
