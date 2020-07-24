package de.aaaaaaah.velcom.shared.util.systeminfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about the current system hardware and setup (as long as the current system is a linux
 * system).
 */
public class LinuxSystemInfo {

	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxSystemInfo.class);

	private final MemoryInfo memoryInfo;
	private final CpuInfo cpuInfo;

	private LinuxSystemInfo(MemoryInfo memoryInfo, CpuInfo cpuInfo) {
		this.memoryInfo = memoryInfo;
		this.cpuInfo = cpuInfo;
	}

	public MemoryInfo getMemoryInfo() {
		return memoryInfo;
	}

	public CpuInfo getCpuInfo() {
		return cpuInfo;
	}

	/**
	 * @return a snapshot of the current system state as a {@link LinuxSystemInfo}
	 */
	public static LinuxSystemInfo getCurrent() {
		List<String> meminfoLines = readFileOrEmpty(Path.of("/proc/meminfo"));
		List<String> cpuinfoLines = readFileOrEmpty(Path.of("/proc/cpuinfo"));

		MemoryInfo memoryInfo = MemoryInfo.fromMeminfo(meminfoLines);
		CpuInfo cpuInfo = new CpuInfo(new CoreInfoParser().coreInfos(cpuinfoLines));

		return new LinuxSystemInfo(memoryInfo, cpuInfo);
	}

	private static List<String> readFileOrEmpty(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			LOGGER.warn("Failed to read system info from " + path.toAbsolutePath(), e);
			return Collections.emptyList();
		}
	}
}
