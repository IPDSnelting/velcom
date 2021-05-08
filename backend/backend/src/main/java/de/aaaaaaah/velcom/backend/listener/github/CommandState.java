package de.aaaaaaah.velcom.backend.listener.github;

import java.util.List;

public enum CommandState {
	NEW("NEW"),
	MARKED_SEEN("MARKED_SEEN"),
	QUEUED("QUEUED"),
	RESPONDED("RESPONDED"),
	ERROR("ERROR");

	private final String textualRepresentation;

	CommandState(String textualRepresentation) {
		this.textualRepresentation = textualRepresentation;
	}

	public static CommandState fromTextualRepresentation(String representation)
		throws IllegalArgumentException {

		for (CommandState state : List.of(NEW, MARKED_SEEN, QUEUED, RESPONDED, ERROR)) {
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
