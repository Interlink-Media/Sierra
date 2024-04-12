package de.feelix.sierra.command.api;

import de.feelix.sierraapi.commands.ISierraLabel;

/**
 * The SierraLabel class represents a label used in the Sierra plugin. It implements the ISierraLabel interface
 * and provides the implementation for the getLabel() method.
 */
public class SierraLabel implements ISierraLabel {

    /**
     * Represents a label used in the Sierra plugin.
     * This class implements the ISierraLabel interface and provides the implementation for the getLabel() method.
     */
    private final String label;

    /**
     * Initializes a new instance of the SierraLabel class with the specified label.
     *
     * @param label the label to be assigned to the SierraLabel instance
     */
    public SierraLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the label of the SierraLabel instance.
     *
     * @return the label of the SierraLabel instance
     */
    @Override
    public String getLabel() {
        return label;
    }
}
