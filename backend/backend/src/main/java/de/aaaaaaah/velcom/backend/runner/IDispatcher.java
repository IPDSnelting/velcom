package de.aaaaaaah.velcom.backend.runner;

import java.util.List;

/**
 * The dispatcher interface other modules can compile against.
 */
public interface IDispatcher {

	/**
	 * @return a list with all known runners
	 */
	List<KnownRunner> getKnownRunners();
}
