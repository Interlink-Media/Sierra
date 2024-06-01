package de.feelix.sierra.compatibility.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierra.compatibility.Descriptor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FastAsyncWorldEditDescriptor implements Descriptor {

    @Override
    public String pluginName() {
        return "FastAsyncWorldEdit";
    }

    @Override
    public List<String> knownProblems() {
        return Arrays.asList(
            "We were able to identify a known FAWE bug and have fixed",
            "it by making changes to the configuration.",
            "A restart may be necessary afterward."
        );
    }

    @Override
    public boolean fixProblems() {

        File file = new File("plugins/FastAsyncWorldEdit", "commands.yml");

        if (!file.exists()) {
            return false;
        }

        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);

        List<String> aliases = yamlConfiguration.getStringList("BrushOptionsCommands.targetoffset.aliases");

        if (!aliases.isEmpty()) {
            yamlConfiguration.set("BrushOptionsCommands.targetoffset.aliases", new ArrayList<>());
            yamlConfiguration.set("UtilityCommands./calc.aliases", new ArrayList<>());
        }
        try {
            yamlConfiguration.save(file);
        } catch (IOException exception) {
            Sierra.getPlugin().getLogger().severe("Cant save FAWE file. (" + exception.getMessage() + ")");
            return false;
        }

        return true;
    }

    @Override
    public boolean compatibilityProblematic() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }
}
