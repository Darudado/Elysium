package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static elysium.Util.FACTION_ID;
import static elysium.Util.FACTION_ID_VOID;

public class ELYS_OverchargedMunitions extends BaseHullMod {

    // Configuration settings
    private static final float ENERGY_DAMAGE_PERCENT = 0.30f; // 30% additional energy damage
    private static final float EMP_ARC_CHANCE = 0.20f; // 20% chance to cause EMP arcs
    private static final float MISSILE_HEALTH_PENALTY = 0.25f; // 25% less missile health
    private static final float RELOAD_TIME_PENALTY = 0.20f; // 20% increased reload time
    private static final float SMOD_RELOAD_PENALTY = 0.40f; // 40% reload time penalty when S-modded

    // EMP arc settings
    private static final float EMP_ARC_RANGE = 50f; // Range for EMP arcs
    private static final Color EMP_CORE_COLOR = new Color(100, 150, 255, 255);
    private static final Color EMP_FRINGE_COLOR = new Color(200, 225, 255, 175);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Apply missile reload time penalty
	boolean sMod = isSMod(stats);
	float reloadPenalty = sMod ? SMOD_RELOAD_PENALTY : RELOAD_TIME_PENALTY;
	stats.getMissileRoFMult().modifyMult(id, 1f - reloadPenalty);

	// Apply missile health penalty
	stats.getMissileHealthBonus().modifyMult(id, 1f - MISSILE_HEALTH_PENALTY);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	// Add the missile tracker to handle our custom missile effects
	ship.addListener(new OverchargedMunitionsTracker(ship));
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(ENERGY_DAMAGE_PERCENT * 100) + "%";
	if (index == 1) return Math.round(EMP_ARC_CHANCE * 100) + "%";
	if (index == 2) return Math.round(MISSILE_HEALTH_PENALTY * 100) + "%";
	if (index == 3) return Math.round(RELOAD_TIME_PENALTY * 100) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(SMOD_RELOAD_PENALTY * 100) + "%";
	return null;
    }

    /**
     * Listener that tracks missiles fired from the ship and enhances their impacts
     */
    public static class OverchargedMunitionsTracker implements AdvanceableListener {
	private final ShipAPI ship;
	private final List<MissileTracker> missilesToTrack = new ArrayList<>();

	public OverchargedMunitionsTracker(ShipAPI ship) {
	    this.ship = ship;
	}

	@Override
	public void advance(float amount) {
	    if (Global.getCombatEngine() == null) return;
	    if (ship == null || !ship.isAlive()) return;

	    // Find new missiles from our ship
	    for (MissileAPI missile : Global.getCombatEngine().getMissiles()) {
		if (missile.getSource() == ship && !isTrackedMissile(missile)) {
		    // This is one of our missiles that we're not tracking yet
		    missilesToTrack.add(new MissileTracker(missile));
		}
	    }

	    // Update our tracked missiles
	    Iterator<MissileTracker> iter = missilesToTrack.iterator();
	    while (iter.hasNext()) {
		MissileTracker tracker = iter.next();

		// Remove trackers for missiles that no longer exist
		if (!Global.getCombatEngine().isEntityInPlay(tracker.missile) ||
			tracker.missile.isFading() ||
			tracker.missile.didDamage()) {

		    // If the missile hit something
		    if (tracker.missile.didDamage()) {
			// Apply our custom impact effects
			applyOverchargedImpact(tracker.missile);
		    }

		    iter.remove();
		}
	    }
	}

	private boolean isTrackedMissile(MissileAPI missile) {
	    for (MissileTracker tracker : missilesToTrack) {
		if (tracker.missile == missile) {
		    return true;
		}
	    }
	    return false;
	}

	private void applyOverchargedImpact(MissileAPI missile) {
	    CombatEngineAPI engine = Global.getCombatEngine();

	    // Get the target the missile hit
	    CombatEntityAPI target = missile.getDamageTarget();
	    if (target == null) return;

	    // Calculate base damage from missile
	    float baseDamage = missile.getDamage().getDamage();
	    if (baseDamage <= 0) return;

	    // Calculate energy damage to add (30% of the original damage)
	    float energyDamage = baseDamage * ENERGY_DAMAGE_PERCENT;

	    // Apply additional energy damage
	    Vector2f hitLocation = missile.getLocation();

	    engine.applyDamage(
		    target,            // target
		    hitLocation,       // location
		    energyDamage,      // amount
		    DamageType.ENERGY, // damage type
		    0f,                // EMP
		    false,             // bypass shields
		    false,             // don't force hard flux
		    missile            // source
	    );

	    // Visual effect for energy damage
	    engine.addHitParticle(
		    hitLocation,
		    new Vector2f(0, 0),
		    100f,
		    1f,
		    0.2f,
		    new Color(100, 200, 255, 200)
	    );

	    // Chance to create EMP arc
	    if (Math.random() < EMP_ARC_CHANCE && target instanceof ShipAPI) {
		ShipAPI targetShip = (ShipAPI) target;

		// Apply EMP arc
		engine.spawnEmpArcPierceShields(
			missile.getSource(),             // source
			hitLocation,                     // origin
			targetShip,                         // origin anchor
			targetShip,                      // target
			DamageType.ENERGY,               // damage type
			0f,                              // hull damage
			energyDamage * 0.5f,             // emp damage
			100000f,                   // max range
			"tachyon_lance_emp_impact",      // sound ID
			10f,                             // thickness
			EMP_FRINGE_COLOR,                // fringe color
			EMP_CORE_COLOR                   // core color
		);
	    }
	}
    }

    /**
     * Simple class to track missiles
     */
    private static class MissileTracker {
	public final MissileAPI missile;

	public MissileTracker(MissileAPI missile) {
	    this.missile = missile;
	}
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	// Return false if ship is null
	if (ship == null) return false;

	// Check if ship belongs to your faction
	return FACTION_ID.equals(ship.getHullSpec().getManufacturer()) || FACTION_ID_VOID.equals(ship.getHullSpec().getManufacturer()) ;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
	if (ship == null) return "Ship does not exist";

	if (!FACTION_ID.equals(ship.getHullSpec().getManufacturer()) || FACTION_ID_VOID.equals(ship.getHullSpec().getManufacturer()) ) {
	    return "Can only be installed on ships of the Elysium faction";
	}

	return null;
    }
}