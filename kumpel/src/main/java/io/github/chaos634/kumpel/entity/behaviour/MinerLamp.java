package io.github.chaos634.kumpel.entity.behaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;

/**
 * Grubenlampe: underground, the Kumpel places a torch from its backpack wherever it gets too dark.
 */
public final class MinerLamp {
	private MinerLamp() {
	}

	/** @return whether a torch was placed */
	public static boolean tryPlaceTorch(KumpelEntity kumpel, ServerLevel level) {
		KumpelSettings settings = KumpelSettings.get();
		KumpelConfig.Behaviour behaviour = settings.behaviour();
		// In a shaft, the ladders need the room.
		if (!behaviour.placeTorches || !kumpel.onGround() || kumpel.isInWater() || kumpel.getShaft() != null) {
			return false;
		}

		BlockPos pos = kumpel.blockPosition();
		if (!level.getBlockState(pos).isAir() || level.canSeeSky(pos)
				|| level.getBrightness(LightLayer.BLOCK, pos) >= behaviour.torchLightLevel) {
			return false;
		}

		KumpelPockets pockets = kumpel.getPockets();
		for (int i = 0; i < pockets.getContainerSize(); i++) {
			ItemStack stack = pockets.getItem(i);
			Block torch = settings.torchBlock(stack);
			if (torch == null) {
				continue;
			}

			BlockState state = torch.defaultBlockState();
			if (!state.canSurvive(level, pos)) {
				return false;
			}

			level.setBlock(pos, state, Block.UPDATE_ALL);
			stack.shrink(1);
			pockets.setChanged();
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, kumpel.getSoundSource(), 1.0F, 1.0F);
			return true;
		}

		return false;
	}
}
