package de.aaaaaaah.velcom.shared.util;

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

	/**
	 * Turn an {@link Either} into a value of another type using one function to convert a left value
	 * to the target type and one to convert a right value to the target type.
	 *
	 * @param left the function that creates a value of the target type from a left value
	 * @param right the function that creates a value of the target type from a right value
	 * @param <Result> the target type
	 * @return a value of the target type using either the left or right function
	 */
	public <Result> Result consume(Function<L, Result> left, Function<R, Result> right) {
		return isLeft() ? left.apply(this.left) : right.apply(this.right);
	}

	/**
	 * Map one of two functions over the {@link Either} depeding on whether it's left or right.
	 *
	 * @param left the function to map over a left value
	 * @param right the function to map over the right value
	 * @param <L2> the result left type
	 * @param <R2> the result right type
	 * @return the mapped-over {@link Either}
	 */
	public <L2, R2> Either<L2, R2> mapBoth(Function<L, L2> left, Function<R, R2> right) {
		return consume(
			l -> Either.ofLeft(left.apply(l)),
			r -> Either.ofRight(right.apply(r))
		);
	}

	/**
	 * Map a function over the left value, if the {@link Either} is left.
	 *
	 * @param left the function to map over the left value
	 * @param <L2> the resulting left type
	 * @return the mapped-over {@link Either}
	 */
	public <L2> Either<L2, R> mapLeft(Function<L, L2> left) {
		return mapBoth(left, it -> it);
	}

	/**
	 * Map a function over the right value, if the {@link Either} is right.
	 *
	 * @param right the function to map over the right value
	 * @param <R2> the resulting right type
	 * @return the mapped-over {@link Either}
	 */
	public <R2> Either<L, R2> mapRight(Function<R, R2> right) {
		return mapBoth(it -> it, right);
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
