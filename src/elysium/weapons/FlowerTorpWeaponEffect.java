package elysium.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import elysium.Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Special effect for Elysium's Flower Torpedo weapons
 * Handles the weapon charge effect and pre-launch visuals
 */
public class FlowerTorpWeaponEffect implements EveryFrameWeaponEffectPlugin {
    // Standard Elysium colors (cyan/blue)
    private static final Color CYAN_AQUA = new Color(0, 255, 255); // Cyan/Aqua
    private static final Color BLUE = new Color(0, 150, 255); // Blue

    // Comet variant colors (red/orange)
    private static final Color ORANGE_RED = new Color(255, 100, 0); // Orange-Red
    private static final Color RED = new Color(255, 50, 0); // Red

    private List<Float> chargeLevel = new ArrayList<>();
    private boolean wasCharging = false;
    private boolean runOnce = false;
    private Random rand = new Random();

    // Used to store colors for the current instance
    private Color chargeInnerColor;
    private Color chargeOuterColor;
    private boolean isComet = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	if (engine.isPaused() || weapon == null) {
	    return;
	}

	// Determine which color scheme to use based on weapon ID
	determineColorScheme(weapon);

	// Initialize charge levels if not done already
	if (!runOnce) {
	    runOnce = true;
	    int barrels = weapon.getSpec().getTurretAngleOffsets().size();
	    for (int i = 0; i < barrels; i++) {
		chargeLevel.add(0f);
	    }
	}

	// Get weapon state
	boolean isCharging = weapon.getChargeLevel() > 0;

	// Charging build-up effect
	if (isCharging) {
	    float intensity = weapon.getChargeLevel() * 0.8f;

	    // For each barrel of the weapon
	    for (int i = 0; i < Math.min(chargeLevel.size(), weapon.getSpec().getHardpointAngleOffsets().size()); i++) {
		// Increase charge per barrel gradually
		float currentCharge = chargeLevel.get(i);

		// Different charge sequence depending on barrel
		float targetCharge = Math.min(weapon.getChargeLevel() * (1f + (0.15f * i)), 1f);
		float chargeRate = 2.0f + (i * 0.5f);

		// Update charge level with smoother transition
		currentCharge = Misc.interpolate(currentCharge, targetCharge, chargeRate * amount);
		chargeLevel.set(i, currentCharge);

		// Calculate barrel position
		Vector2f barrelLoc = weapon.getLocation();
		if (weapon.getSlot() != null) {
		    float angle = weapon.getCurrAngle();

		    // Get the position of this specific barrel
		    if (weapon.getSlot().isHardpoint()) {
			Vector2f offset = weapon.getSpec().getHardpointFireOffsets().get(i);
			float barrelAngle = angle + weapon.getSpec().getHardpointAngleOffsets().get(i);

			barrelLoc = new Vector2f(
				weapon.getLocation().x + offset.x * (float)Math.cos(Math.toRadians(barrelAngle)) -
					offset.y * (float)Math.sin(Math.toRadians(barrelAngle)),
				weapon.getLocation().y + offset.x * (float)Math.sin(Math.toRadians(barrelAngle)) +
					offset.y * (float)Math.cos(Math.toRadians(barrelAngle))
			);
		    } else if (weapon.getSlot().isTurret()) {
			Vector2f offset = weapon.getSpec().getTurretFireOffsets().get(i);
			float barrelAngle = angle + weapon.getSpec().getTurretAngleOffsets().get(i);

			barrelLoc = new Vector2f(
				weapon.getLocation().x + offset.x * (float)Math.cos(Math.toRadians(barrelAngle)) -
					offset.y * (float)Math.sin(Math.toRadians(barrelAngle)),
				weapon.getLocation().y + offset.x * (float)Math.sin(Math.toRadians(barrelAngle)) +
					offset.y * (float)Math.cos(Math.toRadians(barrelAngle))
			);
		    }
		}

		// Only show effects if visible enough
		if (currentCharge > 0.1f) {
		    // Create charging visual effects for this barrel
		    createChargingEffect(engine, barrelLoc, currentCharge);
		}
	    }
	} else if (wasCharging) {
	    // Reset charge levels when no longer charging
	    for (int i = 0; i < chargeLevel.size(); i++) {
		chargeLevel.set(i, 0f);
	    }
	}

	// Update weapon state
	wasCharging = isCharging;
    }

    private void determineColorScheme(WeaponAPI weapon) {
	// Default to standard colors
	chargeInnerColor = CYAN_AQUA;
	chargeOuterColor = BLUE;
	isComet = false;

	// Check if this is the Comet variant
	if (weapon != null && weapon.getId() != null && weapon.getId().contains("_hidden")) {
	    chargeInnerColor = ORANGE_RED;
	    chargeOuterColor = RED;
	    isComet = true;
	}
    }

    private void createChargingEffect(CombatEngineAPI engine, Vector2f position, float chargeLevel) {
	// Parameters that scale with charge level
	float visualIntensity = chargeLevel * 0.8f;
	float particleSize = 3f + (chargeLevel * 15f);
	float duration = 0.1f + (chargeLevel * 0.3f);

	// Inner glow
	engine.addSmoothParticle(
		position,
		new Vector2f(0, 0),
		particleSize * 0.7f,
		visualIntensity * 0.7f,
		duration * 0.5f,
		chargeInnerColor
	);

	// Outer glow
	engine.addSmoothParticle(
		position,
		new Vector2f(0, 0),
		particleSize * 1.2f,
		visualIntensity * 0.5f,
		duration * 0.7f,
		chargeOuterColor
	);

	// Add some jitter effect
	if (rand.nextFloat() < chargeLevel * 0.3f) {
	    float angle = rand.nextFloat() * 360f;
	    float distance = rand.nextFloat() * 10f * chargeLevel;

	    Vector2f jitterPos = new Vector2f(
		    position.x + (float)Math.cos(Math.toRadians(angle)) * distance,
		    position.y + (float)Math.sin(Math.toRadians(angle)) * distance
	    );

	    // Choose jitter color based on variant
	    Color jitterColor = isComet ? new Color(255, 150, 0, 75) : Util.JITTER_COLOR;

	    engine.addSmoothParticle(
		    jitterPos,
		    new Vector2f(0, 0),
		    particleSize * 0.5f,
		    visualIntensity * 0.4f,
		    duration * 0.3f,
		    jitterColor
	    );
	}

	// Add occasional small flower petal effect when highly charged
	if (chargeLevel > 0.7f && rand.nextFloat() < 0.15f) {
	    float petalAngle = rand.nextFloat() * 360f;
	    float petalDist = 10f + (rand.nextFloat() * 10f);

	    Vector2f petalPos = new Vector2f(
		    position.x + (float)Math.cos(Math.toRadians(petalAngle)) * petalDist,
		    position.y + (float)Math.sin(Math.toRadians(petalAngle)) * petalDist
	    );

	    // Choose petal color based on variant
	    Color petalColor = isComet ? new Color(255, 100, 0, 180) : Util.FLOWER_MEDIUM_COLOR;

	    engine.addNebulaParticle(
		    petalPos,
		    new Vector2f(
			    (float)Math.cos(Math.toRadians(petalAngle)) * 10f * chargeLevel,
			    (float)Math.sin(Math.toRadians(petalAngle)) * 10f * chargeLevel
		    ),
		    10f + (rand.nextFloat() * 5f),
		    1.5f,
		    0.1f,
		    0.3f * chargeLevel,
		    0.3f + (0.2f * chargeLevel),
		    petalColor
	    );
	}
    }
}