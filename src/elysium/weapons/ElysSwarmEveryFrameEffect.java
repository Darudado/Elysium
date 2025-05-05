package elysium.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import elysium.Util;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class ElysSwarmEveryFrameEffect implements EveryFrameWeaponEffectPlugin {

    private final IntervalUtil effectInterval = new IntervalUtil(0.2f, 0.3f); // Longer interval
    private float chargeLevel = 0f;
    private boolean wasFiring = false;
    // Reduced opacity for effect colors using Elysium colors
    private final Color GLOW_COLOR = new Color(0, 255, 255, 70); // Cyan/Aqua with low opacity

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	if (engine.isPaused() || weapon == null) return;

	ShipAPI ship = weapon.getShip();
	if (ship == null) return;

	// Check if the weapon is firing
	boolean isFiring = weapon.isFiring();

	// Handle charging animation
	if (isFiring && !wasFiring) {
	    // Started firing
	    chargeLevel = 0f;
	} else if (isFiring) {
	    // Continue charging - slower build-up
	    chargeLevel = Math.min(1f, chargeLevel + amount * 1.0f);
	} else {
	    // Not firing, reset charge - faster reset
	    chargeLevel = Math.max(0f, chargeLevel - amount * 4.0f);
	}

	wasFiring = isFiring;

	// Apply visual effects based on charge level - with reduced intensity
	if (chargeLevel > 0.1f) {
	    effectInterval.advance(amount);

	    if (effectInterval.intervalElapsed()) {
		// Apply jitter to the weapon - minimal jitter
		if (weapon.getSlot() != null) {
		    float jitterIntensity = chargeLevel * 0.3f; // Greatly reduced jitter
		    Vector2f jitterOffset = MathUtils.getRandomPointInCircle(new Vector2f(0,0), jitterIntensity);
		    weapon.getSprite().setCenter(
			    weapon.getSprite().getWidth() / 2 + jitterOffset.x,
			    weapon.getSprite().getHeight() / 2 + jitterOffset.y);
		}

		// Visual charge effect - smaller and more subtle
		float effectSize = 5f + (10f * chargeLevel); // Smaller effect
		float effectBrightness = 0.4f + (0.3f * chargeLevel); // Less bright

		// Create fewer particles with smaller size
		Vector2f weaponLoc = weapon.getLocation();
		Vector2f barrelEnd = new Vector2f(
			weaponLoc.x + (float)Math.cos(Math.toRadians(weapon.getCurrAngle())) * 10f,
			weaponLoc.y + (float)Math.sin(Math.toRadians(weapon.getCurrAngle())) * 10f
		);

		// Only create particles near the barrel end
		engine.addHitParticle(
			barrelEnd,
			MathUtils.getRandomPointInCircle(new Vector2f(0,0), 15f),
			effectSize,
			effectBrightness,
			0.15f + (0.1f * chargeLevel),
			GLOW_COLOR
		);

		// Add a small glow at the barrel - much less obtrusive
		engine.addSmoothParticle(
			barrelEnd,
			new Vector2f(0, 0),
			10f * chargeLevel,
			0.5f,
			0.05f, // Very short duration
			GLOW_COLOR
		);
	    }
	}

	// For missiles - minimal intervention to avoid visual clutter
	for (MissileAPI missile : engine.getMissiles()) {
	    if (missile.getWeapon() == weapon && !missile.isExpired()) {
		SpriteAPI missileSprite = missile.getSpriteAPI();

		// Just ensure the missile has the right color
		if (missileSprite != null) {
		    missileSprite.setColor(Util.ELYSIUM_PRIMARY);
		}

		// Let the missile AI handle all other effects
	    }
	}
    }
}