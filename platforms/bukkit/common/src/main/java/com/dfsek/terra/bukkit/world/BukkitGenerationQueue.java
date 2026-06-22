package com.dfsek.terra.bukkit.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.event.events.world.generation.EntitySpawnEvent;
import com.dfsek.terra.bukkit.BukkitEntity;
import com.dfsek.terra.bukkit.TerraBukkitPlugin;
import com.dfsek.terra.bukkit.world.entity.BukkitEntityType;


public final class BukkitGenerationQueue {
    private static final ConcurrentMap<ChunkKey, Queue<PendingOperation>> QUEUE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, AtomicBoolean> SCHEDULED_FLUSHES = new ConcurrentHashMap<>();
    private static volatile TerraBukkitPlugin plugin;

    private BukkitGenerationQueue() {

    }

    static void queueBlock(World world, int x, int y, int z, BlockState data, boolean physics) {
        QUEUE.computeIfAbsent(key(world, x, z), ignored -> new ConcurrentLinkedQueue<>())
            .add(new PendingBlock(x, y, z, data, physics));
        scheduleFlushLoaded(world);
    }

    static void queueEntity(World world, double x, double y, double z, EntityType entityType) {
        QUEUE.computeIfAbsent(key(world, (int) Math.floor(x), (int) Math.floor(z)), ignored -> new ConcurrentLinkedQueue<>())
            .add(new PendingEntity(x, y, z, entityType));
        scheduleFlushLoaded(world);
    }

    public static void setPlugin(TerraBukkitPlugin plugin) {
        BukkitGenerationQueue.plugin = plugin;
    }

    static void flush(BukkitProtoWorld world) {
        ChunkKey key = new ChunkKey(world.getBukkitWorld().getUID(), world.centerChunkX(), world.centerChunkZ());
        Queue<PendingOperation> operations = QUEUE.remove(key);
        if(operations == null) return;

        PendingOperation operation;
        while((operation = operations.poll()) != null) {
            operation.apply(world);
        }
    }

    private static void flushLoaded(World world) {
        UUID worldId = world.getUID();
        for(Map.Entry<ChunkKey, Queue<PendingOperation>> entry : QUEUE.entrySet()) {
            ChunkKey key = entry.getKey();
            if(!key.world().equals(worldId) || !world.isChunkLoaded(key.x(), key.z())) continue;

            Queue<PendingOperation> operations = QUEUE.remove(key);
            if(operations == null) continue;

            PendingOperation operation;
            while((operation = operations.poll()) != null) {
                operation.apply(world);
            }
        }
    }

    public static void scheduleFlushLoaded(World world) {
        TerraBukkitPlugin plugin = BukkitGenerationQueue.plugin;
        if(plugin == null || !plugin.isEnabled()) return;

        UUID worldId = world.getUID();
        AtomicBoolean scheduled = SCHEDULED_FLUSHES.computeIfAbsent(worldId, ignored -> new AtomicBoolean());
        if(!scheduled.compareAndSet(false, true)) return;

        plugin.getGlobalRegionScheduler().run(plugin, task -> {
            try {
                flushLoaded(world);
            } finally {
                scheduled.set(false);
                if(hasLoadedOperations(world)) {
                    scheduleFlushLoaded(world);
                }
            }
        });
    }

    private static boolean hasLoadedOperations(World world) {
        UUID worldId = world.getUID();
        for(ChunkKey key : QUEUE.keySet()) {
            if(key.world().equals(worldId) && world.isChunkLoaded(key.x(), key.z())) return true;
        }
        return false;
    }

    private static ChunkKey key(World world, int x, int z) {
        return new ChunkKey(world.getUID(), Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    private record ChunkKey(UUID world, int x, int z) {
    }

    private interface PendingOperation {
        void apply(BukkitProtoWorld world);

        void apply(World world);
    }

    private record PendingBlock(int x, int y, int z, BlockState data, boolean physics) implements PendingOperation {
        @Override
        public void apply(BukkitProtoWorld world) {
            world.setBlockStateNow(x, y, z, data, physics);
        }

        @Override
        public void apply(World world) {
            BlockData bukkitData = BukkitAdapter.adapt(data);
            world.getBlockAt(x, y, z).setBlockData(bukkitData, false);
        }
    }

    private record PendingEntity(double x, double y, double z, EntityType entityType) implements PendingOperation {
        @Override
        public void apply(BukkitProtoWorld world) {
            world.spawnEntityNow(x, y, z, entityType);
        }

        @Override
        public void apply(World world) {
            BukkitEntity entity = new BukkitEntity(world.spawnEntity(new Location(world, x, y, z), ((BukkitEntityType) entityType).getHandle()));
            TerraBukkitPlugin plugin = BukkitGenerationQueue.plugin;
            if(plugin != null && plugin.isEnabled()) {
                plugin.getPlatform().getEventManager().callEvent(new EntitySpawnEvent(entity.world().getPack(), entity));
            }
        }
    }
}
