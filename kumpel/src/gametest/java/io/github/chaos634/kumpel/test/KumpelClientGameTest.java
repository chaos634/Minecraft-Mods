package io.github.chaos634.kumpel.test;

import java.util.List;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.behaviour.BarbaraDay;
import io.github.chaos634.kumpel.entity.behaviour.CoalDust;
import io.github.chaos634.kumpel.registry.ModBlocks;
import io.github.chaos634.kumpel.registry.ModEntities;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * Renders Kumpels in a real client and takes screenshots, to check the model, textures and ore sensing.
 */
@SuppressWarnings("UnstableApiUsage")
public class KumpelClientGameTest implements FabricClientGameTest {
	/** Chat messages (like command feedback) fade out after 200 ticks. */
	private static final int CHAT_FADE_TICKS = 220;

	/** A tamed Kumpel of the given level that sits still (its AI still runs, so its helmet lamp and forge work), dusty if asked. */
	private static KumpelEntity sittingKumpel(ServerLevel level, ServerPlayer owner, double x, double y, double z, float yaw, int tier) {
		return sittingKumpel(level, owner, x, y, z, yaw, tier, 0);
	}

	private static KumpelEntity sittingKumpel(ServerLevel level, ServerPlayer owner, double x, double y, double z, float yaw, int tier, int dust) {
		KumpelEntity kumpel = new KumpelEntity(ModEntities.KUMPEL, level);
		kumpel.snapTo(x, y, z, yaw, 0.0F);
		kumpel.addExperience(KumpelSettings.get().tier(tier).requiredExperience());
		kumpel.addDust(dust);
		kumpel.tame(owner);
		kumpel.setOrderedToSit(true);
		kumpel.setOreSensing(false);
		level.addFreshEntity(kumpel);
		return kumpel;
	}

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
			List<KumpelTier> tiers = KumpelSettings.get().tiers();
			for (int i = 0; i < tiers.size(); i++) {
				server.runCommand("execute at @p run summon kumpel:kumpel ~%d ~ ~5 {NoAI:1b,Rotation:[180f,0f],experience:%d}"
						.formatted(-4 + i * 2, tiers.get(i).requiredExperience()));
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
				kumpel.addExperience(KumpelSettings.get().tier(4).requiredExperience());
				kumpel.tame(player);
				level.addFreshEntity(kumpel);
			});
			context.waitTicks(15);
			context.takeScreenshot("kumpel_senses_ore");

			// The Zeche: a Hauer with its pickaxe and canary, a Kumpel dancing to a jukebox, a Grubenhelm on an armor stand,
			// and everyone dressed up for Barbaratag. The Kumpel from before sits down, so it doesn't follow into the picture.
			server.runOnServer(minecraftServer -> singleplayer.getConnection().getServerLevel()
					.getEntities(ModEntities.KUMPEL, KumpelEntity::isTame)
					.forEach(kumpel -> kumpel.setOrderedToSit(true)));
			server.runCommand("execute as @p at @s run tp @s ~20 ~ ~ 0 10");
			server.runCommand("item replace entity @p weapon.mainhand with kumpel:steiger_whistle");
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			BarbaraDay.setOverride(true);
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				BlockPos origin = player.blockPosition();

				KumpelEntity hauer = new KumpelEntity(ModEntities.KUMPEL, level);
				hauer.snapTo(origin.getX() - 1.0, origin.getY(), origin.getZ() + 4.0, 205.0F, 0.0F);
				hauer.addExperience(KumpelSettings.get().tier(2).requiredExperience());
				hauer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
				hauer.setNoAi(true);
				hauer.setMining(true);
				hauer.setCanary(true);
				level.addFreshEntity(hauer);

				BlockPos jukebox = origin.offset(3, 0, 6);
				level.setBlockAndUpdate(jukebox, Blocks.JUKEBOX.defaultBlockState());
				if (level.getBlockEntity(jukebox) instanceof Container container) {
					container.setItem(0, new ItemStack(Items.MUSIC_DISC_CAT));
				}
				level.setBlockAndUpdate(origin.offset(-3, 0, 6), Blocks.CHEST.defaultBlockState());

				KumpelEntity dancer = new KumpelEntity(ModEntities.KUMPEL, level);
				dancer.snapTo(origin.getX() + 2.0, origin.getY(), origin.getZ() + 4.5, 160.0F, 0.0F);
				dancer.addExperience(KumpelSettings.get().tier(3).requiredExperience());
				dancer.setNoAi(true);
				dancer.setDancing(true);
				level.addFreshEntity(dancer);

				ArmorStand stand = new ArmorStand(EntityTypes.ARMOR_STAND, level);
				stand.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 7.5, 180.0F, 0.0F);
				stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MINER_HELMET));
				level.addFreshEntity(stand);
			});
			context.waitTicks(40);
			context.takeScreenshot("kumpel_zeche");
			BarbaraDay.setOverride(null);

			// Unter Tage: a closed chamber at night, lit only by the Kumpels' helmet lamps. One carries a burning field forge,
			// one a canary, one a pickaxe; a Markentafel and a Förderkorb stand at the back, ores glint in the walls.
			// No remarks, greetings or time announcements, so the chat stays empty for the picture.
			KumpelSettings.get().behaviour().chatter = false;
			KumpelSettings.get().behaviour().timeAnnouncements = false;
			server.runCommand("time set midnight");
			server.runCommand("execute as @p at @s run tp @s ~30 ~ ~ 0 20");
			singleplayer.getConnection().waitForChunksRender();
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				BlockPos origin = player.blockPosition();
				for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-6, -1, -3), origin.offset(6, 5, 8))) {
					boolean inside = Math.abs(pos.getX() - origin.getX()) < 6 && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + 5
							&& pos.getZ() > origin.getZ() - 3 && pos.getZ() < origin.getZ() + 8;
					level.setBlockAndUpdate(pos, inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
				}
				level.setBlockAndUpdate(origin.offset(-3, 1, 8), Blocks.DIAMOND_ORE.defaultBlockState());
				level.setBlockAndUpdate(origin.offset(3, 2, 8), Blocks.GOLD_ORE.defaultBlockState());
				level.setBlockAndUpdate(origin.offset(6, 0, 5), Blocks.COPPER_ORE.defaultBlockState());
				level.setBlockAndUpdate(origin.offset(1, 0, 7), ModBlocks.MARKENTAFEL.defaultBlockState()
						.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
				level.setBlockAndUpdate(origin.offset(-2, 0, 7), ModBlocks.FOERDERKORB.defaultBlockState());

				// The forge will smelt right away; its advancement toast should be long gone by the time of the picture.
				KumpelAdvancements.award(player, KumpelAdvancements.HUETTE);
				KumpelEntity smith = sittingKumpel(level, player, origin.getX() - 1.2, origin.getY(), origin.getZ() + 3.2, 210.0F, 3);
				smith.setForge(true);
				smith.getPockets().addToPockets(new ItemStack(Items.RAW_IRON, 16));
				smith.getPockets().addToPockets(new ItemStack(Items.COAL, 8));
				KumpelEntity hauer = sittingKumpel(level, player, origin.getX() + 1.8, origin.getY(), origin.getZ() + 3.5, 150.0F, 2);
				hauer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
				KumpelEntity lookout = sittingKumpel(level, player, origin.getX() + 0.5, origin.getY(), origin.getZ() + 5.5, 180.0F, 5);
				lookout.setCanary(true);
			});
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("kumpel_unter_tage");

			// Strecke: a gallery with support frames and lanterns, coal in the walls, and Kumpels black with coal dust;
			// a water cauldron for the Kaue waits by the entrance.
			server.runCommand("execute as @p at @s run tp @s ~30 ~ ~ 0 8");
			singleplayer.getConnection().waitForChunksRender();
			server.runOnServer(minecraftServer -> {
				ServerLevel level = singleplayer.getConnection().getServerLevel();
				ServerPlayer player = singleplayer.getConnection().getServerPlayer();
				BlockPos origin = player.blockPosition();
				for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -2), origin.offset(4, 4, 16))) {
					boolean gallery = Math.abs(pos.getX() - origin.getX()) <= 1 && pos.getY() >= origin.getY() && pos.getY() <= origin.getY() + 2
							&& pos.getZ() - origin.getZ() < 15;
					level.setBlockAndUpdate(pos, gallery ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
				}
				BlockState post = Blocks.OAK_LOG.defaultBlockState();
				BlockState cap = post.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
				for (int z = 2, frame = 0; z <= 14; z += 4, frame++) {
					for (int y = 0; y <= 2; y++) {
						level.setBlockAndUpdate(origin.offset(-2, y, z), post);
						level.setBlockAndUpdate(origin.offset(2, y, z), post);
					}
					for (int x = -2; x <= 2; x++) {
						level.setBlockAndUpdate(origin.offset(x, 3, z), cap);
					}
					level.setBlockAndUpdate(origin.offset(frame % 2 == 0 ? 2 : -2, 2, z), Blocks.LANTERN.defaultBlockState());
				}
				for (BlockPos coal : List.of(origin.offset(-2, 1, 4), origin.offset(2, 0, 8), origin.offset(-2, 2, 9), origin.offset(2, 1, 12),
						origin.offset(-1, 1, 15), origin.offset(0, 2, 15), origin.offset(1, 0, 15), origin.offset(0, 0, 15))) {
					level.setBlockAndUpdate(coal, Blocks.COAL_ORE.defaultBlockState());
				}
				level.setBlockAndUpdate(origin.offset(-1, 0, 3), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

				// Dust first, then the owner, so nobody calls out for the Kaue during the picture.
				KumpelEntity hauer = new KumpelEntity(ModEntities.KUMPEL, level);
				hauer.snapTo(origin.getX() - 0.3, origin.getY(), origin.getZ() + 5.5, 165.0F, 0.0F);
				hauer.addExperience(KumpelSettings.get().tier(3).requiredExperience());
				hauer.addDust(CoalDust.MAX);
				hauer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
				hauer.setNoAi(true);
				level.addFreshEntity(hauer);

				sittingKumpel(level, player, origin.getX() + 1.5, origin.getY(), origin.getZ() + 7.5, 200.0F, 2, CoalDust.WASH_AT);

				KumpelEntity smith = new KumpelEntity(ModEntities.KUMPEL, level);
				smith.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 10.5, 180.0F, 0.0F);
				smith.addExperience(KumpelSettings.get().tier(4).requiredExperience());
				smith.addDust(CoalDust.WASH_AT / 2);
				smith.setForge(true);
				smith.setNoAi(true);
				level.addFreshEntity(smith);
			});
			singleplayer.getConnection().waitForChunksRender();
			context.waitTicks(CHAT_FADE_TICKS);
			context.takeScreenshot("kumpel_strecke");
			KumpelSettings.get().behaviour().chatter = true;
			KumpelSettings.get().behaviour().timeAnnouncements = true;
		}
	}
}
