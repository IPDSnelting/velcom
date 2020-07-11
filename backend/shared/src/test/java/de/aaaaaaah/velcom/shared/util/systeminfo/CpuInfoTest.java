package de.aaaaaaah.velcom.shared.util.systeminfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CpuInfoTest {

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
	private CpuInfo cpuInfo;

	@BeforeEach
	void setUp() {
		cpuInfo = new CpuInfo(new CoreInfoParser().coreInfos(NORMAL_TEST_DATA));
	}

	@Test
	void testCounts() {
		assertThat(cpuInfo.virtualCoreCount()).isEqualTo(8);
		assertThat(cpuInfo.physicalCoreCount()).isEqualTo(4);
		assertThat(cpuInfo.cpuCount()).isEqualTo(1);
		assertThat(cpuInfo.coreModels())
			.containsExactly("Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz");
	}

	@Test
	void testModelReturnsSet() {
		List<String> data = new ArrayList<>(NORMAL_TEST_DATA);
		data.set(1, "model name: My cool model");
		cpuInfo = new CpuInfo(new CoreInfoParser().coreInfos(data));

		assertThat(cpuInfo.coreModels())
			.containsExactly(
				"Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
				"My cool model");
	}

	@Test
	void testMultipleCpusCountedCorrectly() {
		List<String> data = List.of(
			"processor	: 0",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 0",
			"core id		: 0",
			"cpu cores	: 1",
			"",

			"processor	: 1",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 1",
			"core id		: 0",
			"cpu cores	: 1",
			"",

			"processor	: 2",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 1",
			"core id		: 1",
			"cpu cores	: 1",
			""
		);
		cpuInfo = new CpuInfo(new CoreInfoParser().coreInfos(data));

		assertThat(cpuInfo.coreModels())
			.containsExactly("Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz");
		assertThat(cpuInfo.cpuCount()).isEqualTo(2);
		assertThat(cpuInfo.physicalCoreCount()).isEqualTo(2);
		assertThat(cpuInfo.virtualCoreCount()).isEqualTo(3);
	}

	@Test
	void testEqualsCoreInfos() {
		List<String> data = List.of(
			"processor	: 0",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 0",
			"core id		: 0",
			"cpu cores	: 1",
			"",

			"processor	: 1",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 1",
			"core id		: 0",
			"cpu cores	: 1",
			"",

			"processor	: 2",
			"model name	: Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz",
			"physical id	: 1",
			"core id		: 1",
			"cpu cores	: 1",
			""
		);
		cpuInfo = new CpuInfo(new CoreInfoParser().coreInfos(data));

		assertThat(cpuInfo).isEqualTo(new CpuInfo(List.of(
			new CoreInfo(0, 0, 0, 1, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(1, 0, 1, 1, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz"),
			new CoreInfo(2, 1, 1, 1, "Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz")
			))
		);
	}
}