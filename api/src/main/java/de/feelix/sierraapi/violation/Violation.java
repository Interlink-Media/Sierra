package de.feelix.sierraapi.violation;

/**
 * The Violation interface represents a violation that has occurred.
 * It provides information about the violation, such as debug information, punishment type, and the associated user.
 */
public interface Violation {

    /**
     * Returns the debug information associated with the violation.
     *
     * @return The debug information associated with the violation.
     */
    String debugInformation();

    String description();

    /**
     * Retrieves the punishment type of the violation document.
     *
     * @return The punishment type of the violation document.
     */
    MitigationStrategy mitigationStrategy();
}
