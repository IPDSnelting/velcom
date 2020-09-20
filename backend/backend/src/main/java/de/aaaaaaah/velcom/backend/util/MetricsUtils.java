package de.aaaaaaah.velcom.backend.util;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;

public class MetricsUtils {

	public static Timer timer(String name) {
		return Timer.builder(name)
			.publishPercentileHistogram(true)
			.register(Metrics.globalRegistry);
	}

	public static Timer timer(String name, Tag... tags) {
		return Timer.builder(name)
			.tags(Arrays.asList(tags))
			.publishPercentileHistogram(true)
			.register(Metrics.globalRegistry);
	}

	public static Timer timer(String name, String... tagPairs) {
		return Timer.builder(name)
			.tags(tagPairs)
			.publishPercentileHistogram(true)
			.register(Metrics.globalRegistry);
	}

}
