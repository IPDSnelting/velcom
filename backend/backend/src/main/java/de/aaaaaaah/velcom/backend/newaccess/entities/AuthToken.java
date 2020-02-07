package de.aaaaaaah.velcom.backend.newaccess.entities;

public class AuthToken {

	private final String token;

	public AuthToken(String token) {
		this.token = token;
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
