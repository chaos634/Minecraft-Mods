package io.github.chaos634.kumpel.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import io.github.chaos634.kumpel.registry.ModComponents;

/**
 * What remains when a Kumpel falls: its core, cracked but still holding its soul.
 * Combined with a block of copper in a crafting table it becomes a working Kumpel Core again.
 */
public class CrackedKumpelCoreItem extends Item {
	public CrackedKumpelCoreItem(Properties properties) {
		super(properties);
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		KumpelSoul soul = stack.get(ModComponents.SOUL);
		if (soul != null) {
			KumpelCoreItem.appendSoulTooltip(soul, tooltip);
		}
		tooltip.accept(Component.translatable("item.kumpel.cracked_kumpel_core.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
