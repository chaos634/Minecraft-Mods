package io.github.chaos634.kumpel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chaos634.kumpel.command.KumpelCommand;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.behaviour.OreGlimmer;
import io.github.chaos634.kumpel.item.MinerHelmetItem;
import io.github.chaos634.kumpel.item.RescueCapsuleItem;
import io.github.chaos634.kumpel.registry.ModBlocks;
import io.github.chaos634.kumpel.registry.ModComponents;
import io.github.chaos634.kumpel.registry.ModEntities;
import io.github.chaos634.kumpel.registry.ModItems;
import io.github.chaos634.kumpel.registry.ModSounds;

public class Kumpel implements ModInitializer {
	public static final String MOD_ID = "kumpel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		KumpelSettings.reload();
		ModComponents.initialize();
		ModSounds.initialize();
		ModBlocks.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		OreGlimmer.initialize();
		MinerHelmetItem.initialize();
		RescueCapsuleItem.initialize();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> KumpelCommand.register(dispatcher));
		// Tags may have changed, so forget which blocks were ores.
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> KumpelSettings.clearCaches());

		LOGGER.info("Glück auf! Kumpel is ready.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
