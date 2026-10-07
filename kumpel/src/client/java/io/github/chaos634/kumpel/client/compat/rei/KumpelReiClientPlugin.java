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
import io.github.chaos634.kumpel.registry.ModBlocks;
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
				.line(Component.translatable("guide.kumpel.core"))
				.line(Component.translatable("guide.kumpel.hauer"))
				.line(Component.translatable("guide.kumpel.ausfahrt")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.KUMPEL_SPAWN_EGG), Component.translatable("item.kumpel.kumpel_spawn_egg"))
				.line(Component.translatable("guide.kumpel.spawn_egg")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.STEIGER_WHISTLE), Component.translatable("item.kumpel.steiger_whistle"))
				.line(Component.translatable("guide.kumpel.steiger_whistle")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.STEIGERHAECKEL), Component.translatable("item.kumpel.steigerhaeckel"))
				.line(Component.translatable("guide.kumpel.steigerhaeckel")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.CANARY_CAGE), Component.translatable("item.kumpel.canary_cage"))
				.line(Component.translatable("guide.kumpel.canary_cage")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.FIELD_FORGE), Component.translatable("item.kumpel.field_forge"))
				.line(Component.translatable("guide.kumpel.field_forge")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModBlocks.FOERDERKORB), Component.translatable("block.kumpel.foerderkorb"))
				.line(Component.translatable("guide.kumpel.foerderkorb")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.MINER_HELMET), Component.translatable("item.kumpel.miner_helmet"))
				.line(Component.translatable("guide.kumpel.miner_helmet")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.RESCUE_CAPSULE), Component.translatable("item.kumpel.rescue_capsule"))
				.line(Component.translatable("guide.kumpel.rescue_capsule")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.COKE), Component.translatable("item.kumpel.coke"))
				.line(Component.translatable("guide.kumpel.coke")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModBlocks.MARKENTAFEL), Component.translatable("block.kumpel.markentafel"))
				.line(Component.translatable("guide.kumpel.markentafel")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.MUSIC_DISC_GLUECK_AUF), Component.translatable("jukebox_song.kumpel.glueck_auf"))
				.line(Component.translatable("guide.kumpel.music_disc_glueck_auf")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.KUMPELFIBEL), Component.translatable("item.kumpel.kumpelfibel"))
				.line(Component.translatable("guide.kumpel.kumpelfibel")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.KNIFTE), Component.translatable("item.kumpel.knifte"))
				.line(Component.translatable("guide.kumpel.knifte")));
		registry.add(DefaultInformationDisplay.createFromEntry(EntryStacks.of(ModItems.MUCKEFUCK), Component.translatable("item.kumpel.muckefuck"))
				.line(Component.translatable("guide.kumpel.muckefuck")));
	}
}
