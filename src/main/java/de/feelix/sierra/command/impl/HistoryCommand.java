package de.feelix.sierra.command.impl;


import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.HistoryDocument;
import de.feelix.sierra.utilities.FormatUtils;
import de.feelix.sierra.utilities.Pagination;
import de.feelix.sierraapi.commands.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HistoryCommand implements ISierraCommand {

    @Override
    public void process(ISierraSender sierraSender, IBukkitAbstractCommand abstractCommand,
                        ISierraLabel sierraLabel, ISierraArguments sierraArguments) {

        if (!(sierraArguments.getArguments().size() > 1)) {
            sendHelpSyntax(sierraSender);
            return;
        }

        Pagination<HistoryDocument> pagination = new Pagination<>(
            Sierra.getPlugin().getDataManager().getHistories().stream()
                .sorted(Comparator.comparing(HistoryDocument::getTimestamp)
                            .reversed())
                .collect(Collectors.toList()), 10);

        int page = FormatUtils.toInt(sierraArguments.getArguments().get(1));
        int totalPages = pagination.totalPages();

        // Correct page
        if (page > totalPages || page < 0) {
            page = 1;
        }

        int totalHistory = pagination.getItems().size();

        String unformulated = "%s §fShowing entries: §7(page §c%s §7of §c%d §7- §c%d §7entries)";
        sierraSender.getSender()
            .sendMessage(String.format(unformulated, Sierra.PREFIX, page, totalPages, totalHistory));

        List<HistoryDocument> historyDocumentList = pagination.itemsForPage(page);

        if (historyDocumentList.isEmpty()) {
            sierraSender.getSender().sendMessage(Sierra.PREFIX + " §cNo history available");
            return;
        }

        for (HistoryDocument historyDocument : historyDocumentList) {

            String formatTimestamp = FormatUtils.formatTimestamp(historyDocument.getTimestamp());
            String username        = historyDocument.getUsername();

            sierraSender.getSenderAsPlayer().sendMessage(FormatUtils.formatColor(
                String.format(
                    "§7[%s] §c%s §7-> §c%s", formatTimestamp, username,
                    FormatUtils.shortenString(historyDocument.getDescription())
                )));
        }
    }

    private void sendHelpSyntax(ISierraSender sierraSender) {
        String prefix = Sierra.PREFIX;
        sierraSender.getSender().sendMessage(prefix + " §c§lCommand Usage §8- §7History");
        sierraSender.getSender().sendMessage(prefix + " §8- §7Show the action history");
        sierraSender.getSender().sendMessage(prefix + " §7Arguments:");
        sierraSender.getSender().sendMessage(prefix + " §8- §8<§7page§8> §8-> §7The specific page");
    }

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

    @Override
    public String description() {
        return "Show recent action history";
    }
}
