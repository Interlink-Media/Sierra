package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.ISierraLabel;

public class SierraLabel implements ISierraLabel {

    private final String label;

    public SierraLabel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
