package de.feelix.sierra.manager.init;

/**
 * The Initable interface represents an object that can be initialized.
 * Classes that implement this interface should provide an implementation for the start() method
 * to perform initialization tasks.
 */
public interface Initable {

    /**
     * The start() method is used to initiate the initialization process of an object implementing the Initable interface.
     * It performs specific tasks to set up and configure the object.
     * Classes that implement the Initable interface should provide an implementation for this method to perform initialization tasks.
     */
    void start();
}
