package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.module.SierraModuleDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an event that is fired when a module is enabled.
 */
@AllArgsConstructor
@Getter
public class ModuleEnableEvent extends SierraAbstractEvent {

    /**
     * Represents the name of a module.
     * <p>
     * This variable stores the name of a module. It is a string value that represents the name of the module.
     * <p>
     * Example usage:
     * ModuleEnableEvent event = new ModuleEnableEvent(moduleName, description);
     * String moduleName = event.getModuleName();
     * <p>
     * // Output: "Example Module"
     * System.out.println(moduleName);
     *
     * @see ModuleEnableEvent
     * @see SierraModuleDescription
     */
    private final String                  moduleName;

    /**
     * Represents a module description, containing information about a module.
     */
    private final SierraModuleDescription description;
}
