package io.github.chaos634.taubenschlag.entity;

import java.time.Year;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.advancement.TaubenschlagAdvancements;
import io.github.chaos634.taubenschlag.flight.Flugplan;
import io.github.chaos634.taubenschlag.item.ReisekorbItem;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;
import io.github.chaos634.taubenschlag.registry.TaubenschlagSounds;

/**
 * Brieftaube: a homing pigeon. Wild ones are tamed with seeds; a tame pigeon settles into the nearest loft and from
 * then on stays around it. Wherever it is taken, it finds its way home: let it go (sneak and use it, or throw it up
 * from a travel basket) and it flies back to its loft, carrying whatever it was given.
 */
public class BrieftaubeEntity extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(BrieftaubeEntity.class, EntityDataSerializers.INT);

	/** Seeds tame wild pigeons, heal tame ones and make two tame ones breed. */
	public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Taubenschlag.id("brieftaube_food"));

	/** One in this many seeds tames a wild pigeon. */
	private static final int TAME_CHANCE = 3;
	/** A tame pigeon without a loft settles into one this close. */
	public static final int SETTLE_RANGE = 8;
	private static final int SETTLE_INTERVAL = 40;
	/** At home, a pigeon keeps within this distance of its loft... */
	public static final double LOFT_RANGE = 12.0;
	/** ...and when it is let out further away than this, it soon flies home by itself. */
	public static final double HOMESICK_RANGE = 32.0;
	public static final int HOMESICK_TICKS = 30 * 20;
	private static final float HEAL_PER_SEED = 2.0F;

	private GlobalPos home;
	private String ring = "";
	private int flights;
	/** The best speed so far, in hundredths of a block per second. */
	private int bestSpeed;
	private int awayTicks;

	// Wing flapping, the way the parrot does it.
	public float flap;
	public float flapSpeed;
	public float oFlap;
	public float oFlapSpeed;
	private float flapping = 1.0F;

	public BrieftaubeEntity(EntityType<? extends BrieftaubeEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 10, false);
		if (!level.isClientSide()) {
			setVariant(BrieftaubeVariant.random(random));
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 8.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new FlyingPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		this.goalSelector.addGoal(4, new ReturnToLoftGoal(this, 1.0));
		// Only pigeons without a loft follow their owner; the others stay at home.
		this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F) {
			@Override
			public boolean canUse() {
				return home == null && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return home == null && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), false));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
	}

	public BrieftaubeVariant getVariant() {
		return BrieftaubeVariant.byIndex(this.entityData.get(DATA_VARIANT));
	}

	public void setVariant(BrieftaubeVariant variant) {
		this.entityData.set(DATA_VARIANT, variant.ordinal());
	}

	public GlobalPos getHome() {
		return home;
	}

	public void setHome(GlobalPos home) {
		this.home = home;
		this.awayTicks = 0;
	}

	public String getRing() {
		return ring;
	}

	public int getFlights() {
		return flights;
	}

	/** The best speed so far, in blocks per second. */
	public double getBestSpeed() {
		return bestSpeed / 100.0;
	}

	/** Tames the pigeon and puts a ring on its leg. */
	public void tameBy(Player player) {
		tame(player);
		giveRing();
	}

	/** Every tame pigeon wears a ring with its number, like DV-04711-26-123: club, year, number. */
	private void giveRing() {
		if (ring.isEmpty()) {
			ring = String.format(Locale.ROOT, "DV-%05d-%02d-%03d", 1 + random.nextInt(9999), Year.now().getValue() % 100, 1 + random.nextInt(999));
		}
	}

	/** Its name, or else its ring number. */
	public Component describe() {
		if (hasCustomName()) {
			return getCustomName();
		}

		return describeRing(ring);
	}

	public static Component describeRing(String ring) {
		return ring.isEmpty() ? Component.translatable("entity.taubenschlag.brieftaube") : Component.translatable("entity.taubenschlag.brieftaube.ringed", ring);
	}

	// ------------------------------------------------------------------
	// Flying

	@Override
	public void aiStep() {
		super.aiStep();
		calculateFlapping();

		if (level() instanceof ServerLevel level && isAlive()) {
			if (tickCount % SETTLE_INTERVAL == 0) {
				checkLoft(level);
			}
			checkHomesick(level);
		}
	}

	private void calculateFlapping() {
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;
		this.flapSpeed += (!onGround() && !isPassenger() ? 4.0F : -1.0F) * 0.3F;
		this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
		if (!onGround() && this.flapping < 1.0F) {
			this.flapping = 1.0F;
		}
		this.flapping *= 0.9F;

		// Pigeons glide down instead of falling.
		Vec3 movement = getDeltaMovement();
		if (!onGround() && movement.y < 0.0) {
			setDeltaMovement(movement.multiply(1.0, 0.6, 1.0));
		}
		this.flap += this.flapping * 2.0F;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
		// Birds don't fall.
	}

	public boolean isAirborne() {
		return !onGround() && !isPassenger();
	}

	// ------------------------------------------------------------------
	// The loft

	/** A tame pigeon without a loft settles into one nearby; one whose loft is gone forgets it. */
	private void checkLoft(ServerLevel level) {
		if (!isTame() || isBaby()) {
			return;
		}
		if (home != null) {
			if (home.dimension().equals(level.dimension()) && level.isLoaded(home.pos())
					&& !level.getBlockState(home.pos()).is(TaubenschlagBlocks.TAUBENSCHLAG)) {
				home = null;
				if (getOwner() instanceof Player owner) {
					owner.sendSystemMessage(Component.translatable("message.taubenschlag.loft_lost", describe()));
				}
			}
			return;
		}

		BlockPos origin = blockPosition();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SETTLE_RANGE, -4, -SETTLE_RANGE), origin.offset(SETTLE_RANGE, 4, SETTLE_RANGE))) {
			if (level.getBlockState(pos).is(TaubenschlagBlocks.TAUBENSCHLAG)) {
				double distance = pos.distSqr(origin);
				if (distance < nearestDistance) {
					nearest = pos.immutable();
					nearestDistance = distance;
				}
			}
		}
		if (nearest != null) {
			settleInto(level, nearest);
		}
	}

	/** The loft at {@code pos} becomes the pigeon's home. */
	public void settleInto(ServerLevel level, BlockPos pos) {
		setHome(GlobalPos.of(level.dimension(), pos.immutable()));
		playSound(TaubenschlagSounds.GURREN, 1.0F, 1.0F);
		if (getOwner() instanceof Player owner) {
			owner.sendSystemMessage(Component.translatable("message.taubenschlag.settled", describe(), pos.getX(), pos.getY(), pos.getZ()));
			TaubenschlagAdvancements.award(owner, TaubenschlagAdvancements.HEIMATSCHLAG);
		}
	}

	/** Let out far from home, a pigeon flies back after a while, unless it is told to stay. */
	private void checkHomesick(ServerLevel level) {
		if (home == null || !isTame() || isOrderedToSit() || isLeashed() || isPassenger() || isBaby() || !home.dimension().equals(level.dimension())
				|| distanceToSqr(Vec3.atCenterOf(home.pos())) <= HOMESICK_RANGE * HOMESICK_RANGE) {
			awayTicks = 0;
			return;
		}

		if (++awayTicks >= HOMESICK_TICKS) {
			takeOff(level, null, ItemStack.EMPTY);
		}
	}

	/**
	 * Auflassen: the pigeon flies home to its loft, with the post if there is any. It leaves the world and turns up at
	 * the loft once it has covered the distance.
	 *
	 * @param sender who let it go, told how it went (may be {@code null})
	 * @return whether it took off
	 */
	public boolean takeOff(ServerLevel level, Player sender, ItemStack post) {
		String problem = null;
		if (home == null) {
			problem = "message.taubenschlag.no_home";
		} else if (!home.dimension().equals(level.dimension())) {
			problem = "message.taubenschlag.other_dimension";
		} else if (isBaby()) {
			problem = "message.taubenschlag.too_young";
		}
		if (problem != null) {
			if (sender != null) {
				sender.sendOverlayMessage(Component.translatable(problem, describe()));
			}
			return false;
		}

		BlockPos from = blockPosition();
		double dx = home.pos().getX() - from.getX();
		double dz = home.pos().getZ() - from.getZ();
		int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
		long flightTicks = Flugplan.flightTicks(distance, flightSpeed());
		long now = level.getServer().overworld().getGameTime();
		EntityReference<LivingEntity> owner = getOwnerReference();
		setOrderedToSit(false);
		Flugplan.get(level.getServer()).add(new Flugplan.Flight(BrieftaubeData.of(this), home, post.copy(),
				Optional.ofNullable(owner).map(EntityReference::getUUID), from, now, now + flightTicks, distance));

		level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
		level.playSound(null, from, TaubenschlagSounds.AUFLASSEN, SoundSource.NEUTRAL, 1.0F, 1.0F);
		if (sender != null) {
			sender.sendOverlayMessage(Component.translatable(post.isEmpty() ? "message.taubenschlag.auflassen" : "message.taubenschlag.auflassen.post",
					describe(), distance));
		}
		discard();
		return true;
	}

	/** How fast it flies this time, in blocks per second: its form on the day, and a bit faster with every flight it has made. */
	private double flightSpeed() {
		double form = 1.0 - Flugplan.FORM_SPREAD + random.nextDouble() * 2.0 * Flugplan.FORM_SPREAD;
		return Flugplan.BASE_SPEED * form * (1.0 + Math.min(flights, Flugplan.MAX_TRAINING) * Flugplan.TRAINING_PER_FLIGHT);
	}

	/**
	 * Called when it is back at its loft.
	 *
	 * @return whether the speed is a new best for this pigeon
	 */
	public boolean landedAfter(double speed, boolean loftStands) {
		flights++;
		int hundredths = (int) Math.round(speed * 100.0);
		boolean best = hundredths > bestSpeed;
		if (best) {
			bestSpeed = hundredths;
		}
		if (!loftStands) {
			home = null;
		}

		return best && flights > 1;
	}

	// ------------------------------------------------------------------
	// Interaction

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof ReisekorbItem) {
			return ReisekorbItem.catchPigeon(stack, player, this);
		}

		if (!isTame()) {
			if (!isFood(stack) || isBaby()) {
				return super.mobInteract(player, hand);
			}

			if (level() instanceof ServerLevel) {
				usePlayerItem(player, hand, stack);
				if (random.nextInt(TAME_CHANCE) == 0) {
					tameBy(player);
					level().broadcastEntityEvent(this, (byte) 7);
					player.sendOverlayMessage(Component.translatable("message.taubenschlag.tamed", describe()));
					TaubenschlagAdvancements.award(player, TaubenschlagAdvancements.RENNPFERD);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
			}
			return InteractionResult.SUCCESS;
		}

		if (!isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		}

		if (player.isSecondaryUseActive()) {
			// Auflassen, with whatever is in the hand as post.
			if (level() instanceof ServerLevel level && takeOff(level, player, stack) && !stack.isEmpty()) {
				player.setItemInHand(hand, ItemStack.EMPTY);
			}
			return InteractionResult.SUCCESS;
		}

		if (isFood(stack) && getHealth() < getMaxHealth()) {
			if (level() instanceof ServerLevel) {
				usePlayerItem(player, hand, stack);
				heal(HEAL_PER_SEED);
			}
			return InteractionResult.SUCCESS;
		}

		if (stack.isEmpty()) {
			if (level() instanceof ServerLevel) {
				boolean sit = !isOrderedToSit();
				setOrderedToSit(sit);
				this.jumping = false;
				getNavigation().stop();
				player.sendOverlayMessage(Component.translatable(sit ? "message.taubenschlag.sitting" : "message.taubenschlag.free", describe()));
			}
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(FOOD);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		BrieftaubeEntity young = TaubenschlagEntities.BRIEFTAUBE.create(level, EntitySpawnReason.BREEDING);
		if (young == null) {
			return null;
		}

		young.setVariant(partner instanceof BrieftaubeEntity other && random.nextBoolean() ? other.getVariant() : getVariant());
		EntityReference<LivingEntity> owner = getOwnerReference();
		if (owner != null) {
			young.setOwnerReference(owner);
			young.setTame(true, true);
			young.giveRing();
			young.home = home;
			TaubenschlagAdvancements.award(getOwner(), TaubenschlagAdvancements.JUNGTAUBE);
		}
		return young;
	}

	// ------------------------------------------------------------------
	// Sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return TaubenschlagSounds.GURREN;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PARROT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PARROT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.PARROT_STEP, 0.15F, 1.0F);
	}

	// ------------------------------------------------------------------
	// Saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("variant", getVariant().ordinal());
		if (home != null) {
			output.store("home", GlobalPos.CODEC, home);
		}
		output.putString("ring", ring);
		output.putInt("flights", flights);
		output.putInt("best_speed", bestSpeed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setVariant(BrieftaubeVariant.byIndex(input.getIntOr("variant", 0)));
		home = input.read("home", GlobalPos.CODEC).orElse(null);
		ring = input.getStringOr("ring", "");
		flights = input.getIntOr("flights", 0);
		bestSpeed = input.getIntOr("best_speed", 0);
	}
}
