package de.aaaaaaah.velcom.runner.exceptions;

import java.net.http.HttpResponse;

/**
 * Thrown when the connection fails during the handshake.
 */
public class HandshakeFailureException extends ConnectionException {

	private HttpResponse<?> response;

	public HandshakeFailureException(HttpResponse<?> response) {
		super("Error in handshake");
		this.response = response;
	}

	public HttpResponse<?> getResponse() {
		return response;
	}

	/**
	 * Checks if the response was an authentication failure.
	 *
	 * @return true if the response was an authentication failure.
	 */
	public boolean isAuthenticationFailure() {
		return response.statusCode() == 403 || response.statusCode() == 401;
	}
}
