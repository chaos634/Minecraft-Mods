package io.github.chaos634.kumpel.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.item.KumpelCoreItem;

public final class ModItems {
	public static final Item KUMPEL_CORE = register("kumpel_core", KumpelCoreItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kumpel.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(KUMPEL_CORE));
	}
}
