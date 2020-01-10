package de.aaaaaaah.velcom.backend.access.token;

// TODO When exactly do we need this class?

/**
 * Represents a certain hashing algorithm used for storing tokens.
 */
public enum HashAlgorithm {

	/**
	 * The password hashing algorithm Argon2id. See https://github.com/P-H-C/phc-winner-argon2
	 */
	ARGON2ID(1);

	private final int id;

	HashAlgorithm(int id) {
		this.id = id;
	}

	/**
	 * @return Returns the unique identifier of this hash algorithm
	 */
	public int getId() {
		return id;
	}

}
