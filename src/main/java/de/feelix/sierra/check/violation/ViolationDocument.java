package de.feelix.sierra.check.violation;

import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Builder;
import lombok.Data;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.PunishType;
import de.feelix.sierraapi.violation.Violation;

/**
 * Represents a violation document.
 */
@Builder
@Data
public class ViolationDocument implements Violation {

    /**
     * The debugInformation variable stores additional information related to a violation document.
     * It provides details that can be useful for debugging and troubleshooting purposes.
     */
    private String     debugInformation;

    /**
     * The punishType variable represents the type of punishment that can be applied.
     */
    private PunishType punishType;

    /**
     * The private variable playerData represents the data associated with a player.
     * It is an instance of the PlayerData class and is used to store information about the player, such as their username, entity ID, UUID, version, and various stats related to
     *  gameplay.
     * The playerData variable is used in the ViolationDocument class to associate a specific PlayerData object with a violation.
     * It is also used in the PlayerData class itself to define methods and behaviors related to the player.
     * The playerData variable is of type SierraUser, which is an interface implemented by the PlayerData class.
     *
     * @see ViolationDocument
     * @see PlayerData
     * @see SierraUser
     */
    private PlayerData playerData;

    /**
     * Returns the debug information of the violation document.
     *
     * @return The debug information of the violation document.
     */
    @Override
    public String debugInformation() {
        return debugInformation;
    }

    /**
     * Retrieves the punishment type of the violation document.
     *
     * @return The punishment type of the violation document.
     */
    @Override
    public PunishType punishType() {
        return punishType;
    }

    /**
     * Retrieves the SierraUser associated with the ViolationDocument.
     *
     * @return The SierraUser associated with the ViolationDocument.
     */
    @Override
    public SierraUser sierraUser() {
        return playerData;
    }
}
