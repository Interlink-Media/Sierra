package de.feelix.sierra.manager.init.impl.start;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.command.SierraCommand;
import de.feelix.sierra.manager.init.Initable;

import java.util.Objects;

/**
 * The InitCommand class represents a command that initializes various components of the Sierra plugin.
 * It implements the Initable interface, which defines a start() method to start the initialization process.
 */
public class InitCommand implements Initable {

    /**
     * Starts the initialization process for the Sierra plugin by setting the executor for the "/sierra" command.
     * The executor is an instance of the SierraCommand class.
     * If the executor does not exist, a NullPointerException will be thrown.
     */
    @Override
    public void start() {
        Objects.requireNonNull(Sierra.getPlugin().getCommand("sierra")).setExecutor(new SierraCommand());
    }
}
