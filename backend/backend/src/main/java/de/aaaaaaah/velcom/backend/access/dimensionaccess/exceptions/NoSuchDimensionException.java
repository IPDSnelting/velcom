package de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;

/**
 * This exception is thrown whenever an invalid {@link Dimension} is used.
 */
public class NoSuchDimensionException extends RuntimeException {

	private final Dimension invalidDimension;

	public NoSuchDimensionException(Throwable t, Dimension invalidDimension) {
		super("no dimension " + invalidDimension, t);
		this.invalidDimension = invalidDimension;
	}

	public Dimension getInvalidDimension() {
		return invalidDimension;
	}
}
