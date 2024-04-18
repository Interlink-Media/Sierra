package de.feelix.sierra.check.impl.sign;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;

@SierraCheckData(checkType = CheckType.SIGN)
public class SignDetection extends SierraDetection implements IngoingProcessor {

    public SignDetection(PlayerData playerData) {
        super(playerData);
    }

    /**
     * The maximum length of a sign in characters.
     */
    private static final int MAX_SIGN_LENGTH = 45;

    /**
     * Handles the incoming packet received event.
     *
     * @param event The packet receive event.
     * @param playerData The player data associated with the event.
     */
    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        boolean preventSignCrasher = Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("prevent-sign-crasher", true);

        if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN && preventSignCrasher) {

            WrapperPlayClientUpdateSign wrapper = null;

            try {
                wrapper = new WrapperPlayClientUpdateSign(event);
            } catch (Exception exception) {
                violation(event, createViolation("Unable to wrap sign packet", PunishType.BAN));
            }

            if (wrapper == null) return;

            for (String textLine : wrapper.getTextLines()) {

                if (textLine.toLowerCase().contains("run_command")) {
                    violation(event, createViolation("Sign contains json command", PunishType.KICK));
                }

                if (textLine.length() > MAX_SIGN_LENGTH) {
                    violation(event, createViolation("Sign length: " + textLine.length(), PunishType.BAN));
                }
            }
        }
    }

    /**
     * Creates a violation document with the given debug information and punishment type.
     *
     * @param debugInfo The debug information related to the violation.
     * @param type The type of punishment to be applied.
     * @return The created violation document.
     */
    private ViolationDocument createViolation(String debugInfo, PunishType type) {
        return ViolationDocument.builder()
            .debugInformation(debugInfo)
            .punishType(type)
            .build();
    }
}
