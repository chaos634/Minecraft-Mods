package io.github.chaos634.taubenschlag.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.advancement.TaubenschlagAdvancements;
import io.github.chaos634.taubenschlag.block.TaubenschlagBlockEntity;
import io.github.chaos634.taubenschlag.entity.BrieftaubeData;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;
import io.github.chaos634.taubenschlag.entity.BrieftaubeVariant;
import io.github.chaos634.taubenschlag.flight.Flugplan;
import io.github.chaos634.taubenschlag.item.ReisekorbItem;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagComponents;
import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;
import io.github.chaos634.taubenschlag.registry.TaubenschlagItems;

public class TaubenschlagGameTests {
	@GameTest
	public void wildPigeonsAreTamedWithSeeds(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BrieftaubeEntity pigeon = helper.spawn(TaubenschlagEntities.BRIEFTAUBE, 2, 1, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 64));

		for (int i = 0; i < 64 && !pigeon.isTame(); i++) {
			pigeon.mobInteract(player, InteractionHand.MAIN_HAND);
		}

		helper.assertTrue(pigeon.isTame() && pigeon.isOwnedBy(player), Component.literal("Seeds should tame a wild pigeon sooner or later"));
		helper.assertTrue(pigeon.getRing().matches("DV-\\d{5}-\\d{2}-\\d{3}"), Component.literal("A tame pigeon wears a ring, got '" + pigeon.getRing() + "'"));
		helper.assertTrue(player.getMainHandItem().getCount() < 64, Component.literal("Taming uses up seeds"));
		helper.succeed();
	}

	@GameTest
	public void pigeonsDoNotTakeFallDamage(GameTestHelper helper) {
		buildFloor(helper);
		BrieftaubeEntity pigeon = helper.spawn(TaubenschlagEntities.BRIEFTAUBE, 2, 1, 2);
		float health = pigeon.getHealth();

		pigeon.causeFallDamage(20.0, 1.0F, helper.getLevel().damageSources().fall());

		helper.assertTrue(pigeon.getHealth() == health, Component.literal("Pigeons fly, they don't fall"));
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void tamePigeonSettlesIntoANearbyLoft(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 5, 5);
		BlockPos loft = helper.absolutePos(new BlockPos(1, 1, 1));

		helper.succeedWhen(() -> helper.assertTrue(pigeon.getHome() != null && pigeon.getHome().pos().equals(loft),
				Component.literal("The pigeon should have settled into the loft, home is " + pigeon.getHome())));
	}

	@GameTest(maxTicks = 200)
	public void pigeonForgetsALoftThatIsGone(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 5, 5);
		pigeon.setHome(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(1, 1, 1))));

		helper.succeedWhen(() -> helper.assertTrue(pigeon.getHome() == null, Component.literal("Without its loft standing, the pigeon should forget it")));
	}

	@GameTest(maxTicks = 300)
	public void pigeonCarriesPostHome(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		BlockPos loft = helper.absolutePos(new BlockPos(1, 1, 1));
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 6, 6);
		pigeon.setHome(GlobalPos.of(helper.getLevel().dimension(), loft));
		String ring = pigeon.getRing();

		boolean tookOff = pigeon.takeOff(helper.getLevel(), player, new ItemStack(Items.COAL, 16));
		helper.assertTrue(tookOff && pigeon.isRemoved(), Component.literal("The pigeon should have flown off"));
		helper.assertTrue(Flugplan.get(helper.getLevel().getServer()).flights().stream().anyMatch(flight -> flight.pigeon().ring().equals(ring)),
				Component.literal("The flight should be on the Flugplan"));

		helper.succeedWhen(() -> {
			TaubenschlagBlockEntity schlag = schlag(helper, loft);
			helper.assertTrue(count(schlag, Items.COAL) == 16, Component.literal("The coal should be in the loft, found " + count(schlag, Items.COAL)));
			List<BrieftaubeEntity> back = pigeonsWithRing(helper, loft, ring);
			helper.assertTrue(back.size() == 1, Component.literal("The pigeon should be back at its loft, found " + back.size()));
			helper.assertTrue(back.getFirst().getFlights() == 1 && back.getFirst().isTame() && back.getFirst().getHome() != null,
					Component.literal("It is still tame, has its loft and one flight more"));
		});
	}

	@GameTest(maxTicks = 300)
	public void fullLoftLeavesThePostInFront(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		BlockPos loft = helper.absolutePos(new BlockPos(1, 1, 1));
		TaubenschlagBlockEntity schlag = schlag(helper, loft);
		for (int i = 0; i < schlag.getContainerSize(); i++) {
			schlag.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		}
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 6, 6);
		pigeon.setHome(GlobalPos.of(helper.getLevel().dimension(), loft));
		pigeon.takeOff(helper.getLevel(), player, new ItemStack(Items.DIRT, 8));

		helper.succeedWhen(() -> {
			List<ItemEntity> dirt = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(loft).inflate(3.0), item -> item.getItem().is(Items.DIRT));
			helper.assertTrue(dirt.stream().mapToInt(item -> item.getItem().getCount()).sum() == 8, Component.literal("The post should lie in front of the full loft"));
		});
	}

	@GameTest
	public void pigeonWithoutALoftStaysPut(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 3, 3);

		helper.assertFalse(pigeon.takeOff(helper.getLevel(), player, new ItemStack(Items.PAPER)), Component.literal("Without a loft it has nowhere to fly"));
		helper.assertFalse(pigeon.isRemoved(), Component.literal("The pigeon should still be here"));
		helper.succeed();
	}

	@GameTest
	public void basketCarriesAPigeonToANewLoft(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		helper.setBlock(6, 1, 6, TaubenschlagBlocks.TAUBENSCHLAG);
		BlockPos oldLoft = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos newLoft = helper.absolutePos(new BlockPos(6, 1, 6));
		BrieftaubeEntity pigeon = tamePigeon(helper, player, 3, 3);
		pigeon.setHome(GlobalPos.of(helper.getLevel().dimension(), oldLoft));
		pigeon.setCustomName(Component.literal("Hansi"));
		pigeon.setVariant(BrieftaubeVariant.GEHAEMMERT);
		String ring = pigeon.getRing();
		ItemStack basket = new ItemStack(TaubenschlagItems.REISEKORB);

		InteractionResult result = ReisekorbItem.catchPigeon(basket, player, pigeon);
		helper.assertTrue(result.consumesAction() && pigeon.isRemoved(), Component.literal("The pigeon should be in the basket"));
		BrieftaubeData data = basket.get(TaubenschlagComponents.BRIEFTAUBE);
		helper.assertTrue(data != null && data.ring().equals(ring) && data.name().orElse("").equals("Hansi")
				&& data.plumage() == BrieftaubeVariant.GEHAEMMERT && data.home().map(home -> home.pos().equals(oldLoft)).orElse(false),
				Component.literal("The basket knows its pigeon: " + data));

		BrieftaubeEntity out = ReisekorbItem.release(helper.getLevel(), basket, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 5))), 0.0F, newLoft);
		helper.assertTrue(out != null && !ReisekorbItem.hasPigeon(basket), Component.literal("The pigeon should be out of the basket"));
		helper.assertTrue(out.getRing().equals(ring) && out.isTame() && "Hansi".equals(out.getCustomName().getString()) && out.getVariant() == BrieftaubeVariant.GEHAEMMERT,
				Component.literal("It is the same pigeon"));
		helper.assertTrue(out.getHome() != null && out.getHome().pos().equals(newLoft), Component.literal("Let out at a loft, it settles in there"));
		helper.succeed();
	}

	@GameTest
	public void strangersPigeonsStayOutOfTheBasket(GameTestHelper helper) {
		buildFloor(helper);
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		BrieftaubeEntity pigeon = tamePigeon(helper, owner, 3, 3);
		BrieftaubeEntity wild = helper.spawn(TaubenschlagEntities.BRIEFTAUBE, 5, 1, 5);
		ItemStack basket = new ItemStack(TaubenschlagItems.REISEKORB);

		ReisekorbItem.catchPigeon(basket, stranger, pigeon);
		ReisekorbItem.catchPigeon(basket, stranger, wild);
		helper.assertFalse(pigeon.isRemoved() || wild.isRemoved() || ReisekorbItem.hasPigeon(basket),
				Component.literal("Only your own tame pigeons go into your basket"));
		helper.succeed();
	}

	@GameTest
	public void youngPigeonsBelongToTheOwner(GameTestHelper helper) {
		buildFloor(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BrieftaubeEntity father = tamePigeon(helper, player, 2, 2);
		BrieftaubeEntity mother = tamePigeon(helper, player, 4, 4);
		father.setVariant(BrieftaubeVariant.ROT);
		mother.setVariant(BrieftaubeVariant.WEISS);

		AgeableMob young = father.getBreedOffspring(helper.getLevel(), mother);
		helper.assertTrue(young instanceof BrieftaubeEntity, Component.literal("Two pigeons make a pigeon"));
		BrieftaubeEntity squab = (BrieftaubeEntity) young;
		helper.assertTrue(squab.isTame() && squab.getOwnerReference() != null && squab.getOwnerReference().getUUID().equals(player.getUUID()),
				Component.literal("The young bird belongs to the owner of its parents"));
		helper.assertTrue(squab.getVariant() == BrieftaubeVariant.ROT || squab.getVariant() == BrieftaubeVariant.WEISS,
				Component.literal("It has the plumage of a parent, got " + squab.getVariant()));
		helper.assertFalse(squab.getRing().isEmpty(), Component.literal("It gets its own ring"));
		helper.succeed();
	}

	@GameTest
	public void longerFlightsTakeLonger(GameTestHelper helper) {
		helper.assertTrue(Flugplan.flightTicks(0, 18.0) == Flugplan.MIN_FLIGHT_TICKS, Component.literal("Even a short hop takes a moment"));
		helper.assertTrue(Flugplan.flightTicks(1800, 18.0) == Flugplan.MIN_FLIGHT_TICKS + 2000, Component.literal("1800 blocks at 18 blocks/s take 100 s"));
		helper.assertTrue(Flugplan.flightTicks(100, 18.0) < Flugplan.flightTicks(1000, 18.0), Component.literal("Further takes longer"));
		helper.assertTrue(Flugplan.formatTime(1300).equals("1:05"), Component.literal("Times read like 1:05, got " + Flugplan.formatTime(1300)));
		helper.succeed();
	}

	@GameTest
	public void loftTellsComparatorsAboutPost(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		TaubenschlagBlockEntity schlag = schlag(helper, helper.absolutePos(new BlockPos(1, 1, 1)));
		helper.assertTrue(schlag.signal() == 0, Component.literal("An empty loft gives no signal"));

		ItemStack rest = schlag.receive(new ItemStack(Items.PAPER, 100));
		helper.assertTrue(rest.isEmpty() && count(schlag, Items.PAPER) == 100, Component.literal("Post goes into the nest boxes"));
		helper.assertTrue(schlag.signal() > 0, Component.literal("A loft with post gives a signal"));
		helper.succeed();
	}

	@GameTest
	public void brokenLoftDropsItsPost(GameTestHelper helper) {
		buildFloor(helper);
		helper.setBlock(1, 1, 1, TaubenschlagBlocks.TAUBENSCHLAG);
		BlockPos loft = helper.absolutePos(new BlockPos(1, 1, 1));
		schlag(helper, loft).receive(new ItemStack(Items.EMERALD, 3));

		helper.getLevel().destroyBlock(loft, true);

		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(loft).inflate(2.0));
		helper.assertTrue(drops.stream().filter(item -> item.getItem().is(Items.EMERALD)).mapToInt(item -> item.getItem().getCount()).sum() == 3,
				Component.literal("The post in the loft should drop"));
		helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(TaubenschlagBlocks.TAUBENSCHLAG_ITEM)), Component.literal("The loft drops itself"));
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

	@GameTest
	public void allAdvancementsAreLoaded(GameTestHelper helper) {
		for (String name : TaubenschlagAdvancements.ALL) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Taubenschlag.id(name)) != null,
					Component.literal("Advancement taubenschlag:" + name + " is missing or broken"));
		}
		helper.succeed();
	}

	private static Set<String> languageKeys(String language) {
		String path = "/assets/taubenschlag/lang/" + language + ".json";
		try (InputStream stream = Taubenschlag.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing " + path);
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			return json.keySet();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	/** A tame pigeon that sits where it is put, so it does not fly off during the test. */
	private static BrieftaubeEntity tamePigeon(GameTestHelper helper, Player owner, int x, int z) {
		BrieftaubeEntity pigeon = helper.spawn(TaubenschlagEntities.BRIEFTAUBE, x, 1, z);
		pigeon.tameBy(owner);
		pigeon.setOrderedToSit(true);
		return pigeon;
	}

	private static List<BrieftaubeEntity> pigeonsWithRing(GameTestHelper helper, BlockPos near, String ring) {
		ServerLevel level = helper.getLevel();
		return level.getEntitiesOfClass(BrieftaubeEntity.class, new AABB(near).inflate(3.0), pigeon -> pigeon.getRing().equals(ring));
	}

	private static TaubenschlagBlockEntity schlag(GameTestHelper helper, BlockPos absolutePos) {
		if (!(helper.getLevel().getBlockEntity(absolutePos) instanceof TaubenschlagBlockEntity schlag)) {
			throw new IllegalStateException("No loft at " + absolutePos);
		}

		return schlag;
	}

	private static int count(TaubenschlagBlockEntity schlag, Item item) {
		int count = 0;
		for (int i = 0; i < schlag.getContainerSize(); i++) {
			ItemStack stack = schlag.getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	private static void buildFloor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}
}
