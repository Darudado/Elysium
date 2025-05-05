package elysium.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import elysium.Util;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class ElysSwarmMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private final MissileAPI missile;
    private CombatEntityAPI target;
    private final IntervalUtil timer = new IntervalUtil(0.05f, 0.10f);
    private final IntervalUtil effectTimer = new IntervalUtil(0.05f, 0.1f);
    private float rotationSpeed = 0f;
    private float currentRotation = 0f;
    private boolean accelerationMode = false;
    private final float maxRotationSpeed = 30f; // Increased rotation speed significantly
    private final float accelerationDistance = 500f; // Distance at which to start final acceleration
    private final Color missileColor = Util.ELYSIUM_PRIMARY; // Cyan/Aqua from color reference
    private final Color glowColor = new Color(0, 255, 255, 150); // Cyan with transparency
    private final Color trailColor = new Color(0, 150, 255, 150); // Blue with transparency

    public ElysSwarmMissileAI(MissileAPI missile, ShipAPI launchingShip) {
	this.missile = missile;

	// Set the missile color with full opacity
	if (missile.getSpriteAPI() != null) {
	    missile.getSpriteAPI().setColor(missileColor);
	}

	// Find a suitable target
	if (launchingShip != null && launchingShip.getWeaponGroupFor(missile.getWeapon()) != null) {
	    ShipAPI shipTarget = launchingShip.getWeaponGroupFor(missile.getWeapon()).getAutofirePlugin(missile.getWeapon()).getTargetShip();
	    setTarget(shipTarget);
	}

	// If no target set from weapon autofire, try to find closest valid target
	if (target == null) {
	    setTarget(findBestTarget());
	}

	// Initialize rotation speed (higher value for more visible spinning)
	rotationSpeed = MathUtils.getRandomNumberInRange(20f, maxRotationSpeed);
    }

    private ShipAPI findBestTarget() {
	ShipAPI closest = null;
	float closestDistance = Float.MAX_VALUE;

	for (ShipAPI ship : AIUtils.getEnemiesOnMap(missile)) {
	    if (ship.isHulk()) continue;

	    // Skip fighters unless we're specifically targeting them
	    if (ship.isFighter()) continue;

	    float distance = MathUtils.getDistance(ship.getLocation(), missile.getLocation());
	    if (distance < closestDistance) {
		closest = ship;
		closestDistance = distance;
	    }
	}

	return closest;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
	this.target = target;
    }

    @Override
    public CombatEntityAPI getTarget() {
	return target;
    }

    @Override
    public void advance(float amount) {
	if (missile.isFading() || missile.isFizzling()) return;

	// Handle sprite rotation manually - directly manipulate sprite angle
	currentRotation += rotationSpeed * amount;
	if (currentRotation > 360f) currentRotation -= 360f;

	// Set the sprite angle directly using sprite API
	SpriteAPI sprite = missile.getSpriteAPI();
	if (sprite != null) {
	    sprite.setAngle(currentRotation);
	}

	// Add visual effects for better visibility
	effectTimer.advance(amount);
	if (effectTimer.intervalElapsed()) {
	    // Add particle trail
	    Vector2f missileVel = missile.getVelocity();
	    Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null,
		    MathUtils.getRandomNumberInRange(10f, 30f));
	    Vector2f effectVel = new Vector2f(randomVel.x + missileVel.x * 0.1f,
		    randomVel.y + missileVel.y * 0.1f);

	    // Particle effects around the missile
	    Global.getCombatEngine().addHitParticle(
		    missile.getLocation(),
		    effectVel,
		    MathUtils.getRandomNumberInRange(10f, 15f),
		    0.8f,
		    0.3f,
		    glowColor
	    );

	    // Create trailing particles behind the missile
	    Vector2f trailOffset = MathUtils.getPointOnCircumference(missile.getLocation(),
		    MathUtils.getRandomNumberInRange(5f, 15f),
		    missile.getFacing() + 180f);
	    Global.getCombatEngine().addHitParticle(
		    trailOffset,
		    missile.getVelocity(),
		    MathUtils.getRandomNumberInRange(5f, 10f),
		    0.8f,
		    0.5f,
		    trailColor
	    );

	    // Add glowing effect circle
	    Global.getCombatEngine().addSmoothParticle(
		    missile.getLocation(),
		    missile.getVelocity(),
		    25f,
		    0.7f,
		    0.1f,
		    glowColor
	    );
	}

	// If no target or target is destroyed, try to find a new one
	if (target == null || (target instanceof ShipAPI && ((ShipAPI) target).isHulk())) {
	    target = findBestTarget();
	    if (target == null) {
		missile.giveCommand(ShipCommand.ACCELERATE);
		return;
	    }
	}

	timer.advance(amount);
	if (timer.intervalElapsed()) {
	    // Get direction to target
	    Vector2f targetLocation = target.getLocation();
	    float distance = MathUtils.getDistance(missile.getLocation(), targetLocation);

	    // Lead the target based on its velocity
	    Vector2f leadLocation = target.getLocation();
	    if (target.getVelocity().length() > 0f) {
		float leadTime = distance / missile.getMaxSpeed();
		leadLocation = Vector2f.add(
			target.getLocation(),
			(Vector2f) new Vector2f(target.getVelocity()).scale(leadTime * 0.5f),
			new Vector2f()
		);
	    }

	    // Calculate the angle to the target
	    float angleToTarget = VectorUtils.getAngle(missile.getLocation(), leadLocation);

	    // Determine the shortest rotation direction
	    float angleDiff = MathUtils.getShortestRotation(missile.getFacing(), angleToTarget);

	    // Rotate towards the target
	    if (angleDiff > 1f) {
		missile.giveCommand(ShipCommand.TURN_RIGHT);
	    } else if (angleDiff < -1f) {
		missile.giveCommand(ShipCommand.TURN_LEFT);
	    }

	    // If we're close to the target, switch to acceleration mode
	    if (distance <= accelerationDistance && !accelerationMode) {
		accelerationMode = true;

		// Increase the missile's speed using engine stats
		missile.getEngineStats().getAcceleration().modifyMult(missile.getProjectileSpecId(), 3f);
		missile.getEngineStats().getMaxSpeed().modifyMult(missile.getProjectileSpecId(), 1.5f);

		// Enhanced visual effects for acceleration mode
		for (int i = 0; i < 8; i++) {
		    // Burst of particles to show acceleration
		    Global.getCombatEngine().addHitParticle(
			    missile.getLocation(),
			    MathUtils.getRandomPointInCircle(null, 60f),
			    MathUtils.getRandomNumberInRange(15f, 30f),
			    1f,
			    0.5f,
			    Util.ELYSIUM_PRIMARY
		    );
		}

		// Add a bright flash
		Global.getCombatEngine().addHitParticle(
			missile.getLocation(),
			new Vector2f(0, 0),
			60f,
			1f,
			0.2f,
			new Color(0, 255, 255, 255) // Full opacity cyan
		);
	    }

	    // More particles during acceleration mode
	    if (accelerationMode) {
		Vector2f velBasis = new Vector2f(missile.getVelocity());
		velBasis.scale(-0.3f);

		Global.getCombatEngine().addHitParticle(
			missile.getLocation(),
			velBasis,
			MathUtils.getRandomNumberInRange(20f, 30f),
			0.8f,
			0.2f,
			glowColor
		);
	    }

	    // Always accelerate
	    missile.giveCommand(ShipCommand.ACCELERATE);
	}
    }
}