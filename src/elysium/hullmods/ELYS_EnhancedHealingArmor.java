package elysium.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


public class ELYS_EnhancedHealingArmor extends BaseHullMod {

    // Configuration
    private static final float ARMOR_CEILING_INCREASE = 0.20f; //check armor implementation, not here
    private static final float KINETIC_DAMAGE_INCREASE = 0.30f; // 30% increase to kinetic damage taken
    private static final float SMOD_KINETIC_DAMAGE_INCREASE = 0.50f; // 50% increase to kinetic damage when S-modded

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Increase kinetic damage taken
	boolean sMod = isSMod(stats);
	float kineticIncrease = sMod ? SMOD_KINETIC_DAMAGE_INCREASE : KINETIC_DAMAGE_INCREASE;
	stats.getKineticDamageTakenMult().modifyMult(id, 1f + kineticIncrease);
	if(sMod){
	    stats.getKineticDamageTakenMult().modifyMult(id, 1f + kineticIncrease);
	}
    }


    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(ARMOR_CEILING_INCREASE * 100) + "%";
	if (index == 1) return Math.round(KINETIC_DAMAGE_INCREASE * 100) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(SMOD_KINETIC_DAMAGE_INCREASE * 100) + "%";
	if (index == 0) return Math.round(ARMOR_CEILING_INCREASE) + "%";
	return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	// Check if the ship has ELYS_Armor installed
	boolean hasArmorMod = false;
	for (String hullModId : ship.getVariant().getHullMods()) {
	    if (hullModId.equals("ELYS_Armor")) {
		hasArmorMod = true;
		break;
	    }
	}

	return hasArmorMod;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
	boolean hasArmorMod = false;
	for (String hullModId : ship.getVariant().getHullMods()) {
	    if (hullModId.equals("ELYS_Armor")) {
		hasArmorMod = true;
		break;
	    }
	}

	if (!hasArmorMod) {
	    return "Requires Elysium Armor System to be installed";
	}

	return null;
    }
}

