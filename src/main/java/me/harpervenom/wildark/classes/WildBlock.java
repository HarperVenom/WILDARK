package me.harpervenom.wildark.classes;

import me.harpervenom.wildark.WILDARK;
import me.harpervenom.wildark.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static me.harpervenom.wildark.WILDARK.db;
import static me.harpervenom.wildark.listeners.WildChunksListener.wildBlocks;

public class WildBlock {

    private final static List<WildBlock> placedBatch = new ArrayList<>();
    private final static List<WildBlock> destroyedBatch = new ArrayList<>();
    private final static List<WildBlock> movedBatch = new ArrayList<>();

    private int id;
    private Location loc;
    private final String ownerId;
    private Timestamp timestamp;

    static {
        startBatchProcessing();
    }

    public WildBlock(Location loc, String ownerId, Timestamp timestamp){
        this.loc = loc;
        this.ownerId = ownerId;
        this.timestamp = timestamp;
    }

    public WildBlock(int id, Location loc, String ownerId, Timestamp timestamp){
        this.id = id;
        this.loc = loc;
        this.ownerId = ownerId;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public Location getLoc(){
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }

    public String getOwnerId(){
        return ownerId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void save() {
        Chunk chunk = loc.getChunk();
        List<WildBlock> wildBlockList = new ArrayList<>(wildBlocks.get(chunk));

        wildBlockList.stream().filter(block -> block.getLoc().equals(loc)).forEach(WildBlock::remove);
        wildBlockList.removeIf(block -> block.getLoc().equals(loc));

        wildBlockList.add(this);
        wildBlocks.put(chunk, wildBlockList);

        queueForPlacedBatch();
    }

    public void remove() {
        Chunk chunk = loc.getChunk();
        wildBlocks.put(chunk, wildBlocks.get(chunk).stream().filter(currentWildBlock -> !currentWildBlock.equals(this)).toList());

        queueForDestroyedBatch(this);
    }

    public void move(Location newLocation) {
        loc = newLocation;

        queueForMovedBatch();
    }

    private void queueForPlacedBatch() {
        placedBatch.add(this);

        int BATCH_SIZE = 50;
        if (placedBatch.size() >= BATCH_SIZE) {
            flushPlacedBatch();
        }
    }

    private void queueForDestroyedBatch(WildBlock b) {
        boolean existed = placedBatch.removeIf(block -> block.getLoc().equals(b.getLoc()));
        if (existed) return;

        destroyedBatch.add(b);

        int BATCH_SIZE = 50;
        if (destroyedBatch.size() >= BATCH_SIZE) {
            flushDestroyedBatch();
        }
    }

    private void queueForMovedBatch() {
        movedBatch.add(this);

        int BATCH_SIZE = 50;
        if (movedBatch.size() >= BATCH_SIZE) {
            flushMovedBatch();
        }
    }

    private static void flushPlacedBatch() {
        List<WildBlock> blocksToSave;

        blocksToSave = new ArrayList<>(placedBatch);
        placedBatch.clear();

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (WildBlock block : blocksToSave) {
            CompletableFuture<Integer> future = db.blocks.logBlock(
                    block.getOwnerId(),
                    block.getLoc().getBlockX(),
                    block.getLoc().getBlockY(),
                    block.getLoc().getBlockZ(),
                    block.getLoc().getWorld().getName(),
                    block.timestamp
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
//            Bukkit.broadcastMessage("saved blocks: " + futures.size());

            // Assigns blocks' generated ids
            for (int i = 0; i < futures.size(); i++) {
                WildBlock b = blocksToSave.get(i);  // Get corresponding block
                CompletableFuture<Integer> future = futures.get(i);

                try {
                    Integer result = future.join();
                    b.setId(result);
                } catch (CompletionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void flushDestroyedBatch() {
        List<WildBlock> blocksToRemove;
        blocksToRemove = new ArrayList<>(destroyedBatch);
        destroyedBatch.clear();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (WildBlock block : blocksToRemove) {
            CompletableFuture<Boolean> future = db.blocks.deleteBlockRecord(block.getId());
            futures.add(future);
        }

//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
//            Bukkit.broadcastMessage("removed blocks: " + futures.size());
//        });
    }

    private static void flushMovedBatch() {
        List<WildBlock> blocksToRemove;
        blocksToRemove = new ArrayList<>(movedBatch);
        movedBatch.clear();

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (WildBlock block : blocksToRemove) {
            CompletableFuture<Boolean> future = db.blocks.updateBlockLoc(block.getId(), block.getLoc());
            futures.add(future);
        }

//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
//            Bukkit.broadcastMessage("moved blocks: " + futures.size());
//        });
    }

    private static void startBatchProcessing() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(WILDARK.getPlugin(),
                () -> {
                    flushPlacedBatch();
                    flushDestroyedBatch();
                    flushMovedBatch();
                }
                , 20L * 10, 20L * 10);
    }
}
