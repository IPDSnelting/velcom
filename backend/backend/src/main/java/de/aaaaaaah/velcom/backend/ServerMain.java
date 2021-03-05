package de.aaaaaaah.velcom.backend;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.aaaaaaah.velcom.backend.access.archiveaccess.ArchiveReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.taskaccess.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.data.recentruns.SignificantRunsCollector;
import de.aaaaaaah.velcom.backend.data.repocomparison.TimesliceComparison;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.data.runcomparison.SignificanceFactors;
import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import de.aaaaaaah.velcom.backend.restapi.authentication.AdminAuthenticator;
import de.aaaaaaah.velcom.backend.restapi.endpoints.AllReposEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CommitEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.CompareEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.DebugEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.GraphComparisonEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.GraphDetailEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.ListenerEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.QueueEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RecentRunsEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RepoEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.RunEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.SearchEndpoint;
import de.aaaaaaah.velcom.backend.restapi.endpoints.TestTokenEndpoint;
import de.aaaaaaah.velcom.backend.restapi.exception.ArgumentParseExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchCommitExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchDimensionExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchRepoExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchRunExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.NoSuchTaskExceptionMapper;
import de.aaaaaaah.velcom.backend.restapi.exception.TaskAlreadyExistsExceptionMapper;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.storage.ManagedDirs;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.tar.TarFileStorage;
import de.aaaaaaah.velcom.shared.GitProperties;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.IOException;
import java.util.EnumSet;
import java.util.stream.Stream;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The backend's main class. Contains the core initialisation routines for the web server.
 */
public class ServerMain extends Application<GlobalConfig> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

	/**
	 * The backend's main class's main method. Starts the web server.
	 *
	 * @param args the command line arguments
	 * @throws Exception if the web server can not be started
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Welcome to VelCom!");
		System.out.printf("Version:     %s (backend)%n", GitProperties.getVersion());
		System.out.printf("Build time:  %s%n", GitProperties.getBuildTime());
		System.out.printf("Commit hash: %s%n", GitProperties.getHash());
		System.out.println();

		new ServerMain().run(args);
	}

	@Override
	public void initialize(Bootstrap<GlobalConfig> bootstrap) {
		bootstrap.addBundle(new MultiPartBundle());
	}

	@Override
	public void run(GlobalConfig configuration, Environment environment) throws Exception {
		configureMetrics(environment);

		// Storage layer
		ManagedDirs managedDirs = new ManagedDirs(
			configuration.getDataDir(),
			configuration.getCacheDir(),
			configuration.getTmpDir()
		);
		managedDirs.createAndCleanDirs();

		DatabaseStorage databaseStorage = new DatabaseStorage(managedDirs.getJdbcUrl());
		RepoStorage repoStorage = new RepoStorage(managedDirs.getReposDir());
		TarFileStorage tarFileStorage = new TarFileStorage(managedDirs.getTarsDir());

		// Caches
		AvailableDimensionsCache availableDimensionsCache = new AvailableDimensionsCache();
		LatestRunCache latestRunCache = new LatestRunCache();
		RunCache runCache = new RunCache();

		// Access layer
		TaskWriteAccess taskAccess = new TaskWriteAccess(databaseStorage, tarFileStorage);
		CommitReadAccess commitAccess = new CommitReadAccess(databaseStorage);
		DimensionReadAccess dimensionAccess = new DimensionReadAccess(databaseStorage);
		RepoWriteAccess repoAccess = new RepoWriteAccess(databaseStorage, availableDimensionsCache,
			runCache, latestRunCache);
		ArchiveReadAccess archiveAccess = new ArchiveReadAccess(
			managedDirs.getArchivesDir(),
			repoStorage,
			tarFileStorage,
			configuration.getBenchmarkRepoRemoteUrl()
		);
		BenchmarkWriteAccess benchmarkAccess = new BenchmarkWriteAccess(databaseStorage,
			availableDimensionsCache, latestRunCache);

		taskAccess.resetAllTaskStatuses();
		taskAccess.cleanUpTarFiles();

		// Data layer
		Queue queue = new Queue(taskAccess, archiveAccess, benchmarkAccess);
		BenchRepo benchRepo = new BenchRepo(archiveAccess);
		SignificanceFactors significanceFactors = new SignificanceFactors(
			configuration.getSignificanceRelativeThreshold(),
			configuration.getSignificanceStddevThreshold(),
			configuration.getSignificanceMinStddevAmount()
		);
		RunComparator runComparator = new RunComparator(significanceFactors);
		TimesliceComparison comparison = new TimesliceComparison(benchmarkAccess, commitAccess,
			dimensionAccess, runCache, latestRunCache);
		SignificantRunsCollector significantRunsCollector = new SignificantRunsCollector(
			significanceFactors, benchmarkAccess, commitAccess, dimensionAccess, runCache, latestRunCache,
			runComparator);

		// Listener
		Listener listener = new Listener(databaseStorage, repoStorage, repoAccess, benchRepo, queue,
			configuration.getPollInterval());

		// Dispatcher
		Dispatcher dispatcher = new Dispatcher(
			queue,
			configuration.getDisconnectedRunnerGracePeriod()
		);
		RunnerAwareServerFactory.getInstance().setDispatcher(dispatcher);
		RunnerAwareServerFactory.getInstance().setBenchRepo(benchRepo);

		// API
		configureSerialization(environment);
		configureExceptionMappers(environment);
		configureAuthentication(environment, configuration.getWebAdminToken());
		configureCors(environment);

		// Endpoints
		Stream.of(
			new AllReposEndpoint(dimensionAccess, repoAccess, availableDimensionsCache),
			new CommitEndpoint(benchmarkAccess, commitAccess, runCache),
			new CompareEndpoint(benchmarkAccess, commitAccess, dimensionAccess, runCache, latestRunCache,
				runComparator, significanceFactors),
			new DebugEndpoint(benchmarkAccess, dispatcher),
			new GraphComparisonEndpoint(dimensionAccess, comparison),
			new GraphDetailEndpoint(commitAccess, benchmarkAccess, dimensionAccess, repoAccess, runCache,
				latestRunCache),
			new ListenerEndpoint(listener),
			new QueueEndpoint(commitAccess, repoAccess, queue, dispatcher),
			new RecentRunsEndpoint(benchmarkAccess, commitAccess, dimensionAccess, runCache,
				significantRunsCollector),
			new RepoEndpoint(dimensionAccess, repoAccess, availableDimensionsCache,
				listener),
			new RunEndpoint(benchmarkAccess, commitAccess, dimensionAccess, runCache, latestRunCache,
				runComparator, significanceFactors, significantRunsCollector),
			new SearchEndpoint(benchmarkAccess, commitAccess),
			new TestTokenEndpoint()
		).forEach(endpoint -> environment.jersey().register(endpoint));
	}

	private void configureMetrics(Environment environment) {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(
			PrometheusConfig.DEFAULT
		);

		registry.config().commonTags("application", "Velcom");

		Metrics.globalRegistry.add(registry);

		Metrics.globalRegistry.config().meterFilter(new MeterFilter() {
			@Override
			public MeterFilterReply accept(Id id) {
				return MeterFilterReply.NEUTRAL;
			}

			@Override
			public Id map(Id id) {
				final String cacheTag = id.getTag("cache");
				if (cacheTag == null) {
					return id;
				}

				// meter is a cache meter => prepend cacheTag to name
				return new Id(
					cacheTag + "_" + id.getName(),
					Tags.of(id.getTags()),
					id.getBaseUnit(),
					id.getDescription(),
					id.getType()
				);
			}
		});

		try {
			Class.forName("org.aspectj.weaver.WeaverMessages");
			LOGGER.info("AspectJ weaver agent detected, providing detailed timing metrics");
			Gauge.builder("aspectj.weaver.enabled", () -> 1).register(registry);
		} catch (ClassNotFoundException e) {
			LOGGER.warn("AspectJ weaver agent NOT FOUND! Many metrics will NOT be available");
			Gauge.builder("aspectj.weaver.enabled", () -> 0).register(registry);
		}

		new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
		new JvmCompilationMetrics().bindTo(Metrics.globalRegistry);
		new JvmGcMetrics().bindTo(Metrics.globalRegistry);
		new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
		new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
		new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
		new ProcessorMetrics().bindTo(Metrics.globalRegistry);
		new UptimeMetrics().bindTo(Metrics.globalRegistry);

		environment.admin()
			.addServlet("prometheusMetrics", new HttpServlet() {
				@Override
				protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws IOException {
					resp.setStatus(HttpServletResponse.SC_OK);
					resp.getWriter().write(registry.scrape());
				}
			})
			.addMapping("/prometheusMetrics");
	}

	private void configureSerialization(Environment environment) {
		// This mapper should be configured the same as the one in SerializingTest.java
		environment.getObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.setSerializationInclusion(Include.NON_NULL);
	}

	private void configureExceptionMappers(Environment environment) {
		environment.jersey().register(new InvalidQueryParamsExceptionMapper());
		environment.jersey().register(new NoSuchCommitExceptionMapper());
		environment.jersey().register(new NoSuchDimensionExceptionMapper());
		environment.jersey().register(new NoSuchRepoExceptionMapper());
		environment.jersey().register(new NoSuchRunExceptionMapper());
		environment.jersey().register(new NoSuchTaskExceptionMapper());
		environment.jersey().register(new TaskAlreadyExistsExceptionMapper());
		environment.jersey().register(new ArgumentParseExceptionMapper());
	}

	private void configureAuthentication(Environment environment, String adminToken) {
		environment.jersey().register(
			new AuthDynamicFeature(
				new BasicCredentialAuthFilter.Builder<Admin>()
					.setAuthenticator(new AdminAuthenticator(adminToken))
					.buildAuthFilter()
			)
		);
		environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Admin.class));
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
