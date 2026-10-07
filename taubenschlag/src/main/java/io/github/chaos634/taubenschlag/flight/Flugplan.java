package io.github.chaos634.taubenschlag.flight;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import io.github.chaos634.taubenschlag.Taubenschlag;
import io.github.chaos634.taubenschlag.advancement.TaubenschlagAdvancements;
import io.github.chaos634.taubenschlag.block.TaubenschlagBlock;
import io.github.chaos634.taubenschlag.block.TaubenschlagBlockEntity;
import io.github.chaos634.taubenschlag.entity.BrieftaubeData;
import io.github.chaos634.taubenschlag.entity.BrieftaubeEntity;
import io.github.chaos634.taubenschlag.registry.TaubenschlagBlocks;
import io.github.chaos634.taubenschlag.registry.TaubenschlagSounds;

/**
 * Flugplan: the pigeons on their way home. A pigeon that has been let go leaves the world and lands at its loft once
 * it has covered the distance, flying even while nobody is near. If the loft's chunk is not loaded when it arrives, it
 * lands as soon as it is; the time it took still counts as flown.
 */
public class Flugplan extends SavedData {
	/** How fast a pigeon flies, in blocks per second, before its form on the day and its training (real ones manage about 65 km/h). */
	public static final double BASE_SPEED = 18.0;
	/** Form on the day: up to this much faster or slower. */
	public static final double FORM_SPREAD = 0.15;
	/** Every flight home makes a pigeon a little faster, up to this many flights. */
	public static final int MAX_TRAINING = 30;
	public static final double TRAINING_PER_FLIGHT = 0.01;
	/** Even a short hop takes this long: circling to get its bearings, then landing. */
	public static final long MIN_FLIGHT_TICKS = 3 * 20;
	/** From this distance on, a flight home earns the Preisflug advancement. */
	public static final int PREISFLUG_DISTANCE = 1000;
	private static final int CHECK_INTERVAL = 20;

	/**
	 * A pigeon on its way home.
	 *
	 * @param pigeon     the pigeon itself
	 * @param home       its loft
	 * @param post       what it carries (may be empty)
	 * @param owner      who to tell when it lands
	 * @param from       where it was let go
	 * @param departedAt game time it took off
	 * @param arrivesAt  game time it reaches its loft
	 * @param distance   how far it flies, in blocks
	 */
	public record Flight(BrieftaubeData pigeon, GlobalPos home, ItemStack post, Optional<UUID> owner, BlockPos from, long departedAt, long arrivesAt,
			int distance) {
		public static final Codec<Flight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BrieftaubeData.CODEC.fieldOf("pigeon").forGetter(Flight::pigeon),
				GlobalPos.CODEC.fieldOf("home").forGetter(Flight::home),
				ItemStack.OPTIONAL_CODEC.optionalFieldOf("post", ItemStack.EMPTY).forGetter(Flight::post),
				UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Flight::owner),
				BlockPos.CODEC.fieldOf("from").forGetter(Flight::from),
				Codec.LONG.fieldOf("departed_at").forGetter(Flight::departedAt),
				Codec.LONG.fieldOf("arrives_at").forGetter(Flight::arrivesAt),
				Codec.INT.fieldOf("distance").forGetter(Flight::distance)
		).apply(instance, Flight::new));

		public long flightTicks() {
			return arrivesAt - departedAt;
		}

		/** Blocks per second. */
		public double speed() {
			return distance / Math.max(1.0, flightTicks() / 20.0);
		}
	}

	public static final Codec<Flugplan> CODEC = Flight.CODEC.listOf().xmap(Flugplan::new, Flugplan::flights);
	public static final SavedDataType<Flugplan> TYPE = new SavedDataType<>(Taubenschlag.id("flugplan"), Flugplan::new, CODEC, null);

	private final List<Flight> flights = new ArrayList<>();

	public Flugplan() {
	}

	private Flugplan(List<Flight> flights) {
		this.flights.addAll(flights);
	}

	public static Flugplan get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** How long a flight over {@code distance} blocks takes at {@code speed} blocks per second, in ticks. */
	public static long flightTicks(int distance, double speed) {
		return MIN_FLIGHT_TICKS + Math.round(distance / speed * 20.0);
	}

	public List<Flight> flights() {
		return List.copyOf(flights);
	}

	public void add(Flight flight) {
		flights.add(flight);
		setDirty();
	}

	/** Lands every pigeon that has arrived, if its loft is loaded. */
	public void tick(MinecraftServer server) {
		if (flights.isEmpty() || server.getTickCount() % CHECK_INTERVAL != 0) {
			return;
		}

		long now = server.overworld().getGameTime();
		List<Flight> landing = new ArrayList<>();
		for (Iterator<Flight> it = flights.iterator(); it.hasNext(); ) {
			Flight flight = it.next();
			if (flight.arrivesAt() > now) {
				continue;
			}
			ServerLevel level = server.getLevel(flight.home().dimension());
			if (level != null && level.isLoaded(flight.home().pos())) {
				it.remove();
				landing.add(flight);
			}
		}

		if (!landing.isEmpty()) {
			setDirty();
			for (Flight flight : landing) {
				land(server.getLevel(flight.home().dimension()), flight);
			}
		}
	}

	/** The pigeon lands in front of its loft and the post goes in. */
	private static void land(ServerLevel level, Flight flight) {
		BlockPos loft = flight.home().pos();
		BlockState state = level.getBlockState(loft);
		boolean loftStands = state.is(TaubenschlagBlocks.TAUBENSCHLAG);
		Direction front = loftStands ? state.getValue(TaubenschlagBlock.FACING) : Direction.SOUTH;
		BlockPos landingSpot = loft.relative(front);
		if (!level.getBlockState(landingSpot).getCollisionShape(level, landingSpot).isEmpty()) {
			landingSpot = loft.above();
		}

		BrieftaubeEntity pigeon = flight.pigeon().restore(level, Vec3.atBottomCenterOf(landingSpot), front.toYRot(), EntitySpawnReason.EVENT);
		boolean best = pigeon != null && pigeon.landedAfter(flight.speed(), loftStands);

		ItemStack post = flight.post().copy();
		int brought = post.getCount();
		if (!post.isEmpty() && loftStands && level.getBlockEntity(loft) instanceof TaubenschlagBlockEntity schlag) {
			post = schlag.receive(post);
		}
		boolean leftOutside = !post.isEmpty();
		if (leftOutside) {
			Containers.dropItemStack(level, landingSpot.getX() + 0.5, landingSpot.getY() + 0.5, landingSpot.getZ() + 0.5, post);
		}

		level.playSound(null, landingSpot, TaubenschlagSounds.GURREN, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, landingSpot.getX() + 0.5, landingSpot.getY() + 0.5, landingSpot.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);

		ServerPlayer owner = flight.owner().map(uuid -> level.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
		if (owner == null) {
			return;
		}

		Component name = flight.pigeon().describe();
		if (!loftStands) {
			owner.sendSystemMessage(Component.translatable("message.taubenschlag.loft_gone", name).withStyle(ChatFormatting.YELLOW));
		}
		owner.sendSystemMessage(Component.translatable("message.taubenschlag.landed", name, flight.distance(), formatTime(flight.flightTicks()),
				String.format(Locale.ROOT, "%.1f", flight.speed())).withStyle(ChatFormatting.GOLD));
		if (brought > 0) {
			owner.sendSystemMessage(Component.translatable("message.taubenschlag.landed.post", brought, flight.post().getHoverName()));
			if (leftOutside && loftStands) {
				owner.sendSystemMessage(Component.translatable("message.taubenschlag.loft_full").withStyle(ChatFormatting.YELLOW));
			}
			TaubenschlagAdvancements.award(owner, TaubenschlagAdvancements.LUFTPOST);
		}
		if (best) {
			owner.sendSystemMessage(Component.translatable("message.taubenschlag.best", name).withStyle(ChatFormatting.GREEN));
		}
		if (flight.distance() >= PREISFLUG_DISTANCE) {
			TaubenschlagAdvancements.award(owner, TaubenschlagAdvancements.PREISFLUG);
		}
	}

	/** Minutes and seconds, like 1:05. */
	public static String formatTime(long ticks) {
		long seconds = ticks / 20;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}
}
