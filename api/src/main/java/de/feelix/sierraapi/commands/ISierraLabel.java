package de.feelix.sierraapi.commands;

/**
 * The ISierraLabel interface represents a label used in the Sierra plugin.
 * It defines the method that needs to be implemented to get the label of the initial symbol.
 */
public interface ISierraLabel {

    /**
     * Returns the label of the initial symbol.
     *
     * @return the label of the initial symbol
     */
    String getLabel();
}
