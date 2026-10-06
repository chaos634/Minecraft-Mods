package io.github.chaos634.kumpel.client.compat.rei;

import java.util.List;

import net.minecraft.network.chat.Component;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;

import io.github.chaos634.kumpel.compat.rei.FeedingDisplay;
import io.github.chaos634.kumpel.registry.ModItems;

public class FeedingDisplayCategory implements DisplayCategory<FeedingDisplay> {
	private static final int TEXT_COLOR = 0xFF404040;

	@Override
	public CategoryIdentifier<? extends FeedingDisplay> getCategoryIdentifier() {
		return FeedingDisplay.CATEGORY;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("guide.kumpel.feeding");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(ModItems.KUMPEL_CORE);
	}

	@Override
	public int getDisplayHeight() {
		return 30;
	}

	@Override
	public List<Widget> setupDisplay(FeedingDisplay display, Rectangle bounds) {
		return List.of(
				Widgets.createRecipeBase(bounds),
				Widgets.createSlot(new Point(bounds.x + 7, bounds.y + 7)).entries(display.items()).markInput(),
				Widgets.createLabel(new Point(bounds.x + 30, bounds.y + 11),
						Component.translatable("guide.kumpel.feeding.experience", display.experience())).leftAligned().noShadow().color(TEXT_COLOR)
		);
	}
}
