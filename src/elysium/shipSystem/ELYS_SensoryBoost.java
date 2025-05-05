package elysium.shipSystem;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public class ELYS_SensoryBoost extends BaseShipSystemScript {

    // Configuration values
    public static final float SPEED_BOOST = 20f;         // Flat speed bonus in su/s
    public static final float SENSOR_RANGE_MULT = 1.75f; // Multiplicative bonus to sensor range
    public static final float ACCELERATION_BONUS = 1.5f; // Bonus to acceleration

    // Visual effects
    private static final Color JITTER_COLOR = new Color(200, 235, 245, 125);
    private static final Color AFTER_IMAGE_COLOR = new Color(200, 235, 245, 30);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
	ShipAPI ship = null;
	if (stats.getEntity() instanceof ShipAPI) {
	    ship = (ShipAPI) stats.getEntity();
	}

	// Apply speed boost
	stats.getMaxSpeed().modifyFlat(id, SPEED_BOOST * effectLevel);
	stats.getAcceleration().modifyMult(id, 1f + (ACCELERATION_BONUS - 1f) * effectLevel);
	stats.getDeceleration().modifyMult(id, 1f + (ACCELERATION_BONUS - 1f) * effectLevel);

	// Apply sensor boost
	stats.getSightRadiusMod().modifyMult(id, 1f + (SENSOR_RANGE_MULT - 1f) * effectLevel);

	// Visual effects if we have a ship
	if (ship != null) {
	    // Intensity varies with effect level
	    float jitterLevel = effectLevel * 0.5f;
	    float jitterRangeBonus = 0;
	    float maxRangeBonus = 10f;

	    if (state == State.IN) {
		jitterRangeBonus = jitterLevel * maxRangeBonus;
	    } else if (state == State.ACTIVE) {
		jitterRangeBonus = maxRangeBonus;
	    } else if (state == State.OUT) {
		jitterRangeBonus = jitterLevel * maxRangeBonus;
	    }

	    jitterLevel = Math.min(1f, jitterLevel + 0.2f);

	    // Add visual "jitter" effect to the ship
	    ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 5f, jitterRangeBonus);

	    // Add faint after-images when moving (more visible at higher speeds)
	    if (effectLevel > 0.5f) {
		ship.addAfterimage(
			AFTER_IMAGE_COLOR,
			0, 0,
			-ship.getVelocity().x * 0.05f,
			-ship.getVelocity().y * 0.05f,
			5f * effectLevel,
			0.1f,
			0.5f,
			0.5f,
			false,
			false,
			false
		);
	    }
	}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
	stats.getMaxSpeed().unmodify(id);
	stats.getAcceleration().unmodify(id);
	stats.getDeceleration().unmodify(id);
	stats.getSightRadiusMod().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
	if (index == 0) {
	    return new StatusData("+" + (int)(SPEED_BOOST * effectLevel) + " speed", false);
	} else if (index == 1) {
	    return new StatusData("+" + (int)((SENSOR_RANGE_MULT - 1f) * 100f * effectLevel) + "% sensor range", false);
	}
	return null;
    }
}