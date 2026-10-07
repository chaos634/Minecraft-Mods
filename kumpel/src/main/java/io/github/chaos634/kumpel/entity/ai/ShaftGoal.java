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
import io.github.chaos634.kumpel.entity.ShaftOrder;

/**
 * Abteufen: sinks the Kumpel's shaft, one block at a time, digging the block under its feet and dropping into the hole.
 * Ladders go up the wall behind it; water and lava next to the shaft are sealed off with stone from its backpack.
 * When it would break into a cave, it stops: Durchschlag!
 */
public class ShaftGoal extends Goal {
	private static final int SEAL_INTERVAL = 4;
	/** After sealing a leak, cut-off flowing water needs a moment to run dry. */
	private static final int SETTLE_TICKS = 40;

	private final KumpelEntity kumpel;
	private BlockPos target;
	private int progress;
	private int neededTicks;
	private int lastStage;
	private int sealCooldown;
	private int settleTicks;

	public ShaftGoal(KumpelEntity kumpel) {
		this.kumpel = kumpel;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return kumpel.canDigShaft();
	}

	@Override
	public boolean canContinueToUse() {
		return kumpel.canDigShaft();
	}

	@Override
	public void start() {
		target = null;
		sealCooldown = 0;
		settleTicks = 0;
	}

	@Override
	public void stop() {
		clearTarget();
		kumpel.setMining(false);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		ServerLevel level = (ServerLevel) kumpel.level();
		ShaftOrder order = kumpel.getShaft();
		if (order == null) {
			return;
		}
		if (order.isDone()) {
			kumpel.finishShaft(level);
			return;
		}

		BlockPos cell = order.currentCell();
		BlockPos stand = order.standPosition();
		if (!kumpel.blockPosition().equals(stand)) {
			boolean dropping = kumpel.getBlockX() == stand.getX() && kumpel.getBlockZ() == stand.getZ() && kumpel.getY() > stand.getY();
			if (!dropping) {
				// Back to the work, the miner's way: by rope.
				Vec3 feet = Vec3.atBottomCenterOf(stand);
				kumpel.getNavigation().stop();
				kumpel.snapTo(feet.x, feet.y, feet.z, kumpel.getYRot(), kumpel.getXRot());
			}
			return;
		}
		kumpel.getNavigation().stop();
		if (order.progress() > 0) {
			kumpel.placeShaftLadder(level, stand, order.ladderSide());
		}

		BlockPos leak = kumpel.findShaftLeak(level, cell);
		if (leak != null) {
			clearTarget();
			kumpel.setMining(false);
			if (--sealCooldown <= 0) {
				sealCooldown = SEAL_INTERVAL;
				if (kumpel.sealLeak(level, leak)) {
					settleTicks = SETTLE_TICKS;
				} else {
					kumpel.stopShaft(level.getFluidState(leak).is(net.minecraft.tags.FluidTags.LAVA) ? "lava" : "water", leak);
				}
			}
			return;
		}
		if (settleTicks > 0) {
			settleTicks--;
			return;
		}

		String problem = kumpel.shaftProblem(level, cell);
		if (problem != null) {
			kumpel.stopShaft(problem, cell);
			return;
		}

		if (!cell.equals(target)) {
			clearTarget();
			target = cell;
			progress = 0;
			lastStage = -1;
			neededTicks = kumpel.miningTicks(cell);
		}

		kumpel.getLookControl().setLookAt(cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5);
		kumpel.setMining(true);
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
			kumpel.advanceShaft();
		}
	}

	private void clearTarget() {
		if (target != null) {
			kumpel.level().destroyBlockProgress(kumpel.getId(), target, -1);
			target = null;
		}
	}
}
