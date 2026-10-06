package io.github.chaos634.kumpel.client.compat.rei;

import net.minecraft.network.chat.Component;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;

import io.github.chaos634.kumpel.compat.KumpelGuideData;
import io.github.chaos634.kumpel.compat.rei.FeedingDisplay;
import io.github.chaos634.kumpel.compat.rei.OreSensingDisplay;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * REI integration: what a Kumpel eats, which ores it senses at which level, and info pages for the items.
 * Only loaded when REI is installed (via the {@code rei_client} entrypoint).
 */
public class KumpelReiClientPlugin implements REIClientPlugin {
	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.add(new FeedingDisplayCategory(), new OreSensingDisplayCategory());
		registry.addWorkstations(FeedingDisplay.CATEGORY, EntryIngredients.of(ModItems.KUMPEL_CORE));
		registry.addWorkstations(OreSensingDisplay.CATEGORY, EntryIngredients.of(ModItems.KUMPEL_CORE));
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		for (KumpelGuideData.FeedingEntry entry : KumpelGuideData.feeding()) {
			registry.add(new FeedingDisplay(EntryIngredients.ofItemStacks(entry.items()), entry.experience()));
		}

		for (KumpelGuideData.OreEntry entry : KumpelGuideData.ores()) {
			registry.add(new OreSensingDisplay(EntryIngredients.ofItemStacks(entry.ores()), entry.level(), entry.value()));
		}

		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.KUMPEL_CORE), Component.translatable("item.kumpel.kumpel_core"))
				.line(Component.translatable("guide.kumpel.core")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.KUMPEL_SPAWN_EGG), Component.translatable("item.kumpel.kumpel_spawn_egg"))
				.line(Component.translatable("guide.kumpel.spawn_egg")));
	}
}
