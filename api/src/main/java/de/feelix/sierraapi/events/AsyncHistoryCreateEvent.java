package de.feelix.sierraapi.events;

import de.feelix.sierraapi.history.History;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncHistoryCreateEvent extends Event {

    private final History history;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public AsyncHistoryCreateEvent(History history){
        super(true);
        this.history = history;
    }
    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
