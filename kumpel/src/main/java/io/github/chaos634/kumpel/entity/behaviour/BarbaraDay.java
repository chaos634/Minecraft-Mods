package io.github.chaos634.kumpel.entity.behaviour;

import java.time.LocalDate;
import java.time.Month;

import io.github.chaos634.kumpel.config.KumpelSettings;

/**
 * Barbaratag: 4 December, the day of Saint Barbara, patron saint of miners. Kumpels wear a Barbara branch
 * and feeding them gives double experience.
 */
public final class BarbaraDay {
	private static volatile Boolean override;

	private BarbaraDay() {
	}

	public static boolean isToday() {
		if (!KumpelSettings.get().behaviour().barbaraDay) {
			return false;
		}

		Boolean forced = override;
		if (forced != null) {
			return forced;
		}

		LocalDate today = LocalDate.now();
		return today.getMonth() == Month.DECEMBER && today.getDayOfMonth() == 4;
	}

	/** Pretends it is (or isn't) Barbaratag, for tests and screenshots; {@code null} goes back to the calendar. */
	public static void setOverride(Boolean value) {
		override = value;
	}
}
