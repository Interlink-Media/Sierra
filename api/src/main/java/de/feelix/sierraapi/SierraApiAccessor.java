package de.feelix.sierraapi;

import de.feelix.sierraapi.exceptions.impl.SierraNotLoadedException;

import java.lang.ref.WeakReference;


/**
 * The SierraApiAccessor class provides a way to access the SierraApi instance.
 * It holds a weak reference to the instance of SierraApi.
 */
@SuppressWarnings("unused")
public class SierraApiAccessor {

    /**
     * The sierraAPI variable holds a weak reference to an instance of the SierraApi interface.
     * This variable is used to access the UserRepository functionality provided by the SierraApi.
     * If SierraApi is not loaded, a SierraNotLoadedException will be thrown.
     * To set the SierraApi instance, use the setSierraApiInstance() method provided by the SierraApiAccessor class.
     * To access the SierraApi instance, use the access() method provided by the SierraApiAccessor class.
     */
    private static WeakReference<SierraApi> sierraAPI = null;

    /**
     * Sets the SierraApi instance.
     * The sierraAPI variable holds a weak reference to an instance of the SierraApi interface.
     * This method is used to set the SierraApi instance.
     *
     * @param instance the instance of SierraApi to set
     */
    public static synchronized void setSierraApiInstance(SierraApi instance) {
        sierraAPI = new WeakReference<>(instance);
    }

    /**
     * Returns a weak reference to the SierraApi instance.
     *
     * @return a WeakReference to the SierraApi instance
     * @throws SierraNotLoadedException if SierraApi is not loaded
     */
    public static synchronized WeakReference<SierraApi> access() throws SierraNotLoadedException {
        if (sierraAPI != null) {
            return sierraAPI;
        } else {
            throw new SierraNotLoadedException();
        }
    }
}
