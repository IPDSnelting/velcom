package de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities;

import java.util.Objects;

/**
 * The name of a branch.
 */
public class BranchName implements Comparable<BranchName> {

	private final String name;

	private BranchName(String name) {
		this.name = name;
	}

	/**
	 * Create a new {@link BranchName} from a string of the format {@code "refs/heads/[ranchname]"}.
	 *
	 * @param name a string of the format {@code "refs/heads/[ranchname]"}
	 * @return the new {@link BranchName}
	 */
	public static BranchName fromFullName(String name) {
		return new BranchName(name);
	}

	/**
	 * Create a new {@link BranchName} from a string containing just the name of the branch.
	 *
	 * @param name a string containing just the name of the branch (i. e. {@code "master"})
	 * @return the new {@link BranchName}
	 */
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
