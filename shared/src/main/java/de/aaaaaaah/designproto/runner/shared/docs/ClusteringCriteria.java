package de.aaaaaaah.designproto.runner.shared.docs;

/**
 * Allows clustering parts of the application into layers or bigger/smaller components.
 */
public class ClusteringCriteria {

	/**
	 * A criterion that marks all elements that are related to the benchamrk runner.
	 */
	public static final String RUNNER = "runner";

	/**
	 * All backend runner components.
	 */
	public static final String RUNNER_BACKEND = "runner-backend";

	/**
	 * All shared runner components.
	 */
	public static final String RUNNER_SHARED = "runner-shared";

	/**
	 * All runner runner components.
	 */
	public static final String RUNNER_RUNNER = "runner-runner";
}
