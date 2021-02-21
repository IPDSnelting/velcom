package de.aaaaaaah.velcom.backend.restapi.authentication;

import java.security.Principal;

public class Admin implements Principal {

	public Admin() {
	}

	@Override
	public String getName() {
		return "admin";
	}
}
