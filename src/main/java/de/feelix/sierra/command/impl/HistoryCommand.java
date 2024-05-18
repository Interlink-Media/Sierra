package de.feelix.sierra.command.impl;


import com.github.retrooper.packetevents.protocol.player.User;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.history.HistoryDocument;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.pagination.Pagination;
import de.feelix.sierraapi.commands.*;
import de.feelix.sierraapi.history.History;
import de.feelix.sierraapi.user.impl.SierraUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The HistoryCommand class represents a command that retrieves and displays the history of a player's punishments.
 * It implements the ISierraCommand interface.
 */
public class HistoryCommand implements ISierraCommand {

    /**
     * This method processes the command by validating the arguments, setting up pagination,
     * sending the appropriate help syntax if arguments are invalid, sending the pagination message
     * to the sender, and sending the history messages.
     *
     * @param user            The User object representing the user.
     * @param sierraUser      The SierraUser object representing the user in the Sierra API.
     * @param abstractCommand The IBukkitAbstractCommand object representing the wrapped Bukkit Command.
     * @param sierraLabel     The ISierraLabel object representing the label of the initial symbol.
     * @param sierraArguments The ISierraArguments object representing the arguments passed with the command.
     */
    @Override
    public void process(User user, SierraUser sierraUser, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel,
                        ISierraArguments sierraArguments) {

        if (!validateArguments(sierraArguments)) {
            sendHelpSyntax(user);
            return;
        }

        Pagination<History> pagination = setupPagination();

        int page = correctPage(FormatUtils.toInt(sierraArguments.getArguments().get(1)), pagination.totalPages());
        sendMessage(user, page, pagination);

        List<History> historyDocumentList = pagination.itemsForPage(page);

        if (historyDocumentList.isEmpty()) {
            user.sendMessage(Sierra.PREFIX + " §cNo history available");
            return;
        }

        sendHistoryMessages(user, historyDocumentList);
    }

    /**
     * Sends the history messages to the specified user.
     *
     * @param user               The User object representing the user.
     * @param historyDocumentList The list of History objects representing the history documents.
     */
    private void sendHistoryMessages(User user, List<History> historyDocumentList) {
        for (History historyDocument : historyDocumentList) {
            user.sendMessage(createHistoryMessage((HistoryDocument) historyDocument));
        }
    }

    /**
     * Validates the arguments passed with a command.
     *
     * @param sierraArguments The ISierraArguments object representing the arguments.
     * @return true if the number of arguments is greater than 1, false otherwise.
     */
    private boolean validateArguments(ISierraArguments sierraArguments) {
        return sierraArguments.getArguments().size() > 1;
    }

    /**
     * Sets up pagination for the history documents.
     *
     * @return A Pagination object containing the sorted history documents.
     */
    private Pagination<History> setupPagination() {
        List<History> list = new ArrayList<>(Sierra.getPlugin()
                                                 .getSierraDataManager()
                                                 .getHistories());
        list.sort(Comparator.comparing(History::timestamp).reversed());
        return new Pagination<>(list, 10);
    }

    /**
     * Corrects the page number by ensuring it is within the valid range of pages.
     *
     * @param page       The current page number.
     * @param totalPages The total number of pages.
     * @return The corrected page number.
     */
    private int correctPage(int page, int totalPages) {
        if (page > totalPages || page < 0) {
            return 1;
        }
        return page;
    }

    /**
     * Sends a formatted history message to the given User.
     *
     * @param user        The User to send the message to.
     * @param page        The current page number.
     * @param pagination  The Pagination object containing the history items.
     */
    private void sendMessage(User user, int page, Pagination<History> pagination) {
        int    totalHistory = pagination.getItems().size();
        String unformulated = "%s §fShowing entries: §7(page §c%s §7of §c%d §7- §c%d §7entries)";
        user.sendMessage(String.format(unformulated, Sierra.PREFIX, page, pagination.totalPages(), totalHistory));
    }

    /**
     * Creates a formatted history message based on the provided HistoryDocument.
     *
     * @param historyDocument The HistoryDocument containing the history information.
     * @return The formatted history message.
     */
    private String createHistoryMessage(HistoryDocument historyDocument) {
        return FormatUtils.formatColor(String.format(
            "§7[%s] §c%s §7(%dms) -> §c%s §7(%s)",
            historyDocument.formatTimestamp(),
            historyDocument.username(),
            historyDocument.ping(),
            historyDocument.punishType().historyMessage(),
            historyDocument.shortenDescription()
        ));
    }

    /**
     * Sends the help syntax message to the user.
     *
     * @param user The User object representing the user.
     */
    private void sendHelpSyntax(User user) {
        user.sendMessage(Sierra.PREFIX + " §cInvalid usage, try /sierra history <page>");
    }

    /**
     * Converts an ID and arguments into a list of strings.
     *
     * @param id   The ID to convert.
     * @param args The arguments to consider while converting.
     * @return The converted list of strings.
     */
    @Override
    public List<String> fromId(int id, String[] args) {
        if (id == 1) {
            return Collections.singletonList("history");
        } else if (id == 2 && args[0].equalsIgnoreCase("history")) {
            return Collections.singletonList("1");
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the description of this method.
     *
     * @return The description of the method
     */
    @Override
    public String description() {
        return "History of player`s punishments";
    }

    @Override
    public String permission() {
        return "sierra.command.history";
    }
}
