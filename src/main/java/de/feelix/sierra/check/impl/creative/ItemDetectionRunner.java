package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.impl.creative.impl.*;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CrashDetails;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 @author ZugPilot (Tobi)
 */
@SierraCheckData(checkType = CheckType.CREATIVE)
public class ItemDetectionRunner extends SierraDetection implements IngoingProcessor {
    /*
    This class is for running and handling all creative checks
     */
    private final List<ItemCheck> checks = new ArrayList<>();

    public ItemDetectionRunner(PlayerData playerData) {
        super(playerData);

        this.addCreativeChecks(
            new CreativeMap(),
            new CreativeClientBookCrash(),
            new PotionLimit(),
            new BooksProtocol(),
            new CreativeAnvil(),
            new FireworkSize(),
            new InvalidPlainNbt()
        );

        if (Sierra.getPlugin().getSierraConfigEngine().config().getInt("max-enchantment-level", 5) != -1) {
            this.addCreativeChecks(new EnchantLimit());
        }

        // Important check. Always import
        this.addCreativeChecks(new CreativeSkull());
    }


    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {

        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-creative-crasher", true)) {
            return;
        }

        ItemStack itemStack = null;
        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            if (playerData != null && playerData.getGameMode() != GameMode.CREATIVE) {

                return;
            }

            WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
            itemStack = wrapper.getItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            if (wrapper.getCarriedItemStack() == null) {
                return;
            }

            itemStack = wrapper.getCarriedItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            if (!wrapper.getItemStack().isPresent()) {
                return;
            }

            itemStack = wrapper.getItemStack().get();
        }

        if (itemStack == null) return;

        NBTCompound compound = itemStack.getNBT();
        //when the compound has block entity tag, do recursion to find nested/hidden items
        if (compound != null && compound.getTags().containsKey("BlockEntityTag") && playerData != null) {
            NBTCompound blockEntityTag = compound.getCompoundTagOrNull("BlockEntityTag");
            //reset recursion count to prevent false kicks
            playerData.recursionCount = 0;
            recursion(event, playerData, itemStack, blockEntityTag);
        } else if (compound != null) {
            //if this gets called, it's not a container, so we don't need to do recursion
            for (ItemCheck check : checks) {
                //Maybe add a check result class, so that we can have more detailed verbose output...
                CrashDetails crashDetails = check.handleCheck(event, itemStack, compound);
                if (crashDetails != null) {
                    violation(event, ViolationDocument.builder()
                        .debugInformation(crashDetails.getDetails())
                        .punishType(crashDetails.getPunishType())
                        .build());
                }
            }
        }
    }

    private void recursion(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                           NBTCompound blockEntityTag) {

        int maxRecursions = 30;
        //prevent recursion abuse with deeply nested items
        if (data.recursionCount++ > maxRecursions) {
            violation(event, ViolationDocument.builder()
                .debugInformation("Recursions: " + data.getRecursionCount())
                .punishType(PunishType.BAN)
                .build());
            return;
        }

        if (blockEntityTag.getTags().containsKey("Items")) {
            NBTList<NBTCompound> items = blockEntityTag.getCompoundListTagOrNull("Items");
            //This is super weird, when control + middle-clicking a chest this becomes null suddenly
            //Is this intentional behaviour? I have no idea how to fix this
            if (items == null) {
                return;
            }

            //it might be possible to send an item container via creative packets with a large amount of items in nbt
            //however I haven't actually found an exploit doing this

            int maxItems = 54;
            if (items.size() > maxItems) {
                violation(event, ViolationDocument.builder()
                    .debugInformation("Too many items: " + items.size())
                    .punishType(PunishType.BAN)
                    .build());
                return;
            }

            //Loop through all items
            for (int i = 0; i < items.size(); i++) {
                NBTCompound item = items.getTag(i);

                //Check if the item has the tag "tag" meaning it got extra nbt (besides the default item data of
                // damage, count, id etc.)
                if (item.getTags().containsKey("tag")) {
                    NBTCompound tag = item.getCompoundTagOrNull("tag");

                    //call creative checks to check for illegal tags
                    if (callDefaultChecks(event, data, clickedItem, tag)) return;

                    //if that item has block entity tag do recursion to find potential nested/"hidden" items
                    if (tag.getTags().containsKey("BlockEntityTag")) {
                        NBTCompound recursionBlockEntityTag = tag.getCompoundTagOrNull("BlockEntityTag");
                        recursion(event, data, clickedItem, recursionBlockEntityTag);
                    }
                } else {
                    //this actually only needed for the crash anvil check, since the crash anvil actually works
                    // without having "tag"
                    //it sets the damage (legacy data) value of the item anvil to 3 which results in the client
                    // placing it crashing
                    //not a fan of this approach, it runs a few unnecessary checks
                    if (callDefaultChecks(event, data, clickedItem, item)) return;
                }
            }
        }
    }

    private boolean callDefaultChecks(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                                      NBTCompound tag) {
        for (ItemCheck check : checks) {
            CrashDetails crashDetails = check.handleCheck(event, clickedItem, tag);
            if (crashDetails != null) {
                violation(event, ViolationDocument.builder()
                    .debugInformation(crashDetails.getDetails() + " | R: " + data.getRecursionCount())
                    .punishType(PunishType.BAN)
                    .build());
                return true;
            }
        }
        return false;
    }

    private void addCreativeChecks(ItemCheck... checks) {
        this.checks.addAll(Arrays.asList(checks));
    }
}
