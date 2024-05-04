package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.SierraAbstractEvent;
import de.feelix.sierraapi.module.SierraModuleDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ModuleDisableEvent extends SierraAbstractEvent {

    private final String                  moduleName;
    private final SierraModuleDescription description;
}
