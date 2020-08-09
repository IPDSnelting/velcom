package de.aaaaaaah.velcom.shared.util.systeminfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoreInfoParserTest {

	private static final List<String> NORMAL_TEST_DATA = List.of(
		"processor	: 0",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"core id		: 0",
		"physical id	: 0",
		"cpu cores	: 4",
		"",

		"processor	: 1",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 0",
		"cpu cores	: 4",
		"",

		"processor	: 2",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 0",
		"cpu cores	: 4",
		"",

		"processor	: 3",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 0",
		"cpu cores	: 4",
		"",

		"processor	: 4",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 1",
		"cpu cores	: 4",
		"",

		"processor	: 5",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 1",
		"cpu cores	: 4",
		"",

		"processor	: 6",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 1",
		"cpu cores	: 4",
		"",

		"processor	: 7",
		"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
		"physical id	: 0",
		"core id		: 1",
		"cpu cores	: 4"
	);

	@Test
	void returnsEmptyListForNoInput() {
		List<CoreInfo> infos = new CoreInfoParser().coreInfos(Collections.emptyList());
		assertThat(infos).isEmpty();
	}

	@Test
	void usesDefaults() {
		List<CoreInfo> infos = new CoreInfoParser().coreInfos(List.of("hey"));
		assertThat(infos).containsExactly(
			new CoreInfo(-1, -1, -1, -1, "N/A")
		);
	}

	@Test
	void parsesTestCores() {
		List<CoreInfo> infos = new CoreInfoParser().coreInfos(NORMAL_TEST_DATA);
		assertThat(infos).containsExactlyInAnyOrder(
			new CoreInfo(0, 0, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(1, 0, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(2, 0, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(3, 0, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(4, 1, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(5, 1, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(6, 1, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(7, 1, 0, 4, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz")
		);
	}
}