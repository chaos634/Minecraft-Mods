package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import io.github.chaos634.kumpel.Kumpel;

/**
 * Markenkontrolle: at the pithead, every miner hung up a brass tag (Marke) when going down and took it back when
 * coming up, so nobody was ever left behind. Here, every Kumpel keeps its tag up to date with where it was last seen,
 * so you can find it even when its chunk is not loaded.
 */
public class Markenkontrolle extends SavedData {
	/** How long the tag of a Kumpel that died stays up (seven Minecraft days). */
	public static final long LOST_TAG_TICKS = 7L * 24000L;

	public record Marke(UUID kumpel, UUID owner, String name, int level, GlobalPos pos, String activity, long seenAt, boolean underground) {
		public static final Codec<Marke> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("kumpel").forGetter(Marke::kumpel),
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Marke::owner),
				Codec.STRING.fieldOf("name").forGetter(Marke::name),
				Codec.INT.fieldOf("level").forGetter(Marke::level),
				GlobalPos.CODEC.fieldOf("pos").forGetter(Marke::pos),
				Codec.STRING.fieldOf("activity").forGetter(Marke::activity),
				Codec.LONG.fieldOf("seen_at").forGetter(Marke::seenAt),
				Codec.BOOL.fieldOf("underground").forGetter(Marke::underground)
		).apply(instance, Marke::new));

		public boolean isLost() {
			return "lost".equals(activity);
		}
	}

	public static final Codec<Markenkontrolle> CODEC = Marke.CODEC.listOf().xmap(Markenkontrolle::new, Markenkontrolle::all);
	public static final SavedDataType<Markenkontrolle> TYPE = new SavedDataType<>(Kumpel.id("markenkontrolle"), Markenkontrolle::new, CODEC, null);

	private final Map<UUID, Marke> marken = new LinkedHashMap<>();

	public Markenkontrolle() {
	}

	private Markenkontrolle(List<Marke> list) {
		list.forEach(marke -> marken.put(marke.kumpel(), marke));
	}

	public static Markenkontrolle get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Marke> all() {
		return List.copyOf(marken.values());
	}

	public void hangUp(Marke marke) {
		if (!marken.containsKey(marke.kumpel())) {
			// A Kumpel brought back from its cracked core replaces the tag it left behind.
			marken.values().removeIf(old -> old.isLost() && old.owner().equals(marke.owner()) && old.name().equals(marke.name()));
		}
		if (!marke.equals(marken.put(marke.kumpel(), marke))) {
			setDirty();
		}
	}

	public void takeDown(UUID kumpel) {
		if (marken.remove(kumpel) != null) {
			setDirty();
		}
	}

	/** The tags of one player's Kumpels; old tags of Kumpels that died are taken down on the way. */
	public List<Marke> ofOwner(UUID owner, long now) {
		if (marken.values().removeIf(marke -> marke.isLost() && now - marke.seenAt() > LOST_TAG_TICKS)) {
			setDirty();
		}

		List<Marke> result = new ArrayList<>();
		for (Marke marke : marken.values()) {
			if (marke.owner().equals(owner)) {
				result.add(marke);
			}
		}

		return result;
	}
}
