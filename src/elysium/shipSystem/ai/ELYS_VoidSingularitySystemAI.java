package elysium.shipSystem.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class ELYS_VoidSingularitySystemAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;

    private final IntervalUtil tracker = new IntervalUtil(0.5f, 1.5f);
    private final float MAX_TARGET_RANGE = 1200f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
	this.ship = ship;
	this.system = system;
	this.engine = engine;
    }


    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
	if (engine.isPaused() || ship.getShipAI() == null) return;

	// Don't use if system is on cooldown or already active
	if (!AIUtils.canUseSystemThisFrame(ship)) return;

	tracker.advance(amount);
	if (!tracker.intervalElapsed()) return;

	// High flux is a good time to use this system as it vents flux
	float fluxLevel = ship.getFluxLevel();
	boolean highFlux = fluxLevel > 0.7f;
	boolean mediumFlux = fluxLevel > 0.5f;

	// Target evaluation
	ShipAPI bestTarget = null;
	Vector2f bestPosition = null;
	float bestScore = 0f;

	List<ShipAPI> enemies = AIUtils.getEnemiesOnMap(ship);

	// 1. Find valuable ship targets
	for (ShipAPI enemy : enemies) {
	    if (!enemy.isAlive() || enemy.isPhased()) continue;

	    float distance = MathUtils.getDistance(ship, enemy);
	    if (distance > MAX_TARGET_RANGE) continue; // Target must be within range

	    // Base score on hull size
	    float score = getScoreForHullSize(enemy.getHullSize());

	    // Prioritize overloaded or high flux targets
	    if (enemy.getFluxTracker().isOverloaded()) {
		score *= 2.0f;
	    } else if (enemy.getFluxLevel() > 0.7f) {
		score *= 1.5f;
	    }

	    // Evaluate nearby clusters of enemies around this target
	    float clusterBonus = evaluateCluster(enemy.getLocation(), enemies, 800f);
	    score += clusterBonus;

	    // Adjust score by distance - moderate range is ideal
	    score *= getDistanceMultiplier(distance);

	    if (score > bestScore) {
		bestScore = score;
		bestTarget = enemy;
		bestPosition = new Vector2f(enemy.getLocation());
	    }
	}

	// 2. Evaluate potential missile clusters even without ships
	if (bestScore < 300f) {
	    for (MissileAPI missile : engine.getMissiles()) {
		if (missile.getOwner() == ship.getOwner()) continue; // Skip friendly missiles

		float distance = MathUtils.getDistance(ship, missile);
		if (distance > MAX_TARGET_RANGE) continue;

		// Check for missile clusters
		float missileClusterScore = 0f;
		for (MissileAPI otherMissile : engine.getMissiles()) {
		    if (otherMissile == missile || otherMissile.getOwner() == ship.getOwner()) continue;

		    float missileDist = MathUtils.getDistance(missile, otherMissile);
		    if (missileDist < 500f) {
			missileClusterScore += 50f * (1f - missileDist/500f);
		    }
		}

		if (missileClusterScore > bestScore) {
		    bestScore = missileClusterScore;
		    bestTarget = null;
		    bestPosition = new Vector2f(missile.getLocation());
		}
	    }
	}

	// Decision to use the system
	boolean shouldUse = false;

	// Use system in high flux situations with a decent target
	if (highFlux && bestScore > 200f) {
	    shouldUse = true;
	}
	// Use system for very high value targets regardless of flux
	else if (bestScore > 500f) {
	    shouldUse = true;
	}
	// Use system for good value targets with medium flux
	else if (mediumFlux && bestScore > 300f) {
	    shouldUse = true;
	}

	if (shouldUse && bestPosition != null) {
	    // Small random offset to make AI targeting less predictable
	    bestPosition.x += MathUtils.getRandomNumberInRange(-50f, 50f);
	    bestPosition.y += MathUtils.getRandomNumberInRange(-50f, 50f);

	    // Set the target coordinates flag for the system to use
	    ship.getAIFlags().setFlag(AIFlags.SYSTEM_TARGET_COORDS, 1,bestPosition);
	    ship.useSystem();
	}
    }

    // Helper methods
    private float getScoreForHullSize(ShipAPI.HullSize hullSize) {
	switch (hullSize) {
	    case CAPITAL_SHIP: return 500f;
	    case CRUISER: return 300f;
	    case DESTROYER: return 200f;
	    case FRIGATE: return 100f;
	    default: return 50f;
	}
    }

    private float evaluateCluster(Vector2f position, List<ShipAPI> ships, float radius) {
	float score = 0f;
	int shipCount = 0;

	for (ShipAPI other : ships) {
	    if (!other.isAlive() || other.isPhased()) continue;

	    float distance = MathUtils.getDistance(position, other.getLocation());
	    if (distance < radius) {
		shipCount++;
		score += getScoreForHullSize(other.getHullSize()) * (1f - distance/radius);
	    }
	}

	// Bonus for multiple ships in cluster
	if (shipCount > 1) {
	    score *= 1f + ((shipCount - 1) * 0.2f);
	}

	return score;
    }

    private float getDistanceMultiplier(float distance) {
	// Prefer targets at medium range - not too close, not at maximum range
	if (distance < 300f) {
	    // Too close - reduced score
	    return 0.5f + (distance / 300f) * 0.5f;
	} else if (distance > 900f) {
	    // Far away - reduced score
	    return 1f - ((distance - 900f) / 300f) * 0.6f;
	}
	// Ideal range
	return 1f;
    }
}