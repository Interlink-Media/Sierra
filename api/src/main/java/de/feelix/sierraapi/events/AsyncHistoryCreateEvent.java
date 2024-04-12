package de.feelix.sierraapi.events;

import de.feelix.sierraapi.history.History;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * The AsyncHistoryCreateEvent class represents an asynchronous event triggered when a history document is created.
 * It extends the Event class.
 */
@Getter
public class AsyncHistoryCreateEvent extends Event {

    /**
     * The history variable represents a user's history of actions or punishments.
     * It contains information such as the username, description, punishment type, and timestamp.
     */
    private final History history;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    /**
     * The AsyncHistoryCreateEvent class represents an asynchronous event triggered when a history document is created.
     * It extends the Event class.
     */
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
