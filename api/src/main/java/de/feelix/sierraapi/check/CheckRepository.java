package de.feelix.sierraapi.check;

import de.feelix.sierraapi.check.impl.SierraCheck;

import java.util.List;

/**
 * The CheckRepository interface defines a contract for classes that manage packet checks for a player.
 */
public interface CheckRepository {

    /**
     * Retrieves a list of available checks.
     *
     * @return a list of {@link SierraCheck} objects representing the available checks
     */
    List<SierraCheck> availableChecks();
}
