package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * The information velcom knows about a repository it tracks.
 */
public class Repo {

	private final RepoId id;
	private final String name;
	private final RemoteUrl remoteUrl;
	@Nullable
	private final String githubAccessToken;
	@Nullable
	private final Instant githubCommentCutoff;

	public Repo(RepoId id, String name, RemoteUrl remoteUrl, @Nullable String githubAccessToken,
		@Nullable Instant githubCommentCutoff) {

		if ((githubAccessToken == null) != (githubCommentCutoff == null)) {
			throw new IllegalArgumentException(
				"githubAccessToken and githubCommentCutoff must either both be present or both be null");
		}

		this.id = id;
		this.name = name;
		this.remoteUrl = remoteUrl;
		this.githubAccessToken = githubAccessToken;
		this.githubCommentCutoff = githubCommentCutoff;
	}

	public RepoId getId() {
		return id;
	}

	public UUID getIdAsUuid() {
		return id.getId();
	}

	public String getIdAsString() {
		return id.getIdAsString();
	}

	public String getName() {
		return name;
	}

	public RemoteUrl getRemoteUrl() {
		return remoteUrl;
	}

	public String getRemoteUrlAsString() {
		return remoteUrl.getUrl();
	}

	public Optional<String> getGithubRepoName() {
		String remoteUrl = getRemoteUrlAsString();
		Pattern pattern = Pattern.compile("^(https://|git@)github.com[:/]([^/]+/[^/.]+)(\\.git)?$");
		Matcher matcher = pattern.matcher(remoteUrl);
		if (matcher.find()) {
			return Optional.of(matcher.group(2));
		} else {
			return Optional.empty();
		}
	}

	public Optional<GithubInfo> getGithubInfo() {
		Optional<String> repoName = getGithubRepoName();
		if (repoName.isPresent() && githubAccessToken != null && githubCommentCutoff != null) {
			return Optional.of(new GithubInfo(repoName.get(), githubAccessToken, githubCommentCutoff));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Repo repo = (Repo) o;
		return Objects.equals(id, repo.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Repo{" +
			"id=" + id.getIdAsString() +
			", name='" + name + '\'' +
			", remoteUrl='" + remoteUrl.getUrl() + '\'' +
			", githubCommentCutoff=" + githubCommentCutoff +
			'}';
	}

	public static class GithubInfo {

		private final String repoName;
		private final String accessToken;
		private final Instant commentCutoff;

		public GithubInfo(String repoName, String accessToken, Instant commentCutoff) {
			this.repoName = repoName;
			this.accessToken = accessToken;
			this.commentCutoff = commentCutoff;
		}

		public String getRepoName() {
			return repoName;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public Instant getCommentCutoff() {
			return commentCutoff;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			GithubInfo that = (GithubInfo) o;
			return Objects.equals(repoName, that.repoName) && Objects
				.equals(accessToken, that.accessToken) && Objects
				.equals(commentCutoff, that.commentCutoff);
		}

		@Override
		public int hashCode() {
			return Objects.hash(repoName, accessToken, commentCutoff);
		}

		@Override
		public String toString() {
			return "GithubInfo{" +
				"repoName='" + repoName + '\'' +
				", commentCutoff=" + commentCutoff +
				'}';
		}
	}
}
