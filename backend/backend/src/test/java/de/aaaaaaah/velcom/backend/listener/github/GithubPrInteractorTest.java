package de.aaaaaaah.velcom.backend.listener.github;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GithubPrInteractorTest {

	@Test
	void buildSuccessfulPrReply() {
		StringBuilder builder = new StringBuilder();

		List<DimensionDifference> differences = List.of(
			new DimensionDifference(
				new Dimension("bin/lean", "binary size"),
				53_422_616.0, 92_456.0, new RunId(), null
			),
			new DimensionDifference(
				new Dimension("binarytrees", "branch-misses"),
				86_454_404.5, 95_712_731.1, new RunId(), 500000.0
			),
			new DimensionDifference(
				new Dimension("binarytrees", "instructions"),
				62_480_581_237.0, 64_322_330_171.1, new RunId(), 1000000.0
			),
			new DimensionDifference(
				new Dimension("deriv", "maxrss"),
				0.0, 10.0, new RunId(), 1.0
			),
			new DimensionDifference(
				new Dimension("const_fold", "branches"),
				100.0, 10.0, new RunId(), null
			)
		);
		List<Dimension> failed = List.of(
			new Dimension("const_fold", "instructions")
		);
		Map<Dimension, DimensionInfo> infos = Map.of(
			new Dimension("bin/lean", "binary size"),
			new DimensionInfo(
				new Dimension("bin/lean", "binary size"),
				new Unit("binary size"),
				Interpretation.LESS_IS_BETTER
			),
			new Dimension("binarytrees", "branch-misses"),
			new DimensionInfo(
				new Dimension("binarytrees", "branch-misses"),
				new Unit("branch-misses"),
				Interpretation.LESS_IS_BETTER
			),
			new Dimension("binarytrees", "instructions"),
			new DimensionInfo(
				new Dimension("binarytrees", "instructions"),
				new Unit("instructions"),
				Interpretation.LESS_IS_BETTER
			),
			new Dimension("deriv", "maxrss"),
			new DimensionInfo(
				new Dimension("deriv", "maxrss"),
				new Unit("thingies"),
				Interpretation.MORE_IS_BETTER
			),
			new Dimension("const_fold", "branches"),
			new DimensionInfo(
				new Dimension("const_fold", "branches"),
				new Unit("bits and bobs"),
				Interpretation.NEUTRAL
			)
		);
		GithubPrInteractor.buildSignificanceDiff(builder, differences, failed, infos);

		String target = ""
			+ "\n```diff"
			+ "\n  Benchmark     Metric          Change"
			+ "\n  ==============================================="
			+ "\n+ bin/lean      binary size     -99.8%"
			+ "\n- binarytrees   branch-misses    10.7%   (18.5 σ)"
			+ "\n- binarytrees   instructions      2.9% (1841.7 σ)"
			+ "\n  const_fold    branches        -90.0%"
			+ "\n- const_fold    instructions    failed"
			+ "\n+ deriv         maxrss               -   (10.0 σ)"
			+ "\n```";

		System.out.println(builder.toString());
		assertThat(builder.toString()).isEqualTo(target);
	}
}
