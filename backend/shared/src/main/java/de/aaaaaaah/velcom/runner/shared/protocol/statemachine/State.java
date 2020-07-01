package de.aaaaaaah.velcom.runner.shared.protocol.statemachine;

/**
 * A state for the {@link StateMachine}.
 */
public interface State {

	/**
	 * This function is called whenever the state is entered. It is also called on the first state
	 * when a {@link StateMachine} is initialized. It exists because large amounts of time may pass
	 * between creation of the state object and the {@link StateMachine} switching to said object.
	 */
	default void onEnter() {
	}

	/**
	 * This function is called whenever the state is exited. It is also called when the {@link
	 * StateMachine} is stopped. It can be used to clean up resources like files or timers.
	 */
	default void onExit() {
	}

	/**
	 * If a state is resting, it can be switched out of at any time using the {@link
	 * StateMachine#switchFromRestingState(State)} function.
	 *
	 * @return whether this state is a resting state
	 */
	default boolean isResting() {
		return false;
	}
}
