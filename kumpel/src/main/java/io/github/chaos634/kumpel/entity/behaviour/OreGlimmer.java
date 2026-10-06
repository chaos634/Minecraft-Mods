package io.github.chaos634.kumpel.entity.behaviour;

import java.util.ArrayList;
import java.util.List;

import com.mojang.math.Transformation;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Wünschelrute: makes a sensed ore glow through walls for a moment. The glow is a glowing block display
 * that sits exactly on top of the ore and is removed again after a few seconds.
 */
public final class OreGlimmer {
	/** Marks our displays, so ones that were saved with a chunk are cleaned up when it loads again. */
	public static final String TAG = "kumpel_ore_glimmer";
	public static final int ORE_COLOR = 0x7FFFD4;
	public static final int DANGER_COLOR = 0xFF4040;

	private static final List<Glimmer> ACTIVE = new ArrayList<>();

	private record Glimmer(Display.BlockDisplay display, long expiresAt) {
	}

	private OreGlimmer() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(OreGlimmer::tick);
		ServerEntityEvents.ENTITY_LOAD.register(OreGlimmer::onEntityLoad);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			ACTIVE.forEach(glimmer -> glimmer.display().discard());
			ACTIVE.clear();
		});
	}

	public static void spawn(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
		spawn(level, pos, state, ticks, ORE_COLOR);
	}

	public static void spawn(ServerLevel level, BlockPos pos, BlockState state, int ticks, int color) {
		Display.BlockDisplay display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
		display.setBlockState(state);
		display.setPos(pos.getX(), pos.getY(), pos.getZ());
		// A hair bigger than the ore itself, so the two don't flicker against each other.
		display.setTransformation(new Transformation(new Vector3f(-0.01F), null, new Vector3f(1.02F), null));
		display.setBrightnessOverride(new Brightness(15, 15));
		display.setGlowColorOverride(color);
		display.setGlowingTag(true);
		display.addTag(TAG);

		ACTIVE.add(new Glimmer(display, level.getGameTime() + Math.max(1, ticks)));
		level.addFreshEntity(display);
	}

	public static boolean isGlowing(ServerLevel level, BlockPos pos) {
		for (Glimmer glimmer : ACTIVE) {
			if (glimmer.display().level() == level && glimmer.display().blockPosition().equals(pos) && !glimmer.display().isRemoved()) {
				return true;
			}
		}

		return false;
	}

	private static void tick(ServerLevel level) {
		if (ACTIVE.isEmpty()) {
			return;
		}

		long now = level.getGameTime();
		ACTIVE.removeIf(glimmer -> {
			Display.BlockDisplay display = glimmer.display();
			if (display.isRemoved()) {
				return true;
			}
			if (display.level() == level && now >= glimmer.expiresAt()) {
				display.discard();
				return true;
			}
			return false;
		});
	}

	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof Display.BlockDisplay display) || !display.entityTags().contains(TAG)) {
			return;
		}

		for (Glimmer glimmer : ACTIVE) {
			if (glimmer.display() == display) {
				return;
			}
		}

		// Left over from before a restart or a chunk unload.
		display.discard();
	}
}
