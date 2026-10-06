package io.github.chaos634.kumpel.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.entity.KumpelEntity;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> KUMPEL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kumpel.id("kumpel"));

	public static final EntityType<KumpelEntity> KUMPEL = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			KUMPEL_KEY,
			EntityType.Builder.<KumpelEntity>of(KumpelEntity::new, MobCategory.MISC)
					.sized(0.6F, 1.1F)
					.eyeHeight(0.9F)
					.clientTrackingRange(10)
					.build(KUMPEL_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(KUMPEL, KumpelEntity.createAttributes());
	}
}
