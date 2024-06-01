package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.compatibility.Descriptor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class ProtocolLibDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "ProtocolLib";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList("Sierra is incompatible with the 4.0 series of ProtocolLib.",
                             "Please update to the latest version of ProtocolLib.",
                             "https://www.spigotmc.org/resources/protocollib.1997/",
                             "We need to disable Sierra, im sorry.");
    }

    @Override
    public boolean fixProblems() {
        Bukkit.getPluginManager().disablePlugin(Sierra.getPlugin());
        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName());

        return plugin != null && plugin.getDescription().getVersion().startsWith("4");
    }
}
