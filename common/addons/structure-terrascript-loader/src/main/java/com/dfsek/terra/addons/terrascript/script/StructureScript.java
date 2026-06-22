/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.terrascript.script;

import com.dfsek.seismic.math.algebra.AlgebraFunctions;
import com.dfsek.seismic.math.trigonometry.TrigonometryFunctions;
import com.dfsek.seismic.type.Rotation;
import com.dfsek.seismic.type.vector.Vector3Int;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.random.RandomGenerator;

import com.dfsek.terra.addons.terrascript.parser.Parser;
import com.dfsek.terra.addons.terrascript.parser.lang.Executable;
import com.dfsek.terra.addons.terrascript.parser.lang.Returnable;
import com.dfsek.terra.addons.terrascript.parser.lang.functions.FunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.BinaryNumberFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.BiomeFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.BlockFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.CheckBlockFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.EntityFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.GetMarkFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.LootFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.PullFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.RandomFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.RecursionsFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.SetMarkFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.StateFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.StructureFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.UnaryBooleanFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.UnaryNumberFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.UnaryStringFunctionBuilder;
import com.dfsek.terra.addons.terrascript.script.builders.ZeroArgFunctionBuilder;
import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.registry.Registry;
import com.dfsek.terra.api.registry.key.Keyed;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.structure.LootTable;
import com.dfsek.terra.api.structure.Structure;
import com.dfsek.terra.api.world.WritableWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;


public class StructureScript implements Structure, Keyed<StructureScript> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureScript.class);
    private final Executable block;
    private final RegistryKey id;

    private final String profile;
    private final Platform platform;
    private int maxHorizontalRadius = -1;
    private boolean calculatingRadius = false;

    @SuppressWarnings("rawtypes")
    public StructureScript(InputStream inputStream, RegistryKey id, Platform platform, Registry<Structure> registry,
                           Registry<LootTable> lootRegistry,
                           Registry<FunctionBuilder> functionRegistry) {
        Parser parser;
        try {
            parser = new Parser(IOUtils.toString(inputStream, Charset.defaultCharset()));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        this.id = id;
        this.profile = "terrascript_direct:" + id;

        //noinspection unchecked
        functionRegistry.forEach((key, function) -> parser.registerFunction(key.getID(), function)); // Register registry functions.

        parser
            .registerFunction("block", new BlockFunctionBuilder(platform))
            .registerFunction("debugBlock", new BlockFunctionBuilder(platform))
            .registerFunction("structure", new StructureFunctionBuilder(registry, platform))
            .registerFunction("randomInt", new RandomFunctionBuilder())
            .registerFunction("recursions", new RecursionsFunctionBuilder())
            .registerFunction("setMark", new SetMarkFunctionBuilder())
            .registerFunction("getMark", new GetMarkFunctionBuilder())
            .registerFunction("pull", new PullFunctionBuilder(platform))
            .registerFunction("loot", new LootFunctionBuilder(platform, lootRegistry, this))
            .registerFunction("entity", new EntityFunctionBuilder(platform))
            .registerFunction("getBiome", new BiomeFunctionBuilder(platform))
            .registerFunction("getBlock", new CheckBlockFunctionBuilder())
            .registerFunction("state", new StateFunctionBuilder(platform))
            .registerFunction("setWaterlog", new UnaryBooleanFunctionBuilder((waterlog, args) -> args.setWaterlog(waterlog)))
            .registerFunction("originX", new ZeroArgFunctionBuilder<Number>(arguments -> arguments.getOrigin().getX(),
                Returnable.ReturnType.NUMBER))
            .registerFunction("originY", new ZeroArgFunctionBuilder<Number>(arguments -> arguments.getOrigin().getY(),
                Returnable.ReturnType.NUMBER))
            .registerFunction("originZ", new ZeroArgFunctionBuilder<Number>(arguments -> arguments.getOrigin().getZ(),
                Returnable.ReturnType.NUMBER))
            .registerFunction("rotation", new ZeroArgFunctionBuilder<>(arguments -> arguments.getRotation().toString(),
                Returnable.ReturnType.STRING))
            .registerFunction("rotationDegrees", new ZeroArgFunctionBuilder<>(arguments -> arguments.getRotation().getDegrees(),
                Returnable.ReturnType.NUMBER))
            .registerFunction("print",
                new UnaryStringFunctionBuilder(string -> LOGGER.info("[TerraScript:{}] {}", id, string)))
            .registerFunction("abs", new UnaryNumberFunctionBuilder(number -> Math.abs(number.doubleValue())))
            .registerFunction("pow2", new UnaryNumberFunctionBuilder(number -> Math.pow(number.doubleValue(), 2)))
            .registerFunction("pow", new BinaryNumberFunctionBuilder(
                (number, number2) -> Math.pow(number.doubleValue(), number2.doubleValue())))
            .registerFunction("sqrt", new UnaryNumberFunctionBuilder(number -> Math.sqrt(number.doubleValue())))
            .registerFunction("rsqrt", new UnaryNumberFunctionBuilder(number -> AlgebraFunctions.invSqrt(number.doubleValue())))
            .registerFunction("floor", new UnaryNumberFunctionBuilder(number -> Math.floor(number.doubleValue())))
            .registerFunction("ceil", new UnaryNumberFunctionBuilder(number -> Math.ceil(number.doubleValue())))
            .registerFunction("log", new UnaryNumberFunctionBuilder(number -> Math.log(number.doubleValue())))
            .registerFunction("round", new UnaryNumberFunctionBuilder(number -> Math.round(number.doubleValue())))
            .registerFunction("sin", new UnaryNumberFunctionBuilder(number -> TrigonometryFunctions.sin(number.doubleValue())))
            .registerFunction("cos", new UnaryNumberFunctionBuilder(number -> TrigonometryFunctions.cos(number.doubleValue())))
            .registerFunction("tan", new UnaryNumberFunctionBuilder(number -> TrigonometryFunctions.tan(number.doubleValue())))
            .registerFunction("asin", new UnaryNumberFunctionBuilder(number -> Math.asin(number.doubleValue())))
            .registerFunction("acos", new UnaryNumberFunctionBuilder(number -> Math.acos(number.doubleValue())))
            .registerFunction("atan", new UnaryNumberFunctionBuilder(number -> Math.atan(number.doubleValue())))
            .registerFunction("max", new BinaryNumberFunctionBuilder(
                (number, number2) -> Math.max(number.doubleValue(), number2.doubleValue())))
            .registerFunction("min", new BinaryNumberFunctionBuilder(
                (number, number2) -> Math.min(number.doubleValue(), number2.doubleValue())));

        if(!platform.getTerraConfig().isDebugScript()) {
            parser.ignoreFunction("debugBlock");
        }

        block = parser.parse();
        this.platform = platform;
    }

    @Override
    @SuppressWarnings("try")
    public boolean generate(Vector3Int location, WritableWorld world, RandomGenerator random, Rotation rotation) {
        platform.getProfiler().push(profile);
        boolean result = applyBlock(new TerraImplementationArguments(location, rotation, random, world, 0));
        platform.getProfiler().pop(profile);
        return result;
    }

    public boolean generate(Vector3Int location, WritableWorld world, RandomGenerator random, Rotation rotation, int recursions) {
        platform.getProfiler().push(profile);
        boolean result = applyBlock(new TerraImplementationArguments(location, rotation, random, world, recursions));
        platform.getProfiler().pop(profile);
        return result;
    }

    private boolean applyBlock(TerraImplementationArguments arguments) {
        try {
            return block.execute(arguments);
        } catch(RuntimeException e) {
            LOGGER.error("Failed to generate structure at {}", arguments.getOrigin(), e);
            return false;
        }
    }

    @Override
    public RegistryKey getRegistryKey() {
        return id;
    }

    @Override
    public int getMaxHorizontalRadius() {
        if(maxHorizontalRadius >= 0) return maxHorizontalRadius;
        if(calculatingRadius) return 0;
        calculatingRadius = true;
        maxHorizontalRadius = Math.max(block.getMaxHorizontalRadius(), dryRunMaxHorizontalRadius());
        calculatingRadius = false;
        return maxHorizontalRadius;
    }

    private int dryRunMaxHorizontalRadius() {
        int max = 0;
        for(RandomGenerator random : new RandomGenerator[] {
            new ExtremeRandom(false),
            new ExtremeRandom(true),
            new Random(0),
            new Random(1),
            new Random(31)
        }) {
            RecordingWorld recordingWorld = new RecordingWorld(platform.getWorldHandle().createBlockState("minecraft:air"));
            applyBlock(new TerraImplementationArguments(Vector3Int.of(0, 0, 0), Rotation.NONE, random, recordingWorld, 0));
            max = Math.max(max, recordingWorld.getMaxHorizontalRadius());
        }
        return max;
    }

    private static final class ExtremeRandom extends Random {
        private final boolean high;

        private ExtremeRandom(boolean high) {
            this.high = high;
        }

        @Override
        public int nextInt(int bound) {
            return high ? bound - 1 : 0;
        }
    }

    private final class RecordingWorld implements WritableWorld {
        private final BlockState air;
        private int maxHorizontalRadius = 0;

        private RecordingWorld(BlockState air) {
            this.air = air;
        }

        private int getMaxHorizontalRadius() {
            return maxHorizontalRadius;
        }

        private void record(double x, double z) {
            maxHorizontalRadius = Math.max(maxHorizontalRadius, (int) Math.ceil(Math.max(Math.abs(x), Math.abs(z))));
        }

        @Override
        public Object getHandle() {
            return null;
        }

        @Override
        public BlockState getBlockState(int x, int y, int z) {
            return air;
        }

        @Override
        public BlockEntity getBlockEntity(int x, int y, int z) {
            return null;
        }

        @Override
        public long getSeed() {
            return 0;
        }

        @Override
        public int getMaxHeight() {
            return 384;
        }

        @Override
        public int getMinHeight() {
            return -64;
        }

        @Override
        public ChunkGenerator getGenerator() {
            return null;
        }

        @Override
        public BiomeProvider getBiomeProvider() {
            return null;
        }

        @Override
        public ConfigPack getPack() {
            return null;
        }

        @Override
        public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
            record(x, z);
        }

        @Override
        public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
            record(x, z);
            return null;
        }
    }
}
