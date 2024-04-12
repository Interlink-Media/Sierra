package de.feelix.sierraapi.exceptions.impl;

import de.feelix.sierraapi.exceptions.SierraException;

/**
 * SierraNotLoadedException is a custom exception class that extends the SierraException class.
 * It is thrown when the SierraApi is not yet loaded.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     // Do something that may throw a SierraNotLoadedException
 * } catch (SierraNotLoadedException e) {
 *     // Handle the exception
 * }
 * }</pre>
 */
public class SierraNotLoadedException extends SierraException {

    /**
     * Custom exception class that extends the RuntimeException class.
     * It is thrown when the SierraApi is not yet loaded.
     */
    public SierraNotLoadedException() {
        super("Sierra is not yet loaded.");
    }
}
