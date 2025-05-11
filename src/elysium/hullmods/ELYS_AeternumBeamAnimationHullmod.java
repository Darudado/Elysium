package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import elysium.Util;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * A hullmod that handles the animation of the Aeternum ship's frontal armor modules.
 * This is applied to the main ship via its hull file or variant.
 */
public class AeternumBeamAnimationHullmod extends BaseHullMod {
    private static final Logger log = Logger.getLogger(AeternumBeamAnimationHullmod.class);

    // Map to store animation states for all ships that have this hullmod
    private static final Map<ShipAPI, ModuleAnimationState> SHIP_ANIMATION_STATES = new HashMap<>();

    // Constants for animation control
    private static final String WEAPON_ID = "elys_aeternum_beam";
    private static final String LEFT_MODULE_SLOT = "WS 017";
    private static final String RIGHT_MODULE_SLOT = "WS 018";

    // Animation parameters
    private static final float MAX_OFFSET = 35f; // Maximum displacement in game units
    private static final float ACCELERATION = 2.0f; // For smooth easing
    private static final boolean SHOW_DEBUG = true; // Set to true to display debug info

    // Visual effect parameters
    private static final Color EFFECT_COLOR = Util.ELYSIUM_PRIMARY;
    private static final float EFFECT_INTERVAL = 0.1f; // Seconds between visual effects

    /**
     * Animation state class to track each ship's module positions and animation status
     */
    private static class ModuleAnimationState {
	Vector2f leftModuleOriginalPos;
	Vector2f rightModuleOriginalPos;
	float currentOffset = 0f;
	float currentVelocity = 0f;
	boolean isAnimating = false;
	boolean isReturning = false;
	boolean hasInitialPositions = false;
	IntervalUtil effectTimer = new IntervalUtil(EFFECT_INTERVAL, EFFECT_INTERVAL);

	public ModuleAnimationState() {
	    this.leftModuleOriginalPos = new Vector2f();
	    this.rightModuleOriginalPos = new Vector2f();
	}

	public void setOriginalPositions(Vector2f leftPos, Vector2f rightPos) {
	    this.leftModuleOriginalPos = new Vector2f(leftPos);
	    this.rightModuleOriginalPos = new Vector2f(rightPos);
	    this.hasInitialPositions = true;
	    log.info("Set original positions - Left: " + leftPos + ", Right: " + rightPos);
	}
    }

    /**
     * This method is called every frame for each ship that has this hullmod
     */
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
	if (ship.isHulk() || !ship.isAlive() || Global.getCombatEngine().isPaused()) return;

	// Get or create animation state for this ship
	ModuleAnimationState state = getAnimationState(ship);

	// Find the main beam weapon
	WeaponAPI mainBeam = findMainBeam(ship);
	if (mainBeam == null) {
	    if (SHOW_DEBUG) {
		log.warn("Main beam weapon not found for Aeternum ship: " + ship.getName());
	    }
	    return;
	}

	// Get the ship modules
	ShipAPI leftModule = findModule(ship, LEFT_MODULE_SLOT);
	ShipAPI rightModule = findModule(ship, RIGHT_MODULE_SLOT);

	if (leftModule == null || rightModule == null) {
	    if (SHOW_DEBUG) {
		log.warn("Modules not found for Aeternum ship: " + ship.getName());
	    }
	    return;
	}

	// If this is the first time seeing these modules, save their original positions
	if (!state.hasInitialPositions) {
	    state.setOriginalPositions(new Vector2f(leftModule.getLocation()),
		    new Vector2f(rightModule.getLocation()));
	}

	// Check weapon state and update animation accordingly
	boolean isBeamActive = mainBeam.getChargeLevel() > 0;

	// Start animation if charging or firing
	if (isBeamActive && !state.isAnimating) {
	    state.isAnimating = true;
	    state.isReturning = false;
	    if (SHOW_DEBUG) {
		log.info("Starting outward animation for " + ship.getName());
	    }
	}
	// Start return animation if no longer charging or firing
	else if (!isBeamActive && state.isAnimating && !state.isReturning) {
	    state.isReturning = true;
	    if (SHOW_DEBUG) {
		log.info("Starting return animation for " + ship.getName());
	    }
	}

	// Update the animation state
	if (state.isAnimating) {
	    // Calculate target and apply easing for smooth animation
	    float targetOffset = state.isReturning ? 0f : MAX_OFFSET;
	    float diff = targetOffset - state.currentOffset;

	    // Apply acceleration-based easing
	    state.currentVelocity += diff * ACCELERATION * amount;
	    state.currentVelocity *= 0.95f; // Damping to prevent oscillation
	    state.currentOffset += state.currentVelocity * amount;

	    // Ensure we stay within bounds
	    if (!state.isReturning && state.currentOffset > MAX_OFFSET) {
		state.currentOffset = MAX_OFFSET;
		state.currentVelocity = 0;
	    } else if (state.isReturning && state.currentOffset < 0.1f) {
		state.currentOffset = 0;
		state.currentVelocity = 0;
		state.isAnimating = false;
		state.isReturning = false;
		if (SHOW_DEBUG) {
		    log.info("Animation complete for " + ship.getName());
		}
	    }

	    // Apply the current offset to module positions
	    updateModulePositions(leftModule, rightModule, state,ship);

	    // Add visual effects if modules are moving significantly
	    state.effectTimer.advance(amount);
	    if (state.effectTimer.intervalElapsed() && Math.abs(state.currentVelocity) > 5f) {
		addVisualEffects(Global.getCombatEngine(), leftModule, rightModule);
	    }

	    // Debug visualization
	    if (SHOW_DEBUG) {
		Global.getCombatEngine().addFloatingText(ship.getLocation(),
			"Offset: " + String.format("%.1f", state.currentOffset),
			15f, Color.WHITE, ship, 0.5f, 0.5f);
	    }
	}
    }

    /**
     * Get or create animation state for this ship
     */
    private ModuleAnimationState getAnimationState(ShipAPI ship) {
	if (!SHIP_ANIMATION_STATES.containsKey(ship)) {
	    SHIP_ANIMATION_STATES.put(ship, new ModuleAnimationState());
	    if (SHOW_DEBUG) {
		log.info("Created new animation state for " + ship.getName());
	    }
	}
	return SHIP_ANIMATION_STATES.get(ship);
    }

    /**
     * Find a specific module by its slot ID
     */
    private ShipAPI findModule(ShipAPI ship, String slotId) {
	for (ShipAPI module : ship.getChildModulesCopy()) {
	    if (module.getStationSlot() != null &&
		    slotId.equals(module.getStationSlot().getId())) {
		return module;
	    }
	}
	return null;
    }

    /**
     * Find the main beam weapon on the ship
     */
    private WeaponAPI findMainBeam(ShipAPI ship) {
	for (WeaponAPI weapon : ship.getAllWeapons()) {
	    if (WEAPON_ID.equals(weapon.getId())) {
		return weapon;
	    }
	}
	return null;
    }

    /**
     * Calculate and apply new positions for the modules
     */
    private void updateModulePositions(ShipAPI leftModule, ShipAPI rightModule, ModuleAnimationState state,
	    ShipAPI ship) {
	// Calculate angle perpendicular to ship facing for proper module movement
	float shipFacing = ship.getFacing();
	float facingRadians = (float) Math.toRadians(90 - shipFacing);

	// Calculate the lateral offset vector
	float offsetX = (float) Math.cos(facingRadians) * state.currentOffset;
	float offsetY = (float) Math.sin(facingRadians) * state.currentOffset;

	// Update module positions
	Vector2f leftNewPos = new Vector2f(
		state.leftModuleOriginalPos.x - offsetX,
		state.leftModuleOriginalPos.y - offsetY
	);

	Vector2f rightNewPos = new Vector2f(
		state.rightModuleOriginalPos.x + offsetX,
		state.rightModuleOriginalPos.y + offsetY
	);

	leftModule.getLocation().set(leftNewPos);
	rightModule.getLocation().set(rightNewPos);
    }

    /**
     * Add visual effects at the module separation points
     */
    private void addVisualEffects(CombatEngineAPI engine, ShipAPI leftModule, ShipAPI rightModule) {
	// Inner module edges - where the modules are separating
	Vector2f leftInnerPoint = new Vector2f(leftModule.getLocation());
	Vector2f rightInnerPoint = new Vector2f(rightModule.getLocation());

	// Add energy particle effects at the separation points
	engine.addHitParticle(leftInnerPoint, new Vector2f(), 10f, 1.0f, 0.2f, EFFECT_COLOR);
	engine.addHitParticle(rightInnerPoint, new Vector2f(), 10f, 1.0f, 0.2f, EFFECT_COLOR);
    }


    /**
     * Description shown in refit screen (will never be seen since it's hidden)
     */
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
	return null;
    }


}