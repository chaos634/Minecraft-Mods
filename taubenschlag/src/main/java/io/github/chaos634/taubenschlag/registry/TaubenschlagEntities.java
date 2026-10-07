package io.github.chaos634.taubenschlag.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;

public final class TaubenschlagEntities {
	public static final ResourceKey<EntityType<?>> BRIEFTAUBE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Taubenschlag.id("brieftaube"));

	public static final EntityType<BrieftaubeEntity> BRIEFTAUBE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			BRIEFTAUBE_KEY,
			EntityType.Builder.<BrieftaubeEntity>of(BrieftaubeEntity::new, MobCategory.CREATURE)
					.sized(0.5F, 0.6F)
					.eyeHeight(0.5F)
					.clientTrackingRange(8)
					.build(BRIEFTAUBE_KEY)
	);

	/** How often wild pigeons turn up, compared to other animals (sheep have 12). */
	private static final int SPAWN_WEIGHT = 8;

	private TaubenschlagEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(BRIEFTAUBE, BrieftaubeEntity.createAttributes());
		SpawnPlacements.register(BRIEFTAUBE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
		// Wild pigeons live on open land, in small flocks.
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW),
				MobCategory.CREATURE, BRIEFTAUBE, SPAWN_WEIGHT, 2, 4);
	}
}
