package de.aaaaaaah.velcom.backend.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * An {@code Either<L, R>} can either have a left value of type {@code L} or a right value of type
 * {@code R}. It can't have no value or both values at the same time.
 *
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
public final class Either<L, R> {

	private final L left;
	private final R right;

	private Either(L left, R right) {
		this.left = left;
		this.right = right;
	}

	/**
	 * Create a new {@link Either} with a left value.
	 *
	 * @param left the left value
	 * @param <L2> the type of the left value
	 * @param <R2> the type of the right value
	 * @return the new {@link Either}
	 */
	public static <L2, R2> Either<L2, R2> ofLeft(L2 left) {
		return new Either<>(Objects.requireNonNull(left), null);
	}


	/**
	 * Create a new {@link Either} with a right value.
	 *
	 * @param right the right value
	 * @param <L2> the type of the left value
	 * @param <R2> the type of the right value
	 * @return the new {@link Either}
	 */
	public static <L2, R2> Either<L2, R2> ofRight(R2 right) {
		return new Either<>(null, Objects.requireNonNull(right));
	}

	/**
	 * @return whether this {@link Either} has a right value
	 */
	public boolean isRight() {
		return right != null;
	}

	/**
	 * @return whether this {@link Either} has a left value
	 */
	public boolean isLeft() {
		return left != null;
	}

	public Optional<L> getLeft() {
		return Optional.ofNullable(left);
	}

	public Optional<R> getRight() {
		return Optional.ofNullable(right);
	}

	public <Result> Result consume(Function<L, Result> left, Function<R, Result> right) {
		return isLeft() ? left.apply(this.left) : right.apply(this.right);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Either<?, ?> either = (Either<?, ?>) o;
		return Objects.equals(left, either.left) &&
			Objects.equals(right, either.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}
}
