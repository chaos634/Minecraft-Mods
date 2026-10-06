package io.github.chaos634.kumpel.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Vortrieb: a 1×2 tunnel to dig, slice by slice. Slice {@code i} is the floor-level block {@code i} steps from the start
 * in the given direction, plus the block above it.
 */
public record TunnelOrder(BlockPos start, Direction direction, int length, int progress) {
	public static final Codec<TunnelOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("start").forGetter(TunnelOrder::start),
			Direction.CODEC.fieldOf("direction").forGetter(TunnelOrder::direction),
			Codec.INT.fieldOf("length").forGetter(TunnelOrder::length),
			Codec.INT.fieldOf("progress").forGetter(TunnelOrder::progress)
	).apply(instance, TunnelOrder::new));

	/** The floor-level block of a slice. */
	public BlockPos slice(int index) {
		return start.relative(direction, index);
	}

	public BlockPos currentSlice() {
		return slice(progress);
	}

	/** Where the Kumpel stands to dig the current slice: in the slice before it. */
	public BlockPos standPosition() {
		return progress == 0 ? start.relative(direction.getOpposite()) : slice(progress - 1);
	}

	public boolean isDone() {
		return progress >= length;
	}

	public TunnelOrder advance() {
		return new TunnelOrder(start, direction, length, progress + 1);
	}
}
