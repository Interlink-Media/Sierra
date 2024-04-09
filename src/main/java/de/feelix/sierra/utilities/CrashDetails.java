package de.feelix.sierra.utilities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import de.feelix.sierraapi.violation.PunishType;

@AllArgsConstructor
@Getter
public class CrashDetails {
    private final String     details;
    private final PunishType punishType;
}
