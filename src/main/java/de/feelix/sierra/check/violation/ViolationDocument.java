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

    private String     debugInformation;
    private PunishType punishType;
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
