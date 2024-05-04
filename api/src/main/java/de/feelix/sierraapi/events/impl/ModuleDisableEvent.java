package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.module.SierraModuleDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an event that is triggered when a module is disabled.
 */
@AllArgsConstructor
@Getter
public class ModuleDisableEvent extends SierraAbstractEvent {

    /**
     * Represents the name of a module.
     * <p>
     * The moduleName variable is a private final String that stores the name of a module.
     * The module name is used to identify a specific module and is typically unique.
     * <p>
     * Example usage:
     * <pre>{@code
     * ModuleDisableEvent event = new ModuleDisableEvent("moduleName", description);
     * String moduleName = event.getModuleName();
     *
     * // Output: "moduleName"
     * System.out.println(moduleName);
     * }</pre>
     * @see ModuleDisableEvent
     */
    private final String                  moduleName;

    /**
     * The module description representing information about a module.
     *
     * @see SierraModuleDescription
     */
    private final SierraModuleDescription description;
}
