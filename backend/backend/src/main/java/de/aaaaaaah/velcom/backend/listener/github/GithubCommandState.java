package de.aaaaaaah.velcom.backend.listener.github;

import java.util.List;

public enum GithubCommandState {
	NEW("NEW"),
	MARKED_SEEN("MARKED_SEEN"),
	QUEUED("QUEUED"),
	ERROR("ERROR");

	private final String textualRepresentation;

	GithubCommandState(String textualRepresentation) {
		this.textualRepresentation = textualRepresentation;
	}

	public static GithubCommandState fromTextualRepresentation(String representation)
		throws IllegalArgumentException {

		for (GithubCommandState state : List.of(NEW, MARKED_SEEN, QUEUED, ERROR)) {
			if (state.getTextualRepresentation().equals(representation)) {
				return state;
			}
		}

		throw new IllegalArgumentException("\"" + representation + "\" is not a valid command state");
	}

	public String getTextualRepresentation() {
		return textualRepresentation;
	}
}
