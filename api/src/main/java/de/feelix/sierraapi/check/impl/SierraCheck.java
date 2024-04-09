package de.feelix.sierraapi.check.impl;

import de.feelix.sierraapi.check.CheckType;

public interface SierraCheck {

    double violations();

    CheckType checkType();
}
