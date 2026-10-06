package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.chaos634.kumpel.util.Chunks;

/**
 * Erzfunde: the most valuable ores a Kumpel has sensed, and where they are. They go into its shift report.
 */
public class OreFinds {
	public static final int MAX_FINDS = 8;
	private static final int FINDS_PER_PAGE = 4;

	public record Find(BlockPos pos, Identifier block, int value) {
		public static final Codec<Find> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(Find::pos),
				Identifier.CODEC.fieldOf("block").forGetter(Find::block),
				Codec.INT.fieldOf("value").forGetter(Find::value)
		).apply(instance, Find::new));
	}

	private final List<Find> finds = new ArrayList<>();

	/** Remembers an ore, as long as it is among the {@value #MAX_FINDS} most valuable ones. */
	public void remember(BlockPos pos, BlockState state, int value) {
		forget(pos);
		finds.add(new Find(pos.immutable(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), value));
		finds.sort((a, b) -> Integer.compare(b.value(), a.value()));
		while (finds.size() > MAX_FINDS) {
			finds.removeLast();
		}
	}

	public void forget(BlockPos pos) {
		finds.removeIf(find -> find.pos().equals(pos));
	}

	/** Forgets ores that are gone (mined by anyone), wherever the Kumpel can check without loading chunks. */
	public void tidyUp(ServerLevel level) {
		finds.removeIf(find -> Chunks.isLoaded(level, SectionPos.blockToSectionCoord(find.pos().getX()), SectionPos.blockToSectionCoord(find.pos().getZ()))
				&& !BuiltInRegistries.BLOCK.getKey(level.getBlockState(find.pos()).getBlock()).equals(find.block()));
	}

	public List<Find> finds() {
		return Collections.unmodifiableList(finds);
	}

	/** Book pages listing the finds, most valuable first; none if there are no finds. */
	public List<Component> pages() {
		List<Component> pages = new ArrayList<>();
		MutableComponent page = null;
		for (int i = 0; i < finds.size(); i++) {
			if (i % FINDS_PER_PAGE == 0) {
				page = Component.empty().append(Component.translatable("book.kumpel.finds").withStyle(ChatFormatting.BOLD)).append("\n");
				pages.add(page);
			}

			Find find = finds.get(i);
			page.append("\n")
					.append(BuiltInRegistries.BLOCK.getValue(find.block()).getName())
					.append("\n")
					.append(Component.translatable("book.kumpel.find_at", find.pos().getX(), find.pos().getY(), find.pos().getZ())
							.withStyle(ChatFormatting.DARK_GRAY));
		}

		return pages;
	}

	public void save(ValueOutput output) {
		output.store("ore_finds", Find.CODEC.listOf(), List.copyOf(finds));
	}

	public void load(ValueInput input) {
		finds.clear();
		input.read("ore_finds", Find.CODEC.listOf()).ifPresent(list -> {
			finds.addAll(list);
			finds.sort((a, b) -> Integer.compare(b.value(), a.value()));
			while (finds.size() > MAX_FINDS) {
				finds.removeLast();
			}
		});
	}
}
