package de.feelix.sierraapi.exceptions;

/**
 * SierraException is a custom exception class that extends the RuntimeException class.
 * It is used to indicate any exceptions that occur within the Sierra application.
 *
 * <p>Example usage:</p>
 * {@code
 * try {
 *     // Do something that may throw a SierraException
 * } catch (SierraException e) {
 *     // Handle the exception
 * }
 * }
 */
public class SierraException extends RuntimeException{

    /**
     * SierraException class is a custom exception that extends the RuntimeException class.
     * It is used to indicate any exceptions that occur within the Sierra application.
     *
     * @param cause The cause of the exception.
     */
    public SierraException(String cause) {
        super(cause);
    }
}
