package de.feelix.sierraapi.commands;

import java.util.List;

public interface ISierraCommand {

    void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                 ISierraLabel sierraLabel, ISierraArguments sierraArguments);

    List<String> fromId(int id, String[] args);

    String description();
}
