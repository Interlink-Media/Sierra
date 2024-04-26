package de.feelix.sierraapi.module;

import java.io.File;
import java.util.Map;

public interface ModuleGateway {

    File moduleDirectory();

    Map<String, Module> modules();

    boolean moduleActivated(String moduleName);

    boolean deactivateModule(String moduleName);

    boolean activateModule(String moduleName);
}
