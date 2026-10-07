package io.github.chaos634.taubenschlag.advancement;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import io.github.chaos634.taubenschlag.Taubenschlag;

/**
 * The Taubenschlag advancements. Apart from the root, each has a single {@code minecraft:impossible} criterion
 * named {@value #CODE_CRITERION} and is awarded from code.
 */
public final class TaubenschlagAdvancements {
	public static final String CODE_CRITERION = "code";

	public static final String RENNPFERD = "rennpferd";
	public static final String HEIMATSCHLAG = "heimatschlag";
	public static final String LUFTPOST = "luftpost";
	public static final String PREISFLUG = "preisflug";
	public static final String JUNGTAUBE = "jungtaube";

	/** Every advancement the mod ships. */
	public static final List<String> ALL = List.of("root", RENNPFERD, HEIMATSCHLAG, LUFTPOST, PREISFLUG, JUNGTAUBE);

	private TaubenschlagAdvancements() {
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

		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Taubenschlag.id(name));
		return advancement != null && player.getAdvancements().award(advancement, CODE_CRITERION);
	}
}
