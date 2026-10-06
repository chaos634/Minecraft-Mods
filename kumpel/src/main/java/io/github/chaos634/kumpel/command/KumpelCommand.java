package io.github.chaos634.kumpel.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;
import io.github.chaos634.kumpel.registry.ModEntities;

/**
 * {@code /kumpel reload} re-reads the config; {@code /kumpel levels} and {@code /kumpel ores} show what is configured;
 * {@code /kumpel list} shows your Kumpels.
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
						}))
				.then(Commands.literal("list")
						.executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> list(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
	}

	private static int list(CommandSourceStack source, ServerPlayer owner) throws CommandSyntaxException {
		List<KumpelEntity> kumpels = findKumpels(owner);
		if (kumpels.isEmpty()) {
			source.sendFailure(Component.translatable("command.kumpel.list.none", owner.getDisplayName()));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("command.kumpel.list.header", owner.getDisplayName(), kumpels.size())
				.withStyle(ChatFormatting.GOLD), false);
		for (KumpelEntity kumpel : kumpels) {
			KumpelTier tier = kumpel.getTier();
			source.sendSuccess(() -> Component.translatable("command.kumpel.list.entry",
					kumpel.getDisplayName(),
					tier.level(),
					tier.displayName(),
					(int) Math.ceil(kumpel.getHealth()),
					(int) kumpel.getMaxHealth(),
					kumpel.level().dimension().identifier().toString(),
					kumpel.getBlockX(), kumpel.getBlockY(), kumpel.getBlockZ(),
					Component.translatable("command.kumpel.list.activity." + kumpel.activity())).withStyle(ChatFormatting.GRAY), false);
		}

		return kumpels.size();
	}

	/** All of the player's Kumpels that are loaded right now, in every dimension. */
	public static List<KumpelEntity> findKumpels(ServerPlayer owner) {
		List<KumpelEntity> kumpels = new ArrayList<>();
		for (ServerLevel level : owner.level().getServer().getAllLevels()) {
			kumpels.addAll(level.getEntities(ModEntities.KUMPEL, kumpel -> kumpel.isAlive() && kumpel.isOwnedBy(owner)));
		}

		return kumpels;
	}

	private static void sendOre(CommandSourceStack source, OreRule rule) {
		source.sendSuccess(() -> Component.translatable("command.kumpel.ore", rule.describe(), rule.level(), rule.value())
				.withStyle(ChatFormatting.GRAY), false);
	}
}
