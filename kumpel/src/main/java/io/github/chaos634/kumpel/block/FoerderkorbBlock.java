package io.github.chaos634.kumpel.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.registry.ModSounds;

/**
 * Förderkorb: the cage of a mine shaft. Right-click rides up to the next cage above, sneak + right-click
 * (with an empty hand) rides down to the next one below. Your Kumpels nearby ride along.
 */
public class FoerderkorbBlock extends Block {
	/** How far up or down the next cage may be. */
	public static final int MAX_DISTANCE = 384;
	private static final double CREW_RANGE = 8.0;

	public FoerderkorbBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel) {
			ride(serverLevel, player, pos, !player.isSecondaryUseActive());
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Takes the player (and the Kumpels following them) to the next cage up or down the shaft.
	 *
	 * @return the cage they arrived at, or {@code null} if there is none
	 */
	public static BlockPos ride(ServerLevel level, Player player, BlockPos from, boolean up) {
		BlockPos target = findCage(level, from, up);
		if (target == null) {
			player.sendOverlayMessage(Component.translatable(up ? "message.kumpel.foerderkorb.none_above" : "message.kumpel.foerderkorb.none_below"));
			return null;
		}

		List<KumpelEntity> crew = KumpelEntity.findOwnedBy(level, player, CREW_RANGE).stream()
				.filter(kumpel -> !kumpel.isOrderedToSit())
				.toList();
		double x = target.getX() + 0.5;
		double y = target.getY() + 1.0;
		double z = target.getZ() + 0.5;
		SoundEvent signal = up ? ModSounds.FOERDERKORB_UP : ModSounds.FOERDERKORB_DOWN;

		level.playSound(null, from, signal, SoundSource.BLOCKS, 1.0F, 1.0F);
		player.teleportTo(x, y, z);
		for (KumpelEntity kumpel : crew) {
			kumpel.getNavigation().stop();
			kumpel.teleportTo(x, y, z);
		}
		level.playSound(null, target, signal, SoundSource.BLOCKS, 1.0F, 1.0F);

		player.sendOverlayMessage(Component.translatable(up ? "message.kumpel.foerderkorb.up" : "message.kumpel.foerderkorb.down",
				Math.abs(target.getY() - from.getY()), crew.size()));
		KumpelAdvancements.award(player, KumpelAdvancements.SEILFAHRT);
		return target;
	}

	/** The nearest cage above or below with room for a player on top. */
	public static BlockPos findCage(ServerLevel level, BlockPos from, boolean up) {
		BlockPos.MutableBlockPos cursor = from.mutable();
		for (int i = 1; i <= MAX_DISTANCE; i++) {
			cursor.move(0, up ? 1 : -1, 0);
			if (level.isOutsideBuildHeight(cursor)) {
				return null;
			}
			if (level.getBlockState(cursor).getBlock() instanceof FoerderkorbBlock && hasRoom(level, cursor.above())) {
				return cursor.immutable();
			}
		}

		return null;
	}

	private static boolean hasRoom(ServerLevel level, BlockPos feet) {
		return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
	}
}
