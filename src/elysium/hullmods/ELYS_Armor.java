package elysium.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ELYS_Armor extends BaseHullMod {

    public final float MIN_DURATION = 1.95f;
    public final float MAX_DURATION = 2.05f;
    public final int MAX_DURATION_TEXT = 2	;
    IntervalUtil counter = new IntervalUtil(MIN_DURATION, MAX_DURATION);

    // Configuration
    private static final float MAX_ARMOR_REPAIR_PERCENT = 0.20f; // Maximum of armor that can be repaired per cycle
    private static final float ARMOR_REPAIR_CEILING = 0.70f; // Cannot repair beyond
    private static final float FLUX_PER_ARMOR_RATIO = 2.0f; // Hard flux spent per armor point repaired
    private static final boolean SHOW_REPAIR_EFFECT = true; // Visual indicator of repair
    private static final Color REPAIR_EFFECT_COLOR = new Color(0, 255, 150, 150);
    private static final Color FLUX_EFFECT_COLOR = new Color(50, 252, 255, 150);

    // Cache for valid armor cells to avoid recalculating each time
    private static final Map<String, boolean[][]> shipIdToValidArmorMap = new HashMap<>();

    // Stats
    private static final float DAMAGE_TWEAK_MULTIPLIER = 0.67f;
    private static final float EMP_REDUCTION_MAP = 70f;
    private static final float BEAM_REDUCTION_MAP = 40f;
    private static final float FUEL_REDUCTION = 20f;

    public void applyEffectsBeforeShipCreation(HullSize hullSize,
	    MutableShipStatsAPI stats, String id) {
	stats.getEmpDamageTakenMult().modifyMult(id, 1f - EMP_REDUCTION_MAP / 100f);
	stats.getBeamDamageTakenMult().modifyMult(id, 1f - BEAM_REDUCTION_MAP / 100f);
	stats.getHighExplosiveDamageTakenMult().modifyMult(id, DAMAGE_TWEAK_MULTIPLIER);
	stats.getKineticDamageTakenMult().modifyMult(id, 2f - DAMAGE_TWEAK_MULTIPLIER);
	if(!hullSize.equals(HullSize.FIGHTER)){
	    stats.getFuelUseMod().modifyMult(id, 1f - FUEL_REDUCTION / 100f);
	}
    }

    /**
     * Determines which cells in the armor grid are valid armor cells
     * This accounts for the ship's shape and only includes cells that actually represent armor
     *
     * @param ship The ship to analyze
     * @return A boolean grid where true indicates a valid armor cell
     */
    private boolean[][] getValidArmorCells(ShipAPI ship) {
	String hullId = ship.getHullSpec().getHullId();

	// Check cache first
	if (shipIdToValidArmorMap.containsKey(hullId)) {
	    return shipIdToValidArmorMap.get(hullId);
	}

	ArmorGridAPI armorGrid = ship.getArmorGrid();
	if (armorGrid == null) return null;

	float[][] armorCells = armorGrid.getGrid();
	int gridWidth = armorCells.length;
	int gridHeight = armorCells[0].length;

	// Initialize all cells to invalid
	boolean[][] validCells = new boolean[gridWidth][gridHeight];

	// We'll consider a cell valid if it has a positive armor value in the original grid
	for (int x = 0; x < gridWidth; x++) {
	    for (int y = 0; y < gridHeight; y++) {
		if (armorCells[x][y] > 0) {
		    validCells[x][y] = true;
		}
	    }
	}

	// Store in cache for future use
	shipIdToValidArmorMap.put(hullId, validCells);
	return validCells;
    }

    /*
     * Repairs ship's armor grid on a cell-by-cell basis
     * Each damaged cell below 70% integrity will be repaired up to 70% of its max value
     * Repairs are limited by available flux using the 2:1 flux:armor ratio
     * @param ship The ship to repair
     */
    private void repairShipArmor(ShipAPI ship) {
	CombatEngineAPI engine = Global.getCombatEngine();
	ArmorGridAPI armorGrid = ship.getArmorGrid();
	if (armorGrid == null) return;

	// Get valid armor cells mask
	boolean[][] validCells = getValidArmorCells(ship);
	if (validCells == null) return;

	float armorRepairCeilingFinal = ARMOR_REPAIR_CEILING;
	if (ship.getVariant().hasHullMod("ELYS_EnhancedHealingArmor")){
	    armorRepairCeilingFinal += 0.1f;
	}

	FluxTrackerAPI fluxTracker = ship.getFluxTracker();
	float maxFlux = fluxTracker.getMaxFlux();
	float currentFlux = fluxTracker.getCurrFlux();
	float availableFlux = maxFlux - currentFlux;

	// If no flux available, don't continue
	if (availableFlux <= (maxFlux * 0.15)) {
	    if (ship == engine.getPlayerShip()) {
		engine.addFloatingText(
			ship.getLocation(),
			"NO FLUX AVAILABLE FOR REPAIRS",
			15f,
			Color.RED,
			ship,
			0.5f, 1.5f
		);
	    }
	    return;
	}

	// Get armor grid dimensions
	float[][] armorGrid2D = armorGrid.getGrid();
	int shipWidth = armorGrid2D.length;
	int shipHeight = armorGrid2D[0].length;
	float maxArmorInCell = armorGrid.getMaxArmorInCell();

	// Cell repair ceiling (70% of max)
	float cellRepairCeiling = maxArmorInCell * (armorRepairCeilingFinal);



	// First pass: identify cells that need repair and calculate total potential repair
	float totalRepairNeeded = 0f;
	boolean repairNeeded = false;

	for (int x = 0; x < shipWidth; x++) {
	    for (int y = 0; y < shipHeight; y++) {
		if (validCells[x][y]) {
		    float currentArmor = armorGrid.getArmorValue(x, y);

		    // Only repair cells below the ceiling threshold
		    if (currentArmor < cellRepairCeiling) {
			float repairAmount = Math.min(
				cellRepairCeiling - currentArmor,  // Amount to reach ceiling
				maxArmorInCell * MAX_ARMOR_REPAIR_PERCENT  // Max 10% repair per cycle
			);

			if (repairAmount > 0) {
			    totalRepairNeeded += repairAmount;
			    repairNeeded = true;
			}
		    }
		}
	    }
	}

	// Calculate total flux needed and repair ratio
	float totalFluxNeeded = totalRepairNeeded * FLUX_PER_ARMOR_RATIO;
	float repairRatio = 1.0f;

	// If not enough flux, scale down repairs
	boolean partialRepair = false;
	if (totalFluxNeeded > availableFlux) {
	    repairRatio = availableFlux / totalFluxNeeded;
	    partialRepair = true;
	}

	// Second pass: apply repairs based on available flux
	float totalArmorRepaired = 0f;
	float totalFluxUsed = 0f;

	for (int x = 0; x < shipWidth; x++) {
	    for (int y = 0; y < shipHeight; y++) {
		if (validCells[x][y]) {
		    float currentArmor = armorGrid.getArmorValue(x, y);

		    // Only repair cells below the ceiling threshold
		    if (currentArmor < cellRepairCeiling) {
			float idealRepairAmount = Math.min(
				cellRepairCeiling - currentArmor,  // Amount to reach ceiling
				maxArmorInCell * MAX_ARMOR_REPAIR_PERCENT  // Max 10% repair per cycle
			);

			// Apply repair ratio if flux limited
			float actualRepair = idealRepairAmount * repairRatio;
			float fluxForRepair = actualRepair * FLUX_PER_ARMOR_RATIO;

			// Safety check - ensure we don't exceed available flux
			if (totalFluxUsed + fluxForRepair > availableFlux) {
			    // Adjust repair to use remaining flux
			    float remainingFlux = availableFlux - totalFluxUsed;
			    actualRepair = remainingFlux / FLUX_PER_ARMOR_RATIO;
			    fluxForRepair = remainingFlux;
			}

			if (actualRepair > 0) {
			    // Apply repair to cell
			    float newArmor = currentArmor + actualRepair;
			    armorGrid.setArmorValue(x, y, newArmor);

			    // Track totals
			    totalArmorRepaired += actualRepair;
			    totalFluxUsed += fluxForRepair;

			    // Stop if we've used all available flux
			    if (totalFluxUsed >= availableFlux) {
				break;
			    }
			}
		    }
		}

		// Second brake to exit both loops
		if (totalFluxUsed >= availableFlux) {
		    break;
		}
	    }
	}

	// If repairs were performed, apply flux cost and effects
	if (totalArmorRepaired > 0) {
	    // Add hard flux cost
	    fluxTracker.increaseFlux(totalFluxUsed, true);

	    // Show visual effects
	    if (SHOW_REPAIR_EFFECT) {
		Vector2f shipLoc = ship.getLocation();
		float shipRadius = ship.getCollisionRadius();

		// Create armor repair visual effect
		engine.addHitParticle(
			shipLoc,
			new Vector2f(0, 0),
			shipRadius * 2f,
			1.0f,
			1.0f,
			REPAIR_EFFECT_COLOR
		);

		// Create flux buildup visual effect
		engine.addHitParticle(
			shipLoc,
			new Vector2f(0, 0),
			shipRadius * 1.5f,
			0.8f,
			0.5f,
			FLUX_EFFECT_COLOR
		);

		// Add repair message to combat log with specific amounts
		engine.addFloatingText(
			shipLoc,
			String.format("ARMOR REPAIR: %.0f POINTS (%.0f FLUX)",
				totalArmorRepaired,
				totalFluxUsed),
			15f,
			REPAIR_EFFECT_COLOR,
			ship,
			1f, 1f
		);
	    }
	}
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
	CombatEngineAPI engine = Global.getCombatEngine();
	if (engine.isPaused() || ship.getHullSize().equals(HullSize.FIGHTER)) {
	    return;
	}
	if (ship.isAlive()) {
	    counter.advance(amount);
	    if(counter.intervalElapsed()) {
		repairShipArmor(ship);
	    }
	}
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
	return true;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
	float pad = 3f;
	float opad = 10f;
	Color h = Misc.getHighlightColor();
	Color bad = Misc.getNegativeHighlightColor();
	Color pos = Misc.getPositiveHighlightColor();
	Color t = Misc.getTextColor();
	Color g = Misc.getGrayColor();

	tooltip.addSectionHeading("Damage Modifiers", Alignment.MID, opad);

	tooltip.addPara("Reduces EMP damage by %s", pad, pos, "" + (int)EMP_REDUCTION_MAP + "%");
	tooltip.addPara("Reduces beam damage by %s", pad, pos, "" + (int)BEAM_REDUCTION_MAP + "%");
	tooltip.addPara("Reduces high-explosive damage by %s", pad, pos, "" + (int)(DAMAGE_TWEAK_MULTIPLIER * 100) + "%");
	tooltip.addPara("Increases kinetic damage by %s", pad, bad, "" + (int)((2f - DAMAGE_TWEAK_MULTIPLIER) * 100 - 100) + "%");

	tooltip.addSectionHeading("Regeneration System", Alignment.MID, opad);

	tooltip.addPara("Every %s seconds, the armor will attempt to repair itself, consuming ship flux in the process.",
		pad, h, "" + MAX_DURATION_TEXT);

	tooltip.addPara("- Repairs up to %s of maximum armor per cycle",
		pad, h, "" + (int)(MAX_ARMOR_REPAIR_PERCENT * 100) + "%");

	tooltip.addPara("- Cannot repair beyond %s of total armor",
		pad, h, "" + (int)(ARMOR_REPAIR_CEILING * 100) + "%");

	tooltip.addPara("- Consumes %s units of hard flux per point of armor repaired",
		pad, h, "" + (int)(FLUX_PER_ARMOR_RATIO));

	if (ship != null && ship.getVariant().hasHullMod("ELYS_EnhancedHealingArmor")) {
	    tooltip.addPara("Enhanced Healing Armor installed: Repair ceiling increased to %s",
		    pad, pos, "" + (int)((ARMOR_REPAIR_CEILING + 0.1f) * 100) + "%");
	}

	tooltip.addSectionHeading("Additional Benefits", Alignment.MID, opad);

	tooltip.addPara("Reduces fuel consumption by %s",
		pad, pos, "" + (int)(FUEL_REDUCTION) + "%");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	return !((ship.getVariant().getHullMods().contains(HullMods.HEAVYARMOR))||(ship.getVariant().getHullMods().contains(HullMods.ARMOREDWEAPONS)));
    }

    public String getUnapplicableReason(ShipAPI ship) {
	if (ship.getVariant().getHullMods().contains(HullMods.HEAVYARMOR) || ship.getVariant().getHullMods().contains(HullMods.ARMOREDWEAPONS)) {
	    return "Incompatible with other armor mods";
	}
	return null;
    }
}