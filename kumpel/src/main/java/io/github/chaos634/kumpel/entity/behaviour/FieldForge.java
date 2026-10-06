package io.github.chaos634.kumpel.entity.behaviour;

import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelPockets;
import io.github.chaos634.kumpel.entity.ShiftLog;

/**
 * Feldschmiede: a small furnace on the Kumpel's back. It smelts raw ores from the backpack, one at a time,
 * burning coal from the backpack (one piece lasts for {@value #ITEMS_PER_COAL} items, like in a furnace).
 */
public class FieldForge {
	public static final int ITEMS_PER_COAL = 8;
	/** How often (in ticks) {@link #tick} is called. */
	public static final int INTERVAL = 20;

	private int fuel;
	private int progress;
	private boolean burning;

	public void tick(KumpelEntity kumpel, ServerLevel level) {
		burning = false;
		if (!KumpelSettings.get().behaviour().fieldForge) {
			return;
		}

		KumpelPockets pockets = kumpel.getPockets();
		Smelt smelt = findSomethingToSmelt(level, pockets);
		if (smelt == null || (fuel <= 0 && pockets.count(FieldForge::isFuel) == 0)) {
			progress = 0;
			return;
		}

		burning = true;
		progress += INTERVAL;
		Vec3 back = kumpel.position().add(Vec3.directionFromRotation(0.0F, kumpel.yBodyRot).scale(-0.35)).add(0.0, 0.9, 0.0);
		level.sendParticles(ParticleTypes.SMOKE, back.x, back.y + 0.3, back.z, 2, 0.05, 0.05, 0.05, 0.01);
		level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 1, 0.05, 0.02, 0.05, 0.0);

		if (progress < Math.max(INTERVAL, KumpelSettings.get().behaviour().smeltTicks)) {
			return;
		}

		progress = 0;
		if (fuel <= 0) {
			pockets.takeOne(FieldForge::isFuel);
			fuel = ITEMS_PER_COAL;
		}

		pockets.getItem(smelt.slot()).shrink(1);
		pockets.addToPockets(smelt.result());
		fuel--;
		kumpel.addExperience(1);
		kumpel.getLog().add(ShiftLog.Entry.ITEMS_SMELTED);
		KumpelAdvancements.award(kumpel.getOwner(), KumpelAdvancements.HUETTE);
	}

	private record Smelt(int slot, ItemStack result) {
	}

	/** The first smeltable item in the backpack whose result still fits in. */
	private static Smelt findSomethingToSmelt(ServerLevel level, KumpelPockets pockets) {
		KumpelSettings settings = KumpelSettings.get();
		for (int slot = 0; slot < pockets.getContainerSize(); slot++) {
			ItemStack stack = pockets.getItem(slot);
			if (stack.isEmpty() || !settings.isSmeltable(stack)) {
				continue;
			}

			SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
			Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
			if (recipe.isEmpty()) {
				continue;
			}

			ItemStack result = recipe.get().value().assemble(input);
			if (!result.isEmpty() && pockets.canAddToPockets(result)) {
				return new Smelt(slot, result);
			}
		}

		return null;
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.is(ItemTags.COALS);
	}

	/** Whether the forge could keep working: it has embers left or coal to burn. */
	public boolean hasFuel(KumpelPockets pockets) {
		return fuel > 0 || pockets.count(FieldForge::isFuel) > 0;
	}

	public boolean isBurning() {
		return burning;
	}

	public void save(ValueOutput output) {
		output.putInt("forge_fuel", fuel);
		output.putInt("forge_progress", progress);
	}

	public void load(ValueInput input) {
		fuel = Mth.clamp(input.getIntOr("forge_fuel", 0), 0, ITEMS_PER_COAL);
		progress = Math.max(0, input.getIntOr("forge_progress", 0));
	}
}
