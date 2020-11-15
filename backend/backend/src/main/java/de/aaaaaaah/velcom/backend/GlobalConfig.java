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

	private double significanceRelativeThreshold;
	private double significanceStddevThreshold;
	@Min(2)
	private int significanceMinStddevAmount;

	@NotNull
	@Min(1)
	private int hashMemory;

	@NotNull
	@Min(1)
	private int hashIterations;

	public GlobalConfig() {
		RunnerAwareServerFactory.getInstance().setConfig(this);
	}

	/**
	 * @return the interval between listener updates
	 */
	public Duration getPollInterval() {
		return Duration.ofSeconds(pollInterval);
	}

	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	/**
	 * @return the token used to authorize as a web administrator
	 */
	public String getWebAdminToken() {
		return webAdminToken;
	}

	public void setWebAdminToken(String webAdminToken) {
		this.webAdminToken = webAdminToken;
	}

	/**
	 * @return the port that the dispatcher is listening on
	 */
	public int getRunnerPort() {
		return runnerPort;
	}

	public void setRunnerPort(int runnerPort) {
		this.runnerPort = runnerPort;
	}

	/**
	 * @return the remote url where the benchmark repo remote url can be cloned from
	 */
	public String getBenchmarkRepoRemoteUrl() {
		return benchmarkRepoRemoteUrl;
	}

	public void setBenchmarkRepoRemoteUrl(String benchmarkRepoRemoteUrl) {
		this.benchmarkRepoRemoteUrl = benchmarkRepoRemoteUrl;
	}

	/**
	 * @return the token runners need to provide as authentication
	 */
	public String getRunnerToken() {
		return runnerToken;
	}

	public void setRunnerToken(String runnerToken) {
		this.runnerToken = runnerToken;
	}

	/**
	 * @return the duration after which disconnected runners are given up on (removed and commit
	 * 	rescheduled)
	 */
	public Duration getDisconnectedRunnerGracePeriod() {
		return Duration.ofSeconds(disconnectedRunnerGracePeriodSeconds);
	}

	public void setDisconnectedRunnerGracePeriodSeconds(long disconnectedRunnerGracePeriodSeconds) {
		this.disconnectedRunnerGracePeriodSeconds = disconnectedRunnerGracePeriodSeconds;
	}

	/**
	 * Explained in more detail in the example config.
	 *
	 * @return the relative threshold
	 */
	public double getSignificanceRelativeThreshold() {
		return significanceRelativeThreshold;
	}

	public void setSignificanceRelativeThreshold(double significanceRelativeThreshold) {
		this.significanceRelativeThreshold = significanceRelativeThreshold;
	}

	/**
	 * Explained in more detail in the example config.
	 *
	 * @return the stddev threshold
	 */
	public double getSignificanceStddevThreshold() {
		return significanceStddevThreshold;
	}

	public void setSignificanceStddevThreshold(double significanceStddevThreshold) {
		this.significanceStddevThreshold = significanceStddevThreshold;
	}

	/**
	 * Explained in more detail in the example config.
	 *
	 * @return the minimum amount of values for stddev calculations to apply
	 */
	public int getSignificanceMinStddevAmount() {
		return significanceMinStddevAmount;
	}

	public void setSignificanceMinStddevAmount(int significanceMinStddevAmount) {
		this.significanceMinStddevAmount = significanceMinStddevAmount;
	}

	public int getHashMemory() {
		return hashMemory;
	}

	public void setHashMemory(int hashMemory) {
		this.hashMemory = hashMemory;
	}

	public int getHashIterations() {
		return hashIterations;
	}

	public void setHashIterations(int hashIterations) {
		this.hashIterations = hashIterations;
	}

	@Override
	public ServerFactory getServerFactory() {
		if (RunnerAwareServerFactory.getInstance().lacksFactory()) {
			RunnerAwareServerFactory.getInstance().setServerFactory(super.getServerFactory());
		}
		return RunnerAwareServerFactory.getInstance();
	}
}
