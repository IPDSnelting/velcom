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
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.DefaultProxyDataFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

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

		SshdSessionFactory factory = new SshdSessionFactory(
			new JGitKeyCache(),
			new DefaultProxyDataFactory()
		);

		SshSessionFactory.setInstance(factory);

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
		LinearLog linearLog = new CommitAccessBasedLinearLog(commitAccess);
		Queue queue = new Queue(commitAccess, new PolicyManualFilo());
		Dispatcher dispatcher = new DispatcherImpl(queue, repoAccess, benchmarkAccess);

		// Dispatcher
		RunnerAwareServerFactory.getInstance().setDispatcher(dispatcher);
		addDummyWorkRepo(repoAccess);

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

	private void addDummyWorkRepo(RepoAccess repoAccess) {
		boolean containsRepo = repoAccess.getAllRepos()
			.stream()
			.anyMatch(it -> it.getName().equals("test-work"));
		if (containsRepo) {
			return;
		}
		repoAccess.addRepo(
			"test-work", new RemoteUrl("https://github.com/I-Al-Istannen/Configurator.git")
		);
		System.out.println("Added test-work repo!");
	}
}
