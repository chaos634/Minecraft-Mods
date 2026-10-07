package io.github.chaos634.kumpel.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Abteufen: a shaft to sink, straight down from {@code top} (the first block to dig), {@code depth} blocks deep.
 * Ladders go on the wall towards {@code ladderSide}; a lift, if there is one, on the other side.
 */
public record ShaftOrder(BlockPos top, Direction ladderSide, int depth, int progress) {
	public static final Codec<ShaftOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("top").forGetter(ShaftOrder::top),
			Direction.CODEC.fieldOf("ladder_side").forGetter(ShaftOrder::ladderSide),
			Codec.INT.fieldOf("depth").forGetter(ShaftOrder::depth),
			Codec.INT.fieldOf("progress").forGetter(ShaftOrder::progress)
	).apply(instance, ShaftOrder::new));

	/** The block dug at the given step. */
	public BlockPos cell(int index) {
		return top.below(index);
	}

	public BlockPos currentCell() {
		return cell(progress);
	}

	/** Where the Kumpel stands to dig the current cell: right on top of it. */
	public BlockPos standPosition() {
		return progress == 0 ? top.above() : cell(progress - 1);
	}

	/** The side of the shaft for the Förderkorb: opposite the ladders. */
	public Direction liftSide() {
		return ladderSide.getOpposite();
	}

	public boolean isDone() {
		return progress >= depth;
	}

	public ShaftOrder advance() {
		return new ShaftOrder(top, ladderSide, depth, progress + 1);
	}
}
