package io.github.chaos634.kumpel.bau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;

import io.github.chaos634.kumpel.Kumpel;

/**
 * Bauplan: a building a Kumpel can build, loaded from {@code data/<namespace>/kumpel/bauplan/<name>.json}, so other mods
 * and data packs can add their own.
 *
 * <p>The palette maps single characters to block states ({@code "minecraft:oak_stairs[facing=south]"}). The layers go from
 * the bottom up; each layer is a list of rows, the first row nearest to whoever orders the building, and each row is read
 * from their left to their right. Characters not in the palette (like spaces) are left alone. States are written as seen
 * by someone looking south, so "north" faces them.
 */
public record Bauplan(Map<String, String> palette, List<List<String>> layers) {
	public static final ResourceKey<Registry<Bauplan>> REGISTRY = ResourceKey.createRegistryKey(Kumpel.id("bauplan"));
	public static final Codec<Bauplan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("palette").forGetter(Bauplan::palette),
			Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(Bauplan::layers)
	).apply(instance, Bauplan::new));

	/** One block of the building, in plan coordinates: x to the right, y up, z away from whoever ordered it. */
	public record Piece(int x, int y, int z, BlockState state) {
	}

	public int width() {
		int width = 0;
		for (List<String> layer : layers) {
			for (String row : layer) {
				width = Math.max(width, row.length());
			}
		}
		return width;
	}

	/** The blocks to place, bottom layer first, each layer row by row from the front. Blocks that don't exist are left out. */
	public List<Piece> pieces() {
		Map<Character, BlockState> states = new HashMap<>();
		palette.forEach((key, value) -> {
			if (key.length() != 1) {
				return;
			}
			try {
				states.put(key.charAt(0), BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, value, false).blockState());
			} catch (CommandSyntaxException e) {
				Kumpel.LOGGER.warn("Bauplan block '{}' is unknown: {}", value, e.getMessage());
			}
		});

		List<Piece> pieces = new ArrayList<>();
		for (int y = 0; y < layers.size(); y++) {
			List<String> layer = layers.get(y);
			for (int z = 0; z < layer.size(); z++) {
				String row = layer.get(z);
				for (int x = 0; x < row.length(); x++) {
					BlockState state = states.get(row.charAt(x));
					if (state != null) {
						pieces.add(new Piece(x, y, z, state));
					}
				}
			}
		}
		return pieces;
	}

	/** The translation key of a plan's name: {@code bauplan.<namespace>.<path>}. */
	public static String translationKey(ResourceKey<Bauplan> key) {
		return "bauplan." + key.identifier().getNamespace() + "." + key.identifier().getPath().replace('/', '.');
	}
}
