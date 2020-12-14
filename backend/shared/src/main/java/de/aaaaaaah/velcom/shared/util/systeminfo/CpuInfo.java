package de.aaaaaaah.velcom.shared.util.systeminfo;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information about one or more CPUs.
 */
public class CpuInfo {

	private final List<CoreInfo> coreInfos;

	public CpuInfo(List<CoreInfo> coreInfos) {
		this.coreInfos = new ArrayList<>(coreInfos);
	}

	/**
	 * @return the amount of virtual cores.
	 */
	public int virtualCoreCount() {
		return coreInfos.size();
	}

	/**
	 * @return the amount of physical cores.
	 */
	public int physicalCoreCount() {
		return coreInfos.stream()
			.collect(groupingBy(CoreInfo::getPhysicalId, toList()))
			.values()
			.stream()
			.mapToInt(it -> it.get(0).getCpuCores())
			.sum();
	}

	/**
	 * @return the amount of CPUs (might be more than one if you use some dedicated server hardware)
	 */
	public int cpuCount() {
		return (int) coreInfos.stream()
			.mapToInt(CoreInfo::getPhysicalId)
			.distinct()
			.count();
	}

	/**
	 * @return the model of all virtual cores (e.g. Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz)
	 */
	public Set<String> coreModels() {
		return coreInfos.stream()
			.map(CoreInfo::getModel)
			.collect(Collectors.toSet());
	}

	/**
	 * @return a human-readable string summarizing this info
	 */
	public String format() {
		String models = String.join(",", coreModels());
		int n = virtualCoreCount();
		return String.format("%s (%d %s)", models, n, (n == 1) ? "thread" : "threads");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CpuInfo cpuInfo = (CpuInfo) o;
		return Objects.equals(coreInfos, cpuInfo.coreInfos);
	}

	@Override
	public int hashCode() {
		return Objects.hash(coreInfos);
	}

	@Override
	public String toString() {
		return "CpuInfo{" +
			"coreInfos=" + coreInfos +
			'}';
	}
}
