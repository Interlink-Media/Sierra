package de.feelix.sierra.check.violation;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Debug<T> {
    private final String name;
    private final T      info;
}
