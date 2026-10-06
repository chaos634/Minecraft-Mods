package io.github.chaos634.kumpel.compat.rei;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;

import io.github.chaos634.kumpel.Kumpel;

/** REI display: ores a Kumpel senses and the level it needs for that. */
public class OreSensingDisplay extends BasicDisplay {
	public static final CategoryIdentifier<OreSensingDisplay> CATEGORY = CategoryIdentifier.of(Kumpel.MOD_ID, "ore_sensing");
	public static final DisplaySerializer<OreSensingDisplay> SERIALIZER = DisplaySerializer.of(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					EntryIngredient.codec().fieldOf("ores").forGetter(OreSensingDisplay::ores),
					Codec.INT.fieldOf("level").forGetter(OreSensingDisplay::level),
					Codec.INT.fieldOf("value").forGetter(OreSensingDisplay::value)
			).apply(instance, OreSensingDisplay::new)),
			StreamCodec.composite(
					EntryIngredient.streamCodec(), OreSensingDisplay::ores,
					ByteBufCodecs.VAR_INT, OreSensingDisplay::level,
					ByteBufCodecs.VAR_INT, OreSensingDisplay::value,
					OreSensingDisplay::new));

	private final int level;
	private final int value;

	public OreSensingDisplay(EntryIngredient ores, int level, int value) {
		super(List.of(ores), List.of());
		this.level = level;
		this.value = value;
	}

	public EntryIngredient ores() {
		return getInputEntries().getFirst();
	}

	public int level() {
		return level;
	}

	public int value() {
		return value;
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends OreSensingDisplay> getSerializer() {
		return SERIALIZER;
	}
}
