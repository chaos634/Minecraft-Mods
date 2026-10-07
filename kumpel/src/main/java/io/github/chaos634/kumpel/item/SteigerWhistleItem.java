package io.github.chaos634.kumpel.item;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import io.github.chaos634.kumpel.entity.ShaftOrder;
import io.github.chaos634.kumpel.registry.ModSounds;

/**
 * Steigerpfeife, the foreman's whistle. Calls all your Kumpels or sends them on a break. Used sneaking on a container,
 * it marks the chest they bring their loot to; on the side of a block, it orders a tunnel.
 */
public class SteigerWhistleItem extends Item {
	private static final int COOLDOWN_TICKS = 20;
	private static final int FULL_CREW = 5;
	private static final double TUNNEL_ORDER_RANGE = 16.0;
	/** Looking down at least this steeply orders a shaft instead of a break. */
	private static final float SHAFT_PITCH = 60.0F;

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

		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		// Both sides must agree on what the click means, so only look at things the client knows too:
		// a container becomes the storage, the side of any other block is where a tunnel starts,
		// and everything else (like the ground) falls through to sending everyone on a break.
		boolean container = level.getBlockEntity(pos) != null;
		boolean wall = context.getClickedFace().getAxis().isHorizontal() && !level.getBlockState(pos).isAir();
		// Looking straight down at the ground: a shaft goes down right there.
		boolean shaft = context.getClickedFace() == Direction.UP && player.getXRot() >= SHAFT_PITCH && !level.getBlockState(pos).isAir();
		if (!container && !wall && !shaft) {
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel serverLevel) {
			if (container) {
				markStorage(serverLevel, player, pos);
			} else if (shaft) {
				orderShaft(serverLevel, player, pos);
			} else {
				orderTunnel(serverLevel, player, pos, context.getClickedFace());
			}
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

		whistle(level, player, ModSounds.WHISTLE_CALL);
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

		whistle(level, player, ModSounds.WHISTLE_BREAK);
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

		whistle(level, player, ModSounds.WHISTLE_ORDER);
		level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.0);
		player.sendOverlayMessage(Component.translatable(alreadyMarked ? "message.kumpel.storage.cleared" : "message.kumpel.storage.set",
				kumpels.size(), level.getBlockState(pos).getBlock().getName()));

		if (!alreadyMarked) {
			KumpelAdvancements.award(player, KumpelAdvancements.STORAGE);
		}
	}

	/**
	 * Vortrieb: the nearest Kumpel with a pickaxe digs a tunnel into the clicked wall. The tunnel starts at the height
	 * of the player's feet if they clicked the block in front of their feet or head.
	 */
	/** The Hauer nearest to the player, or {@code null}. */
	private static KumpelEntity nearestHauer(ServerLevel level, Player player) {
		KumpelEntity hauer = null;
		double nearest = Double.MAX_VALUE;
		for (KumpelEntity kumpel : KumpelEntity.findOwnedBy(level, player, TUNNEL_ORDER_RANGE)) {
			double distance = kumpel.distanceToSqr(player);
			if (kumpel.isHauer() && distance < nearest) {
				hauer = kumpel;
				nearest = distance;
			}
		}
		return hauer;
	}

	/** Abteufen: the nearest Hauer sinks a shaft from the clicked block down, ladders on the far wall. */
	public static void orderShaft(ServerLevel level, Player player, BlockPos clicked) {
		KumpelEntity hauer = nearestHauer(level, player);
		if (hauer == null) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.tunnel.no_hauer"));
			return;
		}
		if (!KumpelSettings.get().behaviour().shafts || !hauer.canDigHere()) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.shaft.disabled"));
			return;
		}

		int depth = Math.clamp(KumpelSettings.get().behaviour().shaftDepth, 2, 64);
		hauer.startShaft(new ShaftOrder(clicked.immutable(), player.getDirection(), depth, 0));
		whistle(level, player, ModSounds.WHISTLE_ORDER);
		level.sendParticles(ParticleTypes.WAX_OFF, clicked.getX() + 0.5, clicked.getY() + 1.1, clicked.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
		player.sendOverlayMessage(Component.translatable("message.kumpel.shaft.started", hauer.getDisplayName(), depth));
	}

	public static void orderTunnel(ServerLevel level, Player player, BlockPos clicked, Direction face) {
		KumpelEntity hauer = nearestHauer(level, player);
		if (hauer == null) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.tunnel.no_hauer"));
			return;
		}
		if (!hauer.canDigHere()) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.tunnel.disabled"));
			return;
		}

		int feetY = player.getBlockY();
		int floorY = clicked.getY() == feetY + 1 ? feetY : clicked.getY();
		BlockPos start = new BlockPos(clicked.getX(), floorY, clicked.getZ());
		Direction direction = face.getOpposite();
		int length = KumpelSettings.get().behaviour().tunnelLength;

		hauer.startTunnel(start, direction, length);
		whistle(level, player, ModSounds.WHISTLE_ORDER);
		level.sendParticles(ParticleTypes.WAX_OFF, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.0);
		player.sendOverlayMessage(Component.translatable("message.kumpel.tunnel.started", hauer.getDisplayName(), length,
				Component.translatable("direction.kumpel." + direction.getSerializedName())));
	}

	private static double range() {
		return Math.max(1, KumpelSettings.get().behaviour().whistleRange);
	}

	private static void whistle(ServerLevel level, Player player, SoundEvent sound) {
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, 2.0F, 1.0F);
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.call").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.break").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.storage").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.tunnel").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.kumpel.steiger_whistle.shaft").withStyle(ChatFormatting.GRAY));
	}
}
