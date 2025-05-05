package elysium.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import elysium.Util;

import java.awt.*;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicRender;

public class EnergyBlasterOnHitAlt implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

	if (target instanceof CombatEntityAPI) {
	    // For the alt version, we have a larger explosion with EMP component
	    float explosionRadius = 300f;
	    float baseDamage = 1500f;
	    float empDamage = 300f;

	    // Primary impact effect - brighter and larger
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/fx/particlealpha_textured.png"),
		    point,
		    new Vector2f(),
		    new Vector2f(explosionRadius * 0.6f, explosionRadius * 0.6f),
		    new Vector2f(explosionRadius * 2f, explosionRadius * 2f),
		    projectile.getFacing() - 90f,
		    0f,
		    new Color(0, 255, 255, 255),
		    true,
		    0.15f,
		    0f,
		    0.3f
	    );

	    // Create main explosion with proper damage spec
	    DamagingExplosionSpec explosion = new DamagingExplosionSpec(
		    0.15f, // longer duration
		    explosionRadius, // radius
		    explosionRadius * 0.6f, // larger core radius
		    baseDamage, // max damage
		    baseDamage * 0.5f, // min damage
		    CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
		    CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
		    6f, // particle size min
		    4f, // particle size range
		    1.2f, // particle duration
		    40, // more particles
		    Util.ELYSIUM_SECONDARY, // particle color - blue
		    Util.ELYSIUM_PRIMARY  // explosion color - cyan
	    );

	    explosion.setDamageType(DamageType.HIGH_EXPLOSIVE); // High explosive damage
	    explosion.setMaxEMPDamage(empDamage);
	    // Add EMP component

	    // Enhanced visual effects for the special weapon
	    explosion.setShowGraphic(true);
	    explosion.setUseDetailedExplosion(true);
	    explosion.setDetailedExplosionFlashRadius(explosionRadius * 3f);
	    explosion.setDetailedExplosionFlashColorCore(Util.ELYSIUM_PRIMARY);
	    explosion.setDetailedExplosionFlashColorFringe(Util.ELYSIUM_SECONDARY);

	    // Apply the explosion at the hit location
	    engine.spawnDamagingExplosion(explosion, projectile.getSource(), point);

	    // Play enhanced explosion sound
	    Global.getSoundPlayer().playSound("heavy_mortar_fire", 0.9f, 0.7f, point, new Vector2f());

	    // Add a shockwave effect
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
		    point,
		    new Vector2f(),
		    new Vector2f(explosionRadius * 0.4f, explosionRadius * 0.4f),
		    new Vector2f(explosionRadius * 3f, explosionRadius * 3f),
		    MathUtils.getRandomNumberInRange(0, 360),
		    0f,
		    new Color(0, 255, 255, 200),
		    true,
		    0.2f,
		    0f,
		    0.4f
	    );

	    // Create secondary explosions around the impact point
	    /*
	    for (int i = 0; i < 3; i++) {
		Vector2f randomOffset = MathUtils.getRandomPointInCircle(point, explosionRadius * 0.5f);

		// Secondary smaller explosions
		DamagingExplosionSpec secondaryExplosion = new DamagingExplosionSpec(
			0.1f,
			explosionRadius * 0.3f,
			explosionRadius * 0.15f,
			baseDamage * 0.2f,
			baseDamage * 0.1f,
			CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
			CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
			4f,
			2f,
			0.8f,
			15,
			Util.ELYSIUM_SECONDARY,
			Util.ELYSIUM_PRIMARY
		);

		secondaryExplosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
		explosion.setMaxEMPDamage(empDamage* 0.2f);

		engine.spawnDamagingExplosion(secondaryExplosion, projectile.getSource(), randomOffset);
	    }*/

	    // Secondary effect - energy tendrils
	    for (int i = 0; i < 8; i++) {
		float angle = i * 45f;
		Vector2f offset = MathUtils.getPointOnCircumference(point, explosionRadius * 0.5f, angle);

		MagicRender.battlespace(
			Global.getSettings().getSprite("graphics/fx/energy_shock.png"),
			offset,
			new Vector2f(),
			new Vector2f(30f, 30f),
			new Vector2f(90f, 90f),
			angle,
			MathUtils.getRandomNumberInRange(-5f, 5f),
			new Color(0, 255, 255, 150),
			true,
			0.1f,
			0.05f,
			0.2f
		);
	    }

	    // EMP arc visual effect if shield hit
	    if (target instanceof ShipAPI) {
		ShipAPI ship = (ShipAPI) target;
		if (ship.getShield() != null && ship.getShield().isOn()) {
		    engine.addSmoothParticle(
			    ship.getLocation(),
			    new Vector2f(),
			    150f,
			    0.5f,
			    0.1f,
			    new Color(100, 200, 255, 150)
		    );

		    // Add more shield impact effects
		    for (int i = 0; i < 5; i++) {
			float angle = MathUtils.getRandomNumberInRange(0, 360);
			Vector2f arcPoint = MathUtils.getPointOnCircumference(
				target.getLocation(),
				ship.getShield().getRadius() * 0.8f,
				angle
			);

			engine.spawnEmpArcVisual(
				point,
				target,
				arcPoint,
				target,
				10f, // thickness
				new Color(0, 255, 255, 255),
				new Color(100, 200, 255, 255)
			);
		    }
		}
	    }
	}
    }
}