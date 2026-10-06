package io.github.chaos634.kumpel.client.compat.jei;

import net.minecraft.network.chat.Component;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;

import io.github.chaos634.kumpel.compat.KumpelGuideData;
import io.github.chaos634.kumpel.registry.ModItems;

/** "Feed your Kumpel": an item and how much experience it gives. */
public class FeedingCategory extends AbstractRecipeCategory<KumpelGuideData.FeedingEntry> {
	private static final int TEXT_COLOR = 0xFF404040;

	public FeedingCategory(IGuiHelper guiHelper) {
		super(KumpelJeiPlugin.FEEDING, Component.translatable("guide.kumpel.feeding"),
				guiHelper.createDrawableItemLike(ModItems.KUMPEL_CORE), 130, 26);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, KumpelGuideData.FeedingEntry recipe, IFocusGroup focuses) {
		builder.addInputSlot(4, 5)
				.setStandardSlotBackground()
				.addItemStacks(recipe.items());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, KumpelGuideData.FeedingEntry recipe, IFocusGroup focuses) {
		builder.addText(Component.translatable("guide.kumpel.feeding.experience", recipe.experience()), 100, 12)
				.setPosition(28, 9)
				.setColor(TEXT_COLOR);
	}
}
