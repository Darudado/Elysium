package elysium.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class EnergyBlasterAltEffect implements BeamEffectPlugin {

    private IntervalUtil fireInterval = new IntervalUtil(0.5f, 3.0f); // Even longer charge time
    private boolean wasZero = true;
    boolean runOnce = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
	WeaponAPI weapon = beam.getWeapon();
	float range = beam.getBrightness() * 600f;

	// More dramatic visual effect during charging
	MagicRender.battlespace(
		Global.getSettings().getSprite("graphics/fx/particlealpha_textured.png"),
		beam.getFrom(),
		new Vector2f(),
		new Vector2f(35f, 35f),
		new Vector2f(range * 1.2f, range * 0.6f),
		weapon.getCurrAngle() - 90f,
		MathUtils.getRandomNumberInRange(-10f, 10f),
		new Color(0, 255, 255, Math.round(beam.getBrightness() * 60f)),
		true,
		0.08f,
		0f,
		0.16f
	);

	// Additional visual effect
	if (beam.getBrightness() > 0.7f) {
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/starscape/star1.png"),
		    beam.getFrom(),
		    new Vector2f(),
		    new Vector2f(30f, 30f),
		    new Vector2f(150f * beam.getBrightness(), 150f * beam.getBrightness()),
		    MathUtils.getRandomNumberInRange(0, 360),
		    0f,
		    new Color(0, 255, 255, Math.round(beam.getBrightness() * 70f)),
		    true,
		    0.1f,
		    0f,
		    0.2f
	    );
	}

	if (beam.getBrightness() >= 1f) {
	    float dur = beam.getDamage().getDpsDuration();
	    if (!wasZero) dur = 0;
	    wasZero = beam.getDamage().getDpsDuration() <= 0;
	    fireInterval.advance(dur);

	    // When fully charged, fire multiple projectiles in a pattern
	    if (!runOnce) {
		runOnce = true;

		// Play enhanced sound effect
		Global.getSoundPlayer().playSound("plasma_cannon_fire", 1.0f, 1.2f, beam.getFrom(), new Vector2f());

		// Fire multiple projectiles in a spread pattern
		for (int i = -1; i <= 1; i++) {
		    float angleOffset = i * 10f; // 5-degree spread between projectiles

		    engine.spawnProjectile(
			    weapon.getShip(),
			    weapon,
			    "elys_energy_blaster_largeALT_projectile_shot_hidden",
			    beam.getFrom(),
			    weapon.getCurrAngle() + angleOffset,
			    new Vector2f()
		    );
		}

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