package elysium.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import elysium.Util;
import org.lwjgl.util.vector.Vector2f;


public class ElysiumTorpedoHitEffect implements OnHitEffectPlugin {
    // Constants for damage calculation
    private static final float MAX_BONUS_MULTIPLIER = 1.5f; // Maximum 50% bonus damage at point-blank
    private static final float MAX_FLIGHT_TIME = 2.0f; // Maximum flight time before damage drops to base

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
	// Calculate flight time-based damage multiplier
	float flightTime = projectile.getElapsed();
	float damageMult = calculateDamageMultiplier(flightTime);

	// Apply damage bonus if target is a ship (not asteroid or other entity)
	if (target instanceof ShipAPI) {
	    float actualDamage = (projectile.getDamageAmount() * damageMult) - projectile.getDamageAmount();

	    // Create visual explosion effect
	    DamagingExplosionSpec explosion = new DamagingExplosionSpec(
		    0.1f, // duration
		    175f, // radius
		    75f,  // core radius
		    actualDamage, // damage
		    actualDamage / 2f, // min damage (half of max)
		    CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
		    CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
		    5f, // particle size min
		    3f, // particle size range
		    1f, // particle duration
		    200, // particle count
		    Util.ELYSIUM_SECONDARY, // particle color - blue
		    Util.ELYSIUM_PRIMARY  // explosion color - cyan
	    );

	    explosion.setDamageType(DamageType.ENERGY); // Energy damage as specified

	    // Add visual flare based on damage multiplier
	    if (damageMult > 1.2f) {
		// More impressive visual for high-damage hits
		explosion.setShowGraphic(true);
		explosion.setUseDetailedExplosion(true);
		explosion.setDetailedExplosionFlashRadius(500f * damageMult);
		explosion.setDetailedExplosionFlashColorCore(Util.ELYSIUM_PRIMARY);
		explosion.setDetailedExplosionFlashColorFringe(Util.ELYSIUM_SECONDARY);
	    }

	    // Apply the explosion at the hit location
	    engine.spawnDamagingExplosion(explosion, projectile.getSource(), point);

	    // Add visual feedback about damage multiplier
	    String damageText = String.format("%.0f", actualDamage);
	    if (damageMult > 1.0f) {
		damageText += String.format(" (x%.1f)", damageMult);
	    }

	    // Show floating damage text
	    engine.addFloatingDamageText(point, actualDamage, Util.ELYSIUM_PRIMARY, target, projectile.getSource());

	    // Add particle effect that scales with damage multiplier
	    int particleCount = (int)(50 * damageMult);
	    for (int i = 0; i < particleCount; i++) {
		float angle = (float) (Math.random() * 360);
		float distance = (float) (Math.random() * 100 * damageMult);
		Vector2f particlePos = new Vector2f(
			point.x + (float) Math.cos(Math.toRadians(angle)) * distance,
			point.y + (float) Math.sin(Math.toRadians(angle)) * distance
		);

		float size = 5f + (float) (Math.random() * 5f) * damageMult;
		float duration = 0.5f + (float) (Math.random() * 0.5f);

		engine.addHitParticle(
			particlePos,
			new Vector2f(0, 0),
			size,
			1f,
			duration,
			Util.ELYSIUM_PRIMARY
		);
	    }
	}
    }

    /**
     * Calculate damage multiplier based on flight time
     * - Fresh missiles do maximum damage
     * - Damage decreases linearly with flight time
     */
    private float calculateDamageMultiplier(float flightTime) {
	if (flightTime >= MAX_FLIGHT_TIME) {
	    return 1.0f; // Base damage at max flight time
	} else {
	    // Linear damage decrease from max bonus to base damage
	    float timeRatio = flightTime / MAX_FLIGHT_TIME;
	    return MAX_BONUS_MULTIPLIER - (timeRatio * (MAX_BONUS_MULTIPLIER - 1.0f));
	}
    }
}

/**
 * Every frame effect for missile glow and trail enhancement

public class ElysiumTorpedoEveryFrameEffect implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	// No implementation needed for now
	// This can be used for additional weapon effects if needed
    }
} */