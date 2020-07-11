package de.aaaaaaah.velcom.shared.util.systeminfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryInfoTest {

	@Test
	void usesMinusOneForEmptyList() {
		MemoryInfo meminfo = MemoryInfo.fromMeminfo(Collections.emptyList());
		assertThat(meminfo.freeMemoryKiB()).isEqualTo(-1);
		assertThat(meminfo.totalMemoryKib()).isEqualTo(-1);
	}

	@Test
	void usesMinusOneForInvalidList() {
		MemoryInfo meminfo = MemoryInfo.fromMeminfo(List.of("hello", "there"));
		assertThat(meminfo.freeMemoryKiB()).isEqualTo(-1);
		assertThat(meminfo.totalMemoryKib()).isEqualTo(-1);
	}

	@Test
	void usesMinusOneForListWithInvalidNumber() {
		MemoryInfo meminfo = MemoryInfo.fromMeminfo(List.of("MemTotal: 2000 kB", "MemFree: shdsj"));
		assertThat(meminfo.freeMemoryKiB()).isEqualTo(-1);
		assertThat(meminfo.totalMemoryKib()).isEqualTo(2000);
	}

	@Test
	void parseValid() {
		MemoryInfo meminfo = MemoryInfo.fromMeminfo(List.of(
			"Hugepagesize:       2048 kB",
			"Hugetlb:               0 kB",
			"MemTotal:       16247728 kB",
			"MemFree:         6526572 kB",
			"MemAvailable:   10049620 kB",
			"DirectMap4k:      260468 kB",
			"DirectMap2M:    13219840 kB",
			"DirectMap1G:     3145728 kB"
		));
		assertThat(meminfo.freeMemoryKiB()).isEqualTo(6526572);
		assertThat(meminfo.totalMemoryKib()).isEqualTo(16247728);
	}
}