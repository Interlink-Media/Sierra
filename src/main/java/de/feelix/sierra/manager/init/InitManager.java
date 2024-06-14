package de.feelix.sierra.manager.init;

import java.util.ArrayList;
import java.util.List;
import de.feelix.sierra.manager.init.impl.load.InitPacketEvents;
import de.feelix.sierra.manager.init.impl.start.*;
import de.feelix.sierra.manager.init.impl.stop.DisablePacketEvents;

/**
 * The InitManager class represents a manager for initializing various components of the Sierra plugin.
 * It contains three List fields: `initializersOnLoad`, `initializersOnStart`, and `initializersOnStop`,
 * which store objects implementing the Initable interface.
 * The class provides methods for loading, starting, and stopping the initialization process.
 *
 * @see Initable
 */
public class InitManager {

    /**
     * The initializersOnLoad variable is a List that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store initializers that need to be executed on load.
     *
     * @see InitManager
     */
    private final List<Initable> initializersOnLoad = new ArrayList<>();

    /**
     * The initializersOnStart variable is a List that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store initializers that need to be executed on the start of the application.
     * This variable is populated with instances of classes that implement the Initable interface, such as InitPacketListeners, InitBStats, InitCommand, Ticker, InitEnvironment, and
     *  InitUpdateChecker.
     * The start() method of each Initable object in this list will be called when the start() method of the InitManager class is invoked.
     *
     * @see Initable
     * @see InitManager
     */
    private final List<Initable> initializersOnStart = new ArrayList<>();

    /**
     * The initializersOnStop variable is a List that stores objects implementing the Initable interface.
     * It is used in the InitManager class to store objects that need to be initialized when the application stops.
     *
     * @see Initable
     * @see InitManager
     */
    private final List<Initable> initializersOnStop = new ArrayList<>();

    /**
     * The InitManager class represents a manager for initializing various components of the Sierra plugin.
     * It contains three List fields: `initializersOnLoad`, `initializersOnStart`, and `initializersOnStop`,
     * which store objects implementing the Initable interface.
     * The class provides methods for loading, starting, and stopping the initialization process.
     *
     * @see Initable
     */
    public InitManager() {
        // On load
        initializersOnLoad.add(new InitPacketEvents());

        // On start
        initializersOnStart.add(new InitPacketListeners());
        initializersOnStart.add(new InitBStats());
        initializersOnStart.add(new InitCommand());
        initializersOnStart.add(new Ticker());
        initializersOnStart.add(new InitEnvironment());
        initializersOnStart.add(new InitUpdateChecker());

        // On stop
        initializersOnStop.add(new DisablePacketEvents());
    }

    /**
     * The load method is responsible for initiating the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface stored in the initializersOnLoad list.
     * This method does not return any value.
     *
     * @see Initable
     */
    public void load() {
        for (Initable initable : initializersOnLoad) {
            initable.start();
        }
    }

    /**
     * The start() method is used to initiate the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface stored in the initializersOnStart list.
     */
    public void start() {
        for (Initable initable : initializersOnStart) {
            initable.start();
        }
    }

    /**
     * The stop() method is responsible for stopping the initialization process of various components of the Sierra plugin.
     * It calls the start() method of each object implementing the Initable interface and stored in the initializersOnStop list.
     * This method does not return any value.
     *
     * @see Initable
     */
    public void stop() {
        for (Initable initable : initializersOnStop) {
            initable.start();
        }
    }
}
