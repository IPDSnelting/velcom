package de.aaaaaaah.velcom.shared.util.systeminfo;

import java.util.Objects;

/**
 * Information about a single virtual core.
 */
public class CoreInfo {

	private final int virtualCoreId;
	private final int coreId;
	private final int physicalId;
	private final int cpuCores;
	private final String model;

	public CoreInfo(int virtualCoreId, int coreId, int physicalId, int cpuCores, String model) {
		this.virtualCoreId = virtualCoreId;
		this.coreId = coreId;
		this.physicalId = physicalId;
		this.cpuCores = cpuCores;
		this.model = model;
	}

	public int getCoreId() {
		return coreId;
	}

	public int getCpuCores() {
		return cpuCores;
	}

	public String getModel() {
		return model;
	}

	public int getPhysicalId() {
		return physicalId;
	}

	public int getVirtualCoreId() {
		return virtualCoreId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CoreInfo coreInfo = (CoreInfo) o;
		return virtualCoreId == coreInfo.virtualCoreId &&
			coreId == coreInfo.coreId &&
			physicalId == coreInfo.physicalId &&
			cpuCores == coreInfo.cpuCores &&
			Objects.equals(model, coreInfo.model);
	}

	@Override
	public int hashCode() {
		return Objects.hash(virtualCoreId, coreId, physicalId, cpuCores, model);
	}

	@Override
	public String toString() {
		return "CoreInfo{" +
			"virtualCoreId=" + virtualCoreId +
			", coreId=" + coreId +
			", physicalId=" + physicalId +
			", cpuCores=" + cpuCores +
			", model='" + model + '\'' +
			'}';
	}
}
