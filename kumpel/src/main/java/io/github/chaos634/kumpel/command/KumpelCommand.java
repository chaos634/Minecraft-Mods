package io.github.chaos634.kumpel.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;

/**
 * {@code /kumpel reload} re-reads the config; {@code /kumpel levels} and {@code /kumpel ores} show what is configured.
 */
public final class KumpelCommand {
	private KumpelCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("kumpel")
				.then(Commands.literal("reload")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> {
							KumpelSettings settings = KumpelSettings.reload();
							context.getSource().sendSuccess(() -> Component.translatable("command.kumpel.reloaded",
									settings.tiers().size(), settings.ores().size(), settings.feeding().size(),
									KumpelConfig.path().getFileName().toString()), true);
							return 1;
						}))
				.then(Commands.literal("levels")
						.executes(context -> {
							for (KumpelTier tier : KumpelSettings.get().tiers()) {
								context.getSource().sendSuccess(() -> Component.translatable("command.kumpel.level",
										tier.level(), tier.displayName(), tier.requiredExperience(), (int) tier.maxHealth(),
										tier.senseRadius(), tier.pocketSlots()).withStyle(ChatFormatting.GRAY), false);
							}
							return KumpelSettings.get().tiers().size();
						}))
				.then(Commands.literal("ores")
						.executes(context -> {
							KumpelSettings settings = KumpelSettings.get();
							for (OreRule rule : settings.ores()) {
								sendOre(context.getSource(), rule);
							}
							if (settings.unlistedOre() != null) {
								sendOre(context.getSource(), settings.unlistedOre());
							}
							return settings.ores().size();
						})));
	}

	private static void sendOre(CommandSourceStack source, OreRule rule) {
		source.sendSuccess(() -> Component.translatable("command.kumpel.ore", rule.describe(), rule.level(), rule.value())
				.withStyle(ChatFormatting.GRAY), false);
	}
}
