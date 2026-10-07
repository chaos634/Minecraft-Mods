package io.github.chaos634.taubenschlag.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;

/** The nest boxes of a loft, where the pigeons put down their post. */
public class TaubenschlagBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 9;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

	public TaubenschlagBlockEntity(BlockPos pos, BlockState state) {
		super(TaubenschlagBlocks.TAUBENSCHLAG_BLOCK_ENTITY, pos, state);
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.taubenschlag.taubenschlag");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, this);
		}
	}

	/**
	 * Puts post into the nest boxes, onto stacks of the same kind first.
	 *
	 * @return what did not fit
	 */
	public ItemStack receive(ItemStack stack) {
		for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				if (moved > 0) {
					slot.grow(moved);
					stack.shrink(moved);
				}
			}
		}
		for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				items.set(i, stack.split(stack.getMaxStackSize()));
			}
		}

		setChanged();
		return stack;
	}

	/** For comparators: 0 when empty, 15 when every nest box holds something. */
	public int signal() {
		int filled = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				filled++;
			}
		}

		return filled == 0 ? 0 : 1 + (filled * 14) / SLOTS;
	}
}
