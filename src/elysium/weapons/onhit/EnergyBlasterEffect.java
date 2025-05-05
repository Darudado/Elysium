package elysium.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.util.IntervalUtil;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class EnergyBlasterEffect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(0.5f, 2.5f); // Longer charge time
    private boolean wasZero = true;
    boolean runOnce = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
	WeaponAPI weapon = beam.getWeapon();
	float range = beam.getBrightness() * 500f;

	// Visual effect during charging
	MagicRender.battlespace(
		Global.getSettings().getSprite("graphics/fx/particlealpha_textured.png"),
		beam.getFrom(),
		new Vector2f(),
		new Vector2f(25f, 25f),
		new Vector2f(range, range * 0.5f),
		weapon.getCurrAngle() - 90f,
		0f,
		new Color(0, 255, 255, Math.round(beam.getBrightness() * 50f)),
		true,
		0.07f,
		0f,
		0.14f
	);

	if (beam.getBrightness() >= 1f) {
	    float dur = beam.getDamage().getDpsDuration();
	    // Needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
	    if (!wasZero) dur = 0;
	    wasZero = beam.getDamage().getDpsDuration() <= 0;
	    fireInterval.advance(dur);

	    // When fully charged, fire the projectile
	    if (!runOnce) {
		runOnce = true;

		// Determine which projectile to spawn based on weapon ID
		String weaponSpecId = "";
		if (weapon.getId().equals("elys_energy_blaster_small")) {
		    weaponSpecId = "elys_energy_blaster_small_projectile_shot_hidden";
		} else if (weapon.getId().equals("elys_energy_blaster_medium")) {
		    weaponSpecId = "elys_energy_blaster_medium_projectile_shot_hidden";
		} else if (weapon.getId().equals("elys_energy_blaster_large")) {
		    weaponSpecId = "elys_energy_blaster_large_projectile_shot_hidden";
		} else {
		    // Default fallback
		    weaponSpecId = "elys_energy_blaster_small_projectile_shot_hidden";
		}

		// Play sound effect
		Global.getSoundPlayer().playSound("plasma_cannon_fire", 0.9f, 1.0f, beam.getFrom(), new Vector2f());

		// Fire projectile directly
		engine.spawnProjectile(
			weapon.getShip(),
			weapon,
			weaponSpecId,
			beam.getFrom(),
			weapon.getCurrAngle(),
			new Vector2f()
		);

		// Adjust beam width based on charge level, with proper bounds
		if (weapon.getChargeLevel() >= 0.5f) {
		    // Calculate a multiplier that scales from 1.0 to 2.0 as charge goes from 0.5 to 1.0
		    float chargeScale = 1.0f + 2.0f * (weapon.getChargeLevel() - 0.5f);

		    // Apply the multiplier directly (no negative factor)
		    beam.setWidth(beam.getWidth() * chargeScale);

		    // Set a maximum width limit if needed
		    float maxWidth = beam.getWidth() * 2.5f; // example: 2.5x original width
		    if (beam.getWidth() > maxWidth) {
			beam.setWidth(maxWidth);
		    }
		}
	    }
	} else {
	    runOnce = false;
	}
    }
}