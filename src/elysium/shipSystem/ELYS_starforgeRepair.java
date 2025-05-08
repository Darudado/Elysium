package elysium.shipSystem;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import elysium.Util;
import org.lazywizard.lazylib.combat.CombatUtils;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicFakeBeam;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.magiclib.util.MagicFakeBeam.*;

public class ELYS_starforgeRepair extends BaseShipSystemScript {
    // Configuration
    private static final float REPAIR_RANGE = 1000f;             // Range in units
    private static final float REPAIR_AMOUNT = 100f;
    private static final float REPAIR_AMOUNT_CAPITAL = 1000f;
    private static final float REPAIR_FLUX_RATIO = 3f;           // Hardflux generated per hull point repaired
    private static final float MAX_FLUX_PERCENT = 0.8f;          // Max flux level before the system stops repairing
    private static final Color BEAM_COLOR = new Color(180, 80, 255);  // Purple
    private static final Color CLOUD_COLOR = new Color(80, 255, 120); // Green

    // Internal variables
    private ShipAPI target = null;
    private float totalRepaired = 0f;
    private IntervalUtil effectInterval = new IntervalUtil(0.05f, 0.1f);  // Visual effects timing
    private IntervalUtil empInterval = new IntervalUtil(0.2f, 0.3f);      // EMP arc timing

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	if (effectLevel <= 0f) return;

	ShipAPI ship = (ShipAPI) stats.getEntity();
	CombatEngineAPI engine = Global.getCombatEngine();

	if (engine == null || ship == null) return;

	// Only perform targeting on the first frame or if target is destroyed
	if (state == State.IN || target == null || !engine.isEntityInPlay(target) || target.isHulk()) {
	    pickTarget(ship, engine);
	}

	// If we have a target, repair it
	if (target != null) {
	    // Check if we should stop due to high flux
	    if (ship.getFluxLevel() >= MAX_FLUX_PERCENT) {
		addSystemMessage(engine, ship, "FLUX CRITICAL - REPAIR PAUSED");
		return;
	    }

	    // Check if target is at full health
	    if (target.getHitpoints() >= target.getMaxHitpoints()) {
		addSystemMessage(engine, ship, "TARGET AT FULL HEALTH");
		target = null;
		return;
	    }

	    // Check if target is still in range
	    if (Misc.getDistance(ship.getLocation(), target.getLocation()) > REPAIR_RANGE) {
		addSystemMessage(engine, ship, "TARGET OUT OF RANGE");
		target = null;
		return;
	    }

	    // Calculate repair amount for this frame
	    float repairThisFrame = (ship.getHullSize()== HullSize.FRIGATE) ? REPAIR_AMOUNT * Global.getCombatEngine().getElapsedInLastFrame() : REPAIR_AMOUNT_CAPITAL * Global.getCombatEngine().getElapsedInLastFrame() ;
	    float remainingDamage = target.getMaxHitpoints() - target.getHitpoints();
	    repairThisFrame = Math.min(repairThisFrame, remainingDamage);

	    if (repairThisFrame > 0) {
		// Apply hull repair
		target.setHitpoints(target.getHitpoints() + repairThisFrame);
		totalRepaired += repairThisFrame;

		// Generate hard flux on the ship using the system
		float fluxToAdd = repairThisFrame * REPAIR_FLUX_RATIO;
		ship.getFluxTracker().increaseFlux(fluxToAdd, true);

		// Visual effects
		handleVisualEffects(ship, target, engine, effectLevel);
	    }
	}
    }

    private void pickTarget(ShipAPI ship, CombatEngineAPI engine) {
	target = null;

	List<ShipAPI> potentialTargets = new ArrayList<>();
	for (ShipAPI potentialTarget : engine.getShips()) {
	    // Skip invalid targets
	    if (potentialTarget.isHulk() ||
		    potentialTarget.getOwner() != ship.getOwner() ||
		    potentialTarget == ship ||
		    !potentialTarget.isAlive() || potentialTarget.getHullSize() == HullSize.FIGHTER) {
		continue;
	    }

	    // Check if target needs repair
	    float targetHullLevel = potentialTarget.getHitpoints() / potentialTarget.getMaxHitpoints();
	    if (targetHullLevel >= 0.99f) continue; // Skip ships at full health

	    // Check if target is in range
	    float distance = Misc.getDistance(ship.getLocation(), potentialTarget.getLocation());
	    if (distance <= REPAIR_RANGE) {
		potentialTargets.add(potentialTarget);
	    }
	}

	// Pick the most damaged target in range
	float lowestHullPercent = 1f;
	for (ShipAPI potentialTarget : potentialTargets) {
	    float hullPercent = potentialTarget.getHitpoints() / potentialTarget.getMaxHitpoints();
	    if (hullPercent < lowestHullPercent) {
		lowestHullPercent = hullPercent;
		target = potentialTarget;
	    }
	}

	if (target != null) {
	    totalRepaired = 0f;
	    addSystemMessage(engine, ship, "Repairing " + target.getHullSpec().getHullName());
	}
    }

    private void handleVisualEffects(ShipAPI ship, ShipAPI target, CombatEngineAPI engine, float effectLevel) {
	// Beam effect from ship to target
	Vector2f shipCenter = ship.getLocation();
	Vector2f targetCenter = target.getLocation();

	// Update effect timers
	float elapsed = engine.getElapsedInLastFrame();
	effectInterval.advance(elapsed);
	empInterval.advance(elapsed);

	// Render beam
	if (effectInterval.intervalElapsed()) {
	    engine.addSmoothParticle(shipCenter,
		    new Vector2f(0, 0),
		    20f * effectLevel,
		    0.7f * effectLevel,
		    0.1f,
		    BEAM_COLOR);

 	    float distance = Util.getDistanceBetweenShips(ship, target)+200f;
	    float angleradians = Util.getAngleBetweenShips(ship, target);
	    // Convert to degrees for MagicFakeBeam
	    float angle = (float) Math.toDegrees(angleradians);
	    MagicFakeBeam.spawnFakeBeam(
		    engine,           // CombatEngineAPI - The current combat engine
		    shipCenter,       // Vector2f from - Starting point of the beam
		    distance,         // float range - Calculated distance to target
		    angle,            // float angle - Calculated angle to target
		    30f * effectLevel,// float width - Beam width (scales with effect level)
		    0.1f,             // float full - Short duration at full opacity
		    0.1f,             // float fading - Short fade time
		    20f * effectLevel,// float impactSize - Impact glow size
		    new Color(200, 180, 255, 200), // Color core - Bright purple core
		    BEAM_COLOR,       // Color fringe - Original purple as fringe
		    0f,               // float normalDamage - No damage (handled separately)
		    DamageType.ENERGY,// DamageType type - Energy type (though no damage)
		    0f,               // float emp - No EMP damage
		    ship              // ShipAPI source - The ship using the system
	    );

	    // Spawn green repair cloud at target
	    float size = 30f + 20f * effectLevel;
	    engine.addNebulaParticle(targetCenter,
		    target.getVelocity(),
		    size,
		    1.5f,
		    0.5f,
		    0.5f,
		    0.6f * effectLevel,
		    CLOUD_COLOR);
	}

	// EMP arcs at target location
	if (empInterval.intervalElapsed()) {
	    float empArcChance = 0.6f * effectLevel;
	    if (Math.random() < empArcChance) {
		// Create short EMP arcs around the target
		float arcRange = target.getCollisionRadius() * 0.6f;
		Vector2f arcStart = new Vector2f(targetCenter.x + (float)Math.random() * arcRange - arcRange/2,
			targetCenter.y + (float)Math.random() * arcRange - arcRange/2);
		Vector2f arcEnd = new Vector2f(targetCenter.x + (float)Math.random() * arcRange - arcRange/2,
			targetCenter.y + (float)Math.random() * arcRange - arcRange/2);

		engine.spawnEmpArcVisual(arcStart,
			target,
			arcEnd,
			null,
			5f, // thickness
			CLOUD_COLOR,
			new Color(255, 255, 255, 200)); // core color
	    }
	}
    }

    private void addSystemMessage(CombatEngineAPI engine, ShipAPI ship, String message) {
	if (ship == engine.getPlayerShip()) {
	    engine.addFloatingText(ship.getLocation(), message, 20f, Color.GREEN, ship, 1f, 2f);
	}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
	target = null;
	totalRepaired = 0f;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
	if (index == 0 && target != null) {
	    return new StatusData("Repairing " + target.getHullSpec().getHullName(), false);
	} else if (index == 1 && totalRepaired > 0) {
	    return new StatusData("Repaired: " + (int)totalRepaired + " hull", false);
	}
	return null;
    }
}