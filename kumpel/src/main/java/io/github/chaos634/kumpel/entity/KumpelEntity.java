package io.github.chaos634.kumpel.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import io.github.chaos634.kumpel.advancement.KumpelAdvancements;
import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;
import io.github.chaos634.kumpel.entity.ai.CollectItemsGoal;
import io.github.chaos634.kumpel.entity.ai.DeliverItemsGoal;
import io.github.chaos634.kumpel.entity.ai.MineOreGoal;
import io.github.chaos634.kumpel.entity.behaviour.BarbaraDay;
import io.github.chaos634.kumpel.entity.behaviour.DangerSense;
import io.github.chaos634.kumpel.entity.behaviour.MinerLamp;
import io.github.chaos634.kumpel.entity.behaviour.OreGlimmer;
import io.github.chaos634.kumpel.entity.behaviour.PackedLunch;
import io.github.chaos634.kumpel.entity.behaviour.ShiftEnd;
import io.github.chaos634.kumpel.entity.behaviour.Steigerlied;
import io.github.chaos634.kumpel.item.KumpelSoul;
import io.github.chaos634.kumpel.registry.ModComponents;
import io.github.chaos634.kumpel.registry.ModItems;

/**
 * The Kumpel: a small mining golem that follows its owner, collects dropped items,
 * senses nearby ores and grows stronger as it gains experience. Its levels, ores and food come from the config.
 */
public class KumpelEntity extends TamableAnimal implements InventoryCarrier {
	private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> DATA_TEXTURE = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> DATA_POINTING = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_MINING = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.BOOLEAN);

	private static final double MAX_SENSE_DISTANCE_FROM_OWNER_SQ = 24.0 * 24.0;
	private static final double MAX_MINE_DISTANCE_FROM_OWNER_SQ = 16.0 * 16.0;
	private static final double CALL_TELEPORT_DISTANCE_SQ = 12.0 * 12.0;
	private static final int STORAGE_RETRY_TICKS = 600;
	private static final int POINTING_TICKS = 50;
	private static final String[] COMPASS_DIRECTIONS = {
			"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"
	};
	private static final MenuType<?>[] POCKET_MENUS = {
			MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
			MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6
	};

	private final KumpelPockets pockets = new KumpelPockets(this);
	private final Set<Integer> ignoredItems = new HashSet<>();
	private final DangerSense dangerSense = new DangerSense();
	private final PackedLunch packedLunch = new PackedLunch();
	private final ShiftEnd shiftEnd = new ShiftEnd();
	private int experience;
	private boolean oreSensing = true;
	private int settingsRevision = -1;
	private boolean healOnFirstRefresh = true;
	private int ticksSinceLastPickup;
	private int senseCooldown;
	private int pointingTicks;
	private Vec3 pointingTarget;
	private OreRule lastAnnouncedOre;
	private BlockPos lastAnnouncedPos;
	private int lastAnnounceTick = Integer.MIN_VALUE / 2;
	private GlobalPos storage;
	private int storageBlockedTicks;
	private long lastBarbaraGreeting = Long.MIN_VALUE;

	public KumpelEntity(EntityType<? extends KumpelEntity> type, Level level) {
		super(type, level);
		// The pickaxe is dropped in full by dropEquipment, never by chance.
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		KumpelTier first = KumpelSettings.get().firstTier();
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, first.maxHealth())
				.add(Attributes.MOVEMENT_SPEED, first.movementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(2, new DeliverItemsGoal(this, 1.15));
		this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 10.0F, 3.0F));
		this.goalSelector.addGoal(4, new CollectItemsGoal(this, 1.15));
		this.goalSelector.addGoal(5, new MineOreGoal(this, 1.1));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		KumpelTier first = KumpelSettings.get().firstTier();
		builder.define(DATA_LEVEL, first.level());
		builder.define(DATA_TEXTURE, first.texture().toString());
		builder.define(DATA_POINTING, false);
		builder.define(DATA_MINING, false);
		builder.define(DATA_DANCING, false);
	}

	private static KumpelConfig.Behaviour behaviour() {
		return KumpelSettings.get().behaviour();
	}

	// ------------------------------------------------------------------
	// Levels & experience

	public KumpelTier getTier() {
		return KumpelSettings.get().tier(this.entityData.get(DATA_LEVEL));
	}

	/** The texture of the current level, as sent by the server (so it matches the server's config). */
	public Identifier getTexture() {
		Identifier texture = Identifier.tryParse(this.entityData.get(DATA_TEXTURE));
		return texture != null ? texture : getTier().texture();
	}

	public int getExperience() {
		return experience;
	}

	public boolean isPointing() {
		return this.entityData.get(DATA_POINTING);
	}

	public boolean isOreSensing() {
		return oreSensing;
	}

	/** Swinging its pickaxe at an ore right now. */
	public boolean isMining() {
		return this.entityData.get(DATA_MINING);
	}

	public void setMining(boolean mining) {
		this.entityData.set(DATA_MINING, mining);
	}

	/** A jukebox plays nearby. */
	public boolean isDancing() {
		return this.entityData.get(DATA_DANCING);
	}

	/** Takes over the experience and settings stored in a Kumpel Core. */
	public void loadSoul(KumpelSoul soul) {
		experience = soul.experience();
		oreSensing = soul.oreSensing();
		applyTier(KumpelSettings.get().tierForExperience(experience), true);
	}

	/** This Kumpel's experience and settings, keeping the given share of its experience. */
	public KumpelSoul createSoul(double experienceShare) {
		return new KumpelSoul((int) Math.floor(experience * Math.clamp(experienceShare, 0.0, 1.0)), oreSensing);
	}

	public ItemStack createCoreStack(net.minecraft.world.item.Item item, double experienceShare) {
		ItemStack stack = new ItemStack(item);
		stack.set(ModComponents.SOUL, createSoul(experienceShare));
		if (hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, getCustomName());
		}

		return stack;
	}

	public void addExperience(int amount) {
		if (amount <= 0) {
			return;
		}

		int before = this.entityData.get(DATA_LEVEL);
		experience += amount;
		KumpelTier after = KumpelSettings.get().tierForExperience(experience);

		if (after.level() != before) {
			applyTier(after, after.level() > before);
			if (after.level() > before) {
				celebrateLevelUp(after);
				if (after.level() == KumpelSettings.get().maxTier().level()) {
					KumpelAdvancements.award(getOwner(), KumpelAdvancements.MAX_LEVEL);
				}
			}
		}
	}

	/** Recalculates the level from the experience, e.g. after the config changed. */
	private void refreshTier() {
		KumpelTier tier = KumpelSettings.get().tierForExperience(experience);
		applyTier(tier, healOnFirstRefresh);
		healOnFirstRefresh = false;
	}

	private void applyTier(KumpelTier tier, boolean healToFull) {
		this.entityData.set(DATA_LEVEL, tier.level());
		this.entityData.set(DATA_TEXTURE, tier.texture().toString());

		AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.setBaseValue(tier.maxHealth());
		}

		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(tier.movementSpeed());
		}

		if (healToFull) {
			setHealth(getMaxHealth());
		} else if (getHealth() > getMaxHealth()) {
			setHealth(getMaxHealth());
		}
	}

	private void celebrateLevelUp(KumpelTier tier) {
		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.6, getZ(), 40, 0.4, 0.5, 0.4, 0.3);
		}

		playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.3F);

		if (getOwner() instanceof Player owner) {
			owner.sendSystemMessage(Component.translatable("message.kumpel.level_up", getDisplayName(), tier.level(), tier.displayName())
					.withStyle(ChatFormatting.GOLD));
		}
	}

	private Component experienceProgress() {
		KumpelSettings settings = KumpelSettings.get();
		KumpelTier next = settings.nextTier(getTier());
		return next == null
				? Component.translatable("message.kumpel.max_level")
				: Component.translatable("message.kumpel.xp_progress", experience, next.requiredExperience());
	}

	/** One line describing the Kumpel, used as the title of its backpack. */
	public Component statusLine() {
		KumpelTier tier = getTier();
		return Component.translatable("message.kumpel.status",
				getDisplayName(),
				tier.level(),
				tier.displayName(),
				experienceProgress(),
				(int) Math.ceil(getHealth()),
				(int) getMaxHealth());
	}

	// ------------------------------------------------------------------
	// Interaction

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		KumpelSettings settings = KumpelSettings.get();

		if (!isTame()) {
			if (!settings.isRepairItem(stack)) {
				return super.mobInteract(player, hand);
			}

			if (!level().isClientSide()) {
				usePlayerItem(player, hand, stack);
				tame(player);
				setOrderedToSit(false);
				level().broadcastEntityEvent(this, (byte) 7);
				KumpelAdvancements.award(player, KumpelAdvancements.GLUECK_AUF);
				player.sendOverlayMessage(Component.translatable("message.kumpel.tamed", getDisplayName()));
			}

			return InteractionResult.SUCCESS;
		}

		boolean packing = player.isSecondaryUseActive() && stack.is(ModItems.KUMPEL_CORE) && !stack.has(ModComponents.SOUL);
		boolean handled = stack.isEmpty()
				|| packing
				|| stack.is(Items.COMPASS)
				|| stack.is(ItemTags.PICKAXES)
				|| KumpelPockets.isTorch(stack)
				|| settings.isRepairItem(stack)
				|| settings.feedExperience(stack) > 0;

		if (!isOwnedBy(player) || !handled) {
			return super.mobInteract(player, hand);
		}

		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (packing) {
			packInto(player, stack);
		} else if (stack.is(ItemTags.PICKAXES)) {
			// Swap pickaxes: the Kumpel takes yours and hands back the one it had, if any.
			ItemStack previous = getMainHandItem();
			setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
			player.setItemInHand(hand, previous);
			playSound(SoundEvents.COPPER_GOLEM_ITEM_GET, 1.0F, 1.0F);
			player.sendOverlayMessage(Component.translatable(canMineAtAll() ? "message.kumpel.hauer" : "message.kumpel.hauer.disabled", getDisplayName()));
		} else if (KumpelPockets.isTorch(stack)) {
			ItemStack rest = pockets.addToPockets(stack);
			int stored = stack.getCount() - rest.getCount();
			if (stored > 0) {
				player.setItemInHand(hand, rest);
				playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
			}
			player.sendOverlayMessage(Component.translatable("message.kumpel.torches", getDisplayName(), pockets.count(KumpelPockets::isTorch)));
		} else if (stack.isEmpty()) {
			if (player.isSecondaryUseActive()) {
				openPockets(player);
			} else {
				boolean sit = !isOrderedToSit();
				setOrderedToSit(sit);
				this.jumping = false;
				getNavigation().stop();
				setTarget(null);
				player.sendOverlayMessage(Component.translatable(sit ? "message.kumpel.sitting" : "message.kumpel.following", getDisplayName()));
			}
		} else if (stack.is(Items.COMPASS)) {
			oreSensing = !oreSensing;
			player.sendOverlayMessage(Component.translatable(oreSensing ? "message.kumpel.ore_sense_on" : "message.kumpel.ore_sense_off", getDisplayName()));
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, oreSensing ? 1.4F : 0.6F);
		} else if (settings.isRepairItem(stack) && getHealth() < getMaxHealth()) {
			usePlayerItem(player, hand, stack);
			heal(behaviour().repairAmount);
			playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.3F + (random.nextFloat() - random.nextFloat()) * 0.2F);
		} else {
			int amount = settings.feedExperience(stack);
			if (amount <= 0) {
				return InteractionResult.PASS;
			}
			if (BarbaraDay.isToday()) {
				amount *= 2;
			}

			usePlayerItem(player, hand, stack);
			addExperience(amount);
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F + random.nextFloat() * 0.6F);

			if (level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.3, 0.3, 0.0);
			}

			player.sendOverlayMessage(Component.translatable("message.kumpel.fed", getDisplayName(), amount, experienceProgress()));
		}

		return InteractionResult.SUCCESS;
	}

	/** Puts the Kumpel back into an empty core (keeping all its experience) and hands over everything it carries. */
	public void packInto(Player player, ItemStack emptyCore) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		for (ItemStack carried : pockets.removeAllItems()) {
			giveOrDrop(serverLevel, player, carried);
		}
		giveOrDrop(serverLevel, player, getMainHandItem());
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

		ItemStack core = createCoreStack(ModItems.KUMPEL_CORE, 1.0);
		emptyCore.consume(1, player);
		giveOrDrop(serverLevel, player, core);

		serverLevel.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.5, 0.3, 0.0);
		serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.COPPER_GOLEM_BECOME_STATUE, SoundSource.NEUTRAL, 1.0F, 1.2F);
		player.sendOverlayMessage(Component.translatable("message.kumpel.packed", getDisplayName()));
		discard();
	}

	private static void giveOrDrop(ServerLevel level, Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
			drop.setThrower(player);
			level.addFreshEntity(drop);
		}
	}

	/** Opens the Kumpel's backpack; its size depends on the Kumpel's level. */
	public void openPockets(Player player) {
		// The pickaxe goes into the backpack while it is open, so you can take it back. The Kumpel picks it up again afterwards.
		stashTool();

		int rows = getTier().pocketRows();
		MenuType<?> menuType = POCKET_MENUS[rows - 1];
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, user) -> new ChestMenu(menuType, containerId, inventory, pockets, rows),
				statusLine()));
		playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------
	// Steigerpfeife

	/** All living Kumpels of this owner within the given distance. */
	public static List<KumpelEntity> findOwnedBy(ServerLevel level, Player owner, double range) {
		return level.getEntitiesOfClass(KumpelEntity.class, owner.getBoundingBox().inflate(range),
				kumpel -> kumpel.isAlive() && kumpel.isOwnedBy(owner));
	}

	/** Called by the whistle: stand up and come to the owner. */
	public void answerWhistle(Player owner) {
		shiftEnd.setResting(false);
		setOrderedToSit(false);
		setInSittingPose(false);
		setTarget(null);

		if (distanceToSqr(owner) > CALL_TELEPORT_DISTANCE_SQ) {
			tryToTeleportToOwner();
		} else {
			getNavigation().moveTo(owner, 1.2);
		}

		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.3, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Called by the whistle when sneaking: sit down and wait. */
	public void takeBreak() {
		shiftEnd.setResting(false);
		setOrderedToSit(true);
		getNavigation().stop();
		setTarget(null);
	}

	// ------------------------------------------------------------------
	// Lagerkiste

	public void setStorage(ServerLevel level, BlockPos pos) {
		storage = GlobalPos.of(level.dimension(), pos.immutable());
		storageBlockedTicks = 0;
	}

	public void clearStorage() {
		storage = null;
		storageBlockedTicks = 0;
	}

	public boolean hasStorageAt(ServerLevel level, BlockPos pos) {
		return storage != null && storage.dimension() == level.dimension() && storage.pos().equals(pos);
	}

	public GlobalPos getStorage() {
		return storage;
	}

	/** Where the Kumpel should bring its loot right now, or {@code null} to bring it to the owner. */
	public BlockPos usableStorage() {
		if (storage == null || storageBlockedTicks > 0 || !behaviour().storageChests || storage.dimension() != level().dimension()) {
			return null;
		}

		double maxDistance = behaviour().maxStorageDistance;
		return distanceToSqr(Vec3.atCenterOf(storage.pos())) <= maxDistance * maxDistance ? storage.pos() : null;
	}

	/** The storage can't be reached at the moment: deliver to the owner for a while. */
	public void storageUnreachable() {
		storageBlockedTicks = STORAGE_RETRY_TICKS;
	}

	/** Puts the loot into the storage container. Whatever doesn't fit stays in the backpack. */
	public void deliverItemsToStorage(ServerLevel level) {
		if (storage == null) {
			return;
		}

		Storage<ItemVariant> target = ItemStorage.SIDED.find(level, storage.pos(), (Direction) null);
		if (target == null) {
			// The chest is gone.
			if (getOwner() instanceof Player owner) {
				owner.sendOverlayMessage(Component.translatable("message.kumpel.storage.gone", getDisplayName()));
			}
			clearStorage();
			return;
		}

		List<ItemStack> loot = pockets.takeLoot();
		long delivered = 0;
		try (Transaction transaction = Transaction.openOuter()) {
			for (ItemStack stack : loot) {
				long inserted = target.insert(ItemVariant.of(stack), stack.getCount(), transaction);
				stack.shrink((int) inserted);
				delivered += inserted;
			}
			transaction.commit();
		}

		for (ItemStack rest : loot) {
			if (!rest.isEmpty()) {
				ItemStack left = pockets.addToPockets(rest);
				if (!left.isEmpty()) {
					spawnAtLocation(level, left);
				}
			}
		}

		ticksSinceLastPickup = 0;
		if (delivered > 0) {
			Vec3 center = Vec3.atCenterOf(storage.pos());
			playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.9F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 0.6, center.z, 4, 0.3, 0.2, 0.3, 0.0);
		}

		if (pockets.hasLoot()) {
			// Full: the rest goes to the owner, and the Kumpel tries the chest again later.
			storageBlockedTicks = STORAGE_RETRY_TICKS;
			if (getOwner() instanceof Player owner) {
				owner.sendOverlayMessage(Component.translatable("message.kumpel.storage.full", getDisplayName()));
			}
		}
	}

	// ------------------------------------------------------------------
	// Hauer

	public boolean isHauer() {
		return getMainHandItem().is(ItemTags.PICKAXES);
	}

	/** Whether mining is allowed at all here (config and game rule). */
	private boolean canMineAtAll() {
		return behaviour().mineOres && level() instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	/** Whether the Kumpel should look for ores to mine right now. */
	public boolean canMine() {
		return isTame()
				&& !isOrderedToSit()
				&& isHauer()
				&& !pockets.pocketsFull()
				&& canMineAtAll()
				&& getOwner() instanceof Player owner
				&& owner.level() == level()
				&& distanceToSqr(owner) < MAX_MINE_DISTANCE_FROM_OWNER_SQ;
	}

	/** An ore this Kumpel can sense, that its pickaxe can harvest, and that is exposed to air. */
	public boolean isMineableOre(BlockPos pos) {
		BlockState state = level().getBlockState(pos);
		if (state.isAir()) {
			return false;
		}

		OreRule rule = KumpelSettings.get().matchOre(state);
		if (rule == null || rule.level() > getTier().level() || !getMainHandItem().isCorrectToolForDrops(state)) {
			return false;
		}

		for (Direction direction : Direction.values()) {
			if (level().getBlockState(pos.relative(direction)).isAir()) {
				return true;
			}
		}

		return false;
	}

	/** The nearest ore the Kumpel could mine, or {@code null}. */
	public BlockPos findOreToMine(Predicate<BlockPos> skip) {
		if (!(getOwner() instanceof Player owner)) {
			return null;
		}

		int radius = Math.max(1, behaviour().mineRadius);
		double maxFromOwner = behaviour().maxCollectDistanceFromOwner;
		BlockPos center = blockPosition();
		BlockPos ownerFloor = owner.blockPosition().below();
		BlockPos ownFloor = blockPosition().below();

		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			double distance = pos.distSqr(center);
			if (distance > radius * radius || distance >= bestDistance) {
				continue;
			}
			if (pos.equals(ownerFloor) || pos.equals(ownFloor) || skip.test(pos)) {
				continue;
			}
			if (owner.distanceToSqr(Vec3.atCenterOf(pos)) > maxFromOwner * maxFromOwner || !isMineableOre(pos)) {
				continue;
			}

			best = pos.immutable();
			bestDistance = distance;
		}

		return best;
	}

	/** How long mining this block takes, like a player with the same pickaxe would need. */
	public int miningTicks(BlockPos pos) {
		BlockState state = level().getBlockState(pos);
		float hardness = state.getDestroySpeed(level(), pos);
		float speed = Math.max(1.0F, getMainHandItem().getDestroySpeed(state));
		return Mth.clamp(Mth.ceil(hardness * 30.0F / speed), 4, 200);
	}

	/** Breaks the ore with the pickaxe: drops as if a player mined it (enchantments included) and wears down the pickaxe. */
	public void mineOre(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		OreRule rule = KumpelSettings.get().matchOre(state);
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		ItemStack tool = getMainHandItem();

		level.destroyBlock(pos, false, this);
		Block.dropResources(state, level, pos, blockEntity, this, tool);
		tool.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);

		addExperience(behaviour().experiencePerOreMined + (rule != null ? rule.level() : 0));
		KumpelAdvancements.award(getOwner(), KumpelAdvancements.HAUER);
	}

	/** Puts the pickaxe from the Kumpel's hand into its backpack, if there is room. */
	public void stashTool() {
		ItemStack tool = getMainHandItem();
		if (!tool.isEmpty() && pockets.canAddToPockets(tool)) {
			pockets.addToPockets(tool);
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
	}

	/** Takes a pickaxe out of the backpack into the hand, unless someone is looking into the backpack. */
	private void equipToolFromPockets(ServerLevel level) {
		if (!getMainHandItem().isEmpty() || isPocketsOpen(level)) {
			return;
		}

		ItemStack pickaxe = pockets.takeFirst(KumpelPockets::isPickaxe);
		if (!pickaxe.isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
		}
	}

	private boolean isPocketsOpen(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (player.containerMenu instanceof ChestMenu menu && menu.getContainer() == pockets) {
				return true;
			}
		}

		return false;
	}

	// ------------------------------------------------------------------
	// Collecting & delivering items

	@Override
	public SimpleContainer getInventory() {
		return pockets;
	}

	public KumpelPockets getPockets() {
		return pockets;
	}

	public boolean isInventoryFull() {
		return pockets.pocketsFull();
	}

	public boolean hasItemsToDeliver() {
		return pockets.hasLoot();
	}

	public boolean wantsToDeliver() {
		return hasItemsToDeliver() && (isInventoryFull() || ticksSinceLastPickup > behaviour().deliverDelayTicks);
	}

	public ItemEntity findCollectableItem() {
		LivingEntity owner = getOwner();
		double radius = getTier().collectRadius();
		List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(radius, 3.0, radius),
				item -> canCollect(item, owner));

		ItemEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (ItemEntity item : items) {
			double distance = distanceToSqr(item);
			if (distance < nearestDistance) {
				nearest = item;
				nearestDistance = distance;
			}
		}

		return nearest;
	}

	public boolean canCollect(ItemEntity item, LivingEntity owner) {
		if (!item.isAlive() || item.getItem().isEmpty() || item.hasPickUpDelay() || ignoredItems.contains(item.getId())) {
			return false;
		}

		if (owner != null) {
			// Things the owner threw away on purpose stay where they are.
			double maxDistance = behaviour().maxCollectDistanceFromOwner;
			if (item.getOwner() == owner || item.distanceToSqr(owner) > maxDistance * maxDistance) {
				return false;
			}
		}

		return pockets.canAddToPockets(item.getItem());
	}

	public void ignoreItem(ItemEntity item) {
		ignoredItems.add(item.getId());
	}

	public void collect(ItemEntity item) {
		ItemStack stack = item.getItem();
		int before = stack.getCount();
		ItemStack remainder = pockets.addToPockets(stack);
		int taken = before - remainder.getCount();

		if (taken <= 0) {
			ignoreItem(item);
			return;
		}

		take(item, taken);
		if (remainder.isEmpty()) {
			item.discard();
		} else {
			item.setItem(remainder);
		}

		playSound(SoundEvents.ITEM_PICKUP, 0.3F, (random.nextFloat() - random.nextFloat()) * 1.4F + 2.0F);
		ticksSinceLastPickup = 0;
		addExperience(behaviour().experiencePerPickup);
	}

	public void deliverItemsTo(Player player) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int delivered = 0;
		for (ItemStack stack : pockets.takeLoot()) {
			delivered += stack.getCount();
			// If the inventory is full, the rest lands at the owner's feet, marked as thrown by them so it isn't picked up again.
			giveOrDrop(serverLevel, player, stack);
		}

		ticksSinceLastPickup = 0;

		if (delivered > 0) {
			playSound(SoundEvents.ALLAY_ITEM_GIVEN, 0.8F, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.kumpel.delivered", getDisplayName(), delivered));
		}
	}

	// ------------------------------------------------------------------
	// Ticking & ore sensing

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		ticksSinceLastPickup++;

		if (settingsRevision != KumpelSettings.revision()) {
			settingsRevision = KumpelSettings.revision();
			refreshTier();
		}

		if (pointingTicks > 0) {
			getLookControl().setLookAt(pointingTarget.x, pointingTarget.y, pointingTarget.z);
			if (--pointingTicks == 0) {
				this.entityData.set(DATA_POINTING, false);
			}
		}

		if (tickCount % 600 == 0) {
			ignoredItems.clear();
		}

		if (storageBlockedTicks > 0) {
			storageBlockedTicks--;
		}

		if (tickCount % 20 == 15) {
			equipToolFromPockets(level);
			updateDancing(level);
		}

		if (isTame() && oreSensing && --senseCooldown <= 0) {
			senseCooldown = Math.max(1, behaviour().senseIntervalTicks);
			if (getOwner() instanceof Player owner && owner.level() == level && distanceToSqr(owner) < MAX_SENSE_DISTANCE_FROM_OWNER_SQ) {
				senseOres(level, owner);
			}
		}

		if (isTame() && getOwner() instanceof Player owner && owner.level() == level) {
			double distanceSq = distanceToSqr(owner);
			if (tickCount % 10 == 0 && !isOrderedToSit() && distanceSq < 24.0 * 24.0) {
				MinerLamp.tryPlaceTorch(this, level);
			}
			if (tickCount % 10 == 5 && distanceSq < 32.0 * 32.0) {
				dangerSense.tick(this, level, owner);
			}
			if (tickCount % 20 == 10 && distanceSq < 16.0 * 16.0) {
				packedLunch.tick(this, level, owner);
			}
			if (tickCount % 20 == 0) {
				shiftEnd.tick(this, owner);
			}
			if (tickCount % 100 == 50 && distanceSq < 8.0 * 8.0) {
				celebrateBarbaraDay(level, owner);
			}
		}
	}

	private void updateDancing(ServerLevel level) {
		boolean dance = behaviour().danceToJukebox
				&& !isInSittingPose()
				&& Steigerlied.findPlayingJukebox(level, blockPosition()) != null;

		if (dance != isDancing()) {
			this.entityData.set(DATA_DANCING, dance);
			if (dance && getOwner() instanceof Player owner && distanceToSqr(owner) < 16.0 * 16.0) {
				KumpelAdvancements.award(owner, KumpelAdvancements.STEIGERLIED);
			}
		}

		if (dance) {
			level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.4, getZ(), 1, 0.2, 0.0, 0.2, 1.0);
		}
	}

	/** On Barbaratag the Kumpel wishes its owner "Glück auf!" once and sheds a few blossoms now and then. */
	private void celebrateBarbaraDay(ServerLevel level, Player owner) {
		if (!BarbaraDay.isToday()) {
			return;
		}

		level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getEyeY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.0);

		long today = LocalDate.now().toEpochDay();
		if (lastBarbaraGreeting != today) {
			lastBarbaraGreeting = today;
			owner.sendSystemMessage(Component.translatable("message.kumpel.barbara", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
			KumpelAdvancements.award(owner, KumpelAdvancements.BARBARA);
		}
	}

	/** Turns the Kumpel's head and right arm towards a point for a moment. */
	public void pointAt(Vec3 target) {
		pointingTarget = target;
		pointingTicks = POINTING_TICKS;
		this.entityData.set(DATA_POINTING, true);
	}

	private void senseOres(ServerLevel level, Player owner) {
		SensedOre best = findBestOre(level);
		if (best == null) {
			return;
		}

		KumpelConfig.Behaviour behaviour = behaviour();
		int sinceLast = tickCount - lastAnnounceTick;
		if (best.pos().equals(lastAnnouncedPos) && sinceLast < behaviour.sameOreCooldownTicks) {
			return;
		}

		boolean moreValuable = lastAnnouncedOre == null || best.rule().value() > lastAnnouncedOre.value();
		if (!moreValuable && sinceLast < behaviour.announceCooldownTicks) {
			return;
		}

		announceOre(level, owner, best);
		lastAnnouncedOre = best.rule();
		lastAnnouncedPos = best.pos();
		lastAnnounceTick = tickCount;
		addExperience(1 + best.rule().level());
	}

	/**
	 * Scans the sphere around the Kumpel for the most valuable ore it can sense at its level (the nearest one, if there are several).
	 *
	 * @return the ore found, or {@code null} if there is none in range
	 */
	public SensedOre findBestOre(ServerLevel level) {
		KumpelSettings settings = KumpelSettings.get();
		KumpelTier tier = getTier();
		int radius = tier.senseRadius();
		int radiusSq = radius * radius;
		BlockPos center = blockPosition();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		OreRule bestRule = null;
		BlockPos bestPos = null;
		int bestDistanceSq = Integer.MAX_VALUE;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int distanceSq = dx * dx + dy * dy + dz * dz;
					if (distanceSq > radiusSq) {
						continue;
					}

					cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					BlockState state = level.getBlockState(cursor);
					if (state.isAir()) {
						continue;
					}

					OreRule rule = settings.matchOre(state);
					if (rule == null || rule.level() > tier.level()) {
						continue;
					}

					boolean better = bestRule == null
							|| rule.value() > bestRule.value()
							|| (rule.value() == bestRule.value() && distanceSq < bestDistanceSq);
					if (better) {
						bestRule = rule;
						bestPos = cursor.immutable();
						bestDistanceSq = distanceSq;
					}
				}
			}
		}

		return bestRule == null ? null : new SensedOre(bestRule, bestPos);
	}

	public record SensedOre(OreRule rule, BlockPos pos) {
	}

	private void announceOre(ServerLevel level, Player owner, SensedOre sensed) {
		BlockPos orePos = sensed.pos();
		Vec3 eyes = new Vec3(getX(), getEyeY(), getZ());
		Vec3 ore = Vec3.atCenterOf(orePos);
		Vec3 direction = ore.subtract(eyes);
		double distance = direction.length();
		direction = direction.normalize();

		// A short sparkling trail from the Kumpel's head towards the ore...
		int steps = (int) (Math.min(distance, 5.0) * 3);
		for (int i = 1; i <= steps; i++) {
			Vec3 point = eyes.add(direction.scale(i / 3.0));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		// ...and a glimmer on the ore itself (visible once it is uncovered).
		level.sendParticles(ParticleTypes.WAX_ON, ore.x, ore.y, ore.z, 10, 0.4, 0.4, 0.4, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, sensed.rule().pitch());

		pointAt(ore);

		BlockState state = level.getBlockState(orePos);
		int dowsingLevel = behaviour().dowsingLevel;
		if (dowsingLevel > 0 && getTier().level() >= dowsingLevel && !OreGlimmer.isGlowing(level, orePos)) {
			// Wünschelrute: experienced Kumpels make the ore glow through the rock for a moment.
			OreGlimmer.spawn(level, orePos, state, behaviour().dowsingGlowTicks);
		}

		owner.sendOverlayMessage(Component.translatable("message.kumpel.ore_sensed",
				getDisplayName(),
				state.getBlock().getName().withStyle(ChatFormatting.AQUA),
				horizontalDistance(owner, orePos),
				compassDirection(owner, orePos),
				verticalHint(owner, orePos)
		));
	}

	private static int horizontalDistance(Player owner, BlockPos pos) {
		double dx = pos.getX() + 0.5 - owner.getX();
		double dz = pos.getZ() + 0.5 - owner.getZ();
		return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
	}

	private static Component compassDirection(Player owner, BlockPos pos) {
		double dx = pos.getX() + 0.5 - owner.getX();
		double dz = pos.getZ() + 0.5 - owner.getZ();
		// 0° = north (-Z), 90° = east (+X)
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		int index = Math.floorMod((int) Math.round(angle / 45.0), 8);
		return Component.translatable("direction.kumpel." + COMPASS_DIRECTIONS[index]);
	}

	private static Component verticalHint(Player owner, BlockPos pos) {
		int dy = pos.getY() - owner.getBlockY();
		if (dy < -1) {
			return Component.translatable("message.kumpel.below", -dy);
		} else if (dy > 1) {
			return Component.translatable("message.kumpel.above", dy);
		}

		return Component.translatable("message.kumpel.same_height");
	}

	// ------------------------------------------------------------------
	// Golem traits

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof LivingEntity attacker && isOwnedBy(attacker)) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean fireImmune() {
		return getTier().fireImmune() || super.fireImmune();
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (isTame()) {
			// The core survives, cracked: it can be repaired and keeps most of the Kumpel's experience.
			spawnAtLocation(level, createCoreStack(ModItems.CRACKED_KUMPEL_CORE, behaviour().deathExperienceKept));
		}
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		ItemStack tool = getMainHandItem();
		if (!tool.isEmpty()) {
			spawnAtLocation(level, tool);
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}

		super.dropEquipment(level);
		for (ItemStack stack : pockets.removeAllItems()) {
			spawnAtLocation(level, stack);
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COPPER_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COPPER_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.COPPER_GOLEM_STEP, 0.5F, 1.2F);
	}

	// A golem is built, not born.

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	protected boolean canBeABaby() {
		return false;
	}

	// ------------------------------------------------------------------
	// Saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("experience", experience);
		output.putBoolean("ore_sensing", oreSensing);
		output.putBoolean("resting", shiftEnd.isResting());
		if (storage != null) {
			output.store("storage", GlobalPos.CODEC, storage);
		}
		output.putLong("last_barbara_greeting", lastBarbaraGreeting);
		writeInventoryToTag(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		experience = input.getIntOr("experience", 0);
		oreSensing = input.getBooleanOr("ore_sensing", true);
		shiftEnd.setResting(input.getBooleanOr("resting", false));
		storage = input.read("storage", GlobalPos.CODEC).orElse(null);
		lastBarbaraGreeting = input.getLongOr("last_barbara_greeting", Long.MIN_VALUE);
		readInventoryFromTag(input);

		healOnFirstRefresh = false;
		KumpelTier tier = KumpelSettings.get().tierForExperience(experience);
		this.entityData.set(DATA_LEVEL, tier.level());
		this.entityData.set(DATA_TEXTURE, tier.texture().toString());
	}
}
