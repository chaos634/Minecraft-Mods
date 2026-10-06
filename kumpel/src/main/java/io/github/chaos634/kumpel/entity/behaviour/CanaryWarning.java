package io.github.chaos634.kumpel.entity.behaviour;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Kanarienvogel: a canary in a cage on the Kumpel's shoulder, like the ones miners took underground.
 * It gets restless when monsters come close (and makes them glow), and when its owner is running out of air.
 * Creepers are left to the Schlagwetter warning.
 */
public class CanaryWarning {
	public static final int MONSTER_RADIUS = 12;
	private static final int GLOW_TICKS = 60;
	private static final int MONSTER_WARNING_COOLDOWN = 300;
	private static final int AIR_WARNING_COOLDOWN = 100;

	private int lastMonsterWarning = Integer.MIN_VALUE / 2;
	private int lastAirWarning = Integer.MIN_VALUE / 2;

	public void tick(KumpelEntity kumpel, ServerLevel level, Player owner) {
		int now = kumpel.tickCount;

		List<Monster> monsters = level.getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(MONSTER_RADIUS),
				monster -> monster.isAlive() && !(monster instanceof Creeper));
		Monster nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Monster monster : monsters) {
			monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
			double distance = monster.distanceToSqr(owner);
			if (distance < nearestDistance) {
				nearest = monster;
				nearestDistance = distance;
			}
		}

		if (nearest != null && now - lastMonsterWarning > MONSTER_WARNING_COOLDOWN) {
			lastMonsterWarning = now;
			owner.sendOverlayMessage(Component.translatable("message.kumpel.canary.monster", nearest.getDisplayName(), (int) Math.round(Math.sqrt(nearestDistance)))
					.withStyle(ChatFormatting.YELLOW));
			chirp(level, kumpel, 1.0F);
		}

		if (owner.getAirSupply() < owner.getMaxAirSupply() / 3 && now - lastAirWarning > AIR_WARNING_COOLDOWN) {
			lastAirWarning = now;
			owner.sendOverlayMessage(Component.translatable("message.kumpel.canary.air").withStyle(ChatFormatting.AQUA));
			chirp(level, kumpel, 1.8F);
		}
	}

	private static void chirp(ServerLevel level, KumpelEntity kumpel, float pitch) {
		level.playSound(null, kumpel.getX(), kumpel.getY() + 1.0, kumpel.getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1.0F, pitch);
	}
}
