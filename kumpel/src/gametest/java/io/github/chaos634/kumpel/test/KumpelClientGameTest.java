package io.github.chaos634.kumpel.test;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.registry.ModEntities;

/**
 * Renders Kumpels in a real client and takes screenshots, to check the model, textures and ore sensing.
 */
@SuppressWarnings("UnstableApiUsage")
public class KumpelClientGameTest implements FabricClientGameTest {
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
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 10");

			// One Kumpel of every level in a row, facing the camera.
			KumpelTier[] tiers = KumpelTier.values();
			for (int i = 0; i < tiers.length; i++) {
				server.runCommand("execute at @p run summon kumpel:kumpel ~%d ~ ~5 {NoAI:1b,Rotation:[180f,0f],experience:%d}"
						.formatted(-4 + i * 2, tiers[i].requiredExperience()));
			}

			// A close-up pair a bit further east: one standing, one sitting.
			server.runCommand("execute at @p run summon kumpel:kumpel ~19 ~ ~2.5 {NoAI:1b,Rotation:[160f,0f],experience:180}");
			server.runCommand("execute at @p run summon kumpel:kumpel ~21 ~ ~2.5 {NoAI:1b,Rotation:[200f,0f],Sitting:1b}");

			singleplayer.getConnection().waitForClientboundEntityUpdates(ModEntities.KUMPEL);
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("kumpel_tiers");

			server.runCommand("execute as @p at @s run tp @s ~20 ~ ~ 0 10");
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("kumpel_closeup");

			// End to end ore sensing: a diamond level Kumpel owned by the player, next to buried diamond ore.
			server.runCommand("execute as @p at @s run tp @s ~20 ~ ~ 0 10");
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				BlockPos origin = player.blockPosition();
				level.setBlockAndUpdate(origin.offset(3, -2, 4), Blocks.DIAMOND_ORE.defaultBlockState());

				KumpelEntity kumpel = new KumpelEntity(ModEntities.KUMPEL, level);
				kumpel.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 4.5, 180.0F, 0.0F);
				kumpel.addExperience(KumpelTier.DIAMOND.requiredExperience());
				kumpel.tame(player);
				level.addFreshEntity(kumpel);
			});
			context.waitTicks(15);
			context.takeScreenshot("kumpel_senses_ore");
		}
	}
}
