package io.github.chaos634.zechenbau.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.chaos634.zechenbau.Zechenbau;
import io.github.chaos634.zechenbau.registry.ZechenbauBlocks;

public class ZechenbauGameTests {
	private static final List<String> LANGUAGES = List.of("de_de", "pl_pl", "tr_tr", "nl_nl", "fr_fr", "es_es");
	private static final List<String> RECIPES = List.of("zechenziegel", "zechenziegel_stairs", "zechenziegel_slab", "zechenziegel_wall",
			"zechenziegel_stairs_from_zechenziegel_stonecutting", "zechenziegel_slab_from_zechenziegel_stonecutting",
			"zechenziegel_wall_from_zechenziegel_stonecutting", "schlaegel_und_eisen_from_zechenziegel_stonecutting",
			"stahlfachwerk", "fachwerk_fenster", "stahltraeger", "foerdergeruest", "grubenlampe",
			"rissige_zechenziegel", "bemooste_zechenziegel_from_vine", "bemooste_zechenziegel_from_moss_block", "hunt", "seilscheibe", "kauenhaken");

	@GameTest
	public void everyBlockHasAnItem(GameTestHelper helper) {
		helper.assertTrue(ZechenbauBlocks.all().size() == 15, Component.literal("Expected 15 blocks, got " + ZechenbauBlocks.all().size()));
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
	public void doubleSlabDropsLikeVanilla(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState doubleSlab = ZechenbauBlocks.ZECHENZIEGEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
		BlockState vanilla = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
		List<ItemStack> drops = Block.getDrops(doubleSlab, helper.getLevel(), pos, null);
		List<ItemStack> vanillaDrops = Block.getDrops(vanilla, helper.getLevel(), pos, null);
		helper.assertTrue(drops.size() == 1 && vanillaDrops.size() == 1 && drops.getFirst().getCount() == vanillaDrops.getFirst().getCount(),
				Component.literal("A double slab should drop like a vanilla one: got " + drops + ", vanilla " + vanillaDrops));
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

		// It hangs from a girder and stays there when its neighbours change.
		BlockPos lamp = new BlockPos(1, 3, 1);
		helper.setBlock(lamp.above(), ZechenbauBlocks.STAHLTRAEGER);
		helper.setBlock(lamp, hanging);
		helper.setBlock(lamp.below(), Blocks.STONE);
		helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(lamp)).is(ZechenbauBlocks.GRUBENLAMPE)
				&& hanging.canSurvive(helper.getLevel(), helper.absolutePos(lamp)), Component.literal("The lamp should hang from the girder"));
		helper.succeed();
	}

	@GameTest
	public void windowsAndLatticeDontSuffocate(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		for (Block block : List.of(ZechenbauBlocks.FACHWERK_FENSTER, ZechenbauBlocks.FOERDERGERUEST)) {
			helper.assertFalse(block.defaultBlockState().isSuffocating(helper.getLevel(), pos),
					Component.literal(BuiltInRegistries.BLOCK.getKey(block) + " should not suffocate"));
		}
		helper.succeed();
	}

	@GameTest
	public void decorBlocksTurnTheirShapeWithThem(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		AABB north = ZechenbauBlocks.SEILSCHEIBE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
				.getShape(helper.getLevel(), pos).bounds();
		AABB east = ZechenbauBlocks.SEILSCHEIBE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
				.getShape(helper.getLevel(), pos).bounds();
		helper.assertTrue(north.getZsize() < 0.5 && north.getXsize() == 1.0, Component.literal("Facing north, the wheel is thin north to south: " + north));
		helper.assertTrue(east.getXsize() < 0.5 && east.getZsize() == 1.0, Component.literal("Facing east, the wheel is thin east to west: " + east));

		AABB hunt = ZechenbauBlocks.HUNT.defaultBlockState().getShape(helper.getLevel(), pos).bounds();
		helper.assertTrue(hunt.getYsize() < 1.0, Component.literal("A Hunt is lower than a block: " + hunt));
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
	public void bauplaeneForKumpelsUseRealBlocks(GameTestHelper helper) {
		Set<String> english = languageKeys("en_us");
		for (String plan : List.of("foerdergeruest", "maschinenhaus")) {
			String path = "/data/zechenbau/kumpel/bauplan/" + plan + ".json";
			try (InputStream stream = Zechenbau.class.getResourceAsStream(path)) {
				helper.assertTrue(stream != null, Component.literal("Missing " + path));
				JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("palette").entrySet()) {
					String block = entry.getValue().getAsString().split("\\[")[0];
					helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Identifier.parse(block)), Component.literal(plan + " uses unknown block " + block));
				}
				helper.assertTrue(json.getAsJsonArray("layers").size() > 0, Component.literal(plan + " has no layers"));
			} catch (IOException e) {
				throw new IllegalStateException("Could not read " + path, e);
			}
			helper.assertTrue(english.contains("bauplan.zechenbau." + plan), Component.literal("The Bauplan " + plan + " has no name"));
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
