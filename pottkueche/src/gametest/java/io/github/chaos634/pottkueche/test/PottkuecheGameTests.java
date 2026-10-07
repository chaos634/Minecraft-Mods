package io.github.chaos634.pottkueche.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.pottkueche.Pottkueche;
import io.github.chaos634.pottkueche.registry.PottkuecheItems;

public class PottkuecheGameTests {
	private static final List<String> LANGUAGES = List.of("de_de", "pl_pl", "tr_tr", "nl_nl", "fr_fr", "es_es");
	private static final List<String> RECIPES = List.of("rohe_bratwurst", "bratwurst", "bratwurst_from_smoking", "bratwurst_from_campfire_cooking",
			"currysosse", "currywurst", "pommes", "currywurst_pommes", "rohe_frikadelle", "frikadelle", "frikadelle_from_smoking",
			"frikadelle_from_campfire_cooking", "pfefferpotthast", "malzbier");

	@GameTest
	public void everythingButTheSauceIsFood(GameTestHelper helper) {
		helper.assertTrue(PottkuecheItems.all().size() == 10, Component.literal("Expected 10 items, got " + PottkuecheItems.all().size()));
		for (Item item : PottkuecheItems.all()) {
			boolean food = new ItemStack(item).has(DataComponents.FOOD);
			helper.assertTrue(food == (item != PottkuecheItems.CURRYSOSSE),
					Component.literal(BuiltInRegistries.ITEM.getKey(item) + (food ? " should not be food" : " should be food")));
		}
		int currywurst = new ItemStack(PottkuecheItems.CURRYWURST).get(DataComponents.FOOD).nutrition();
		int bratwurst = new ItemStack(PottkuecheItems.BRATWURST).get(DataComponents.FOOD).nutrition();
		int pommes = new ItemStack(PottkuecheItems.CURRYWURST_POMMES).get(DataComponents.FOOD).nutrition();
		helper.assertTrue(bratwurst < currywurst && currywurst < pommes, Component.literal("Sauce and Pommes make it better"));
		helper.succeed();
	}

	@GameTest
	public void theSauceBottleStaysBehind(GameTestHelper helper) {
		helper.assertTrue(PottkuecheItems.CURRYSOSSE.getCraftingRemainder().is(Items.GLASS_BOTTLE),
				Component.literal("Cooking with curry sauce gives the bottle back"));
		helper.succeed();
	}

	@GameTest
	public void currywurstKeepsYouGoing(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getFoodData().setFoodLevel(4);
		ItemStack currywurst = new ItemStack(PottkuecheItems.CURRYWURST);
		player.setItemInHand(InteractionHand.MAIN_HAND, currywurst);
		currywurst.finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(player.getFoodData().getFoodLevel() == 12, Component.literal("A Currywurst fills 8 hunger points, got " + player.getFoodData().getFoodLevel()));
		helper.assertTrue(player.hasEffect(MobEffects.SPEED), Component.literal("A Currywurst makes you quick"));
		helper.succeed();
	}

	@GameTest
	public void stewAndBeerGiveTheirContainerBack(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.getFoodData().setFoodLevel(2);
		ItemStack stew = new ItemStack(PottkuecheItems.PFEFFERPOTTHAST);
		player.setItemInHand(InteractionHand.MAIN_HAND, stew);
		helper.assertTrue(stew.finishUsingItem(helper.getLevel(), player).is(Items.BOWL), Component.literal("The Pfefferpotthast bowl comes back"));

		ItemStack beer = new ItemStack(PottkuecheItems.MALZBIER);
		player.setItemInHand(InteractionHand.MAIN_HAND, beer);
		helper.assertTrue(beer.finishUsingItem(helper.getLevel(), player).is(Items.GLASS_BOTTLE), Component.literal("The Malzbier bottle comes back"));
		helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), Component.literal("Malzbier heals a little"));
		helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void bratwurstCooksInAFurnace(GameTestHelper helper) {
		helper.setBlock(2, 1, 2, Blocks.FURNACE);
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2))) instanceof Container furnace)) {
			throw new IllegalStateException("No furnace");
		}
		furnace.setItem(0, new ItemStack(PottkuecheItems.ROHE_BRATWURST));
		furnace.setItem(1, new ItemStack(Items.COAL));

		helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(2).is(PottkuecheItems.BRATWURST), Component.literal("The raw Bratwurst should cook")));
	}

	@GameTest
	public void everyRecipeIsLoaded(GameTestHelper helper) {
		for (String name : RECIPES) {
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Pottkueche.id(name))).isPresent(),
					Component.literal("Recipe pottkueche:" + name + " is missing or broken"));
		}
		helper.succeed();
	}

	@GameTest
	public void everyItemHasAName(GameTestHelper helper) {
		Set<String> english = languageKeys("en_us");
		helper.assertTrue(english.contains("itemGroup.pottkueche"), Component.literal("The creative tab has no name"));
		for (Item item : PottkuecheItems.all()) {
			helper.assertTrue(english.contains(item.getDescriptionId()), Component.literal(item.getDescriptionId() + " has no English name"));
		}
		helper.succeed();
	}

	@GameTest
	public void everyLanguageHasEveryText(GameTestHelper helper) {
		Set<String> english = languageKeys("en_us");
		for (String language : LANGUAGES) {
			Set<String> keys = languageKeys(language);
			helper.assertTrue(keys.equals(english), Component.literal(language + " differs from en_us: "
					+ english.stream().filter(key -> !keys.contains(key)).toList() + " missing, "
					+ keys.stream().filter(key -> !english.contains(key)).toList() + " extra"));
		}
		helper.succeed();
	}

	private static Set<String> languageKeys(String language) {
		String path = "/assets/pottkueche/lang/" + language + ".json";
		try (InputStream stream = Pottkueche.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().keySet();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}
}
