package de.aaaaaaah.velcom.backend.data.repocomparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimesliceComparisonTest {

	private BenchmarkReadAccess benchmarkReadAccess;
	private CommitReadAccess commitReadAccess;
	private DimensionReadAccess dimensionAccess;
	private RunCache runCache;
	private LatestRunCache latestRunCache;
	private RepoComparison comparison;

	private Dimension dimension;
	private DimensionInfo dimensionInfo;
	private RepoId repoId;
	private Set<BranchName> startBranches;
	private Map<RepoId, Set<BranchName>> repoBranches;

	private CommitHash c1Hash;
	private CommitHash c2Hash;
	private CommitHash c3Hash;
	private CommitHash c4Hash;
	private Commit c1;
	private Commit c2;
	private Commit c3;
	private Commit c4;
	private List<Commit> commitList;

	private Measurement m1;
	private Measurement m2;
	private Measurement m3;
	private Measurement m4;
	private Run r1;
	private Run r2;
	private Run r3;
	private Run r4;
	private Map<CommitHash, Run> runMap;

	@BeforeEach
	void setup() {
		benchmarkReadAccess = mock(BenchmarkReadAccess.class);
		commitReadAccess = mock(CommitReadAccess.class);
		dimensionAccess = mock(DimensionReadAccess.class);
		runCache = mock(RunCache.class);
		latestRunCache = mock(LatestRunCache.class);
		comparison = new TimesliceComparison(benchmarkReadAccess, commitReadAccess, dimensionAccess,
			runCache, latestRunCache);

		c1Hash = new CommitHash("hash1");
		c2Hash = new CommitHash("hash2");
		c3Hash = new CommitHash("hash3");
		c4Hash = new CommitHash("hash4");
		c1 = mock(Commit.class);
		c2 = mock(Commit.class);
		c3 = mock(Commit.class);
		c4 = mock(Commit.class);
		when(c1.getHash()).thenReturn(c1Hash);
		when(c2.getHash()).thenReturn(c2Hash);
		when(c3.getHash()).thenReturn(c3Hash);
		when(c4.getHash()).thenReturn(c4Hash);
		commitList = new ArrayList<>();
		commitList.add(c1);
		commitList.add(c2);
		commitList.add(c3);
		commitList.add(c4);

		dimension = new Dimension("benchmark", "metric");
		dimensionInfo = new DimensionInfo(dimension, new Unit("testunit"),
			Interpretation.MORE_IS_BETTER, true);
		repoId = new RepoId(UUID.randomUUID());
		startBranches = Set.of(BranchName.fromName("main"), BranchName.fromName("stable"));

		repoBranches = new HashMap<>();
		repoBranches.put(repoId, startBranches);

		m1 = new Measurement(mock(RunId.class), dimension,
			Either.ofRight(new MeasurementValues(List.of(1d, 2d, 3d))));
		m2 = new Measurement(mock(RunId.class), dimension,
			Either.ofRight(new MeasurementValues(List.of(4d, 5d, 6d))));
		m3 = new Measurement(mock(RunId.class), dimension,
			Either.ofRight(new MeasurementValues(List.of(7d, 8d, 9d))));
		m4 = new Measurement(mock(RunId.class), dimension,
			Either.ofRight(new MeasurementValues(List.of(10d, 11d, 12d))));
		r1 = mock(Run.class);
		r2 = mock(Run.class);
		r3 = mock(Run.class);
		r4 = mock(Run.class);
		when(r1.getResult()).thenReturn(Either.ofRight(List.of(m1)));
		when(r2.getResult()).thenReturn(Either.ofRight(List.of(m2)));
		when(r3.getResult()).thenReturn(Either.ofRight(List.of(m3)));
		when(r4.getResult()).thenReturn(Either.ofRight(List.of(m4)));
		runMap = new HashMap<>();
		runMap.put(c1Hash, r1);
		runMap.put(c2Hash, r2);
		runMap.put(c3Hash, r3);
		runMap.put(c4Hash, r4);

		when(latestRunCache.getLatestRuns(
			any(BenchmarkReadAccess.class),
			any(RunCache.class),
			eq(repoId),
			anySet()
		)).thenReturn(runMap);

		when(dimensionAccess.getDimensionInfo(dimension)).thenReturn(dimensionInfo);
	}

	@Test
	void hourly() {
		// c1 and c3 are exactly one hour apart and thus must be sorted into different entry groups.
		// c2 is right between c1 and c3 and thus must be sorted into either c1's or c3's group.
		// c4 is more than an hour apart from c3.
		// The hourly threshold is not hit.
		// This means that there should be exactly 3 groups.
		long startTime = 1577873472;
		long c1Time = startTime + 123;
		long c2Time = c1Time + 60 * 50;
		long c3Time = c1Time + 60 * 60;
		long c4Time = c3Time + 60 * 60 + 1;
		long stopTime = startTime + TimesliceComparison.HOURLY_THRESHOLD - 1;
		Instant startInstant = Instant.ofEpochSecond(startTime);
		Instant stopInstant = Instant.ofEpochSecond(stopTime);

		when(c1.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c1Time));
		when(c2.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c2Time));
		when(c3.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c3Time));
		when(c4.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c4Time));

		when(commitReadAccess.getCommitsBetween(repoId, startBranches, startInstant, stopInstant))
			.thenReturn(commitList);

		RepoComparisonGraph graph = comparison
			.generateGraph(dimension, repoBranches, startInstant, stopInstant);

		assertEquals(dimension, graph.getDimensionInfo().getDimension());
		assertEquals(1, graph.getData().size()); // Only one repo

		RepoGraphData data = graph.getData().get(0);
		assertEquals(repoId, data.getRepoId());
		assertEquals(3, data.getEntries().size());
	}

	@Test
	void daily() {
		// c1 and c3 are exactly one day apart and thus must be sorted into different entry groups.
		// c2 is right between c1 and c3 and thus must be sorted into either c1's or c3's group.
		// c4 is more than a day apart from c3.
		// The daily threshold is not hit.
		// This means that there should be exactly 3 groups.
		long startTime = 1577873472;
		long c1Time = startTime + 123;
		long c2Time = c1Time + 60 * 60 * 22;
		long c3Time = c1Time + 60 * 60 * 24;
		long c4Time = c3Time + 60 * 60 * 24 + 1;
		long stopTime = startTime + TimesliceComparison.DAILY_THRESHOLD - 1;
		Instant startInstant = Instant.ofEpochSecond(startTime);
		Instant stopInstant = Instant.ofEpochSecond(stopTime);

		when(c1.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c1Time));
		when(c2.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c2Time));
		when(c3.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c3Time));
		when(c4.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c4Time));

		when(commitReadAccess.getCommitsBetween(repoId, startBranches, startInstant, stopInstant))
			.thenReturn(commitList);

		RepoComparisonGraph graph = comparison.generateGraph(dimension, repoBranches,
			startInstant, stopInstant);

		assertEquals(dimension, graph.getDimensionInfo().getDimension());
		assertEquals(1, graph.getData().size()); // Only one repo

		RepoGraphData data = graph.getData().get(0);
		assertEquals(repoId, data.getRepoId());
		assertEquals(3, data.getEntries().size());
	}

	@Test
	void weekly() {
		// c1 and c3 are exactly one week apart and thus must be sorted into different entry groups.
		// c2 is right between c1 and c3 and thus must be sorted into either c1's or c3's group.
		// c4 is more than a week apart from c3.
		// The daily threshold is hit.
		// This means that there should be exactly 3 groups.
		long startTime = 1577873472;
		long c1Time = startTime + 123;
		long c2Time = c1Time + 60 * 60 * 24 * 3;
		long c3Time = c1Time + 60 * 60 * 24 * 7;
		long c4Time = c3Time + 60 * 60 * 24 * 7 + 1;
		long stopTime = startTime + TimesliceComparison.DAILY_THRESHOLD;
		Instant startInstant = Instant.ofEpochSecond(startTime);
		Instant stopInstant = Instant.ofEpochSecond(stopTime);

		when(c1.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c1Time));
		when(c2.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c2Time));
		when(c3.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c3Time));
		when(c4.getAuthorDate()).thenReturn(Instant.ofEpochSecond(c4Time));

		when(commitReadAccess.getCommitsBetween(repoId, startBranches, startInstant, stopInstant))
			.thenReturn(commitList);

		RepoComparisonGraph graph = comparison.generateGraph(dimension, repoBranches,
			startInstant, stopInstant);

		assertEquals(dimension, graph.getDimensionInfo().getDimension());
		assertEquals(1, graph.getData().size()); // Only one repo

		RepoGraphData data = graph.getData().get(0);
		assertEquals(repoId, data.getRepoId());
		assertEquals(3, data.getEntries().size());
	}
}
