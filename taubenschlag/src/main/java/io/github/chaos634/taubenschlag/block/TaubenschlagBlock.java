package io.github.chaos634.taubenschlag.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Taubenschlag: a pigeon loft, a little wooden house with a landing board in front. Tame pigeons nearby settle into it
 * and fly back to it from wherever they are let go. Its nine nest boxes take the post they bring; a comparator reads
 * how full they are.
 */
public class TaubenschlagBlock extends HorizontalDirectionalBlock implements EntityBlock {
	/** The parts of the model when the loft faces north, in pixels: house, roof, ridge, landing board. */
	private static final double[][] BOXES = {
			{1, 0, 2, 15, 11, 15},
			{0, 11, 1, 16, 13, 16},
			{2, 13, 3, 14, 15, 14},
			{2, 3, 0, 14, 4, 2},
	};
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] box : BOXES) {
				shape = Shapes.or(shape, turned(direction, box[0], box[1], box[2], box[3], box[4], box[5]));
			}
			SHAPES.put(direction, shape);
		}
	}

	public TaubenschlagBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** A box given for a loft facing north, turned clockwise (seen from above) like the block models. */
	private static VoxelShape turned(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
		return switch (facing) {
			case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
			case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
			case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
			default -> Block.box(x1, y1, z1, x2, y2, z2);
		};
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
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TaubenschlagBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TaubenschlagBlockEntity schlag) {
			player.openMenu(schlag);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof TaubenschlagBlockEntity schlag ? schlag.signal() : 0;
	}
}
