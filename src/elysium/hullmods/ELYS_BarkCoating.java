package elysium.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import static elysium.Util.FACTION_ID;

public class ELYS_BarkCoating extends BaseHullMod {

    // Enhancement percentages
    private static final float HULL_BOOST_PERCENT = 40f;
    private static final float MISSILE_HP_BOOST_PERCENT = 20f;
    private static final float SMOD_MISSILE_HP_BOOST_PERCENT = 40f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Increase ship's hull hit points by 40%
	stats.getHullBonus().modifyPercent(id, HULL_BOOST_PERCENT);

	// Check if this hull mod is built-in (S-modded)
	boolean sMod = isSMod(stats);

	// Apply standard or S-mod missile health bonus
	float missileBonus = sMod ? SMOD_MISSILE_HP_BOOST_PERCENT : MISSILE_HP_BOOST_PERCENT;
	stats.getMissileHealthBonus().modifyPercent(id, missileBonus);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(HULL_BOOST_PERCENT) + "%";
	if (index == 1) return Math.round(MISSILE_HP_BOOST_PERCENT) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(SMOD_MISSILE_HP_BOOST_PERCENT) + "%";
	return null;
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	// Return false if ship is null
	if (ship == null) return false;

	// Check if ship belongs to your faction
	return FACTION_ID.equals(ship.getHullSpec().getManufacturer());
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
	if (ship == null) return "Ship does not exist";

	if (!FACTION_ID.equals(ship.getHullSpec().getManufacturer())) {
	    return "Can only be installed on ships of the Elysium faction";
	}

	return null;
    }
}