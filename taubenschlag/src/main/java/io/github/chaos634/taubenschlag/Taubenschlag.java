package io.github.chaos634.taubenschlag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import io.github.chaos634.taubenschlag.flight.Flugplan;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagComponents;
import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;
import io.github.chaos634.taubenschlag.registry.TaubenschlagItems;
import io.github.chaos634.taubenschlag.registry.TaubenschlagSounds;

/**
 * Taubenschlag: homing pigeons, the "racehorses of the little man" that miners in the Ruhr kept in lofts on their
 * allotments. Tame one, let it settle into a loft, carry it away in a travel basket and let it fly home, with a bit of
 * post if you like.
 */
public class Taubenschlag implements ModInitializer {
	public static final String MOD_ID = "taubenschlag";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		TaubenschlagSounds.initialize();
		TaubenschlagComponents.initialize();
		TaubenschlagBlocks.initialize();
		TaubenschlagEntities.initialize();
		TaubenschlagItems.initialize();
		ServerTickEvents.END_SERVER_TICK.register(server -> Flugplan.get(server).tick(server));
		LOGGER.info("Glück auf! The pigeons of Taubenschlag are home.");
	}
}
