package de.feelix.sierra.check.violation;

import lombok.Builder;
import lombok.Data;
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
    private String debugInformation;

    /**
     * The punishType variable represents the type of punishment that can be applied.
     */
    private PunishType punishType;

    /**
     * Returns the debug information of the violation document.
     *
     * @return The debug information of the violation document.
     */
    @Override
    public String debugInformation() {
        return debugInformation == null ? "No debug available" : debugInformation;
    }

    /**
     * Retrieves the punishment type of the violation document.
     *
     * @return The punishment type of the violation document.
     */
    @Override
    public PunishType punishType() {
        return punishType == null ? PunishType.MITIGATE : punishType;
    }
}
