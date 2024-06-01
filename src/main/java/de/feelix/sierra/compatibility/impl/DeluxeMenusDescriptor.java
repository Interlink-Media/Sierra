package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.compatibility.Descriptor;
import de.feelix.sierra.manager.storage.SierraDataManager;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public class DeluxeMenusDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "DeluxeMenus";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList("Due to an issue with the inventory skulls, we need",
                             " to skip a specific check. This poses security risks!");
    }

    @Override
    public boolean fixProblems() {
        SierraDataManager.skipSkullUUIDCheck = true;
        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }
}
