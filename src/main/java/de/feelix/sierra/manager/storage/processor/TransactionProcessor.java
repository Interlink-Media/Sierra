package de.feelix.sierra.manager.storage.processor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import de.feelix.sierra.Sierra;
import de.feelix.sierra.manager.storage.PlayerData;
import de.feelix.sierra.utilities.Pair;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TransactionProcessor {

    private final PlayerData player;

    public final  Queue<Pair<Short, Long>>            transactionsSent   = new ConcurrentLinkedQueue<>();
    public final  Queue<Pair<Long, Long>>             keepAlivesSent     = new ConcurrentLinkedQueue<>();
    private final LinkedList<Pair<Integer, Runnable>> transactionMap     = new LinkedList<>();
    public final  List<Short>                         didWeSendThatTrans = Collections.synchronizedList(
        new ArrayList<>());

    private final AtomicInteger transactionIDCounter    = new AtomicInteger(0);
    public        AtomicInteger lastTransactionSent     = new AtomicInteger(0);
    public        AtomicInteger lastTransactionReceived = new AtomicInteger(0);

    private short lastId = 0;

    private long transactionPing        = 0;
    private long lastTransactionPing    = 0;
    private int  lastRunnableId         = 1;
    public  long lastTransSent          = 0;
    public  long lastTransReceived      = 0;
    private long playerClockAtLeast     = System.nanoTime();
    private int  pendingSinceLastFlying = 0;

    public TransactionProcessor(PlayerData playerData) {
        this.player = playerData;
    }

    // Players can get 0 ping by repeatedly sending invalid transaction packets, but that will only hurt them
    // The design is allowing players to miss transaction packets, which shouldn't be possible
    // But if some error made a client miss a packet, then it won't hurt them too bad.
    // Also it forces players to take knockback
    public boolean addTransactionResponse(short id) {
        Pair<Short, Long> data  = null;
        boolean           hasID = false;

        for (Pair<Short, Long> iterator : transactionsSent) {
            if (iterator.getFirst() == id) {
                hasID = true;
                break;
            }
        }

        if (hasID) {
            do {
                data = transactionsSent.poll();
                if (data == null)
                    break;

                lastId = data.getFirst();
                lastTransactionReceived.incrementAndGet();
                lastTransReceived = System.currentTimeMillis();
                this.lastTransactionPing = transactionPing;
                transactionPing = (System.nanoTime() - data.getSecond()) / 1000000;
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

        // Sending in non-play corrupts the pipeline, don't waste bandwidth when anticheat disabled
        try {
            if (player.getUser().getConnectionState() != ConnectionState.PLAY) return;
        } catch (Exception exception) {
            Sierra.getPlugin()
                .getLogger()
                .warning("Skip Transaction #" + this.lastTransactionSent.get() + " for " + player.username()
                         + ", can be ignored");
            return;
        }

        // Send a packet once every 15 seconds to avoid any memory leaks
        if ((System.nanoTime() - getPlayerClockAtLeast()) > 15e9) {
            return;
        }

        lastTransSent = System.currentTimeMillis();
        short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));

        try {
            addTransactionSend(transactionID);

            PacketWrapper<?> packet;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = new WrapperPlayServerPing(transactionID);
            } else {
                packet = new WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false);
            }

            if (async) {
                ChannelHelper.runInEventLoop(player.getUser().getChannel(), () -> player.getUser().writePacket(packet));
                pendingSinceLastFlying++;
            } else {
                player.getUser().writePacket(packet);
                pendingSinceLastFlying++;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addRealTimeTask(int transaction, Runnable runnable) {

        this.lastRunnableId = transaction;

        if (lastTransactionReceived.get() >= transaction) { // If the player already responded to this transaction
            User user = PacketEvents.getAPI().getPlayerManager().getUser(this.player.getPlayer());

            if (user == null) return;

            ChannelHelper.runInEventLoop(user.getChannel(), runnable); // Run it sync to player channel
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
                if (transaction + 1 < pair.getFirst())
                    return;

                // This is at most tick ahead of what we want
                if (transaction == pair.getFirst() - 1)
                    continue;

                try {
                    // Run the task
                    pair.getSecond().run();
                } catch (Exception e) {
                    String name = player.username();
                    System.out.printf("An error has occurred when running transactions for player: %s%n", name);
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
