package io.github.chaos634.kumpel.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.entity.KumpelTier;
import io.github.chaos634.kumpel.registry.ModComponents;
import io.github.chaos634.kumpel.registry.ModEntities;

/**
 * Used on a block, the core awakens a Kumpel that belongs to the player. An empty core creates a new Kumpel;
 * a core holding a soul (a packed-up or revived Kumpel) brings back that Kumpel with its name and experience.
 * Sneak-using an empty core on your Kumpel packs it into the core.
 */
public class KumpelCoreItem extends Item {
	public KumpelCoreItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}

		KumpelEntity kumpel = ModEntities.KUMPEL.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (kumpel == null) {
			return InteractionResult.FAIL;
		}

		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		float yaw = player != null ? player.getYRot() + 180.0F : 0.0F;

		kumpel.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
		kumpel.setPersistenceRequired();
		if (player != null) {
			kumpel.tame(player);
		}

		KumpelSoul soul = stack.get(ModComponents.SOUL);
		if (soul != null) {
			kumpel.loadSoul(soul);
		}

		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName != null) {
			kumpel.setCustomName(customName);
		}

		level.addFreshEntity(kumpel);
		level.sendParticles(ParticleTypes.WAX_ON, kumpel.getX(), kumpel.getY() + 0.5, kumpel.getZ(), 20, 0.4, 0.5, 0.4, 0.0);
		level.playSound(null, kumpel.getX(), kumpel.getY(), kumpel.getZ(), SoundEvents.COPPER_GOLEM_SPAWN, SoundSource.NEUTRAL, 1.0F, 1.0F);

		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		KumpelSoul soul = stack.get(ModComponents.SOUL);
		if (soul == null) {
			tooltip.accept(Component.translatable("item.kumpel.kumpel_core.tooltip").withStyle(ChatFormatting.GRAY));
			tooltip.accept(Component.translatable("item.kumpel.kumpel_core.tooltip.pack").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			appendSoulTooltip(soul, tooltip);
		}
	}

	static void appendSoulTooltip(KumpelSoul soul, Consumer<Component> tooltip) {
		KumpelTier tier = KumpelSettings.get().tierForExperience(soul.experience());
		tooltip.accept(Component.translatable("item.kumpel.soul", tier.level(), tier.displayName(), soul.experience())
				.withStyle(ChatFormatting.GOLD));
	}
}
