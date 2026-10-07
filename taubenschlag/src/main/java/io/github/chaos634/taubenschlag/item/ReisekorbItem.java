package io.github.chaos634.taubenschlag.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.taubenschlag.block.TaubenschlagBlock;
import io.github.chaos634.taubenschlag.entity.BrieftaubeData;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagComponents;

/**
 * Reisekorb: the wicker basket pigeons travel in. Use it on your tame pigeon to put it in, take it wherever you like,
 * then use the basket on the ground to let it out, or up in the air to let it fly straight home, carrying whatever is
 * in your other hand. Sneak and use it on a loft to settle the pigeon in there.
 */
public class ReisekorbItem extends Item {
	public ReisekorbItem(Properties properties) {
		super(properties);
	}

	public static boolean hasPigeon(ItemStack basket) {
		return basket.has(TaubenschlagComponents.BRIEFTAUBE);
	}

	/** Puts the player's own tame pigeon into the basket. */
	public static InteractionResult catchPigeon(ItemStack basket, Player player, BrieftaubeEntity pigeon) {
		String problem = null;
		if (hasPigeon(basket)) {
			problem = "message.taubenschlag.basket_full";
		} else if (!pigeon.isTame() || !pigeon.isOwnedBy(player)) {
			problem = "message.taubenschlag.not_yours";
		} else if (pigeon.isLeashed()) {
			problem = "message.taubenschlag.leashed";
		} else if (pigeon.isBaby()) {
			problem = "message.taubenschlag.too_young";
		}

		if (pigeon.level() instanceof ServerLevel level) {
			if (problem != null) {
				player.sendOverlayMessage(Component.translatable(problem, pigeon.describe()));
			} else {
				BrieftaubeData data = BrieftaubeData.of(pigeon);
				basket.set(TaubenschlagComponents.BRIEFTAUBE, data);
				level.playSound(null, pigeon.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0F, 1.0F);
				player.sendOverlayMessage(Component.translatable("message.taubenschlag.basket_in", data.describe()));
				pigeon.discard();
			}
		}

		return problem == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	/**
	 * Lets the pigeon out of the basket at {@code spot}. If {@code loft} is given, the pigeon settles in there.
	 *
	 * @return the pigeon, or {@code null} if the basket was empty
	 */
	public static BrieftaubeEntity release(ServerLevel level, ItemStack basket, Vec3 spot, float yRot, BlockPos loft) {
		BrieftaubeData data = basket.get(TaubenschlagComponents.BRIEFTAUBE);
		if (data == null) {
			return null;
		}

		BrieftaubeEntity pigeon = data.restore(level, spot, yRot, EntitySpawnReason.BUCKET);
		if (pigeon != null) {
			basket.remove(TaubenschlagComponents.BRIEFTAUBE);
			level.playSound(null, BlockPos.containing(spot), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (loft != null) {
				pigeon.settleInto(level, loft);
			}
		}
		return pigeon;
	}

	/** On the ground: let the pigeon out there. Sneaking, on a loft: it settles in there. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack basket = context.getItemInHand();
		if (!hasPigeon(basket)) {
			return InteractionResult.PASS;
		}

		if (context.getLevel() instanceof ServerLevel level) {
			BlockPos clicked = context.getClickedPos();
			BlockState state = level.getBlockState(clicked);
			boolean loft = state.is(TaubenschlagBlocks.TAUBENSCHLAG);
			BlockPos spot = loft ? clicked.relative(state.getValue(TaubenschlagBlock.FACING)) : clicked.relative(context.getClickedFace());
			if (!level.getBlockState(spot).getCollisionShape(level, spot).isEmpty()) {
				spot = clicked.above();
			}
			float yRot = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
			BrieftaubeEntity pigeon = release(level, basket, Vec3.atBottomCenterOf(spot), yRot, loft ? clicked : null);
			if (pigeon != null && context.getPlayer() != null && !loft) {
				context.getPlayer().sendOverlayMessage(Component.translatable("message.taubenschlag.released", pigeon.describe()));
			}
		}

		return InteractionResult.SUCCESS;
	}

	/** Up in the air: Auflassen. The pigeon flies home at once, carrying what is in the other hand. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack basket = player.getItemInHand(hand);
		if (!hasPigeon(basket)) {
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel serverLevel) {
			Vec3 spot = player.getEyePosition().add(player.getLookAngle());
			BrieftaubeEntity pigeon = release(serverLevel, basket, spot, player.getYRot(), null);
			if (pigeon != null) {
				InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
				ItemStack post = player.getItemInHand(otherHand);
				if (pigeon.takeOff(serverLevel, player, post) && !post.isEmpty()) {
					player.setItemInHand(otherHand, ItemStack.EMPTY);
				}
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		BrieftaubeData data = stack.get(TaubenschlagComponents.BRIEFTAUBE);
		if (data == null) {
			tooltip.accept(Component.translatable("item.taubenschlag.reisekorb.empty").withStyle(ChatFormatting.GRAY));
			return;
		}

		tooltip.accept(data.describe().copy().withStyle(ChatFormatting.WHITE));
		if (data.name().isPresent() && !data.ring().isEmpty()) {
			tooltip.accept(Component.translatable("item.taubenschlag.reisekorb.ring", data.ring()).withStyle(ChatFormatting.GRAY));
		}
		tooltip.accept(Component.translatable("item.taubenschlag.reisekorb.plumage",
				Component.translatable("entity.taubenschlag.brieftaube.plumage." + data.plumage().id())).withStyle(ChatFormatting.GRAY));
		tooltip.accept(data.home()
				.map(home -> Component.translatable("item.taubenschlag.reisekorb.home", home.pos().getX(), home.pos().getY(), home.pos().getZ()))
				.orElseGet(() -> Component.translatable("item.taubenschlag.reisekorb.no_home"))
				.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.taubenschlag.reisekorb.flights", data.flights()).withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.taubenschlag.reisekorb.hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
