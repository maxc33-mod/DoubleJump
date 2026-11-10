package fr.barton.doublejump.genius;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Un petit éclair lancé par une épée enchantée.
 * Ce n'est pas une vraie entité : seul le serveur le connaît. Il avance en ligne droite
 * à chaque tick, et les joueurs le voient grâce à la traînée d'étincelles.
 */
public class GeniusBolt {
	private static final double SPEED = 3.0;           // blocs par tick (comme une flèche bien tirée)
	private static final double MAX_DISTANCE = 20.0;   // portée maximale, en blocs
	private static final double PARTICLE_STEP = 0.25;  // une étincelle tous les quarts de bloc
	private static final double HIT_MARGIN = 0.3;      // les mobs sont un peu plus faciles à toucher

	private final ServerLevel level;
	private final ServerPlayer owner;   // le joueur qui a lancé l'éclair
	private final ItemStack sword;      // l'épée au moment du lancer
	private final float baseDamage;     // dégâts de base de cette épée
	private final int ignoredEntityId;  // le mob frappé au corps à corps par le même coup
	private final Vec3 direction;       // direction de vol (longueur 1)

	private Vec3 position;
	private double travelled = 0;

	public GeniusBolt(ServerPlayer owner, ItemStack sword, int ignoredEntityId) {
		this.level = owner.level();
		this.owner = owner;
		this.sword = sword;
		this.ignoredEntityId = ignoredEntityId;
		this.direction = owner.getLookAngle().normalize();
		this.position = owner.getEyePosition();

		// Dégâts de base = 1 (le poing) + le bonus de l'épée (ex. 6 pour une épée en diamant, soit 7)
		ItemAttributeModifiers modifiers =
				sword.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		this.baseDamage = (float) modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
	}

	public ServerLevel level() {
		return level;
	}

	/**
	 * Fait avancer l'éclair d'un pas.
	 * Renvoie true quand il a fini sa course (mob touché, bloc touché ou portée atteinte).
	 */
	public boolean tick() {
		if (owner.isRemoved() || owner.level() != level) {
			return true; // le joueur s'est déconnecté ou a changé de dimension
		}

		double step = Math.min(SPEED, MAX_DISTANCE - travelled);
		Vec3 from = position;
		Vec3 to = from.add(direction.scale(step));

		// 1. Un bloc sur le chemin ? On raccourcit le trajet jusqu'à lui.
		BlockHitResult blockHit = level.clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
		boolean hitsBlock = blockHit.getType() != HitResult.Type.MISS;
		if (hitsBlock) {
			to = blockHit.getLocation();
		}

		// 2. Un mob sur ce trajet (avant le bloc) ?
		LivingEntity target = findTarget(from, to);
		if (target != null) {
			Vec3 impact = target.getBoundingBox().inflate(HIT_MARGIN).clip(from, to).orElse(to);
			drawTrail(from, impact);
			hit(target, impact);
			return true;
		}

		drawTrail(from, to);
		if (hitsBlock) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 8, 0.1, 0.1, 0.1, 0.05);
			return true;
		}

		position = to;
		travelled += step;
		return travelled >= MAX_DISTANCE;
	}

	// Le mob le plus proche touché entre "from" et "to", ou null
	private LivingEntity findTarget(Vec3 from, Vec3 to) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		AABB searchBox = new AABB(from, to).inflate(1.0);
		for (Entity entity : level.getEntities(owner, searchBox, this::canHit)) {
			Optional<Vec3> crossing = entity.getBoundingBox().inflate(HIT_MARGIN).clip(from, to);
			if (crossing.isPresent()) {
				double distance = from.distanceToSqr(crossing.get());
				if (distance < bestDistance) {
					best = (LivingEntity) entity;
					bestDistance = distance;
				}
			}
		}
		return best;
	}

	// Ce qu'une épée peut toucher : les créatures vivantes, sauf le lanceur et sa cible au corps à corps
	private boolean canHit(Entity entity) {
		return entity instanceof LivingEntity
				&& entity.isAlive()
				&& entity.isPickable()
				&& !entity.isSpectator()
				&& entity != owner
				&& entity.getId() != ignoredEntityId;
	}

	// L'éclair touche un mob : dégâts, Recul, Aura de feu, usure de l'épée, effets
	private void hit(LivingEntity target, Vec3 impact) {
		DamageSource source = owner.damageSources().playerAttack(owner);

		// Tranchant, Châtiment et Fléau des arthropodes ajoutent leur bonus ici
		float damage = EnchantmentHelper.modifyDamage(level, sword, target, source, baseDamage);

		// hurtServer refuse tout seul les cas interdits (PvP désactivé, mob invulnérable...)
		boolean hurt = target.hurtServer(level, source, damage);
		if (hurt) {
			// Pas de recul de base ; seulement celui de l'enchantement Recul s'il est présent
			float knockback = EnchantmentHelper.modifyKnockback(level, sword, target, source, 0.0F);
			if (knockback > 0) {
				target.knockback(knockback, -direction.x, -direction.z, source, damage);
			}

			// Aura de feu (et les autres effets "après l'attaque")
			EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, sword);

			// Un point d'usure, si le joueur tient toujours la même épée
			if (owner.getMainHandItem() == sword) {
				sword.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
			}
		}

		level.sendParticles(ParticleTypes.SWEEP_ATTACK, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 10, 0.2, 0.2, 0.2, 0.1);
		level.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4F, 2.0F);
	}

	// Traînée d'étincelles électriques entre deux points
	private void drawTrail(Vec3 from, Vec3 to) {
		double length = from.distanceTo(to);
		for (double d = 0; d <= length; d += PARTICLE_STEP) {
			Vec3 point = from.add(direction.scale(d));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}
}
