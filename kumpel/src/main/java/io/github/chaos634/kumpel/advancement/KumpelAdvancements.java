package io.github.chaos634.kumpel.advancement;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import io.github.chaos634.kumpel.Kumpel;

/**
 * The Kumpel advancements. Apart from the root, each has a single {@code minecraft:impossible} criterion
 * named {@value #CODE_CRITERION} and is awarded from code.
 */
public final class KumpelAdvancements {
	public static final String CODE_CRITERION = "code";

	public static final String GLUECK_AUF = "glueck_auf";
	public static final String REVIVED = "revived";
	public static final String MAX_LEVEL = "max_level";
	public static final String FULL_CREW = "full_crew";
	public static final String SHIFT_END = "shift_end";
	public static final String HAUER = "hauer";
	public static final String STORAGE = "storage";
	public static final String STEIGERLIED = "steigerlied";
	public static final String BARBARA = "barbara";
	public static final String EARLY_WARNING = "early_warning";
	public static final String VOR_ORT = "vor_ort";
	public static final String HUETTE = "huette";
	public static final String SEILFAHRT = "seilfahrt";
	public static final String GRUBENWEHR = "grubenwehr";
	public static final String RESCUED = "rescued";
	public static final String AUSFAHRT = "ausfahrt";
	public static final String ZOLLVEREIN = "zollverein";
	public static final String MARKENKONTROLLE = "markenkontrolle";
	public static final String KUMPELKAPELLE = "kumpelkapelle";
	public static final String LEIBGERICHT = "leibgericht";

	/** Every advancement the mod ships. */
	public static final List<String> ALL = List.of(
			"root", GLUECK_AUF, REVIVED, MAX_LEVEL, FULL_CREW, SHIFT_END, HAUER, STORAGE, STEIGERLIED, BARBARA, EARLY_WARNING, VOR_ORT, HUETTE, SEILFAHRT, GRUBENWEHR, RESCUED, AUSFAHRT,
			ZOLLVEREIN, MARKENKONTROLLE, KUMPELKAPELLE, LEIBGERICHT);

	private KumpelAdvancements() {
	}

	/**
	 * Awards one of the code-triggered advancements to a player (does nothing for anything else).
	 *
	 * @return whether the player just got it for the first time
	 */
	public static boolean award(Entity entity, String name) {
		if (!(entity instanceof ServerPlayer player)) {
			return false;
		}

		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Kumpel.id(name));
		return advancement != null && player.getAdvancements().award(advancement, CODE_CRITERION);
	}
}
