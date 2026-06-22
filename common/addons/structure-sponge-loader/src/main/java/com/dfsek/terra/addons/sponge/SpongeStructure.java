/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.sponge;

import com.dfsek.seismic.type.Rotation;
import com.dfsek.seismic.type.vector.Vector2Int;
import com.dfsek.seismic.type.vector.Vector3Int;

import java.util.random.RandomGenerator;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.registry.key.Keyed;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.structure.Structure;
import com.dfsek.terra.api.world.WritableWorld;


public class SpongeStructure implements Structure, Keyed<SpongeStructure> {

    private final BlockState[][][] blocks;

    private final int offsetX, offsetY, offsetZ;
    private final int maxHorizontalRadius;

    private final RegistryKey id;

    public SpongeStructure(BlockState[][][] blocks, Vector3Int offset, RegistryKey id) {
        this.blocks = blocks;
        this.offsetX = offset.getX();
        this.offsetY = offset.getY();
        this.offsetZ = offset.getZ();
        this.maxHorizontalRadius = calculateMaxHorizontalRadius(blocks, offsetX, offsetZ);
        this.id = id;
    }

    @Override
    public boolean generate(Vector3Int location, WritableWorld world, RandomGenerator random, Rotation rotation) {
        int bX = location.getX();
        int bY = location.getY();
        int bZ = location.getZ();
        for(int x = 0; x < blocks.length; x++) {
            for(int z = 0; z < blocks[x].length; z++) {
                int oX = x + offsetX;
                int oZ = z + offsetZ;
                Vector2Int r = Vector2Int.Mutable.of(oX, oZ).rotate(rotation);
                int rX = r.getX();
                int rZ = r.getZ();
                for(int y = 0; y < blocks[x][z].length; y++) {
                    BlockState state = blocks[x][z][y];
                    if(state == null) continue;
                    world.setBlockState(bX + rX, bY + y + offsetY, bZ + rZ, state);
                }
            }
        }
        return true;
    }

    @Override
    public RegistryKey getRegistryKey() {
        return id;
    }

    @Override
    public int getMaxHorizontalRadius() {
        return maxHorizontalRadius;
    }

    private static int calculateMaxHorizontalRadius(BlockState[][][] blocks, int offsetX, int offsetZ) {
        int max = 0;
        for(int x = 0; x < blocks.length; x++) {
            for(int z = 0; z < blocks[x].length; z++) {
                max = Math.max(max, Math.abs(x + offsetX));
                max = Math.max(max, Math.abs(z + offsetZ));
            }
        }
        return max;
    }
}
