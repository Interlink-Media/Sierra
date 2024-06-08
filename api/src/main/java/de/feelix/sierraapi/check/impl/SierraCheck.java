package de.feelix.sierraapi.check.impl;

import de.feelix.sierraapi.check.CheckType;

/**
 * SierraCheck is an interface representing a check for violations in player data.
 */
public interface SierraCheck {

    /**
     * Returns the number of violations found by this check.
     *
     * @return the number of violations
     */
    double violations();

    /**
     * Sets the number of violations found by this check.
     *
     * @since 1.0
     */
    void setViolations(double violations);

    /**
     * Retrieves the CheckType of the current instance.
     *
     * @return The CheckType of the instance.
     */
    CheckType checkType();
}
