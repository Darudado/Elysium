package elysium;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ELYS_VoidSingularityPlugin extends BaseEveryFrameCombatPlugin {
    // Sprite paths - using the actual paths from settings
    private static final String FULL_CIRCLE_PATH = "graphics/fx/fullcircle.png";
    private static final String GLOW_SPRITE_PATH = "graphics/fx/glow.png";
    private static final String SHIELD_SPRITE_PATH = "graphics/fx/shields256.png";

    // Timing for effect refresh
    private static float coreRefreshTimer = 0f;
    private static boolean coreActive = false;

    // Colors from Elysium faction palette
    private static final Color CORE_COLOR = new Color(90, 60, 255, 255);          // Deep purple
    private static final Color FRINGE_COLOR = new Color(0, 255, 255, 255);        // Cyan/Aqua
    private static final Color PARTICLE_COLOR = new Color(219, 166, 255, 180);    // Light purple
    private static final Color DAMAGE_ARC_COLOR = new Color(0, 150, 255, 100);    // Blue with low alpha
    private static final Color EMPTY_COLOR = new Color(0, 0, 0, 0);

    // Inner colors for the black hole effect
    private static final Color BLACK_CORE = new Color(0, 0, 0, 255);              // Pure black for center
    private static final Color DARK_PURPLE = new Color(30, 0, 50, 200);           // Very dark purple
    private static final Color MAGENTA_RAYS = new Color(230, 80, 255, 255);       // Bright purple/magenta

    // Effect parameters
    private static final float PULL_FORCE_BASE = 6.0f;
    private static final float MAX_PULL_RANGE = 1800f;
    private static final float DAMAGE_RANGE = 800f;
    private static final float DAMAGE_BASE = 400f;
    private static final float EFFECT_INTERVAL = 0.015f;

    // Safety timeout
    private static final float SAFETY_TIMEOUT = 60f; // 60 seconds max lifetime per singularity

    // Timer for effect updates
    private final IntervalUtil effectTimer = new IntervalUtil(EFFECT_INTERVAL, EFFECT_INTERVAL);

    // Runtime tracking
    private static float totalRuntime = 0f;

    // List to track active singularities
    private static final List<SingularityData> ACTIVE_SINGULARITIES = new ArrayList<>();

    // Storage class for singularity data
    private static class SingularityData {
	private final Vector2f location;
	private float timeRemaining;
	private final List<ShipAPI> affectedShips;
	private final Map<CombatEntityAPI, CollisionClass> collisions;
	private final ShipAPI source;
	private final float power;
	private float visualScale = 0.1f; // Start small and grow
	private float rotation = 0f;      // For rotating visual effects
	private float totalLifetime = 0f; // Track how long this singularity has existed
	private boolean isDying = false;  // Flag for forced cleanup

	// Rotation angles for different layers
	private float outerRingAngle = 0f;
	private float middleRingAngle = 0f;
	private float raysAngle = 0f;

	public SingularityData(Vector2f loc, float time, List<ShipAPI> affected,
		Map<CombatEntityAPI, CollisionClass> collisions, ShipAPI source, float power) {
	    this.location = loc;
	    this.timeRemaining = time;
	    this.affectedShips = affected;
	    this.collisions = collisions;
	    this.source = source;
	    this.power = power;
	    this.rotation = MathUtils.getRandomNumberInRange(0, 360);

	    // Initialize rotation angles with different starting positions
	    this.outerRingAngle = MathUtils.getRandomNumberInRange(0, 360);
	    this.middleRingAngle = MathUtils.getRandomNumberInRange(0, 360);
	    this.raysAngle = MathUtils.getRandomNumberInRange(0, 360);
	}
    }

    @Override
    public void init(CombatEngineAPI engine) {
	ACTIVE_SINGULARITIES.clear();
	totalRuntime = 0f;
	coreRefreshTimer = 0f;
	coreActive = false;
    }

    public static void cleanAll() {
	ACTIVE_SINGULARITIES.clear();
	totalRuntime = 0f;
	coreRefreshTimer = 0f;
	coreActive = false;
    }

    // Method to add a new singularity
    public static void addSingularity(Vector2f location, float duration, List<ShipAPI> affectedShips,
	    Map<CombatEntityAPI, CollisionClass> collisions, ShipAPI source, float power) {

	// Force cleanup any existing singularities before creating a new one
	for (SingularityData existing : ACTIVE_SINGULARITIES) {
	    existing.isDying = true;
	    existing.timeRemaining = Math.min(existing.timeRemaining, 1f); // Force expire soon
	}

	// Ensure the plugin is registered with the combat engine
	CombatEngineAPI engine = Global.getCombatEngine();

	if (engine.getCustomData().get("elysium_singularity_plugin_registered") == null) {
	    ELYS_VoidSingularityPlugin plugin = new ELYS_VoidSingularityPlugin();
	    engine.addPlugin(plugin);
	    engine.getCustomData().put("elysium_singularity_plugin_registered", Boolean.TRUE);
	}

	// Add the singularity data
	ACTIVE_SINGULARITIES.add(new SingularityData(location, duration, affectedShips, collisions, source, power));

	// Initial visual effects
	// Main explosion effect
	engine.spawnExplosion(location, new Vector2f(), FRINGE_COLOR, 300f * power, 1f);

	// 1. Particle burst for dramatic effect (outermost)
	for (int i = 0; i < 100 * power; i++) {
	    float angle = MathUtils.getRandomNumberInRange(0, 360);
	    float distance = MathUtils.getRandomNumberInRange(300f, 800f) * power;
	    Vector2f particlePos = MathUtils.getPoint(location, distance, angle);

	    // Velocity pointing back toward center
	    Vector2f velocity = Vector2f.sub(location, particlePos, new Vector2f());
	    velocity.normalise();
	    velocity.scale(MathUtils.getRandomNumberInRange(100f, 300f));

	    // Random color from our palette
	    Color particleColor;
	    float random = MathUtils.getRandomNumberInRange(0f, 1f);
	    if (random < 0.3f) {
		particleColor = FRINGE_COLOR;
	    } else if (random < 0.6f) {
		particleColor = CORE_COLOR;
	    } else {
		particleColor = PARTICLE_COLOR;
	    }

	    engine.addHitParticle(
		    particlePos,
		    velocity,
		    MathUtils.getRandomNumberInRange(5f, 15f),
		    1f,
		    MathUtils.getRandomNumberInRange(0.5f, 1.5f),
		    particleColor
	    );
	}
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
	CombatEngineAPI engine = Global.getCombatEngine();
	if (engine.isPaused()) return;

	// Track total runtime for safety timeout
	totalRuntime += amount;

	// Update core refresh timer
	coreRefreshTimer -= amount;

	// Force cleanup if no singularities exist but plugin is still running
	if (ACTIVE_SINGULARITIES.isEmpty() && totalRuntime > 60f) {
	    Global.getCombatEngine().removePlugin(this);
	    Global.getCombatEngine().getCustomData().remove("elysium_singularity_plugin_registered");
	    coreActive = false;
	    return;
	}

	// Process singularity effects at fixed intervals
	effectTimer.advance(amount);
	if (effectTimer.intervalElapsed() && !ACTIVE_SINGULARITIES.isEmpty()) {
	    processSingularityEffects(amount, engine);
	}
    }

    private void processSingularityEffects(float amount, CombatEngineAPI engine) {
	for (Iterator<SingularityData> iterator = ACTIVE_SINGULARITIES.iterator(); iterator.hasNext();) {
	    SingularityData singularity = iterator.next();

	    singularity.timeRemaining -= EFFECT_INTERVAL;
	    singularity.totalLifetime += EFFECT_INTERVAL;

	    if (singularity.totalLifetime > SAFETY_TIMEOUT) {
		singularity.timeRemaining = -1f;
	    }

	    // Create pulsing effect timing - oscillates between 0 and 1
	    float pulseTime = engine.getTotalElapsedTime(true) * 4f;
	    float pulseFactor = 0.5f + 0.5f * (float)Math.sin(pulseTime);

	    // Update rotation angles at different speeds
	    singularity.rotation += (6f + 4f * pulseFactor) * amount * singularity.power;
	    singularity.outerRingAngle += 15f * amount * singularity.power;
	    singularity.middleRingAngle -= 25f * amount * singularity.power;
	    singularity.raysAngle += 5f * amount * singularity.power;

	    // Growth and shrinking
	    if (singularity.timeRemaining > 3f && singularity.visualScale < 1f) {
		singularity.visualScale += 0.15f;
	    } else if (singularity.timeRemaining < 1f || singularity.isDying) {
		singularity.visualScale = Math.max(0.2f, singularity.visualScale - 0.03f);
	    }

	    // Check if singularity has expired
	    if (singularity.timeRemaining < 0 || singularity.isDying) {
		// Play collapse sound
		Global.getSoundPlayer().playSound("system_emp_emitter_activate", 1f, 0.9f, singularity.location, new Vector2f());

		// Restore ship stats
		for (ShipAPI ship : singularity.affectedShips) {
		    if (ship.isAlive()) {
			ship.getMutableStats().getAcceleration().unmodify("elys_singularity");
			ship.getMutableStats().getTurnAcceleration().unmodify("elys_singularity");
		    }
		}

		// Restore collision classes
		for (CombatEntityAPI entity : singularity.collisions.keySet()) {
		    if (engine.isEntityInPlay(entity) && entity.getCollisionClass() == CollisionClass.ASTEROID) {
			entity.setCollisionClass(singularity.collisions.get(entity));
		    }
		}

		// Mark core as inactive
		coreActive = false;

		// Final collapse visual
		engine.spawnExplosion(
			singularity.location,
			new Vector2f(),
			FRINGE_COLOR,
			300f * singularity.power,
			1.5f
		);

		// Additional collapse explosion
		engine.spawnExplosion(
			singularity.location,
			new Vector2f(),
			CORE_COLOR,
			200f * singularity.power,
			1.0f
		);

		// Outward burst of particles on collapse
		for (int i = 0; i < 150 * singularity.power; i++) {
		    float angle = MathUtils.getRandomNumberInRange(0, 360);
		    float speed = MathUtils.getRandomNumberInRange(100f, 700f);
		    Vector2f velocity = MathUtils.getPoint(new Vector2f(), speed, angle);

		    engine.addHitParticle(
			    singularity.location,
			    velocity,
			    MathUtils.getRandomNumberInRange(3f, 15f) * singularity.power,
			    1f,
			    MathUtils.getRandomNumberInRange(0.5f, 2.0f),
			    Math.random() < 0.5f ? FRINGE_COLOR : CORE_COLOR
		    );
		}

		// Remove this singularity
		iterator.remove();
	    } else {
		// Apply active singularity effects
		float effectPower = Math.min(2f, singularity.timeRemaining) / 2f * singularity.power;
		effectPower = Math.max(effectPower, singularity.power * 0.75f);

		// Apply gravitational pull to nearby entities
		List<CombatEntityAPI> nearbyEntities = CombatUtils.getEntitiesWithinRange(singularity.location, MAX_PULL_RANGE);
		if (nearbyEntities != null) {
		    for (CombatEntityAPI entity : nearbyEntities) {
			// Skip modules and fixed stations
			if (entity instanceof ShipAPI &&
				((ShipAPI)entity).getParentStation() != null &&
				((ShipAPI)entity).getVariant().getHullMods().contains("axialrotation")) {
			    continue;
			}

			// Skip the source ship
			if (entity == singularity.source) continue;

			// Calculate distance and pull force
			float distance = MathUtils.getDistance(entity.getLocation(), singularity.location);
			float angle = VectorUtils.getAngle(entity.getLocation(), singularity.location);

			if (distance < MAX_PULL_RANGE) {
			    // Pull force decreases with distance
			    float distanceFactor = Math.max(0.25f, 1f - (distance / MAX_PULL_RANGE));
			    if (distance < 600f) {
				distanceFactor *= 1.5f;
			    }

			    float force = PULL_FORCE_BASE * effectPower * distanceFactor;

			    // Apply special effects for ships
			    if (entity instanceof ShipAPI) {
				ShipAPI ship = (ShipAPI)entity;

				// Apply damage based on proximity
				/*
				if (distance < DAMAGE_RANGE) {
				    float damageAmount = DAMAGE_BASE * effectPower * (1f - (distance / DAMAGE_RANGE));

				    if (Math.random() < 0.15f) {
					int numDamageArcs = 2 + (int)(Math.random() * 3);
					for (int i = 0; i < numDamageArcs; i++) {
					    engine.spawnEmpArcPierceShields(
						    singularity.source,
						    singularity.location,
						    ship,
						    ship,
						    DamageType.FRAGMENTATION,
						    damageAmount / numDamageArcs,
						    0f,
						    1000f,
						    null,
						    0.8f,
						    EMPTY_COLOR,
						    DAMAGE_ARC_COLOR
					    );
					}
				    }
				}
				*/


				// Visual afterimage effect showing being pulled
				if (Math.random() < 0.3f && distance < 1000f) {
				    Vector2f velocityOffset = MathUtils.getPoint(new Vector2f(), force * 10f, angle);
				    Color afterimageColor = new Color(
					    0.1f,
					    0.3f,
					    0.5f,
					    0.4f * effectPower
				    );
				    ship.addAfterimage(
					    afterimageColor,
					    0f, 0f,
					    velocityOffset.x, velocityOffset.y,
					    1f, 0.1f, 0.2f, 0.3f,
					    false, true, false
				    );
				}

				// Ship size factor for pull force
				if (ship.getHullSize() == ShipAPI.HullSize.FIGHTER) {
				    force *= 4.0f;
				} else if (ship.getHullSize() == ShipAPI.HullSize.FRIGATE) {
				    force *= 2.0f;
				} else if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) {
				    force *= 1.5f;
				} else if (ship.getHullSize() == ShipAPI.HullSize.CRUISER) {
				    force *= 1.0f;
				}
			    }

			    // Stronger pull for projectiles
			    if (entity instanceof DamagingProjectileAPI) {
				force *= 20f;

				// Missiles sometimes get friendly fire enabled
				if (entity instanceof MissileAPI && Math.random() < 0.5f * effectPower) {
				    if (((MissileAPI)entity).getCollisionClass() == CollisionClass.MISSILE_NO_FF) {
					((MissileAPI)entity).setCollisionClass(CollisionClass.MISSILE_FF);
				    }
				}
			    }

			    // Apply direct force
			    Vector2f direction = MathUtils.getPoint(new Vector2f(), 1f, angle);
			    direction.scale(force);
			    Vector2f.add(direction, entity.getVelocity(), entity.getVelocity());

			    // Update facing of projectiles to match new velocity
			    if (entity instanceof DamagingProjectileAPI) {
				entity.setFacing(VectorUtils.getFacing(entity.getVelocity()));
			    }
			}
		    }
		}

		// Apply movement debuffs to affected ships
		for (ShipAPI ship : singularity.affectedShips) {
		    if (ship.isAlive()) {
			float debuffFactor = Math.max(0.4f, (2f - effectPower) / 2.5f);
			ship.getMutableStats().getAcceleration().modifyMult("elys_singularity", debuffFactor);
			ship.getMutableStats().getTurnAcceleration().modifyMult("elys_singularity", debuffFactor);
		    }
		}

		// Visual effects - using MagicRender instead of particles
		if (engine.isEntityInPlay(singularity.source)) {
		    renderMagicSingularity(engine, singularity, pulseFactor, pulseTime);
		}
	    }
	}
    }

    private void renderMagicSingularity(CombatEngineAPI engine, SingularityData singularity, float pulseFactor, float pulseTime) {
	// Only create new sprites if timer is up or sprites aren't active
	if ((coreRefreshTimer <= 0f || !coreActive) && singularity.visualScale > 0.2f) {
	    // Reset timer - controls how often we refresh the sprites
	    coreRefreshTimer = 0.05f; // Refresh every 0.05 seconds for smoother rotation
	    coreActive = true;

	    // Calculate base size for the singularity
	    float baseSize = 500f * singularity.visualScale * singularity.power;
	    float expansionFactor = 0.8f + 0.3f * pulseFactor;

	    // Load sprites
	    SpriteAPI fullCircleSprite = Global.getSettings().getSprite(FULL_CIRCLE_PATH);
	    SpriteAPI glowSprite = Global.getSettings().getSprite(GLOW_SPRITE_PATH);
	    SpriteAPI shieldSprite = Global.getSettings().getSprite(SHIELD_SPRITE_PATH);

	    // =====================================================================
	    // 1. BLACK CORE (INNERMOST LAYER)
	    // =====================================================================

	    // Center black core
	    MagicRender.battlespace(
		    fullCircleSprite,             // sprite
		    singularity.location,         // loc
		    new Vector2f(0f, 0f),         // vel
		    new Vector2f(baseSize * 0.25f, baseSize * 0.25f), // size - smaller core
		    new Vector2f(0f, 0f),         // growth
		    0f,                           // angle
		    0f,                           // spin
		    Color.BLACK,                  // color
		    false,                        // non-additive for solid black
		    0f, 0f, 0f, 1f, 0f,           // jitter/flicker params
		    0.05f, 0.1f, 0.05f,           // fade params
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );

	    // =====================================================================
	    // 2. PURPLE RAYS (REVERTED TO STATIC VERSION)
	    // =====================================================================

	    // Create magenta/purple rays radiating from the center with varying lengths
	    int numRays = 20;
	    for (int i = 0; i < numRays; i++) {
		float angle = i * (360f / numRays) + singularity.raysAngle;

		// Vary the ray length with wave pattern
		float rayLengthVariation = 0.8f + 0.4f * (float)Math.sin(angle * 2f + pulseTime);

		// Start point just at edge of black core
		Vector2f rayStart = MathUtils.getPoint(singularity.location, baseSize * 0.15f, angle);

		// End point with varied length
		Vector2f rayEnd = MathUtils.getPoint(singularity.location,
			baseSize * 0.25f * rayLengthVariation,
			angle);

		// Calculate ray velocity from start to end
		Vector2f rayVel = Vector2f.sub(rayEnd, rayStart, new Vector2f());
		rayVel.scale(8f); // Speed to create line effect

		// Create ray using glow sprite stretched to create a line - thinner rays
		MagicRender.battlespace(
			glowSprite,
			rayStart,
			rayVel,
			new Vector2f(baseSize * 0.02f, baseSize * 0.15f * rayLengthVariation),
			new Vector2f(0f, 0f),
			angle,
			0f,
			new Color(225, 80, 255, 180), // Purple rays
			true,
			0f, 0f, 0f, 1f, 0f,
			0.02f, 0.05f, 0.02f,
			CombatEngineLayers.BELOW_SHIPS_LAYER
		);
	    }

	    // =====================================================================
	    // 3. SIMPLER, MORE RELIABLE MOVING PARTICLES
	    // =====================================================================

	    // Create several clearly moving particles that orbit the core
	    int numOrbitalParticles = 24;
	    for (int i = 0; i < numOrbitalParticles; i++) {
		// Calculate position around a circle with some variation
		float angle = i * (360f / numOrbitalParticles) + singularity.rotation * 0.1f;
		float distance = baseSize * (0.4f + 0.1f * (float)Math.sin(i + pulseTime));
		Vector2f particlePos = MathUtils.getPoint(singularity.location, distance, angle);

		// Calculate a VERY clear orbital velocity - simple but effective
		float speed = 200f * singularity.power; // High speed for clear movement
		float orbitAngle = angle + 90f; // Tangential
		Vector2f velocity = MathUtils.getPoint(new Vector2f(), speed, orbitAngle);

		// Vary colors
		Color particleColor;
		if (i % 3 == 0) {
		    particleColor = new Color(225, 80, 255, 150); // Purple
		} else if (i % 3 == 1) {
		    particleColor = new Color(80, 180, 255, 150); // Blue
		} else {
		    particleColor = new Color(140, 50, 220, 150); // Deeper purple
		}

		// Create clearly moving particle
		MagicRender.battlespace(
			glowSprite,
			particlePos,
			velocity,
			new Vector2f(baseSize * 0.04f, baseSize * 0.04f),
			new Vector2f(0f, 0f),
			0f,
			0f,
			particleColor,
			true,
			0f, 0f, 0f, 1f, 0f,
			0.05f, 0.1f, 0.05f,
			CombatEngineLayers.BELOW_SHIPS_LAYER
		);
	    }

	    // =====================================================================
	    // 4. SECONDARY ORBITAL PARTICLES ON DIFFERENT RADIUS
	    // =====================================================================

	    // Create a second ring of particles moving in the opposite direction
	    int numSecondaryParticles = 16;
	    for (int i = 0; i < numSecondaryParticles; i++) {
		float angle = i * (360f / numSecondaryParticles) - singularity.rotation * 0.15f;
		float distance = baseSize * (0.7f + 0.05f * (float)Math.sin(i * 2f + pulseTime));
		Vector2f particlePos = MathUtils.getPoint(singularity.location, distance, angle);

		// Calculate velocity - opposite direction
		float speed = 150f * singularity.power;
		float orbitAngle = angle - 90f; // Opposite direction
		Vector2f velocity = MathUtils.getPoint(new Vector2f(), speed, orbitAngle);

		// Color
		Color particleColor = new Color(40, 100, 220, 120); // Blue

		// Create particle
		MagicRender.battlespace(
			glowSprite,
			particlePos,
			velocity,
			new Vector2f(baseSize * 0.03f, baseSize * 0.03f),
			new Vector2f(0f, 0f),
			0f,
			0f,
			particleColor,
			true,
			0f, 0f, 0f, 1f, 0f,
			0.05f, 0.1f, 0.05f,
			CombatEngineLayers.BELOW_SHIPS_LAYER
		);
	    }

	    // =====================================================================
	    // 5. INWARD FLOWING PARTICLES
	    // =====================================================================

	    // Create particles that clearly flow toward the center
	    int numInflowParticles = 8;
	    for (int i = 0; i < numInflowParticles; i++) {
		float angle = i * (360f / numInflowParticles) + singularity.rotation * 0.2f;
		float distance = baseSize * (0.9f + 0.1f * (float)Math.sin(i + pulseTime * 2f));
		Vector2f particlePos = MathUtils.getPoint(singularity.location, distance, angle);

		// Inward velocity with spiral component
		float inSpeed = 120f * singularity.power;
		float inAngle = angle + 180f; // Toward center
		Vector2f inVelocity = MathUtils.getPoint(new Vector2f(), inSpeed, inAngle);

		// Add spiral component
		float spiralSpeed = 80f * singularity.power;
		float spiralAngle = angle + 90f; // Tangential
		Vector2f spiralVelocity = MathUtils.getPoint(new Vector2f(), spiralSpeed, spiralAngle);

		// Combine
		Vector2f finalVelocity = new Vector2f();
		Vector2f.add(inVelocity, spiralVelocity, finalVelocity);

		// Color
		Color particleColor = new Color(180, 70, 220, 150); // Purple

		// Create inflow particle
		MagicRender.battlespace(
			glowSprite,
			particlePos,
			finalVelocity,
			new Vector2f(baseSize * 0.045f, baseSize * 0.045f),
			new Vector2f(0f, 0f),
			0f,
			0f,
			particleColor,
			true,
			0f, 0f, 0f, 1f, 0f,
			0.05f, 0.1f, 0.05f,
			CombatEngineLayers.BELOW_SHIPS_LAYER
		);
	    }

	    // =====================================================================
	    // 6. OUTER RINGS
	    // =====================================================================

	    // Outer cyan ring
	    MagicRender.battlespace(
		    shieldSprite,
		    singularity.location,
		    new Vector2f(0f, 0f),
		    new Vector2f(baseSize * 1.1f, baseSize * 1.1f),
		    new Vector2f(0f, 0f),
		    singularity.outerRingAngle,
		    60f * singularity.power, // Faster spin
		    new Color(FRINGE_COLOR.getRed(), FRINGE_COLOR.getGreen(), FRINGE_COLOR.getBlue(), 60),
		    true,
		    0f, 0f, 0f, 1f, 0f,
		    0.05f, 0.1f, 0.05f,
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );

	    // Middle purple ring - counter-rotating
	    SpriteAPI shieldsprite2 = Global. getSettings().getSprite(SHIELD_SPRITE_PATH);
	    MagicRender.battlespace(
		    shieldsprite2,
		    singularity.location,
		    new Vector2f(0f, 0f),
		    new Vector2f(baseSize * 0.7f, baseSize * 0.7f),
		    new Vector2f(0f, 0f),
		    singularity.middleRingAngle,
		    -80f * singularity.power, // Faster spin
		    new Color(CORE_COLOR.getRed(), CORE_COLOR.getGreen(), CORE_COLOR.getBlue(), 70),
		    true,
		    0f, 0f, 0f, 1f, 0f,
		    0.05f, 0.1f, 0.05f,
		    CombatEngineLayers.BELOW_SHIPS_LAYER
	    );
	}

	// Keep some background particle effects but at reduced rate
	if (Math.random() < 0.3f) {
	    renderSwirlingParticles(engine, singularity, pulseFactor, pulseTime, 500f * singularity.visualScale * singularity.power);
	}
    }
    // Helper method to render swirling particles around the main singularity
    private void renderSwirlingParticles(CombatEngineAPI engine, SingularityData singularity, float pulseFactor, float pulseTime, float baseSize) {
	// Fast orbital speed that varies with pulsing
	float baseOrbitalSpeed = 300f * singularity.power;
	float currentOrbitalSpeed = baseOrbitalSpeed * (0.7f + 0.7f * pulseFactor);

	// Dynamic spiral arm effect
	if (Math.random() < 0.6f) {
	    float spiralStartRadius = baseSize * 0.5f; // Start outside the core
	    float spiralEndRadius = baseSize * 1.2f * pulseFactor;
	    float totalAngle = 1080f + 360f * pulseFactor; // Rotation varies with pulse
	    int segments = 20; // Reduced from 40 to reduce particle count

	    for (int i = 0; i < segments; i++) {
		float progress = (float)i / segments;
		float angle = progress * totalAngle + singularity.rotation * (1.5f + progress);
		float radius = spiralStartRadius + (spiralEndRadius - spiralStartRadius) * progress;

		Vector2f spiralPoint = MathUtils.getPoint(singularity.location, radius, angle);

		// Rapid circular motion
		float tangentSpeed = 300f * singularity.power * (1f - progress) * (0.7f + 0.6f * pulseFactor);
		float tangentAngle = angle + 90f;
		Vector2f velocity = MathUtils.getPoint(new Vector2f(), tangentSpeed, tangentAngle);

		// Add pulsing radial motion
		float radialDir = Math.sin(progress * 10f + pulseTime * 3f) > 0 ? 1f : -1f;
		float radialSpeed = 100f * singularity.power * (1f - progress) * Math.abs(radialDir);
		float radialAngle = angle + (radialDir > 0 ? 0f : 180f);
		Vector2f radialVelocity = MathUtils.getPoint(new Vector2f(), radialSpeed, radialAngle);

		// Combine velocities
		Vector2f.add(velocity, radialVelocity, velocity);

		float particleSize = 15f * (1f - progress) * singularity.power;

		engine.addSmoothParticle(
			spiralPoint,
			velocity,
			particleSize,
			0.7f,
			0.1f,
			Math.random() < 0.5f ? FRINGE_COLOR : CORE_COLOR
		);
	    }
	}

	// Particles being drawn in with spiral motion - very few for performance
	if (Math.random() < 0.7f) {
	    for (int i = 0; i < 3; i++) {
		float radius = baseSize * (1.0f + 0.5f * (float)Math.random());
		float particleAngle = (float)Math.random() * 360f;
		Vector2f particlePos = MathUtils.getPoint(singularity.location, radius, particleAngle);

		// Strong inward velocity that pulses
		float velocityMagnitude = 250f + 450f * (float)Math.random() * pulseFactor;
		float inwardAngle = VectorUtils.getAngle(particlePos, singularity.location);
		Vector2f particleVel = MathUtils.getPoint(new Vector2f(), velocityMagnitude, inwardAngle);

		// Strong spiral component
		float tangentMagnitude = velocityMagnitude * 0.7f * (0.8f + 0.4f * (float)Math.random());
		float tangentAngle = inwardAngle + (Math.random() < 0.5f ? 90f : -90f);
		Vector2f tangentialVel = MathUtils.getPoint(new Vector2f(), tangentMagnitude, tangentAngle);
		Vector2f.add(particleVel, tangentialVel, particleVel);

		// Color varies with distance - clamped to prevent invalid values
		float ratio = Math.min(1.0f, radius / baseSize); // Ensure ratio never exceeds 1.0f
		Color particleColor = new Color(
			0.3f,
			Math.min(0.9f, 0.3f + 0.6f * ratio), // Clamp to max 0.9f
			Math.min(1.0f, 0.6f + 0.4f * ratio), // Clamp to max 1.0f
			0.8f
		);
		engine.addSmoothParticle(
			particlePos,
			particleVel,
			5f + 15f * (float)Math.random() * singularity.power,
			0.8f,
			0.3f + 0.3f * (float)Math.random(),
			particleColor
		);
	    }
	}
    }
}