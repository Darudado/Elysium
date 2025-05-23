package elysium.hullmods;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ELYS_EnergyRangefinder extends BaseHullMod {

    public static float BONUS_MAX_1 = 800;
    public static float BONUS_MAX_2 = 800;
    public static float BONUS_MAX_3 = 900;
    public static float BONUS_SMALL_1 = 100;
    public static float BONUS_SMALL_2 = 100;
    public static float BONUS_SMALL_3 = 200;
    public static float BONUS_MEDIUM_3 = 100;

    public static float SYNERGY_MULT = 2f;
    public static float SYNERGY_BONUS_MIN = 100f;


    public static WeaponSize getLargestEnergySlot(ShipAPI ship) {
	if (ship == null) return null;
	WeaponSize largest = null;
	for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
	    if (slot.isDecorative() ) continue;
	    if (slot.getWeaponType() == WeaponType.ENERGY) {
		if (largest == null || largest.ordinal() < slot.getSlotSize().ordinal()) {
		    largest = slot.getSlotSize();
		}
	    }
	}
	return largest;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
	WeaponSize largest = getLargestEnergySlot(ship);
	if (largest == null) return;
	float small = 0f;
	float medium = 0f;
	float max = 0f;
	switch (largest) {
	    case LARGE:
		small = BONUS_SMALL_3;
		medium = BONUS_MEDIUM_3;
		max = BONUS_MAX_3;
		break;
	    case MEDIUM:
		small = BONUS_SMALL_2;
		max = BONUS_MAX_2;
		break;
	    case SMALL:
		small = BONUS_SMALL_1;
		max = BONUS_MAX_1;
		break;
	}

	ship.addListener(new RangefinderRangeModifier(small, medium, max));
    }

    public static class RangefinderRangeModifier implements WeaponBaseRangeModifier {
	public float small, medium, max;
	public RangefinderRangeModifier(float small, float medium, float max) {
	    this.small = small;
	    this.medium = medium;
	    this.max = max;
	}

	public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
	    return 0;
	}
	public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
	    return 1f;
	}
	public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
	    if (weapon.getSpec() == null) {
		return 0f;
	    }
	    if (weapon.getSpec().getMountType() != WeaponType.ENERGY &&
		    weapon.getSpec().getMountType() != WeaponType.SYNERGY) {
		return 0f;
	    }
	    if (weapon.hasAIHint(AIHints.PD)) {
		return 0f;
	    }

	    float bonus = 0;
	    if (weapon.getSize() == WeaponSize.SMALL) {
		bonus = small;
	    } else if (weapon.getSize() == WeaponSize.MEDIUM) {
		bonus = medium;
	    }
	    if (weapon.getSpec().getMountType() == WeaponType.SYNERGY) {
		bonus *= SYNERGY_MULT;
		if (bonus < SYNERGY_BONUS_MIN) {
		    bonus = SYNERGY_BONUS_MIN;
		}
	    }
	    if (bonus == 0f) return 0f;

	    float base = weapon.getSpec().getMaxRange();
	    if (base + bonus > max) {
		bonus = max - base;
	    }
	    if (bonus < 0) bonus = 0;
	    return bonus;
	}
    }


    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
	return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
	float pad = 3f;
	float opad = 10f;
	Color h = Misc.getHighlightColor();
	Color bad = Misc.getNegativeHighlightColor();
	Color t = Misc.getTextColor();
	Color g = Misc.getGrayColor();

	WeaponSize largest = getLargestEnergySlot(ship);

	tooltip.addPara("Utilizes targeting data from the ship's largest energy slot "
			+ "to benefit certain weapons, extending the base range of "
			+ "typical energy weapons to match similar but larger weapons. "
			+ "Greatly benefits synergy weapons. Point-defense weapons are unaffected.",
		opad, h, "ship's largest energy slot", "base range", "Greatly benefits synergy weapons");

	tooltip.addPara("The range bonus is based on the size of the largest energy slot, "
		+ "and the increased base range is capped, but still subject to other modifiers.", opad);

	tooltip.addSectionHeading("Energy weapon range", Alignment.MID, opad);


	tooltip.addPara("Affects small and medium energy weapons.", opad);

	float col1W = 120;
	float colW = (int) ((width - col1W - 12f) / 3f);
	float lastW = colW;

	tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
		20f, true, true,
		new Object [] {"Largest e. slot", col1W, "Small wpn", colW, "Medium wpn", colW, "Range cap", lastW});

	Color reallyG = g;
	if (Global.CODEX_TOOLTIP_MODE) {
	    g = h;
	}

	Color c = null;
	if (largest == WeaponSize.SMALL) c = h;
	else if (largest == WeaponSize.MEDIUM) c = h;
	else c = g;


	tooltip.addRow(Alignment.MID, c, "Small / Medium",
		Alignment.MID, c, "+" + (int) BONUS_SMALL_1,
		Alignment.MID, reallyG, "---",
		Alignment.MID, c, "" + (int)BONUS_MAX_1);

	if (largest == WeaponSize.LARGE) c = h;
	else c = g;
	tooltip.addRow(Alignment.MID, c, "Large",
		Alignment.MID, c, "+" + (int) BONUS_SMALL_3,
		Alignment.MID, c, "+" + (int) BONUS_MEDIUM_3,
		Alignment.MID, c, "" + (int)BONUS_MAX_3);

	tooltip.addTable("", 0, opad);


	tooltip.addSectionHeading("Synergy weapon range", Alignment.MID, opad + 7f);

	tooltip.addPara("Affects synergy weapons (those that can fit into both energy and universal slots)"
		+ " of all sizes.", opad);

	col1W = 120;
	colW = (int) ((width - col1W - lastW - 15f) / 3f);

	tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
		20f, true, true,
		new Object [] {"Largest e. slot", col1W, "Small", colW, "Medium", colW, "Large", colW, "Range cap", lastW});


	c = null;
	if (largest == WeaponSize.SMALL) c = h;
	else if (largest == WeaponSize.MEDIUM) c = h;
	else c = g;
	tooltip.addRow(Alignment.MID, c, "Small / Medium",
		Alignment.MID, c, "+" + (int) (BONUS_SMALL_1 * SYNERGY_MULT),
		Alignment.MID, c, "+" + (int) SYNERGY_BONUS_MIN,
		Alignment.MID, c, "+" + (int) SYNERGY_BONUS_MIN,
		Alignment.MID, c, "" + (int)BONUS_MAX_1);

	if (largest == WeaponSize.LARGE) c = h;
	else c = g;
	tooltip.addRow(Alignment.MID, c, "Large",
		Alignment.MID, c, "+" + (int) (BONUS_SMALL_3 * SYNERGY_MULT),
		Alignment.MID, c, "+" + (int) (BONUS_MEDIUM_3 * SYNERGY_MULT),
		Alignment.MID, c, "+" + (int) SYNERGY_BONUS_MIN,
		Alignment.MID, c, "" + (int)BONUS_MAX_3);

	tooltip.addTable("", 0, opad);


	tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, opad + 7f);
	tooltip.addPara("Since the base range is increased, this modifier"
		+ " - unlike most other flat modifiers - "
		+ "is increased by percentage modifiers from other hullmods and skills.", opad);
    }

    public float getTooltipWidth() {
	return 412f;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
	WeaponSize largest = getLargestEnergySlot(ship);
	if (ship != null && largest == null) {
	    return false;
	}
	return getUnapplicableReason(ship) == null;
    }

    public String getUnapplicableReason(ShipAPI ship) {
	WeaponSize largest = getLargestEnergySlot(ship);
	if (ship != null && largest == null) {
	    return "Ship has no energy weapon slots";
	}
	if (ship != null &&
		ship.getHullSize() != HullSize.CAPITAL_SHIP &&
		ship.getHullSize() != HullSize.DESTROYER &&
		ship.getHullSize() != HullSize.CRUISER) {
	    return "Can only be installed on destroyer-class hulls and larger";
	}
	return null;
    }
}
