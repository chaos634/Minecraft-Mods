package io.github.chaos634.kumpel.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

import io.github.chaos634.kumpel.Kumpel;

/**
 * The contents of {@code config/kumpel.json}. Field names are written in snake_case.
 *
 * <p>Missing values fall back to the defaults below, and new options are added to the file automatically
 * after an update, so the file can be edited by hand without worrying about breaking it.
 */
public final class KumpelConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.create();

	public String comment = "Kumpel config. Every option is explained in the mod's README. Use /kumpel reload to apply changes without restarting.";

	/** Levels in ascending order. The first one is the level every new Kumpel starts with. */
	public List<Tier> tiers = defaultTiers();
	/** Which ores a Kumpel can sense, and from which level on. */
	public List<Ore> ores = defaultOres();
	/** What happens with ores that are in {@code c:ores} but not listed above (e.g. ores from other mods). */
	public UnlistedOres unlistedOres = new UnlistedOres();
	/** Items a Kumpel can be fed for experience. */
	public List<Food> feeding = defaultFeeding();
	public Behaviour behaviour = new Behaviour();

	public static class Tier {
		public String name;
		public int experience;
		public double maxHealth = 20.0;
		public double movementSpeed = 0.27;
		public int senseRadius = 8;
		public int collectRadius = 6;
		/** Rows of the Kumpel's backpack (1 to 6, nine slots each). */
		public int pocketRows = 1;
		public boolean fireImmune = false;
		/** Texture of the Kumpel at this level, e.g. {@code kumpel:textures/entity/kumpel/copper.png}. */
		public String texture;

		public Tier() {
		}

		Tier(String name, int experience, double maxHealth, double movementSpeed, int senseRadius, int collectRadius, int pocketRows, boolean fireImmune) {
			this.name = name;
			this.experience = experience;
			this.maxHealth = maxHealth;
			this.movementSpeed = movementSpeed;
			this.senseRadius = senseRadius;
			this.collectRadius = collectRadius;
			this.pocketRows = pocketRows;
			this.fireImmune = fireImmune;
			this.texture = "kumpel:textures/entity/kumpel/" + name + ".png";
		}
	}

	public static class Ore {
		/** A block tag like {@code c:ores/tin}. Either this or {@link #block} must be set. */
		public String tag;
		/** A single block like {@code minecraft:ancient_debris}. */
		public String block;
		/** The Kumpel level needed to sense this ore. */
		public int level = 1;
		/** How valuable the ore is. The Kumpel always points at the most valuable ore it can sense. */
		public int value = 10;
		/** Pitch of the chime when this ore is found (0.5 to 2.0). */
		public float pitch = 1.0F;

		public Ore() {
		}

		static Ore tag(String tag, int level, int value, float pitch) {
			Ore ore = new Ore();
			ore.tag = tag;
			ore.level = level;
			ore.value = value;
			ore.pitch = pitch;
			return ore;
		}
	}

	public static class UnlistedOres {
		public boolean enabled = true;
		public int level = 3;
		public int value = 45;
		public float pitch = 1.15F;
	}

	public static class Food {
		/** An item like {@code minecraft:diamond}. Either this or {@link #tag} must be set. */
		public String item;
		/** An item tag like {@code c:gems}. */
		public String tag;
		public int experience;

		public Food() {
		}

		static Food item(String item, int experience) {
			Food food = new Food();
			food.item = item;
			food.experience = experience;
			return food;
		}
	}

	public static class Behaviour {
		/** Item that repairs a Kumpel, and how much health one item restores. */
		public String repairItem = "minecraft:copper_ingot";
		public float repairAmount = 5.0F;
		/** Experience for each stack of items picked up. */
		public int experiencePerPickup = 1;
		/** Ticks between two ore scans (20 ticks = 1 second). */
		public int senseIntervalTicks = 40;
		/** Minimum ticks between two ore announcements, unless a more valuable ore turns up. */
		public int announceCooldownTicks = 300;
		/** The same ore block is not announced again within this many ticks. */
		public int sameOreCooldownTicks = 2400;
		/** How long the Kumpel waits after its last pickup before bringing its loot to you. */
		public int deliverDelayTicks = 80;
		/** Items further away from the owner than this are left alone. */
		public int maxCollectDistanceFromOwner = 16;
	}

	private static List<Tier> defaultTiers() {
		List<Tier> tiers = new ArrayList<>();
		tiers.add(new Tier("copper", 0, 20.0, 0.27, 8, 6, 1, false));
		tiers.add(new Tier("iron", 60, 30.0, 0.28, 11, 8, 1, false));
		tiers.add(new Tier("gold", 180, 40.0, 0.29, 14, 10, 2, false));
		tiers.add(new Tier("diamond", 400, 50.0, 0.30, 17, 12, 2, false));
		tiers.add(new Tier("netherite", 800, 60.0, 0.31, 20, 14, 3, true));
		return tiers;
	}

	private static List<Ore> defaultOres() {
		List<Ore> ores = new ArrayList<>();
		ores.add(Ore.tag("c:ores/coal", 1, 10, 0.7F));
		ores.add(Ore.tag("c:ores/copper", 1, 15, 0.8F));
		ores.add(Ore.tag("c:ores/quartz", 2, 20, 0.9F));
		ores.add(Ore.tag("c:ores/iron", 2, 25, 1.0F));
		ores.add(Ore.tag("c:ores/redstone", 2, 30, 1.05F));
		ores.add(Ore.tag("c:ores/lapis", 3, 35, 1.1F));
		ores.add(Ore.tag("c:ores/gold", 3, 40, 1.2F));
		ores.add(Ore.tag("c:ores/emerald", 4, 60, 1.4F));
		ores.add(Ore.tag("c:ores/diamond", 4, 70, 1.6F));
		ores.add(Ore.tag("c:ores/netherite_scrap", 5, 100, 1.9F));
		return ores;
	}

	private static List<Food> defaultFeeding() {
		List<Food> feeding = new ArrayList<>();
		feeding.add(Food.item("minecraft:coal", 1));
		feeding.add(Food.item("minecraft:raw_copper", 2));
		feeding.add(Food.item("minecraft:copper_ingot", 2));
		feeding.add(Food.item("minecraft:redstone", 2));
		feeding.add(Food.item("minecraft:lapis_lazuli", 3));
		feeding.add(Food.item("minecraft:quartz", 3));
		feeding.add(Food.item("minecraft:amethyst_shard", 4));
		feeding.add(Food.item("minecraft:raw_iron", 4));
		feeding.add(Food.item("minecraft:iron_ingot", 6));
		feeding.add(Food.item("minecraft:raw_gold", 6));
		feeding.add(Food.item("minecraft:gold_ingot", 8));
		feeding.add(Food.item("minecraft:emerald", 25));
		feeding.add(Food.item("minecraft:diamond", 40));
		feeding.add(Food.item("minecraft:netherite_scrap", 80));
		feeding.add(Food.item("minecraft:netherite_ingot", 300));
		return feeding;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(Kumpel.MOD_ID + ".json");
	}

	/**
	 * Reads the config file, creating it with defaults if it doesn't exist yet.
	 * A broken file is left untouched and the defaults are used instead.
	 */
	public static KumpelConfig load() {
		Path path = path();
		KumpelConfig config = new KumpelConfig();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				KumpelConfig read = GSON.fromJson(reader, KumpelConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | JsonParseException e) {
				Kumpel.LOGGER.error("Could not read {}, using the default settings instead. Fix the file and run /kumpel reload.", path, e);
				return new KumpelConfig();
			}
		}

		config.fillMissingValues();
		config.save();
		return config;
	}

	public void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Kumpel.LOGGER.error("Could not write {}", path, e);
		}
	}

	private void fillMissingValues() {
		if (tiers == null || tiers.isEmpty()) {
			tiers = defaultTiers();
		}
		if (ores == null) {
			ores = defaultOres();
		}
		if (unlistedOres == null) {
			unlistedOres = new UnlistedOres();
		}
		if (feeding == null) {
			feeding = defaultFeeding();
		}
		if (behaviour == null) {
			behaviour = new Behaviour();
		}
	}
}
