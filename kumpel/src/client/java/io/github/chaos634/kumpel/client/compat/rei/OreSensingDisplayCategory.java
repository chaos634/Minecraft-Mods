package io.github.chaos634.kumpel.client.compat.rei;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;

import io.github.chaos634.kumpel.compat.KumpelGuideData;
import io.github.chaos634.kumpel.compat.rei.OreSensingDisplay;

public class OreSensingDisplayCategory implements DisplayCategory<OreSensingDisplay> {
	private static final int TEXT_COLOR = 0xFF404040;

	@Override
	public CategoryIdentifier<? extends OreSensingDisplay> getCategoryIdentifier() {
		return OreSensingDisplay.CATEGORY;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("guide.kumpel.ore_sensing");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Items.DIAMOND_ORE);
	}

	@Override
	public int getDisplayHeight() {
		return 34;
	}

	@Override
	public List<Widget> setupDisplay(OreSensingDisplay display, Rectangle bounds) {
		Component tier = new KumpelGuideData.OreEntry(List.of(), display.level(), display.value()).tierName();
		return List.of(
				Widgets.createRecipeBase(bounds),
				Widgets.createSlot(new Point(bounds.x + 7, bounds.y + 9)).entries(display.ores()).markInput(),
				Widgets.createLabel(new Point(bounds.x + 30, bounds.y + 7),
						Component.translatable("guide.kumpel.ore_sensing.level", display.level(), tier)).leftAligned().noShadow().color(TEXT_COLOR),
				Widgets.createLabel(new Point(bounds.x + 30, bounds.y + 19),
						Component.translatable("guide.kumpel.ore_sensing.value", display.value())).leftAligned().noShadow().color(TEXT_COLOR)
		);
	}
}
