package de.aaaaaaah.velcom.backend.listener.github;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;

public class JsonBodyHandler implements BodyHandler<JsonNode> {

	@Override
	public BodySubscriber<JsonNode> apply(ResponseInfo responseInfo) {
		return HttpResponse.BodySubscribers.mapping(
			HttpResponse.BodySubscribers.ofInputStream(),
			in -> {
				try {
					return Jackson.newObjectMapper().readTree(in);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		);
	}
}
