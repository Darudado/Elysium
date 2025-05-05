package elysium.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import elysium.Util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Special beam effect that can hit phase ships with continuous damage.
 * Also applies phase disruption to affected ships.
 */
public class EtherealBeamEffect implements BeamEffectPlugin {
    // Constants for beam behavior
    private static final float PHASE_DISRUPTION_TIME = 0.2f; // Time in seconds added to phase cooldown per hit
    private static final float EFFECT_INTERVAL = 0.1f;       // Visual effect interval in seconds

    // Debug flag - set to true to print debug info to console
    private static final boolean DEBUG = true;

    // Tracking when we last did hit effects for each ship to avoid visual spam
    private final Map<ShipAPI, Float> lastEffectTimeMap = new HashMap<>();


    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
	if (engine.isPaused()) return;

	// Get beam source and weapon info
	ShipAPI source = beam.getSource();
	if (source == null) return;


	// Apply effects to phased ships in beam path
	applyEffectsToPhasedShips(engine, beam, amount);
    }

    /**
     * Custom line-circle intersection test that ignores phase state
     * @param from Start point of line
     * @param to End point of line
     * @param center Circle center
     * @param radius Circle radius
     * @return True if the line intersects the circle
     */
    private boolean lineIntersectsCircle(Vector2f from, Vector2f to, Vector2f center, float radius) {
	// Vector from line start to circle center
	float dx = center.x - from.x;
	float dy = center.y - from.y;

	// Direction vector of the line
	float dirX = to.x - from.x;
	float dirY = to.y - from.y;

	// Normalize direction vector
	float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
	if (length == 0) return false; // Zero length line

	dirX /= length;
	dirY /= length;

	// Calculate dot product (projection of center-from onto the line direction)
	float dot = dx * dirX + dy * dirY;

	// Find closest point on line to circle center
	float closestX = from.x + dot * dirX;
	float closestY = from.y + dot * dirY;

	// Check if closest point is on the line segment
	boolean onSegment = false;
	if (dot >= 0 && dot <= length) {
	    onSegment = true;
	}

	// If closest point is not on segment, check endpoints
	if (!onSegment) {
	    // Check if either endpoint is within radius
	    float distFromSq = (from.x - center.x) * (from.x - center.x) + (from.y - center.y) * (from.y - center.y);
	    float distToSq = (to.x - center.x) * (to.x - center.x) + (to.y - center.y) * (to.y - center.y);
	    return distFromSq <= radius * radius || distToSq <= radius * radius;
	}

	// Distance from closest point to circle center
	float distSq = (closestX - center.x) * (closestX - center.x) + (closestY - center.y) * (closestY - center.y);

	// Return true if distance is less than or equal to radius
	return distSq <= radius * radius;
    }

    /**
     * Find approximate hit point for a beam intersecting a ship
     */
    private Vector2f findHitPoint(Vector2f from, Vector2f to, ShipAPI ship) {
	Vector2f center = ship.getLocation();
	float radius = ship.getCollisionRadius();

	// Use closest point on line to circle center as hit point
	Vector2f lineDir = new Vector2f(to.x - from.x, to.y - from.y);
	float length = (float) Math.sqrt(lineDir.x * lineDir.x + lineDir.y * lineDir.y);
	if (length == 0) return new Vector2f(from); // Return from if line has zero length

	lineDir.x /= length;
	lineDir.y /= length;

	float dx = center.x - from.x;
	float dy = center.y - from.y;
	float dot = dx * lineDir.x + dy * lineDir.y;

	if (dot < 0) {
	    // Circle center is behind line start
	    return new Vector2f(from);
	} else if (dot > length) {
	    // Circle center is beyond line end
	    return new Vector2f(to);
	} else {
	    // Find closest point on line
	    float x = from.x + dot * lineDir.x;
	    float y = from.y + dot * lineDir.y;

	    // Create a vector pointing from closest point to circle center
	    Vector2f toCenter = new Vector2f(center.x - x, center.y - y);
	    float distToCenter = (float) Math.sqrt(toCenter.x * toCenter.x + toCenter.y * toCenter.y);

	    // Normalize direction to center
	    if (distToCenter > 0) {
		toCenter.x /= distToCenter;
		toCenter.y /= distToCenter;
	    }

	    // Hit point is on the edge of the collision circle
	    float hitX = center.x - toCenter.x * radius;
	    float hitY = center.y - toCenter.y * radius;

	    return new Vector2f(hitX, hitY);
	}
    }

    private void applyEffectsToPhasedShips(CombatEngineAPI engine, BeamAPI beam, float amount) {
	Vector2f from = beam.getFrom();
	Vector2f to = beam.getTo();

	// Check all ships to find phased ships in beam path
	for (ShipAPI ship : engine.getShips()) {
	    // Skip if ship is not phased, is same team as beam source, is not alive, or was already hit this frame
	    if (!ship.isPhased() || ship.getOwner() == beam.getSource().getOwner() ||
		    !ship.isAlive()) {
		continue;
	    }

	    // Custom intersection test that ignores phase state
	    boolean intersects = lineIntersectsCircle(from, to, ship.getLocation(), ship.getCollisionRadius());

	    if (intersects) {
		Vector2f hitPoint = findHitPoint(from, to, ship);

		// Apply damage to phased ship continuously
		float damage = beam.getDamage().getDamage() * amount;

		engine.applyDamage(
			ship,
			hitPoint,
			damage,
			beam.getDamage().getType(),
			0f,
			true,
			true,  // Does direct hull damage
			beam.getSource()
		);

		// Apply phase system disruption
		applyPhaseDisruption(ship, PHASE_DISRUPTION_TIME * amount, amount);

		// Add visual jitter effect to the target ship
		ship.setJitter(ship, Util.JITTER_COLOR, 4f, 2, 20f);
		ship.setJitterUnder(ship, Util.JITTER_UNDER_COLOR, 4f, 10, 20f);

		// Add visual effects at intervals to avoid overwhelming particles
		Float lastEffectTime = lastEffectTimeMap.get(ship);
		float currentTime = engine.getTotalElapsedTime(false);

		if (lastEffectTime == null || currentTime - lastEffectTime >= EFFECT_INTERVAL) {
		    addPhaseHitEffects(engine, hitPoint);
		    lastEffectTimeMap.put(ship, currentTime);
		}
	    }
	}
    }

    /**
     * Apply phase disruption to the target ship by adding to phase cooldown
     */
    private void applyPhaseDisruption(ShipAPI ship, float disruptionTime, float amount) {
	// Not all phased ships have a phase cloak system, so check first
	if (ship.getPhaseCloak() != null) {
	    // Different disruption effects based on the phase cloak state
	    if (ship.getPhaseCloak().isActive()) {
		// For severe disruption, we might even force unphase
		if (Math.random() < 0.05f * amount) { // chance per hit of severe disruption
		    ship.getPhaseCloak().deactivate();
		}
	    } else {
		// If the ship is not actively phasing, we add to the current cooldown
		float currentCooldown = ship.getPhaseCloak().getCooldownRemaining();
		ship.getPhaseCloak().setCooldownRemaining(currentCooldown + disruptionTime);

		if (DEBUG) {
		    Global.getLogger(EtherealBeamEffect.class).info(
			    "Added " + disruptionTime + "s to phase cooldown for " + ship.getName());
		}
	    }
	}
    }

    private void addPhaseHitEffects(CombatEngineAPI engine, Vector2f hitPoint) {
	// Add main impact flash
	engine.addHitParticle(
		hitPoint,
		new Vector2f(0, 0),
		60f,
		0.7f,
		0.2f,
		Util.FLOWER_LARGE_COLOR
	);

	// Add "petals" radiating outward
	int numPetals = 4; // Reduced number to avoid visual spam during continuous hits
	for (int i = 0; i < numPetals; i++) {
	    float angle = i * 360f / numPetals;
	    Vector2f petalVel = MathUtils.getPointOnCircumference(
		    new Vector2f(0, 0),
		    30f, // Velocity
		    angle
	    );

	    engine.addHitParticle(
		    hitPoint,
		    petalVel,
		    20f + (float)Math.random() * 10f,
		    0.7f,
		    0.3f + (float)Math.random() * 0.2f,
		    Util.FLOWER_MEDIUM_COLOR
	    );
	}

	// Add distortion effect
	engine.addSmoothParticle(
		hitPoint,
		new Vector2f(0, 0),
		40f,
		0.8f,
		0.12f,
		Util.JITTER_UNDER_COLOR
	);

    }
}