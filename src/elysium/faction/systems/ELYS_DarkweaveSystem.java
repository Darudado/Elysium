package elysium.faction.systems;

import elysium.faction.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;

import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;

import java.util.Random;

    public class ELYS_DarkweaveSystem {

	// Constants for orbital distances
	final float greenholdDist = 4500f;
	final float thaumaspireDist = 2200f;
	final float shadowveilDist = 8500f;
	final float starshard1Dist = 1800f;
	final float starshard2Dist = 2100f;
	final float luminarchDist = 10000f;

	// Jump points and navigation
	final float jumpFringeDist = 11000f;
	final float jumpInnerDist = 3000f;
	final float comDist = 9500f;
	final float navDist = 7000f;
	final float sensorDist = 5500f;

	public void generate(SectorAPI sector) {
	    // Create the star system
	    StarSystemAPI system = sector.createStarSystem("Darkweave");
	    system.getLocation().set(-34000, -20000); // Southwest quadrant, near Elysium
	    system.setBackgroundTextureFilename("graphics/backgrounds/ELYS_BG2.jpeg");

	    // Create a nebula effect for the system
	    /*SectorEntityToken darkweave_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/nebula_dark.png",
		    0, 0, // center of nebula
		    system, // location to add to
		    "terrain", "nebula_dark", // texture to use
		    4, 4, StarAge.YOUNG); // cell settings */

	    // Initialize the black hole as the center
	    PlanetAPI voidsEye = system.initStar("voids_eye", // unique id
		    "black_hole", // star type
		    350f, // radius
		    0); // no corona for black hole

	    voidsEye.setName("Void's Eye");
	    voidsEye.setCustomDescriptionId("star_voids_eye");

	    // Create a glow effect around the black hole
	    system.addRingBand(voidsEye, "misc", "rings_special0", 256f, 0, new Color(60, 20, 120), 256f, 700, 50f);
	    system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 1, new Color(100, 50, 150, 100), 256f, 1100, 70f);

	    // Greenhold (terran world)
	    PlanetAPI greenhold = system.addPlanet("greenhold",
		    voidsEye,
		    "Greenhold",
		    "terran",
		    135f,
		    170f,
		    greenholdDist,
		    270f);

	    // Add market to Greenhold
	    MarketAPI greenhold_market = ELYS_Gen.addMarketplace(
		    "elysium",
		    greenhold,
		    null,
		    "Greenhold",
		    6,
		    new ArrayList<>(
			    Arrays.asList(
				    Conditions.POPULATION_6,
				    Conditions.HABITABLE,
				    Conditions.FARMLAND_BOUNTIFUL,
				    Conditions.MILD_CLIMATE,
				    Conditions.ORGANICS_ABUNDANT,
				    Conditions.ORE_SPARSE,
				    Conditions.DARK
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
				    Industries.MILITARYBASE,
				    Industries.COMMERCE,
				    Industries.STARFORTRESS_HIGH,
				    Industries.WAYSTATION,
				    Industries.REFINING
			    )
		    ),
		    0.3f,
		    false,
		    true);

	    greenhold.setCustomDescriptionId("planet_greenhold");

	    // Add a military commander
	    PersonAPI militaryCommander = Global.getFactory().createPerson();
	    militaryCommander.setId("elysium_commander");
	    militaryCommander.setFaction("elysium");
	    militaryCommander.setGender(FullName.Gender.MALE);
	    militaryCommander.setRankId(Ranks.SPACE_ADMIRAL);
	    militaryCommander.setPostId(Ranks.POST_FLEET_COMMANDER);
	    militaryCommander.setImportance(PersonImportance.HIGH);
	    militaryCommander.getName().setFirst("Thorne");
	    militaryCommander.getName().setLast("Dawnblade");
	    militaryCommander.setPortraitSprite("graphics/portraits/portrait_mercenary01.png");

	    Global.getSector().getImportantPeople().addPerson(militaryCommander);
	    greenhold_market.getCommDirectory().addPerson(militaryCommander);
	    greenhold_market.addPerson(militaryCommander);

	    // Thaumaspire (volcanic world)
	    PlanetAPI thaumaspire = system.addPlanet("thaumaspire",
		    voidsEye,
		    "Thaumaspire",
		    "lava",
		    45f,
		    110f,
		    thaumaspireDist,
		    160f);

	    MarketAPI thaumaspire_market = ELYS_Gen.addMarketplace(
		    "elysium",
		    thaumaspire,
		    null,
		    "Thaumaspire",
		    5,
		    new ArrayList<>(
			    Arrays.asList(
				    Conditions.POPULATION_5,
				    Conditions.HOT,
				    Conditions.TECTONIC_ACTIVITY,
				    Conditions.ORE_ULTRARICH,
				    Conditions.RARE_ORE_RICH,
				    Conditions.VOLATILES_DIFFUSE,
				    Conditions.DARK
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
				    Industries.HEAVYINDUSTRY,
				    Industries.MILITARYBASE,
				    Industries.SPACEPORT,
				    Industries.HEAVYBATTERIES,
				    Industries.STARFORTRESS_HIGH,
				    Industries.WAYSTATION
			    )
		    ),
		    0.3f,
		    false,
		    true);

	    thaumaspire.setCustomDescriptionId("planet_thaumaspire");

	    // Add non-colonized celestial bodies

	    // Shadowveil (gas giant)
	    PlanetAPI shadowveil = system.addPlanet("shadowveil",
		    voidsEye,
		    "Shadowveil",
		    "gas_giant",
		    200f,
		    350f,
		    shadowveilDist,
		    480f);

	    Misc.initConditionMarket(shadowveil);
	    MarketAPI shadowveil_market = shadowveil.getMarket();
	    shadowveil_market.addCondition(Conditions.EXTREME_WEATHER);
	    shadowveil_market.addCondition(Conditions.HIGH_GRAVITY);
	    shadowveil_market.addCondition(Conditions.VOLATILES_ABUNDANT);
	    shadowveil_market.addCondition(Conditions.DARK);
	    shadowveil.setCustomDescriptionId("planet_shadowveil");

	    // Whisperhold (barren world close to black hole)
	    PlanetAPI whisperhold = system.addPlanet("whisperhold",
		    voidsEye,
		    "Whisperhold",
		    "barren-bombarded",
		    270f,
		    60f,
		    1400f,
		    120f);

	    Misc.initConditionMarket(whisperhold);
	    MarketAPI whisperhold_market = whisperhold.getMarket();
	    whisperhold_market.addCondition(Conditions.NO_ATMOSPHERE);
	    whisperhold_market.addCondition(Conditions.LOW_GRAVITY);
	    whisperhold_market.addCondition(Conditions.RARE_ORE_MODERATE);
	    whisperhold_market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);
	    whisperhold_market.addCondition(Conditions.DARK);
	    whisperhold.setCustomDescriptionId("planet_whisperhold");

	    SectorEntityToken gate = system.addCustomEntity("gateStarshard",
		    "Starshard gate",
		    "inactive_gate",
		    Factions.NEUTRAL);
	    gate.setCircularOrbitPointingDown(voidsEye, 120f, starshard1Dist, 140f);

	    // Luminarch (gas giant with rings)
	    PlanetAPI luminarch = system.addPlanet("luminarch",
		    voidsEye,
		    "Luminarch",
		    "ice_giant",
		    330f,
		    300f,
		    luminarchDist,
		    600f);

	    Misc.initConditionMarket(luminarch);
	    MarketAPI luminarch_market = luminarch.getMarket();
	    luminarch_market.addCondition(Conditions.EXTREME_WEATHER);
	    luminarch_market.addCondition(Conditions.HIGH_GRAVITY);
	    luminarch_market.addCondition(Conditions.VOLATILES_PLENTIFUL);
	    luminarch.setCustomDescriptionId("planet_luminarch");

	    // Add gas giant rings to Luminarch
	    system.addRingBand(luminarch, "misc", "rings_dust0", 256f, 2, Color.white, 256f, luminarch.getRadius() + 200, 50f);
	    system.addRingBand(luminarch, "misc", "rings_dust0", 256f, 2, new Color(180, 180, 220), 256f, luminarch.getRadius() + 400, 70f);

	    // Ebontide (moon of Luminarch)
	    PlanetAPI ebontide = system.addPlanet("ebontide",
		    luminarch,
		    "Ebontide",
		    "barren2",
		    30f,
		    80f,
		    600f,
		    60f);

	    Misc.initConditionMarket(ebontide);
	    MarketAPI ebontide_market = ebontide.getMarket();
	    ebontide_market.addCondition(Conditions.NO_ATMOSPHERE);
	    ebontide_market.addCondition(Conditions.LOW_GRAVITY);
	    ebontide_market.addCondition(Conditions.RUINS_SCATTERED);
	    ebontide.setCustomDescriptionId("planet_ebontide");

	    // The Spires (3 barren worlds in a line)
	    PlanetAPI spire1 = system.addPlanet("spire1",
		    voidsEye,
		    "Spire Alpha",
		    "barren",
		    240f,
		    50f,
		    6200f,
		    340f);

	    PlanetAPI spire2 = system.addPlanet("spire2",
		    voidsEye,
		    "Spire Beta",
		    "barren-bombarded",
		    242f,
		    60f,
		    6500f,
		    350f);

	    PlanetAPI spire3 = system.addPlanet("spire3",
		    voidsEye,
		    "Spire Gamma",
		    "barren-bombarded",
		    244f,
		    55f,
		    6800f,
		    360f);

	    // Add some asteroid fields for visual interest
	    SectorEntityToken darkweaveAF1 = system.addTerrain(Terrain.ASTEROID_FIELD,
		    new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			    200f, // min radius
			    300f, // max radius
			    8, // min asteroid count
			    16, // max asteroid count
			    4f, // min asteroid radius
			    16f, // max asteroid radius
			    "Asteroids Field")); // name
	    darkweaveAF1.setCircularOrbit(voidsEye, 60f, 5200f, 320f);

	    SectorEntityToken darkweaveAF2 = system.addTerrain(Terrain.ASTEROID_FIELD,
		    new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			    500f, // min radius
			    700f, // max radius
			    20, // min asteroid count
			    30, // max asteroid count
			    8f, // min asteroid radius
			    16f, // max asteroid radius
			    "Dense Asteroids Field")); // name
	    darkweaveAF2.setCircularOrbit(voidsEye, 200f, 7500f, 400f);

	    // Add accretion disk effect
	    system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 3, new Color(50, 20, 100, 130), 256f, 700, 40f);
	    system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 0, new Color(80, 30, 120, 150), 256f, 1000, 50f);

	    // Add debris fields
	    Random rand = new Random();

	    DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
		    200f, // field radius
		    0.8f, // density
		    10000000f, // duration
		    0f); // days the field will keep generating glowing pieces
	    params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
	    params1.baseSalvageXP = 500;
	    SectorEntityToken debrisField1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random);
	    debrisField1.setSensorProfile(800f);
	    debrisField1.setDiscoverable(true);
	    debrisField1.setCircularOrbit(voidsEye, rand.nextFloat() * 360f, 2700 + rand.nextInt(1000), 200f);

	    // Add navigation entities

	    // Comm relay
	    SectorEntityToken commRelay = system.addCustomEntity("darkweave_comm_relay",
		    "Darkweave Comm Relay",
		    "comm_relay",
		    "elysium");
	    commRelay.setCircularOrbitPointingDown(voidsEye, 180f, comDist, 365f);

	    // Nav beacon
	    SectorEntityToken navBeacon = system.addCustomEntity("darkweave_nav_buoy",
		    "Darkweave Nav Beacon",
		    "nav_buoy",
		    "elysium");
	    navBeacon.setCircularOrbitPointingDown(voidsEye, 90f, navDist, 365f);

	    // Sensor array
	    SectorEntityToken sensorArray = system.addCustomEntity("darkweave_sensor_array",
		    "Darkweave Sensor Array",
		    "sensor_array",
		    "elysium");
	    sensorArray.setCircularOrbitPointingDown(voidsEye, 270f, sensorDist, 365f);

	    // Add jump points
	    JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint(
		    "inner_jump",
		    "Darkweave Core Jump Point");
	    jumpPoint1.setCircularOrbit(voidsEye, 315f, jumpInnerDist, 200f);
	    jumpPoint1.setStandardWormholeToHyperspaceVisual();
	    system.addEntity(jumpPoint1);

	    JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint(
		    "outer_jump",
		    "Darkweave Fringe Jump Point");
	    jumpPoint2.setCircularOrbit(voidsEye, 225f, jumpFringeDist, 600f);
	    jumpPoint2.setStandardWormholeToHyperspaceVisual();
	    system.addEntity(jumpPoint2);

	    // Generate hyperspace connections
	    system.autogenerateHyperspaceJumpPoints(true, false);
	}
    }