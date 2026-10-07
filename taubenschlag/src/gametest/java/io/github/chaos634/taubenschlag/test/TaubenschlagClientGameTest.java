package io.github.chaos634.taubenschlag.test;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.chaos634.taubenschlag.block.TaubenschlagBlock;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;
import io.github.chaos634.taubenschlag.entity.BrieftaubeVariant;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;

/**
 * An allotment with two pigeon lofts, one up on posts, and pigeons of every plumage around them, seen by a player
 * holding a travel basket. Takes a screenshot of it.
 */
@SuppressWarnings("UnstableApiUsage")
public class TaubenschlagClientGameTest implements FabricClientGameTest {
	/** Chat messages (like command feedback) fade out after 200 ticks. */
	private static final int CHAT_FADE_TICKS = 220;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 15");
			server.runCommand("item replace entity @p weapon.mainhand with taubenschlag:reisekorb");
			singleplayer.getConnection().waitForChunksRender();
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				buildAllotment(level, player, player.blockPosition());
			});
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("taubenschlag_garten");
		}
	}

	private static void buildAllotment(ServerLevel level, ServerPlayer player, BlockPos origin) {
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, 0, -1), origin.offset(7, 6, 10))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, -1, -1), origin.offset(7, -1, 10))) {
			level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
		}

		BlockState loft = TaubenschlagBlocks.TAUBENSCHLAG.defaultBlockState().setValue(TaubenschlagBlock.FACING, Direction.NORTH);
		// The big loft, up on two posts like on the allotments in the Ruhr.
		level.setBlockAndUpdate(origin.offset(-1, 0, 6), Blocks.OAK_FENCE.defaultBlockState());
		level.setBlockAndUpdate(origin.offset(-1, 1, 6), Blocks.OAK_FENCE.defaultBlockState());
		level.setBlockAndUpdate(origin.offset(-1, 2, 6), loft);
		// A second one on the ground beside it.
		level.setBlockAndUpdate(origin.offset(2, 0, 6), loft);

		// A low fence round the garden, a patch of wheat and some flowers.
		for (int x = -6; x <= 6; x++) {
			level.setBlockAndUpdate(origin.offset(x, 0, 9), Blocks.OAK_FENCE.defaultBlockState());
		}
		for (int x = 4; x <= 6; x++) {
			for (int z = 4; z <= 7; z++) {
				level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(origin.offset(x, 0, z), Blocks.WHEAT.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(origin.offset(-4, 0, 5), Blocks.POPPY.defaultBlockState());
		level.setBlockAndUpdate(origin.offset(-5, 0, 6), Blocks.DANDELION.defaultBlockState());
		level.setBlockAndUpdate(origin.offset(-4, 0, 7), Blocks.CORNFLOWER.defaultBlockState());

		// Pigeons of every plumage: on the roof, on the landing board, on the ground.
		spawnPigeon(level, player, origin.getX() - 0.5, origin.getY() + 3.0, origin.getZ() + 6.6, 160.0F, BrieftaubeVariant.BLAU);
		spawnPigeon(level, player, origin.getX() - 0.75, origin.getY() + 2.25, origin.getZ() + 6.1, 200.0F, BrieftaubeVariant.WEISS);
		spawnPigeon(level, player, origin.getX() + 2.5, origin.getY() + 1.0, origin.getZ() + 6.6, 180.0F, BrieftaubeVariant.ROT);
		spawnPigeon(level, player, origin.getX() + 0.8, origin.getY(), origin.getZ() + 4.2, 140.0F, BrieftaubeVariant.GEHAEMMERT);
		spawnPigeon(level, player, origin.getX() + 1.6, origin.getY(), origin.getZ() + 3.4, 220.0F, BrieftaubeVariant.BLAU);
	}

	private static void spawnPigeon(ServerLevel level, ServerPlayer owner, double x, double y, double z, float yRot, BrieftaubeVariant variant) {
		BrieftaubeEntity pigeon = TaubenschlagEntities.BRIEFTAUBE.create(level, EntitySpawnReason.COMMAND);
		if (pigeon == null) {
			throw new IllegalStateException("Could not create a pigeon");
		}

		pigeon.snapTo(x, y, z, yRot, 0.0F);
		pigeon.setYHeadRot(yRot);
		pigeon.setYBodyRot(yRot);
		pigeon.setVariant(variant);
		pigeon.tameBy(owner);
		pigeon.setNoAi(true);
		level.addFreshEntity(pigeon);
	}
}
