package de.feelix.sierra.manager.storage.alert;

import de.feelix.sierraapi.user.settings.AlertSettings;

/**
 * {@code AbstractAlertSetting} is a class that implements the {@code AlertSettings} interface.
 * It provides a basic implementation of the methods in the {@code AlertSettings} interface.
 *
 * @see AlertSettings
 */
public class AbstractAlertSetting implements AlertSettings {

    /**
     * Represents the enabled state of an alert.
     *
     * <p>
     * The enabled state determines whether the alert is enabled or disabled.
     * </p>
     *
     * @see AlertSettings
     * @see AbstractAlertSetting
     */
    private boolean enabled = false;

    /**
     * Returns whether the alert is enabled or disabled.
     *
     * @return true if the alert is enabled, false if it is disabled
     */
    @Override
    public boolean enabled() {
        return enabled;
    }

    /**
     * Toggles the enabled state of the alert.
     *
     * @param enabled true to enable the alert, false to disable it
     */
    @Override
    public void toggle(boolean enabled) {
        this.enabled = enabled;
    }
}
