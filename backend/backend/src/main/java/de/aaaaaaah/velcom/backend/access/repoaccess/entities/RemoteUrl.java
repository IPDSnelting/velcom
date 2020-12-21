package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import java.util.Objects;

/**
 * Locate a remote repository.
 */
public class RemoteUrl {

	private final String url;

	/**
	 * Construct a new remote url.
	 *
	 * @param url the url
	 */
	public RemoteUrl(String url) {
		this.url = Objects.requireNonNull(url);

		if (url.isBlank()) {
			throw new IllegalArgumentException("url must not be blank: " + url);
		}
	}

	public String getUrl() {
		return url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RemoteUrl remoteUrl = (RemoteUrl) o;
		return url.equals(remoteUrl.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(url);
	}

	@Override
	public String toString() {
		return "RemoteUrl{" +
			"url='" + url + '\'' +
			'}';
	}

}
