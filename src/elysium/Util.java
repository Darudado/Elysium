package elysium;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Util
{
    public static final String FACTION_ID = "Elysium";

    // Music state tracking
    public static boolean isElysiumMusicPlaying = false;

    // Visual effect colors
    public static final Color JITTER_COLOR = new Color(219, 166, 255, 75);            // Light purple
    public static final Color JITTER_UNDER_COLOR = new Color(219, 166, 255, 175);     // Deeper purple
    public static final Color ENGINE_CORE_COLOR = new Color(255, 130, 237);           // Pink/magenta
    public static final Color ENGINE_FRINGE_COLOR = new Color(186, 127, 255);         // Purple

    // Flower effect colors - using existing colors but with increased opacity
    public static final Color FLOWER_LARGE_COLOR = new Color(186, 127, 255, 180);     // Purple with more opacity
    public static final Color FLOWER_MEDIUM_COLOR = new Color(219, 166, 255, 180);    // Light purple with more opacity
    public static final Color FLOWER_SMALL_COLOR = new Color(255, 255, 255, 180);     // Pink/magenta with more opacity
    //colors

    public static final Color ELYSIUM_PRIMARY = new Color(0, 255, 255);     // Cyan/Aqua
    public static final Color ELYSIUM_SECONDARY = new Color(0, 150, 255);   // Blue
    public static final Color ELYSIUM_TERTIARY = new Color(0, 180, 255);    // Light Blue

    // Repair/Flux
    public static final Color REPAIR_EFFECT_COLOR = new Color(0, 255, 150, 150);
    public static final Color FLUX_EFFECT_COLOR = new Color(50, 252, 255, 150);



    /**
     * Calculate the angle between two ships
     * @param ship1 The first ship
     * @param ship2 The second ship
     * @return The angle in radians
     */
    public static float getAngleBetweenShips(ShipAPI ship1, ShipAPI ship2) {
        // Get the positions of both ships
        Vector2f ship1Pos = ship1.getLocation();
        Vector2f ship2Pos = ship2.getLocation();

        // Calculate the vector from ship1 to ship2
        Vector2f vectorBetween = new Vector2f(
                ship2Pos.x - ship1Pos.x,
                ship2Pos.y - ship1Pos.y
        );

        // Calculate the angle of this vector
        // The game uses a coordinate system where 0 degrees is to the right
        // and angles increase counterclockwise
        float angle = (float) Math.atan2(vectorBetween.y, vectorBetween.x);

        // Convert to the range [0, 2π] if needed
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }
    /**
     * Get the relative angle between two ships (accounting for ship facing)
     * @param ship1 The reference ship
     * @param ship2 The target ship
     * @return The relative angle in radians
     */
    public static float getRelativeAngleBetweenShips(ShipAPI ship1, ShipAPI ship2) {
        // Get absolute angle between ships
        float absoluteAngle = getAngleBetweenShips(ship1, ship2);

        // Get ship1's facing angle
        float ship1Facing = ship1.getFacing() * (float)Math.PI / 180f; // Convert from degrees to radians

        // Calculate relative angle
        float relativeAngle = absoluteAngle - ship1Facing;

        // Normalize to [0, 2π]
        while (relativeAngle < 0) {
            relativeAngle += 2 * Math.PI;
        }
        while (relativeAngle >= 2 * Math.PI) {
            relativeAngle -= 2 * Math.PI;
        }

        return relativeAngle;
    }

    /**
     * Get the distance between two ships using LazyLib's MathUtils
     * @param ship1 The first ship
     * @param ship2 The second ship
     * @return The distance between the ships
     */
    public static float getDistanceBetweenShips(CombatEntityAPI ship1, CombatEntityAPI ship2) {
        return MathUtils.getDistance(ship1, ship2);
    }
}
