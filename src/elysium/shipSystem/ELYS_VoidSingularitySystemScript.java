package elysium.shipSystem;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import elysium.Util;
import elysium.ELYS_VoidSingularityPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ELYS_VoidSingularitySystemScript extends BaseShipSystemScript {
    // Configuration parameters
    private static final float MAX_RANGE = 1200f;
    private static final float SINGULARITY_RADIUS = 1000f;
    private static final float FLUX_VENT_PERCENT = 0.40f; // 40% of current flux

    private static final float BASE_DURATION = 6; // seconds

    // System state tracking
    private boolean activated = false;
    private Vector2f targetLocation = null;

    // For jitter effect
    public static final Color JITTER_COLOR = new Color(0, 180, 255, 75);     // Light blue with 75 alpha
    public static final Color JITTER_UNDER_COLOR = new Color(90, 60, 255, 175);  // Purple with 175 alpha

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	ShipAPI ship = (ShipAPI) stats.getEntity();
	CombatEngineAPI engine = Global.getCombatEngine();

	// Add jitter effect during charge-up
	float jitterLevel = effectLevel;
	if (state == State.OUT) {
	    jitterLevel *= jitterLevel;
	}
	float maxRangeBonus = 25f;
	float jitterRangeBonus = jitterLevel * maxRangeBonus;

	ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
	ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

	if (state == State.IN && !activated) {
	    activated = true;

	    // Get target location - from mouse if player or AI flags if AI
	    Vector2f target = ship.getMouseTarget();
	    if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.SYSTEM_TARGET_COORDS)) {
		target = (Vector2f) ship.getAIFlags().getCustom(AIFlags.SYSTEM_TARGET_COORDS);
	    }

	    if (target != null) {
		targetLocation = new Vector2f(target);

		// Limit target location to be within range
		if (Misc.getDistance(ship.getLocation(), targetLocation) > MAX_RANGE) {
		    Vector2f direction = Vector2f.sub(targetLocation, ship.getLocation(), new Vector2f());
		    direction.normalise();
		    direction.scale(MAX_RANGE);
		    targetLocation = Vector2f.add(ship.getLocation(), direction, new Vector2f());
		}

		// Vent flux
		float currentFlux = ship.getFluxTracker().getCurrFlux();
		float fluxToVent = currentFlux * FLUX_VENT_PERCENT;
		ship.getFluxTracker().decreaseFlux(fluxToVent);

		// Calculate power based on vented flux (more flux = stronger singularity)
		float power = Math.min(2f, fluxToVent / (ship.getMaxFlux() * 0.5f));

		// FIX: Always ensure a minimum power level to guarantee visible effects
		power = Math.max(0.75f, power);

		// Create lists for affected ships and collision tracking
		List<ShipAPI> affectedShips = new ArrayList<>();
		Map<CombatEntityAPI, CollisionClass> collisions = new HashMap<>();

		// Find ships to be affected
		for (ShipAPI targetShip : engine.getShips()) {
		    if (targetShip.isAlive() && !targetShip.isPhased() && targetShip != ship) {
			float distance = Misc.getDistance(targetLocation, targetShip.getLocation());
			if (distance < SINGULARITY_RADIUS * 1.8f) {
			    affectedShips.add(targetShip);

			    // Store fighters and drones collision classes for later restoration
			    if (targetShip.isFighter() || targetShip.isDrone()) {
				collisions.put(targetShip, targetShip.getCollisionClass());
				targetShip.setCollisionClass(CollisionClass.ASTEROID);
			    }
			}
		    }
		}

		// Create the void singularity through the plugin
		// IMPORTANT FIX: Pass fixed duration, not multiplied by power
		ELYS_VoidSingularityPlugin.addSingularity(
			targetLocation,
			BASE_DURATION,
			affectedShips,
			collisions,
			ship,
			power  // Power only affects visual effects and pull strength
		);

		// Sound effect
		//Global.getSoundPlayer().playSound("system_high_energy_focus", 1f, 1f, targetLocation, ship.getVelocity());
	    }
	} else if (state == State.OUT) {
	    activated = false;
	    targetLocation = null;
	}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
	activated = false;
	targetLocation = null;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
	if (index == 0) {
	    return new StatusData("Generating void singularity", false);
	} else if (index == 1) {
	    return new StatusData("Venting flux", false);
	}
	return null;
    }
}