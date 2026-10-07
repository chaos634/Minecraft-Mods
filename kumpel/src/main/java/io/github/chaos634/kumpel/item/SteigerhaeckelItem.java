package io.github.chaos634.kumpel.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Steigerhäckel: the ceremonial hatchet a Steiger carries at the Bergparade. Hold it, and your Kumpels line up and
 * march behind you in single file, the most experienced first.
 */
public class SteigerhaeckelItem extends Item {
	public SteigerhaeckelItem(Properties properties) {
		super(properties);
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.steigerhaeckel.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
