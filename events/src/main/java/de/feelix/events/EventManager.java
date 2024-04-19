package de.feelix.events;

import de.feelix.events.exceptions.EventDispatchException;
import de.feelix.events.exceptions.EventRegisterException;
import de.feelix.events.listeners.EventListener;
import de.feelix.events.listeners.Eventable;
import de.feelix.events.listeners.ListenerPriority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The EventManager class is responsible for registering, unregistering, and dispatching events to event listeners.
 * Events are represented by subclasses of AbstractEvent, and event listeners are represented by classes that
 * implement the Listener interface.
 * To register event listeners, use the registerListeners() method, passing an instance of the listener class.
 * Event listeners are methods annotated with the @EventHandler annotation, and must accept a single parameter of a
 * subclass of AbstractEvent.
 * <p>
 * Example usage:
 * EventManager.registerListeners(new MyListener());
 * EventManager.callEvent(new MyEvent());
 * <p>
 * To unregister event listeners, use the unregisterListeners() method, passing the instance of the listener class
 * that was previously registered.
 * Alternatively, you can unregister all listeners for a specific event class using the unregisterListenersOfEvent()
 * method.
 * <p>
 * Example usage:
 * EventManager.unregisterListeners(new MyListener());
 * EventManager.unregisterListenersOfEvent(MyEvent.class);
 * <p>
 * EventManager uses a priority system for dispatching events to listeners. The priority determines the order in
 * which listeners are
 */
@SuppressWarnings("unused")
public final class EventManager {

    /**
     * Holds the registered event listeners for each event type.
     */
    private static final HashMap<Class<? extends AbstractEvent>,
        CopyOnWriteArrayList<TransformedListener>> registeredListeners = new HashMap<>();

    /**
     * Registers event listeners by validating and adding them to the manager's list of registered listeners.
     *
     * @param listenerClassInstance an instance of the listener class to register
     * @throws EventRegisterException if the listener class does not implement the Listener interface
     */
    public static void registerListeners(final Object listenerClassInstance) {
        Class<?> aClass = listenerClassInstance.getClass();
        if (!EventListener.class.isAssignableFrom(aClass)) {
            throw new EventRegisterException("Illegal class: " + aClass.getSimpleName() +
                                             ": Class is not implements Listener.class (required: 1)");
        }
        for (Method method : aClass.getMethods()) {
            if (method.isAnnotationPresent(Eventable.class)) {
                validateListenerMethod(method, listenerClassInstance);
            }
        }
    }

    /**
     * Validates the provided listener method and adds it to the manager's list of registered listeners.
     *
     * @param method                the listener method to validate and register
     * @param listenerClassInstance an instance of the listener class
     * @throws EventRegisterException if the listener method is invalid or cannot be registered
     */
    private static void validateListenerMethod(Method method, Object listenerClassInstance) {
        if (method.getParameterCount() != 1) {
            throw new EventRegisterException("Illegal event handler: " + method.getName() +
                                             ": Wrong number of arguments (required: 1)");
        }
        if (!AbstractEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new EventRegisterException(
                "Illegal event handler: " + method.getName() + ": Argument must extend " +
                AbstractEvent.class.getName());
        }
        @SuppressWarnings("unchecked") Class<? extends AbstractEvent> eventType =
            (Class<? extends AbstractEvent>) method.getParameterTypes()[0];
        ListenerPriority    priority       = method.getAnnotation(Eventable.class).priority();
        TransformedListener cashedListener = new TransformedListener(listenerClassInstance, method, priority);
        addListener(eventType, cashedListener);
    }

    /**
     * Adds a listener to the manager's list of registered listeners for a specific event type.
     *
     * @param eventType      the class object representing the event type
     * @param cashedListener the TransformedListener object representing the listener to be added
     */
    private static void addListener(final Class<? extends AbstractEvent> eventType,
                                    final TransformedListener cashedListener) {

        if (!registeredListeners.containsKey(eventType)) {
            registeredListeners.put(eventType, new CopyOnWriteArrayList<>());
        }

        registeredListeners.get(eventType).add(cashedListener);
    }

    /**
     * Unregisters all listeners of a specific listener class instance from the event manager.
     *
     * @param listenerClassInstance the instance of the listener class to unregister
     */
    public static void unregisterListeners(final Object listenerClassInstance) {
        for (CopyOnWriteArrayList<TransformedListener> cachedListenerList : registeredListeners.values()) {
            cachedListenerList.removeIf(
                transformedListener -> transformedListener.getListenerClassInstance() == listenerClassInstance);
        }
    }

    /**
     * Unregisters all listeners of a specific event class from the event manager.
     *
     * @param eventClass the class object representing the event class
     */
    public static void unregisterListenersOfEvent(final Class<? extends AbstractEvent> eventClass) {
        registeredListeners.get(eventClass).clear();
    }

    /**
     * Calls the given event by dispatching it to all registered listeners in the manager's list of listeners.
     * The event is dispatched according to the listener priority, with the highest priority listeners called first.
     *
     * @param event the event to be called
     */
    public static void callEvent(final AbstractEvent event) {
        for (ListenerPriority value : ListenerPriority.values()) {
            dispatchEvent(event, value);
        }
    }

    /**
     * Dispatches an event to the registered listeners with the specified priority.
     *
     * @param event    the event to dispatch
     * @param priority the priority of the listeners to dispatch the event to
     * @throws EventDispatchException if the event cannot be dispatched
     */
    private static void dispatchEvent(final AbstractEvent event, final ListenerPriority priority) {
        CopyOnWriteArrayList<TransformedListener> cashedListeners = registeredListeners.get(event.getClass());
        if (cashedListeners != null) {
            for (TransformedListener cashedListener : cashedListeners) {
                if (isPriorityMatched(cashedListener, priority)) {
                    invokeListenerMethod(cashedListener, event);
                }
            }
        }
    }

    /**
     * Checks whether the priority of the cashedListener is same with given priority
     *
     * @param cashedListener the cashedListener to check the priority
     * @param priority       the comparing priority
     * @return true if the cashedListener's priority is same as the provided priority
     */
    private static boolean isPriorityMatched(TransformedListener cashedListener, ListenerPriority priority) {
        return cashedListener.getPriority() == priority;
    }

    /**
     * Invokes the listenerMethod on the given cashedListener's listenerClassInstance with the provided event
     *
     * @param cashedListener the transformed listener with the method to be invoked
     * @param event          the event passed as an argument to the listener method
     * @throws EventDispatchException if the event cannot be dispatched
     */
    private static void invokeListenerMethod(TransformedListener cashedListener, AbstractEvent event) {
        try {
            Method listenerMethod = cashedListener.getListenerMethod();
            listenerMethod.setAccessible(true);
            listenerMethod.invoke(cashedListener.getListenerClassInstance(), event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EventDispatchException("Cant dispatch given event, cause: " + e.getMessage());
        }
    }
}
