package io.github.chaos634.taubenschlag.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.block.TaubenschlagBlock;
import io.github.chaos634.taubenschlag.block.TaubenschlagBlockEntity;

public final class TaubenschlagBlocks {
	private static final ResourceKey<Block> TAUBENSCHLAG_KEY = ResourceKey.create(Registries.BLOCK, Taubenschlag.id("taubenschlag"));
	public static final Block TAUBENSCHLAG = Registry.register(BuiltInRegistries.BLOCK, TAUBENSCHLAG_KEY,
			new TaubenschlagBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion()
					.ignitedByLava().setId(TAUBENSCHLAG_KEY)));
	public static final Item TAUBENSCHLAG_ITEM = registerItem(TAUBENSCHLAG_KEY, TAUBENSCHLAG);

	public static final BlockEntityType<TaubenschlagBlockEntity> TAUBENSCHLAG_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Taubenschlag.id("taubenschlag"),
			FabricBlockEntityTypeBuilder.create(TaubenschlagBlockEntity::new, TAUBENSCHLAG).build());

	private TaubenschlagBlocks() {
	}

	private static Item registerItem(ResourceKey<Block> blockKey, Block block) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, blockKey.identifier());
		return Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}

	public static void initialize() {
		// Loading the class registers the blocks.
	}
}
