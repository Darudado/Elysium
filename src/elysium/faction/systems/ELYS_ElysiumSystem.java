package elysium.faction.systems;

import elysium.faction.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class ELYS_ElysiumSystem {

    // Constants for orbital distances
    final float elysiumPrimeDist = 3000f;
    final float emberForgeDist = 5000f;
    final float tideSongDist = 7500f;
    final float sylvanShroudDist = 9200f;
    final float thornbeltDist = 4200f;
    final float frostrimDist = 11000f;

    // Jump points and navigation
    final float jumpFringeDist = 12000f;
    final float jumpCenterDist = 4000f;
    final float comDist = 10000f;
    final float navDist = 8000f;
    final float sensorDist = 6500f;

    public void generate(SectorAPI sector) {
	// Create the star system
	StarSystemAPI system = sector.createStarSystem("Elysium");
	system.getLocation().set(-35000, -18000); // Southwest quadrant
	system.setBackgroundTextureFilename("graphics/backgrounds/ELYS_BG2.jpeg");


	// Initialize the star
	PlanetAPI elysiumStar = system.initStar("elysium_star", // unique id
		"star_white", // star type
		650f, // radius
		500, // corona radius
		10f, // solar wind burn level
		0.7f, // flare probability
		3f); // CR loss multiplier

	// Add Elysium Prime (capital, terran world)
	PlanetAPI elysiumPrime = system.addPlanet("elysium_prime",
		elysiumStar,
		"Elysium Prime",
		"terran",
		45f, // starting angle
		180f, // planet radius
		elysiumPrimeDist, // orbit radius
		320f); // orbit days

	// Add market to Elysium Prime
	MarketAPI elysiumPrime_market = ELYS_Gen.addMarketplace(
		"elysium",
		elysiumPrime,
		null,
		"Elysium Prime",
		7,
		new ArrayList<>(
			Arrays.asList(
				Conditions.POPULATION_7,
				Conditions.HABITABLE,
				Conditions.FARMLAND_RICH,
				Conditions.MILD_CLIMATE,
				Conditions.ORGANICS_ABUNDANT,
				Conditions.ORE_MODERATE,
				Conditions.RARE_ORE_SPARSE
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Submarkets.GENERIC_MILITARY,
				Submarkets.SUBMARKET_OPEN,
				Submarkets.SUBMARKET_STORAGE,
				Submarkets.SUBMARKET_BLACK
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Industries.POPULATION,
				Industries.FARMING,
				Industries.MEGAPORT,
				Industries.LIGHTINDUSTRY,
				Industries.HIGHCOMMAND,
				Industries.STARFORTRESS_HIGH,
				Industries.WAYSTATION,
				Industries.MINING,
				Industries.HEAVYBATTERIES
			)
		),
		0.3f,
		false,
		true);

	elysiumPrime.setCustomDescriptionId("planet_elysium_prime");



	// Add AI cores to Elysium Prime
	if (elysiumPrime_market.hasIndustry(Industries.HIGHCOMMAND)) {
	    elysiumPrime_market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (elysiumPrime_market.hasIndustry(Industries.STARFORTRESS_HIGH)) {
	    elysiumPrime_market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (elysiumPrime_market.hasIndustry(Industries.ORBITALWORKS)) {
	    elysiumPrime_market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (elysiumPrime_market.hasIndustry(Industries.MEGAPORT)) {
	    elysiumPrime_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (elysiumPrime_market.hasIndustry(Industries.LIGHTINDUSTRY)) {
	    elysiumPrime_market.getIndustry(Industries.LIGHTINDUSTRY).setAICoreId(Commodities.GAMMA_CORE);
	}
	if (elysiumPrime_market.hasIndustry(Industries.FARMING)) {
	    elysiumPrime_market.getIndustry(Industries.FARMING).setAICoreId(Commodities.GAMMA_CORE);
	}

	// Add a faction leader
	PersonAPI elysiumLeader = Global.getFactory().createPerson();
	elysiumLeader.setId("elysium_leader");
	elysiumLeader.setFaction("elysium");
	elysiumLeader.setGender(FullName.Gender.FEMALE);
	elysiumLeader.setRankId(Ranks.FACTION_LEADER);
	elysiumLeader.setPostId(Ranks.POST_FACTION_LEADER);
	elysiumLeader.setImportance(PersonImportance.VERY_HIGH);
	elysiumLeader.getName().setFirst("Sylvaris");
	elysiumLeader.getName().setLast("Everlight");
	elysiumLeader.setPortraitSprite("graphics/portraits/female/Elys_1.png");

	Global.getSector().getImportantPeople().addPerson(elysiumLeader);
	elysiumPrime_market.getCommDirectory().addPerson(elysiumLeader);
	elysiumPrime_market.addPerson(elysiumLeader);

	// EmberForge (volcanic world)
	PlanetAPI emberForge = system.addPlanet("ember_forge",
		elysiumStar,
		"Emberforge",
		"lava",
		135f,
		120f,
		emberForgeDist,
		220f);

	MarketAPI emberForge_market = ELYS_Gen.addMarketplace(
		"elysium",
		emberForge,
		null,
		"Emberforge",
		5,
		new ArrayList<>(
			Arrays.asList(
				Conditions.POPULATION_5,
				Conditions.HOT,
				Conditions.TECTONIC_ACTIVITY,
				Conditions.ORE_RICH,
				Conditions.RARE_ORE_ABUNDANT,
				Conditions.VOLATILES_DIFFUSE
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Submarkets.GENERIC_MILITARY,
				Submarkets.SUBMARKET_OPEN,
				Submarkets.SUBMARKET_STORAGE,
				Submarkets.SUBMARKET_BLACK
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Industries.POPULATION,
				Industries.MINING,
				Industries.REFINING,
				Industries.MILITARYBASE,
				Industries.SPACEPORT,
				Industries.ORBITALSTATION,
				Industries.WAYSTATION,
				Industries.HEAVYBATTERIES
			)
		),
		0.3f,
		false,
		true);

	emberForge.setCustomDescriptionId("planet_emberforge");

	// Add ORBITALWORKS with PRISTINE_NANOFORGE
	emberForge_market.addIndustry(Industries.ORBITALWORKS, Collections.singletonList(Items.PRISTINE_NANOFORGE));

	// Add AI cores to Emberforge
	if (emberForge_market.hasIndustry(Industries.ORBITALWORKS)) {
	    emberForge_market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (emberForge_market.hasIndustry(Industries.MILITARYBASE)) {
	    emberForge_market.getIndustry(Industries.MILITARYBASE).setAICoreId(Commodities.BETA_CORE);
	}
	if (emberForge_market.hasIndustry(Industries.REFINING)) {
	    emberForge_market.getIndustry(Industries.REFINING).setAICoreId(Commodities.BETA_CORE);
	}
	if (emberForge_market.hasIndustry(Industries.MINING)) {
	    emberForge_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
	}

	// Tidesong (water world)
	PlanetAPI tideSong = system.addPlanet("tide_song",
		elysiumStar,
		"Tidesong",
		"water",
		270f,
		160f,
		tideSongDist,
		390f);

	MarketAPI tideSong_market = ELYS_Gen.addMarketplace(
		"elysium",
		tideSong,
		null,
		"Tidesong",
		5,
		new ArrayList<>(
			Arrays.asList(
				Conditions.POPULATION_5,
				Conditions.WATER_SURFACE,
				Conditions.HABITABLE,
				Conditions.ORGANICS_PLENTIFUL,
				Conditions.VOLATILES_ABUNDANT,
				Conditions.MILD_CLIMATE
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Submarkets.SUBMARKET_OPEN,
				Submarkets.SUBMARKET_STORAGE,
				Submarkets.SUBMARKET_BLACK
			)
		),
		new ArrayList<>(
			Arrays.asList(
				Industries.POPULATION,
				Industries.MEGAPORT,
				Industries.MINING,
				Industries.FARMING,
				Industries.FUELPROD,
				Industries.PATROLHQ,
				Industries.WAYSTATION,
				Industries.GROUNDDEFENSES
			)
		),
		0.3f,
		false,
		true);

	tideSong.setCustomDescriptionId("planet_tidesong");

	// Add AI cores to Tidesong
	if (tideSong_market.hasIndustry(Industries.MEGAPORT)) {
	    tideSong_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (tideSong_market.hasIndustry(Industries.FARMING)) {
	    tideSong_market.getIndustry(Industries.FARMING).setAICoreId(Commodities.GAMMA_CORE);
	}
	if (tideSong_market.hasIndustry(Industries.FUELPROD)) {
	    tideSong_market.getIndustry(Industries.FUELPROD).setAICoreId(Commodities.GAMMA_CORE);
	}
	if (tideSong_market.hasIndustry(Industries.MINING)) {
	    tideSong_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
	}

	// Add non-colonized celestial bodies

	// SylvanShroud (gas giant)
	PlanetAPI sylvanShroud = system.addPlanet("sylvan_shroud",
		elysiumStar,
		"Sylvanshroud",
		"gas_giant",
		200f,
		320f,
		sylvanShroudDist,
		550f);

	Misc.initConditionMarket(sylvanShroud);
	MarketAPI sylvanShroud_market = sylvanShroud.getMarket();
	sylvanShroud_market.addCondition(Conditions.EXTREME_WEATHER);
	sylvanShroud_market.addCondition(Conditions.HIGH_GRAVITY);
	sylvanShroud_market.addCondition(Conditions.VOLATILES_PLENTIFUL);
	sylvanShroud.setCustomDescriptionId("planet_sylvanshroud");

	// Mistwalker (moon of Sylvanshroud)
	PlanetAPI mistWalker = system.addPlanet("mist_walker",
		sylvanShroud,
		"Mistwalker",
		"barren-desert",
		30f,
		40f,
		500f,
		60f);

	Misc.initConditionMarket(mistWalker);
	MarketAPI mistWalker_market = mistWalker.getMarket();
	mistWalker_market.addCondition(Conditions.NO_ATMOSPHERE);
	mistWalker_market.addCondition(Conditions.LOW_GRAVITY);
	mistWalker_market.addCondition(Conditions.RARE_ORE_SPARSE);
	mistWalker.setCustomDescriptionId("planet_mistwalker");

	// The Twins (Dusk and Dawn)
	PlanetAPI dusk = system.addPlanet("dusk",
		elysiumStar,
		"Dusk",
		"barren",
		160f,
		60f,
		6200f,
		300f);

	PlanetAPI dawn = system.addPlanet("dawn",
		elysiumStar,
		"Dawn",
		"barren-desert",
		170f,
		55f,
		6400f,
		310f);

	// Add Frostrim (ice world)
	PlanetAPI frostRim = system.addPlanet("frost_rim",
		elysiumStar,
		"Frostrim",
		"frozen",
		315f,
		85f,
		frostrimDist,
		650f);

	Misc.initConditionMarket(frostRim);
	MarketAPI frostRim_market = frostRim.getMarket();
	frostRim_market.addCondition(Conditions.VERY_COLD);
	frostRim_market.addCondition(Conditions.THIN_ATMOSPHERE);
	frostRim_market.addCondition(Conditions.VOLATILES_TRACE);
	frostRim.setCustomDescriptionId("planet_frostrim");

	// Asteroid belt (Thornbelt)
	system.addAsteroidBelt(elysiumStar, 100, thornbeltDist, 500, 250, 400, Terrain.ASTEROID_BELT, "Thornbelt");

	// Add debris fields for visual interest
	Random rand = new Random();

	DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
		225f, // field radius
		0.8f, // density
		10000000f, // duration
		0f); // days the field will keep generating glowing pieces
	params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
	params1.baseSalvageXP = 500;
	SectorEntityToken debrisField1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random);
	debrisField1.setSensorProfile(800f);
	debrisField1.setDiscoverable(true);
	debrisField1.setCircularOrbit(elysiumStar, rand.nextFloat() * 360f, 3700 + rand.nextInt(1000), 200f);

	// Add navigation entities

	// Comm relay
	SectorEntityToken commRelay = system.addCustomEntity("elysium_comm_relay",
		"Elysium Comm Relay",
		"comm_relay",
		"elysium");
	commRelay.setCircularOrbitPointingDown(elysiumStar, 180f, comDist, 365f);

	// Nav beacon
	SectorEntityToken navBeacon = system.addCustomEntity("elysium_nav_buoy",
		"Elysium Nav Beacon",
		"nav_buoy",
		"elysium");
	navBeacon.setCircularOrbitPointingDown(elysiumStar, 90f, navDist, 365f);

	// Sensor array
	SectorEntityToken sensorArray = system.addCustomEntity("elysium_sensor_array",
		"Elysium Sensor Array",
		"sensor_array",
		"elysium");
	sensorArray.setCircularOrbitPointingDown(elysiumStar, 270f, sensorDist, 365f);

	// Add jump points
	JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint(
		"inner_jump",
		"Elysium Core Jump Point");
	jumpPoint1.setCircularOrbit(elysiumStar, 315f, jumpCenterDist, 200f);
	jumpPoint1.setStandardWormholeToHyperspaceVisual();
	system.addEntity(jumpPoint1);

	JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint(
		"outer_jump",
		"Elysium Fringe Jump Point");
	jumpPoint2.setCircularOrbit(elysiumStar, 225f, jumpFringeDist, 600f);
	jumpPoint2.setStandardWormholeToHyperspaceVisual();
	system.addEntity(jumpPoint2);

	// Generate hyperspace connections
	system.autogenerateHyperspaceJumpPoints(true, false);
    }
}