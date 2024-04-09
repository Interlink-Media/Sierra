package de.feelix.sierra.manager.config;

import lombok.Getter;

@Getter
public enum PunishmentConfig {

    HARD(true, true),
    LIGHT(true, false);

    private final boolean kick;
    private final boolean ban;

    PunishmentConfig(boolean kick, boolean ban) {
        this.kick = kick;
        this.ban = ban;
    }
}
