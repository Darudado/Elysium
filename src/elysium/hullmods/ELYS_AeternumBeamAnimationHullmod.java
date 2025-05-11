package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import elysium.Util;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * A hullmod that handles the animation of the Aeternum ship's frontal armor modules.
 * This is applied to the main ship via its hull file or variant.
 */
public class ELYS_AeternumBeamAnimationHullmod extends BaseHullMod {

    // Constants for animation control
    private static final String WEAPON_ID = "elys_aeternum_beam";
    private static final String LEFT_MODULE_SLOT = "WS 017";
    private static final String RIGHT_MODULE_SLOT = "WS 018";

    // Animation parameters
    private static final float MAX_OFFSET = 30f; // Maximum displacement in game units
    private static final float ACCELERATION = 4.0f; // For smooth easing
    private static final float SNAP_THRESHOLD = 0.1f; // Threshold for considering animation complete

    // Visual effect parameters
    private static final float EFFECT_INTERVAL = 0.1f; // Seconds between visual effects

    // Key for CustomData storage
    private static final String ANIMATION_DATA_KEY = "ELYS_aeternum_animation_data";
    private static final String MODULE_OFFSET_KEY = "ELYS_module_animation_offset";

    /**
     * Animation state class to track each ship's module positions and animation status
     */
    private static class ModuleAnimationState {
	// Store the original slot locations rather than absolute positions
	Vector2f leftModuleSlotLocation;
	Vector2f rightModuleSlotLocation;
	float currentOffset = 0f;
	float currentVelocity = 0f;
	boolean isAnimating = false;
	boolean isReturning = false;
	boolean hasInitialPositions = false;
	IntervalUtil effectTimer = new IntervalUtil(EFFECT_INTERVAL, EFFECT_INTERVAL);
	int firingCount = 0;

	public ModuleAnimationState() {
	    this.leftModuleSlotLocation = new Vector2f();
	    this.rightModuleSlotLocation = new Vector2f();
	}

	public void setInitialPositions(ShipAPI ship, ShipAPI leftModule, ShipAPI rightModule) {
	    // Store the original module positions from station slots
	    if (leftModule.getStationSlot() != null) {
		this.leftModuleSlotLocation = new Vector2f(
			leftModule.getStationSlot().getLocation().x,
			leftModule.getStationSlot().getLocation().y
		);
	    } else {
		// Fallback to current offset from ship if station slot info unavailable
		Vector2f offset = new Vector2f(
			leftModule.getLocation().x - ship.getLocation().x,
			leftModule.getLocation().y - ship.getLocation().y
		);
		this.leftModuleSlotLocation = offset;
	    }

	    if (rightModule.getStationSlot() != null) {
		this.rightModuleSlotLocation = new Vector2f(
			rightModule.getStationSlot().getLocation().x,
			rightModule.getStationSlot().getLocation().y
		);
	    } else {
		// Fallback to current offset from ship if station slot info unavailable
		Vector2f offset = new Vector2f(
			rightModule.getLocation().x - ship.getLocation().x,
			rightModule.getLocation().y - ship.getLocation().y
		);
		this.rightModuleSlotLocation = offset;
	    }

	    // Reset any existing offsets on the modules
	    leftModule.setCustomData(MODULE_OFFSET_KEY, new Vector2f(0f, 0f));
	    rightModule.setCustomData(MODULE_OFFSET_KEY, new Vector2f(0f, 0f));

	    this.hasInitialPositions = true;
	}

	public void reset() {
	    this.currentOffset = 0f;
	    this.currentVelocity = 0f;
	    this.isAnimating = false;
	    this.isReturning = false;
	    this.hasInitialPositions = false;
	}
    }

    /**
     * Get the animation data map from CustomData, creating it if needed
     */
    @SuppressWarnings("unchecked")
    private Map<String, ModuleAnimationState> getAnimationDataMap(CombatEngineAPI engine) {
	Map<String, ModuleAnimationState> dataMap = (Map<String, ModuleAnimationState>) engine.getCustomData().get(ANIMATION_DATA_KEY);
	if (dataMap == null) {
	    dataMap = new HashMap<>();
	    engine.getCustomData().put(ANIMATION_DATA_KEY, dataMap);
	}
	return dataMap;
    }

    /**
     * Get a unique ID for this ship
     */
    private String getShipKey(ShipAPI ship) {
	return ship.getId();
    }

    /**
     * Get or create animation state for this ship
     */
    private ModuleAnimationState getAnimationState(ShipAPI ship) {
	CombatEngineAPI engine = Global.getCombatEngine();
	Map<String, ModuleAnimationState> dataMap = getAnimationDataMap(engine);

	String shipKey = getShipKey(ship);
	if (!dataMap.containsKey(shipKey)) {
	    dataMap.put(shipKey, new ModuleAnimationState());
	}
	return dataMap.get(shipKey);
    }

    /**
     * This method is called every frame for each ship that has this hullmod
     */
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
	CombatEngineAPI engine = Global.getCombatEngine();
	if (ship == null || engine == null || ship.isHulk() || !ship.isAlive() || engine.isPaused()) return;

	// Get animation state for this ship
	ModuleAnimationState state = getAnimationState(ship);

	// Find the main beam weapon
	WeaponAPI mainBeam = findMainBeam(ship);
	if (mainBeam == null) {
	    return;
	}

	// Get the ship modules
	ShipAPI leftModule = findModule(ship, LEFT_MODULE_SLOT);
	ShipAPI rightModule = findModule(ship, RIGHT_MODULE_SLOT);

	// Skip if modules are null or destroyed
	if (leftModule == null || rightModule == null) {
	    return;
	}

	// Skip if either module is destroyed
	if (leftModule.isHulk() || !leftModule.isAlive() || rightModule.isHulk() || !rightModule.isAlive()) {
	    return;
	}

	// Reset animation state if ship was recently deployed
	// or if modules have been re-attached after being destroyed
	if (ship.getFullTimeDeployed() < 1f ||
		(leftModule.getHitpoints() == leftModule.getMaxHitpoints() &&
			rightModule.getHitpoints() == rightModule.getMaxHitpoints() &&
			state.firingCount > 0 &&
			!state.hasInitialPositions)) {
	    state.reset();
	}

	// If this is the first time seeing these modules, save their original positions
	if (!state.hasInitialPositions) {
	    state.setInitialPositions(ship, leftModule, rightModule);
	}

	// Ensure modules have custom data for offsets
	ensureModuleOffsetData(leftModule);
	ensureModuleOffsetData(rightModule);

	// ANIMATION LOGIC: SIMPLIFIED
	// Read the beam state - is it charging or firing?
	boolean isBeamActive = mainBeam.getChargeLevel() > 0;
	boolean isShipOverloaded = ship.getFluxTracker().isOverloaded();

	// Determine target state
	// If beam is active AND not overloaded -> open modules (target = MAX_OFFSET)
	// Otherwise -> close modules (target = 0)
	float targetOffset = (isBeamActive && !isShipOverloaded) ? MAX_OFFSET : 0f;

	// Always animate if we're not at the target
	boolean shouldAnimate = Math.abs(state.currentOffset - targetOffset) > SNAP_THRESHOLD;
	state.isAnimating = shouldAnimate;

	// Count firing cycles
	if (isBeamActive && targetOffset > 0 && state.currentOffset < 1f) {
	    state.firingCount++;
	}

	// Update current offset with acceleration-based easing
	if (state.isAnimating) {
	    // Calculate difference to target
	    float diff = targetOffset - state.currentOffset;

	    // Apply acceleration-based easing
	    state.currentVelocity += diff * ACCELERATION * amount;
	    state.currentVelocity *= 0.95f; // Damping to prevent oscillation
	    state.currentOffset += state.currentVelocity * amount;

	    // Snap to bounds if very close
	    if (Math.abs(state.currentOffset - targetOffset) < SNAP_THRESHOLD) {
		state.currentOffset = targetOffset;
		state.currentVelocity = 0f;
		state.isAnimating = false;
	    }

	    // Update module positions with the new offset
	    updateModuleOffsets(ship, leftModule, rightModule, state);

	    // Add visual effects if modules are moving significantly
	    state.effectTimer.advance(amount);
	    if (state.effectTimer.intervalElapsed() && Math.abs(state.currentVelocity) > 5f) {
		addVisualEffects(engine, leftModule, rightModule);
	    }
	} else if (state.currentOffset != targetOffset) {
	    // Ensure we're at exactly the target if not animating
	    state.currentOffset = targetOffset;
	    updateModuleOffsets(ship, leftModule, rightModule, state);
	}

	// Apply the calculated offsets to actually position the modules
	applyModuleOffsets(ship, leftModule, rightModule);
    }

    /**
     * Ensure the module has offset data
     */
    private void ensureModuleOffsetData(ShipAPI module) {
	if (module.getCustomData().get(MODULE_OFFSET_KEY) == null) {
	    module.setCustomData(MODULE_OFFSET_KEY, new Vector2f(0f, 0f));
	}
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
     * Calculate and store new offset values for the modules during animation
     */
    private void updateModuleOffsets(ShipAPI ship, ShipAPI leftModule, ShipAPI rightModule, ModuleAnimationState state) {
	// Get the ship's facing in radians
	float shipFacingDegrees = ship.getFacing();
	float shipFacingRadians = (float) Math.toRadians(shipFacingDegrees);

	// Calculate perpendicular vector (90 degrees to the right of forward direction)
	float rightX = (float) Math.cos(shipFacingRadians + Math.PI/2);
	float rightY = (float) Math.sin(shipFacingRadians + Math.PI/2);

	// Scale by the current animation offset
	float scaledRightX = rightX * state.currentOffset;
	float scaledRightY = rightY * state.currentOffset;

	// Get the offset vectors from module custom data
	Vector2f leftOffset = (Vector2f) leftModule.getCustomData().get(MODULE_OFFSET_KEY);
	Vector2f rightOffset = (Vector2f) rightModule.getCustomData().get(MODULE_OFFSET_KEY);

	// Apply offsets - left goes left, right goes right
	// When beam is active, currentOffset increases, modules move apart
	// When beam is inactive, currentOffset decreases, modules move together
	if (leftOffset != null) {
	    leftOffset.set(scaledRightX, scaledRightY);
	}

	if (rightOffset != null) {
	    rightOffset.set(-scaledRightX, -scaledRightY);
	}
    }

    /**
     * Add visual effects at the module separation points
     */
    private void addVisualEffects(CombatEngineAPI engine, ShipAPI leftModule, ShipAPI rightModule) {
	// Get the effect color from Util class
	Color effectColor = Util.ELYSIUM_PRIMARY;

	// Inner module edges - where the modules are separating
	Vector2f leftInnerPoint = new Vector2f(leftModule.getLocation());
	Vector2f rightInnerPoint = new Vector2f(rightModule.getLocation());

	// Add energy particle effects at the separation points
	engine.addHitParticle(leftInnerPoint, new Vector2f(), 10f, 1.0f, 0.2f, effectColor);
	engine.addHitParticle(rightInnerPoint, new Vector2f(), 10f, 1.0f, 0.2f, effectColor);
    }

    /**
     * Apply module offsets during ship stats calculation
     */
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// No pre-creation effects needed
    }

    /**
     * Apply the module positioning after advancing the animation state
     * This uses a different approach to apply offsets to the modules
     */
    private void applyModuleOffsets(ShipAPI ship, ShipAPI leftModule, ShipAPI rightModule) {
	// First, check if modules have offset data
	Vector2f leftOffset = (Vector2f) leftModule.getCustomData().get(MODULE_OFFSET_KEY);
	Vector2f rightOffset = (Vector2f) rightModule.getCustomData().get(MODULE_OFFSET_KEY);

	if (leftOffset == null || rightOffset == null) {
	    return;
	}

	// Create a vector based on ship's location
	Vector2f shipLoc = ship.getLocation();

	// Get the base positions from the station slots
	Vector2f leftBasePos = getWorldCoordinates(ship, leftModule.getStationSlot().getLocation());
	Vector2f rightBasePos = getWorldCoordinates(ship, rightModule.getStationSlot().getLocation());

	// Add our offsets to the base positions
	Vector2f leftTargetPos = new Vector2f(
		leftBasePos.x + leftOffset.x,
		leftBasePos.y + leftOffset.y
	);

	Vector2f rightTargetPos = new Vector2f(
		rightBasePos.x + rightOffset.x,
		rightBasePos.y + rightOffset.y
	);

	// Apply position with a smoothing approach
	float smoothing = 0.3f; // Higher = more responsive

	// Apply smoothed position to left module
	Vector2f leftCurrentPos = leftModule.getLocation();
	leftCurrentPos.x += (leftTargetPos.x - leftCurrentPos.x) * smoothing;
	leftCurrentPos.y += (leftTargetPos.y - leftCurrentPos.y) * smoothing;

	// Apply smoothed position to right module
	Vector2f rightCurrentPos = rightModule.getLocation();
	rightCurrentPos.x += (rightTargetPos.x - rightCurrentPos.x) * smoothing;
	rightCurrentPos.y += (rightTargetPos.y - rightCurrentPos.y) * smoothing;
    }

    /**
     * Convert local coordinates (relative to ship) to world coordinates
     */
    private Vector2f getWorldCoordinates(ShipAPI ship, Vector2f localCoords) {
	// Convert from local ship coordinates to world coordinates
	// This accounts for ship position and rotation
	float shipFacingDegrees = ship.getFacing();
	float shipFacingRadians = (float) Math.toRadians(shipFacingDegrees);
	float cos = (float) Math.cos(shipFacingRadians);
	float sin = (float) Math.sin(shipFacingRadians);

	// Rotate the local coordinates
	float rotatedX = localCoords.x * cos - localCoords.y * sin;
	float rotatedY = localCoords.x * sin + localCoords.y * cos;

	// Add ship's position to get world coordinates
	return new Vector2f(
		ship.getLocation().x + rotatedX,
		ship.getLocation().y + rotatedY
	);
    }

    /**
     * Clear animation data when the engine is destroyed (between combats)
     */
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	CombatEngineAPI engine = Global.getCombatEngine();
	if (engine != null && engine.getCustomData().containsKey(ANIMATION_DATA_KEY)) {
	    Map<String, ModuleAnimationState> dataMap = getAnimationDataMap(engine);
	    String shipKey = getShipKey(ship);
	    if (dataMap.containsKey(shipKey)) {
		dataMap.get(shipKey).reset();
	    }
	}
    }
}