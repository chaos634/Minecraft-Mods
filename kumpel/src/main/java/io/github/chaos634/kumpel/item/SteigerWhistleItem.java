package io.github.chaos634.kumpel.item;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;

/**
 * Steigerpfeife, the foreman's whistle. Calls all your Kumpels, sends them on a break, or (sneaking, on a container)
 * marks the chest they should bring their loot to.
 */
public class SteigerWhistleItem extends Item {
	private static final int COOLDOWN_TICKS = 20;
	private static final int FULL_CREW = 5;

	public SteigerWhistleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel serverLevel) {
			if (player.isSecondaryUseActive()) {
				sendOnBreak(serverLevel, player);
			} else {
				callKumpels(serverLevel, player);
			}
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		// Both sides must agree on whether this counts as marking a chest, so only look at things the client knows too.
		if (player == null || !player.isSecondaryUseActive() || level.getBlockEntity(pos) == null) {
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel serverLevel) {
			markStorage(serverLevel, player, pos);
			player.getCooldowns().addCooldown(context.getItemInHand(), COOLDOWN_TICKS);
		}

		return InteractionResult.SUCCESS;
	}

	/** Calls every Kumpel in hearing range: they stand up and come over (or teleport, if they are far away). */
	public static int callKumpels(ServerLevel level, Player player) {
		List<KumpelEntity> kumpels = KumpelEntity.findOwnedBy(level, player, range());
		for (KumpelEntity kumpel : kumpels) {
			kumpel.answerWhistle(player);
		}

		whistle(level, player, 1.8F);
		player.sendOverlayMessage(kumpels.isEmpty()
				? Component.translatable("message.kumpel.whistle.nobody")
				: Component.translatable("message.kumpel.whistle.called", kumpels.size()));

		if (kumpels.size() >= FULL_CREW) {
			KumpelAdvancements.award(player, KumpelAdvancements.FULL_CREW);
		}

		return kumpels.size();
	}

	/** Tells every Kumpel in hearing range to sit down and wait. */
	public static int sendOnBreak(ServerLevel level, Player player) {
		List<KumpelEntity> kumpels = KumpelEntity.findOwnedBy(level, player, range());
		for (KumpelEntity kumpel : kumpels) {
			kumpel.takeBreak();
		}

		whistle(level, player, 1.2F);
		player.sendOverlayMessage(kumpels.isEmpty()
				? Component.translatable("message.kumpel.whistle.nobody")
				: Component.translatable("message.kumpel.whistle.break", kumpels.size()));
		return kumpels.size();
	}

	/**
	 * Makes a container the storage of every Kumpel in hearing range. If they all use it already, they stop using it.
	 */
	public static void markStorage(ServerLevel level, Player player, BlockPos pos) {
		if (!KumpelSettings.get().behaviour().storageChests) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.storage.disabled"));
			return;
		}

		if (ItemStorage.SIDED.find(level, pos, null) == null) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.storage.not_a_container"));
			return;
		}

		List<KumpelEntity> kumpels = KumpelEntity.findOwnedBy(level, player, range());
		if (kumpels.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.whistle.nobody"));
			return;
		}

		boolean alreadyMarked = kumpels.stream().allMatch(kumpel -> kumpel.hasStorageAt(level, pos));
		for (KumpelEntity kumpel : kumpels) {
			if (alreadyMarked) {
				kumpel.clearStorage();
			} else {
				kumpel.setStorage(level, pos);
			}
		}

		whistle(level, player, alreadyMarked ? 1.0F : 2.0F);
		level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.0);
		player.sendOverlayMessage(Component.translatable(alreadyMarked ? "message.kumpel.storage.cleared" : "message.kumpel.storage.set",
				kumpels.size(), level.getBlockState(pos).getBlock().getName()));

		if (!alreadyMarked) {
			KumpelAdvancements.award(player, KumpelAdvancements.STORAGE);
		}
	}

	private static double range() {
		return Math.max(1, KumpelSettings.get().behaviour().whistleRange);
	}

	private static void whistle(ServerLevel level, Player player, float pitch) {
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 2.0F, pitch);
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.call").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.break").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.storage").withStyle(ChatFormatting.GRAY));
	}
}
