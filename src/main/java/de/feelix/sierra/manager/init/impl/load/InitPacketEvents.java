package de.feelix.sierra.manager.init.impl.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.init.Initable;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

/**
 * The InitPacketEvents class implements the Initable interface.
 * It is responsible for initializing the PacketEvents API in the Sierra plugin.
 *
 * @see Initable
 */
public class InitPacketEvents implements Initable {

    /**
     * Starts the PacketEvents API and initializes its settings.
     * It sets the API instance using the SpigotPacketEventsBuilder.build method with the Sierra plugin instance.
     * It configures various settings of the API through the API's getSettings method, such as enabling full stack trace in case of packet exceptions,
     * whether to kick the player on packet exception, whether to re-encode packets by default, whether to check for updates, whether to enable debug mode, and whether to enable b
     * Stats.
     * Finally, it loads the PacketEvents API.
     *
     * @see Sierra#getPlugin()
     * @see Sierra#getSierraConfigEngine()
     * @see de.feelix.sierra.manager.config.SierraConfigEngine#config()
     * @see PacketEvents#getAPI()
     * @see PacketEventsSettings#fullStackTrace(boolean)
     * @see PacketEventsSettings#kickOnPacketException(boolean)
     * @see PacketEventsSettings#reEncodeByDefault(boolean)
     * @see PacketEventsSettings#checkForUpdates(boolean)
     * @see PacketEventsSettings#debug(boolean)
     * @since 1.0.0
     */
    @Override
    public void start() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(Sierra.getPlugin()));
        PacketEvents.getAPI().getSettings()
            .fullStackTrace(true)
            .kickOnPacketException(
                Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("kick-on-packet-exception", true))
            // .reEncodeByDefault(false)
            .checkForUpdates(true)
            .debug(false);
        PacketEvents.getAPI().load();
    }
}
