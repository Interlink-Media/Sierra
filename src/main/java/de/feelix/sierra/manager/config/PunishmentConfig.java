package de.feelix.sierra.manager.config;

import lombok.Getter;

@Getter
public enum PunishmentConfig {

    HARD(true, true),
    LIGHT(true, false);

    private final boolean kick;
    private final boolean ban;

    /**
     * PunishmentConfig represents the configuration for punishments in a system.
     * It defines whether a user should be kicked or banned based on certain criteria.
     */
    PunishmentConfig(boolean kick, boolean ban) {
        this.kick = kick;
        this.ban = ban;
    }
}
