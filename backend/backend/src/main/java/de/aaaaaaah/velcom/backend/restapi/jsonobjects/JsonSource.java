package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.UUID;
import javax.annotation.Nullable;

public class JsonSource {

	private final JsonSourceType type;

	@Nullable
	private final JsonCommitDescription commitDescription;
	@Nullable
	private final JsonSourceUploadedTar uploadedTar;

	private JsonSource(JsonSourceType type, @Nullable JsonCommitDescription commitDescription,
		@Nullable JsonSourceUploadedTar uploadedTar) {

		this.type = type;
		this.commitDescription = commitDescription;
		this.uploadedTar = uploadedTar;
	}

	public static JsonSource fromCommit(JsonCommitDescription commitDescription) {
		return new JsonSource(JsonSourceType.COMMIT, commitDescription, null);
	}

	public static JsonSource fromUploadedTar(String description, @Nullable UUID repoId) {
		return new JsonSource(JsonSourceType.UPLOADED_TAR, null,
			new JsonSourceUploadedTar(description, repoId));
	}

	public JsonSourceType getType() {
		return type;
	}

	public Object getSource() {
		switch (type) {
			case COMMIT:
				return commitDescription;
			case UPLOADED_TAR:
				return uploadedTar;
		}
		return null;
	}

	private enum JsonSourceType {
		COMMIT, UPLOADED_TAR
	}

	private static class JsonSourceUploadedTar {

		private final String description;
		@Nullable
		private final UUID repoId;

		public JsonSourceUploadedTar(String description, @Nullable UUID repoId) {
			this.description = description;
			this.repoId = repoId;
		}

		public String getDescription() {
			return description;
		}

		@Nullable
		public UUID getRepoId() {
			return repoId;
		}
	}
}
