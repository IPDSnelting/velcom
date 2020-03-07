package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * A measurement unit that measurement values are given in.
 */
public class Unit {

	private final String name;

	public Unit(String name) {
		this.name = Objects.requireNonNull(name);
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Unit unit = (Unit) o;
		return name.equals(unit.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Unit{" +
			"name='" + name + '\'' +
			'}';
	}
}
