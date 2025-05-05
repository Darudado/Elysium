
package elysium.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import static elysium.Util.FACTION_ID;

public class ELYS_EMPBeam extends BaseHullMod {

    // Configuration settings
    private static final float EMP_DAMAGE_PERCENT = 0.15f; // 10% of beam damage as EMP
    private static final float FLUX_COST_INCREASE_PERCENT = 0.20f; // 20% increased flux cost

    // S-mod bonuses
    private static final float SMOD_FLUX_COST_INCREASE_PERCENT = 0.30f; // Only 15% increased flux cost when S-modded

    // Arc chance
    private static final float EMP_ARC_CHANCE = 0.15f; // 15% chance to create an EMP arc

    // Visual effect settings
    private static final Color EMP_CORE_COLOR = new Color(75, 100, 255, 255);
    private static final Color EMP_FRINGE_COLOR = new Color(150, 175, 255, 155);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	boolean sMod = isSMod(stats);

	// Apply the flux cost penalty
	float fluxIncrease = sMod ? SMOD_FLUX_COST_INCREASE_PERCENT : FLUX_COST_INCREASE_PERCENT;
	stats.getBeamWeaponFluxCostMult().modifyPercent(id, fluxIncrease * 100f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	boolean sMod = isSMod(ship.getMutableStats());



	// Add the listener that will apply EMP damage when beams hit
	ship.addListener(new BeamEMPEffect(ship, EMP_DAMAGE_PERCENT));
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(EMP_DAMAGE_PERCENT * 100f) + "%";
	if (index == 1) return Math.round(FLUX_COST_INCREASE_PERCENT * 100f) + "%";
	return null;
    }

    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(SMOD_FLUX_COST_INCREASE_PERCENT * 100f) + "%";
	return null;
    }

    /**
     * Listener that applies EMP damage when beam weapons hit
     */
    public static class BeamEMPEffect implements DamageDealtModifier {
	private final ShipAPI ship;
	private final float empDamagePercent;

	public BeamEMPEffect(ShipAPI ship, float empDamagePercent) {
	    this.ship = ship;
	    this.empDamagePercent = empDamagePercent;
	}

	@Override
	public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
	    if (!(param instanceof BeamAPI)) return null;
	    if (!(target instanceof ShipAPI)) return null;

	    BeamAPI beam = (BeamAPI) param;
	    ShipAPI targetShip = (ShipAPI) target;

	    // Only apply to beams from our ship
	    if (beam.getSource() != ship) return null;

	    // Calculate EMP damage based on the actual damage dealt
	    float damageAmount = damage.getDamage();
	    float empAmount = damageAmount * empDamagePercent;

	    if (empAmount <= 0) return null;

	    CombatEngineAPI engine = Global.getCombatEngine();

	    // Apply actual EMP damage - only when hitting armor or hull
	    if (!shieldHit) {
		engine.applyDamage(
			target,          // Target entity
			point,           // Location of the hit
			0f,              // Hull damage (none, we're just applying EMP)
			DamageType.ENERGY, // Damage type
			empAmount,       // EMP damage
			false,           // Don't bypass shields
			false,           // Don't force hard flux
			ship             // Source entity
		);
	    }

	    // Random chance to create an EMP arc
	    if (Math.random() < EMP_ARC_CHANCE) {
		// Create actual EMP arc (not just visual) that can pierce shields
		engine.spawnEmpArcPierceShields(
			beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
			DamageType.ENERGY, // Damage type
			empAmount * 0.25f,              // Hull damage (none)
			empAmount * 0.5f, // EMP damage (50% of the calculated amount)
			100000f,            // Max range
			"tachyon_lance_emp_impact", // Sound ID
			beam.getWidth() + 5f,
			beam.getFringeColor(),
			beam.getCoreColor()

		);
	    }

	    return null;
	}
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