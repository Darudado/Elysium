package elysium.shipSystem.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class ELYS_starforgeRepairAI implements ShipSystemAIScript {

	private ShipAPI ship;
	private ShipSystemAPI system;
	private CombatEngineAPI engine;

	private static final float REPAIR_RANGE = 1000f;           // Match value in ElysiumHullRepairSystem
	private static final float MAX_FLUX_PERCENT = 0.75f;       // Slightly lower threshold for AI than the system itself
	private static final float DAMAGE_THRESHOLD = 0.1f;        // Minimum amount of damage to activate (10% hull damage)
	private static final float HIGH_PRIORITY_DAMAGE = 0.4f;    // Hull damage level considered high priority

	private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);  // Decision-making interval
	private ShipAPI bestTarget = null;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
	    this.ship = ship;
	    this.system = system;
	    this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
	    if (engine == null || system == null || ship == null) return;
	    if (engine.isPaused() || !ship.isAlive()) return;

	    // Only make decisions at fixed intervals to reduce performance impact
	    tracker.advance(amount);
	    if (!tracker.intervalElapsed()) return;

	    // Don't activate system if flux is too high
	    if (ship.getFluxLevel() >= MAX_FLUX_PERCENT) {
		if (system.isActive()) {
		    ship.useSystem();  // Deactivate if already active
		}
		return;
	    }

	    // Find potential targets
	    bestTarget = findBestRepairTarget();

	    // Decide whether to activate
	    if (bestTarget != null) {
		// If the system is already active, keep it active as long as target is valid
		if (system.isActive()) return;

		// Calculate hull damage percentage of target
		float targetHullDamagePercent = 1f - (bestTarget.getHitpoints() / bestTarget.getMaxHitpoints());

		// Only activate if damage exceeds threshold
		if (targetHullDamagePercent >= DAMAGE_THRESHOLD) {
		    ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.ESCORT_OTHER_SHIP, 2f, bestTarget);
		    ship.useSystem();
		}
	    } else if (system.isActive()) {
		// Deactivate if no valid targets and system is active
		ship.useSystem();
	    }
	}

	private ShipAPI findBestRepairTarget() {
	    if (engine == null) return null;

	    List<ShipAPI> potentialTargets = new ArrayList<>();
	    for (ShipAPI otherShip : engine.getShips()) {
		// Skip invalid targets
		if (otherShip.isHulk() ||
			otherShip.getOwner() != ship.getOwner() ||
			otherShip == ship ||
			!otherShip.isAlive()) {
		    continue;
		}

		// Check if target needs repair
		float targetHullPercent = otherShip.getHitpoints() / otherShip.getMaxHitpoints();
		if (targetHullPercent >= 0.99f) continue; // Skip ships at full health

		// Check if target is in range
		float distance = Misc.getDistance(ship.getLocation(), otherShip.getLocation());
		if (distance <= REPAIR_RANGE) {
		    potentialTargets.add(otherShip);
		}
	    }

	    // No valid targets
	    if (potentialTargets.isEmpty()) return null;

	    // First priority: Check for high priority targets (ships with severe damage)
	    ShipAPI highPriorityTarget = null;
	    float highestDamage = 0f;

	    for (ShipAPI potentialTarget : potentialTargets) {
		float hullDamagePercent = 1f - (potentialTarget.getHitpoints() / potentialTarget.getMaxHitpoints());

		// Find the most damaged high-priority ship
		if (hullDamagePercent >= HIGH_PRIORITY_DAMAGE && hullDamagePercent > highestDamage) {
		    highestDamage = hullDamagePercent;
		    highPriorityTarget = potentialTarget;
		}
	    }

	    // If we found a high priority target, use it
	    if (highPriorityTarget != null) {
		return highPriorityTarget;
	    }

	    // Second priority: Just pick the most damaged target
	    ShipAPI mostDamagedTarget = null;
	    highestDamage = 0f;

	    for (ShipAPI potentialTarget : potentialTargets) {
		float hullDamagePercent = 1f - (potentialTarget.getHitpoints() / potentialTarget.getMaxHitpoints());

		if (hullDamagePercent > highestDamage) {
		    highestDamage = hullDamagePercent;
		    mostDamagedTarget = potentialTarget;
		}
	    }

	    return mostDamagedTarget;
	}
    }