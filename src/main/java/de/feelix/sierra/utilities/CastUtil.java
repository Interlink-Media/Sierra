package de.feelix.sierra.utilities;

import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This class provides utility methods for type casting and exception handling.
 */
@UtilityClass
public class CastUtil {

    /**
     * This method executes the provided supplier and handles any exceptions that might occur.
     * If an exception occurs, the onFailure consumer is called with the exception as argument.
     *
     * @param supplier   The supplier to execute
     * @param onFailure  The consumer to handle exceptions
     * @param <T>        The type of the value returned by the supplier
     * @return The result of the supplier execution, or null if an exception occurred
     */
    public <T> T getSupplier(Supplier<T> supplier, Consumer<Exception> onFailure) {
        try {
            return supplier.get();
        } catch (Exception e) {
            onFailure.accept(e);
            return null;
        }
    }
}
