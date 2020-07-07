package de.aaaaaaah.velcom.backend.util;

/**
 * TODO
 * @param <T>
 * @param <R>
 * @param <E>
 */
public interface CheckedFunction<T, R, E extends Throwable> {

	/**
	 * TODO
	 * @param t
	 * @return
	 * @throws E
	 */
	R apply(T t) throws E;

}
