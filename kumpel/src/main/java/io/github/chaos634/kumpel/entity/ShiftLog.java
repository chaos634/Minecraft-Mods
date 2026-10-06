package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Schichtbuch: what a Kumpel has done in its life. Written into a book when its owner asks for a report.
 */
public class ShiftLog {
	public enum Entry {
		ORES_SENSED("ores_sensed"),
		ORES_MINED("ores_mined"),
		BLOCKS_DUG("blocks_dug"),
		TUNNELS_DUG("tunnels_dug"),
		ITEMS_COLLECTED("items_collected"),
		ITEMS_DELIVERED("items_delivered"),
		ITEMS_SMELTED("items_smelted"),
		TORCHES_PLACED("torches_placed"),
		MONSTERS_DEFEATED("monsters_defeated"),
		LEAKS_SEALED("leaks_sealed"),
		COKE_MADE("coke_made");

		private final String key;

		Entry(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}
	}

	/** Entries per book page, so that even long lines that wrap still fit. */
	private static final int LINES_PER_PAGE = 6;

	private final long[] counts = new long[Entry.values().length];

	public void add(Entry entry, long amount) {
		if (amount > 0) {
			counts[entry.ordinal()] += amount;
		}
	}

	public void add(Entry entry) {
		add(entry, 1);
	}

	public long get(Entry entry) {
		return counts[entry.ordinal()];
	}

	/** One line per entry, for the book ({@code book.kumpel.log.<entry>}). */
	public List<Component> lines() {
		List<Component> lines = new ArrayList<>();
		for (Entry entry : Entry.values()) {
			lines.add(Component.translatable("book.kumpel.log." + entry.key(), get(entry)));
		}

		return lines;
	}

	/** The lines, a few per book page. */
	public List<Component> pages() {
		List<Component> pages = new ArrayList<>();
		List<Component> lines = lines();
		for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
			MutableComponent page = Component.empty();
			for (Component line : lines.subList(start, Math.min(lines.size(), start + LINES_PER_PAGE))) {
				page.append(line).append("\n");
			}
			pages.add(page);
		}

		return pages;
	}

	public void save(ValueOutput output) {
		for (Entry entry : Entry.values()) {
			output.putLong("log_" + entry.key(), get(entry));
		}
	}

	public void load(ValueInput input) {
		for (Entry entry : Entry.values()) {
			counts[entry.ordinal()] = Math.max(0, input.getLongOr("log_" + entry.key(), 0));
		}
	}
}
