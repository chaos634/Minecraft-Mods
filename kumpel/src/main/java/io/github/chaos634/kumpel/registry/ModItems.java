package io.github.chaos634.kumpel.registry;

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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.item.CrackedKumpelCoreItem;
import io.github.chaos634.kumpel.item.KumpelCoreItem;
import io.github.chaos634.kumpel.item.KumpelSoul;

public final class ModItems {
	public static final Item KUMPEL_CORE = register("kumpel_core", KumpelCoreItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item CRACKED_KUMPEL_CORE = register("cracked_kumpel_core", CrackedKumpelCoreItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
	public static final Item KUMPEL_SPAWN_EGG = register("kumpel_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.KUMPEL));

	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kumpel.id("kumpel"));
	public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.kumpel"))
			.icon(() -> new ItemStack(KUMPEL_CORE))
			.displayItems((parameters, output) -> {
				output.accept(KUMPEL_CORE);
				// A ready-made core for every level, handy for testing and for map makers.
				for (KumpelTier tier : KumpelSettings.get().tiers()) {
					if (tier.level() > 1) {
						ItemStack core = new ItemStack(KUMPEL_CORE);
						core.set(ModComponents.SOUL, new KumpelSoul(tier.requiredExperience(), true));
						output.accept(core);
					}
				}
				output.accept(CRACKED_KUMPEL_CORE);
				output.accept(KUMPEL_SPAWN_EGG);
			})
			.build();

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kumpel.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);

		// Also show up in the vanilla tabs where players would look for them.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(KUMPEL_CORE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(KUMPEL_SPAWN_EGG));
	}
}
