package io.github.chaos634.kumpel.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;
import io.github.chaos634.kumpel.entity.behaviour.BarbaraDay;
import io.github.chaos634.kumpel.entity.behaviour.OreGlimmer;
import io.github.chaos634.kumpel.block.FoerderkorbBlock;
import io.github.chaos634.kumpel.entity.ExitTrail;
import io.github.chaos634.kumpel.entity.Markenkontrolle;
import io.github.chaos634.kumpel.entity.OreFinds;
import io.github.chaos634.kumpel.entity.ShiftLog;
import io.github.chaos634.kumpel.item.KumpelFibel;
import io.github.chaos634.kumpel.item.KumpelSoul;
import io.github.chaos634.kumpel.item.MinerHelmetItem;
import io.github.chaos634.kumpel.item.RescueCapsuleItem;
import io.github.chaos634.kumpel.item.SteigerWhistleItem;
import io.github.chaos634.kumpel.registry.ModBlocks;
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

	@GameTest
	public void whistleCallsAndSendsOnBreak(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 2, 2);
		KumpelEntity first = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		KumpelEntity second = helper.spawn(ModEntities.KUMPEL, 6, 1, 6);
		KumpelEntity stranger = helper.spawn(ModEntities.KUMPEL, 4, 1, 1);
		first.tame(owner);
		second.tame(owner);

		int resting = SteigerWhistleItem.sendOnBreak(helper.getLevel(), owner);
		helper.assertTrue(resting == 2, Component.literal("Both of the owner's Kumpels should hear the whistle, got " + resting));
		helper.assertTrue(first.isOrderedToSit() && second.isOrderedToSit(), Component.literal("Both should sit down"));
		helper.assertFalse(stranger.isTame(), Component.literal("A wild Kumpel doesn't care about the whistle"));

		int called = SteigerWhistleItem.callKumpels(helper.getLevel(), owner);
		helper.assertTrue(called == 2, Component.literal("Both Kumpels should come, got " + called));
		helper.assertFalse(first.isOrderedToSit() || second.isOrderedToSit(), Component.literal("Both should stand up again"));
		helper.succeed();
	}

	@GameTest
	public void fillsStorageChestAndKeepsWhatDoesNotFit(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		BlockPos chestPos = helper.absolutePos(new BlockPos(4, 1, 4));
		helper.setBlock(4, 1, 4, Blocks.CHEST);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.setStorage(helper.getLevel(), chestPos);
		helper.assertTrue(chestPos.equals(kumpel.usableStorage()), Component.literal("The chest should be the Kumpel's storage"));

		kumpel.getPockets().addToPockets(new ItemStack(Items.RAW_COPPER, 12));
		kumpel.getPockets().addToPockets(new ItemStack(Items.TORCH, 4));
		kumpel.deliverItemsToStorage(helper.getLevel());

		Container chest = container(helper, chestPos);
		helper.assertTrue(count(chest, Items.RAW_COPPER) == 12, Component.literal("The raw copper should be in the chest"));
		helper.assertTrue(count(kumpel.getPockets(), Items.TORCH) == 4, Component.literal("Torches stay with the Kumpel"));

		for (int i = 0; i < chest.getContainerSize(); i++) {
			chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		}
		kumpel.getPockets().addToPockets(new ItemStack(Items.DIAMOND, 2));
		kumpel.deliverItemsToStorage(helper.getLevel());

		helper.assertTrue(count(kumpel.getPockets(), Items.DIAMOND) == 2, Component.literal("What doesn't fit stays in the backpack"));
		helper.assertTrue(kumpel.usableStorage() == null, Component.literal("A full chest is skipped for a while"));
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void walksLootToStorageChest(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		BlockPos chestPos = helper.absolutePos(new BlockPos(6, 1, 6));
		helper.setBlock(6, 1, 6, Blocks.CHEST);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 2);
		kumpel.tame(owner);
		kumpel.setStorage(helper.getLevel(), chestPos);
		kumpel.getPockets().addToPockets(new ItemStack(Items.COAL, 9));

		helper.succeedWhen(() -> {
			helper.assertTrue(count(container(helper, chestPos), Items.COAL) == 9, Component.literal("The Kumpel should carry the coal to the chest"));
			helper.assertTrue(count(owner.getInventory(), Items.COAL) == 0, Component.literal("Nothing should go to the owner"));
		});
	}

	@GameTest(maxTicks = 300)
	public void minesExposedOreWithItsPickaxe(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		helper.setBlock(5, 1, 4, Blocks.COPPER_ORE);
		helper.setBlock(2, 1, 5, Blocks.DIAMOND_ORE);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));

		BlockPos copper = helper.absolutePos(new BlockPos(5, 1, 4));
		BlockPos diamond = helper.absolutePos(new BlockPos(2, 1, 5));

		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().getBlockState(copper).isAir(), Component.literal("The copper ore should be mined"));
			helper.assertTrue(kumpel.getMainHandItem().getDamageValue() >= 1, Component.literal("Mining should wear down the pickaxe"));
			// Too valuable for a level 1 Kumpel, and too hard for a stone pickaxe.
			helper.assertTrue(helper.getLevel().getBlockState(diamond).is(Blocks.DIAMOND_ORE), Component.literal("The diamond ore should stay"));
		});
	}

	@GameTest
	public void keepsOnePickaxeAndHandsItBackWhenPacked(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(player);
		kumpel.getPockets().addToPockets(new ItemStack(Items.IRON_PICKAXE));
		kumpel.getPockets().addToPockets(new ItemStack(Items.STONE_PICKAXE));
		kumpel.getPockets().addToPockets(new ItemStack(Items.EMERALD, 2));

		kumpel.deliverItemsTo(player);
		helper.assertTrue(count(kumpel.getPockets(), Items.IRON_PICKAXE) == 1, Component.literal("The Kumpel keeps its first pickaxe"));
		helper.assertTrue(count(player.getInventory(), Items.STONE_PICKAXE) == 1, Component.literal("A second pickaxe is loot"));

		kumpel.getPockets().removeAllItems();
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		kumpel.stashTool();
		helper.assertTrue(kumpel.getMainHandItem().isEmpty() && count(kumpel.getPockets(), Items.IRON_PICKAXE) == 1,
				Component.literal("Opening the backpack puts the pickaxe in it"));

		kumpel.getPockets().removeAllItems();
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_PICKAXE));
		kumpel.setCanary(true);
		kumpel.packInto(player, new ItemStack(ModItems.KUMPEL_CORE));
		helper.assertTrue(count(player.getInventory(), Items.GOLDEN_PICKAXE) == 1, Component.literal("Packing hands the pickaxe back"));
		helper.assertTrue(count(player.getInventory(), ModItems.CANARY_CAGE) == 1, Component.literal("Packing hands the canary cage back"));
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void dancesWhileJukeboxPlays(GameTestHelper helper) {
		buildFloor(helper);
		BlockPos jukeboxPos = helper.absolutePos(new BlockPos(5, 1, 5));
		helper.setBlock(5, 1, 5, Blocks.JUKEBOX);
		container(helper, jukeboxPos).setItem(0, new ItemStack(Items.MUSIC_DISC_CAT));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 2, 1, 2);

		helper.succeedWhen(() -> helper.assertTrue(kumpel.isDancing(), Component.literal("The Kumpel should dance to the music")));
	}

	@GameTest(maxTicks = 120)
	public void experiencedKumpelMakesOreGlow(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 2, 2);
		BlockPos orePos = helper.absolutePos(new BlockPos(6, 0, 6));
		helper.setBlock(6, 0, 6, Blocks.DIAMOND_ORE);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);
		kumpel.addExperience(settings().tier(settings().behaviour().dowsingLevel).requiredExperience());

		helper.succeedWhen(() -> helper.assertTrue(OreGlimmer.isGlowing(helper.getLevel(), orePos),
				Component.literal("The sensed diamond ore should glow")));
	}

	@GameTest
	public void barbaraDayDoublesFeedingExperience(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 2, 2);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);
		int diamond = settings().feedExperience(new ItemStack(Items.DIAMOND));

		BarbaraDay.setOverride(true);
		try {
			owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
			kumpel.mobInteract(owner, InteractionHand.MAIN_HAND);
		} finally {
			BarbaraDay.setOverride(null);
		}

		helper.assertTrue(kumpel.getExperience() == 2 * diamond,
				Component.literal("A diamond should give " + 2 * diamond + " XP on Barbaratag, got " + kumpel.getExperience()));
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void canaryMakesMonstersGlow(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 2, 2);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);
		kumpel.setCanary(true);
		Monster spider = helper.spawn(EntityTypes.SPIDER, 6, 1, 6);
		spider.setNoAi(true);

		helper.succeedWhen(() -> helper.assertTrue(spider.hasEffect(MobEffects.GLOWING),
				Component.literal("The canary should make the spider glow")));
	}

	@GameTest(maxTicks = 100)
	public void minerHelmetLightsUpTheDark(GameTestHelper helper) {
		// A closed stone box, so no light gets in.
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				for (int z = 0; z < 5; z++) {
					boolean wall = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
					helper.setBlock(x, y, z, wall ? Blocks.STONE : Blocks.AIR);
				}
			}
		}

		Player miner = helper.makeMockPlayer(GameType.SURVIVAL);
		miner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
		miner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MINER_HELMET));
		Player tourist = helper.makeMockPlayer(GameType.SURVIVAL);
		tourist.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));

		helper.succeedWhen(() -> {
			MinerHelmetItem.updateLamp(miner);
			MinerHelmetItem.updateLamp(tourist);
			helper.assertTrue(miner.hasEffect(MobEffects.NIGHT_VISION), Component.literal("The helmet's lamp should light up the dark"));
			helper.assertFalse(tourist.hasEffect(MobEffects.NIGHT_VISION), Component.literal("No helmet, no lamp"));
		});
	}

	@GameTest
	public void newKumpelsGetANameFromThePott(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.giveRandomName();
		helper.assertTrue(kumpel.hasCustomName() && settings().names().contains(kumpel.getCustomName().getString()),
				Component.literal("The Kumpel should get a name from the list, got " + kumpel.getCustomName()));

		KumpelEntity named = helper.spawn(ModEntities.KUMPEL, 2, 1, 1);
		named.setCustomName(Component.literal("Glückauf-Günni"));
		named.giveRandomName();
		helper.assertTrue(named.getCustomName().getString().equals("Glückauf-Günni"), Component.literal("A name you gave stays"));
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void warnsAboutSilverfishInTheRock(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 2, 2);
		BlockPos infested = helper.absolutePos(new BlockPos(5, 0, 5));
		helper.setBlock(5, 0, 5, Blocks.INFESTED_STONE);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.tame(owner);

		helper.assertTrue(kumpel.findInfestedBlocks(helper.getLevel(), 8, 4).equals(List.of(infested)),
				Component.literal("The infested stone should be found"));
		helper.succeedWhen(() -> helper.assertTrue(OreGlimmer.isGlowing(helper.getLevel(), infested),
				Component.literal("The infested stone should be marked")));
	}

	@GameTest(maxTicks = 600)
	public void digsATunnelIntoTheWall(GameTestHelper helper) {
		buildFloor(helper);
		buildWall(helper);
		Player owner = ownerAt(helper, 1, 5);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 3);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		kumpel.startTunnel(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.EAST, 4);

		helper.succeedWhen(() -> {
			for (int x = 3; x <= 6; x++) {
				for (int y = 1; y <= 2; y++) {
					helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, 3))).isAir(),
							Component.literal("Tunnel block " + x + "," + y + " should be dug"));
				}
			}
			helper.assertTrue(kumpel.getTunnel() == null, Component.literal("The tunnel order should be done"));
			helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(7, 1, 3))).is(Blocks.STONE),
					Component.literal("The tunnel should be exactly 4 long"));
			helper.assertTrue(count(kumpel.getPockets(), Items.COBBLESTONE) + count(owner.getInventory(), Items.COBBLESTONE) == 8,
					Component.literal("The 8 dug blocks should end up as cobblestone"));
		});
	}

	@GameTest(maxTicks = 300)
	public void tunnelStopsBeforeWaterWithoutStone(GameTestHelper helper) {
		buildFloor(helper);
		// Terracotta leaves nothing the Kumpel could seal the water with.
		buildWall(helper, Blocks.TERRACOTTA);
		// A closed water pocket next to the third slice.
		helper.setBlock(5, 1, 5, Blocks.STONE);
		helper.setBlock(5, 1, 4, Blocks.WATER);
		Player owner = ownerAt(helper, 1, 5);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 3);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		kumpel.startTunnel(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.EAST, 4);

		helper.succeedWhen(() -> {
			helper.assertTrue(kumpel.getTunnel() == null, Component.literal("The Kumpel should stop the tunnel"));
			helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 3))).isAir(),
					Component.literal("The first slice is safe to dig"));
			helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(5, 1, 3))).is(Blocks.TERRACOTTA),
					Component.literal("The slice next to the water stays"));
		});
	}

	@GameTest(maxTicks = 600)
	public void sealsWaterAndBridgesHolesInTheTunnel(GameTestHelper helper) {
		buildFloor(helper);
		buildWall(helper);
		// A closed water pocket next to the third slice, and a hole in the floor of the fourth.
		helper.setBlock(5, 1, 5, Blocks.STONE);
		helper.setBlock(5, 1, 4, Blocks.WATER);
		helper.setBlock(6, 0, 3, Blocks.AIR);
		Player owner = ownerAt(helper, 1, 5);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 3);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		kumpel.startTunnel(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.EAST, 4);

		helper.succeedWhen(() -> {
			for (int x = 3; x <= 6; x++) {
				for (int y = 1; y <= 2; y++) {
					helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, 3))).isAir(),
							Component.literal("Tunnel block " + x + "," + y + " should be dug (stopped: " + kumpel.getLastTunnelStop()
									+ ", order " + kumpel.getTunnel() + ", kumpel at " + kumpel.blockPosition() + ", sealed "
									+ kumpel.getLog().get(ShiftLog.Entry.LEAKS_SEALED) + ", fillers " + kumpel.getPockets().count(KumpelPockets::isTunnelFiller)
									+ ", hole " + helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(6, 0, 3))) + ")"));
				}
			}
			helper.assertTrue(kumpel.getTunnel() == null, Component.literal("The tunnel order should be done"));
			helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(5, 1, 4))).isEmpty(),
					Component.literal("The water should be sealed off"));
			helper.assertFalse(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(6, 0, 3))).isAir(),
					Component.literal("The hole in the floor should be closed"));
			helper.assertTrue(kumpel.getLog().get(ShiftLog.Entry.LEAKS_SEALED) == 2, Component.literal("Two leaks go into the shift log, got "
					+ kumpel.getLog().get(ShiftLog.Entry.LEAKS_SEALED)));
		});
	}

	@GameTest(maxTicks = 400)
	public void fieldForgeSmeltsRawOre(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.setForge(true);
		kumpel.getPockets().addToPockets(new ItemStack(Items.RAW_IRON, 2));
		kumpel.getPockets().addToPockets(new ItemStack(Items.COAL));

		helper.succeedWhen(() -> {
			helper.assertTrue(count(kumpel.getPockets(), Items.IRON_INGOT) == 2, Component.literal("Both raw iron should be smelted"));
			helper.assertTrue(count(kumpel.getPockets(), Items.RAW_IRON) == 0, Component.literal("No raw iron left"));
			helper.assertTrue(count(kumpel.getPockets(), Items.COAL) == 0, Component.literal("The coal should be burnt"));
		});
	}

	@GameTest
	public void forgeKeepsRawOreUntilItIsSmelted(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.setForge(true);
		kumpel.getPockets().addToPockets(new ItemStack(Items.RAW_GOLD, 3));
		kumpel.getPockets().addToPockets(new ItemStack(Items.CHARCOAL, 2));
		kumpel.getPockets().addToPockets(new ItemStack(Items.EMERALD));

		kumpel.deliverItemsTo(player);
		helper.assertTrue(count(player.getInventory(), Items.EMERALD) == 1, Component.literal("The emerald is loot"));
		helper.assertTrue(count(kumpel.getPockets(), Items.RAW_GOLD) == 3, Component.literal("Raw gold waits for the forge"));
		helper.assertTrue(count(kumpel.getPockets(), Items.CHARCOAL) == 2, Component.literal("The forge keeps its fuel"));

		kumpel.setForge(false);
		kumpel.deliverItemsTo(player);
		helper.assertTrue(count(player.getInventory(), Items.RAW_GOLD) == 3, Component.literal("Without a forge, raw gold is loot"));
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void defendsItsOwnerAgainstMonsters(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 2, 1, 1);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		Monster husk = helper.spawn(EntityTypes.HUSK, 5, 1, 5);
		husk.setNoAi(true);
		Creeper creeper = helper.spawn(EntityTypes.CREEPER, 6, 1, 1);
		creeper.setNoAi(true);

		// The husk hit the owner (an unticked mock player needs a tick count for the goal to notice).
		owner.tickCount = 100;
		owner.setLastHurtByMob(husk);
		helper.assertFalse(kumpel.wantsToAttack(creeper, owner), Component.literal("Creepers are left alone"));

		helper.succeedWhen(() -> {
			helper.assertFalse(husk.isAlive(), Component.literal("The Kumpel should defeat the husk (target " + kumpel.getTarget()
					+ ", husk health " + husk.getHealth() + ", kumpel at " + kumpel.blockPosition() + ", sitting " + kumpel.isOrderedToSit()
					+ ", owner hurt by " + owner.getLastHurtByMob() + ")"));
			helper.assertTrue(kumpel.getLog().get(ShiftLog.Entry.MONSTERS_DEFEATED) == 1, Component.literal("The fight goes into the shift log"));
		});
	}

	@GameTest(maxTicks = 300)
	public void fightsATargetUntilItIsDefeated(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 2, 1, 1);
		kumpel.tame(owner);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		Monster husk = helper.spawn(EntityTypes.HUSK, 4, 1, 3);
		husk.setNoAi(true);
		kumpel.setTarget(husk);

		helper.succeedWhen(() -> helper.assertFalse(husk.isAlive(), Component.literal("The Kumpel should defeat its target (target "
				+ kumpel.getTarget() + ", husk health " + husk.getHealth() + ", kumpel at " + kumpel.blockPosition()
				+ ", damage " + kumpel.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) + ")")));
	}

	@GameTest
	public void foerderkorbTakesPlayerAndKumpelsUpAndDown(GameTestHelper helper) {
		buildFloor(helper);
		helper.setBlock(2, 1, 2, ModBlocks.FOERDERKORB);
		helper.setBlock(2, 5, 2, ModBlocks.FOERDERKORB);
		BlockPos bottom = helper.absolutePos(new BlockPos(2, 1, 2));
		BlockPos top = helper.absolutePos(new BlockPos(2, 5, 2));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(Vec3.atBottomCenterOf(bottom.above()));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(player);

		helper.assertTrue(top.equals(FoerderkorbBlock.ride(helper.getLevel(), player, bottom, true)), Component.literal("Up to the top cage"));
		helper.assertTrue(player.blockPosition().equals(top.above()), Component.literal("The player should stand on the top cage, is at " + player.blockPosition()));
		helper.assertTrue(kumpel.blockPosition().equals(top.above()), Component.literal("The Kumpel should ride along, is at " + kumpel.blockPosition()));

		helper.assertTrue(bottom.equals(FoerderkorbBlock.ride(helper.getLevel(), player, top, false)), Component.literal("Down to the bottom cage"));
		helper.assertTrue(player.blockPosition().equals(bottom.above()), Component.literal("Back on the bottom cage"));
		helper.assertTrue(FoerderkorbBlock.findCage(helper.getLevel(), bottom, false) == null, Component.literal("Nothing further down"));
		helper.succeed();
	}

	@GameTest
	public void writesItsShiftLogIntoABook(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.getLog().add(ShiftLog.Entry.ORES_MINED, 3);
		kumpel.getLog().add(ShiftLog.Entry.BLOCKS_DUG, 42);

		ItemStack book = kumpel.writeShiftReport();
		WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(book.is(Items.WRITTEN_BOOK) && content != null && content.pages().size() == 3,
				Component.literal("The report should be a written book with a cover and two pages of log"));
		helper.assertTrue(kumpel.getLog().get(ShiftLog.Entry.BLOCKS_DUG) == 42, Component.literal("The log keeps counting"));
		helper.succeed();
	}

	@GameTest
	public void rescueCapsulePullsTheCrewUpToTheSurface(GameTestHelper helper) {
		buildFloor(helper);
		buildRoof(helper, 3);
		Player player = ownerAt(helper, 2, 2);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 2);
		kumpel.tame(player);
		KumpelEntity sitter = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		sitter.tame(player);
		sitter.setOrderedToSit(true);
		BlockPos sitterPos = sitter.blockPosition();
		int roof = helper.absolutePos(new BlockPos(2, 3, 2)).getY();

		helper.assertTrue(RescueCapsuleItem.problem(helper.getLevel(), player) == null, Component.literal("Under a roof the capsule works"));
		helper.assertTrue(RescueCapsuleItem.pullUp(helper.getLevel(), player), Component.literal("There is a way up"));
		helper.assertTrue(player.getY() > roof, Component.literal("The player should be above the roof, is at " + player.blockPosition()));
		helper.assertTrue(kumpel.blockPosition().equals(player.blockPosition()), Component.literal("The Kumpel comes along, is at " + kumpel.blockPosition()));
		helper.assertTrue(sitter.blockPosition().equals(sitterPos), Component.literal("A sitting Kumpel stays where it is"));
		helper.succeed();
	}

	@GameTest
	public void exitTrailCutsLoopsAndNeedsTheSurface(GameTestHelper helper) {
		ExitTrail lost = new ExitTrail();
		lost.record(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), true);
		lost.record(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 7)), true);
		helper.assertFalse(lost.knowsTheWay(), Component.literal("Without having seen the surface there is no way out"));

		ExitTrail trail = new ExitTrail();
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(0, 5, 0)), false);
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0)), true);
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 0)), true);
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 5)), true);
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 5)), true);
		helper.assertTrue(trail.points().size() == 5, Component.literal("Surface plus four steps, got " + trail.points()));

		// Walking back towards the start cuts off the loop.
		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 1)), true);
		helper.assertTrue(trail.points().size() == 3 && trail.knowsTheWay(), Component.literal("The loop should be cut, got " + trail.points()));
		helper.assertTrue(trail.points().getFirst().equals(helper.absolutePos(new BlockPos(0, 5, 0))), Component.literal("The way out starts at the surface"));

		trail.record(helper.getLevel(), helper.absolutePos(new BlockPos(3, 5, 3)), false);
		helper.assertFalse(trail.knowsTheWay(), Component.literal("Back at the surface the trail starts over"));
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void leadsTheWayBackOut(GameTestHelper helper) {
		buildFloor(helper);
		buildRoof(helper, 3);
		Player owner = ownerAt(helper, 4, 4);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 6, 1, 6);
		kumpel.tame(owner);
		BlockPos exit = helper.absolutePos(new BlockPos(1, 1, 1));
		kumpel.getExitTrail().record(helper.getLevel(), exit, false);
		kumpel.getExitTrail().record(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 1)), true);
		kumpel.getExitTrail().record(helper.getLevel(), helper.absolutePos(new BlockPos(6, 1, 6)), true);
		kumpel.startLeadingOut();

		helper.succeedWhen(() -> {
			helper.assertFalse(kumpel.isLeadingOut(), Component.literal("The Kumpel should finish leading, is at " + kumpel.blockPosition()));
			helper.assertTrue(kumpel.distanceToSqr(Vec3.atBottomCenterOf(exit)) < 4.0,
					Component.literal("The Kumpel should end up at the exit, is at " + kumpel.blockPosition()));
		});
	}

	@GameTest(maxTicks = 200)
	public void catchesItsBreathWhileSitting(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.setOrderedToSit(true);
		float hurt = kumpel.getMaxHealth() - 4.0F;
		kumpel.setHealth(hurt);

		helper.succeedWhen(() -> helper.assertTrue(kumpel.getHealth() > hurt, Component.literal("A sitting Kumpel should heal up a little")));
	}

	@GameTest(maxTicks = 400)
	public void fieldForgeCokesSpareCoal(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		kumpel.setForge(true);
		kumpel.getPockets().addToPockets(new ItemStack(Items.COAL, 3));

		helper.succeedWhen(() -> {
			helper.assertTrue(count(kumpel.getPockets(), ModItems.COKE) == 2, Component.literal("Two coal should become coke, one feeds the fire (coke "
					+ count(kumpel.getPockets(), ModItems.COKE) + ", coal " + count(kumpel.getPockets(), Items.COAL) + ")"));
			helper.assertTrue(count(kumpel.getPockets(), Items.COAL) == 0, Component.literal("No coal left"));
			helper.assertTrue(kumpel.getLog().get(ShiftLog.Entry.COKE_MADE) == 2, Component.literal("The coke goes into the shift log"));
		});
	}

	@GameTest(maxTicks = 400)
	public void cokeIsFurnaceFuel(GameTestHelper helper) {
		helper.assertTrue(new ItemStack(ModItems.COKE).has(DataComponents.COOKING_FUEL), Component.literal("Coke has a burn time"));
		helper.setBlock(2, 1, 2, Blocks.FURNACE);
		Container furnace = container(helper, helper.absolutePos(new BlockPos(2, 1, 2)));
		furnace.setItem(0, new ItemStack(Items.RAW_IRON));
		furnace.setItem(1, new ItemStack(ModItems.COKE));

		helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT), Component.literal("A furnace fired with coke smelts raw iron")));
	}

	@GameTest
	public void remembersItsBestFindsForTheShiftReport(GameTestHelper helper) {
		buildFloor(helper);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		helper.setBlock(4, 1, 4, Blocks.DIAMOND_ORE);
		BlockPos diamond = helper.absolutePos(new BlockPos(4, 1, 4));
		for (int i = 0; i < OreFinds.MAX_FINDS + 3; i++) {
			BlockPos coal = helper.absolutePos(new BlockPos(i % 8, 2, i / 8));
			kumpel.getFinds().remember(coal, Blocks.COAL_ORE.defaultBlockState(), 10);
		}
		kumpel.getFinds().remember(diamond, Blocks.DIAMOND_ORE.defaultBlockState(), 70);

		helper.assertTrue(kumpel.getFinds().finds().size() == OreFinds.MAX_FINDS, Component.literal("Only the best finds are kept"));
		helper.assertTrue(kumpel.getFinds().finds().getFirst().pos().equals(diamond), Component.literal("The diamond comes first"));

		// The coal ore was never placed, so those finds are gone; the diamond is still there.
		WrittenBookContent content = kumpel.writeShiftReport().get(DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(kumpel.getFinds().finds().size() == 1, Component.literal("Ores that are gone are forgotten, left: " + kumpel.getFinds().finds()));
		helper.assertTrue(content != null && content.pages().size() == 4, Component.literal("Cover, two log pages and one page of finds"));

		helper.setBlock(4, 1, 4, Blocks.AIR);
		kumpel.getFinds().tidyUp(helper.getLevel());
		helper.assertTrue(kumpel.getFinds().finds().isEmpty(), Component.literal("A mined ore is forgotten"));
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void kumpelsSingAlongToTheirRecord(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		BlockPos jukeboxPos = helper.absolutePos(new BlockPos(5, 1, 5));
		helper.setBlock(5, 1, 5, Blocks.JUKEBOX);
		container(helper, jukeboxPos).setItem(0, new ItemStack(ModItems.MUSIC_DISC_GLUECK_AUF));
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 2, 1, 2);
		kumpel.tame(owner);
		kumpel.setOrderedToSit(false);

		helper.succeedWhen(() -> {
			helper.assertTrue(kumpel.isDancing(), Component.literal("The Kumpel should dance to its record"));
			helper.assertTrue(owner.hasEffect(MobEffects.HASTE), Component.literal("Their good mood gives the owner Haste"));
		});
	}

	@GameTest
	public void theRecordIsAJukeboxSong(GameTestHelper helper) {
		ItemStack disc = new ItemStack(ModItems.MUSIC_DISC_GLUECK_AUF);
		helper.assertTrue(disc.has(DataComponents.JUKEBOX_PLAYABLE), Component.literal("The disc plays in a jukebox"));
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(ModItems.GLUECK_AUF_SONG).isPresent(),
				Component.literal("The song kumpel:glueck_auf is loaded"));
		helper.succeed();
	}

	@GameTest
	public void markenkontrolleKeepsTrackOfKumpels(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 3, 1, 3);
		kumpel.tame(owner);
		kumpel.setCustomName(Component.literal("Markentest"));
		Markenkontrolle markenkontrolle = Markenkontrolle.get(helper.getLevel().getServer());
		long now = helper.getLevel().getServer().overworld().getGameTime();

		kumpel.hangUpMarke(helper.getLevel(), "mining");
		List<Markenkontrolle.Marke> marken = markenkontrolle.ofOwner(owner.getUUID(), now);
		helper.assertTrue(marken.size() == 1 && marken.getFirst().name().equals("Markentest") && marken.getFirst().pos().pos().equals(kumpel.blockPosition()),
				Component.literal("The Kumpel's tag hangs at the board: " + marken));

		kumpel.die(helper.getLevel().damageSources().generic());
		marken = markenkontrolle.ofOwner(owner.getUUID(), now);
		helper.assertTrue(marken.size() == 1 && marken.getFirst().isLost(), Component.literal("A Kumpel that died is marked as lost: " + marken));

		KumpelEntity revived = helper.spawn(ModEntities.KUMPEL, 4, 1, 4);
		revived.tame(owner);
		revived.setCustomName(Component.literal("Markentest"));
		revived.hangUpMarke(helper.getLevel(), "following");
		marken = markenkontrolle.ofOwner(owner.getUUID(), now);
		helper.assertTrue(marken.size() == 1 && !marken.getFirst().isLost() && marken.getFirst().kumpel().equals(revived.getUUID()),
				Component.literal("Brought back, the Kumpel replaces its old tag: " + marken));

		markenkontrolle.takeDown(revived.getUUID());
		helper.assertTrue(markenkontrolle.ofOwner(owner.getUUID(), now).isEmpty(), Component.literal("Packed up, the tag comes down"));
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void helmetLampLightsTheWayAndGoesOut(GameTestHelper helper) {
		buildFloor(helper);
		buildRoof(helper, 3);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 4, 1, 4);
		kumpel.tame(owner);
		kumpel.setOrderedToSit(true);
		BlockPos head = helper.absolutePos(new BlockPos(4, 2, 4));
		boolean[] lit = {false};

		helper.onEachTick(() -> {
			if (!lit[0] && helper.getLevel().getBlockState(head).is(ModBlocks.LAMP_LIGHT)) {
				lit[0] = true;
				helper.assertTrue(helper.getLevel().getBlockState(head).getLightEmission() > 0, Component.literal("The lamp gives off light"));
				kumpel.discard();
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(lit[0], Component.literal("Underground, the Kumpel's helmet lamp should light up"));
			helper.assertTrue(helper.getLevel().getBlockState(head).isAir(), Component.literal("Without its Kumpel, the lamp goes out"));
		});
	}

	@GameTest
	public void kumpelfibelIsABook(GameTestHelper helper) {
		WrittenBookContent content = new ItemStack(ModItems.KUMPELFIBEL).get(DataComponents.WRITTEN_BOOK_CONTENT);
		helper.assertTrue(content != null && content.pages().size() == KumpelFibel.PAGES, Component.literal("The Kumpelfibel has all its pages"));
		Set<String> english = languageKeys("en_us");
		for (int page = 0; page < KumpelFibel.PAGES; page++) {
			helper.assertTrue(english.contains("fibel.kumpel.page." + page), Component.literal("Page " + page + " has a text"));
		}
		helper.succeed();
	}

	@GameTest
	public void putsAWornOutPickaxeAwayBeforeItBreaks(GameTestHelper helper) {
		buildFloor(helper);
		helper.setBlock(3, 1, 3, Blocks.COAL_ORE);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 1, 1, 1);
		ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
		pickaxe.setDamageValue(pickaxe.getMaxDamage() - 4);
		kumpel.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
		helper.assertTrue(kumpel.isHauer(), Component.literal("Still good enough to work with"));

		kumpel.mineOre(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)));
		helper.assertTrue(kumpel.getMainHandItem().isEmpty(), Component.literal("The worn-out pickaxe is put away"));
		helper.assertTrue(count(kumpel.getPockets(), Items.IRON_PICKAXE) == 1, Component.literal("It is in the Kiepe, not broken"));
		helper.assertFalse(KumpelPockets.isPickaxe(ItemStack.EMPTY) || kumpel.isHauer(), Component.literal("Without a usable pickaxe it is no Hauer"));
		helper.succeed();
	}

	@GameTest
	public void favoriteFoodHealsCompletely(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity kumpel = helper.spawn(ModEntities.KUMPEL, 2, 1, 2);
		kumpel.tame(owner);
		Item favorite = kumpel.getFavoriteFood();
		helper.assertTrue(favorite != Items.AIR && KumpelSettings.get().behaviour().favoriteFoods.contains(
				net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(favorite).toString()), Component.literal("Its favourite comes from the config: " + favorite));
		helper.assertTrue(kumpel.getFavoriteFood() == favorite, Component.literal("It keeps its favourite"));

		kumpel.setHealth(2.0F);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(favorite, 2));
		kumpel.mobInteract(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(kumpel.getHealth() == kumpel.getMaxHealth(), Component.literal("Its favourite food heals it completely"));
		helper.assertTrue(owner.getMainHandItem().getCount() == 1, Component.literal("One portion is eaten"));
		helper.succeed();
	}

	@GameTest
	public void youngKumpelsLearnFromOldOnes(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = ownerAt(helper, 1, 1);
		KumpelEntity master = helper.spawn(ModEntities.KUMPEL, 2, 1, 2);
		master.tame(owner);
		master.addExperience(KumpelSettings.get().tiers().getLast().requiredExperience());
		KumpelEntity apprentice = helper.spawn(ModEntities.KUMPEL, 4, 1, 4);
		apprentice.tame(owner);

		int learned = apprentice.learnFromOthers(helper.getLevel());
		helper.assertTrue(learned > 0 && apprentice.getExperience() == learned, Component.literal("The apprentice learns from the master, got " + learned));
		helper.assertTrue(master.learnFromOthers(helper.getLevel()) == 0, Component.literal("The master has nothing to learn from the apprentice"));
		helper.succeed();
	}

	@GameTest
	public void muckefuckGivesHaste(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MINING_FATIGUE, 600));
		ItemStack rest = new ItemStack(ModItems.MUCKEFUCK).finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(player.hasEffect(MobEffects.HASTE), Component.literal("Muckefuck gives Haste"));
		helper.assertFalse(player.hasEffect(MobEffects.MINING_FATIGUE), Component.literal("and shakes off Mining Fatigue"));
		helper.assertTrue(rest.is(Items.GLASS_BOTTLE) || findInInventory(player, Items.GLASS_BOTTLE).getCount() == 1,
				Component.literal("The bottle is left over"));
		helper.assertTrue(new ItemStack(ModItems.KNIFTE).has(DataComponents.FOOD), Component.literal("A Knifte is food"));
		helper.succeed();
	}

	@GameTest
	public void everyLanguageHasEveryText(GameTestHelper helper) {
		Set<String> english = languageKeys("en_us");
		for (String language : List.of("de_de", "pl_pl", "tr_tr", "nl_nl", "fr_fr", "es_es")) {
			Set<String> keys = languageKeys(language);
			helper.assertTrue(keys.equals(english), Component.literal(language + " differs from en_us: "
					+ english.stream().filter(key -> !keys.contains(key)).toList() + " missing, "
					+ keys.stream().filter(key -> !english.contains(key)).toList() + " extra"));
		}
		helper.succeed();
	}

	private static Set<String> languageKeys(String language) {
		String path = "/assets/kumpel/lang/" + language + ".json";
		try (InputStream stream = Kumpel.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing " + path);
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			return json.keySet();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	@GameTest
	public void allAdvancementsAreLoaded(GameTestHelper helper) {
		for (String name : KumpelAdvancements.ALL) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Kumpel.id(name)) != null,
					Component.literal("Advancement kumpel:" + name + " is missing or broken"));
		}
		helper.succeed();
	}

	private static Player ownerAt(GameTestHelper helper, int x, int z) {
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		owner.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, z))));
		return owner;
	}

	private static Container container(GameTestHelper helper, BlockPos absolutePos) {
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolutePos);
		if (!(blockEntity instanceof Container container)) {
			throw new IllegalStateException("No container at " + absolutePos + ": " + blockEntity);
		}

		return container;
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

	/** A stone block from x = 3 to 7, three blocks high and three deep (z = 2 to 4), standing on the floor. */
	private static void buildWall(GameTestHelper helper) {
		buildWall(helper, Blocks.STONE);
	}

	private static void buildWall(GameTestHelper helper, Block block) {
		for (int x = 3; x <= 7; x++) {
			for (int y = 1; y <= 3; y++) {
				for (int z = 2; z <= 4; z++) {
					helper.setBlock(x, y, z, block);
				}
			}
		}
	}

	private static void buildRoof(GameTestHelper helper, int y) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, y, z, Blocks.STONE);
			}
		}
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
