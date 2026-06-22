package com.dfsek.terra.bukkit.nms;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.nms.config.VanillaBiomeProperties;


public class NMSBiomeInjector {

    public static <T> Optional<Holder<T>> getEntry(Registry<T> registry, Identifier identifier) {
        return registry.getOptional(identifier)
            .flatMap(registry::getResourceKey)
            .flatMap(registry::get);
    }

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties)
    throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder();

        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder();
        EnvironmentAttributeMap.Builder attributes = EnvironmentAttributeMap.builder().putAll(vanilla.getAttributes());
        AmbientSounds ambientSounds = vanilla.getAttributes().applyModifier(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.EMPTY);

        effects.waterColor(Objects.requireNonNullElse(vanillaBiomeProperties.getWaterColor(), vanilla.getWaterColor()))
            .grassColorModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getGrassColorModifier(),
                vanilla.getSpecialEffects().grassColorModifier()));

        if(vanillaBiomeProperties.getFogColor() != null) {
            attributes.set(EnvironmentAttributes.FOG_COLOR, vanillaBiomeProperties.getFogColor());
        }

        if(vanillaBiomeProperties.getWaterFogColor() != null) {
            attributes.set(EnvironmentAttributes.WATER_FOG_COLOR, vanillaBiomeProperties.getWaterFogColor());
        }

        if(vanillaBiomeProperties.getSkyColor() != null) {
            attributes.set(EnvironmentAttributes.SKY_COLOR, vanillaBiomeProperties.getSkyColor());
        }

        if(vanillaBiomeProperties.getMusicVolume() != null) {
            attributes.set(EnvironmentAttributes.MUSIC_VOLUME, vanillaBiomeProperties.getMusicVolume());
        }

        if(vanillaBiomeProperties.getGrassColor() == null) {
            vanilla.getSpecialEffects().grassColorOverride().ifPresent(effects::grassColorOverride);
        } else {
            effects.grassColorOverride(vanillaBiomeProperties.getGrassColor());
        }

        if(vanillaBiomeProperties.getFoliageColor() == null) {
            vanilla.getSpecialEffects().foliageColorOverride().ifPresent(effects::foliageColorOverride);
        } else {
            effects.foliageColorOverride(vanillaBiomeProperties.getFoliageColor());
        }

        if(vanillaBiomeProperties.getDryFoliageColor() == null) {
            vanilla.getSpecialEffects().dryFoliageColorOverride().ifPresent(effects::dryFoliageColorOverride);
        } else {
            effects.dryFoliageColorOverride(vanillaBiomeProperties.getDryFoliageColor());
        }

        if(vanillaBiomeProperties.getParticleConfig() != null) {
            attributes.set(EnvironmentAttributes.AMBIENT_PARTICLES, List.of(vanillaBiomeProperties.getParticleConfig()));
        }

        if(vanillaBiomeProperties.getLoopSound() != null) {
            Optional<? extends Holder<net.minecraft.sounds.SoundEvent>> loop = RegistryFetcher.soundEventRegistry()
                .get(vanillaBiomeProperties.getLoopSound().location());
            ambientSounds = new AmbientSounds(loop.map(holder -> (Holder<net.minecraft.sounds.SoundEvent>) holder),
                ambientSounds.mood(), ambientSounds.additions());
            attributes.set(EnvironmentAttributes.AMBIENT_SOUNDS, ambientSounds);
        }

        if(vanillaBiomeProperties.getMoodSound() != null) {
            ambientSounds = new AmbientSounds(ambientSounds.loop(), Optional.of(vanillaBiomeProperties.getMoodSound()),
                ambientSounds.additions());
            attributes.set(EnvironmentAttributes.AMBIENT_SOUNDS, ambientSounds);
        }

        if(vanillaBiomeProperties.getAdditionsSound() != null) {
            ambientSounds = new AmbientSounds(ambientSounds.loop(), ambientSounds.mood(), List.of(vanillaBiomeProperties.getAdditionsSound()));
            attributes.set(EnvironmentAttributes.AMBIENT_SOUNDS, ambientSounds);
        }

        if(vanillaBiomeProperties.getMusic() != null) {
            attributes.set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(vanillaBiomeProperties.getMusic()));
        }

        builder.hasPrecipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getBaseTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(), vanilla.climateSettings.downfall()));

        builder.temperatureAdjustment(
            Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(), vanilla.climateSettings.temperatureModifier()));

        builder.mobSpawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getMobSettings()));

        return builder
            .putAttributes(attributes)
            .specialEffects(effects.build())
            .generationSettings(new BiomeGenerationSettings.PlainBuilder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }
}
