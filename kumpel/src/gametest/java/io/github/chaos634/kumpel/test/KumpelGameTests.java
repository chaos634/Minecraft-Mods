package io.github.chaos634.kumpel.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;
import io.github.chaos634.kumpel.item.KumpelSoul;
import io.github.chaos634.kumpel.registry.ModComponents;
import io.github.chaos634.kumpel.registry.ModEntities;
import io.github.chaos634.kumpel.registry.ModItems;

public class KumpelGameTests {
	private static KumpelSettings settings() {
		return KumpelSettings.get();
	}

	@GameTest
	public void defaultConfigIsSensible(GameTestHelper helper) {
		KumpelSettings settings = settings();
		helper.assertTrue(settings.tiers().size() == 5, Component.literal("Expected 5 default levels, got " + settings.tiers().size()));
		helper.assertTrue(settings.firstTier().requiredExperience() == 0, Component.literal("The first level must start at 0 XP"));

		OreRule diamond = settings.matchOre(Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState());
		helper.assertTrue(diamond != null && diamond.level() == 4, Component.literal("Deepslate diamond ore should need level 4, got " + diamond));
		OreRule coal = settings.matchOre(Blocks.COAL_ORE.defaultBlockState());
		helper.assertTrue(coal != null && coal.value() < diamond.value(), Component.literal("Coal should be worth less than diamonds"));
		helper.assertTrue(settings.matchOre(Blocks.STONE.defaultBlockState()) == null, Component.literal("Stone is no ore"));

		helper.assertTrue(settings.feedExperience(new ItemStack(Items.DIAMOND)) == 40, Component.literal("A diamond should give 40 XP"));
		helper.assertTrue(settings.feedExperience(new ItemStack(Items.STICK)) == 0, Component.literal("Sticks are no food"));
		helper.assertTrue(settings.isRepairItem(new ItemStack(Items.COPPER_INGOT)), Component.literal("Copper ingots repair a Kumpel"));
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void collectsDroppedItems(GameTestHelper helper) {
		buildFloor(helper);
		// Tamed animals sit down when their owner can't be found, so give the Kumpel an owner standing nearby.
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);

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
		veteran.addExperience(settings().tier(4).requiredExperience());

		KumpelEntity.SensedOre rookieOre = rookie.findBestOre(helper.getLevel());
		helper.assertTrue(rookieOre != null && rookieOre.pos().equals(helper.absolutePos(new BlockPos(2, 1, 6))),
				Component.literal("A level 1 Kumpel should only sense the coal, got " + rookieOre));

		KumpelEntity.SensedOre veteranOre = veteran.findBestOre(helper.getLevel());
		helper.assertTrue(veteranOre != null && veteranOre.pos().equals(helper.absolutePos(new BlockPos(6, 1, 6))),
				Component.literal("A level 4 Kumpel should prefer the diamond ore, got " + veteranOre));
		helper.succeed();
	}

	@GameTest
	public void levelsUpWithExperience(GameTestHelper helper) {
		buildFloor(helper);
		KumpelTier first = settings().tier(1);
		KumpelTier second = settings().tier(2);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		helper.assertTrue(kumpel.getTier().level() == 1, Component.literal("A new Kumpel should be level 1"));

		kumpel.addExperience(second.requiredExperience() - 1);
		helper.assertTrue(kumpel.getTier() == first, Component.literal("Not enough XP for level 2 yet"));

		kumpel.addExperience(1);
		helper.assertTrue(kumpel.getTier() == second, Component.literal("Kumpel should have reached level 2"));
		helper.assertTrue(kumpel.getMaxHealth() == (float) second.maxHealth(),
				Component.literal("Max health should grow with the level, got " + kumpel.getMaxHealth()));
		helper.assertTrue(kumpel.getHealth() == kumpel.getMaxHealth(), Component.literal("A level up should fully heal"));
		helper.assertTrue(kumpel.getTexture().equals(second.texture()), Component.literal("The texture should change with the level"));
		helper.succeed();
	}

	@GameTest
	public void backpackGrowsWithLevel(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		Item[] items = {Items.STONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.OAK_LOG, Items.COBBLESTONE,
				Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.TUFF};

		int slots = kumpel.getTier().pocketSlots();
		for (int i = 0; i < slots; i++) {
			helper.assertTrue(kumpel.getPockets().addToPockets(new ItemStack(items[i % items.length], 64)).isEmpty(),
					Component.literal("Slot " + i + " should be free"));
		}
		helper.assertTrue(kumpel.isInventoryFull(), Component.literal("All " + slots + " slots are full"));
		helper.assertFalse(kumpel.getPockets().addToPockets(new ItemStack(Items.TUFF, 1)).isEmpty(),
				Component.literal("A full backpack takes nothing more"));

		KumpelTier bigger = settings().tiers().stream().filter(tier -> tier.pocketRows() > kumpel.getTier().pocketRows()).findFirst().orElseThrow();
		kumpel.addExperience(bigger.requiredExperience());
		helper.assertFalse(kumpel.isInventoryFull(), Component.literal("A bigger backpack has room again"));
		helper.assertTrue(kumpel.getPockets().addToPockets(new ItemStack(Items.TUFF, 1)).isEmpty(),
				Component.literal("The new row should take items"));
		helper.succeed();
	}

	@GameTest
	public void deliversItemsToOwner(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.getPockets().addToPockets(new ItemStack(Items.DIAMOND, 5));

		kumpel.deliverItemsTo(player);

		helper.assertTrue(count(player.getInventory(), Items.DIAMOND) == 5, Component.literal("Owner should have received 5 diamonds"));
		helper.assertTrue(kumpel.getInventory().isEmpty(), Component.literal("Kumpel should have emptied its pockets"));
		helper.succeed();
	}

	@GameTest
	public void soulSurvivesPackingAndUnpacking(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(player);
		kumpel.addExperience(settings().tier(3).requiredExperience() + 5);
		kumpel.getPockets().addToPockets(new ItemStack(Items.EMERALD, 2));

		kumpel.packInto(player, new ItemStack(ModItems.KUMPEL_CORE));

		helper.assertTrue(kumpel.isRemoved(), Component.literal("The packed Kumpel should be gone"));
		helper.assertTrue(count(player.getInventory(), Items.EMERALD) == 2, Component.literal("Its loot should go to the owner"));
		ItemStack core = findInInventory(player, ModItems.KUMPEL_CORE);
		KumpelSoul soul = core.get(ModComponents.SOUL);
		helper.assertTrue(soul != null && soul.experience() == settings().tier(3).requiredExperience() + 5,
				Component.literal("The core should remember all experience, got " + soul));

		KumpelEntity revived = helper.spawn(ModEntities.KUMPEL, 2, 1, 2);
		revived.loadSoul(soul);
		helper.assertTrue(revived.getTier().level() == 3, Component.literal("The revived Kumpel should be level 3 again"));
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void leavesCrackedCoreWhenItDies(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.addExperience(100);
		int expected = (int) Math.floor(100 * settings().behaviour().deathExperienceKept);

		kumpel.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F);

		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8.0);
		helper.succeedWhen(() -> {
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
					item -> item.getItem().is(ModItems.CRACKED_KUMPEL_CORE));
			helper.assertTrue(!drops.isEmpty(), Component.literal("A cracked core should drop"));
			KumpelSoul soul = drops.getFirst().getItem().get(ModComponents.SOUL);
			helper.assertTrue(soul != null && soul.experience() == expected,
					Component.literal("The cracked core should keep " + expected + " XP, got " + soul));
		});
	}

	@GameTest(maxTicks = 200)
	public void placesTorchesInTheDark(GameTestHelper helper) {
		buildFloor(helper);
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 3, z, Blocks.STONE);
			}
		}

		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4))));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.getPockets().addToPockets(new ItemStack(Items.TORCH, 8));

		helper.succeedWhen(() -> helper.assertTrue(kumpel.getPockets().count(KumpelPockets::isTorch) < 8,
				Component.literal("The Kumpel should have placed a torch in the dark")));
	}

	@GameTest(maxTicks = 100)
	public void makesCreepersGlow(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);
		Creeper creeper = helper.spawn(EntityTypes.CREEPER, 6, 1, 6);
		creeper.setNoAi(true);

		helper.succeedWhen(() -> helper.assertTrue(creeper.hasEffect(MobEffects.GLOWING),
				Component.literal("The creeper should glow")));
	}

	@GameTest(maxTicks = 100)
	public void sharesLunchWhenOwnerIsHungry(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		owner.getFoodData().setFoodLevel(2);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);
		kumpel.getPockets().addToPockets(new ItemStack(Items.BREAD, 3));

		helper.succeedWhen(() -> helper.assertTrue(count(owner.getInventory(), Items.BREAD) == 1,
				Component.literal("The Kumpel should hand over one bread")));
	}

	@GameTest
	public void keepsTorchesAndLunchWhenDelivering(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.getPockets().addToPockets(new ItemStack(Items.TORCH, 16));
		kumpel.getPockets().addToPockets(new ItemStack(Items.BREAD, 5));
		kumpel.getPockets().addToPockets(new ItemStack(Items.DIAMOND, 3));

		kumpel.deliverItemsTo(player);

		helper.assertTrue(count(player.getInventory(), Items.DIAMOND) == 3, Component.literal("Loot is delivered"));
		helper.assertTrue(count(player.getInventory(), Items.TORCH) == 0, Component.literal("Torches stay with the Kumpel"));
		helper.assertTrue(count(kumpel.getPockets(), Items.BREAD) == 5, Component.literal("The packed lunch stays with the Kumpel"));
		helper.assertFalse(kumpel.hasItemsToDeliver(), Component.literal("Nothing left to deliver"));
		helper.succeed();
	}

	private static ItemStack findInInventory(Player player, Item item) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
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
