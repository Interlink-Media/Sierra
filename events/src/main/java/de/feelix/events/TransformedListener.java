package de.feelix.events;

import de.feelix.events.listeners.ListenerPriority;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class TransformedListener {

    private final Object           listenerClassInstance;
    private final Method           listenerMethod;
    private final ListenerPriority priority;
}
