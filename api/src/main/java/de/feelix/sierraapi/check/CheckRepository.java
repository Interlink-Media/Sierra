package de.feelix.sierraapi.check;

import de.feelix.sierraapi.check.impl.SierraCheck;

import java.util.List;

public interface CheckRepository {

    List<SierraCheck> availableChecks();
}
