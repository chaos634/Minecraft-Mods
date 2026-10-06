package io.github.chaos634.kumpel.compat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;

/**
 * What JEI and REI show about the Kumpel: which items it eats and which ores it senses.
 * Built from the active config and the current tags, so modpack changes show up automatically.
 */
public final class KumpelGuideData {
	private KumpelGuideData() {
	}

	public record FeedingEntry(List<ItemStack> items, int experience) {
	}

	public record OreEntry(List<ItemStack> ores, int level, int value) {
		public Component tierName() {
			KumpelSettings settings = KumpelSettings.get();
			KumpelTier tier = settings.tier(level);
			return tier.level() == level ? tier.displayName() : Component.literal("?");
		}
	}

	public static List<FeedingEntry> feeding() {
		List<FeedingEntry> entries = new ArrayList<>();
		for (KumpelSettings.FoodRule food : KumpelSettings.get().feeding()) {
			List<ItemStack> items = new ArrayList<>();
			if (food.tag() != null) {
				for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(food.tag())) {
					items.add(new ItemStack(holder.value()));
				}
			} else if (food.item() != null) {
				BuiltInRegistries.ITEM.getOptional(food.item()).ifPresent(item -> items.add(new ItemStack(item)));
			}

			if (!items.isEmpty()) {
				entries.add(new FeedingEntry(items, food.experience()));
			}
		}

		return entries;
	}

	public static List<OreEntry> ores() {
		KumpelSettings settings = KumpelSettings.get();
		List<OreEntry> entries = new ArrayList<>();
		Set<Item> covered = new LinkedHashSet<>();

		for (OreRule rule : settings.ores()) {
			List<ItemStack> ores = oreItems(rule, covered);
			if (!ores.isEmpty()) {
				entries.add(new OreEntry(ores, rule.level(), rule.value()));
			}
		}

		OreRule unlisted = settings.unlistedOre();
		if (unlisted != null) {
			List<ItemStack> ores = oreItems(unlisted, covered);
			if (!ores.isEmpty()) {
				entries.add(new OreEntry(ores, unlisted.level(), unlisted.value()));
			}
		}

		return entries;
	}

	private static List<ItemStack> oreItems(OreRule rule, Set<Item> covered) {
		List<ItemStack> items = new ArrayList<>();
		if (rule.tag() != null) {
			for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(rule.tag())) {
				addBlockItem(holder.value(), items, covered);
			}
		} else if (rule.block() != null) {
			BuiltInRegistries.BLOCK.getOptional(rule.block()).ifPresent(block -> addBlockItem(block, items, covered));
		}

		return items;
	}

	private static void addBlockItem(Block block, List<ItemStack> items, Set<Item> covered) {
		Item item = block.asItem();
		if (item != Items.AIR && covered.add(item)) {
			items.add(new ItemStack(item));
		}
	}
}
