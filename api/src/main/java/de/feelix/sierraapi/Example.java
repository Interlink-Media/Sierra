package de.feelix.sierraapi;

import de.feelix.sierraapi.events.impl.AsyncUserDetectionEvent;

/**
 * The Example class represents an example usage of the SierraApi and EventBus.
 * It provides a startFunction method that demonstrates subscribing to an AsyncUserDetectionEvent and accessing the SierraApi functionality.
 */
public class Example {

    /**
     * The sierraApi variable represents an instance of the SierraApi interface,
     * which provides access to the functionality provided by the Sierra plugin.
     */
    private SierraApi sierraApi;

    /**
     * Starts the function.
     *
     * <p>
     * The startFunction method is a callback that is invoked when the Sierra plugin is started and access to SierraApi is not null.
     * It registers a subscription to the AsyncUserDetectionEvent and prints the friendly name of the check type when the event occurs.
     * </p>
     *
     * @see LoaderAPI#registerEnableCallback(EnableCallback)
     * @see SierraApi#eventBus()
     * @see AsyncUserDetectionEvent#getCheckType()
     */
    public void startFunction() {
        // Callback when Sierra is started and access is never null.
        LoaderAPI.registerEnableCallback(() -> {
            sierraApi = SierraApiAccessor.access().get();

            // This cant happen
            if (sierraApi == null) return;

            // Example Event
            sierraApi.eventBus()
                .subscribe(
                    AsyncUserDetectionEvent.class, asyncUserDetectionEvent -> System.out.println(
                        asyncUserDetectionEvent.getCheckType().getFriendlyName()));
        });
    }
}
