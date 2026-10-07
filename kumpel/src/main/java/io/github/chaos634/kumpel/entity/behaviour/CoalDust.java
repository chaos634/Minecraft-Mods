package io.github.chaos634.kumpel.entity.behaviour;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Kohlenstaub &amp; Waschkaue: digging covers a Kumpel in dust, coal most of all, in stages you can see on it.
 * Water and rain wash it off bit by bit and a bucket of water all at once; a dusty Kumpel with nothing to do walks
 * to a water cauldron nearby and washes there, like miners in the Kaue after their shift.
 */
public final class CoalDust {
	/** The most dust a Kumpel can carry. */
	public static final int MAX = 60;
	/** Visible stages of dust (0 is clean). */
	public static final int STAGES = 3;
	private static final int PER_STAGE = MAX / (STAGES + 1);
	/** From this much dust on, a Kumpel goes to wash when it finds a water cauldron. */
	public static final int WASH_AT = 2 * PER_STAGE;
	private static final int PER_BLOCK = 1;
	private static final int PER_COAL = 4;
	/** Dust washed off per second standing in water, or in the rain. */
	private static final int WATER_WASH = 10;
	private static final int RAIN_WASH = 3;
	/** Coal ores, modded ones included. */
	private static final TagKey<Block> COAL_ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/coal"));

	private CoalDust() {
	}

	public static int stage(int dust) {
		return Math.min(STAGES, dust / PER_STAGE);
	}

	/** How much dust digging this block raises. */
	public static int fromDigging(BlockState state) {
		boolean coal = state.is(COAL_ORES) || state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE);
		return coal ? PER_COAL : PER_BLOCK;
	}

	/** Called once a second: water and rain wash the dust off. */
	public static void tick(KumpelEntity kumpel) {
		if (kumpel.getDust() == 0) {
			return;
		}

		if (kumpel.isInWater()) {
			kumpel.wash(WATER_WASH);
		} else if (kumpel.isInWaterOrRain()) {
			kumpel.wash(RAIN_WASH);
		}
	}

	/** A splash of water over the Kumpel. */
	public static void splash(ServerLevel level, KumpelEntity kumpel) {
		level.sendParticles(ParticleTypes.SPLASH, kumpel.getX(), kumpel.getY() + 0.8, kumpel.getZ(), 30, 0.3, 0.4, 0.3, 0.1);
		level.sendParticles(ParticleTypes.BUBBLE_POP, kumpel.getX(), kumpel.getY() + 0.6, kumpel.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, kumpel.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.6F, 1.3F);
	}
}
