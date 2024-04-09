package de.feelix.sierraapi.history;

import de.feelix.sierraapi.violation.PunishType;

public interface History {

    String username();

    String description();

    PunishType punishType();

    long timestamp();
}
