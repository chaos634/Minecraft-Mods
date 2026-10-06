package io.github.chaos634.kumpel.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.Markenkontrolle;
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
		List<Component> lines = report(owner);
		if (lines.isEmpty()) {
			source.sendFailure(Component.translatable("command.kumpel.list.none", owner.getDisplayName()));
			return 0;
		}

		lines.forEach(line -> source.sendSuccess(() -> line, false));
		return lines.size() - 1;
	}

	/** Markentafel: sends the report about the owner's Kumpels to the viewer. */
	public static void report(ServerPlayer viewer, ServerPlayer owner) {
		List<Component> lines = report(owner);
		if (lines.isEmpty()) {
			viewer.sendSystemMessage(Component.translatable("command.kumpel.list.none", owner.getDisplayName()));
		} else {
			lines.forEach(viewer::sendSystemMessage);
		}
	}

	/**
	 * Markenkontrolle: a header and one line per Kumpel. Loaded Kumpels are shown as they are right now,
	 * the others as their tag last saw them. Empty if the player has no Kumpels at all.
	 */
	public static List<Component> report(ServerPlayer owner) {
		MinecraftServer server = owner.level().getServer();
		long now = server.overworld().getGameTime();
		List<KumpelEntity> kumpels = findKumpels(owner);
		Set<UUID> loaded = new HashSet<>();
		kumpels.forEach(kumpel -> loaded.add(kumpel.getUUID()));
		List<Markenkontrolle.Marke> away = Markenkontrolle.get(server).ofOwner(owner.getUUID(), now).stream()
				.filter(marke -> !loaded.contains(marke.kumpel()))
				.toList();
		if (kumpels.isEmpty() && away.isEmpty()) {
			return List.of();
		}

		int underground = 0;
		List<Component> entries = new ArrayList<>();
		for (KumpelEntity kumpel : kumpels) {
			KumpelTier tier = kumpel.getTier();
			if (!kumpel.level().canSeeSky(kumpel.blockPosition())) {
				underground++;
			}
			entries.add(Component.translatable("command.kumpel.list.entry",
					kumpel.getDisplayName(),
					tier.level(),
					tier.displayName(),
					(int) Math.ceil(kumpel.getHealth()),
					(int) kumpel.getMaxHealth(),
					kumpel.level().dimension().identifier().toString(),
					kumpel.getBlockX(), kumpel.getBlockY(), kumpel.getBlockZ(),
					Component.translatable("command.kumpel.list.activity." + kumpel.activity())).withStyle(ChatFormatting.GRAY));
		}
		for (Markenkontrolle.Marke marke : away) {
			long minutes = Math.max(1, (now - marke.seenAt()) / 1200);
			if (marke.isLost()) {
				entries.add(Component.translatable("command.kumpel.list.lost", marke.name(),
						marke.pos().dimension().identifier().toString(), marke.pos().pos().getX(), marke.pos().pos().getY(), marke.pos().pos().getZ(),
						minutes).withStyle(ChatFormatting.RED));
				continue;
			}
			if (marke.underground()) {
				underground++;
			}
			entries.add(Component.translatable("command.kumpel.list.away", marke.name(), marke.level(),
					marke.pos().dimension().identifier().toString(), marke.pos().pos().getX(), marke.pos().pos().getY(), marke.pos().pos().getZ(),
					minutes, Component.translatable("command.kumpel.list.activity." + marke.activity())).withStyle(ChatFormatting.DARK_GRAY));
		}

		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("command.kumpel.list.header", owner.getDisplayName(), entries.size())
				.append(" · ")
				.append(Component.translatable("command.kumpel.list.underground", underground))
				.withStyle(ChatFormatting.GOLD));
		lines.addAll(entries);
		return lines;
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
