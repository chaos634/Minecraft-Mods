package io.github.chaos634.kumpel.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.block.FoerderkorbBlock;
import io.github.chaos634.kumpel.block.LampLightBlock;
import io.github.chaos634.kumpel.block.MarkentafelBlock;

public final class ModBlocks {
	public static final Block FOERDERKORB = register("foerderkorb", FoerderkorbBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 6.0F).sound(SoundType.COPPER).requiresCorrectToolForDrops());
	/** The invisible light of a Kumpel's helmet lamp; it has no item. */
	public static final Block LAMP_LIGHT = registerBlockOnly("lamp_light", LampLightBlock::new,
			BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().noOcclusion().strength(-1.0F, 0.0F)
					.lightLevel(state -> state.getValue(LampLightBlock.LEVEL)).pushReaction(PushReaction.DESTROY));
	public static final Block MARKENTAFEL = register("markentafel", MarkentafelBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD));

	private ModBlocks() {
	}

	private static Block registerBlockOnly(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Kumpel.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
	}

	/** Registers a block together with its item. */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Kumpel.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Kumpel.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static void initialize() {
		// Loading the class registers the blocks.
	}
}
