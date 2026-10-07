package io.github.chaos634.taubenschlag.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.item.ReisekorbItem;

public final class TaubenschlagItems {
	public static final Item REISEKORB = register("reisekorb", ReisekorbItem::new, new Item.Properties().stacksTo(1));
	public static final Item BRIEFTAUBE_SPAWN_EGG = register("brieftaube_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(TaubenschlagEntities.BRIEFTAUBE));

	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Taubenschlag.id("taubenschlag"));
	public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.taubenschlag"))
			.icon(() -> new ItemStack(TaubenschlagBlocks.TAUBENSCHLAG_ITEM))
			.displayItems((parameters, output) -> {
				output.accept(TaubenschlagBlocks.TAUBENSCHLAG_ITEM);
				output.accept(REISEKORB);
				output.accept(BRIEFTAUBE_SPAWN_EGG);
			})
			.build();

	private TaubenschlagItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Taubenschlag.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(TaubenschlagBlocks.TAUBENSCHLAG_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(REISEKORB));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(BRIEFTAUBE_SPAWN_EGG));
	}
}
