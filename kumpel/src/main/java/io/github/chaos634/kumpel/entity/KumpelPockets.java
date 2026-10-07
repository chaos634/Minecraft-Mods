package io.github.chaos634.kumpel.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.behaviour.FieldForge;

/**
 * The Kumpel's backpack ("Kiepe"). It always has room for six rows, but the Kumpel only fills
 * (and the menu only shows) as many rows as its level allows.
 */
public class KumpelPockets extends SimpleContainer {
	public static final int SIZE = KumpelTier.MAX_POCKET_ROWS * 9;
	/** What the Kumpel may use to seal leaks in its tunnels; modpacks can add their stone. */
	public static final TagKey<Item> TUNNEL_FILLERS = TagKey.create(Registries.ITEM, Kumpel.id("tunnel_fillers"));
	/** What the Kumpel builds the support frames in its tunnels from: logs, and steel girders from Zechenbau. */
	public static final TagKey<Item> TUNNEL_SUPPORTS = TagKey.create(Registries.ITEM, Kumpel.id("tunnel_supports"));
	/** The lamps it puts into those frames: lanterns, and the Grubenlampe from Zechenbau. */
	public static final TagKey<Item> TUNNEL_LAMPS = TagKey.create(Registries.ITEM, Kumpel.id("tunnel_lamps"));
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

	public static boolean isPickaxe(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ItemTags.PICKAXES);
	}

	/** Stone and the like that the Kumpel uses to seal leaks in its tunnels. */
	public static boolean isTunnelFiller(ItemStack stack) {
		return !stack.isEmpty() && stack.is(TUNNEL_FILLERS) && stack.getItem() instanceof BlockItem;
	}

	/** Logs, girders and the like for the support frames in its tunnels. */
	public static boolean isTunnelSupport(ItemStack stack) {
		return !stack.isEmpty() && stack.is(TUNNEL_SUPPORTS) && stack.getItem() instanceof BlockItem;
	}

	/** Lamps for the support frames in its tunnels. */
	public static boolean isTunnelLamp(ItemStack stack) {
		return !stack.isEmpty() && stack.is(TUNNEL_LAMPS) && stack.getItem() instanceof BlockItem;
	}

	/**
	 * Takes out everything the Kumpel should bring to its owner. It keeps its torches,
	 * one stack of food as a packed lunch (if food sharing is on) and a pickaxe, if it has none in its hand.
	 */
	public List<ItemStack> takeLoot() {
		List<ItemStack> loot = new ArrayList<>();
		Supplies supplies = new Supplies();

		for (int i = 0; i < getContainerSize(); i++) {
			if (!supplies.keeps(getItem(i))) {
				loot.add(removeItemNoUpdate(i));
			}
		}

		setChanged();
		return loot;
	}

	public boolean hasLoot() {
		Supplies supplies = new Supplies();
		for (int i = 0; i < getContainerSize(); i++) {
			if (!supplies.keeps(getItem(i))) {
				return true;
			}
		}

		return false;
	}

	/** Decides, stack by stack, what the Kumpel keeps for itself. */
	private class Supplies {
		private boolean keepFood = KumpelSettings.get().behaviour().shareFood;
		private boolean keepPickaxe = owner.getMainHandItem().isEmpty();
		private boolean keepFuel = owner.hasForge();
		// A stack of stone to seal leaks with, while there is a tunnel to dig.
		private boolean keepFiller = owner.getTunnel() != null && KumpelSettings.get().behaviour().sealTunnels;
		// A Hauer keeps a stack of pit props and one of lamps for its tunnels.
		private boolean keepSupports = owner.isHauer() && KumpelSettings.get().behaviour().tunnelSupports;
		private boolean keepLamps = keepSupports;
		// Raw ores wait for the field forge, as long as it has something to burn.
		private final boolean keepSmeltables = owner.hasForge() && owner.getForge().hasFuel(KumpelPockets.this);

		boolean keeps(ItemStack stack) {
			if (stack.isEmpty() || isTorch(stack)) {
				return true;
			}
			if (owner.isBuildingMaterial(stack)) {
				return true;
			}
			if (keepFuel && FieldForge.isFuel(stack)) {
				keepFuel = false;
				return true;
			}
			if (keepSmeltables && KumpelSettings.get().isSmeltable(stack)) {
				return true;
			}
			if (keepFood && isFood(stack)) {
				keepFood = false;
				return true;
			}
			if (keepFiller && isTunnelFiller(stack)) {
				keepFiller = false;
				return true;
			}
			if (keepSupports && isTunnelSupport(stack)) {
				keepSupports = false;
				return true;
			}
			if (keepLamps && isTunnelLamp(stack)) {
				keepLamps = false;
				return true;
			}
			if (keepPickaxe && isPickaxe(stack) && !KumpelEntity.isWornOut(stack)) {
				keepPickaxe = false;
				return true;
			}

			return false;
		}
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

	/** Takes out the first whole stack matching the filter, or returns an empty stack. */
	public ItemStack takeFirst(Predicate<ItemStack> filter) {
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (!stack.isEmpty() && filter.test(stack)) {
				ItemStack taken = removeItemNoUpdate(i);
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
