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
 * Vortrieb: digs the Kumpel's tunnel order slice by slice, upper block first. Water, lava and holes in the floor
 * are closed with stone from the backpack (Abdämmen); without stone it stops in front of them, and it also stops
 * when it meets a block its pickaxe can't handle.
 */
public class DigTunnelGoal extends Goal {
	private static final double REACH_SQ = 2.8 * 2.8;
	private static final int STUCK_TICKS = 200;
	private static final double STEP_IN_SQ = 2.0 * 2.0;
	private static final int SEAL_INTERVAL = 4;
	/** After sealing a leak, flowing water that is cut off needs a moment to run dry. */
	private static final int SETTLE_TICKS = 40;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private BlockPos target;
	private int progress;
	private int neededTicks;
	private int lastStage;
	private int ticksWithoutProgress;
	private int repathCooldown;
	private int sealCooldown;
	private int settleTicks;

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
		sealCooldown = 0;
		settleTicks = 0;
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
			BlockPos leak = kumpel.findLeakToSeal(level, lower);
			if (leak != null) {
				seal(level, order, leak);
			} else if (settleTicks > 0) {
				settleTicks--;
			} else {
				kumpel.stopTunnel(problem, lower);
			}
			return;
		}
		settleTicks = 0;

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
			walkToSlice(order);
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

	/** Abdämmen: walks up to the leak and closes it with a block from the backpack. */
	private void seal(ServerLevel level, TunnelOrder order, BlockPos leak) {
		clearTarget();
		kumpel.setMining(false);
		Vec3 center = Vec3.atCenterOf(leak);
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);

		if (kumpel.distanceToSqr(center) > REACH_SQ) {
			if (++ticksWithoutProgress > STUCK_TICKS) {
				kumpel.stopTunnel("stuck", leak);
				return;
			}
			walkToSlice(order);
			return;
		}

		kumpel.getNavigation().stop();
		if (--sealCooldown > 0) {
			return;
		}

		sealCooldown = SEAL_INTERVAL;
		if (kumpel.sealLeak(level, leak)) {
			ticksWithoutProgress = 0;
			settleTicks = SETTLE_TICKS;
		}
	}

	private void walkToSlice(TunnelOrder order) {
		Vec3 stand = Vec3.atBottomCenterOf(order.standPosition());
		if (kumpel.distanceToSqr(stand) < STEP_IN_SQ) {
			// The path finder counts "next to the spot" as arrived, which can be just out of reach: step onto it.
			kumpel.getNavigation().stop();
			kumpel.getMoveControl().setWantedPosition(stand.x, stand.y, stand.z, speedModifier);
			return;
		}
		if (--repathCooldown <= 0) {
			repathCooldown = adjustedTickDelay(10);
			kumpel.getNavigation().moveTo(stand.x, stand.y, stand.z, speedModifier);
		}
	}

	private void clearTarget() {
		if (target != null) {
			kumpel.level().destroyBlockProgress(kumpel.getId(), target, -1);
			target = null;
		}
	}
}
