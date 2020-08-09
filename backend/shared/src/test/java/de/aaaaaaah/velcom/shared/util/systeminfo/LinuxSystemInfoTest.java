package de.aaaaaaah.velcom.shared.util.systeminfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import de.aaaaaaah.velcom.shared.util.OSCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinuxSystemInfoTest {

	private LinuxSystemInfo systemInfo;

	@BeforeEach
	void setUp() {
		systemInfo = LinuxSystemInfo.getCurrent();
	}

	@Test
	void hasPlausibleCpuInfo() {
		assumeFalse(OSCheck.isStupidWindows());

		assertThat(systemInfo.getCpuInfo().cpuCount()).isGreaterThan(0);
		assertThat(systemInfo.getCpuInfo().physicalCoreCount()).isGreaterThan(0);
		assertThat(systemInfo.getCpuInfo().coreModels()).hasSizeGreaterThanOrEqualTo(1);

		assertThat(systemInfo.getCpuInfo().virtualCoreCount())
			.isGreaterThanOrEqualTo(Runtime.getRuntime().availableProcessors());

	}

	@Test
	void hasPlausibleMemoryInfo() {
		assumeFalse(OSCheck.isStupidWindows());

		assertThat(systemInfo.getMemoryInfo().freeMemoryKiB()).isGreaterThanOrEqualTo(0);
		assertThat(systemInfo.getMemoryInfo().totalMemoryKib())
			.isGreaterThanOrEqualTo(Runtime.getRuntime().maxMemory() / 1024);
	}
}