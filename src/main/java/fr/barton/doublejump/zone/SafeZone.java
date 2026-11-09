package fr.barton.doublejump.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * Une safe zone : la position de sa Magnétite et son niveau (de 1 à 4).
 */
public record SafeZone(BlockPos pos, int level) {
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 4;

	// Indique à Minecraft comment écrire et relire une zone sur le disque.
	public static final Codec<SafeZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(SafeZone::pos),
			Codec.INT.fieldOf("level").forGetter(SafeZone::level)
	).apply(instance, SafeZone::new));

	// Indique comment envoyer une zone sur le réseau.
	public static final StreamCodec<ByteBuf, SafeZone> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, SafeZone::pos,
			ByteBufCodecs.VAR_INT, SafeZone::level,
			SafeZone::new
	);

	/** Longueur du côté du cube : 51, 101, 151 ou 201 blocs selon le niveau. */
	public int side() {
		return 50 * level + 1;
	}

	/** Indique si un bloc se trouve à l'intérieur du cube, centré sur la Magnétite. */
	public boolean contains(BlockPos other) {
		int radius = radius();
		return Math.abs(other.getX() - pos.getX()) <= radius
				&& Math.abs(other.getY() - pos.getY()) <= radius
				&& Math.abs(other.getZ() - pos.getZ()) <= radius;
	}

	/** Moitié du côté, sans compter le bloc central : 25, 50, 75 ou 100 blocs. */
	public int radius() {
		return 25 * level;
	}

	/** Distance entre un point situé dans le cube et la face du cube dans cette direction. */
	public double distanceToFace(Vec3 point, Direction side) {
		int step = side.getAxisDirection().getStep(); // +1 ou -1 selon le sens de la direction
		double face = pos.get(side.getAxis()) + 0.5 + step * (radius() + 0.5);
		return (face - point.get(side.getAxis())) * step;
	}

	public boolean isMaxLevel() {
		return level >= MAX_LEVEL;
	}

	/** Quartz à payer pour passer au niveau suivant : 50, 100 puis 150. */
	public int upgradeCost() {
		return 50 * level;
	}

	/** La même zone, un niveau au-dessus. */
	public SafeZone nextLevel() {
		return new SafeZone(pos, level + 1);
	}
}
