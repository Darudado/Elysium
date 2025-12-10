package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.awt.Color;

import static elysium.Util.FACTION_ID;
import static elysium.Util.FACTION_ID_VOID;

public class ELYS_TacticalFluxRedirector extends BaseHullMod {

    // Configuration values
    private static final float ENERGY_DAMAGE_BOOST = 0.50f; // 50% energy weapon damage boost
    private static final float FLUX_COST_PERCENT = 0.20f; // 20% flux cost increase
    private static final float ARMOR_DAMAGE_PERCENT = 0.03f; // 3% armor damage per second above threshold
    private static final float FLUX_THRESHOLD = 0.60f; // Start damaging armor at 60% flux
    private static final float SMOD_FLUX_THRESHOLD = 0.70f; // S-mod: Start damaging armor at 70% flux
    private static final float SMOD_ARMOR_DAMAGE_PERCENT = 0.06f; // S-mod: Start damaging armor at 70% flux


    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Apply energy weapon damage bonus (always active)
	stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + ENERGY_DAMAGE_BOOST);
	stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f + FLUX_COST_PERCENT);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
	if (Global.getCombatEngine().isPaused()) return;

	boolean sMod = isSMod(ship.getMutableStats());
	float threshold = sMod ? SMOD_FLUX_THRESHOLD : FLUX_THRESHOLD;

	// Get ship's current flux level
	float currentFluxLevel = ship.getFluxTracker().getFluxLevel();

	// Apply armor damage when above threshold
	if (currentFluxLevel > threshold) {
	    // Calculate armor damage based on time
	    float armorDamage = sMod ? SMOD_ARMOR_DAMAGE_PERCENT * amount: ARMOR_DAMAGE_PERCENT * amount;

	    // Apply damage to armor grid
	    ArmorGridAPI armorGrid = ship.getArmorGrid();
	    if (armorGrid != null) {
		// Calculate armor cell damage
		int gridWidth = armorGrid.getGrid().length;
		int gridHeight = armorGrid.getGrid()[0].length;

		// Get total armor and calculate damage per cell
		float totalMaxArmorInGrid = armorGrid.getMaxArmorInCell() * gridWidth * gridHeight;
		float cellDamagePercent = armorDamage / totalMaxArmorInGrid;

		// Apply damage to each cell
		for (int x = 0; x < gridWidth; x++) {
		    for (int y = 0; y < gridHeight; y++) {
			if (armorGrid.getArmorValue(x, y) > 0) {
			    float currentArmor = armorGrid.getArmorValue(x, y);
			    float damageAmount = armorGrid.getMaxArmorInCell() * cellDamagePercent;
			    armorGrid.setArmorValue(x, y, Math.max(0, currentArmor - damageAmount));
			}
		    }
		}
	    }
	}
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(ENERGY_DAMAGE_BOOST * 100) + "%";
	if (index == 1) return Math.round(FLUX_THRESHOLD * 100) + "%";
	if (index == 2) return Math.round(ARMOR_DAMAGE_PERCENT * 100) + "%";
	if (index == 3) return Math.round(FLUX_COST_PERCENT * 100) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(SMOD_FLUX_THRESHOLD * 100) + "%";
	if (index == 1) return Math.round(SMOD_ARMOR_DAMAGE_PERCENT * 100) + "%";
	return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {

	return ship.getShield() == null || ship.getShield().getType() == ShieldAPI.ShieldType.NONE || FACTION_ID.equals(ship.getHullSpec().getManufacturer()) || FACTION_ID_VOID.equals(ship.getHullSpec().getManufacturer()) ;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
	if (ship.getShield() != null && ship.getShield().getType() != ShieldAPI.ShieldType.NONE) {
	    return "Incompatible with shielded ships";
	}
	if (!FACTION_ID.equals(ship.getHullSpec().getManufacturer()) || FACTION_ID_VOID.equals(ship.getHullSpec().getManufacturer()) ) {
	    return "Can only be installed on ships of the Elysium faction";
	}
	return null;
    }

}