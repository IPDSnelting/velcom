package de.aaaaaaah.velcom.backend.access.repo;

import java.util.Objects;

/**
 * The name of a branch.
 */
public class BranchName {

	private final String name;

	public BranchName(String name) {
		this.name = name;
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
		BranchName that = (BranchName) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "BranchName{" +
			"name='" + name + '\'' +
			'}';
	}
}
