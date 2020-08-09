package de.aaaaaaah.velcom.shared.protocol;

/**
 * A collection of shared header names needed during the runner-backend connection.
 */
public enum RunnerConnectionHeader {
	CONNECT_RUNNER_TOKEN("Runner-Token"),
	CONNECT_RUNNER_NAME("Runner-Name"),
	DISCONNECT_DENY_REASON("Runner-Deny");

	private final String headerName;

	RunnerConnectionHeader(String headerName) {
		this.headerName = headerName;
	}

	/**
	 * @return the name of the header
	 */
	public String getName() {
		return headerName;
	}
}
