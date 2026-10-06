package io.github.chaos634.kumpel.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.entity.OreRule;

/**
 * The config, checked and turned into something fast to use at runtime.
 * Replaced as a whole when the config is reloaded, so readers always see a consistent state.
 */
public final class KumpelSettings {
	private static final Identifier FALLBACK_TEXTURE = Kumpel.id("textures/entity/kumpel/copper.png");
	private static final OreRule NO_ORE = new OreRule(null, null, Integer.MAX_VALUE, 0, 1.0F);

	private static volatile KumpelSettings current = compile(new KumpelConfig());
	private static volatile int revision;

	private final List<KumpelTier> tiers;
	private final List<OreRule> ores;
	private final OreRule unlistedOre;
	private final List<FoodRule> feeding;
	private final Identifier repairItem;
	private final List<Identifier> torchItems;
	private final List<String> names;
	private final KumpelConfig.Behaviour behaviour;
	private final Map<BlockState, OreRule> oreCache = new ConcurrentHashMap<>();

	private KumpelSettings(List<KumpelTier> tiers, List<OreRule> ores, OreRule unlistedOre, List<FoodRule> feeding, Identifier repairItem,
			List<Identifier> torchItems, List<String> names, KumpelConfig.Behaviour behaviour) {
		this.tiers = List.copyOf(tiers);
		this.ores = List.copyOf(ores);
		this.unlistedOre = unlistedOre;
		this.feeding = List.copyOf(feeding);
		this.repairItem = repairItem;
		this.torchItems = List.copyOf(torchItems);
		this.names = List.copyOf(names);
		this.behaviour = behaviour;
	}

	public static KumpelSettings get() {
		return current;
	}

	/** Increases every time the config is (re)loaded, so Kumpels know to recheck their level. */
	public static int revision() {
		return revision;
	}

	/** Reads {@code config/kumpel.json} and makes it the active config. */
	public static KumpelSettings reload() {
		KumpelSettings settings = compile(KumpelConfig.load());
		current = settings;
		revision++;
		Kumpel.LOGGER.info("Loaded Kumpel config: {} levels, {} ores, {} foods", settings.tiers.size(), settings.ores.size(), settings.feeding.size());
		return settings;
	}

	/** Forgets which blocks are ores, e.g. after tags changed because of a datapack reload. */
	public static void clearCaches() {
		current.oreCache.clear();
	}

	// ------------------------------------------------------------------
	// Levels

	public List<KumpelTier> tiers() {
		return tiers;
	}

	public KumpelTier tier(int level) {
		return tiers.get(Mth.clamp(level - 1, 0, tiers.size() - 1));
	}

	public KumpelTier firstTier() {
		return tiers.getFirst();
	}

	public KumpelTier maxTier() {
		return tiers.getLast();
	}

	public KumpelTier tierForExperience(int experience) {
		KumpelTier result = tiers.getFirst();
		for (KumpelTier tier : tiers) {
			if (experience >= tier.requiredExperience()) {
				result = tier;
			}
		}

		return result;
	}

	/** The next level, or {@code null} at the highest level. */
	public KumpelTier nextTier(KumpelTier tier) {
		return tier.level() < tiers.size() ? tiers.get(tier.level()) : null;
	}

	// ------------------------------------------------------------------
	// Ores

	/** Ore rules from the config, most valuable first. */
	public List<OreRule> ores() {
		return ores;
	}

	/** The rule for ores that are only in {@code c:ores}, or {@code null} if that is turned off. */
	public OreRule unlistedOre() {
		return unlistedOre;
	}

	/** The most valuable ore rule this block matches, or {@code null} if it is no ore. */
	public OreRule matchOre(BlockState state) {
		OreRule rule = oreCache.computeIfAbsent(state, this::findOre);
		return rule == NO_ORE ? null : rule;
	}

	private OreRule findOre(BlockState state) {
		for (OreRule rule : ores) {
			if (rule.matches(state)) {
				return rule;
			}
		}

		if (unlistedOre != null && unlistedOre.matches(state)) {
			return unlistedOre;
		}

		return NO_ORE;
	}

	// ------------------------------------------------------------------
	// Feeding & repairing

	public List<FoodRule> feeding() {
		return feeding;
	}

	/** Experience for feeding this item, or 0 if a Kumpel doesn't eat it. */
	public int feedExperience(ItemStack stack) {
		for (FoodRule food : feeding) {
			if (food.matches(stack)) {
				return food.experience();
			}
		}

		return 0;
	}

	public boolean isRepairItem(ItemStack stack) {
		return !stack.isEmpty() && repairItem.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	public Identifier repairItem() {
		return repairItem;
	}

	/** The block a Kumpel places for this torch item, or {@code null} if it isn't one of the configured torches. */
	public Block torchBlock(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
			return null;
		}

		return torchItems.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())) ? blockItem.getBlock() : null;
	}

	public KumpelConfig.Behaviour behaviour() {
		return behaviour;
	}

	public List<String> names() {
		return names;
	}

	/** A random name for a new Kumpel, or {@code null} if names are turned off. */
	public String randomName(RandomSource random) {
		if (!behaviour.giveNames || names.isEmpty()) {
			return null;
		}

		return names.get(random.nextInt(names.size()));
	}

	public record FoodRule(TagKey<Item> tag, Identifier item, int experience) {
		public boolean matches(ItemStack stack) {
			if (stack.isEmpty()) {
				return false;
			}

			if (tag != null && stack.is(tag)) {
				return true;
			}

			return item != null && item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
		}
	}

	// ------------------------------------------------------------------
	// Turning the config into settings

	static KumpelSettings compile(KumpelConfig config) {
		List<KumpelConfig.Tier> tierEntries = new ArrayList<>(config.tiers);
		tierEntries.sort(Comparator.comparingInt(tier -> tier.experience));

		List<KumpelTier> tiers = new ArrayList<>();
		for (KumpelConfig.Tier entry : tierEntries) {
			int level = tiers.size() + 1;
			String name = entry.name == null || entry.name.isBlank() ? "level_" + level : entry.name;
			Identifier texture = entry.texture == null ? null : Identifier.tryParse(entry.texture);
			if (texture == null) {
				Kumpel.LOGGER.warn("Kumpel level '{}' has no valid texture, using the copper one", name);
				texture = FALLBACK_TEXTURE;
			}

			tiers.add(new KumpelTier(
					level,
					name,
					level == 1 ? 0 : Math.max(0, entry.experience),
					Math.max(1.0, entry.maxHealth),
					Mth.clamp(entry.movementSpeed, 0.05, 1.0),
					Mth.clamp(entry.senseRadius, 0, 48),
					Mth.clamp(entry.collectRadius, 0, 32),
					Mth.clamp(entry.pocketRows, 1, KumpelTier.MAX_POCKET_ROWS),
					entry.fireImmune,
					texture
			));
		}

		List<OreRule> ores = new ArrayList<>();
		for (KumpelConfig.Ore entry : config.ores) {
			OreRule rule = oreRule(entry.tag, entry.block, entry.level, entry.value, entry.pitch);
			if (rule != null) {
				ores.add(rule);
			}
		}
		ores.sort(Comparator.comparingInt(OreRule::value).reversed());

		KumpelConfig.UnlistedOres unlisted = config.unlistedOres;
		OreRule unlistedOre = unlisted.enabled
				? new OreRule(ConventionalBlockTags.ORES, null, Math.max(1, unlisted.level), unlisted.value, Mth.clamp(unlisted.pitch, 0.5F, 2.0F))
				: null;

		List<FoodRule> feeding = new ArrayList<>();
		for (KumpelConfig.Food entry : config.feeding) {
			if (entry.experience <= 0) {
				continue;
			}

			if (entry.tag != null) {
				Identifier id = Identifier.tryParse(stripHash(entry.tag));
				if (id != null) {
					feeding.add(new FoodRule(TagKey.create(Registries.ITEM, id), null, entry.experience));
					continue;
				}
			}

			Identifier id = entry.item == null ? null : Identifier.tryParse(entry.item);
			if (id != null) {
				feeding.add(new FoodRule(null, id, entry.experience));
			} else {
				Kumpel.LOGGER.warn("Ignoring Kumpel food entry without a valid item or tag");
			}
		}

		Identifier repairItem = Identifier.tryParse(config.behaviour.repairItem == null ? "" : config.behaviour.repairItem);
		if (repairItem == null) {
			repairItem = Identifier.fromNamespaceAndPath("minecraft", "copper_ingot");
		}

		List<Identifier> torchItems = new ArrayList<>();
		if (config.behaviour.torchItems != null) {
			for (String torch : config.behaviour.torchItems) {
				Identifier id = torch == null ? null : Identifier.tryParse(torch);
				if (id != null) {
					torchItems.add(id);
				}
			}
		}

		List<String> names = new ArrayList<>();
		for (String name : config.names) {
			if (name != null && !name.isBlank()) {
				names.add(name.strip());
			}
		}

		return new KumpelSettings(tiers, ores, unlistedOre, feeding, repairItem, torchItems, names, config.behaviour);
	}

	private static OreRule oreRule(String tag, String block, int level, int value, float pitch) {
		level = Math.max(1, level);
		pitch = Mth.clamp(pitch, 0.5F, 2.0F);

		if (tag != null) {
			Identifier id = Identifier.tryParse(stripHash(tag));
			if (id != null) {
				return new OreRule(TagKey.create(Registries.BLOCK, id), null, level, value, pitch);
			}
		}

		Identifier id = block == null ? null : Identifier.tryParse(block);
		if (id != null) {
			return new OreRule(null, id, level, value, pitch);
		}

		Kumpel.LOGGER.warn("Ignoring Kumpel ore entry without a valid block or tag");
		return null;
	}

	private static String stripHash(String id) {
		return id.startsWith("#") ? id.substring(1) : id;
	}
}
