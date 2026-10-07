package io.github.chaos634.pottkueche.test;

import java.util.List;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.chaos634.pottkueche.registry.PottkuecheItems;

/**
 * The wall of a Bude (a Ruhr snack bar): every dish of the mod in an item frame, between two lanterns, with
 * campfires smoking on either side. Takes a screenshot of it.
 */
@SuppressWarnings("UnstableApiUsage")
public class PottkuecheClientGameTest implements FabricClientGameTest {
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
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 12");
			singleplayer.getConnection().waitForChunksRender();
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				buildBude(level, singleplayer.getConnection().getServerPlayer().blockPosition());
			});
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("pottkueche_bude");
		}
	}

	private static void buildBude(ServerLevel level, BlockPos origin) {
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6, 0, -2), origin.offset(6, 6, 6))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		for (int x = -5; x <= 5; x++) {
			for (int y = 0; y <= 4; y++) {
				level.setBlockAndUpdate(origin.offset(x, y, 4), (y == 4 ? Blocks.SPRUCE_PLANKS : Blocks.BRICKS).defaultBlockState());
			}
			level.setBlockAndUpdate(origin.offset(x, 5, 4), Blocks.SPRUCE_SLAB.defaultBlockState());
		}

		List<Item> dishes = PottkuecheItems.all();
		for (int i = 0; i < dishes.size(); i++) {
			BlockPos pos = origin.offset(-2 + i % 5, 2 - i / 5, 3);
			ItemFrame frame = new ItemFrame(level, pos, Direction.NORTH);
			frame.setItem(new ItemStack(dishes.get(i)));
			level.addFreshEntity(frame);
		}

		for (int x : new int[] {-4, 4}) {
			level.setBlockAndUpdate(origin.offset(x, 3, 3), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
			level.setBlockAndUpdate(origin.offset(x, 2, 3), Blocks.SPRUCE_FENCE.defaultBlockState());
			level.setBlockAndUpdate(origin.offset(x, 1, 3), Blocks.SPRUCE_FENCE.defaultBlockState());
			level.setBlockAndUpdate(origin.offset(x, 0, 3), Blocks.CAMPFIRE.defaultBlockState());
		}
	}
}
