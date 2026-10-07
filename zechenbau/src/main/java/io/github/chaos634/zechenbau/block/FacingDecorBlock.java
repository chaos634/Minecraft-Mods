package io.github.chaos634.zechenbau.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A decorative block that faces the player who places it, with a box shape that turns along with its model.
 * The shape is given for a block facing north, in pixels.
 */
public class FacingDecorBlock extends HorizontalDirectionalBlock {
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

	public FacingDecorBlock(Properties properties, double x1, double y1, double z1, double x2, double y2, double z2) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		// Turned clockwise (seen from above) like the block models: north -> east -> south -> west.
		shapes.put(Direction.NORTH, Block.box(x1, y1, z1, x2, y2, z2));
		shapes.put(Direction.EAST, Block.box(16 - z2, y1, x1, 16 - z1, y2, x2));
		shapes.put(Direction.SOUTH, Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1));
		shapes.put(Direction.WEST, Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}
}
