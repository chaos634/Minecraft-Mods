package io.github.chaos634.kumpel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * The light of a Kumpel's helmet lamp: an invisible block at the Kumpel's head that gives off light.
 * It checks every few ticks whether its Kumpel is still there and removes itself once it is not,
 * so no stray light is ever left behind, not even after a crash or a chunk unload.
 */
public class LampLightBlock extends Block {
	public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
	private static final int CHECK_TICKS = 5;

	public LampLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 7));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		level.scheduleTick(pos, this, CHECK_TICKS);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (KumpelEntity.wearsLampAt(level, pos)) {
			level.scheduleTick(pos, this, CHECK_TICKS);
		} else {
			level.removeBlock(pos, false);
		}
	}
}
