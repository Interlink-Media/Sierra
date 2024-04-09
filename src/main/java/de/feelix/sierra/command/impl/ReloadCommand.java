package de.feelix.sierra.command.impl;

import de.feelix.sierra.Sierra;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements ISierraCommand {

    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        Sierra.getPlugin().getSierraConfigEngine().invalidateCache();
        Sierra.getPlugin().setPrefix();
        sierraSender.getSender().sendMessage(Sierra.PREFIX + " §fConfiguration reloaded §asuccessfully");
    }

    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }

    @Override
    public String description() {
        return "Reload the configuration";
    }
}
