package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the animated sliding of frontal armor modules for the Aeternum ship.
 * Modules move outward (left/right) when the central beam is charging/firing.
 */
public class ELYS_AeternumBeamAnimationHullmod extends BaseHullMod {

    // --- Configuration ---
    private static final String WEAPON_ID = "elys_aeternum_beam";
    private static final String LEFT_MODULE_SLOT = "WS 017";
    private static final String RIGHT_MODULE_SLOT = "WS 018";

    private static final float MAX_OFFSET = 30f;      // Width of the opening
    private static final float ACCELERATION = 8.0f;   // Speed of opening/closing
    private static final float SNAP_THRESHOLD = 0.1f; // Margin for error

    // Resting Offsets (Manual Adjustment)
    private static final float RESTING_OFFSET_X = -10f; // Horizontal adjustment (+Outward / -Inward)
    private static final float RESTING_OFFSET_Y = 30f; // Vertical adjustment (+Forward / -Backward)

    // Key for storing custom state in the combat engine
    private static final String ANIMATION_DATA_KEY = "ELYS_aeternum_animation_data";

    /**
     * Internal state to track the progress of the animation per-ship.
     */
    private static class ModuleAnimationState {
	float currentOffset = 0f;
	float currentVelocity = 0f;

	public void reset() {
	    this.currentOffset = 0f;
	    this.currentVelocity = 0f;
	}
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
	CombatEngineAPI engine = Global.getCombatEngine();
	if (ship == null || engine == null || ship.isHulk() || !ship.isAlive() || engine.isPaused()) return;

	ModuleAnimationState state = getAnimationState(ship);

	// 1. Locate components
	WeaponAPI mainBeam = findMainBeam(ship);
	ShipAPI leftModule = findModule(ship, LEFT_MODULE_SLOT);
	ShipAPI rightModule = findModule(ship, RIGHT_MODULE_SLOT);

	// Fail-safe: ignore if modules are missing or destroyed
	if (mainBeam == null || leftModule == null || rightModule == null ||
		leftModule.isHulk() || rightModule.isHulk()) return;

	// Reset state on initial deployment
	if (ship.getFullTimeDeployed() < 0.1f) state.reset();

	// 2. Determine target position
	boolean isBeamActive = mainBeam.getChargeLevel() > 0;
	boolean isShipOverloaded = ship.getFluxTracker().isOverloaded();
	float targetOffset = (isBeamActive && !isShipOverloaded) ? MAX_OFFSET : 0f;

	// 3. Simple Spring-Dampening Animation Physics
	float diff = targetOffset - state.currentOffset;
	if (Math.abs(diff) > SNAP_THRESHOLD) {
	    state.currentVelocity += diff * ACCELERATION * amount;
	    state.currentVelocity *= 0.90f; // Friction/Damping
	    state.currentOffset += state.currentVelocity * amount;
	} else {
	    state.currentOffset = targetOffset;
	    state.currentVelocity = 0;
	}

	// 4. Force override module physics to new location
	applyMovement(ship, leftModule, rightModule, state.currentOffset);
    }

    /**
     * Forces the modules to follow the ship rotation and apply the lateral offset.
     */
    private void applyMovement(ShipAPI ship, ShipAPI left, ShipAPI right, float animOffset) {
 float shipFacing = ship.getFacing();

 // 1. Calculate vectors
 // Forward Vector (for Y offset)
 float forwardRad = (float) Math.toRadians(shipFacing);
 float fx = (float) Math.cos(forwardRad);
 float fy = (float) Math.sin(forwardRad);

 // Right Vector (for X offset + animation)
 float rightRad = (float) Math.toRadians(shipFacing - 90f);
 float rx = (float) Math.cos(rightRad);
 float ry = (float) Math.sin(rightRad);

 // 2. Calculate total horizontal offset
 float totalX = animOffset + RESTING_OFFSET_X;

 // 3. Get anchor positions
 Vector2f leftAnchor = getWorldCoordinates(ship, left.getStationSlot().getLocation());
 Vector2f rightAnchor = getWorldCoordinates(ship, right.getStationSlot().getLocation());

 // 4. Apply offsets
 // Left: -RightVector * totalX + ForwardVector * RESTING_OFFSET_Y
 left.getLocation().set(
  leftAnchor.x - (rx * totalX) + (fx * RESTING_OFFSET_Y),
  leftAnchor.y - (ry * totalX) + (fy * RESTING_OFFSET_Y)
 );

 // Right: +RightVector * totalX + ForwardVector * RESTING_OFFSET_Y
 right.getLocation().set(
  rightAnchor.x + (rx * totalX) + (fx * RESTING_OFFSET_Y),
  rightAnchor.y + (ry * totalX) + (fy * RESTING_OFFSET_Y)
 );

 // Lock rotation to parent
 left.setFacing(shipFacing);
 right.setFacing(shipFacing);
    }

    /**
     * Converts a local vector coordinate (from the ship hull file) to its current world coordinate.
     */
    private Vector2f getWorldCoordinates(ShipAPI ship, Vector2f localCoords) {
	float shipFacingRadians = (float) Math.toRadians(ship.getFacing());
	float cos = (float) Math.cos(shipFacingRadians);
	float sin = (float) Math.sin(shipFacingRadians);

	float rotatedX = localCoords.x * cos - localCoords.y * sin;
	float rotatedY = localCoords.x * sin + localCoords.y * cos;

	return new Vector2f(ship.getLocation().x + rotatedX, ship.getLocation().y + rotatedY);
    }

    // --- Helper Getters ---

    private Map<String, ModuleAnimationState> getAnimationDataMap(CombatEngineAPI engine) {
	Map<String, ModuleAnimationState> dataMap = (Map<String, ModuleAnimationState>) engine.getCustomData().get(ANIMATION_DATA_KEY);
	if (dataMap == null) {
	    dataMap = new HashMap<>();
	    engine.getCustomData().put(ANIMATION_DATA_KEY, dataMap);
	}
	return dataMap;
    }

    private ModuleAnimationState getAnimationState(ShipAPI ship) {
	Map<String, ModuleAnimationState> dataMap = getAnimationDataMap(Global.getCombatEngine());
	String shipKey = ship.getId();
	if (!dataMap.containsKey(shipKey)) {
	    dataMap.put(shipKey, new ModuleAnimationState());
	}
	return dataMap.get(shipKey);
    }

    private ShipAPI findModule(ShipAPI ship, String slotId) {
	for (ShipAPI module : ship.getChildModulesCopy()) {
	    if (module.getStationSlot() != null && slotId.equals(module.getStationSlot().getId())) return module;
	}
	return null;
    }

    private WeaponAPI findMainBeam(ShipAPI ship) {
	for (WeaponAPI weapon : ship.getAllWeapons()) {
	    if (WEAPON_ID.equals(weapon.getId())) return weapon;
	}
	return null;
    }
}