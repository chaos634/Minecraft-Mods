package io.github.chaos634.kumpel.client.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;

import io.github.chaos634.kumpel.compat.KumpelGuideData;

/** "Ore sensing": which ores a Kumpel senses, and from which level on. */
public class OreSensingCategory extends AbstractRecipeCategory<KumpelGuideData.OreEntry> {
	private static final int TEXT_COLOR = 0xFF404040;

	public OreSensingCategory(IGuiHelper guiHelper) {
		super(KumpelJeiPlugin.ORE_SENSING, Component.translatable("guide.kumpel.ore_sensing"),
				guiHelper.createDrawableItemLike(Items.DIAMOND_ORE), 150, 30);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, KumpelGuideData.OreEntry recipe, IFocusGroup focuses) {
		builder.addInputSlot(4, 7)
				.setStandardSlotBackground()
				.addItemStacks(recipe.ores());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, KumpelGuideData.OreEntry recipe, IFocusGroup focuses) {
		builder.addText(Component.translatable("guide.kumpel.ore_sensing.level", recipe.level(), recipe.tierName()), 120, 12)
				.setPosition(28, 4)
				.setColor(TEXT_COLOR);
		builder.addText(Component.translatable("guide.kumpel.ore_sensing.value", recipe.value()), 120, 12)
				.setPosition(28, 16)
				.setColor(TEXT_COLOR);
	}
}
