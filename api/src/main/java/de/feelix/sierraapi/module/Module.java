package de.feelix.sierraapi.module;

import java.io.File;

public interface Module {

    String moduleName();

    File dataFolder();

    boolean enabled();

    ModuleDescription moduleDescription();

    String fullModulePath();
}
