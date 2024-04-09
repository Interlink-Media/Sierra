package de.feelix.sierraapi.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncSierraCommandEvent extends Event {

    private final String command;
    private final String label;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public AsyncSierraCommandEvent(String command, String label) {
        super(true);
        this.command = command;
        this.label = label;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
