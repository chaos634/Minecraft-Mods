package io.github.chaos634.kumpel.entity.ai;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.build.Bauplan;
import io.github.chaos634.kumpel.build.BuildOrder;
import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Baumeister: puts up the Kumpel's building piece by piece, bottom up, with blocks from its Kiepe. It walks close to each
 * piece (a Kumpel can reach any height right above it), clears what is in the way and asks its owner for blocks it lacks.
 */
public class BuildGoal extends Goal {
	private static final double REACH_SQ = 3.5 * 3.5;
	private static final int PLACE_INTERVAL = 6;
	private static final int WAIT_INTERVAL = 40;
	/** After this long without getting close, it places the block from where it is. */
	private static final int STUCK_TICKS = 160;
	/** Pieces already in place are skipped this many at a time. */
	private static final int SKIP_PER_TICK = 32;

	private final KumpelEntity kumpel;
	private final double speedModifier;
	private ResourceKey<Bauplan> cachedPlan;
	private List<Bauplan.Piece> pieces;
	private int cooldown;
	private int ticksWalking;

	public BuildGoal(KumpelEntity kumpel, double speedModifier) {
		this.kumpel = kumpel;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return kumpel.canBuild();
	}

	@Override
	public boolean canContinueToUse() {
		return kumpel.canBuild();
	}

	@Override
	public void start() {
		cooldown = 0;
		ticksWalking = 0;
	}

	@Override
	public void stop() {
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
		BuildOrder order = kumpel.getBuild();
		Bauplan bauplan = order == null ? null : level.registryAccess().lookupOrThrow(Bauplan.REGISTRY).getValue(order.plan());
		if (bauplan == null) {
			kumpel.stopBuilding();
			return;
		}
		if (!order.plan().equals(cachedPlan)) {
			cachedPlan = order.plan();
			pieces = bauplan.pieces();
		}

		int progress = order.progress();
		for (int skipped = 0; progress < pieces.size() && skipped < SKIP_PER_TICK && isDone(level, order, bauplan, pieces.get(progress)); skipped++) {
			progress++;
		}
		if (progress != order.progress()) {
			kumpel.setBuildProgress(progress);
			ticksWalking = 0;
		}
		if (progress >= pieces.size()) {
			kumpel.finishBuilding(bauplan);
			return;
		}

		Bauplan.Piece piece = pieces.get(progress);
		BlockPos pos = order.worldPos(bauplan, piece);
		BlockState target = order.worldState(piece);
		Item item = itemFor(target);
		if (item == Items.AIR) {
			// Nothing to take it from (like fire): leave it out.
			kumpel.setBuildProgress(progress + 1);
			return;
		}

		if (item != null && kumpel.getPockets().count(stack -> stack.is(item)) == 0) {
			kumpel.setMining(false);
			kumpel.getNavigation().stop();
			if (--cooldown <= 0) {
				cooldown = WAIT_INTERVAL;
				kumpel.askForBuildingBlocks(bauplan, order, item, remaining(order, bauplan, item, progress, level));
			}
			return;
		}

		Vec3 center = Vec3.atCenterOf(pos);
		double dx = center.x - kumpel.getX();
		double dz = center.z - kumpel.getZ();
		if (dx * dx + dz * dz > REACH_SQ && ticksWalking < STUCK_TICKS) {
			kumpel.setMining(false);
			if (ticksWalking++ % 10 == 0) {
				kumpel.getNavigation().moveTo(center.x, order.origin().getY(), center.z, speedModifier);
			}
			return;
		}

		kumpel.getNavigation().stop();
		kumpel.getLookControl().setLookAt(center.x, center.y, center.z);
		List<LivingEntity> inTheWay = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos));
		if (!inTheWay.isEmpty()) {
			if (inTheWay.contains(kumpel)) {
				// Standing where the block goes: step out to the front.
				Vec3 yard = Vec3.atBottomCenterOf(order.frontYard());
				kumpel.getNavigation().moveTo(yard.x, yard.y, yard.z, speedModifier);
			}
			return;
		}

		kumpel.setMining(true);
		if (--cooldown > 0) {
			return;
		}
		cooldown = PLACE_INTERVAL;
		kumpel.placeBuildingBlock(level, pos, target, item);
		kumpel.setBuildProgress(progress + 1);
		ticksWalking = 0;
	}

	/** The item a piece is made from; {@code null} for the upper half of a door, which comes with the lower one. */
	private static Item itemFor(BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
			return null;
		}
		return state.getBlock().asItem();
	}

	/** Already there, or something the Kumpel can't (or mustn't) clear is in the way. */
	private boolean isDone(ServerLevel level, BuildOrder order, Bauplan bauplan, Bauplan.Piece piece) {
		BlockPos pos = order.worldPos(bauplan, piece);
		BlockState current = level.getBlockState(pos);
		return current.is(piece.state().getBlock()) || (!current.canBeReplaced() && !kumpel.isDiggable(pos));
	}

	/** How many more of this block the building still needs. */
	private int remaining(BuildOrder order, Bauplan bauplan, Item item, int from, ServerLevel level) {
		int count = 0;
		for (int i = from; i < pieces.size(); i++) {
			Bauplan.Piece piece = pieces.get(i);
			if (itemFor(piece.state()) == item && !isDone(level, order, bauplan, piece)) {
				count++;
			}
		}
		return count;
	}
}
