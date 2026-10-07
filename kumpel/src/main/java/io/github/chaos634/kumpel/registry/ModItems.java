package io.github.chaos634.kumpel.registry;

import java.util.Comparator;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.build.Bauplan;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.item.BauplanItem;
import io.github.chaos634.kumpel.item.CrackedKumpelCoreItem;
import io.github.chaos634.kumpel.item.KumpelCoreItem;
import io.github.chaos634.kumpel.item.KumpelFibel;
import io.github.chaos634.kumpel.item.KumpelSoul;
import io.github.chaos634.kumpel.item.MinerHelmetItem;
import io.github.chaos634.kumpel.item.RescueCapsuleItem;
import io.github.chaos634.kumpel.item.SteigerWhistleItem;
import io.github.chaos634.kumpel.item.SteigerhaeckelItem;

public final class ModItems {
	public static final Item KUMPEL_CORE = register("kumpel_core", KumpelCoreItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item CRACKED_KUMPEL_CORE = register("cracked_kumpel_core", CrackedKumpelCoreItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
	public static final Item STEIGER_WHISTLE = register("steiger_whistle", SteigerWhistleItem::new,
			new Item.Properties().stacksTo(1));
	public static final Item CANARY_CAGE = register("canary_cage", Item::new,
			new Item.Properties().stacksTo(1));
	public static final Item FIELD_FORGE = register("field_forge", Item::new,
			new Item.Properties().stacksTo(1));
	public static final Item RESCUE_CAPSULE = register("rescue_capsule", RescueCapsuleItem::new,
			new Item.Properties().stacksTo(4));
	/** Kokerei: coke burns half as long again as coal (data/kumpel/context_int_provider/cooking/time_coke.json). */
	public static final ResourceKey<ContextIntProvider> COKE_BURN_TIME = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, Kumpel.id("cooking/time_coke"));
	public static final Item COKE = register("coke", Item::new, new Item.Properties().cookingFuel(COKE_BURN_TIME));
	/** The Kumpelkapelle's record, "Glück auf" (data/kumpel/jukebox_song/glueck_auf.json). */
	public static final ResourceKey<JukeboxSong> GLUECK_AUF_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, Kumpel.id("glueck_auf"));
	public static final Item MUSIC_DISC_GLUECK_AUF = register("music_disc_glueck_auf", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(GLUECK_AUF_SONG));
	public static final Item KUMPELFIBEL = register("kumpelfibel", KumpelFibel::new,
			new Item.Properties().stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT, KumpelFibel.content()));
	/** Knifte: the miner's sandwich from the Henkelmann. */
	public static final Item KNIFTE = register("knifte", Item::new,
			new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build()));
	/** Muckefuck: grain coffee for the shift. Gives Haste and shakes off Mining Fatigue. */
	public static final Item MUCKEFUCK = register("muckefuck", Item::new, new Item.Properties().stacksTo(16)
			.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).alwaysEdible().build(), Consumables.defaultDrink()
					.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 90 * 20, 0)))
					.onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.MINING_FATIGUE))
					.build())
			.usingConvertsTo(Items.GLASS_BOTTLE));
	/** The Steiger's ceremonial hatchet: hold it and your Kumpels march behind you. */
	public static final Item STEIGERHAECKEL = register("steigerhaeckel", SteigerhaeckelItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	/** Bauplan: choose a building and your Kumpel builds it. */
	public static final Item BAUPLAN = register("bauplan", BauplanItem::new, new Item.Properties().stacksTo(1));
	public static final Item MINER_HELMET = register("miner_helmet", MinerHelmetItem::new,
			new Item.Properties().humanoidArmor(MinerHelmetItem.MATERIAL, ArmorType.HELMET));
	public static final Item KUMPEL_SPAWN_EGG = register("kumpel_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.KUMPEL));

	/** One in eight mineshaft chests holds the record. */
	private static final int MINESHAFT_EMPTY_WEIGHT = 7;

	public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kumpel.id("kumpel"));
	public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.kumpel"))
			.icon(() -> new ItemStack(KUMPEL_CORE))
			.displayItems((parameters, output) -> {
				output.accept(KUMPEL_CORE);
				output.accept(KUMPELFIBEL);
				// A ready-made core for every level, handy for testing and for map makers.
				for (KumpelTier tier : KumpelSettings.get().tiers()) {
					if (tier.level() > 1) {
						ItemStack core = new ItemStack(KUMPEL_CORE);
						core.set(ModComponents.SOUL, new KumpelSoul(tier.requiredExperience(), true));
						output.accept(core);
					}
				}
				output.accept(CRACKED_KUMPEL_CORE);
				output.accept(STEIGER_WHISTLE);
				output.accept(STEIGERHAECKEL);
				output.accept(BAUPLAN);
				// One Bauplan for every building the data packs know.
				parameters.holders().lookup(Bauplan.REGISTRY).ifPresent(plans -> plans.listElementIds()
						.sorted(Comparator.comparing(key -> key.identifier().toString()))
						.forEach(key -> {
							ItemStack plan = new ItemStack(BAUPLAN);
							plan.set(ModComponents.BAUPLAN, key);
							output.accept(plan);
						}));
				output.accept(CANARY_CAGE);
				output.accept(FIELD_FORGE);
				output.accept(ModBlocks.FOERDERKORB);
				output.accept(RESCUE_CAPSULE);
				output.accept(COKE);
				output.accept(KNIFTE);
				output.accept(MUCKEFUCK);
				output.accept(ModBlocks.MARKENTAFEL);
				output.accept(MUSIC_DISC_GLUECK_AUF);
				output.accept(MINER_HELMET);
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
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(KUMPEL_CORE);
			output.accept(STEIGER_WHISTLE);
			output.accept(STEIGERHAECKEL);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> output.accept(MINER_HELMET));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(KUMPEL_SPAWN_EGG));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(COKE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(KNIFTE);
			output.accept(MUCKEFUCK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(MUSIC_DISC_GLUECK_AUF));

		// The record lies in abandoned mineshafts, now and then.
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (source.isBuiltin() && BuiltInLootTables.ABANDONED_MINESHAFT.equals(key)) {
				table.withPool(LootPool.lootPool()
						.add(LootItem.lootTableItem(MUSIC_DISC_GLUECK_AUF).setWeight(1))
						.add(EmptyLootItem.emptyItem().setWeight(MINESHAFT_EMPTY_WEIGHT)));
			}
		});
	}
}
