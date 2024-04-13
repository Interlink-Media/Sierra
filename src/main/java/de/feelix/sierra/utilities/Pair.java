package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.Objects;

@Getter
// Original class: https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/utils/data/Pair.java#L23
public class Pair<A, B> {

    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <T, K> Pair<T, K> of(T a, K b) {
        return new Pair<T, K>(a, b);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair b = (Pair) o;
        return Objects.equals(this.first, b.first) && Objects.equals(this.second, b.second);
    }
}
