package de.feelix.sierra.utilities;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class provides utility methods for type casting and exception handling.
 */
public class CastUtil {

    /**
     * Executes a supplier function and returns its value. If an exception occurs during the execution,
     * the specified consumer is called with the exception, and null is returned.
     *
     * @param supplier  the supplier function to execute
     * @param onFailure the consumer to be called if an exception occurs
     * @param <T>       the type of the result
     * @return the result of the supplier function, or null if an exception occurs
     */
    public static <T> T getSupplierValue(Supplier<T> supplier, Consumer<Exception> onFailure) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return handleFailure(onFailure, e);
        }
    }

    /**
     * Handles the failure by calling the specified consumer with the exception and returning null.
     *
     * @param <T>        the type of the result
     * @param onFailure  the consumer to be called if an exception occurs
     * @param e          the exception that occurred
     * @return null
     */
    private static <T> T handleFailure(Consumer<Exception> onFailure, Exception e) {
        onFailure.accept(e);
        return null;
    }
}
