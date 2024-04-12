package de.feelix.sierra.utilities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import de.feelix.sierraapi.violation.PunishType;

/**
 * A class representing crash details of a player.
 */
@AllArgsConstructor
@Getter
public class CrashDetails {
    /**
     * Represents the details of a crash.
     *
     * <p>
     * This variable is a {@code private final} instance of type {@code String} in the {@code CrashDetails} class.
     * It stores the details of a crash that occurred.
     * </p>
     *
     * <p>
     * Example usage:
     * </p>
     *
     * <pre>
     * CrashDetails crash = new CrashDetails("Crash details");
     * String details = crash.getDetails();
     * System.out.println("Crash details: " + details);
     * </pre>
     *
     * @see CrashDetails
     */
    private final String     details;
    /**
     * Represents the type of punishment for a player.
     *
     * <p>
     * The PunishType enum defines three types of punishment: {@code MITIGATE}, {@code KICK}, and {@code BAN}.
     * Each punishment type has a friendly message associated with it.
     * </p>
     *
     * <p>
     * Example usage:
     * </p>
     *
     * <pre>
     * PunishType punishType = PunishType.BAN;
     * String friendlyMessage = punishType.friendlyMessage();
     * System.out.println("You have been " + friendlyMessage + " hacking.");
     * </pre>
     *
     * @see CrashDetails
     */
    private final PunishType punishType;
}
