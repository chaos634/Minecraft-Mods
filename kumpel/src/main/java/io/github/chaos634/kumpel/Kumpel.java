package io.github.chaos634.kumpel;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chaos634.kumpel.registry.ModEntities;
import io.github.chaos634.kumpel.registry.ModItems;

public class Kumpel implements ModInitializer {
	public static final String MOD_ID = "kumpel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		LOGGER.info("Glück auf! Kumpel is ready.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
