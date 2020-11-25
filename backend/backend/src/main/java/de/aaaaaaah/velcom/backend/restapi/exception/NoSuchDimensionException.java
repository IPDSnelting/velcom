package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;

public class NoSuchDimensionException extends RuntimeException {

	private final Dimension dimension;

	public NoSuchDimensionException(Dimension dimension) {
		this.dimension = dimension;
	}

	public Dimension getDimension() {
		return dimension;
	}
}
