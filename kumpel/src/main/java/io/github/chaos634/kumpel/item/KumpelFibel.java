package io.github.chaos634.kumpel.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * Kumpelfibel: a little handbook about the Kumpel. Its pages are translation keys
 * ({@code fibel.kumpel.page.<n>}), so everyone reads it in their own language.
 */
public class KumpelFibel extends Item {
	public static final int PAGES = 19;

	public KumpelFibel(Properties properties) {
		super(properties);
	}

	public static WrittenBookContent content() {
		List<Filterable<Component>> pages = new ArrayList<>();
		for (int page = 0; page < PAGES; page++) {
			pages.add(Filterable.passThrough(Component.translatable("fibel.kumpel.page." + page)));
		}

		return new WrittenBookContent(Filterable.passThrough("Kumpelfibel"), "Kumpel", 0, pages, true);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openItemGui(player.getItemInHand(hand), hand);
		}

		return InteractionResult.SUCCESS;
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.kumpelfibel.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
