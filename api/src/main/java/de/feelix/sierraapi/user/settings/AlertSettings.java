package de.feelix.sierraapi.user.settings;

/**
 * This interface represents the settings for an alert.
 */
public interface AlertSettings {

    /**
     * Returns whether the alert is enabled or disabled.
     *
     * @return true if the alert is enabled, false if it is disabled
     */
    boolean enabled();

    /**
     * Toggles the enabled state of the alert.
     *
     * @param enabled true to enable the alert, false to disable it
     */
    void toggle(boolean enabled);
}
