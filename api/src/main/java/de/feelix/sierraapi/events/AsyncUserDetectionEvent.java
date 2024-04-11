package de.feelix.sierraapi.events;

import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.user.impl.SierraUser;
import de.feelix.sierraapi.violation.Violation;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncUserDetectionEvent extends Event {

    private final Violation  violation;
    private final CheckType  checkType;
    private final SierraUser sierraUser;

    private final double violations;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public AsyncUserDetectionEvent(Violation violation, SierraUser sierraUser, CheckType checkType, double violations) {
        super(true);
        this.checkType = checkType;
        this.violation = violation;
        this.violations = violations;
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
