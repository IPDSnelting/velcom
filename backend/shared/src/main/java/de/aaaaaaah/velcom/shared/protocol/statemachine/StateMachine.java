package de.aaaaaaah.velcom.shared.protocol.statemachine;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A {@link StateMachine} keeps track of a state machine's current state in a threadsafe manner.
 *
 * @param <S> type of the state object
 */
public class StateMachine<S extends State> {

	// This lock protects stopped and currentState.
	private final Lock lock;
	private final Condition inRestingStateOrStopped;
	private boolean stopped;
	@Nullable
	private S currentState;

	public StateMachine(S initialState) {
		lock = new ReentrantLock(true);
		inRestingStateOrStopped = lock.newCondition();
		stopped = false;

		switchUnconditionally(initialState);
	}

	/**
	 * Change the state machine's current state in a threadsafe manner.
	 *
	 * @param modifier the function to decide the new state based on the current state. It receives
	 * 	the current state as its argument and its return value will become the new state.
	 * @return true if the state was switched successfully, false otherwise
	 */
	public boolean changeCurrentState(Function<S, S> modifier) {
		lock.lock();
		try {
			if (stopped) {
				return false;
			}

			return switchUnconditionally(modifier.apply(currentState));
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Switch to a new state, no matter what state the state machine is currently in.
	 *
	 * @param newState the state to switch to
	 * @return true if the state was switched successfully, false otherwise
	 */
	private boolean switchUnconditionally(S newState) {
		lock.lock();
		try {
			if (stopped) {
				return false;
			}

			// Intentionally comparing objects via == instead of using .equals()
			if (currentState != newState) {
				if (currentState != null) {
					currentState.onExit();
				}

				currentState = newState;

				if (currentState != null) {
					currentState.onEnter();
				}

				inRestingStateOrStopped.signalAll();
			}

			return true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Switch to a new state as soon as the state machine enters a resting state again.
	 *
	 * @param newState the state to switch to
	 * @return true if the state was switched successfully, false otherwise
	 * @throws InterruptedException if the thread got interrupted while waiting for the state
	 * 	machine to enter a resting state
	 */
	public boolean switchFromRestingState(S newState) throws InterruptedException {
		lock.lock();
		try {
			while (!(stopped || currentState == null || currentState.isResting())) {
				inRestingStateOrStopped.await();
			}

			return switchUnconditionally(newState);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stop the state machine. After this function is called, all currently blocking calls to switch
	 * the state will return and all further attempts at state switching become a noop. Also, the
	 * current state (if there is one) receives its {@link State#onExit()} call.
	 */
	public void stop() {
		lock.lock();
		try {
			// Ensure the current state receives its onExit() call
			switchUnconditionally(null);

			// Ensure that no further state switch will succeed and all switches currently waiting
			// for a resting state return.
			stopped = true;
			inRestingStateOrStopped.signalAll();
		} finally {
			lock.unlock();
		}
	}

}
