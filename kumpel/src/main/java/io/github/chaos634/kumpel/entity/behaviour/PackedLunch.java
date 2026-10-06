package io.github.chaos634.kumpel.entity.behaviour;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;

/**
 * Henkelmann: the Kumpel keeps some food in its backpack and hands it to its owner when they get hungry.
 */
public class PackedLunch {
	private static final int COOLDOWN = 400;

	private int lastLunch = Integer.MIN_VALUE / 2;

	public void tick(KumpelEntity kumpel, ServerLevel level, Player owner) {
		KumpelConfig.Behaviour behaviour = KumpelSettings.get().behaviour();
		if (!behaviour.shareFood || kumpel.tickCount - lastLunch < COOLDOWN
				|| owner.getFoodData().getFoodLevel() > behaviour.shareFoodAtHunger) {
			return;
		}

		ItemStack food = kumpel.getPockets().takeOne(KumpelPockets::isFood);
		if (food.isEmpty()) {
			return;
		}

		lastLunch = kumpel.tickCount;
		Component name = food.getHoverName();
		owner.getInventory().add(food);
		if (!food.isEmpty()) {
			ItemEntity drop = new ItemEntity(level, owner.getX(), owner.getY() + 0.5, owner.getZ(), food);
			drop.setThrower(owner);
			level.addFreshEntity(drop);
		}

		kumpel.playSound(SoundEvents.ALLAY_ITEM_GIVEN, 0.8F, 1.2F);
		owner.sendOverlayMessage(Component.translatable("message.kumpel.lunch", kumpel.getDisplayName(), name));
	}
}
