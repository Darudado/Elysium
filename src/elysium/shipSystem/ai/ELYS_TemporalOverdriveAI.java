package elysium.shipSystem.ai;

import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;

/**
 * Simplified AI for the Temporal Overdrive system.
 * - Primary focus: Prevent overload by disabling system at high flux
 * - Secondary: Activate when enemies are nearby or missiles are incoming
 */
public class ELYS_TemporalOverdriveAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipSystemAPI system;

    // Very frequent checks to catch dangerous flux levels quickly
    private IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);

    // Very basic flux thresholds - single threshold for all ships
    private static final float DISABLE_FLUX_LEVEL = 0.85f; // Disable at 75% flux

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
	this.ship = ship;
	this.flags = flags;
	this.engine = engine;
	this.system = ship.getPhaseCloak();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
	// Check frequently to catch dangerous flux levels
	tracker.advance(amount);
	if (!tracker.intervalElapsed()) return;

	boolean systemActive = system.isActive();
	float fluxLevel = ship.getFluxTracker().getFluxLevel();
	boolean isLargeShip = ship.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP ||
		ship.getHullSize() == ShipAPI.HullSize.CRUISER;

	// ==============================
	// TOP PRIORITY: AVOID OVERLOAD
	// ==============================
	if (systemActive) {
	    // Use same flux threshold for all ships
	    if (fluxLevel >= DISABLE_FLUX_LEVEL) {
		ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
		return; // Exit immediately after giving command
	    }
	}

	// ==============================
	// ACTIVATION LOGIC
	// ==============================
	if (!systemActive) {
	    // Don't activate if flux is already too high
	    if (fluxLevel >= DISABLE_FLUX_LEVEL - 0.05f) {
		return;
	    }

	    // Check for activation triggers
	    boolean shouldActivate = false;

	    // 1. Missiles incoming
	    if (areMissilesIncoming()) {
		shouldActivate = true;
	    }

	    // 2. Enemies nearby
	    else if (areEnemiesNearby(isLargeShip)) {
		shouldActivate = true;
	    }

	    // 3. Danger flags from AI
	    else if (flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) ||
		    flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) {
		shouldActivate = true;
	    }

	    // Activate if needed
	    if (shouldActivate) {
		ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
		return; // Exit immediately after giving command
	    }
	}
    }

    /**
     * Check for incoming missiles, including damage assessment
     */
    private boolean areMissilesIncoming() {
	List<MissileAPI> missiles = engine.getMissiles();
	if (missiles.isEmpty()) return false;

	float dangerRadius = ship.getCollisionRadius() * 5f;

	for (MissileAPI missile : missiles) {
	    // Skip friendly missiles
	    if (missile.getOwner() == ship.getOwner()) continue;

	    // Simple distance check
	    float distance = Vector2f.sub(ship.getLocation(), missile.getLocation(), new Vector2f()).length();
	    if (distance < dangerRadius) {
		// Check missile damage - dodge high damage missiles even if just one
		if (missile.getDamageAmount() > 2000) {
		    return true;
		}

		// For regular missiles, need at least 2
		int missileCount = 0;
		for (MissileAPI otherMissile : missiles) {
		    if (otherMissile == missile || otherMissile.getOwner() == ship.getOwner()) continue;

		    float otherDistance = Vector2f.sub(ship.getLocation(), otherMissile.getLocation(), new Vector2f()).length();
		    if (otherDistance < dangerRadius) {
			missileCount++;
			if (missileCount >= 3) return true;
		    }
		}
	    }
	}

	return false;
    }

    /**
     * Simple check for nearby enemies with same range for all ship sizes
     */
    private boolean areEnemiesNearby(boolean isLargeShip) {
	List<ShipAPI> ships = engine.getShips();
	float detectionRange = 1000f; // Same detection range for all ships

	for (ShipAPI other : ships) {
	    // Skip allies and destroyed ships
	    if (other.getOwner() == ship.getOwner() || !other.isAlive()) continue;

	    // Simple distance check
	    float distance = Vector2f.sub(ship.getLocation(), other.getLocation(), new Vector2f()).length();
	    if (distance < detectionRange) {
		return true;
	    }
	}

	return false;
    }
}