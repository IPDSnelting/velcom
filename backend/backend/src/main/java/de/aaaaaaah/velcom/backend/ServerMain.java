package de.aaaaaaah.velcom.backend;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.aaaaaaah.velcom.backend.access.ArchiveAccess;
import de.aaaaaaah.velcom.backend.access.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.access.TokenWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.linearlog.CommitAccessBasedLinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparison;
import de.aaaaaaah.velcom.backend.data.repocomparison.TimesliceComparison;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparer;
import de.aaaaaaah.velcom.backend.data.runcomparison.SignificanceFactors;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoAuthenticator;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.endpoints.AllReposEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CommitEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CompareEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RunEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.TestTokenEndpoint;
import de.aaaaaaah.velcom.backend.restapi.exception.CommitAccessExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchCommitExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchRepoExceptionMapper;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The backend's main class. Contains the core initialisation routines for the web server.
 */
public class ServerMain extends Application<GlobalConfig> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

	private static MetricRegistry metricRegistry;

	/**
	 * @return the global metric registry
	 */
	public static MetricRegistry getMetricRegistry() {
		// Called before `run` was called (in a Test`)
		if (metricRegistry == null) {
			LOGGER.warn("Returning bogus metrics factory!");
			return new MetricRegistry();
		}
		return metricRegistry;
	}

	/**
	 * The backend's main class's main method. Starts the web server.
	 *
	 * @param args the command line arguments
	 * @throws Exception if the web server can not be started
	 */
	public static void main(String[] args) throws Exception {
		new ServerMain().run(args);
	}

	@Override
	public void initialize(Bootstrap<GlobalConfig> bootstrap) {
		bootstrap.addCommand(new HashPerformanceTestCommand());
	}

	@Override
	public void run(GlobalConfig configuration, Environment environment) throws Exception {
		metricRegistry = environment.metrics();

		CollectorRegistry collectorRegistry = new CollectorRegistry();
		collectorRegistry.register(new DropwizardExports(environment.metrics()));
		environment.admin()
			.addServlet("prometheusMetrics", new MetricsServlet(collectorRegistry))
			.addMapping("/prometheusMetrics");

		// Storage layer
		RepoStorage repoStorage = new RepoStorage(configuration.getRepoDir());
		DatabaseStorage databaseStorage = new DatabaseStorage(configuration);

		// Access layer
		TaskWriteAccess taskAccess = new TaskWriteAccess(databaseStorage);
		CommitReadAccess commitAccess = new CommitReadAccess(repoStorage);
		KnownCommitWriteAccess knownCommitAccess = new KnownCommitWriteAccess(
			databaseStorage,
			taskAccess
		);
		RepoWriteAccess repoAccess = new RepoWriteAccess(
			databaseStorage,
			repoStorage
		);
		TokenWriteAccess tokenAccess = new TokenWriteAccess(
			databaseStorage,
			new AuthToken(configuration.getWebAdminToken()),
			configuration.getHashMemory(),
			configuration.getHashIterations()
		);
		ArchiveAccess archiveAccess = new ArchiveAccess(
			Path.of(configuration.getArchivesRootDir()),
			new RemoteUrl(configuration.getBenchmarkRepoRemoteUrl()),
			repoStorage
		);
		BenchmarkWriteAccess benchmarkAccess = new BenchmarkWriteAccess(
			databaseStorage, repoAccess, taskAccess
		);

		// Data layer
		CommitComparer commitComparer = new CommitComparer(configuration.getSignificantFactor());
		LinearLog linearLog = new CommitAccessBasedLinearLog(commitAccess, repoAccess);
		RepoComparison repoComparison = new TimesliceComparison(commitAccess, benchmarkAccess);
		Queue queue = new Queue(repoAccess, taskAccess, archiveAccess, benchmarkAccess);
		BenchRepo benchRepo = new BenchRepo(archiveAccess);
		// TODO Read significance factors from config file
		RunComparer comparer = new RunComparer(new SignificanceFactors(0.01, 2.0, 25));

		// Listener
		Listener listener = new Listener(configuration, repoAccess, commitAccess, knownCommitAccess,
			benchRepo);

		// Dispatcher
		Dispatcher dispatcher = new Dispatcher(
			queue,
			Duration.ofSeconds(configuration.getDisconnectedRunnerGracePeriodSeconds())
		);
		RunnerAwareServerFactory.getInstance().setDispatcher(dispatcher);
		RunnerAwareServerFactory.getInstance().setBenchRepo(benchRepo);

		// API
		configureApi(environment, tokenAccess);
		configureCors(environment);

		// Endpoints
		environment.jersey().register(new AllReposEndpoint(repoAccess, benchmarkAccess, tokenAccess));
		environment.jersey().register(new CommitEndpoint(commitAccess, benchmarkAccess));
		environment.jersey().register(new CompareEndpoint(benchmarkAccess));
		environment.jersey().register(new RunEndpoint(benchmarkAccess, commitAccess, comparer));
		environment.jersey().register(new TestTokenEndpoint());
	}

	private void configureApi(Environment environment, TokenWriteAccess tokenAccess) {
		// Serialization
		// This mapper should be configured the same as the one in SerializingTest.java
		environment.getObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.setSerializationInclusion(Include.NON_NULL);

		// Exceptions
		environment.jersey().register(new NoSuchRepoExceptionMapper());
		environment.jersey().register(new NoSuchCommitExceptionMapper());
		environment.jersey().register(new CommitAccessExceptionMapper());

		// Authentication
		environment.jersey().register(
			new AuthDynamicFeature(
				new BasicCredentialAuthFilter.Builder<RepoUser>()
					.setAuthenticator(new RepoAuthenticator(tokenAccess))
					.buildAuthFilter()
			)
		);
		environment.jersey().register(new AuthValueFactoryProvider.Binder<>(RepoUser.class));
	}

	private void configureCors(Environment environment) {
		var filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
		filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		filter.setInitParameter(
			CrossOriginFilter.ALLOWED_METHODS_PARAM,
			"GET,PUT,POST,DELETE,OPTIONS,PATCH"
		);
		filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		filter.setInitParameter(
			"allowedHeaders",
			"Content-Type,Authorization,X-Requested-With,Content-Length,Accept-Encoding,Origin"
		);
		filter.setInitParameter("allowCredentials", "true");
	}


}
