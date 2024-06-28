package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a triple of values.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 */
@Getter
public class Triple<A, B, C> {

    /**
     * Represents the first element of a Triple object.
     * This variable is immutable, meaning it cannot be modified after it is set.
     * It represents the first value of a triple of values.
     */
    private final A first;

    /**
     * Represents the second element of a Triple object.
     * This variable is immutable, meaning it cannot be modified after it is set.
     * It represents the second value of a triple of values.
     */
    private final B second;

    /**
     * Represents the third element of a Triple object.
     * This variable is immutable, meaning it cannot be modified after it is set.
     * It represents the third value of a triple of values.
     */
    private final C third;

    /**
     * Represents a triple of values.
     *
     * @param first the type of the first element
     * @param second the type of the second element
     * @param third the type of the third element
     */
    public Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Creates a Triple object with the given values.
     *
     * @param a the value of the first element of the triple
     * @param b the value of the second element of the triple
     * @param c the value of the third element of the triple
     * @param <T> the type of the first element
     * @param <K> the type of the second element
     * @param <L> the type of the third element
     * @return the created Triple object
     */
    public static <T, K, L> Triple<T, K, L> of(T a, K b, L c) {
        return new Triple<>(a, b, c);
    }

    /**
     * Checks if this Triple is equal to the specified object.
     * Two triples are considered equal if they have the same first, second, and third values.
     *
     * @param obj the object to compare this Triple with
     * @return true if the specified object is equal to this Triple, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        // check if both objects are of the same type
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) obj;
        boolean isFirstEqual = Objects.equals(this.first, triple.first);
        boolean isSecondEqual = Objects.equals(this.second, triple.second);
        boolean isThirdEqual = Objects.equals(this.third, triple.third);
        return isFirstEqual && isSecondEqual && isThirdEqual;
    }
}