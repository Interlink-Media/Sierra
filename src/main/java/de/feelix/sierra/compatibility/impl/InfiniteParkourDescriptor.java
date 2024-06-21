package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.compatibility.Descriptor;
import de.feelix.sierra.manager.storage.SierraDataManager;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public class InfiniteParkourDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "InfiniteParkour";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList("Due to faulty NBT data in the plugin",
                             "we are forced to disable the custom model check");
    }

    @Override
    public boolean fixProblems() {
        SierraDataManager.skipModelCheck = true;
        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        boolean containsInfiniteParkour = Bukkit.getPluginManager().getPlugin("IP") != null;
        boolean containsInfiniteParkourPlus = Bukkit.getPluginManager().getPlugin("IPPlus") != null;
        return containsInfiniteParkour || containsInfiniteParkourPlus;
    }
}
