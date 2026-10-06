package io.github.chaos634.kumpel.client.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.compat.KumpelGuideData;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * JEI integration: what a Kumpel eats, which ores it senses at which level, and info pages for the items.
 * Only loaded when JEI is installed (via the {@code jei_mod_plugin} entrypoint).
 */
public class KumpelJeiPlugin implements IModPlugin {
	public static final IRecipeType<KumpelGuideData.FeedingEntry> FEEDING =
			IRecipeType.create(Kumpel.MOD_ID, "feeding", KumpelGuideData.FeedingEntry.class);
	public static final IRecipeType<KumpelGuideData.OreEntry> ORE_SENSING =
			IRecipeType.create(Kumpel.MOD_ID, "ore_sensing", KumpelGuideData.OreEntry.class);

	@Override
	public Identifier getPluginUid() {
		return Kumpel.id("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new FeedingCategory(guiHelper), new OreSensingCategory(guiHelper));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(FEEDING, KumpelGuideData.feeding());
		registration.addRecipes(ORE_SENSING, KumpelGuideData.ores());
		registration.addIngredientInfo(ModItems.KUMPEL_CORE, Component.translatable("guide.kumpel.core"), Component.translatable("guide.kumpel.hauer"));
		registration.addIngredientInfo(ModItems.STEIGER_WHISTLE, Component.translatable("guide.kumpel.steiger_whistle"));
		registration.addIngredientInfo(ModItems.CANARY_CAGE, Component.translatable("guide.kumpel.canary_cage"));
		registration.addIngredientInfo(ModItems.MINER_HELMET, Component.translatable("guide.kumpel.miner_helmet"));
		registration.addIngredientInfo(ModItems.KUMPEL_SPAWN_EGG, Component.translatable("guide.kumpel.spawn_egg"));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(FEEDING, ModItems.KUMPEL_CORE);
		registration.addCraftingStation(ORE_SENSING, ModItems.KUMPEL_CORE);
	}
}
