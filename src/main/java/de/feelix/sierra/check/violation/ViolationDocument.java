package de.feelix.sierra.check.violation;

import de.feelix.sierra.manager.storage.PlayerData;
import lombok.Builder;
import lombok.Data;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.PunishType;
import de.feelix.sierraapi.violation.Violation;

@Builder
@Data
public class ViolationDocument implements Violation {

    private String     debugInformation;
    private PunishType punishType;
    private PlayerData playerData;

    @Override
    public String debugInformation() {
        return debugInformation;
    }

    @Override
    public PunishType punishType() {
        return punishType;
    }

    @Override
    public SierraUser sierraUser() {
        return playerData;
    }
}
