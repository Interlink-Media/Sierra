package de.feelix.sierraapi.events;

import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.Violation;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncUserDetectionEvent extends Event {

    private final Violation  violation;
    private final SierraUser sierraUser;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public AsyncUserDetectionEvent(Violation violation, SierraUser sierraUser) {
        super(true);
        this.violation = violation;
        this.sierraUser = sierraUser;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
