package io.github.chaos634.zechenbau.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.zechenbau.Zechenbau;
import io.github.chaos634.zechenbau.registry.ZechenbauBlocks;

public class ZechenbauGameTests {
	private static final List<String> LANGUAGES = List.of("de_de", "pl_pl", "tr_tr", "nl_nl", "fr_fr", "es_es");
	private static final List<String> RECIPES = List.of("zechenziegel", "zechenziegel_stairs", "zechenziegel_slab", "zechenziegel_wall",
			"zechenziegel_stairs_from_zechenziegel_stonecutting", "zechenziegel_slab_from_zechenziegel_stonecutting",
			"zechenziegel_wall_from_zechenziegel_stonecutting", "schlaegel_und_eisen_from_zechenziegel_stonecutting",
			"stahlfachwerk", "fachwerk_fenster", "stahltraeger", "foerdergeruest", "grubenlampe");

	@GameTest
	public void everyBlockHasAnItem(GameTestHelper helper) {
		helper.assertTrue(ZechenbauBlocks.all().size() == 10, Component.literal("Expected 10 blocks, got " + ZechenbauBlocks.all().size()));
		for (Block block : ZechenbauBlocks.all()) {
			helper.assertTrue(block.asItem() != Items.AIR, Component.literal(BuiltInRegistries.BLOCK.getKey(block) + " has no item"));
		}
		helper.succeed();
	}

	@GameTest
	public void everyBlockDropsItself(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		for (Block block : ZechenbauBlocks.all()) {
			List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), helper.getLevel(), pos, null);
			helper.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount() == 1,
					Component.literal(BuiltInRegistries.BLOCK.getKey(block) + " drops " + drops));
		}
		helper.succeed();
	}

	@GameTest
	public void doubleSlabDropsTwo(GameTestHelper helper) {
		BlockState doubleSlab = ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
		List<ItemStack> drops = Block.getDrops(doubleSlab, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null);
		helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 2, Component.literal("A double slab should drop two slabs, got " + drops));
		helper.succeed();
	}

	@GameTest
	public void blocksNeedAPickaxe(GameTestHelper helper) {
		for (Block block : ZechenbauBlocks.all()) {
			helper.assertTrue(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
					Component.literal(BuiltInRegistries.BLOCK.getKey(block) + " is not mineable with a pickaxe"));
		}
		helper.assertTrue(ZechenbauBlocks.ZECHENZIEGEL_WALL.defaultBlockState().is(BlockTags.WALLS), Component.literal("The wall must be tagged, or it won't connect"));
		helper.assertTrue(ZechenbauBlocks.ZECHENZIEGEL_STAIRS.defaultBlockState().is(BlockTags.STAIRS), Component.literal("The stairs are not tagged"));
		helper.assertTrue(ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState().is(BlockTags.SLABS), Component.literal("The slab is not tagged"));
		helper.succeed();
	}

	@GameTest
	public void grubenlampeGlowsStandingAndHanging(GameTestHelper helper) {
		BlockState standing = ZechenbauBlocks.GRUBENLAMPE.defaultBlockState();
		BlockState hanging = standing.setValue(LanternBlock.HANGING, true);
		helper.assertTrue(standing.getLightEmission() == 14 && hanging.getLightEmission() == 14, Component.literal("The Grubenlampe should shine at 14"));

		BlockPos lamp = new BlockPos(1, 3, 1);
		helper.setBlock(lamp.above(), ZechenbauBlocks.STAHLTRAEGER);
		helper.setBlock(lamp, hanging);
		helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(lamp.below())) >= 12,
				Component.literal("No light below the hanging lamp")));
	}

	@GameTest
	public void windowsAndLatticeDontSuffocate(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		for (Block block : List.of(ZechenbauBlocks.FACHWERK_FENSTER, ZechenbauBlocks.FOERDERGERUEST)) {
			helper.assertFalse(block.defaultBlockState().isSuffocating(helper.getLevel(), pos),
					Component.literal(BuiltInRegistries.BLOCK.getKey(block) + " should not suffocate"));
		}
		helper.assertTrue(ZechenbauBlocks.ZECHENZIEGEL.defaultBlockState().isSuffocating(helper.getLevel(), pos), Component.literal("Bricks are solid"));
		helper.succeed();
	}

	@GameTest
	public void everyRecipeIsLoaded(GameTestHelper helper) {
		for (String name : RECIPES) {
			helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Zechenbau.id(name))).isPresent(),
					Component.literal("Recipe zechenbau:" + name + " is missing or broken"));
		}
		helper.succeed();
	}

	@GameTest
	public void everyBlockHasAName(GameTestHelper helper) {
		Set<String> english = languageKeys("en_us");
		helper.assertTrue(english.contains("itemGroup.zechenbau"), Component.literal("The creative tab has no name"));
		for (Block block : ZechenbauBlocks.all()) {
			helper.assertTrue(english.contains(block.getDescriptionId()), Component.literal(block.getDescriptionId() + " has no English name"));
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
		String path = "/assets/zechenbau/lang/" + language + ".json";
		try (InputStream stream = Zechenbau.class.getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().keySet();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}
}
