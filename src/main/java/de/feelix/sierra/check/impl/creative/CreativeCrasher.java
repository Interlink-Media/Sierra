package de.feelix.sierra.check.impl.creative;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.check.SierraDetection;
import de.feelix.sierra.check.impl.creative.impl.*;
import de.feelix.sierra.check.violation.Debug;
import de.feelix.sierra.check.violation.ViolationDocument;
import de.feelix.sierra.manager.packet.IngoingProcessor;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Triple;
import de.feelix.sierraapi.check.SierraCheckData;
import de.feelix.sierraapi.check.CheckType;
import de.feelix.sierraapi.violation.MitigationStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SierraCheckData(checkType = CheckType.CREATIVE)
public class CreativeCrasher extends SierraDetection implements IngoingProcessor {

    private static final int    MAX_RECURSIONS       = 30;
    private static final String ITEMS_KEY            = "Items";
    private static final String TAG_KEY              = "tag";
    private static final String BLOCK_ENTITY_TAG_KEY = "BlockEntityTag";
    private static final int    MAX_ITEMS            = 54;

    private final List<ItemCheck> checks         = new ArrayList<>();
    private       int             recursionCount = 0;

    public CreativeCrasher(PlayerData playerData) {
        super(playerData);
        initializeChecks();
    }

    private void initializeChecks() {
        addCreativeChecks(
            new CreativeMap(),
            new CreativeClientBookCrash(),
            new PotionLimit(),
            new BooksProtocol(),
            new CreativeAnvil(),
            new FireworkSize(),
            new InvalidPlainNbt()
        );

        if (Sierra.getPlugin().getSierraConfigEngine().config().getInt("max-enchantment-level", 5) != -1) {
            addCreativeChecks(new EnchantLimit());
        }

        addCreativeChecks(new CreativeSkull());
    }

    @Override
    public void handle(PacketReceiveEvent event, PlayerData playerData) {
        if (!Sierra.getPlugin().getSierraConfigEngine().config().getBoolean("prevent-creative-crasher", true)
            || playerData == null) {
            return;
        }

        ItemStack itemStack = getItemStackFromEvent(event, playerData);
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

    private ItemStack getItemStackFromEvent(PacketReceiveEvent event, PlayerData playerData) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType.equals(PacketType.Play.Client.CREATIVE_INVENTORY_ACTION)) {
            if (playerData.getGameMode() != GameMode.CREATIVE) return null;
            return CastUtil.getSupplier(
                    () -> new WrapperPlayClientCreativeInventoryAction(event), playerData::exceptionDisconnect)
                .getItemStack();
        } else if (packetType.equals(PacketType.Play.Client.CLICK_WINDOW)) {
            WrapperPlayClientClickWindow clickWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientClickWindow(event), playerData::exceptionDisconnect);
            return clickWrapper != null ? clickWrapper.getCarriedItemStack() : null;
        } else if (packetType.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT)) {
            WrapperPlayClientPlayerBlockPlacement blockPlacementWrapper = CastUtil.getSupplier(
                () -> new WrapperPlayClientPlayerBlockPlacement(event), playerData::exceptionDisconnect);
            return blockPlacementWrapper != null ? blockPlacementWrapper.getItemStack().orElse(null) : null;
        }
        return null;
    }

    private void recursion(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                           NBTCompound blockEntityTag) {

        if (exceededRecursionMax() || !blockEntityTag.getTags().containsKey(ITEMS_KEY)) {
            return;
        }

        NBTList<NBTCompound> items = blockEntityTag.getCompoundListTagOrNull(ITEMS_KEY);

        if(items == null) return;

        if (exceededMaxItems(items)) {
            this.dispatch(event, ViolationDocument.builder()
                .mitigationStrategy(MitigationStrategy.BAN)
                .description("performed invalid item click")
                .debugs(Arrays.asList(
                    new Debug<>("Items", items.size()),
                    new Debug<>("Recursion", recursionCount),
                    new Debug<>("Item", clickedItem.getType().getName())
                ))
                .build());
            return;
        }

        processItems(event, data, clickedItem, items);
    }

    private boolean exceededRecursionMax() {
        return ++recursionCount > MAX_RECURSIONS;
    }

    private boolean exceededMaxItems(NBTList<NBTCompound> items) {
        return items.size() > MAX_ITEMS;
    }

    private void processItems(PacketReceiveEvent event, PlayerData data, ItemStack clickedItem,
                              NBTList<NBTCompound> items) {
        for (NBTCompound item : items.getTags()) {
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
            Triple<String, MitigationStrategy, List<Debug<?>>> crashDetails = check.handleCheck(event, item, tag, data);
            if (crashDetails != null) {
                List<Debug<?>> debugs = crashDetails.getThird();

                debugs.addAll(Arrays.asList(
                    new Debug<>("Item", item.getType().getName()),
                    new Debug<>("Recursion", recursionCount)
                ));

                this.dispatch(event, ViolationDocument.builder()
                    .mitigationStrategy(crashDetails.getSecond())
                    .description(crashDetails.getFirst())
                    .debugs(debugs)
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
