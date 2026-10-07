package io.github.chaos634.pottkueche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import io.github.chaos634.pottkueche.registry.PottkuecheItems;

/**
 * Pottküche: food from the Ruhr. Currywurst and Pommes from the Bude, Frikadellen, Pfefferpotthast and Malzbier.
 */
public class Pottkueche implements ModInitializer {
	public static final String MOD_ID = "pottkueche";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PottkuecheItems.initialize();
		LOGGER.info("Glück auf! Pottküche is cooking.");
	}
}
