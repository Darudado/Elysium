package elysium.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

    public class ELYS_ChargeStorage extends BaseHullMod {

	public static float AMMO_BONUS = 30f;
	public static float DAMAGE_BONUS = 10f;

	// S-mod bonuses if you want to add them later
	public static float SMOD_REGEN_BONUS = 25f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	    // Increase energy/synergy weapon ammo capacity
	    stats.getEnergyAmmoBonus().modifyPercent(id, AMMO_BONUS);

	    // Increase energy/synergy weapon damage
	    stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);

	}


	public String getDescriptionParam(int index, HullSize hullSize) {
	    if (index == 0) return "" + (int) AMMO_BONUS + "%";
	    if (index == 1) return "" + (int) DAMAGE_BONUS + "%";
	    return null;
	}
    }

