package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.user.impl.SierraUser;
import lombok.Getter;

/**
 * The UserBrandEvent class represents an event that occurs when a user interacts with a brand.
 * It extends the SierraAbstractEvent class.
 */
@Getter
public class UserBrandEvent extends SierraAbstractEvent {

    /**
     * The brand represents the brand associated with a user in the Sierra API.
     * It is a private final String variable in the UserBrandEvent class.
     * <p>
     * The brand variable stores the brand name as a String.
     * <p>
     * Example usage:
     * UserBrandEvent event = new UserBrandEvent(user, "Vanilla");
     * String brand = event.getBrand();
     * <p>
     * Note: The brand variable cannot be modified once it is set during object creation.
     */
    private final String     brand;

    /**
     * The sierraUser variable represents a user in the Sierra API.
     * It is an instance of the SierraUser interface.
     * <p>
     * The SierraUser interface provides methods to retrieve information about the user such as username, entity ID, UUID, existence timestamp, and version.
     * It also provides methods to perform actions such as kicking the user, checking for exemptions, setting exemptions, checking for alerts, and enabling/disabling alerts.
     * Additional functionality can be accessed through the CheckRepository and TimingHandler interfaces obtained from the SierraUser instance.
     * <p>
     * The sierraUser variable is declared as private final, which means it cannot be reassigned once initialized and its value can only be accessed within the current class.
     * It must be initialized with a valid object implementing the SierraUser interface before it can be used.
     */
    private final SierraUser sierraUser;

    /**
     * Constructs a new UserBrandEvent with the given SierraUser and brand.
     *
     * @param sierraUser the SierraUser associated with the event
     * @param brand      the brand associated with the event
     */
    public UserBrandEvent(SierraUser sierraUser, String brand) {
        this.sierraUser = sierraUser;
        this.brand = brand;
    }
}
