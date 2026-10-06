package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.TunnelOrder;

/**
 * Vortrieb: digs the Kumpel's tunnel order slice by slice, upper block first. It stops in front of water, lava
 * and drops, and when it meets a block its pickaxe can't handle.
 */
public class DigTunnelGoal extends Goal {
	private static final double REACH_SQ = 2.8 * 2.8;
	private static final int STUCK_TICKS = 200;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private BlockPos target;
	private int progress;
	private int neededTicks;
	private int lastStage;
	private int ticksWithoutProgress;
	private int repathCooldown;

	public DigTunnelGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return kumpel.canDigTunnel();
	}

	@Override
	public boolean canContinueToUse() {
		return kumpel.canDigTunnel();
	}

	@Override
	public void start() {
		target = null;
		ticksWithoutProgress = 0;
		repathCooldown = 0;
	}

	@Override
	public void stop() {
		clearTarget();
		kumpel.setMining(false);
		kumpel.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ServerLevel level = (ServerLevel) kumpel.level();
		TunnelOrder order = kumpel.getTunnel();
		if (order == null) {
			return;
		}
		if (order.isDone()) {
			kumpel.finishTunnel();
			return;
		}

		BlockPos lower = order.currentSlice();
		BlockPos upper = lower.above();
		String problem = kumpel.tunnelProblem(level, lower);
		if (problem != null) {
			kumpel.stopTunnel(problem, lower);
			return;
		}

		BlockPos next = !kumpel.isOpen(upper) ? upper : (!kumpel.isOpen(lower) ? lower : null);
		if (next == null) {
			clearTarget();
			kumpel.advanceTunnel();
			ticksWithoutProgress = 0;
			return;
		}
		if (!kumpel.isDiggable(next)) {
			kumpel.stopTunnel("blocked", next);
			return;
		}
		if (!next.equals(target)) {
			clearTarget();
			target = next;
			progress = 0;
			lastStage = -1;
			neededTicks = kumpel.miningTicks(next);
		}

		Vec3 center = Vec3.atCenterOf(target);
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);

		if (kumpel.distanceToSqr(center) > REACH_SQ) {
			kumpel.setMining(false);
			if (++ticksWithoutProgress > STUCK_TICKS) {
				kumpel.stopTunnel("stuck", target);
				return;
			}
			if (--repathCooldown <= 0) {
				repathCooldown = adjustedTickDelay(10);
				BlockPos stand = order.standPosition();
				kumpel.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, speedModifier);
			}
			return;
		}

		kumpel.getNavigation().stop();
		kumpel.setMining(true);
		ticksWithoutProgress = 0;
		progress++;

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
			kumpel.digBlock(level, target);
			target = null;
		}
	}

	private void clearTarget() {
		if (target != null) {
			kumpel.level().destroyBlockProgress(kumpel.getId(), target, -1);
			target = null;
		}
	}
}
