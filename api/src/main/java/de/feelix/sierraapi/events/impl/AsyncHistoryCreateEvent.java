package de.feelix.sierraapi.events.impl;

import de.feelix.sierraapi.events.api.AbstractEvent;
import de.feelix.sierraapi.history.History;
import lombok.Getter;

/**
 * The AsyncHistoryCreateEvent class represents an asynchronous event triggered when a history document is created.
 * It extends the Event class.
 */
@Getter
public class AsyncHistoryCreateEvent extends AbstractEvent {

    /**
     * The history variable represents a user's history of actions or punishments.
     * It contains information such as the username, description, punishment type, and timestamp.
     */
    private final History history;

    /**
     * The AsyncHistoryCreateEvent class represents an asynchronous event triggered when a history document is created.
     * It extends the Event class.
     */
    public AsyncHistoryCreateEvent(History history){
        this.history = history;
    }
}
