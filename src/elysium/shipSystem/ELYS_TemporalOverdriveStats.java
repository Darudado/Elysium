package elysium.shipSystem;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.magiclib.util.MagicRender;

import static elysium.Util.*;

public class ELYS_TemporalOverdriveStats extends BaseShipSystemScript {
    // System constants
    public static final float TIME_MULT = 3f;
    public static final float FLUX_PERCENT_PER_SECOND = 0.04f; // 5% flux per second

    // Speed boost based on hull size
    public static final float FRIGATE_SPEED_BOOST = 0.7f;     // +100%
    public static final float DESTROYER_SPEED_BOOST = 1f;   // +110%
    public static final float CRUISER_SPEED_BOOST = 1.3f;     // +120%
    public static final float CAPITAL_SPEED_BOOST = 1.5f;     // +130%

    // Flower sprite paths
    private static final String FLOWER_LARGE_SPRITE_PATH = "FLOWER_LARGE_SPRITE_PATH";
    private static final String FLOWER_MEDIUM_SPRITE_PATH = "FLOWER_MEDIUM_SPRITE_PATH";
    private static final String FLOWER_SMALL_SPRITE_PATH = "FLOWER_SMALL_SPRITE_PATH";

    // Flower rotation speeds - increased speeds for better visibility and scaled by size
    private static final float LARGE_ROTATION_SPEED = 30f;    // degrees per second
    private static final float MEDIUM_ROTATION_SPEED = -45f;  // negative for opposite direction, faster than large
    private static final float SMALL_ROTATION_SPEED = 60f;    // fastest rotation for smallest flower

    // To track if flowers are currently active
    private boolean flowersActive = false;
    private float flowerTimer = 0f;
    private float largeAngle = 0f;
    private float mediumAngle = 120f;
    private float smallAngle = 240f;

    // System reference - specifically for phase cloak
    private ShipSystemAPI phaseSystem = null;

    // Afterimage drone constants
    private static final int MAX_AFTERIMAGES = 4;
    private static final float AFTERIMAGE_HULL_ARMOR_FACTOR = 0.05f; // 1/10 of the ship
    private static final float AFTERIMAGE_MIN_ALPHA = 0.3f;
    private static final float AFTERIMAGE_MAX_ALPHA = 0.6f;
    private static final float AFTERIMAGE_FLICKER_SPEED = 3.0f; // Cycles per second
    private static final float RESPAWN_COOLDOWN = 60.0f; // Cooldown in seconds

    // Decoy slot data structure
    private static class DecoySlot {
	ShipAPI decoy;        // The actual decoy ship (null if not active)
	float timer;          // Timer: 0 = ready to spawn, >0 = active, <0 = cooldown (counting up to 0)
	float angle;          // Position angle relative to main ship
	float flickerOffset;  // Random offset for flicker effect

	// Constructor
	public DecoySlot(float angle) {
	    this.decoy = null;
	    this.timer = 0f;  // Ready to spawn initially
	    this.angle = angle;
	    this.flickerOffset = (float)Math.random() * 2f * (float)Math.PI;
	}
    }

    // Array of decoy slots
    private DecoySlot[] decoySlots = new DecoySlot[MAX_AFTERIMAGES];

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	ShipAPI ship = null;
	boolean player = false;
	CombatEngineAPI engine = Global.getCombatEngine();

	if (stats.getEntity() instanceof ShipAPI) {
	    ship = (ShipAPI) stats.getEntity();
	    player = ship == engine.getPlayerShip();
	    id = id + "_" + ship.getId();

	    // Get the phase cloak system reference specifically
	    if (phaseSystem == null) {
		phaseSystem = ship.getPhaseCloak();
	    }

	    // Initialize decoy slots if needed
	    if (decoySlots[0] == null) {
		float[] angles = {45f, 135f, 225f, 315f}; // X-formation
		for (int i = 0; i < MAX_AFTERIMAGES; i++) {
		    decoySlots[i] = new DecoySlot(angles[i]);
		}
	    }
	} else {
	    return;
	}

	// Check if phase cloak is actually active
	boolean systemActive = (phaseSystem != null) && phaseSystem.isActive();

	// Add hard flux gradually while system is active
	if (engine.getElapsedInLastFrame() > 0 && state == State.ACTIVE) {
	    float fluxThisFrame = ship.getMaxFlux() * FLUX_PERCENT_PER_SECOND *
		    engine.getElapsedInLastFrame();
	    ship.getFluxTracker().increaseFlux(fluxThisFrame, true);
	}

	// Prevent hard flux dissipation while system is active
	if (effectLevel > 0) {
	    stats.getHardFluxDissipationFraction().modifyMult(id, 0f);
	}

	// Calculate jitter level for effects
	float jitterLevel = effectLevel;
	if (state == State.IN) {
	    jitterLevel = effectLevel * 2f;
	    if (jitterLevel > 1f) jitterLevel = 1f;
	}

	float jitterRange = 5f + 10f * effectLevel;

	// Handle flower effects using MagicRender
	float elapsed = engine.getElapsedInLastFrame();
	flowerTimer -= elapsed;

	// Update rotation angles continuously while the system is active
	if (effectLevel > 0) {
	    largeAngle = (largeAngle + LARGE_ROTATION_SPEED * elapsed) % 360f;
	    mediumAngle = (mediumAngle + MEDIUM_ROTATION_SPEED * elapsed) % 360f;
	    smallAngle = (smallAngle + SMALL_ROTATION_SPEED * elapsed) % 360f;
	}

	// Only create new flower sprites if timer is up or flowers aren't active
	if ((flowerTimer <= 0f || !flowersActive) && effectLevel > 0.2f) {
	    // Reset timer - controls how often we refresh the sprites
	    flowerTimer = 0.05f; // Refresh more frequently (every 0.05 seconds) for smoother rotation
	    flowersActive = true;

	    // Calculate base size based on ship size, but smaller than before
	    float baseSize = ship.getCollisionRadius() * 1.2f; // Reduced from 2.0f to 1.2f

	    // Create large flower
	    float alphaMultLarge = Math.max(0f, effectLevel * 0.15f);
	    Color largeColorWithAlpha = new Color(
		    FLOWER_LARGE_COLOR.getRed(),
		    FLOWER_LARGE_COLOR.getGreen(),
		    FLOWER_LARGE_COLOR.getBlue(),
		    (int)(FLOWER_LARGE_COLOR.getAlpha() * alphaMultLarge)
	    );

	    SpriteAPI flowerSprite = Global.getSettings().getSprite("fx", FLOWER_LARGE_SPRITE_PATH);
	    // MagicRender for large flower - using advanced version with layer parameter
	    MagicRender.objectspace(
		    flowerSprite,
		    ship,
		    new Vector2f(0f, 0f),  // Offset from ship center
		    new Vector2f(0f, 0f),  // No velocity
		    new Vector2f(baseSize * 1.8f, baseSize * 1.8f),  // Size reduced from 2.2f to 1.8f
		    new Vector2f(0f, 0f),  // No growth
		    largeAngle,  // Use continuously updated angle
		    0f,  // No additional rotation in objectspace (we're handling it manually)
		    false,  // Attach to ship's orientation
		    largeColorWithAlpha,
		    true,  // Additive
		    0f,    // No jitter
		    0f,    // No jitter tilt
		    0f,    // No flicker
		    1f,    // Default median
		    0f,    // No delay
		    0f,  // Faster fade in
		    0.05f,  // Short full time
		    0f,  // Faster fade out
		    true, // Don't fade on death (continually refresh)
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );

	    // Create medium flower with different angle
	    float alphaMultMedium = Math.max(0f, effectLevel * 0.15f);
	    Color mediumColorWithAlpha = new Color(
		    FLOWER_MEDIUM_COLOR.getRed(),
		    FLOWER_MEDIUM_COLOR.getGreen(),
		    FLOWER_MEDIUM_COLOR.getBlue(),
		    (int)(FLOWER_MEDIUM_COLOR.getAlpha() * alphaMultMedium)
	    );

	    flowerSprite = Global.getSettings().getSprite("fx", FLOWER_MEDIUM_SPRITE_PATH);
	    // MagicRender for medium flower - using advanced version with layer parameter
	    MagicRender.objectspace(
		    flowerSprite,
		    ship,
		    new Vector2f(0f, 0f),  // Offset from ship center
		    new Vector2f(0f, 0f),  // No velocity
		    new Vector2f(baseSize * 1.6f, baseSize * 1.6f),  // Size reduced from 1.5f to 1.3f
		    new Vector2f(0f, 0f),  // No growth
		    mediumAngle,  // Use continuously updated angle
		    0f,  // No additional rotation in objectspace (we're handling it manually)
		    false,  // Attach to ship's orientation
		    mediumColorWithAlpha,
		    true,  // Additive
		    0f,    // No jitter
		    0f,    // No jitter tilt
		    0f,    // No flicker
		    1f,    // Default median
		    0f,    // No delay
		    0f,  // Faster fade in
		    0.05f,  // Short full time
		    0f,  // Faster fade out
		    true, // Don't fade on death (continually refresh)
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );

	    // Create small flower with different angle
	    float alphaMultSmall = Math.max(0f, effectLevel * 0.15f);
	    Color smallColorWithAlpha = new Color(
		    FLOWER_SMALL_COLOR.getRed(),
		    FLOWER_SMALL_COLOR.getGreen(),
		    FLOWER_SMALL_COLOR.getBlue(),
		    (int)(FLOWER_SMALL_COLOR.getAlpha() * alphaMultSmall)
	    );

	    flowerSprite = Global.getSettings().getSprite("fx", FLOWER_SMALL_SPRITE_PATH);
	    // MagicRender for small flower - using advanced version with layer parameter
	    MagicRender.objectspace(
		    flowerSprite,
		    ship,
		    new Vector2f(0f, 0f),  // Offset from ship center
		    new Vector2f(0f, 0f),  // No velocity
		    new Vector2f(baseSize * 1.3f, baseSize * 1.3f),  // Size reduced from 1.0f to 0.8f
		    new Vector2f(0f, 0f),  // No growth
		    smallAngle,  // Use continuously updated angle
		    0f,  // No additional rotation in objectspace (we're handling it manually)
		    false,  // Attach to ship's orientation
		    smallColorWithAlpha,
		    true,  // Additive
		    0f,    // No jitter
		    0f,    // No jitter tilt
		    0f,    // No flicker
		    1f,    // Default median
		    0f,    // No delay
		    0f,  // Faster fade in
		    0.05f,  // Short full time
		    0f,  // Faster fade out
		    true, // Don't fade on death (continually refresh)
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );
	}

	// If system is deactivating, mark flowers as inactive
	if (state == State.OUT && effectLevel < 0.2f) {
	    flowersActive = false;
	}

	// -------------- DECOY MANAGEMENT --------------
	// Create jitter effect for the main ship
	ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 3, 0f, jitterRange);


	if(!ship.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.MODULE) && !ship.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.SHIP_WITH_MODULES))
	{
	    float droneDistance = ship.getCollisionRadius() * 3f; // Distance from main ship
	    // Update all decoy slots
	    for (int i = 0; i < MAX_AFTERIMAGES; i++)
	    {
		DecoySlot slot = decoySlots[i];

		// Update timers
		if( slot.timer != 0 )
		{
		    // If timer is negative (cooldown), count up to 0
		    if( slot.timer < 0 )
		    {
			slot.timer += elapsed;
			if( slot.timer >= 0 )
			{
			    slot.timer = 0; // Reset when cooldown is complete
			    if( player )
			    {
				engine.addFloatingText(ship.getLocation(), "Echo " + (i + 1) + " ready", 20f,
					Color.GREEN, ship, 1f, 3f);
			    }
			}
		    }
		}

		// Check if the decoy is still alive
		if( slot.decoy != null )
		{
		    if( slot.decoy.isHulk() || !slot.decoy.isAlive() || slot.decoy.getHitpoints() <= 0 )
		    {
			// Decoy was destroyed, remove it
			engine.removeEntity(slot.decoy);
			slot.decoy = null;

			// Start cooldown timer
			slot.timer = -RESPAWN_COOLDOWN;

		    }
		}

		// If system is not active, remove all decoys but preserve cooldowns
		if( !systemActive )
		{
		    if( slot.decoy != null )
		    {
			engine.removeEntity(slot.decoy);
			slot.decoy = null;

			// If it wasn't in cooldown, reset timer to 0 (ready to spawn when system is active again)
			if( slot.timer > 0 )
			{
			    slot.timer = 0;
			}
		    }
		    continue; // Skip the rest of the loop since system is not active
		}

		// Try to spawn new decoy if timer is 0 (ready) and system is active
		if( slot.timer == 0 && slot.decoy == null && systemActive )
		{
		    float angle = slot.angle;
		    float posX = ship.getLocation().x + (float) Math.cos(Math.toRadians(angle)) * droneDistance;
		    float posY = ship.getLocation().y + (float) Math.sin(Math.toRadians(angle)) * droneDistance;
		    Vector2f position = new Vector2f(posX, posY);

		    // Check for collisions at spawn position
		    if( !isShipAtPosition(position, ship) )
		    {
			// No collision, safe to spawn
			createDecoy(slot, ship, position);

		    }
		}

		// Update existing decoys
		if( slot.decoy != null && slot.decoy.isAlive() && !slot.decoy.isHulk() )
		{
		    // Update position without jitter
		    float angle = slot.angle;
		    float offsetX = (float) Math.cos(Math.toRadians(angle)) * droneDistance;
		    float offsetY = (float) Math.sin(Math.toRadians(angle)) * droneDistance;

		    // Set the position
		    slot.decoy.getLocation().set(ship.getLocation().x + offsetX, ship.getLocation().y + offsetY);

		    // Match velocity and facing of the main ship
		    slot.decoy.getVelocity().set(ship.getVelocity());
		    slot.decoy.setFacing(ship.getFacing());

		    // Calculate alpha based on flickering effect
		    float flickerPhase = (engine.getTotalElapsedTime(
			    false) * AFTERIMAGE_FLICKER_SPEED) + slot.flickerOffset;
		    float flickerFactor = 0.5f + 0.5f * (float) Math.sin(flickerPhase * Math.PI);
		    float alpha = AFTERIMAGE_MIN_ALPHA + (AFTERIMAGE_MAX_ALPHA - AFTERIMAGE_MIN_ALPHA) * flickerFactor;

		    // Apply alpha
		    slot.decoy.setAlphaMult(alpha);

		    // Update flux levels to match (scaled)
		    FluxTrackerAPI droneFlux = slot.decoy.getFluxTracker();
		    FluxTrackerAPI shipFlux = ship.getFluxTracker();
		    droneFlux.setCurrFlux(shipFlux.getCurrFlux() * AFTERIMAGE_HULL_ARMOR_FACTOR);
		    droneFlux.setHardFlux(shipFlux.getHardFlux() * AFTERIMAGE_HULL_ARMOR_FACTOR);
		}
	    }
	}

	// Engine effect
	ship.getEngineController().fadeToOtherColor(this, ENGINE_FRINGE_COLOR, ENGINE_CORE_COLOR, effectLevel, 0.8f);
	ship.getEngineController().extendFlame(this, 0.5f * effectLevel, 0.5f * effectLevel, 0.5f * effectLevel);

	// Apply time dilation
	float shipTimeMult = 1f + (TIME_MULT - 1f) * effectLevel;
	stats.getTimeMult().modifyMult(id, shipTimeMult);

	// Adjust player's perception of time
	if (player) {
	    engine.getTimeMult().modifyMult(id, 1f / shipTimeMult);
	} else {
	    engine.getTimeMult().unmodify(id);
	}

	// Apply speed boost based on hull size
	float speedBoost = 0f;
	if (ship.getHullSize() == HullSize.FRIGATE) {
	    speedBoost = FRIGATE_SPEED_BOOST;
	} else if (ship.getHullSize() == HullSize.DESTROYER) {
	    speedBoost = DESTROYER_SPEED_BOOST;
	} else if (ship.getHullSize() == HullSize.CRUISER) {
	    speedBoost = CRUISER_SPEED_BOOST;
	} else if (ship.getHullSize() == HullSize.CAPITAL_SHIP) {
	    speedBoost = CAPITAL_SPEED_BOOST;
	}

	stats.getMaxSpeed().modifyPercent(id, speedBoost * 100f * effectLevel);
	stats.getAcceleration().modifyPercent(id, speedBoost * 100f * effectLevel);
	stats.getDeceleration().modifyPercent(id, speedBoost * 100f * effectLevel);
	stats.getTurnAcceleration().modifyPercent(id, speedBoost * 100f * effectLevel);
	stats.getMaxTurnRate().modifyPercent(id, speedBoost * 100f * effectLevel);
    }

    /**
     * Create a new decoy at the specified position
     */
    private void createDecoy(DecoySlot slot, ShipAPI sourceShip, Vector2f position) {
	CombatEngineAPI engine = Global.getCombatEngine();
	boolean player = sourceShip == engine.getPlayerShip();

	// Create drone ship using sourceShip's variant
	slot.decoy = engine.createFXDrone(sourceShip.getVariant().clone());
	ShipAPI drone = slot.decoy;

	// Set basic properties
	drone.setHullSize(HullSize.FIGHTER);
	drone.setCollisionRadius(sourceShip.getCollisionRadius());
	drone.setOwner(sourceShip.getOwner());
	drone.setAlphaMult(AFTERIMAGE_MIN_ALPHA);
	drone.getLocation().set(position);
	drone.setFacing(sourceShip.getFacing());
	drone.getVelocity().set(sourceShip.getVelocity());

	// Set collision class - allows taking damage but no collision damage
	drone.setCollisionClass(CollisionClass.FIGHTER);

	// Set flags to control behavior
	drone.setDrone(true);
	drone.setShipSystemDisabled(true);
	drone.setInvalidTransferCommandTarget(true);

	// Prevent explosion and derelict creation when destroyed
	drone.setExplosionScale(0f); // No explosion
	drone.setSpawnDebris(false); // No debris

	// Set hull and armor to 1/10 of the source ship's max values
	float maxHitpoints = sourceShip.getMaxHitpoints() * AFTERIMAGE_HULL_ARMOR_FACTOR;
	drone.setMaxHitpoints(maxHitpoints);
	drone.setHitpoints(maxHitpoints);

	// Set armor values
	for (int x = 0; x < drone.getArmorGrid().getGrid().length; x++) {
	    for (int y = 0; y < drone.getArmorGrid().getGrid()[0].length; y++) {
		float originalValue = sourceShip.getArmorGrid().getGrid()[x][y];
		float scaledValue = originalValue * AFTERIMAGE_HULL_ARMOR_FACTOR;
		drone.getArmorGrid().setArmorValue(x, y, scaledValue);
	    }
	}

	// Disable weapons
	String id = "disable_" + drone.getId();
	drone.getMutableStats().getBallisticWeaponFluxCostMod().modifyFlat(id, 100000f);
	drone.getMutableStats().getEnergyWeaponFluxCostMod().modifyFlat(id, 100000f);
	drone.getMutableStats().getMissileWeaponFluxCostMod().modifyFlat(id, 100000f);

	// Set current and max CR
	drone.setCRAtDeployment(sourceShip.getCRAtDeployment());
	drone.setCurrentCR(sourceShip.getCurrentCR());

	// Initialize flux levels
	FluxTrackerAPI droneFlux = drone.getFluxTracker();
	FluxTrackerAPI shipFlux = sourceShip.getFluxTracker();
	droneFlux.setCurrFlux(shipFlux.getCurrFlux() * AFTERIMAGE_HULL_ARMOR_FACTOR);
	droneFlux.setHardFlux(shipFlux.getHardFlux() * AFTERIMAGE_HULL_ARMOR_FACTOR);

	// Make sure there's NO jitter
	drone.setJitter(null, Color.BLACK, 0f, 0, 0f, 0f);
	drone.setJitterUnder(null, Color.BLACK, 0f, 0, 0f, 0f);

	// Add to combat engine
	engine.addEntity(drone);

	// Set timer to indicate it's active
	slot.timer = 999f; // Very high value, will only be removed if destroyed or system deactivated

	// Debug message
	if (player) {
	    engine.addFloatingText(position, "Echo created", 20f, Color.CYAN, drone, 1f, 2f);
	}
    }

    // Helper method to check if there's a ship at a given position
    private boolean isShipAtPosition(Vector2f position, ShipAPI sourceShip) {
	// Get nearby ships within a reasonable radius
	float checkRadius = 500f; // Increased to catch ships with large collision radii

	// We need to account for the source ship's radius too when checking for collisions
	float sourceShipRadius = sourceShip.getCollisionRadius();

	// Get all ships (both friendly and enemy) around this position
	List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(position, checkRadius);

	for (ShipAPI otherShip : nearbyShips) {
	    // Ignore the source ship
	    if (otherShip == sourceShip) {
		continue;
	    }

	    // Skip fighters, modules, and destroyed ships if needed
	    if (otherShip.isShuttlePod() || otherShip.isHulk()) {
		continue;
	    }

	    // Skip our own decoys
	    boolean isOwnDecoy = false;
	    for (DecoySlot slot : decoySlots) {
		if (slot != null && slot.decoy == otherShip) {
		    isOwnDecoy = true;
		    break;
		}
	    }
	    if (isOwnDecoy) {
		continue;
	    }

	    // Calculate distance between position and other ship center
	    float distance =  MathUtils.getDistance(position, otherShip.getLocation());

	    // Consider collision if the distance is less than the sum of both ship radii
	    if (distance < (sourceShipRadius + otherShip.getCollisionRadius())) {
		return true;
	    }
	}

	return false;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
	ShipAPI ship = null;
	if (stats.getEntity() instanceof ShipAPI) {
	    ship = (ShipAPI) stats.getEntity();
	    id = id + "_" + ship.getId();
	} else {
	    return;
	}

	// Reset all modified stats
	Global.getCombatEngine().getTimeMult().unmodify(id);
	stats.getTimeMult().unmodify(id);
	stats.getMaxSpeed().unmodify(id);
	stats.getAcceleration().unmodify(id);
	stats.getDeceleration().unmodify(id);
	stats.getTurnAcceleration().unmodify(id);
	stats.getMaxTurnRate().unmodify(id);
	stats.getHardFluxDissipationFraction().unmodify(id);

	// Mark flowers inactive
	flowersActive = false;

	// Remove all decoys but preserve cooldowns
	for (DecoySlot slot : decoySlots) {
	    if (slot != null && slot.decoy != null) {
		Global.getCombatEngine().removeEntity(slot.decoy);
		slot.decoy = null;

		// If it wasn't already in cooldown, reset timer to 0 (ready to spawn next time)
		if (slot.timer > 0) {
		    slot.timer = 0;
		}
	    }
	}
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
	if (index == 0) {
	    return new StatusData("time accelerated", false);
	} else if (index == 1) {
	    return new StatusData("speed boosted", false);
	} else if (index == 2) {
	    return new StatusData("hard flux dissipation disabled", true);
	} else if (index == 3) {
	    int activeDecoys = 0;
	    for (DecoySlot slot : decoySlots) {
		if (slot != null && slot.decoy != null && slot.decoy.isAlive() && !slot.decoy.isHulk()) {
		    activeDecoys++;
		}
	    }
	    return new StatusData("temporal echoes: " + activeDecoys + "/4", false);
	}
	return null;
    }
}