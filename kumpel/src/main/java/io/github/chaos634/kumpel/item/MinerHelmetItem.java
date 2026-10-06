package io.github.chaos634.kumpel.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import io.github.chaos634.kumpel.Kumpel;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * Grubenhelm: a miner's helmet with a lamp. Worn in the dark, it lets you see as if the lamp were lit.
 */
public class MinerHelmetItem extends Item {
	public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Kumpel.id("miner_helmet"));
	public static final TagKey<Item> REPAIR_ITEMS = TagKey.create(Registries.ITEM, Kumpel.id("repairs_miner_helmet"));
	public static final ArmorMaterial MATERIAL = new ArmorMaterial(15, ArmorMaterials.makeDefense(1, 3, 4, 2, 4), 12,
			SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, REPAIR_ITEMS, ASSET);

	/** Brightness (0 to 15) below which the lamp is needed. */
	private static final int DARK = 7;
	/** Night vision flickers when it has less than 200 ticks left, so keep it above that while the lamp is on. */
	private static final int LIGHT_TICKS = 300;
	private static final int REFRESH_BELOW = 240;
	private static final int CHECK_INTERVAL = 10;

	public MinerHelmetItem(Properties properties) {
		super(properties);
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(MinerHelmetItem::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_INTERVAL != 0 || !KumpelSettings.get().behaviour().minerHelmetLamp) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			updateLamp(player);
		}
	}

	/** Lights the lamp (night vision) if the player wears the helmet somewhere dark. */
	public static void updateLamp(Player player) {
		if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MINER_HELMET) || !isDark(player)) {
			return;
		}

		MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
		if (current == null || current.getDuration() < REFRESH_BELOW) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, LIGHT_TICKS, 0, true, false, false));
		}
	}

	private static boolean isDark(Player player) {
		return player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())) < DARK;
	}

	// Deprecated in 26.x but still the documented way to add a plain tooltip line.
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.kumpel.miner_helmet.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
