package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.util.Objects;

/**
 * The name of a branch.
 */
public class BranchName implements Comparable<BranchName> {

	private final String name;

	public BranchName(String name) {
		this.name = name;
	}

	public static BranchName fromFullName(String name) {
		return new BranchName(name);
	}

	public static BranchName fromName(String name) {
		return new BranchName("refs/heads/" + name);
	}

	public String getFullName() {
		return name;
	}

	public String getName() {
		if (name.startsWith("refs/heads/")) {
			return name.substring(11);
		} else {
			// Not sure what format the name is in
			return name;
		}
	}

	@Override
	public int compareTo(BranchName o) {
		return name.compareTo(o.name);
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
