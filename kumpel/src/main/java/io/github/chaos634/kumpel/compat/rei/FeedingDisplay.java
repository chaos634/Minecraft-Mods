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

/** REI display: an item a Kumpel eats and the experience it gives. */
public class FeedingDisplay extends BasicDisplay {
	public static final CategoryIdentifier<FeedingDisplay> CATEGORY = CategoryIdentifier.of(Kumpel.MOD_ID, "feeding");
	public static final DisplaySerializer<FeedingDisplay> SERIALIZER = DisplaySerializer.of(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					EntryIngredient.codec().fieldOf("items").forGetter(FeedingDisplay::items),
					Codec.INT.fieldOf("experience").forGetter(FeedingDisplay::experience)
			).apply(instance, FeedingDisplay::new)),
			StreamCodec.composite(
					EntryIngredient.streamCodec(), FeedingDisplay::items,
					ByteBufCodecs.VAR_INT, FeedingDisplay::experience,
					FeedingDisplay::new));

	private final int experience;

	public FeedingDisplay(EntryIngredient items, int experience) {
		super(List.of(items), List.of());
		this.experience = experience;
	}

	public EntryIngredient items() {
		return getInputEntries().getFirst();
	}

	public int experience() {
		return experience;
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends FeedingDisplay> getSerializer() {
		return SERIALIZER;
	}
}
