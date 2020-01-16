package de.aaaaaaah.velcom.backend;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparer;
import de.aaaaaaah.velcom.backend.data.linearlog.CommitAccessBasedLinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.queue.PolicyManualFilo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.data.reducedlog.ReducedLog;
import de.aaaaaaah.velcom.backend.data.reducedlog.timeslice.GroupByHour;
import de.aaaaaaah.velcom.backend.data.reducedlog.timeslice.TimeSliceBasedReducedLog;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.RepoAuthenticator;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import de.aaaaaaah.velcom.backend.restapi.endpoints.AllReposEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CommitCompareEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CommitHistoryEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.MeasurementsEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.QueueEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RecentlyBenchmarkedCommitsEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RepoComparisonGraphEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RepoEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.TestTokenEndpoint;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.DispatcherImpl;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.setup.Environment;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlets.CrossOriginFilter;

/**
 * The backend's main class. Contains the core initialisation routines for the web server.
 */
public class ServerMain extends Application<GlobalConfig> {

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
	public void run(GlobalConfig configuration, Environment environment) throws Exception {
		environment.getObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

		configureCors(environment);

		// Storage layer
		DatabaseStorage databaseStorage = new DatabaseStorage(configuration);
		RepoStorage repoStorage = new RepoStorage();

		// Access layer
		AccessLayer accessLayer = new AccessLayer();
		BenchmarkAccess benchmarkAccess = new BenchmarkAccess(accessLayer, databaseStorage);
		CommitAccess commitAccess = new CommitAccess(accessLayer, databaseStorage, repoStorage);
		RepoAccess repoAccess = new RepoAccess(accessLayer, databaseStorage, repoStorage,
			new RemoteUrl(configuration.getBenchmarkRepoRemoteUrl()));
		TokenAccess tokenAccess = new TokenAccess(accessLayer, databaseStorage,
			new AuthToken(configuration.getWebAdminToken()));

		// Data layer
		CommitComparer commitComparer = new CommitComparer(configuration.getSignificantFactor());
		LinearLog linearLog = new CommitAccessBasedLinearLog(commitAccess);
		ReducedLog reducedLog = new TimeSliceBasedReducedLog(benchmarkAccess, new GroupByHour());
		Queue queue = new Queue(commitAccess, new PolicyManualFilo());

		// Listener
		Listener listener = new Listener(configuration, accessLayer, queue);

		// Dispatcher
		Dispatcher dispatcher = new DispatcherImpl(
			queue,
			repoAccess,
			benchmarkAccess,
			configuration.getDisconnectedRunnerGracePeriod()
		);
		RunnerAwareServerFactory.getInstance().setDispatcher(dispatcher);

		// API authentication
		environment.jersey().register(
			new AuthDynamicFeature(
				new BasicCredentialAuthFilter.Builder<RepoUser>()
					.setAuthenticator(new RepoAuthenticator(tokenAccess))
					.buildAuthFilter()
			)
		);
		environment.jersey().register(new AuthValueFactoryProvider.Binder<>(RepoUser.class));

		// API endpoints
		environment.jersey().register(new AllReposEndpoint(repoAccess));
		environment.jersey().register(
			new CommitCompareEndpoint(benchmarkAccess, commitAccess, commitComparer, linearLog));
		environment.jersey().register(new CommitHistoryEndpoint(repoAccess, linearLog));
		environment.jersey().register(new MeasurementsEndpoint(benchmarkAccess));
		environment.jersey().register(new QueueEndpoint(commitAccess, queue, dispatcher));
		environment.jersey().register(
			new RecentlyBenchmarkedCommitsEndpoint(benchmarkAccess, commitComparer, linearLog));
		environment.jersey().register(
			new RepoComparisonGraphEndpoint(commitAccess, repoAccess, reducedLog));
		environment.jersey().register(new RepoEndpoint(repoAccess, tokenAccess, listener));
		environment.jersey().register(new TestTokenEndpoint(tokenAccess));
	}

	private void configureCors(Environment environment) {
		var filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
		filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		filter.setInitParameter(
			CrossOriginFilter.ALLOWED_METHODS_PARAM,
			"GET,PUT,POST,DELETE,OPTIONS"
		);
		filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		filter.setInitParameter(
			"allowedHeaders",
			"Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin"
		);
		filter.setInitParameter("allowCredentials", "true");
	}


}
