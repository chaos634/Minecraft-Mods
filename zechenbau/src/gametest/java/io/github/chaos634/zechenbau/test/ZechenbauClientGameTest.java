package io.github.chaos634.zechenbau.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.chaos634.zechenbau.registry.ZechenbauBlocks;

/**
 * Builds a small colliery out of every block of the mod, a machine hall with a headframe behind it, and takes a
 * screenshot by day and one by night.
 */
@SuppressWarnings("UnstableApiUsage")
public class ZechenbauClientGameTest implements FabricClientGameTest {
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
			singleplayer.getConnection().waitForChunksRender();
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				buildColliery(level, singleplayer.getConnection().getServerPlayer().blockPosition());
			});
			// Step back a little, so the whole headframe is in the picture.
			server.runCommand("execute as @p at @s run tp @s ~2 ~ ~-6 0 -16");
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("zechenbau_zeche");

			server.runCommand("time set midnight");
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("zechenbau_nacht");
		}
	}

	/** The machine hall faces south (towards the player), its front 12 blocks from {@code origin}; the headframe stands behind it, to the east. */
	private static void buildColliery(ServerLevel level, BlockPos origin) {
		Builder build = new Builder(level, origin);
		BlockState brick = ZechenbauBlocks.ZECHENZIEGEL.defaultBlockState();
		BlockState framed = ZechenbauBlocks.STAHLFACHWERK.defaultBlockState();
		BlockState window = ZechenbauBlocks.FACHWERK_FENSTER.defaultBlockState();
		BlockState pillar = ZechenbauBlocks.STAHLTRAEGER.defaultBlockState();
		BlockState lamp = ZechenbauBlocks.GRUBENLAMPE.defaultBlockState();
		BlockState lattice = ZechenbauBlocks.FOERDERGERUEST.defaultBlockState();
		BlockState stairs = ZechenbauBlocks.ZECHENZIEGEL_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

		// A clean lawn to build on.
		for (int x = -15; x <= 15; x++) {
			for (int z = -9; z <= 26; z++) {
				build.set(x, -1, z, Blocks.GRASS_BLOCK.defaultBlockState());
				for (int y = 0; y <= 22; y++) {
					build.set(x, y, z, Blocks.AIR.defaultBlockState());
				}
			}
		}

		// The hall: brick walls and roof, 17 wide and 7 deep.
		for (int x = -8; x <= 8; x++) {
			for (int z = 12; z <= 18; z++) {
				for (int y = 0; y <= 9; y++) {
					boolean wall = x == -8 || x == 8 || z == 12 || z == 18;
					if (wall) {
						build.set(x, y, z, brick);
					}
				}
				build.set(x, 10, z, ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState());
			}
		}

		// The front: steel girders between the bays, a steel-framed middle with a big window, tall windows in the brick bays.
		for (int y = 0; y <= 8; y++) {
			for (int x : new int[] {-8, -4, 4, 8}) {
				build.set(x, y, 12, pillar);
			}
			for (int x = -3; x <= 3; x++) {
				build.set(x, y, 12, framed);
			}
		}
		for (int x = -8; x <= 8; x++) {
			build.set(x, 9, 12, pillar.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
			build.set(x, 10, 12, stairs);
		}
		for (int x = -2; x <= 2; x++) {
			for (int y = 3; y <= 5; y++) {
				build.set(x, y, 12, window);
			}
		}
		for (int x : new int[] {-6, 6}) {
			for (int y = 2; y <= 6; y++) {
				build.set(x, y, 12, window);
			}
			build.set(x, 8, 12, framed);
		}
		build.set(0, 7, 12, ZechenbauBlocks.SCHLAEGEL_UND_EISEN.defaultBlockState());

		// The gate: a dark doorway with steps, and two lamps hanging from girders above it.
		for (int x = -1; x <= 1; x++) {
			for (int y = 0; y <= 1; y++) {
				build.set(x, y, 12, Blocks.AIR.defaultBlockState());
			}
			build.set(x, 0, 11, stairs);
			for (int z = 6; z <= 10; z++) {
				build.set(x, 0, z, ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState());
			}
		}
		for (int x : new int[] {-3, 3}) {
			build.set(x, 4, 11, pillar.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			build.set(x, 3, 11, lamp.setValue(LanternBlock.HANGING, true));
		}

		// Lamps inside, so the windows glow at night.
		for (int x : new int[] {-6, 0, 6}) {
			build.set(x, 0, 15, lamp);
		}

		// The yard wall, with a lamp on every corner post.
		List<BlockPos> walls = new ArrayList<>();
		for (int x = -8; x <= 8; x++) {
			if (Math.abs(x) >= 2) {
				walls.add(build.set(x, 0, 6, ZechenbauBlocks.ZECHENZIEGEL_WALL.defaultBlockState()));
			}
		}
		for (int x : new int[] {-8, -2, 2, 8}) {
			build.set(x, 1, 6, lamp);
		}
		walls.forEach(pos -> level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos)));

		// The headframe: a lattice tower with the sheave girder on top and a lamp in it.
		for (int y = 0; y <= 16; y++) {
			for (int x = 10; x <= 12; x++) {
				for (int z = 14; z <= 16; z++) {
					if (x != 11 || z != 15) {
						build.set(x, y, z, lattice);
					}
				}
			}
		}
		for (int x = 9; x <= 13; x++) {
			build.set(x, 17, 15, pillar.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		build.set(11, 16, 15, lamp.setValue(LanternBlock.HANGING, true));
		for (int x : new int[] {10, 12}) {
			build.set(x, 18, 15, ZechenbauBlocks.SEILSCHEIBE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		}

		// Years of coal smoke and rain: some bricks cracked, some grown over with moss, mostly low down.
		Random weather = new Random(1880);
		for (int x = -8; x <= 8; x++) {
			for (int y = 0; y <= 9; y++) {
				for (int z = 12; z <= 18; z++) {
					BlockPos pos = origin.offset(x, y, z);
					if (level.getBlockState(pos).is(ZechenbauBlocks.ZECHENZIEGEL) && weather.nextFloat() < 0.14F) {
						boolean mossy = weather.nextFloat() < 0.7F - y * 0.07F;
						level.setBlockAndUpdate(pos, (mossy ? ZechenbauBlocks.BEMOOSTE_ZECHENZIEGEL : ZechenbauBlocks.RISSIGE_ZECHENZIEGEL).defaultBlockState());
					}
				}
			}
		}

		// A train of coal tubs on the track in front of the yard.
		for (int x = -10; x <= -2; x++) {
			build.set(x, 0, 3, x >= -7 && x <= -5
					? ZechenbauBlocks.HUNT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
					: Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
		}

		// Left of the yard, an open shelter where the miners' clothes hang on their hooks, like in the Waschkaue.
		for (int x : new int[] {9, 13}) {
			for (int z : new int[] {8, 11}) {
				for (int y = 0; y <= 2; y++) {
					build.set(x, y, z, pillar);
				}
			}
		}
		for (int x = 9; x <= 13; x++) {
			for (int z = 8; z <= 11; z++) {
				build.set(x, 3, z, ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState());
			}
		}
		for (int x = 10; x <= 12; x++) {
			build.set(x, 2, 9, ZechenbauBlocks.KAUENHAKEN.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		}
	}

	private record Builder(ServerLevel level, BlockPos origin) {
		BlockPos set(int x, int y, int z, BlockState state) {
			BlockPos pos = origin.offset(x, y, z);
			level.setBlockAndUpdate(pos, state);
			return pos;
		}
	}
}
