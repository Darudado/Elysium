package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static elysium.Util.FACTION_ID;

/**
 * A logistics hull mod for the elven-themed faction that increases fuel efficiency and
 * generates small amounts of fuel over time.
 */
public class ELYS_ArborealResinRefinement extends BaseHullMod
{

    // Configuration
    private static final float FUEL_EFFICIENCY_BONUS = 0.20f; // 20% fuel efficiency
    private static final float FUEL_PER_DAY_SMALL = 1f;    // Fuel generated per day (frigate)
    private static final float FUEL_PER_DAY_MEDIUM = 2f;   // Fuel generated per day (destroyer)
    private static final float FUEL_PER_DAY_LARGE = 3f;    // Fuel generated per day (cruiser)
    private static final float FUEL_PER_DAY_CAPITAL = 5f;  // Fuel generated per day (capital)

    // Penalty: maintenance supply cost increase
    private static final float MAINTENANCE_SUPPLY_PENALTY = 0.15f; // 15% increased supply cost

    // Fuel gen is only active when fleet is not in combat and stationary or at very low burn
    private static final float MAX_BURN_FOR_FUEL_GEN = 5f;

    // Store accumulated fuel for each ship
    private static Map<String, Float> accumulatedFuelByShipId = new HashMap<>();

    // Colors for UI
    private static final Color HIGHLIGHT_COLOR = new Color(150, 220, 150, 255);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
	// Increase fuel efficiency
	stats.getFuelUseMod().modifyMult(id, 1f - FUEL_EFFICIENCY_BONUS);

	// Penalty: Increase maintenance supply cost
	stats.getSuppliesPerMonth().modifyPercent(id, MAINTENANCE_SUPPLY_PENALTY * 100f);

	// Note: We can't add fuel production here as there's no Stats.FUEL_PRODUCTION_MOD
	// Instead, we'll handle fuel generation in advanceInCampaign
    }

    private float getFuelPerDayForSize(HullSize hullSize) {
	switch (hullSize) {
	    case CAPITAL_SHIP: return FUEL_PER_DAY_CAPITAL;
	    case CRUISER: return FUEL_PER_DAY_LARGE;
	    case DESTROYER: return FUEL_PER_DAY_MEDIUM;
	    case FRIGATE: return FUEL_PER_DAY_SMALL;
	    default: return 0f;
	}
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
	// Skip if ship is mothballed
	if (member.isMothballed()) return;

	// Check if ship is in player fleet
	CampaignFleetAPI fleet = member.getFleetData().getFleet();
	if (fleet == null || fleet != Global.getSector().getPlayerFleet()) return;

	// Only generate fuel when fleet is stationary or at very low burn
	float currentBurn = fleet.getCurrBurnLevel();
	if (currentBurn > MAX_BURN_FOR_FUEL_GEN) return;

	// Check if fleet is in combat
	boolean inCombat = fleet.getBattle() != null && !fleet.getBattle().isDone();
	if (inCombat) return;

	// Calculate fuel to add
	HullSize hullSize = member.getHullSpec().getHullSize();
	float fuelPerDay = getFuelPerDayForSize(hullSize);
	float fuelToAdd = fuelPerDay * amount * Global.getSector().getClock().getSecondsPerDay();

	// Accumulate fuel for this ship
	String shipId = member.getId();
	float accumulated = 0f;
	if (accumulatedFuelByShipId.containsKey(shipId)) {
	    accumulated = accumulatedFuelByShipId.get(shipId);
	}
	accumulated += fuelToAdd;

	// We need to accumulate at least 1 unit of fuel before adding to cargo
	if (accumulated >= 1f) {
	    // Get the integer part only (whole units of fuel)
	    int toAdd = (int) accumulated;
	    accumulated -= toAdd;

	    // Add fuel to fleet
	    fleet.getCargo().addFuel(toAdd);

	}

	// Store updated accumulated fuel
	accumulatedFuelByShipId.put(shipId, accumulated);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
	float pad = 10f;
	float fuelPerDay = getFuelPerDayForSize(hullSize);
	Color negativeColor = Misc.getNegativeHighlightColor();

	tooltip.addPara("Generates %s fuel per day when the fleet is stationary or at minimal burn level, and not in combat or docked.",
		pad, HIGHLIGHT_COLOR, "" + fuelPerDay);

	tooltip.addPara("Increases fuel efficiency by %s, reducing the amount of fuel consumed during travel.",
		pad, HIGHLIGHT_COLOR, Math.round(FUEL_EFFICIENCY_BONUS * 100f) + "%");

	tooltip.addPara("The sap collection nodes require regular maintenance, increasing the ship's supply consumption by %s.",
		pad, negativeColor, Math.round(MAINTENANCE_SUPPLY_PENALTY * 100f) + "%");
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return Math.round(FUEL_EFFICIENCY_BONUS * 100f) + "%";
	if (index == 1) {
	    return "" + getFuelPerDayForSize(hullSize);
	}
	if (index == 2) return Math.round(MAINTENANCE_SUPPLY_PENALTY * 100f) + "%";
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