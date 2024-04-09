package de.feelix.sierraapi.violation;

import de.feelix.sierraapi.user.impl.SierraUser;

public interface Violation {

    String debugInformation();

    PunishType punishType();

    SierraUser sierraUser();
}
