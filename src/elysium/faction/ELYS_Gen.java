package elysium.faction;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import elysium.faction.systems.ELYS_DarkweaveSystem;
import elysium.faction.systems.ELYS_ElysiumSystem;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class ELYS_Gen implements SectorGeneratorPlugin {
    public static Logger log = Global.getLogger(ELYS_Gen.class);

    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
	    int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries, float tarrif,
	    boolean freePort, boolean withJunkAndChatter) {
	EconomyAPI globalEconomy = Global.getSector().getEconomy();
	String planetID = primaryEntity.getId();
	String marketID = planetID + "_market";

	MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
	newMarket.setFactionId(factionID);
	newMarket.setPrimaryEntity(primaryEntity);
	newMarket.getTariff().modifyFlat("generator", tarrif);

	//Adds submarkets
	if (null != submarkets) {
	    for (String market : submarkets) {
		newMarket.addSubmarket(market);
	    }
	}

	//Adds market conditions
	for (String condition : marketConditions) {
	    newMarket.addCondition(condition);
	}

	//Add market industries
	for (String industry : industries) {
	    newMarket.addIndustry(industry);
	}

	//Sets us to a free port, if we should
	newMarket.setFreePort(freePort);

	//Adds our connected entities, if any
	if (null != connectedEntities) {
	    for (SectorEntityToken entity : connectedEntities) {
		newMarket.getConnectedEntities().add(entity);
	    }
	}

	globalEconomy.addMarket(newMarket, withJunkAndChatter);
	primaryEntity.setMarket(newMarket);
	primaryEntity.setFaction(factionID);

	if (null != connectedEntities) {
	    for (SectorEntityToken entity : connectedEntities) {
		entity.setMarket(newMarket);
		entity.setFaction(factionID);
	    }
	}

	//Finally, return the newly-generated market
	return newMarket;
    }

    @Override
    public void generate(SectorAPI sector) {
	FactionAPI elysium = sector.getFaction("elysium");

	// Generate the two systems
	new ELYS_ElysiumSystem().generate(sector);
	new ELYS_DarkweaveSystem().generate(sector);

	// Get references to the systems
	StarSystemAPI elysiumSystem = Global.getSector().getStarSystem("Elysium");
	StarSystemAPI darkweaveSystem = Global.getSector().getStarSystem("Darkweave");

	// Add faction to bounty events
	SharedData.getData().getPersonBountyEventData().addParticipatingFaction("elysium");

	// Set relationships with vanilla factions
	elysium.setRelationship(Factions.LUDDIC_CHURCH, -0.6f); // Religious conflict with space elves
	elysium.setRelationship(Factions.LUDDIC_PATH, -0.8f); // Extremists despise the alien-like elves
	elysium.setRelationship(Factions.TRITACHYON, 0.2f); // Both high-tech, some respect
	elysium.setRelationship(Factions.PERSEAN, 0.1f); // Neutral to positive
	elysium.setRelationship(Factions.PIRATES, -0.7f); // Elysium dislikes lawlessness
	elysium.setRelationship(Factions.INDEPENDENT, 0f); // Neutral
	elysium.setRelationship(Factions.DIKTAT, -0.3f); // Some tension with authoritarian regime
	elysium.setRelationship(Factions.LIONS_GUARD, -0.3f); // Same as Diktat
	elysium.setRelationship(Factions.HEGEMONY, -0.5f); // Distrust of dominant human faction
	elysium.setRelationship(Factions.REMNANTS, -0.2f); // Cautious about the AI ships

    }
}