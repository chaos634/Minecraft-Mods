package io.github.chaos634.kumpel.advancement;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import io.github.chaos634.kumpel.Kumpel;

/**
 * The Kumpel advancements. Most are triggered by vanilla criteria in their JSON files; the ones listed here
 * have a single {@code minecraft:impossible} criterion named {@value #CODE_CRITERION} and are awarded from code.
 */
public final class KumpelAdvancements {
	public static final String CODE_CRITERION = "code";

	public static final String MAX_LEVEL = "max_level";
	public static final String FULL_CREW = "full_crew";
	public static final String SHIFT_END = "shift_end";
	public static final String HAUER = "hauer";
	public static final String STORAGE = "storage";
	public static final String STEIGERLIED = "steigerlied";
	public static final String BARBARA = "barbara";

	/** Every advancement the mod ships, including the ones triggered from JSON. */
	public static final List<String> ALL = List.of(
			"root", "glueck_auf", "back_from_the_dead",
			MAX_LEVEL, FULL_CREW, SHIFT_END, HAUER, STORAGE, STEIGERLIED, BARBARA);

	private KumpelAdvancements() {
	}

	/** Awards one of the code-triggered advancements to a player (does nothing for anything else). */
	public static void award(Entity entity, String name) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}

		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Kumpel.id(name));
		if (advancement != null) {
			player.getAdvancements().award(advancement, CODE_CRITERION);
		}
	}
}
