package io.github.chaos634.kumpel.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.kumpel.entity.ai.CollectItemsGoal;
import io.github.chaos634.kumpel.entity.ai.DeliverItemsGoal;

/**
 * The Kumpel: a small mining golem that follows its owner, collects dropped items,
 * senses nearby ores and grows from copper to netherite as it gains experience.
 */
public class KumpelEntity extends TamableAnimal implements InventoryCarrier {
	private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_POINTING = SynchedEntityData.defineId(KumpelEntity.class, EntityDataSerializers.BOOLEAN);

	public static final int INVENTORY_SIZE = 9;
	/** How long the Kumpel waits after its last pickup before bringing its loot to the owner. */
	private static final int DELIVER_DELAY_TICKS = 80;
	private static final double MAX_COLLECT_DISTANCE_FROM_OWNER_SQ = 16.0 * 16.0;
	private static final double MAX_SENSE_DISTANCE_FROM_OWNER_SQ = 24.0 * 24.0;
	private static final int SENSE_INTERVAL_TICKS = 40;
	/** Minimum time between two ore announcements, unless a more valuable ore turns up. */
	private static final int ANNOUNCE_COOLDOWN_TICKS = 300;
	/** The same ore block is not announced again within this time. */
	private static final int SAME_ORE_COOLDOWN_TICKS = 2400;
	private static final int POINTING_TICKS = 50;
	private static final float HEAL_PER_COPPER_INGOT = 5.0F;

	/** Experience gained by feeding the Kumpel ores, gems and metals. */
	private static final Map<Item, Integer> FEED_EXPERIENCE = Map.ofEntries(
			Map.entry(Items.COAL, 1),
			Map.entry(Items.RAW_COPPER, 2),
			Map.entry(Items.REDSTONE, 2),
			Map.entry(Items.LAPIS_LAZULI, 3),
			Map.entry(Items.QUARTZ, 3),
			Map.entry(Items.AMETHYST_SHARD, 4),
			Map.entry(Items.RAW_IRON, 4),
			Map.entry(Items.IRON_INGOT, 6),
			Map.entry(Items.RAW_GOLD, 6),
			Map.entry(Items.GOLD_INGOT, 8),
			Map.entry(Items.EMERALD, 25),
			Map.entry(Items.DIAMOND, 40),
			Map.entry(Items.NETHERITE_SCRAP, 80),
			Map.entry(Items.NETHERITE_INGOT, 300)
	);
	private static final String[] COMPASS_DIRECTIONS = {
			"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"
	};

	private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
	private final Set<Integer> ignoredItems = new HashSet<>();
	private int experience;
	private boolean oreSensing = true;
	private int ticksSinceLastPickup;
	private int senseCooldown;
	private int pointingTicks;
	private Vec3 pointingTarget;
	private OreKind lastAnnouncedKind;
	private BlockPos lastAnnouncedPos;
	private int lastAnnounceTick = -ANNOUNCE_COOLDOWN_TICKS;

	public KumpelEntity(EntityType<? extends KumpelEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, KumpelTier.COPPER.maxHealth())
				.add(Attributes.MOVEMENT_SPEED, KumpelTier.COPPER.movementSpeed())
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
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_LEVEL, KumpelTier.COPPER.level());
		builder.define(DATA_POINTING, false);
	}

	// ------------------------------------------------------------------
	// Levels & experience

	public KumpelTier getTier() {
		return KumpelTier.byLevel(this.entityData.get(DATA_LEVEL));
	}

	public boolean isPointing() {
		return this.entityData.get(DATA_POINTING);
	}

	public void addExperience(int amount) {
		if (amount <= 0) {
			return;
		}

		KumpelTier before = getTier();
		experience += amount;
		KumpelTier after = KumpelTier.forExperience(experience);

		if (after != before) {
			this.entityData.set(DATA_LEVEL, after.level());
			applyTierAttributes(after, true);
			celebrateLevelUp(after);
		}
	}

	private void applyTierAttributes(KumpelTier tier, boolean healToFull) {
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

	// ------------------------------------------------------------------
	// Interaction

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!isTame()) {
			if (!stack.is(Items.COPPER_INGOT)) {
				return super.mobInteract(player, hand);
			}

			if (!level().isClientSide()) {
				usePlayerItem(player, hand, stack);
				tame(player);
				setOrderedToSit(false);
				level().broadcastEntityEvent(this, (byte) 7);
				player.sendOverlayMessage(Component.translatable("message.kumpel.tamed", getDisplayName()));
			}

			return InteractionResult.SUCCESS;
		}

		boolean handled = stack.isEmpty()
				|| stack.is(Items.COMPASS)
				|| stack.is(Items.COPPER_INGOT)
				|| FEED_EXPERIENCE.containsKey(stack.getItem());

		if (!isOwnedBy(player) || !handled) {
			return super.mobInteract(player, hand);
		}

		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (stack.isEmpty()) {
			if (player.isSecondaryUseActive()) {
				showStatus(player);
				if (hasItemsToDeliver()) {
					deliverItemsTo(player);
				}
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
		} else if (stack.is(Items.COPPER_INGOT) && getHealth() < getMaxHealth()) {
			usePlayerItem(player, hand, stack);
			heal(HEAL_PER_COPPER_INGOT);
			playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.3F + (random.nextFloat() - random.nextFloat()) * 0.2F);
		} else {
			int amount = stack.is(Items.COPPER_INGOT) ? 2 : FEED_EXPERIENCE.get(stack.getItem());
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

	private Component experienceProgress() {
		KumpelTier next = getTier().next();
		return next == null
				? Component.translatable("message.kumpel.max_level")
				: Component.translatable("message.kumpel.xp_progress", experience, next.requiredExperience());
	}

	private void showStatus(Player player) {
		KumpelTier tier = getTier();
		player.sendSystemMessage(Component.translatable("message.kumpel.status",
				getDisplayName(),
				tier.level(),
				tier.displayName(),
				experienceProgress(),
				(int) Math.ceil(getHealth()),
				(int) getMaxHealth(),
				countCarriedItems(),
				Component.translatable(oreSensing ? "options.on" : "options.off")
		).withStyle(ChatFormatting.YELLOW));
	}

	// ------------------------------------------------------------------
	// Collecting & delivering items

	@Override
	public SimpleContainer getInventory() {
		return inventory;
	}

	public boolean isInventoryFull() {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public boolean hasItemsToDeliver() {
		return !inventory.isEmpty();
	}

	public boolean wantsToDeliver() {
		return hasItemsToDeliver() && (isInventoryFull() || ticksSinceLastPickup > DELIVER_DELAY_TICKS);
	}

	private int countCarriedItems() {
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			count += inventory.getItem(i).getCount();
		}

		return count;
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
			if (item.getOwner() == owner || item.distanceToSqr(owner) > MAX_COLLECT_DISTANCE_FROM_OWNER_SQ) {
				return false;
			}
		}

		return inventory.canAddItem(item.getItem());
	}

	public void ignoreItem(ItemEntity item) {
		ignoredItems.add(item.getId());
	}

	public void collect(ItemEntity item) {
		ItemStack stack = item.getItem();
		int before = stack.getCount();
		ItemStack remainder = inventory.addItem(stack.copy());
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
		addExperience(1);
	}

	public void deliverItemsTo(Player player) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int delivered = 0;
		for (ItemStack stack : inventory.removeAllItems()) {
			delivered += stack.getCount();
			player.getInventory().add(stack);

			if (!stack.isEmpty()) {
				// Inventory is full: drop the rest at the owner's feet (marked as thrown by them, so it isn't picked up again).
				ItemEntity drop = new ItemEntity(serverLevel, player.getX(), player.getY() + 0.5, player.getZ(), stack);
				drop.setThrower(player);
				serverLevel.addFreshEntity(drop);
			}
		}

		ticksSinceLastPickup = 0;

		if (delivered > 0) {
			playSound(SoundEvents.ALLAY_ITEM_GIVEN, 0.8F, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.kumpel.delivered", getDisplayName(), delivered));
		}
	}

	// ------------------------------------------------------------------
	// Ore sensing

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		ticksSinceLastPickup++;

		if (pointingTicks > 0) {
			getLookControl().setLookAt(pointingTarget.x, pointingTarget.y, pointingTarget.z);
			if (--pointingTicks == 0) {
				this.entityData.set(DATA_POINTING, false);
			}
		}

		if (tickCount % 600 == 0) {
			ignoredItems.clear();
		}

		if (isTame() && oreSensing && --senseCooldown <= 0) {
			senseCooldown = SENSE_INTERVAL_TICKS;
			if (getOwner() instanceof Player owner && owner.level() == level && distanceToSqr(owner) < MAX_SENSE_DISTANCE_FROM_OWNER_SQ) {
				senseOres(level, owner);
			}
		}
	}

	private void senseOres(ServerLevel level, Player owner) {
		KumpelTier tier = getTier();
		int radius = tier.senseRadius();
		int radiusSq = radius * radius;
		BlockPos center = blockPosition();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		OreKind bestKind = null;
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

					OreKind kind = OreKind.of(state);
					if (kind == null || kind.minLevel() > tier.level()) {
						continue;
					}

					boolean better = bestKind == null
							|| kind.ordinal() > bestKind.ordinal()
							|| (kind == bestKind && distanceSq < bestDistanceSq);
					if (better) {
						bestKind = kind;
						bestPos = cursor.immutable();
						bestDistanceSq = distanceSq;
					}
				}
			}
		}

		if (bestKind == null) {
			return;
		}

		int sinceLast = tickCount - lastAnnounceTick;
		if (bestPos.equals(lastAnnouncedPos) && sinceLast < SAME_ORE_COOLDOWN_TICKS) {
			return;
		}

		boolean moreValuable = lastAnnouncedKind == null || bestKind.ordinal() > lastAnnouncedKind.ordinal();
		if (!moreValuable && sinceLast < ANNOUNCE_COOLDOWN_TICKS) {
			return;
		}

		announceOre(level, owner, bestKind, bestPos);
		lastAnnouncedKind = bestKind;
		lastAnnouncedPos = bestPos;
		lastAnnounceTick = tickCount;
		addExperience(1 + bestKind.minLevel());
	}

	private void announceOre(ServerLevel level, Player owner, OreKind kind, BlockPos orePos) {
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
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, kind.pitch());

		pointingTarget = ore;
		pointingTicks = POINTING_TICKS;
		this.entityData.set(DATA_POINTING, true);

		BlockState state = level.getBlockState(orePos);
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
		return getTier() == KumpelTier.NETHERITE || super.fireImmune();
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return false;
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		for (ItemStack stack : inventory.removeAllItems()) {
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
		writeInventoryToTag(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		experience = input.getIntOr("experience", 0);
		oreSensing = input.getBooleanOr("ore_sensing", true);
		readInventoryFromTag(input);

		KumpelTier tier = KumpelTier.forExperience(experience);
		this.entityData.set(DATA_LEVEL, tier.level());
		applyTierAttributes(tier, false);
	}
}
