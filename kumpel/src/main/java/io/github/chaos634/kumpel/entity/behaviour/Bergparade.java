package io.github.chaos634.kumpel.entity.behaviour;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * Bergparade: while their owner holds the Steigerhäckel, Kumpels march behind them in single file,
 * the most experienced first, each one following the one before it.
 */
public final class Bergparade {
	/** How far away Kumpels still join the parade. */
	public static final double RANGE = 24.0;
	/** A parade this long earns the advancement. */
	public static final int PROPER_PARADE = 3;

	private Bergparade() {
	}

	/** Is the owner leading a parade? */
	public static boolean isLeading(Player owner) {
		return KumpelSettings.get().behaviour().bergparade
				&& (owner.getMainHandItem().is(ModItems.STEIGERHAECKEL) || owner.getOffhandItem().is(ModItems.STEIGERHAECKEL));
	}

	/** Whether this Kumpel can march along right now (not sitting, not digging a tunnel or building, not leading the way out). */
	public static boolean canMarch(KumpelEntity kumpel) {
		return kumpel.isAlive() && kumpel.isTame() && !kumpel.isOrderedToSit() && kumpel.getTunnel() == null && kumpel.getBuild() == null
				&& !kumpel.isLeadingOut();
	}

	/** The marchers in order: the most experienced first, ties broken the same way every time. */
	public static List<KumpelEntity> column(ServerLevel level, Player owner) {
		List<KumpelEntity> column = new ArrayList<>(KumpelEntity.findOwnedBy(level, owner, RANGE));
		column.removeIf(kumpel -> !canMarch(kumpel));
		column.sort(Comparator.comparingInt(KumpelEntity::getExperience).reversed().thenComparing(Entity::getStringUUID));
		return column;
	}

	/** Who this Kumpel marches behind: the one before it in the column, or the owner at the head. */
	public static LivingEntity leaderOf(KumpelEntity kumpel, List<KumpelEntity> column, Player owner) {
		int index = column.indexOf(kumpel);
		return index <= 0 ? owner : column.get(index - 1);
	}
}
