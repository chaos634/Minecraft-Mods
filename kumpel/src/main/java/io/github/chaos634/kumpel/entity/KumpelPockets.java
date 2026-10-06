package io.github.chaos634.kumpel.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
