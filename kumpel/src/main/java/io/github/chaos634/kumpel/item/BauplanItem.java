package io.github.chaos634.kumpel.item;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.build.Bauplan;
import io.github.chaos634.kumpel.build.BuildOrder;
import io.github.chaos634.kumpel.entity.KumpelEntity;
import io.github.chaos634.kumpel.registry.ModComponents;

/**
 * Bauplan: right-click to choose a building, use it on the ground and your nearest Kumpel builds it there, from the
 * blocks in its Kiepe.
 */
public class BauplanItem extends Item {
	private static final double ORDER_RANGE = 24.0;

	public BauplanItem(Properties properties) {
		super(properties);
	}

	/** All plans the server knows, in a fixed order. */
	public static List<ResourceKey<Bauplan>> plans(Level level) {
		return level.registryAccess().lookupOrThrow(Bauplan.REGISTRY).listElementIds()
				.sorted(Comparator.comparing(key -> key.identifier().toString()))
				.toList();
	}

	public static Component planName(ResourceKey<Bauplan> key) {
		return Component.translatable(Bauplan.translationKey(key));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel) {
			List<ResourceKey<Bauplan>> plans = plans(level);
			if (plans.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.kumpel.bauplan.none"));
				return InteractionResult.SUCCESS;
			}
			ItemStack stack = player.getItemInHand(hand);
			int next = (plans.indexOf(stack.get(ModComponents.BAUPLAN)) + 1) % plans.size();
			stack.set(ModComponents.BAUPLAN, plans.get(next));
			player.sendOverlayMessage(Component.translatable("message.kumpel.bauplan.chosen", planName(plans.get(next))));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		ResourceKey<Bauplan> plan = context.getItemInHand().get(ModComponents.BAUPLAN);
		if (player == null || plan == null) {
			return InteractionResult.PASS;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}

		BlockPos origin = context.getClickedPos().above();
		KumpelEntity builder = KumpelEntity.findOwnedBy(level, player, ORDER_RANGE).stream()
				.filter(KumpelEntity::canTakeBuildOrder)
				.min(Comparator.comparingDouble(kumpel -> kumpel.distanceToSqr(Vec3.atCenterOf(origin))))
				.orElse(null);
		if (builder == null) {
			player.sendOverlayMessage(Component.translatable("message.kumpel.build.nobody"));
			return InteractionResult.SUCCESS;
		}

		builder.startBuilding(new BuildOrder(plan, origin, BuildOrder.rotationFor(player.getDirection()), 0));
		player.sendSystemMessage(Component.translatable("message.kumpel.build.start", builder.getDisplayName(), planName(plan))
				.withStyle(ChatFormatting.GOLD));
		return InteractionResult.SUCCESS;
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		ResourceKey<Bauplan> plan = stack.get(ModComponents.BAUPLAN);
		if (plan != null) {
			tooltip.accept(Component.translatable("item.kumpel.bauplan.plan", planName(plan)).withStyle(ChatFormatting.BLUE));
		}
		tooltip.accept(Component.translatable(plan != null ? "item.kumpel.bauplan.tooltip" : "item.kumpel.bauplan.empty").withStyle(ChatFormatting.GRAY));
	}
}
