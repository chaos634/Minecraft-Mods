package io.github.chaos634.pottkueche.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import io.github.chaos634.pottkueche.Pottkueche;

/** Every item of the mod, in the order they appear in the creative tab. */
public final class PottkuecheItems {
	private static final List<Item> ALL = new ArrayList<>();
	/** How long the Currywurst keeps you going (Speed). */
	private static final int CURRYWURST_SPEED_TICKS = 60 * 20;

	public static final Item ROHE_BRATWURST = register("rohe_bratwurst", Item::new, new Item.Properties().food(food(2, 0.3F)));
	public static final Item BRATWURST = register("bratwurst", Item::new, new Item.Properties().food(food(5, 0.6F)));
	/** Curry sauce: only an ingredient; the bottle stays behind when you cook with it. */
	public static final Item CURRYSOSSE = register("currysosse", Item::new, new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));
	public static final Item CURRYWURST = register("currywurst", Item::new, new Item.Properties()
			.food(food(8, 0.8F), Consumables.defaultFood()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, CURRYWURST_SPEED_TICKS, 0)))
					.build()));
	public static final Item POMMES = register("pommes", Item::new, new Item.Properties().food(food(6, 0.6F)));
	public static final Item CURRYWURST_POMMES = register("currywurst_pommes", Item::new, new Item.Properties()
			.food(food(13, 0.9F), Consumables.defaultFood()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 2 * CURRYWURST_SPEED_TICKS, 0)))
					.build()));
	public static final Item ROHE_FRIKADELLE = register("rohe_frikadelle", Item::new, new Item.Properties().food(food(2, 0.3F)));
	public static final Item FRIKADELLE = register("frikadelle", Item::new, new Item.Properties().food(food(6, 0.8F)));
	/** Pfefferpotthast: the Westphalian beef stew, in a bowl you get back. */
	public static final Item PFEFFERPOTTHAST = register("pfefferpotthast", Item::new, new Item.Properties().stacksTo(1)
			.food(food(10, 1.0F)).usingConvertsTo(Items.BOWL));
	/** Malzbier: dark, sweet and without alcohol; it heals a little. */
	public static final Item MALZBIER = register("malzbier", Item::new, new Item.Properties().stacksTo(16)
			.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).alwaysEdible().build(), Consumables.defaultDrink()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 5 * 20, 0)))
					.build())
			.usingConvertsTo(Items.GLASS_BOTTLE));

	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Pottkueche.id("pottkueche"));
	public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.pottkueche"))
			.icon(() -> new ItemStack(CURRYWURST_POMMES))
			.displayItems((parameters, output) -> ALL.forEach(output::accept))
			.build();

	private PottkuecheItems() {
	}

	private static FoodProperties food(int nutrition, float saturation) {
		return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Pottkueche.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ALL.add(item);
		return item;
	}

	public static List<Item> all() {
		return List.copyOf(ALL);
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> ALL.forEach(output::accept));
	}
}
