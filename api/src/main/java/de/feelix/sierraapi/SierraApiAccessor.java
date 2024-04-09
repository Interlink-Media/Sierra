package de.feelix.sierraapi;

import de.feelix.sierraapi.exceptions.impl.SierraNotLoadedException;

import java.lang.ref.WeakReference;

/**
 * This class provides access to the SierraAPI instance.
 */
@SuppressWarnings("unused")
public class SierraApiAccessor {

    private static WeakReference<SierraApi> sierraAPI = null;

    /**
     * Sets the SierraAPI instance.
     *
     * @param instance the SierraAPI instance to be set
     */
    public static synchronized void setSierraApiInstance(SierraApi instance) {
        sierraAPI = new WeakReference<>(instance);
    }

    /**
     * Returns a weak reference to the SierraAPI instance if it exists.
     *
     * @return a weak reference to the SierraAPI instance
     * @throws SierraNotLoadedException if the SierraAPI instance has not been loaded
     */
    public static synchronized WeakReference<SierraApi> access() throws SierraNotLoadedException {
        if (sierraAPI != null) {
            return sierraAPI;
        } else {
            throw new SierraNotLoadedException();
        }
    }
}
