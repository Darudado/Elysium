package elysium.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public class ELYS_BeamExpertize extends BaseHullMod {

    // Modification values
    private static final float BEAM_RANGE_BONUS = 200f; // Flat 200 range increase
    private static final float BEAM_DAMAGE_INCREASE = 0.10f; // 10% increase in beam damage
    private static final float SUPPLY_COST_MULTI = 0.5f; // 10% increase in beam damage

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Apply beam weapon range increase (flat bonus)
	stats.getBeamWeaponRangeBonus().modifyFlat(id, BEAM_RANGE_BONUS);

	// Apply beam weapon damage increase
	stats.getBeamWeaponDamageMult().modifyPercent(id, BEAM_DAMAGE_INCREASE * 100);
	if(isSMod(stats)) {
	    stats.getSuppliesPerMonth().modifyMult(id,1 + SUPPLY_COST_MULTI);
	}
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	// Check if this hull mod is an S-mod on this ship
	boolean sMod = isSMod(ship.getMutableStats());

	// Only add the beam hardflux listener if this is an S-mod
	if (sMod) {
	    ship.addListener(new BeamHardFluxDamageDealtMod(ship));
	}
    }

    public static class BeamHardFluxDamageDealtMod implements DamageDealtModifier {
	protected ShipAPI ship;

	public BeamHardFluxDamageDealtMod(ShipAPI ship) {
	    this.ship = ship;
	}

	public String modifyDamageDealt(Object param,
		CombatEntityAPI target, DamageAPI damage,
		Vector2f point, boolean shieldHit) {

	    // Check if the damage source is a beam weapon
	    if (param instanceof BeamAPI) {
		damage.setForceHardFlux(true);
	    }
	    return null;
	}
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return "hard flux";
	if (index == 1) return  Math.round( SUPPLY_COST_MULTI * 100) + "%";
	return null;
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(BEAM_RANGE_BONUS) + "";
	if (index == 1) return Math.round(BEAM_DAMAGE_INCREASE * 100) + "%";
	return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	return true; // This hull mod can be applied to any ship
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
	return null;
    }
}