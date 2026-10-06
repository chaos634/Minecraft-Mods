package io.github.chaos634.zechenbau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import io.github.chaos634.zechenbau.registry.ZechenbauBlocks;

/**
 * Zechenbau: building blocks of the Ruhr's collieries. Red brick, steel framework, girders, headframe lattice
 * and miner's lamps, to build your own Zeche.
 */
public class Zechenbau implements ModInitializer {
	public static final String MOD_ID = "zechenbau";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ZechenbauBlocks.initialize();
		LOGGER.info("Glück auf! Zechenbau is ready.");
	}
}
