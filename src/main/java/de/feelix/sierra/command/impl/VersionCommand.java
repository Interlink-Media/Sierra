package de.feelix.sierra.command.impl;


import de.feelix.sierra.command.CommandHelper;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.List;

public class VersionCommand implements ISierraCommand {

    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        CommandHelper.sendVersionOutput(sierraSender);
    }

    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("version");
        }
        return Collections.emptyList();
    }

    @Override
    public String description() {
        return "Show version info";
    }
}
