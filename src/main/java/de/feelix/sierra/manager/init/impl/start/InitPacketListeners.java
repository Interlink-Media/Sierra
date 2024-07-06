package de.feelix.sierra.manager.init.impl.start;

import com.github.retrooper.packetevents.PacketEvents;
import de.feelix.sierra.listener.PacketLoggerListener;
import de.feelix.sierra.listener.PacketReceiveListener;
import de.feelix.sierra.listener.PacketSendListener;
import de.feelix.sierra.manager.init.Initable;

/**
 * The InitPacketListeners class initializes packet listeners upon the start of the application.
 * The start() method registers a PacketListener and initializes the PacketEvents API.
 */
public class InitPacketListeners implements Initable {

    /**
     * The start() method initializes the packet listeners upon the start of the application.
     * It registers a PacketListener and initializes the PacketEvents API.
     */
    @Override
    public void start() {
        PacketEvents.getAPI()
            .getEventManager()
            .registerListeners(new PacketReceiveListener(), new PacketSendListener(), new PacketLoggerListener());
        PacketEvents.getAPI().init();
    }
}
