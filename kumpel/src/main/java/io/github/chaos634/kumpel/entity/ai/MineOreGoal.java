package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Hauer mode: with a pickaxe in hand, the Kumpel walks up to exposed ores near its owner and mines them.
 * The drops land on the ground, where the Kumpel collects them as usual.
 */
public class MineOreGoal extends Goal {
	private static final double REACH_SQ = 2.8 * 2.8;
	private static final int GIVE_UP_TICKS = 200;
	private static final int IGNORE_UNREACHABLE_TICKS = 1200;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private final Map<BlockPos, Long> unreachable = new HashMap<>();
	private BlockPos target;
	private int searchCooldown;
	private int ticks;
	private int progress;
	private int neededTicks;
	private int lastStage;

	public MineOreGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (--searchCooldown > 0 || !kumpel.canMine()) {
			return false;
		}

		searchCooldown = adjustedTickDelay(20);
		long now = kumpel.level().getGameTime();
		unreachable.values().removeIf(until -> until < now);
		target = kumpel.findOreToMine(pos -> unreachable.containsKey(pos));
		return target != null;
	}

	@Override
	public boolean canContinueToUse() {
		return target != null && ticks < GIVE_UP_TICKS && kumpel.canMine() && kumpel.isMineableOre(target);
	}

	@Override
	public void start() {
		ticks = 0;
		progress = 0;
		lastStage = -1;
		neededTicks = kumpel.miningTicks(target);
	}

	@Override
	public void stop() {
		if (target != null) {
			kumpel.level().destroyBlockProgress(kumpel.getId(), target, -1);
			if (ticks >= GIVE_UP_TICKS) {
				giveUpOn(target);
			}
		}

		kumpel.setMining(false);
		kumpel.getNavigation().stop();
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ticks++;
		Vec3 center = Vec3.atCenterOf(target);
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);

		if (kumpel.distanceToSqr(center) > REACH_SQ) {
			kumpel.setMining(false);
			if (ticks % 20 == 1 && !kumpel.getNavigation().moveTo(center.x, target.getY(), center.z, speedModifier)) {
				giveUpOn(target);
				target = null;
			}
			return;
		}

		kumpel.getNavigation().stop();
		kumpel.setMining(true);
		progress++;

		ServerLevel level = (ServerLevel) kumpel.level();
		if (progress % 4 == 1) {
			BlockState state = level.getBlockState(target);
			SoundType sound = state.getSoundType();
			level.playSound(null, target, sound.getHitSound(), SoundSource.NEUTRAL, (sound.getVolume() + 1.0F) / 8.0F, sound.getPitch() * 0.5F);
		}

		int stage = progress * 10 / neededTicks;
		if (stage != lastStage) {
			level.destroyBlockProgress(kumpel.getId(), target, Math.min(stage, 9));
			lastStage = stage;
		}

		if (progress >= neededTicks) {
			level.destroyBlockProgress(kumpel.getId(), target, -1);
			kumpel.mineOre(level, target);
			target = null;
		}
	}

	private void giveUpOn(BlockPos pos) {
		unreachable.put(pos, kumpel.level().getGameTime() + IGNORE_UNREACHABLE_TICKS);
	}
}
