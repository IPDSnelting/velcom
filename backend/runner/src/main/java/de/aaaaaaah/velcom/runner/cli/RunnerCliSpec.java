package de.aaaaaaah.velcom.runner.cli;

import java.net.URI;
import net.jbock.Command;
import net.jbock.Param;

/**
 * The runner for VelCom that executes benchmarks.
 *
 * Made by Aaaaaaah!
 */
@Command(value = "VelCom-Runner")
@SuppressWarnings("CheckStyle")
public abstract class RunnerCliSpec {

	/**
	 * The name of the runner
	 */
	@Param(value = 1)
	public abstract String runnerName();

	/**
	 * The access token to use
	 */
	@Param(value = 2)
	public abstract String accessToken();

	/**
	 * The URL of the server to connect to
	 */
	@Param(value = 3)
	public abstract URI serverUrl();
}
