package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a pair of values.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 */
@Getter
// Original class: https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/utils/data/Pair.java#L23
public class Pair<A, B> {

    /**
     * Represents the first element of a Pair object.
     * This variable is immutable, meaning it cannot be modified after it is set.
     * It represents the first value of a pair of values.
     */
    private final A first;

    /**
     * Represents the second element of a Pair object.
     * This variable is immutable, meaning it cannot be modified after it is set.
     * It represents the second value of a pair of values.
     */
    private final B second;

    /**
     * Represents a pair of values.
     *
     * @param first the type of the first element
     * @param second the type of the second element
     */
    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a Pair object with the given values.
     *
     * @param a the value of the first element of the pair
     * @param b the value of the second element of the pair
     * @param <T> the type of the first element
     * @param <K> the type of the second element
     * @return the created Pair object
     */
    public static <T, K> Pair<T, K> of(T a, K b) {
        return new Pair<>(a, b);
    }

    /**
     * Checks if this Pair is equal to the specified object.
     * Two pairs are considered equal if they have the same first and second values.
     *
     * @param obj the object to compare this Pair with
     * @return true if the specified object is equal to this Pair, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        // check if both objects are of the same type
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        boolean isFirstEqual = Objects.equals(this.first, pair.first);
        boolean isSecondEqual = Objects.equals(this.second, pair.second);
        return isFirstEqual && isSecondEqual;
    }
}
