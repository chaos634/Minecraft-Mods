package io.github.chaos634.kumpel.entity.behaviour;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Schlagwetter warning: the Kumpel notices creepers around its owner and makes them glow (visible through walls),
 * and it warns when its owner is about to step next to lava.
 */
public class DangerSense {
	private static final int GLOW_TICKS = 60;
	private static final int CREEPER_WARNING_COOLDOWN = 200;
	private static final int LAVA_WARNING_COOLDOWN = 300;

	private int lastCreeperWarning = Integer.MIN_VALUE / 2;
	private int lastLavaWarning = Integer.MIN_VALUE / 2;

	public void tick(KumpelEntity kumpel, ServerLevel level, Player owner) {
		KumpelConfig.Behaviour behaviour = KumpelSettings.get().behaviour();
		int now = kumpel.tickCount;

		if (behaviour.creeperWarning) {
			double radius = Math.min(32, behaviour.creeperWarningRadius + 2 * (kumpel.getTier().level() - 1));
			List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, owner.getBoundingBox().inflate(radius), Creeper::isAlive);
			Creeper nearest = null;
			double nearestDistance = Double.MAX_VALUE;
			for (Creeper creeper : creepers) {
				creeper.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
				double distance = creeper.distanceToSqr(owner);
				if (distance < nearestDistance) {
					nearest = creeper;
					nearestDistance = distance;
				}
			}

			if (nearest != null && now - lastCreeperWarning > CREEPER_WARNING_COOLDOWN) {
				lastCreeperWarning = now;
				kumpel.pointAt(nearest.position().add(0.0, nearest.getBbHeight() * 0.5, 0.0));
				owner.sendOverlayMessage(Component.translatable("message.kumpel.creeper", kumpel.getDisplayName(), (int) Math.round(Math.sqrt(nearestDistance)))
						.withStyle(ChatFormatting.RED));
				level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.NEUTRAL, 1.0F, 0.6F);
			}
		}

		if (behaviour.lavaWarning && now - lastLavaWarning > LAVA_WARNING_COOLDOWN && !owner.fireImmune() && lavaNextTo(level, owner.blockPosition())) {
			lastLavaWarning = now;
			owner.sendOverlayMessage(Component.translatable("message.kumpel.lava", kumpel.getDisplayName()).withStyle(ChatFormatting.GOLD));
			level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.NEUTRAL, 1.0F, 1.2F);
		}
	}

	private static boolean lavaNextTo(ServerLevel level, BlockPos feet) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 1; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					cursor.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
					if (level.getBlockState(cursor).is(Blocks.LAVA)) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
