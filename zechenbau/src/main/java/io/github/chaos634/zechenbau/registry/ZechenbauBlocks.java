package io.github.chaos634.zechenbau.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import io.github.chaos634.zechenbau.Zechenbau;
import io.github.chaos634.zechenbau.block.FacingDecorBlock;

/** Every block of the mod, each with its item, in the order they appear in the creative tab. */
public final class ZechenbauBlocks {
	private static final List<Block> ALL = new ArrayList<>();

	public static final Block ZECHENZIEGEL = register("zechenziegel", Block::new, brick());
	public static final Block ZECHENZIEGEL_STAIRS = register("zechenziegel_stairs",
			properties -> new StairBlock(ZECHENZIEGEL.defaultBlockState(), properties), brick());
	public static final Block ZECHENZIEGEL_SLAB = register("zechenziegel_slab", SlabBlock::new, brick());
	public static final Block ZECHENZIEGEL_WALL = register("zechenziegel_wall", WallBlock::new, brick().forceSolidOn());
	public static final Block RISSIGE_ZECHENZIEGEL = register("rissige_zechenziegel", Block::new, brick());
	public static final Block BEMOOSTE_ZECHENZIEGEL = register("bemooste_zechenziegel", Block::new, brick());
	public static final Block STAHLFACHWERK = register("stahlfachwerk", Block::new, brick());
	public static final Block FACHWERK_FENSTER = register("fachwerk_fenster", TransparentBlock::new, seeThrough(steel()));
	public static final Block STAHLTRAEGER = register("stahltraeger", RotatedPillarBlock::new, steel());
	public static final Block FOERDERGERUEST = register("foerdergeruest", TransparentBlock::new, seeThrough(steel()));
	public static final Block GRUBENLAMPE = register("grubenlampe", LanternBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).forceSolidOn().strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> 14).noOcclusion());
	public static final Block SCHLAEGEL_UND_EISEN = register("schlaegel_und_eisen", Block::new, brick());
	/** Seilscheibe: the big wheel at the top of a headframe that the hoisting rope runs over. */
	public static final Block SEILSCHEIBE = register("seilscheibe",
			properties -> new FacingDecorBlock(properties, 0, 0, 6, 16, 16, 10), steel().noOcclusion());
	/** Hunt: a mine tub full of coal. */
	public static final Block HUNT = register("hunt",
			properties -> new FacingDecorBlock(properties, 1, 0, 1, 15, 13, 15), steel().noOcclusion());
	/** Kauenhaken: in the Waschkaue, the miners' clothes hung on hooks pulled up to the ceiling on chains. */
	public static final Block KAUENHAKEN = register("kauenhaken",
			properties -> new FacingDecorBlock(properties, 3, 1, 6, 13, 16, 10), BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLUE).strength(0.8F).sound(SoundType.CHAIN).noOcclusion());

	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Zechenbau.id("zechenbau"));
	public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.zechenbau"))
			.icon(() -> new ItemStack(STAHLFACHWERK))
			.displayItems((parameters, output) -> ALL.forEach(output::accept))
			.build();

	private ZechenbauBlocks() {
	}

	private static BlockBehaviour.Properties brick() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE);
	}

	private static BlockBehaviour.Properties steel() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL);
	}

	/** For blocks you can look (but not walk) through: windows and lattice. */
	private static BlockBehaviour.Properties seeThrough(BlockBehaviour.Properties properties) {
		return properties.noOcclusion()
				.isValidSpawn((state, level, pos, type) -> false)
				.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false);
	}

	/** Registers a block together with its item. */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Zechenbau.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Zechenbau.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		ALL.add(block);
		return block;
	}

	public static List<Block> all() {
		return List.copyOf(ALL);
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> ALL.forEach(output::accept));
	}
}
