package io.github.chaos634.kumpel.entity.behaviour;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;
import io.github.chaos634.kumpel.entity.ShiftLog;
import io.github.chaos634.kumpel.entity.TunnelOrder;

/**
 * Streckenausbau: every few blocks of its tunnel, the digger sets a support frame (a Türstock): a post on either side
 * and a cap across the top, made of logs or steel girders from its backpack. If it carries lamps, one of the posts
 * gets a lamp on top instead, on alternating sides, so the tunnel is lit as well.
 *
 * <p>The frame takes the place of the rock around the tunnel; what the Kumpel digs out for it goes into its backpack.
 */
public final class TunnelSupports {
	private static final int LEFT_FOOT = 0;
	private static final int RIGHT_FOOT = 1;
	private static final int LEFT_SHOULDER = 2;
	private static final int RIGHT_SHOULDER = 3;
	private static final int CAP = 4;

	private TunnelSupports() {
	}

	private static KumpelConfig.Behaviour behaviour() {
		return KumpelSettings.get().behaviour();
	}

	/** Is a frame due at the order's current slice? */
	public static boolean isDue(TunnelOrder order) {
		int interval = behaviour().supportInterval;
		return behaviour().tunnelSupports && interval > 0 && (order.progress() + 1) % interval == 0;
	}

	/** The parts of the frame at the order's current slice: the feet and shoulders of both posts, then the cap. */
	public static List<BlockPos> pieces(TunnelOrder order) {
		BlockPos lower = order.currentSlice();
		BlockPos left = lower.relative(order.direction().getCounterClockWise());
		BlockPos right = lower.relative(order.direction().getClockWise());
		return List.of(left, right, left.above(), right.above(), lower.above(2));
	}

	/**
	 * @return the next part of the current slice's frame to set, or {@code null} if the frame is done, or the Kumpel has
	 *         nothing to build it from, or no frame is due here
	 */
	public static BlockPos nextPiece(KumpelEntity kumpel, ServerLevel level, TunnelOrder order) {
		if (!isDue(order) || kumpel.getPockets().count(KumpelPockets::isTunnelSupport) == 0) {
			return null;
		}

		for (BlockPos piece : pieces(order)) {
			if (canSet(kumpel, level, piece)) {
				return piece;
			}
		}

		return null;
	}

	/** A part of a frame: a support or a lamp. */
	public static boolean isFramePart(BlockState state) {
		return is(state, KumpelPockets.TUNNEL_SUPPORTS) || is(state, KumpelPockets.TUNNEL_LAMPS);
	}

	private static boolean is(BlockState state, TagKey<Item> tag) {
		return state.getBlock().asItem().getDefaultInstance().is(tag);
	}

	/** Not done yet, no fluid that could break in, nobody standing there, and either open or rock the Kumpel can dig. */
	private static boolean canSet(KumpelEntity kumpel, ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (isFramePart(state) || !state.getFluidState().isEmpty()) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			if (!level.getFluidState(pos.relative(direction)).isEmpty()) {
				return false;
			}
		}
		if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
			return false;
		}

		return state.canBeReplaced() || kumpel.isDiggable(pos);
	}

	/**
	 * Sets one part of the frame, digging out the rock that is there first.
	 *
	 * @return whether something was set
	 */
	public static boolean setPiece(KumpelEntity kumpel, ServerLevel level, TunnelOrder order, BlockPos pos) {
		List<BlockPos> pieces = pieces(order);
		int index = pieces.indexOf(pos);
		if (index < 0) {
			return false;
		}

		BlockState placed = index == lampSpot(order) ? takeLamp(kumpel, level, pos) : null;
		if (placed == null) {
			ItemStack support = kumpel.getPockets().takeOne(KumpelPockets::isTunnelSupport);
			if (!(support.getItem() instanceof BlockItem blockItem)) {
				return false;
			}
			placed = blockItem.getBlock().defaultBlockState();
			if (placed.hasProperty(BlockStateProperties.AXIS)) {
				// Posts stand upright; the cap lies across the tunnel.
				placed = placed.setValue(BlockStateProperties.AXIS, index == CAP ? order.direction().getClockWise().getAxis() : Direction.Axis.Y);
			}
		}

		if (!level.getBlockState(pos).canBeReplaced()) {
			kumpel.digBlock(level, pos);
		}
		level.setBlockAndUpdate(pos, placed);
		SoundType sound = placed.getSoundType();
		level.playSound(null, pos, sound.getPlaceSound(), SoundSource.NEUTRAL, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

		if (pieces.stream().allMatch(piece -> isFramePart(level.getBlockState(piece)))) {
			kumpel.record(ShiftLog.Entry.SUPPORTS_SET);
			KumpelAdvancements.award(kumpel.getOwner(), KumpelAdvancements.STRECKENAUSBAU);
		}
		return true;
	}

	/** Lamps go on the left post of every other frame and on the right post of the rest. */
	private static int lampSpot(TunnelOrder order) {
		int frame = (order.progress() + 1) / Math.max(1, behaviour().supportInterval);
		return frame % 2 == 1 ? LEFT_SHOULDER : RIGHT_SHOULDER;
	}

	/** Takes a lamp from the backpack, if it has one that can stand here (on top of the post's foot). */
	private static BlockState takeLamp(KumpelEntity kumpel, ServerLevel level, BlockPos pos) {
		KumpelPockets pockets = kumpel.getPockets();
		for (int i = 0; i < pockets.getContainerSize(); i++) {
			ItemStack stack = pockets.getItem(i);
			if (KumpelPockets.isTunnelLamp(stack) && stack.getItem() instanceof BlockItem blockItem) {
				BlockState lamp = blockItem.getBlock().defaultBlockState();
				if (!lamp.canSurvive(level, pos)) {
					return null;
				}
				stack.shrink(1);
				pockets.setChanged();
				return lamp;
			}
		}

		return null;
	}
}
