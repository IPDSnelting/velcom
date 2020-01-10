package de.aaaaaaah.designproto.backend.util;

import java.util.Objects;

/**
 * A simple key-value pair.
 *
 * <p>
 * This pair has a consistent {@link #equals(Object)} and {@link #hashCode()} method, if the two
 * element types have one.
 *
 * @param <A> the type of the first element in the pair
 * @param <B> the type of the second element in the pair
 */
public class Pair<A, B> {

  private final A first;
  private final B second;

  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(first, pair.first) &&
        Objects.equals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }
}
