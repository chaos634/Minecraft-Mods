package io.github.chaos634.kumpel.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreKind;
import io.github.chaos634.kumpel.registry.ModEntities;

public class KumpelGameTests {
	@GameTest(maxTicks = 300)
	public void collectsDroppedItems(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.setTame(true, false);

		BlockPos itemPos = helper.absolutePos(new BlockPos(5, 1, 5));
		ItemEntity item = new ItemEntity(helper.getLevel(), itemPos.getX() + 0.5, itemPos.getY(), itemPos.getZ() + 0.5,
				new ItemStack(Items.IRON_INGOT, 3));
		helper.getLevel().addFreshEntity(item);

		helper.succeedWhen(() -> helper.assertTrue(count(kumpel.getInventory(), Items.IRON_INGOT) == 3,
				Component.literal("Kumpel should have picked up the 3 iron ingots")));
	}

	@GameTest
	public void sensesMostValuableOreForItsLevel(GameTestHelper helper) {
		buildFloor(helper);
		helper.setBlock(2, 1, 6, Blocks.COAL_ORE);
		helper.setBlock(6, 1, 6, Blocks.DIAMOND_ORE);

		KumpelEntity rookie = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		KumpelEntity veteran = helper.spawn(ModEntities.KUMPEL, 1, 1, 2);
		veteran.addExperience(KumpelTier.DIAMOND.requiredExperience());

		KumpelEntity.SensedOre rookieOre = rookie.findBestOre(helper.getLevel());
		helper.assertTrue(rookieOre != null && rookieOre.kind() == OreKind.COAL,
				Component.literal("A copper Kumpel should only sense the coal, got " + rookieOre));
		helper.assertTrue(rookieOre.pos().equals(helper.absolutePos(new BlockPos(2, 1, 6))),
				Component.literal("Coal ore found at the wrong position: " + rookieOre.pos()));

		KumpelEntity.SensedOre veteranOre = veteran.findBestOre(helper.getLevel());
		helper.assertTrue(veteranOre != null && veteranOre.kind() == OreKind.DIAMOND,
				Component.literal("A diamond Kumpel should prefer the diamond ore, got " + veteranOre));
		helper.succeed();
	}

	@GameTest
	public void levelsUpWithExperience(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		helper.assertTrue(kumpel.getTier() == KumpelTier.COPPER, Component.literal("A new Kumpel should be copper"));

		kumpel.addExperience(KumpelTier.IRON.requiredExperience() - 1);
		helper.assertTrue(kumpel.getTier() == KumpelTier.COPPER, Component.literal("Not enough XP for iron yet"));

		kumpel.addExperience(1);
		helper.assertTrue(kumpel.getTier() == KumpelTier.IRON, Component.literal("Kumpel should have reached iron"));
		helper.assertTrue(kumpel.getMaxHealth() == (float) KumpelTier.IRON.maxHealth(),
				Component.literal("Max health should grow with the level, got " + kumpel.getMaxHealth()));
		helper.assertTrue(kumpel.getHealth() == kumpel.getMaxHealth(), Component.literal("A level up should fully heal"));
		helper.succeed();
	}

	@GameTest
	public void deliversItemsToOwner(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.getInventory().addItem(new ItemStack(Items.DIAMOND, 5));

		kumpel.deliverItemsTo(player);

		helper.assertTrue(count(player.getInventory(), Items.DIAMOND) == 5, Component.literal("Owner should have received 5 diamonds"));
		helper.assertTrue(kumpel.getInventory().isEmpty(), Component.literal("Kumpel should have emptied its pockets"));
		helper.succeed();
	}

	private static void buildFloor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static int count(Container container, Item item) {
		int count = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}
}
