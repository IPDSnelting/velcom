package de.aaaaaaah.velcom.shared.util.systeminfo;

import java.util.List;
import java.util.Optional;

/**
 * Contains information about system memory.
 */
public class MemoryInfo {

	private final long freeMemoryKib;
	private final long totalMemoryKib;

	public MemoryInfo(long freeMemoryKib, long totalMemoryKib) {
		this.freeMemoryKib = freeMemoryKib;
		this.totalMemoryKib = totalMemoryKib;
	}

	public long totalMemoryKib() {
		return totalMemoryKib;
	}

	public long freeMemoryKiB() {
		return freeMemoryKib;
	}

	/**
	 * Creates a {@link MemoryInfo} instance from the "/proc/meminfo" file.
	 * <br>
	 * If the meminfo string is not valid, dummy values of -1 will be inserted.
	 *
	 * @param meminfo the /proc/meminfo lines
	 * @return the created memory info.
	 */
	public static MemoryInfo fromMeminfo(List<String> meminfo) {
		long memTotal = lineFromMeminfo(meminfo, "MemTotal").orElse(-1L);
		long memFree = lineFromMeminfo(meminfo, "MemFree").orElse(-1L);

		return new MemoryInfo(memFree, memTotal);
	}

	private static Optional<Long> lineFromMeminfo(List<String> meminfo, String lineName) {
		return meminfo.stream()
			.filter(it -> it.startsWith(lineName))
			.findFirst()
			.map(it -> it.replaceAll("[^0-9]", ""))
			.flatMap(MemoryInfo::parseLong);
	}

	private static Optional<Long> parseLong(String input) {
		try {
			return Optional.of(Long.parseLong(input));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public String format() {
		if (totalMemoryKib > 256 * 1024) {
			return String
				.format("%d MiB used, %d MiB total", freeMemoryKib / 1024, totalMemoryKib / 1024);
		} else {
			return String.format("%d KiB used, %d KiB total", freeMemoryKib, totalMemoryKib);
		}
	}

	@Override
	public String toString() {
		return "MemoryInfo{" +
			"freeMemoryKib=" + freeMemoryKib +
			", totalMemoryKib=" + totalMemoryKib +
			'}';
	}
}
