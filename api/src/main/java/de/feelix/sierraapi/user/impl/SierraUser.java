package de.feelix.sierraapi.user.impl;

import de.feelix.sierraapi.check.CheckRepository;

import java.util.UUID;

public interface SierraUser {

    String username();

    int entityId();

    UUID uuid();

    long existSince();

    String version();

    boolean kick();

    boolean isExempt();

    boolean setExempt(boolean b);

    boolean isAlerts();

    boolean setAlerts(boolean b);

    CheckRepository checkRepository();
}
