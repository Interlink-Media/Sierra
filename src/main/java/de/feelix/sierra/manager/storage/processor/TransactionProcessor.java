package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.manager.storage.logger.LogTag;
import de.feelix.sierra.utilities.CastUtil;
import de.feelix.sierra.utilities.Pair;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TransactionProcessor {

    private final PlayerData player;

    public final Queue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    public final Queue<Pair<Long, Long>> keepAlivesSent = new ConcurrentLinkedQueue<>();
    private final LinkedList<Pair<Integer, Runnable>> transactionMap = new LinkedList<>();
    public final List<Short> didWeSendThatTrans = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger transactionIDCounter = new AtomicInteger(0);
    public AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public AtomicInteger lastTransactionReceived = new AtomicInteger(0);

    private long transactionPing = 0;
    public long lastTransSent = 0;
    public long lastTransReceived = 0;
    private long playerClockAtLeast = System.nanoTime();

    public TransactionProcessor(PlayerData playerData) {
        this.player = playerData;
    }

    // Players can get 0 ping by repeatedly sending invalid transaction packets, but that will only hurt them
    // The design is allowing players to miss transaction packets, which shouldn't be possible
    // But if some error made a client miss a packet, then it won't hurt them too bad.
    // Also it forces players to take knockback
    public boolean addTransactionResponse(short id) {
        Pair<Short, Long> data = null;
        boolean hasID = false;
        int skipped = 0;
        for (Pair<Short, Long> iterator : transactionsSent) {
            if (iterator.getFirst() == id) {
                hasID = true;
                break;
            }
            skipped++;
        }

        if (hasID) {

            if (skipped > 0 && System.currentTimeMillis() - player.getJoinTime() > 5000) {
                player.getSierraLogger().log(LogTag.SKIP, "Skipped transaction: " + id + " (" + skipped + ")");
            }

            do {
                data = transactionsSent.poll();
                if (data == null) break;

                lastTransactionReceived.incrementAndGet();
                lastTransReceived = System.currentTimeMillis();
                transactionPing = (System.nanoTime() - data.getSecond());
                playerClockAtLeast = data.getSecond();
            } while (data.getFirst() != id);

            handleNettySyncTransaction(lastTransactionReceived.get());
        }

        // Were we the ones who sent the packet?
        return data != null && data.getFirst() == id;
    }

    public void sendTransaction() {
        sendTransaction(false);
    }

    public void sendTransaction(boolean async) {

        // don't send transactions outside PLAY phase
        // Sending in non-play corrupts the pipeline, don't waste bandwidth when anticheat disabled
        if (player.getUser().getEncoderState() != ConnectionState.PLAY) return;

        // Send a packet once every 15 seconds to avoid any memory leaks
        if ((System.nanoTime() - getPlayerClockAtLeast()) > 15e9) {
            return;
        }

        lastTransSent = System.currentTimeMillis();
        short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));
        try {

            PacketWrapper<?> packet;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = CastUtil.getSupplier(
                    () -> new WrapperPlayServerPing(transactionID), player::exceptionDisconnect);
            } else {
                packet = CastUtil.getSupplier(
                    () -> new WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false),
                    player::exceptionDisconnect
                );
            }

            if (async) {
                ChannelHelper.runInEventLoop(player.getUser().getChannel(), () -> {
                    addTransactionSend(transactionID);
                    player.getUser().writePacket(packet);
                });
            } else {
                addTransactionSend(transactionID);
                player.getUser().writePacket(packet);
            }
        } catch (Exception exception) {
            player.getSierraLogger().log(LogTag.TRANS_EXCEP, "Error: " + exception.getMessage());
        }
    }

    public void handleTransactionClient(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            handleWindowConfirmation(event);
        } else if (packetType == PacketType.Play.Client.PONG) {
            handlePong(event);
        }
    }

    private void handleWindowConfirmation(PacketReceiveEvent event) {
        WrapperPlayClientWindowConfirmation wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientWindowConfirmation(event), player::exceptionDisconnect);
        short id = wrapper.getActionId();
        if (id <= 0 && addTransactionResponse(id)) {
            event.setCancelled(true);
        }
    }

    private void handlePong(PacketReceiveEvent event) {
        WrapperPlayClientPong wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayClientPong(event), player::exceptionDisconnect);
        int id = wrapper.getId();
        if (id == (short) id && addTransactionResponse((short) id)) {
            event.setCancelled(true);
        }
    }

    public void handleTransactionSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            handlePingTransaction(event);
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            handleWindowConfirmationTransaction(event);
        }
    }

    private void handlePingTransaction(PacketSendEvent event) {
        WrapperPlayServerPing wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayServerPing(event), player::exceptionDisconnect);

        int id = wrapper.getId();
        // Check if in the short range, we only use short range
        if (id == (short) id) {
            // Cast ID twice so we can use the list
            Short shortID = ((short) id);
            if (didWeSendThatTrans.remove(shortID)) {
                Pair<Short, Long> solarPair = new Pair<>(shortID, System.nanoTime());
                transactionsSent.add(solarPair);
                lastTransactionSent.getAndIncrement();
            }
        }
    }

    private void handleWindowConfirmationTransaction(PacketSendEvent event) {
        WrapperPlayServerWindowConfirmation wrapper = CastUtil.getSupplier(
            () -> new WrapperPlayServerWindowConfirmation(event), player::exceptionDisconnect);

        short id = wrapper.getActionId();

        // Vanilla always uses an ID starting from 1
        if (id <= 0) {
            if (didWeSendThatTrans.remove((Short) id)) {
                Pair<Short, Long> solarPair = new Pair<>(id, System.nanoTime());
                transactionsSent.add(solarPair);
                lastTransactionSent.getAndIncrement();
            }
        }
    }

    public void addRealTimeTask(int transaction, boolean async, Runnable runnable) {
        if (lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            if (async) {
                ChannelHelper.runInEventLoop(player.getUser().getChannel(), runnable); // Run it sync to player channel
            } else {
                runnable.run();
            }
            return;
        }
        synchronized (this) {
            transactionMap.add(new Pair<>(transaction, runnable));
        }
    }

    public void handleNettySyncTransaction(int transaction) {
        synchronized (this) {
            for (ListIterator<Pair<Integer, Runnable>> iterator = transactionMap.listIterator(); iterator.hasNext(); ) {
                Pair<Integer, Runnable> pair = iterator.next();

                // We are at most a tick ahead when running tasks based on transactions, meaning this is too far
                if (transaction + 1 < pair.getFirst()) return;

                // This is at most tick ahead of what we want
                if (transaction == pair.getFirst() - 1) continue;


                try {
                    // Run the task
                    pair.getSecond().run();
                } catch (Exception e) {
                    Sierra.getPlugin().getLogger().severe("An error has occurred when running "
                                                          + "transactions for player: " + player.username());
                    e.printStackTrace();
                }
                // We ran a task, remove it from the list
                iterator.remove();
            }
        }
    }

    public void addTransactionSend(short id) {
        didWeSendThatTrans.add(id);
    }
}
