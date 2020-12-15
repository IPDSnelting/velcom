package de.aaaaaaah.velcom.backend.newaccess.tokenaccess.entities;

import java.util.Objects;

/**
 * A token used for authentication as repo admin or global admin.
 */
public class AuthToken {

	private final String token;

	public AuthToken(String token) {
		this.token = Objects.requireNonNull(token);
	}

	public String getToken() {
		return token;
	}

	@Override
	public String toString() {
		// Auth token content censored so it doesn't appear in any logs
		return "AuthToken{token=*******}";
	}

}
