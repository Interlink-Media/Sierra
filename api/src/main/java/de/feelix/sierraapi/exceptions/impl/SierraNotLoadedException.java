package de.feelix.sierraapi.exceptions.impl;

import de.feelix.sierraapi.exceptions.SierraException;

public class SierraNotLoadedException extends SierraException {

    public SierraNotLoadedException() {
        super("Sierra is not yet loaded.");
    }
}
