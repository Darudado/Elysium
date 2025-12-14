package elysium.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import static elysium.Util.FACTION_ID;
import static elysium.Util.FACTION_ID_VOID;

public class ELYS_EliteCrew extends BaseHullMod {

    // Modification values
    private static final float CREW_DEATH_REDUCTION = 0.6f; // 60% reduction in crew deaths
    private static final float PEAK_CR_BONUS = 0.3f; // 15% increase in energy weapon damage
    private static final float REPAIR_RATE = 0.25f; // 25% increased repair rate

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Apply crew loss reduction
	stats.getCrewLossMult().modifyMult(id, 1f - CREW_DEATH_REDUCTION);

	stats.getPeakCRDuration().modifyPercent(id, 100* PEAK_CR_BONUS);

	if (isSMod(stats)) {
	    stats.getCombatEngineRepairTimeMult().modifyMult(id, 1- REPAIR_RATE);
	}
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(CREW_DEATH_REDUCTION * 100) + "%";
	if (index == 1) return Math.round(PEAK_CR_BONUS * 100) + "%";
	return null;
    }
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return "" + (int) Math.round(REPAIR_RATE*100) + "%";
	return null;
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