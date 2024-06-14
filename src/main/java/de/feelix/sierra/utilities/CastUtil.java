package de.feelix.sierra.utilities;

import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class provides utility methods for type casting and exception handling.
 */
@UtilityClass
public class CastUtil {

    /**
     * Retrieves a value from a Supplier and handles any exceptions thrown.
     *
     * @param <T>       the type of the value to retrieve
     * @param supplier  the Supplier to retrieve the value from
     * @param onFailure the Consumer to be called if an exception occurs
     * @return the value retrieved from the Supplier, or null if an exception occurs
     */
    public <T> T getSupplier(Supplier<T> supplier, Consumer<Exception> onFailure) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return handleFailure(onFailure, e);
        }
    }

    /**
     * Handles the failure by calling the specified consumer with the exception and returning null.
     *
     * @param <T>       the type of the result
     * @param onFailure the consumer to be called if an exception occurs
     * @param e         the exception that occurred
     * @return null
     */
    private <T> T handleFailure(Consumer<Exception> onFailure, Exception e) {
        onFailure.accept(e);
        return null;
    }
}
