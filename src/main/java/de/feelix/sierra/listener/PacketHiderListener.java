package de.feelix.sierra.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;

import java.util.Arrays;
import java.util.List;

public class PacketHiderListener extends PacketListenerAbstract {

    public PacketHiderListener() {
        super(PacketListenerPriority.LOWEST);
    }


    List<String> disabledCommands = Arrays.asList(
        "/pl", "/bukkit:pl", "/plugins", "/bukkit:plugins", "/ver", "/bukkit:ver", "/version", "/bukkit:version", "/?",
        "/bukkit:?", "/help", "/bukkit:help", "/about", "/bukkit:about", "/icanhasbukkit", "/bukkit:icanhasbukkit"
    );

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        final String UNKNOWN_COMMAND = "Unknown command. Type \"/help\" for help.";

        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);

            for (String disabledCommand : disabledCommands) {
                if (wrapper.getMessage().equalsIgnoreCase(disabledCommand)) {
                    event.getUser().sendMessage(UNKNOWN_COMMAND);
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {

            WrapperPlayClientChatCommandUnsigned wrapper = new WrapperPlayClientChatCommandUnsigned(event);

            for (String disabledCommand : disabledCommands) {
                if (wrapper.getCommand().equalsIgnoreCase(disabledCommand)) {
                    event.getUser().sendMessage(UNKNOWN_COMMAND);
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            WrapperPlayClientChatCommand wrapper = new WrapperPlayClientChatCommand(event);

            for (String disabledCommand : disabledCommands) {
                if (wrapper.getCommand().equalsIgnoreCase(disabledCommand)) {
                    event.getUser().sendMessage(UNKNOWN_COMMAND);
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.TAB_COMPLETE) {
            WrapperPlayServerTabComplete wrapper = new WrapperPlayServerTabComplete(event);

            List<WrapperPlayServerTabComplete.CommandMatch> commandMatches = wrapper.getCommandMatches();

            commandMatches.removeIf(commandMatch -> commandMatch.getText().contains("sierra"));
            wrapper.setCommandMatches(commandMatches);
            event.setCancelled(true);

            event.getUser().sendPacket(wrapper);
        }
    }
}
