package io.github.chaos634.kumpel.item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.registry.ModItems;
import io.github.chaos634.kumpel.registry.ModSounds;

/**
 * Rettungskapsel, after the "Dahlbusch-Bombe" that brought trapped miners to the surface in Lengede in 1963.
 * Use it underground, hold still for three seconds, and you are pulled straight up to the surface,
 * together with your Kumpels nearby.
 */
public class RescueCapsuleItem extends Item {
	public static final int PULL_TICKS = 60;
	private static final double MAX_MOVEMENT_SQ = 0.5 * 0.5;
	private static final double CREW_RANGE = 8.0;

	/** Players waiting to be pulled up. */
	private static final Map<UUID, Pull> PULLS = new HashMap<>();

	private record Pull(ResourceKey<Level> dimension, Vec3 start, long pullAt) {
	}

	public RescueCapsuleItem(Properties properties) {
		super(properties);
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(RescueCapsuleItem::tick);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		String problem = problem(serverLevel, player);
		if (problem != null) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.capsule." + problem));
			return InteractionResult.FAIL;
		}

		PULLS.put(player.getUUID(), new Pull(level.dimension(), player.position(), level.getGameTime() + PULL_TICKS));
		player.getCooldowns().addCooldown(player.getItemInHand(hand), PULL_TICKS + 20);
		player.sendOverlayMessage(Component.translatable("message.kumpel.capsule.hold", PULL_TICKS / 20).withStyle(ChatFormatting.YELLOW));
		level.playSound(null, player.blockPosition(), ModSounds.FOERDERKORB_UP, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	/** Why the capsule can't be used here ({@code message.kumpel.capsule.<problem>}), or {@code null}. */
	public static String problem(ServerLevel level, Player player) {
		if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
			return "no_sky";
		}
		// The same column check as pullUp(), so the two always agree (sky light can lag behind).
		BlockPos pos = player.blockPosition();
		if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY()) {
			return "already_up";
		}

		return null;
	}

	private static void tick(MinecraftServer server) {
		if (PULLS.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, Pull>> iterator = PULLS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Pull> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Pull pull = entry.getValue();
			if (player == null || !player.isAlive() || player.level().dimension() != pull.dimension()) {
				iterator.remove();
				continue;
			}

			if (player.position().distanceToSqr(pull.start()) > MAX_MOVEMENT_SQ) {
				player.sendOverlayMessage(Component.translatable("message.kumpel.capsule.moved").withStyle(ChatFormatting.RED));
				iterator.remove();
				continue;
			}

			ServerLevel level = player.level();
			long left = pull.pullAt() - level.getGameTime();
			if (left > 0) {
				level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.3, 0.0, 0.3, 0.01);
				if (left % 20 == 0) {
					player.sendOverlayMessage(Component.translatable("message.kumpel.capsule.countdown", left / 20).withStyle(ChatFormatting.YELLOW));
				}
				continue;
			}

			iterator.remove();
			if (takeCapsule(player)) {
				pullUp(level, player);
			}
		}
	}

	/** Uses up one capsule from the player's inventory (nothing in creative mode). */
	private static boolean takeCapsule(Player player) {
		if (player.isCreative()) {
			return true;
		}

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(ModItems.RESCUE_CAPSULE)) {
				stack.shrink(1);
				return true;
			}
		}

		return false;
	}

	/**
	 * Pulls the player (and their Kumpels nearby) straight up to the highest block above them.
	 *
	 * @return whether there was anywhere to go
	 */
	public static boolean pullUp(ServerLevel level, Player player) {
		BlockPos pos = player.blockPosition();
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		if (surface <= pos.getY()) {
			return false;
		}

		List<KumpelEntity> crew = KumpelEntity.findOwnedBy(level, player, CREW_RANGE).stream()
				.filter(kumpel -> !kumpel.isOrderedToSit())
				.toList();
		double x = pos.getX() + 0.5;
		double z = pos.getZ() + 0.5;

		player.teleportTo(x, surface, z);
		for (KumpelEntity kumpel : crew) {
			kumpel.getNavigation().stop();
			kumpel.teleportTo(x, surface, z);
		}

		level.sendParticles(ParticleTypes.CLOUD, x, surface + 0.2, z, 20, 0.5, 0.2, 0.5, 0.02);
		level.playSound(null, BlockPos.containing(x, surface, z), ModSounds.KUMPEL_CHEER, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.sendSystemMessage(Component.translatable("message.kumpel.capsule.rescued", surface - pos.getY()).withStyle(ChatFormatting.GOLD));
		KumpelAdvancements.award(player, KumpelAdvancements.RESCUED);
		return true;
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.rescue_capsule.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
