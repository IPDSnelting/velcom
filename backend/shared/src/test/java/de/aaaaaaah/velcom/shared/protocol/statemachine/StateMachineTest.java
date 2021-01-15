package de.aaaaaaah.velcom.shared.protocol.statemachine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateMachineTest {

	static class Resting implements State {

		@Override
		public boolean isResting() {
			return true;
		}
	}

	static class Unrest implements State {

	}

	static class RandomUnrest implements State {

	}

	private StateMachine<State> stateMachine;

	@BeforeEach
	void setUp() {
		stateMachine = new StateMachine<>(new Resting());
	}

	@Test
	void canChangeFromResting() throws InterruptedException {
		assertThat(stateMachine.switchFromRestingState(new Unrest())).isTrue();
	}

	@Test
	void canNotChangeOutOfUnrest() throws InterruptedException {
		stateMachine.switchFromRestingState(new Unrest());

		new Thread(() -> {
			try {
				Thread.sleep(1000);
				stateMachine.changeCurrentState(state -> {
					assertThat(state).isInstanceOf(Unrest.class);
					return new Resting();
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();

		assertThat(stateMachine.switchFromRestingState(new Unrest())).isTrue();
	}

	@Test
	void changingFromStopFails() throws InterruptedException {
		stateMachine.stop();
		assertThat(stateMachine.switchFromRestingState(new Unrest())).isFalse();
	}

	@Test
	void changesState() throws InterruptedException {
		Unrest unrest = new Unrest();
		RandomUnrest randomUnrest = new RandomUnrest();
		Resting resting = new Resting();

		assertThat(stateMachine.switchFromRestingState(unrest)).isTrue();

		stateMachine.changeCurrentState(old -> {
			assertThat(old).isSameAs(unrest);
			return randomUnrest;
		});

		stateMachine.changeCurrentState(old -> {
			assertThat(old).isSameAs(randomUnrest);
			return resting;
		});

		stateMachine.changeCurrentState(old -> {
			assertThat(old).isSameAs(resting);
			return resting;
		});
	}

	@Test
	void stopCancelsWait() throws InterruptedException {
		stateMachine.switchFromRestingState(new Unrest());

		Semaphore waiting = new Semaphore(0);

		new Thread(() -> {
			try {
				waiting.acquire();
				Thread.sleep(500);
				stateMachine.stop();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();

		waiting.release();
		// The switch will release then the thread stops the state machine
		assertThat(stateMachine.switchFromRestingState(new RandomUnrest())).isFalse();
	}
}
