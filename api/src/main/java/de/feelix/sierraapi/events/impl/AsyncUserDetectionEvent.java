package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.Violation;
import lombok.Getter;

/**
 * The AsyncUserDetectionEvent class represents an asynchronous event that is triggered when a violation occurs.
 * It provides information about the violation, check type, and associated user.
 */
@Getter
public class AsyncUserDetectionEvent extends SierraAbstractEvent {

    /**
     * The `violation` variable represents a violation that has occurred.
     * It provides information about the violation, such as debug information, punishment type, and the associated user.
     *
     * @see Violation
     */
    private final Violation violation;

    /**
     * The CheckType enum represents the types of checks that can trigger a violation.
     */
    private final CheckType checkType;

    /**
     * The sierraUser variable represents a user in the Sierra API. It is an instance of the SierraUser interface.
     * SierraUser provides methods to retrieve information about the user, such as username, entity ID, UUID,
     * existence since timestamp, version, and various actions and settings related
     * to the user.
     * The SierraUser interface also provides access to the CheckRepository, which is a repository of checks in the
     * Sierra API.
     * The SierraUser interface is implemented by various classes, including the AsyncUserDetectionEvent class.
     * Within the AsyncUserDetectionEvent class, the sierraUser variable represents the SierraUser associated with a
     * violation.
     * The sierraUser variable is final, indicating that it cannot be reassigned once initialized.
     */
    private final SierraUser sierraUser;

    /**
     * The violations variable represents the number of violations for the associated user.
     *
     * @see AsyncUserDetectionEvent
     */
    private final double violations;

    /**
     * The AsyncUserDetectionEvent class represents an asynchronous event that is triggered when a violation occurs.
     * It provides information about the violation, check type, and associated user.
     *
     * @param violation  The Violation object representing the violation that occurred.
     * @param sierraUser The SierraUser object representing the associated user.
     * @param checkType  The CheckType object representing the type of check that triggered the violation.
     * @param violations The number of violations for the associated user.
     */
    public AsyncUserDetectionEvent(Violation violation, SierraUser sierraUser, CheckType checkType, double violations) {
        this.checkType = checkType;
        this.violation = violation;
        this.violations = violations;
        this.sierraUser = sierraUser;
    }
}
