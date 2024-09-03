package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.compatibility.Descriptor;
import de.feelix.sierra.manager.storage.SierraDataManager;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public class BetterRTPDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "BetterRTP";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList("This plugin is teleporting players",
                             "to a exact 0.0 coordinate");
    }

    @Override
    public boolean fixProblems() {
        SierraDataManager.skipDeltaPositionCheck = true;
        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }
}
