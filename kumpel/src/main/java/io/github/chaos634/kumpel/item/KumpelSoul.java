package io.github.chaos634.kumpel.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * What a Kumpel Core remembers about the Kumpel that lived in it: its experience and settings.
 * Stored on the item as the {@code kumpel:soul} component; the name is kept as the item's custom name.
 */
public record KumpelSoul(int experience, boolean oreSensing) {
	public static final Codec<KumpelSoul> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("experience").forGetter(KumpelSoul::experience),
			Codec.BOOL.optionalFieldOf("ore_sensing", true).forGetter(KumpelSoul::oreSensing)
	).apply(instance, KumpelSoul::new));

	public static final StreamCodec<ByteBuf, KumpelSoul> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, KumpelSoul::experience,
			ByteBufCodecs.BOOL, KumpelSoul::oreSensing,
			KumpelSoul::new);
}
