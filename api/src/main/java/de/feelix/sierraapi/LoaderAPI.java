package de.feelix.sierraapi;

import java.util.ArrayList;
import java.util.List;

/**
 * The LoaderAPI class provides functionality for registering and triggering enable callbacks.
 */
public class LoaderAPI {

    /**
     * List of EnableCallback objects that represents registered enable callbacks.
     *
     * <p>
     * The {@code callbacks} variable is a private static final List of EnableCallback objects.
     * These objects represent the callbacks that have been registered to be invoked when
     * the enable condition is met.
     * </p>
     *
     * @see EnableCallback
     * @see LoaderAPI#registerEnableCallback(EnableCallback)
     * @see LoaderAPI#triggerCallbacks()
     */
    private static final List<EnableCallback> callbacks = new ArrayList<>();

    /**
     * Registers a callback to be invoked when the enable condition is met.
     *
     * @param callback the callback to be registered
     */
    public static void registerEnableCallback(EnableCallback callback) {
        callbacks.add(callback);
    }

    /**
     * Method to trigger all registered enable callbacks.
     * <p>
     * This method iterates over the list of registered {@link EnableCallback} objects
     * and invokes the {@code onEnable} method for each of them.
     *
     * @see LoaderAPI#registerEnableCallback(EnableCallback)
     * @see EnableCallback
     */
    public static void triggerCallbacks() {
        for (EnableCallback callback : callbacks) {
            callback.onEnable();
        }
    }
}
