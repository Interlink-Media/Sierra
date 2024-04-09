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

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN && Sierra.getPlugin()
            .getSierraConfigEngine()
            .config()
            .getBoolean("prevent-sign-crasher", true)) {

            WrapperPlayClientUpdateSign wrapper = null;

            try {
                wrapper = new WrapperPlayClientUpdateSign(event);
            } catch (Exception exception) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Unable to wrap sign packet")
                    .punishType(PunishType.BAN)
                    .build());
            }

            if (wrapper == null) return;

            for (String textLine : wrapper.getTextLines()) {

                int maxSignLength = 45;
                if (textLine.length() > maxSignLength) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation("Sign length: " + textLine.length())
                        .punishType(PunishType.BAN)
                        .build());
                }
            }
        }
    }
}
