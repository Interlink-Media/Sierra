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
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Pair;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.PunishType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SierraCheckData(checkType = CheckType.CREATIVE)
public class CreativeCrasher extends SierraDetection implements IngoingProcessor {

    private final        List<ItemCheck> checks               = new ArrayList<>();
    private static final int             MAX_RECURSIONS       = 30;
    private static final String          ITEMS_KEY            = "Items";
    private static final String          TAG_KEY              = "tag";
    private              int             recursionCount       = 0;
    private static final String          BLOCK_ENTITY_TAG_KEY = "BlockEntityTag";
    private static final int             MAX_ITEMS            = 54;

    public CreativeCrasher(PlayerData playerData) {
        super(playerData);
        initializeChecks();
    }

    private void initializeChecks() {
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

        this.addCreativeChecks(new CreativeSkull());
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-creative-crasher", true)
            || playerData == null) {
            return;
        }

        ItemStack itemStack = null;

        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            if (playerData.getGameMode() != GameMode.CREATIVE) {
                return;
            }
            WrapperPlayClientCreativeInventoryAction wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientCreativeInventoryAction(event), playerData::exceptionDisconnect);
            itemStack = wrapper.getItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event), playerData::exceptionDisconnect);

            if(wrapper == null) return;

            itemStack = wrapper.getCarriedItemStack();
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event), playerData::exceptionDisconnect);
            itemStack = wrapper.getItemStack().orElse(null);
        }

        if (itemStack == null) return;

        NBTCompound compound = itemStack.getNBT();
        if (compound != null && compound.getTags().containsKey(BLOCK_ENTITY_TAG_KEY)) {
            recursionCount = 0;
            NBTCompound blockEntityTag = compound.getCompoundTagOrNull(BLOCK_ENTITY_TAG_KEY);
            recursion(event, playerData, itemStack, blockEntityTag);
        } else if (compound != null) {
            performItemChecks(event, itemStack, compound, playerData);
        }
    }

    private void recursion(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                           NBTCompound blockEntityTag) {
        if (exceededRecursionMax() || !blockEntityTag.getTags().containsKey(ITEMS_KEY)) {
            return;
        }

        NBTList<NBTCompound> items = blockEntityTag.getCompoundListTagOrNull(ITEMS_KEY);
        if (items == null || exceededMaxItems(items)) {
            sendViolation(event, items != null ? items.size() : 0);
            return;
        }

        processItems(event, data, clickedItem, items);
    }

    private boolean exceededRecursionMax() {
        recursionCount++;
        return recursionCount > MAX_RECURSIONS;
    }

    private boolean exceededMaxItems(NBTList<NBTCompound> items) {
        return items.size() > MAX_ITEMS;
    }

    private void sendViolation(PacketReceiveEvent event, int info) {
        violation(event, ViolationDocument.builder()
            .debugInformation("Info: " + info)
            .punishType(PunishType.BAN)
            .build());
    }

    private void processItems(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                              NBTList<NBTCompound> items) {
        for (int i = 0; i < items.size(); i++) {
            NBTCompound item = items.getTag(i);
            if (item.getTags().containsKey(TAG_KEY)) {
                NBTCompound tag = item.getCompoundTagOrNull(TAG_KEY);
                if (tag == null || processTaggedItem(event, data, clickedItem, tag)) {
                    return;
                }
            } else if (performItemChecks(event, clickedItem, item, data)) {
                return;
            }
        }
    }

    private boolean processTaggedItem(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                                      NBTCompound tag) {
        if (performItemChecks(event, clickedItem, tag, data)) {
            return true;
        }

        if (tag.getTags().containsKey(BLOCK_ENTITY_TAG_KEY)) {
            NBTCompound recursionBlockEntityTag = tag.getCompoundTagOrNull(BLOCK_ENTITY_TAG_KEY);
            recursion(event, data, clickedItem, recursionBlockEntityTag);
        }
        return false;
    }

    private boolean performItemChecks(PacketReceiveEvent event, ItemStack item, NBTCompound tag, PlayerData data) {
        for (ItemCheck check : checks) {
            Pair<String, PunishType> crashDetails = check.handleCheck(event, item, tag, data);
            if (crashDetails != null) {
                sendViolationDocument(event, crashDetails);
                return true;
            }
        }
        return false;
    }

    private void sendViolationDocument(PacketReceiveEvent event, Pair<String, PunishType> crashDetails) {
        violation(event, buildViolationDocument(crashDetails));
    }

    private ViolationDocument buildViolationDocument(Pair<String, PunishType> crashDetails) {
        return ViolationDocument.builder()
            .debugInformation(crashDetails.getFirst() + " | R: " + recursionCount)
            .punishType(crashDetails.getSecond())
            .build();
    }

    private void addCreativeChecks(ItemCheck... checks) {
        this.checks.addAll(Arrays.asList(checks));
    }
}
