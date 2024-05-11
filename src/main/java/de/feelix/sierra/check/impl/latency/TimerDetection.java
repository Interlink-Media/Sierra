package de.feelix.sierra.check.impl.latency;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.packet.OutgoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.violation.PunishType;

/**
 * TimerDetection is a class that detects violations in player data related to movement frequency.
 *
 * <p>
 * This class extends the SierraDetection class and implements the IngoingProcessor and OutgoingProcessor interfaces.
 * It is used to handle incoming and outgoing packets related to player movement.
 * </p>
 *
 * <p>
 * The TimerDetection class keeps track of the player's balance, which represents the movement frequency.
 * It also tracks the last time the player was flying.
 * </p>
 *
 * <p>
 * If the player's balance exceeds a certain threshold, a violation is generated.
 * </p>
 *
 * <p>
 * The class also has constants that define the maximum balance, balance reset value, and balance decrease value on teleport.
 * </p>
 *
 * @see SierraDetection
 * @see IngoingProcessor
 * @see OutgoingProcessor
 * @since 1.0
 */
@SierraCheckData(checkType = CheckType.LATENCY_ABUSE)
public class TimerDetection extends SierraDetection implements IngoingProcessor, OutgoingProcessor {

    /**
     * The SierraDetection class is used to detect violations in player data.
     *
     * @param playerData The PlayerData object containing the player's data
     */
    public TimerDetection(PlayerData playerData) {
        super(playerData);
    }

    /**
     * Represents the last time the player was flying.
     */
    private long lastFlyingTime = 0L;

    /**
     * Represents the balance of a player.
     * <p>
     * The balance is used to track the movement frequency of the player. It is updated when the player is flying and
     * is decremented when the player is teleported. If the balance exceeds a certain threshold, a violation is created.
     */
    private long balance        = 0L;

    /**
     * Represents the maximum balance of a player.
     * <p>
     * The maximum balance is used to determine the threshold at which a violation is created for movement frequency.
     * If the player's balance exceeds the maximum balance, a violation is generated.
     */
    private static final long maxBal = 0;

    /**
     * The balReset variable represents the balance reset value.
     *
     * <p>
     * This variable is a constant and is used to define the balance value at which a reset should occur.
     * </p>
     *
     * <p>
     * The value of balReset is -50.
     * </p>
     *
     * <p>
     * It is used in the TimerDetection class as one of the fields.
     * </p>
     *
     * @since 1.0
     */
    private static final long balReset = -50;

    /**
     * The balSubOnTp variable represents the balance decrease value when a player is teleported.
     * <p>
     * The balance is used to track the movement frequency of the player. It is updated when the player is flying and
     * is decremented when the player is teleported. If the balance
     * exceeds a certain threshold, a violation is created.
     */
    private static final long balSubOnTp = 50;

    /**
     * The handle method is used to process a PacketReceiveEvent and perform timer cheat detection.
     * If the packet type is WrapperPlayClientPlayerFlying, it checks if timer cheats prevention is enabled.
     * If it is enabled, it calculates the balance based on the time difference since the last flying time,
     * and updates the balance accordingly. If the balance exceeds the maximum balance, it creates a violation
     * and checks if the violation count exceeds the threshold for kicking the player. Finally, it updates the
     * last flying time to the current time.
     *
     * @param event      The PacketReceiveEvent object representing the received packet
     * @param playerData The PlayerData object representing the player's data
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {

            if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-timer-cheats", true)) {
                return;
            }

            if (this.lastFlyingTime != 0L && System.currentTimeMillis() - playerData.getJoinTime() > 1000) {
                final long now = System.currentTimeMillis();
                balance += 50L;
                balance -= now - lastFlyingTime;
                if (balance > maxBal) {
                    createViolation(event, "Movement frequency: bal:~" + balance, PunishType.MITIGATE);
                    if (violations() > 200) {
                        createViolation(event, "Movement frequency: bal:~" + balance, PunishType.KICK);
                    }
                    balance = balReset;

                    // Todo: 1.20 check
                }
            }
            lastFlyingTime = System.currentTimeMillis();
        }
    }

    /**
     * Creates a violation event.
     *
     * @param event             The PacketReceiveEvent object representing the received packet
     * @param debugInformation  Additional information related to the violation
     * @param punishType        The type of punishment for the violation
     */
    private void createViolation(PacketReceiveEvent event, String debugInformation, PunishType punishType) {
        violation(event, ViolationDocument.builder()
            .debugInformation(debugInformation)
            .punishType(punishType)
            .build());
    }

    /**
     * The handle method is used to process a PacketSendEvent and update the player's balance based on the packet type.
     * If the packet type is PacketType.Play.Server.PLAYER_POSITION_AND_LOOK, the balance is decreased by the balSubOnTp value.
     *
     * @param event The PacketSendEvent object representing the event triggered by a packet being sent
     * @param playerData The PlayerData object representing the player's data
     */
    @Override
    public void handle(PacketSendEvent event, PlayerData playerData) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            balance -= balSubOnTp;
        }
    }
}
