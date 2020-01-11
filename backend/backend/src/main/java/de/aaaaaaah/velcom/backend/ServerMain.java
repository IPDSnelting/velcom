package de.aaaaaaah.velcom.backend;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import de.aaaaaaah.velcom.backend.data.linearlog.CommitAccessBasedLinearLog;
import de.aaaaaaah.velcom.backend.data.linearlog.LinearLog;
import de.aaaaaaah.velcom.backend.data.queue.PolicyManualFilo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
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
import io.dropwizard.setup.Environment;

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

		DatabaseStorage databaseStorage = new DatabaseStorage(configuration);
		RepoStorage repoStorage = new RepoStorage();

		AccessLayer accessLayer = new AccessLayer();
		BenchmarkAccess benchmarkAccess = new BenchmarkAccess(accessLayer, databaseStorage);
		CommitAccess commitAccess = new CommitAccess(accessLayer, databaseStorage, repoStorage);
		RepoAccess repoAccess = new RepoAccess(accessLayer, databaseStorage, repoStorage,
			new RemoteUrl(configuration.getBenchmarkRepoRemoteUrl()));
		TokenAccess tokenAccess = new TokenAccess(accessLayer, databaseStorage,
			new AuthToken(configuration.getWebAdminToken()));

		LinearLog linearLog = new CommitAccessBasedLinearLog(commitAccess);
		Queue queue = new Queue(commitAccess, new PolicyManualFilo());
		Dispatcher dispatcher = new DispatcherImpl(queue);

		RunnerAwareServerFactory.getInstance().setDispatcher(dispatcher);

		environment.jersey().register(new AllReposEndpoint(repoAccess));
		environment.jersey()
			.register(new CommitCompareEndpoint(benchmarkAccess, commitAccess, linearLog));
		environment.jersey().register(new CommitHistoryEndpoint(repoAccess, linearLog));
		environment.jersey().register(new MeasurementsEndpoint(benchmarkAccess));
		environment.jersey().register(new QueueEndpoint(commitAccess, queue, dispatcher));
		environment.jersey()
			.register(new RecentlyBenchmarkedCommitsEndpoint(benchmarkAccess, linearLog));
		environment.jersey().register(new RepoComparisonGraphEndpoint());
		environment.jersey().register(new RepoEndpoint(repoAccess, tokenAccess));
		environment.jersey().register(new TestTokenEndpoint(tokenAccess));
	}
}
