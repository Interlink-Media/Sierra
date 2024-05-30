package de.feelix.sierra.manager.init;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import de.feelix.sierra.manager.init.impl.load.InitPacketEvents;
import de.feelix.sierra.manager.init.impl.start.*;
import de.feelix.sierra.manager.init.impl.stop.DisablePacketEvents;

/**
 * The InitManager class represents a manager for initializing various components of the Sierra plugin.
 * It contains three ClassToInstanceMap fields: `initializersOnLoad`, `initializersOnStart`, and `initializersOnStop`,
 * which store objects implementing the Initable interface.
 * The class provides methods for loading, starting, and stopping the initialization process.
 *
 * @see ClassToInstanceMap
 * @see Initable
 */
public class InitManager {

    /**
     * The initializersOnLoad variable is a ClassToInstanceMap that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store initializers that need to be executed on load.
     *
     * @see InitManager
     */
    ClassToInstanceMap<Initable> initializersOnLoad;

    /**
     * The initializersOnStart variable is a ClassToInstanceMap that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store initializers that need to be executed on the start of the application.
     * This variable is populated with instances of classes that implement the Initable interface, such as InitPacketListeners, InitBStats, InitCommand, Ticker, InitEnvironment, and
     *  InitUpdateChecker.
     * The start() method of each Initable object in this map will be called when the start() method of the InitManager class is invoked.
     *
     * @see ClassToInstanceMap
     * @see Initable
     * @see InitManager
     */
    ClassToInstanceMap<Initable> initializersOnStart;

    /**
     * The initializersOnStop variable is a ClassToInstanceMap that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store objects that need to be initialized when the application stops.
     *
     * @see ClassToInstanceMap
     * @see Initable
     * @see InitManager
     */
    ClassToInstanceMap<Initable> initializersOnStop;

    /**
     * The InitManager class represents a manager for initializing various components of the Sierra plugin.
     * It contains three ClassToInstanceMap fields: `initializersOnLoad`, `initializersOnStart`, and `initializersOnStop`,
     * which store objects implementing the Initable interface.
     * The class provides methods for loading, starting, and stopping the initialization process.
     *
     * @see ClassToInstanceMap
     * @see Initable
     */
    public InitManager() {
        initializersOnLoad = new ImmutableClassToInstanceMap.Builder<Initable>()
            .put(InitPacketEvents.class, new InitPacketEvents())
            .build();

        initializersOnStart = new ImmutableClassToInstanceMap.Builder<Initable>()
            .put(InitPacketListeners.class, new InitPacketListeners())
            .put(InitBStats.class, new InitBStats())
            .put(InitCommand.class, new InitCommand())
            .put(Ticker.class, new Ticker())
            .put(InitEnvironment.class, new InitEnvironment())
            .put(InitUpdateChecker.class, new InitUpdateChecker())
            .build();

        initializersOnStop = new ImmutableClassToInstanceMap.Builder<Initable>()
            .put(DisablePacketEvents.class, new DisablePacketEvents())
            .build();
    }

    /**
     * The load method is responsible for initiating the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface stored in the initializersOnLoad map.
     * This method does not return any value.
     *
     * @see Initable
     */
    public void load() {
        for (Initable initable : initializersOnLoad.values()) {
            initable.start();
        }
    }

    /**
     * The start() method is used to initiate the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface stored in the initializersOnStart map.
     */
    public void start() {
        for (Initable initable : initializersOnStart.values()) {
            initable.start();
        }
    }

    /**
     * The stop() method is responsible for stopping the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface and stored in the initializersOnStop map.
     * This method does not return any value.
     *
     * @see Initable
     */
    public void stop() {
        for (Initable initable : initializersOnStop.values()) {
            initable.start();
        }
    }
}
