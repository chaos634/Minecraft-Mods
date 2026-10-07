package io.github.chaos634.taubenschlag.entity;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;

/**
 * A pigeon while it is not in the world: in a travel basket, or on its way home. Keeps everything the pigeon saves,
 * plus a few things to show about it without loading it.
 *
 * @param entity  the pigeon's saved data
 * @param name    its name, if it has been given one
 * @param ring    its ring number
 * @param variant its plumage ({@link BrieftaubeVariant} index)
 * @param flights how many flights home it has made
 * @param home    its loft
 */
public record BrieftaubeData(CompoundTag entity, Optional<String> name, String ring, int variant, int flights, Optional<GlobalPos> home) {
	public static final Codec<BrieftaubeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			CompoundTag.CODEC.fieldOf("entity").forGetter(BrieftaubeData::entity),
			Codec.STRING.optionalFieldOf("name").forGetter(BrieftaubeData::name),
			Codec.STRING.optionalFieldOf("ring", "").forGetter(BrieftaubeData::ring),
			Codec.INT.optionalFieldOf("variant", 0).forGetter(BrieftaubeData::variant),
			Codec.INT.optionalFieldOf("flights", 0).forGetter(BrieftaubeData::flights),
			GlobalPos.CODEC.optionalFieldOf("home").forGetter(BrieftaubeData::home)
	).apply(instance, BrieftaubeData::new));

	/** Takes everything about a pigeon, so it can be put back into the world later. */
	public static BrieftaubeData of(BrieftaubeEntity pigeon) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, pigeon.level().registryAccess());
		pigeon.saveWithoutId(output);
		Optional<String> name = pigeon.hasCustomName() ? Optional.of(pigeon.getCustomName().getString()) : Optional.empty();
		return new BrieftaubeData(output.buildResult(), name, pigeon.getRing(), pigeon.getVariant().ordinal(), pigeon.getFlights(),
				Optional.ofNullable(pigeon.getHome()));
	}

	/** Puts the pigeon back into the world. */
	public BrieftaubeEntity restore(ServerLevel level, Vec3 pos, float yRot, EntitySpawnReason reason) {
		BrieftaubeEntity pigeon = TaubenschlagEntities.BRIEFTAUBE.create(level, reason);
		if (pigeon == null) {
			return null;
		}

		pigeon.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entity));
		pigeon.snapTo(pos.x, pos.y, pos.z, yRot, 0.0F);
		pigeon.setDeltaMovement(Vec3.ZERO);
		if (!level.addFreshEntity(pigeon)) {
			// Should never happen, but if a pigeon with its identity is already about, this one gets a new one.
			pigeon.setUUID(UUID.randomUUID());
			level.addFreshEntity(pigeon);
		}
		return pigeon;
	}

	/** Its name, or its ring number if it has none. */
	public Component describe() {
		return name.<Component>map(Component::literal).orElseGet(() -> BrieftaubeEntity.describeRing(ring));
	}

	public BrieftaubeVariant plumage() {
		return BrieftaubeVariant.byIndex(variant);
	}
}
