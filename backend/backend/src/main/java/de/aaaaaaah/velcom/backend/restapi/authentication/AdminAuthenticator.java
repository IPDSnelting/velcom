package de.aaaaaaah.velcom.backend.restapi.authentication;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import java.util.Optional;

public class AdminAuthenticator implements Authenticator<BasicCredentials, Admin> {

	private final String adminToken;

	public AdminAuthenticator(String adminToken) {
		this.adminToken = adminToken;
	}

	@Override
	public Optional<Admin> authenticate(BasicCredentials basicCredentials) {
		String username = basicCredentials.getUsername();
		String password = basicCredentials.getPassword();

		if (username.equals("admin") && password.equals(adminToken)) {
			return Optional.of(new Admin());
		}

		return Optional.empty();
	}
}
