package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.compatibility.Descriptor;
import de.feelix.sierra.manager.storage.SierraDataManager;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public class InsaneAnnouncerDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "InsaneAnnouncer";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList(
            "Due to an error in an inventory, we have",
            "to skip the anvil check because the",
            "inventory contains a faulty anvil."
        );
    }

    @Override
    public boolean fixProblems() {
        SierraDataManager.skipAnvilCheck = true;
        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }
}
