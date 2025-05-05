package elysium.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import elysium.Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Special effect for Elysium's Flower Torpedoes
 * Applies bonus damage to shields and creates a visual effect on impact
 */
public class FlowerTorpedoEffect implements OnHitEffectPlugin, OnFireEffectPlugin {
    // Standard Elysium colors (cyan/blue)
    private static final Color CYAN_AQUA = new Color(0, 255, 255); // Cyan/Aqua
    private static final Color BLUE = new Color(0, 150, 255); // Blue

    // Comet variant colors (red/orange)
    private static final Color ORANGE_RED = new Color(255, 100, 0); // Orange-Red
    private static final Color RED = new Color(255, 50, 0); // Red

    private static final float SHIELD_DAMAGE_MULT = 1.5f;
    private static final float COMET_SHIELD_DAMAGE_MULT = 1.2f; // 20% bonus as per CSV

    private Random rand = new Random();

    // Used to store colors for the current instance
    private Color explosionInnerColor;
    private Color explosionOuterColor;
    private float shieldDamageMult;
    private boolean isComet = false;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
	// Determine which color scheme to use based on the weapon that fired this projectile
	determineColorScheme(projectile);

	// Apply extra shield damage when hitting shields
	if (shieldHit && target instanceof ShipAPI) {
	    ShipAPI ship = (ShipAPI) target;

	    // Calculate damage
	    float damageAmount = projectile.getDamageAmount();
	    float empAmount = projectile.getEmpAmount();

	    // Apply the bonus shield damage
	    float bonusDamage = damageAmount * (shieldDamageMult - 1f);

	    // Apply shield damage with proper stats taken into account
	    float damageToShield = bonusDamage;
	    if (ship.getShield() != null) {
		float shipHardFlux = ship.getFluxTracker().getHardFlux();
		ship.getFluxTracker().increaseFlux(damageToShield, true);

		// Log the bonus damage dealt to the shield
		engine.addFloatingDamageText(point, bonusDamage, new Color(0, 191, 255), target, projectile.getSource());
	    }
	}

	// Create explosion visual effect
	createExplosionEffect(projectile, point, engine);
    }

    private void determineColorScheme(DamagingProjectileAPI projectile) {
	// Default to standard colors
	explosionInnerColor = CYAN_AQUA;
	explosionOuterColor = BLUE;
	shieldDamageMult = SHIELD_DAMAGE_MULT;
	isComet = false;

	// If no projectile or source weapon, use default colors
	if (projectile == null || projectile.getWeapon() == null) {
	    return;
	}

	String weaponId = projectile.getWeapon().getId();

	// Check if this is from the Comet variant
	if (weaponId != null && weaponId.contains("_hidden")) {
	    explosionInnerColor = ORANGE_RED;
	    explosionOuterColor = RED;
	    shieldDamageMult = COMET_SHIELD_DAMAGE_MULT;
	    isComet = true;
	}
    }

    private void createExplosionEffect(DamagingProjectileAPI projectile, Vector2f point, CombatEngineAPI engine) {
	Random rand = new Random();

	// Create main explosion
	engine.spawnExplosion(
		point,
		new Vector2f(0, 0),
		explosionInnerColor,
		150f,
		0.8f
	);

	engine.spawnExplosion(
		point,
		new Vector2f(0, 0),
		explosionOuterColor,
		100f,
		0.5f
	);

	// Add particle effects - flower-like pattern
	float numPetals = 8f;
	float angleIncrement = 360f / numPetals;

	for (int i = 0; i < numPetals; i++) {
	    float angle = i * angleIncrement + rand.nextFloat() * 15f;
	    float distance = 30f + rand.nextFloat() * 20f;

	    Vector2f petalPos = new Vector2f(
		    point.x + (float)Math.cos(Math.toRadians(angle)) * distance,
		    point.y + (float)Math.sin(Math.toRadians(angle)) * distance
	    );

	    // Choose colors based on variant
	    Color largeColor = isComet ? new Color(255, 100, 0, 180) : Util.FLOWER_LARGE_COLOR;
	    Color mediumColor = isComet ? new Color(255, 50, 0, 180) : Util.FLOWER_MEDIUM_COLOR;
	    Color smallColor = isComet ? new Color(255, 200, 0, 180) : Util.FLOWER_SMALL_COLOR;

	    // Large petal
	    engine.addNebulaParticle(
		    petalPos,
		    new Vector2f(
			    (float)Math.cos(Math.toRadians(angle)) * 20f,
			    (float)Math.sin(Math.toRadians(angle)) * 20f
		    ),
		    40f + rand.nextFloat() * 10f,
		    1.5f,
		    0.2f,
		    0.3f + rand.nextFloat() * 0.2f,
		    0.5f + rand.nextFloat() * 0.5f,
		    largeColor
	    );

	    // Medium petal
	    engine.addNebulaParticle(
		    petalPos,
		    new Vector2f(
			    (float)Math.cos(Math.toRadians(angle + 10f)) * 15f,
			    (float)Math.sin(Math.toRadians(angle + 10f)) * 15f
		    ),
		    30f + rand.nextFloat() * 5f,
		    1.2f,
		    0.1f,
		    0.2f + rand.nextFloat() * 0.1f,
		    0.3f + rand.nextFloat() * 0.3f,
		    mediumColor
	    );

	    // Small center
	    if (i % 2 == 0) {
		engine.addNebulaParticle(
			point,
			new Vector2f(
				(float)Math.cos(Math.toRadians(angle)) * 5f,
				(float)Math.sin(Math.toRadians(angle)) * 5f
			),
			20f + rand.nextFloat() * 5f,
			1.0f,
			0.1f,
			0.1f + rand.nextFloat() * 0.1f,
			0.2f + rand.nextFloat() * 0.2f,
			smallColor
		);
	    }
	}
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
	// Determine color scheme based on weapon ID
	isComet = false;
	explosionInnerColor = CYAN_AQUA;
	explosionOuterColor = BLUE;

	if (weapon != null && weapon.getId() != null && weapon.getId().contains("_hidden")) {
	    explosionInnerColor = ORANGE_RED;
	    explosionOuterColor = RED;
	    isComet = true;
	}

	// Apply visual effects to the projectile at launch
	if (projectile != null) {
	    // Add engine glow effect that matches our color scheme
	    float size = projectile.getCollisionRadius() * 2.0f;

	    // Add particle effect to the missile
	    engine.addHitParticle(
		    projectile.getLocation(),
		    new Vector2f(0, 0),
		    size * 0.5f,
		    0.8f,
		    0.1f,
		    explosionInnerColor
	    );

	    engine.addHitParticle(
		    projectile.getLocation(),
		    new Vector2f(0, 0),
		    size * 0.7f,
		    0.5f,
		    0.2f,
		    explosionOuterColor
	    );

	    // Add a particle controller to the projectile to maintain the glow effect
	    engine.addPlugin(new BaseEveryFrameCombatPlugin() {
		private DamagingProjectileAPI proj = projectile;

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
		    if (engine.isPaused()) return;

		    // If projectile is gone, remove this plugin
		    if (proj == null || !engine.isEntityInPlay(proj)) {
			engine.removePlugin(this);
			return;
		    }

		    // Add continuous glow effect
		    if (rand.nextFloat() < 0.5f) {
			engine.addHitParticle(
				proj.getLocation(),
				new Vector2f(0, 0),
				5f + rand.nextFloat() * 5f,
				0.8f,
				0.05f + rand.nextFloat() * 0.05f,
				explosionInnerColor
			);

			engine.addHitParticle(
				proj.getLocation(),
				new Vector2f(0, 0),
				7f + rand.nextFloat() * 7f,
				0.5f,
				0.1f + rand.nextFloat() * 0.1f,
				explosionOuterColor
			);
		    }
		}
	    });
	}
    }
}