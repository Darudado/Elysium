package elysium.weapons.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class EnergyBlasterSound implements EveryFrameWeaponEffectPlugin {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	if (engine == null || engine.isPaused()) {
	    return;
	}

	// Visual charging effect when at high charge level
	if (weapon.getChargeLevel() > 0.8f) {
	    float intensity = weapon.getChargeLevel() - 0.8f; // 0 to 0.2
	    intensity = intensity * 5f; // Scale to 0-1

	    // Determine size based on weapon size
	    float baseSize = 25f;
	    float glowMultiplier = 1f;

	    if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) {
		baseSize = 35f;
		glowMultiplier = 1.5f;
	    } else if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) {
		baseSize = 50f;
		glowMultiplier = 2f;
	    }

	    // Get appropriate color based on weapon type
	    Color glowColor = new Color(0, 255, 255, Math.round(intensity * 60f));
	    if (weapon.getId().contains("alt")) {
		glowColor = new Color(0, 255, 255, Math.round(intensity * 60f));
	    }

	    // Charging effect at weapon muzzle
	    MagicRender.battlespace(
		    Global.getSettings().getSprite("graphics/fx/explosion_core.png"),
		    weapon.getFirePoint(0),
		    new Vector2f(),
		    new Vector2f(baseSize * intensity, baseSize * intensity),
		    new Vector2f(baseSize * intensity * 2f, baseSize * intensity * 2f),
		    weapon.getCurrAngle() - 90f,
		    0f,
		    glowColor,
		    true,
		    0.1f,
		    0.05f,
		    0.1f
	    );
	}

	// Play charging sound
	if (weapon.getCooldownRemaining() == 0 && weapon.getChargeLevel() > 0.2f) {
	    float pitch = 0.8f + weapon.getChargeLevel() * 0.4f; // 0.8 to 1.2
	    float volume = 0.4f + (weapon.getChargeLevel() * 0.6f); // 0.4 to 1.0

	    // Different sounds for different sizes and types
	    String soundId = "high_intensity_laser_loop";


	    Global.getSoundPlayer().playLoop(soundId, weapon, pitch, volume, weapon.getLocation(), new Vector2f(0, 0), 0.75f, 0.25f);
	}
    }
}