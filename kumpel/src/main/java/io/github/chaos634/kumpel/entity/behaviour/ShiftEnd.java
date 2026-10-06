package io.github.chaos634.kumpel.entity.behaviour;

import net.minecraft.world.entity.player.Player;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Feierabend: when its owner goes to bed, the Kumpel sits down nearby; when they get up, it gets back to work.
 */
public class ShiftEnd {
	private static final double MAX_DISTANCE_SQ = 32.0 * 32.0;

	private boolean resting;

	public void tick(KumpelEntity kumpel, Player owner) {
		if (!KumpelSettings.get().behaviour().restWhenOwnerSleeps) {
			return;
		}

		if (owner.isSleeping()) {
			if (!resting && !kumpel.isOrderedToSit() && kumpel.distanceToSqr(owner) < MAX_DISTANCE_SQ) {
				resting = true;
				kumpel.setOrderedToSit(true);
				kumpel.getNavigation().stop();
				KumpelAdvancements.award(owner, KumpelAdvancements.SHIFT_END);
			}
		} else if (resting) {
			resting = false;
			kumpel.setOrderedToSit(false);
		}
	}

	public boolean isResting() {
		return resting;
	}

	public void setResting(boolean resting) {
		this.resting = resting;
	}
}
