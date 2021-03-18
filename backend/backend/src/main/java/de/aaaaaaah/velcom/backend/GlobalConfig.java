package de.aaaaaaah.velcom.backend;

import io.dropwizard.Configuration;
import io.dropwizard.server.ServerFactory;
import java.nio.file.Path;
import java.time.Duration;
import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 * The main configuration file for the server.
 */
public class GlobalConfig extends Configuration {

	/////////////
	// General //
	/////////////

	@NotEmpty
	private String webAdminToken;
	@NotEmpty
	private String benchmarkRepoRemoteUrl;
	private long pollInterval = 120;
	private long vacuumInterval = 25 * 60 * 60;

	/////////////////
	// Directories //
	/////////////////

	private Path dataDir = Path.of("data");
	private Path cacheDir = Path.of("cache");
	private Path tmpDir = Path.of("tmp");

	////////////
	// Runner //
	////////////

	@NotEmpty
	private String runnerToken;
	@Min(1)
	@Max(65535)
	private int runnerPort = 3546;
	@Min(1)
	private long disconnectedRunnerGracePeriodSeconds = 600;

	/////////////////////////
	// Significant commits //
	/////////////////////////

	private double significanceRelativeThreshold = 0.05;
	private double significanceStddevThreshold = 2;
	@Min(2)
	private int significanceMinStddevAmount = 25;

	public GlobalConfig() {
		RunnerAwareServerFactory.getInstance().setConfig(this);
	}

	@Override
	public ServerFactory getServerFactory() {
		if (RunnerAwareServerFactory.getInstance().lacksFactory()) {
			RunnerAwareServerFactory.getInstance().setServerFactory(super.getServerFactory());
		}
		return RunnerAwareServerFactory.getInstance();
	}

	/////////////
	// General //
	/////////////

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
	 * @return the remote url where the benchmark repo remote url can be cloned from
	 */
	public String getBenchmarkRepoRemoteUrl() {
		return benchmarkRepoRemoteUrl;
	}

	public void setBenchmarkRepoRemoteUrl(String benchmarkRepoRemoteUrl) {
		this.benchmarkRepoRemoteUrl = benchmarkRepoRemoteUrl;
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

	public Duration getVacuumInterval() {
		return Duration.ofSeconds(vacuumInterval);
	}

	public void setVacuumInterval(long vacuumInterval) {
		this.vacuumInterval = vacuumInterval;
	}

	/////////////////
	// Directories //
	/////////////////

	@Nullable
	public Path getDataDir() {
		return dataDir;
	}

	public void setDataDir(@Nullable Path dataDir) {
		this.dataDir = dataDir;
	}

	@Nullable
	public Path getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(@Nullable Path cacheDir) {
		this.cacheDir = cacheDir;
	}

	@Nullable
	public Path getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(@Nullable Path tmpDir) {
		this.tmpDir = tmpDir;
	}

	////////////
	// Runner //
	////////////

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
	 * @return the port that the dispatcher is listening on
	 */
	public int getRunnerPort() {
		return runnerPort;
	}

	public void setRunnerPort(int runnerPort) {
		this.runnerPort = runnerPort;
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

	/////////////////////////
	// Significant commits //
	/////////////////////////

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
}
