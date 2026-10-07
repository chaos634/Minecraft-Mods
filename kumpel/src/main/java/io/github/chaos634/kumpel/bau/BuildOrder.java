package io.github.chaos634.kumpel.bau;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Baumeister: a building to put up. The plan's front row starts at {@code origin} and the building extends away from
 * whoever ordered it, turned by {@code rotation}; {@code progress} counts the pieces already dealt with.
 */
public record BuildOrder(ResourceKey<Bauplan> plan, BlockPos origin, Rotation rotation, int progress) {
	public static final Codec<BuildOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceKey.codec(Bauplan.REGISTRY).fieldOf("plan").forGetter(BuildOrder::plan),
			BlockPos.CODEC.fieldOf("origin").forGetter(BuildOrder::origin),
			Rotation.CODEC.fieldOf("rotation").forGetter(BuildOrder::rotation),
			Codec.INT.fieldOf("progress").forGetter(BuildOrder::progress)
	).apply(instance, BuildOrder::new));

	/** Plans are written for someone looking south; this turns them to whoever looks the given way. */
	public static Rotation rotationFor(Direction facing) {
		return switch (facing) {
			case WEST -> Rotation.CLOCKWISE_90;
			case NORTH -> Rotation.CLOCKWISE_180;
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	/** Where a piece goes: centred on the origin from left to right, and away from the one who ordered it. */
	public BlockPos worldPos(Bauplan bauplan, Bauplan.Piece piece) {
		// Looking south, "right" is west (-x).
		BlockPos local = new BlockPos(bauplan.width() / 2 - piece.x(), piece.y(), piece.z());
		return origin.offset(local.rotate(rotation));
	}

	public BlockState worldState(Bauplan.Piece piece) {
		return piece.state().rotate(rotation);
	}

	/** A spot in front of the building, where the builder can step out of the way. */
	public BlockPos frontYard() {
		return origin.offset(new BlockPos(0, 0, -2).rotate(rotation));
	}

	public BuildOrder withProgress(int progress) {
		return new BuildOrder(plan, origin, rotation, progress);
	}
}
