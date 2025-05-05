package elysium.hullmods;

import com.fs.starfarer.api.loading.WeaponSpecAPI;

import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.List;

import static elysium.Util.FACTION_ID;

/**
 * A comprehensive hull mod that enhances point defense capabilities by combining
 * features from PDIntegration and IntegratedPointDefenseAI.
 */
public class ELYS_CanopyDefense extends BaseHullMod {

    // Configuration
    private static final int OP_REDUCTION = 2;              // OP cost reduction for PD weapons
    private static final float MISSILE_DAMAGE_BONUS = 50f;  // Bonus damage to missiles

    // S-mod bonuses
    private static final float SMOD_PD_RANGE_BONUS = 200f;   // PD weapon range bonus when S-modded
    private static final float SMOD_DISSIPATION_MALUS = 0.20f;   // ship flux dissipation


    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// From PDIntegration: Reduce OP cost for small PD weapons
	stats.getDynamic().getMod(Stats.SMALL_PD_MOD).modifyFlat(id, -OP_REDUCTION);

	// From IntegratedPointDefenseAI: Enhance PD targeting
	stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
	stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f);
	stats.getDamageToMissiles().modifyPercent(id, MISSILE_DAMAGE_BONUS);

	// S-mod bonus: Increase PD weapon range
	if (isSMod(stats)) {
	    stats.getBeamPDWeaponRangeBonus().modifyFlat(id, SMOD_PD_RANGE_BONUS);
	    stats.getFluxDissipation().modifyPercent(id, 1 - SMOD_DISSIPATION_MALUS);
	}
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

	// From IntegratedPointDefenseAI: Convert small non-missile weapons to PD when S-modded
	if (isSMod(ship)) {
	    List<WeaponAPI> weapons = ship.getAllWeapons();
	    for (WeaponAPI weapon : weapons) {
		boolean sizeMatches = weapon.getSize() == WeaponSize.SMALL;

		// Convert eligible small weapons to PD
		if (sizeMatches &&
			weapon.getType() != WeaponType.MISSILE &&
			!weapon.hasAIHint(AIHints.STRIKE)) {
		    weapon.setPD(true);
		}
	    }
	}
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return "" + OP_REDUCTION;
	if (index == 1) return "" + (int) Math.round(MISSILE_DAMAGE_BONUS) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return "" + (int) Math.round(SMOD_PD_RANGE_BONUS);
	if (index == 1) return "" + (int) Math.round(SMOD_DISSIPATION_MALUS * 100) + "%";
	return null;
    }

    @Override
    public boolean affectsOPCosts() {
	return true;
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
	if (ship == null || ship.getVariant() == null) return true; // autofit
	if (!ship.getVariant().hasHullMod("elys_enhanced_pd_system")) return true; // can always add

	for (String slotId : ship.getVariant().getFittedWeaponSlots()) {
	    WeaponSpecAPI spec = ship.getVariant().getWeaponSpec(slotId);
	    if (spec.getAIHints().contains(AIHints.PD)) return false;
	}
	return true;
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CoreUITradeMode mode) {
	return "Cannot remove while ship has point-defense weapons installed";
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