package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import io.github.chaos634.kumpel.config.KumpelSettings;

/**
 * The Kumpel's backpack ("Kiepe"). It always has room for six rows, but the Kumpel only fills
 * (and the menu only shows) as many rows as its level allows.
 */
public class KumpelPockets extends SimpleContainer {
	public static final int SIZE = KumpelTier.MAX_POCKET_ROWS * 9;
	private static final double MAX_USE_DISTANCE_SQ = 8.0 * 8.0;

	private final KumpelEntity owner;

	public KumpelPockets(KumpelEntity owner) {
		super(SIZE);
		this.owner = owner;
	}

	private int usableSlots() {
		return Math.min(SIZE, owner.getTier().pocketSlots());
	}

	/**
	 * Puts as much of the stack as fits into the usable slots.
	 *
	 * @return what didn't fit
	 */
	public ItemStack addToPockets(ItemStack stack) {
		ItemStack remaining = stack.copy();
		int slots = usableSlots();

		for (int i = 0; i < slots && !remaining.isEmpty(); i++) {
			ItemStack existing = getItem(i);
			if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
				int room = Math.min(existing.getMaxStackSize(), getMaxStackSize()) - existing.getCount();
				int moved = Math.min(room, remaining.getCount());
				if (moved > 0) {
					existing.grow(moved);
					remaining.shrink(moved);
				}
			}
		}

		for (int i = 0; i < slots && !remaining.isEmpty(); i++) {
			if (getItem(i).isEmpty()) {
				setItem(i, remaining.copy());
				remaining = ItemStack.EMPTY;
			}
		}

		setChanged();
		return remaining;
	}

	public boolean canAddToPockets(ItemStack stack) {
		int slots = usableSlots();
		for (int i = 0; i < slots; i++) {
			ItemStack existing = getItem(i);
			if (existing.isEmpty()) {
				return true;
			}
			if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
				return true;
			}
		}

		return false;
	}

	public boolean pocketsFull() {
		int slots = usableSlots();
		for (int i = 0; i < slots; i++) {
			ItemStack existing = getItem(i);
			if (existing.isEmpty() || existing.getCount() < existing.getMaxStackSize()) {
				return false;
			}
		}

		return true;
	}

	/** Torches the Kumpel can place itself. */
	public static boolean isTorch(ItemStack stack) {
		return KumpelSettings.get().torchBlock(stack) != null;
	}

	public static boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}

	/**
	 * Takes out everything the Kumpel should bring to its owner. It keeps its torches and,
	 * if food sharing is on, one stack of food as a packed lunch.
	 */
	public List<ItemStack> takeLoot() {
		boolean keepFood = KumpelSettings.get().behaviour().shareFood;
		List<ItemStack> loot = new ArrayList<>();

		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (stack.isEmpty() || isTorch(stack)) {
				continue;
			}
			if (keepFood && isFood(stack)) {
				keepFood = false;
				continue;
			}

			loot.add(removeItemNoUpdate(i));
		}

		setChanged();
		return loot;
	}

	public boolean hasLoot() {
		boolean keepFood = KumpelSettings.get().behaviour().shareFood;
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (stack.isEmpty() || isTorch(stack)) {
				continue;
			}
			if (keepFood && isFood(stack)) {
				keepFood = false;
				continue;
			}

			return true;
		}

		return false;
	}

	/** Takes one item matching the filter, or returns an empty stack. */
	public ItemStack takeOne(Predicate<ItemStack> filter) {
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (!stack.isEmpty() && filter.test(stack)) {
				ItemStack taken = stack.split(1);
				setChanged();
				return taken;
			}
		}

		return ItemStack.EMPTY;
	}

	public int count(Predicate<ItemStack> filter) {
		int count = 0;
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (!stack.isEmpty() && filter.test(stack)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	public int countItems() {
		int count = 0;
		for (int i = 0; i < getContainerSize(); i++) {
			count += getItem(i).getCount();
		}

		return count;
	}

	@Override
	public boolean stillValid(Player player) {
		return owner.isAlive() && owner.isOwnedBy(player) && owner.distanceToSqr(player) < MAX_USE_DISTANCE_SQ;
	}
}
