package de.aaaaaaah.velcom.backend;

import io.dropwizard.Configuration;
import io.dropwizard.server.ServerFactory;
import java.time.Duration;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * The main configuration file for the server.
 */
public class GlobalConfig extends Configuration {

	@NotEmpty
	private String jdbcUrl;

	@NotNull
	private long pollInterval;

	@NotEmpty
	private String webAdminToken;

	@Min(1)
	@Max(65535)
	@NotNull
	private int runnerPort;

	@NotEmpty
	private String benchmarkRepoRemoteUrl;

	@NotEmpty
	private String runnerToken;

	@Min(1)
	private long disconnectedRunnerGracePeriodSeconds;

	private double significantFactor;

	@NotEmpty
	private String repoDir;

	@NotNull
	@Min(1)
	private int hashMemory;

	@NotNull
	@Min(1)
	private int hashIterations;

	public GlobalConfig() {
		RunnerAwareServerFactory.getInstance().setConfig(this);
		RunnerAwareServerFactory.getInstance().setServerFactory(super.getServerFactory());
	}

	/**
	 * @return the JDBC (Java database connectivity) url used to connect to the database
	 */
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	/**
	 * @return the interval between listener updates (in seconds)
	 */
	public long getPollInterval() {
		return pollInterval;
	}

	/**
	 * @return the token used to authorize as a web administrator
	 */
	public String getWebAdminToken() {
		return webAdminToken;
	}

	/**
	 * @return the port that the dispatcher is listening on
	 */
	public int getRunnerPort() {
		return runnerPort;
	}

	/**
	 * @return the remote url where the benchmark repo remote url can be cloned from
	 */
	public String getBenchmarkRepoRemoteUrl() {
		return benchmarkRepoRemoteUrl;
	}

	/**
	 * @return the token runners need to provide as authentication
	 */
	public String getRunnerToken() {
		return runnerToken;
	}

	/**
	 * @return the duration in seconds after which disconnected runners are given up on (removed and
	 * 	commit rescheduled)
	 */
	public long getDisconnectedRunnerGracePeriodSeconds() {
		return disconnectedRunnerGracePeriodSeconds;
	}

	/**
	 * @return the duration after which disconnected runners are given up on (removed and commit
	 * 	rescheduled)
	 */
	public Duration getDisconnectedRunnerGracePeriod() {
		return Duration.ofSeconds(disconnectedRunnerGracePeriodSeconds);
	}

	/**
	 * @return the factor which controls what commit differences become significant. Must be
	 * 	positive
	 */
	public double getSignificantFactor() {
		return significantFactor;
	}

	/**
	 * @return the path to the directory where all local repositories will be placed in
	 */
	public String getRepoDir() {
		return repoDir;
	}

	public int getHashMemory() {
		return hashMemory;
	}

	public int getHashIterations() {
		return hashIterations;
	}

	@Override
	public ServerFactory getServerFactory() {
		return RunnerAwareServerFactory.getInstance();
	}
}
