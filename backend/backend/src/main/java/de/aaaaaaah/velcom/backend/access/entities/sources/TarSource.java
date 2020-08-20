package de.aaaaaaah.velcom.backend.access.entities.sources;

import java.util.Objects;

public class TarSource {

	private final String tarName;

	public TarSource(String tarName) {
		this.tarName = Objects.requireNonNull(tarName);
	}

	public String getTarName() {
		return tarName;
	}

	@Override
	public String toString() {
		return "TarSource{" +
			"tarName='" + tarName + '\'' +
			'}';
	}

}
