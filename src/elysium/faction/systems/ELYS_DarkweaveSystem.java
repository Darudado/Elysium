package elysium.faction.systems;

import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import elysium.faction.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.util.Misc;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import java.util.Random;

public class ELYS_DarkweaveSystem {

    // Constants for orbital distances - MODIFIED FOR MORE COMPACT LAYOUT
    // More compact layout while preserving isolation feeling
    final float greenholdDist = 9000f;     // Reduced from 18000f
    final float thaumaspireDist = 8000f;   // Reduced from 15500f and moved closer to Greenhold
    final float shadowveilDist = 12000f;   // Reduced from 22000f
    final float whisperholdDist = 2500f;   // Slightly reduced from 3500f
    final float luminarchDist = 14000f;    // Reduced from 25000f

    // Dead zone size (no planets inside this except debris) - reduced
    final float deadZoneRadius = 3500f;    // Reduced from 6000f

    // Jump points and navigation - MODIFIED
    final float jumpFringeDist = 10000f;   // Reduced from 19000f
    final float jumpInnerDist = 7000f;     // Reduced from 12000f
    final float comDist = 6500f;           // Reduced from 9000f
    final float navDist = 5000f;           // Reduced from 7000f
    final float sensorDist = 4000f;        // Reduced from 5000f

    public void generate(SectorAPI sector) {
	// Create the star system
	StarSystemAPI system = sector.createStarSystem("Darkweave");
	system.setOptionalUniqueId("Darkweave");
	system.addTag("theme_unsafe");
	system.addTag("theme_hidden");
	system.addTag("ELYS_void");
	system.getLocation().set(-34000, -20000); // Southwest quadrant, near Elysium
	// Initialize the black hole as the center
	PlanetAPI voidsEye = system.initStar("star_voids_eye", // unique id
		"black_hole", // star type
		350f, // radius
		0); // no corona for black hole

	voidsEye.setName("Void's Eye");
	voidsEye.setCustomDescriptionId("star_voids_eye");

	// Enhanced visual effects for the isolated black hole
	system.addRingBand(voidsEye, "misc", "rings_special0", 256f, 0, new Color(60, 20, 120), 256f, 700, 50f);
	system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 1, new Color(100, 50, 150, 100), 256f, 1100, 70f);

	// Add more dramatic accretion disk effects (adjusted for smaller system)
	system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 3, new Color(50, 20, 100, 130), 256f, 1200, 40f);
	system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 0, new Color(80, 30, 120, 150), 256f, 1600, 50f);
	system.addRingBand(voidsEye, "misc", "rings_dust0", 256f, 2, new Color(60, 10, 90, 170), 256f, 2000, 60f);

	// MODIFIED: Moved Greenhold and Thaumaspire closer (now about 60 degrees apart)
	// Greenhold (terran world)
	PlanetAPI greenhold = system.addPlanet("greenhold",
		voidsEye,
		"Greenhold",
		"terran",
		135f,
		170f,
		greenholdDist,
		270f);

	// Add market to Greenhold (unchanged)
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
				Industries.REFINING,
				Industries.HEAVYBATTERIES
			)
		),
		0.3f,
		false,
		true);

	greenhold.setCustomDescriptionId("planet_greenhold");

	// AI cores (unchanged)
	if (greenhold_market.hasIndustry(Industries.MEGAPORT)) {
	    greenhold_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (greenhold_market.hasIndustry(Industries.STARFORTRESS_HIGH)) {
	    greenhold_market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (greenhold_market.hasIndustry(Industries.MILITARYBASE)) {
	    greenhold_market.getIndustry(Industries.MILITARYBASE).setAICoreId(Commodities.BETA_CORE);
	}
	if (greenhold_market.hasIndustry(Industries.REFINING)) {
	    greenhold_market.getIndustry(Industries.REFINING).setAICoreId(Commodities.BETA_CORE);
	}
	if (greenhold_market.hasIndustry(Industries.FARMING)) {
	    greenhold_market.getIndustry(Industries.FARMING).setAICoreId(Commodities.GAMMA_CORE);
	}
	if (greenhold_market.hasIndustry(Industries.LIGHTINDUSTRY)) {
	    greenhold_market.getIndustry(Industries.LIGHTINDUSTRY).setAICoreId(Commodities.GAMMA_CORE);
	}

	// Military commander (unchanged)
	PersonAPI militaryCommander = Global.getFactory().createPerson();
	militaryCommander.setId("elysium_commander");
	militaryCommander.setFaction("elysium");
	militaryCommander.setGender(FullName.Gender.MALE);
	militaryCommander.setRankId(Ranks.SPACE_ADMIRAL);
	militaryCommander.setPostId(Ranks.POST_FLEET_COMMANDER);
	militaryCommander.setImportance(PersonImportance.HIGH);
	militaryCommander.getName().setFirst("Thorne");
	militaryCommander.getName().setLast("Dawnblade");
	militaryCommander.setPortraitSprite("graphics/portraits/male/ELYS_16.png");

	Global.getSector().getImportantPeople().addPerson(militaryCommander);
	greenhold_market.getCommDirectory().addPerson(militaryCommander);
	greenhold_market.addPerson(militaryCommander);

	// MODIFIED: Moved Thaumaspire closer to Greenhold
	PlanetAPI thaumaspire = system.addPlanet("thaumaspire",
		voidsEye,
		"Thaumaspire",
		"lava",
		80f, // Changed angle to be closer to Greenhold
		110f,
		thaumaspireDist,
		220f);

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
				Industries.MILITARYBASE,
				Industries.SPACEPORT,
				Industries.HEAVYBATTERIES,
				Industries.STARFORTRESS_HIGH,
				Industries.WAYSTATION,
				Industries.HEAVYBATTERIES
			)
		),
		0.3f,
		false,
		true);

	thaumaspire.setCustomDescriptionId("planet_thaumaspire");

	// Add ORBITALWORKS with PRISTINE_NANOFORGE (unchanged)
	thaumaspire_market.addIndustry(Industries.ORBITALWORKS, Collections.singletonList(Items.PRISTINE_NANOFORGE));

	// AI cores (unchanged)
	if (thaumaspire_market.hasIndustry(Industries.STARFORTRESS_HIGH)) {
	    thaumaspire_market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (thaumaspire_market.hasIndustry(Industries.ORBITALWORKS)) {
	    thaumaspire_market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
	}
	if (thaumaspire_market.hasIndustry(Industries.MILITARYBASE)) {
	    thaumaspire_market.getIndustry(Industries.MILITARYBASE).setAICoreId(Commodities.BETA_CORE);
	}
	if (thaumaspire_market.hasIndustry(Industries.MINING)) {
	    thaumaspire_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
	}

	// MODIFIED: Shadowveil (gas giant) - moved closer
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

	// MODIFIED: Whisperhold (barren world just outside dead zone)
	PlanetAPI whisperhold = system.addPlanet("whisperhold",
		voidsEye,
		"Whisperhold",
		"barren-bombarded",
		270f,
		60f,
		whisperholdDist,
		120f);

	Misc.initConditionMarket(whisperhold);
	MarketAPI whisperhold_market = whisperhold.getMarket();
	whisperhold_market.addCondition(Conditions.NO_ATMOSPHERE);
	whisperhold_market.addCondition(Conditions.LOW_GRAVITY);
	whisperhold_market.addCondition(Conditions.RARE_ORE_MODERATE);
	whisperhold_market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);
	whisperhold_market.addCondition(Conditions.DARK);
	whisperhold.setCustomDescriptionId("planet_whisperhold");

	// MODIFIED: Gate position
	SectorEntityToken gate = system.addCustomEntity("gateStarshard",
		"Starshard gate",
		"inactive_gate",
		Factions.NEUTRAL);
	gate.setCircularOrbitPointingDown(voidsEye, 120f, 8500f, 140f); // Moved closer

	// MODIFIED: Luminarch moved closer
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

	// Add gas giant rings to Luminarch (unchanged)
	system.addRingBand(luminarch, "misc", "rings_dust0", 256f, 2, Color.white, 256f, luminarch.getRadius() + 200, 50f);
	system.addRingBand(luminarch, "misc", "rings_dust0", 256f, 2, new Color(180, 180, 220), 256f, luminarch.getRadius() + 400, 70f);

	// Ebontide (moon of Luminarch) (unchanged)
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

	// MODIFIED: The Spires (3 barren worlds) - moved closer together
	PlanetAPI spire1 = system.addPlanet("spire1",
		voidsEye,
		"Spire Alpha",
		"barren",
		240f,
		50f,
		7000f, // Reduced from 13000f
		340f);

	PlanetAPI spire2 = system.addPlanet("spire2",
		voidsEye,
		"Spire Beta",
		"barren-bombarded",
		242f,
		60f,
		7200f, // Reduced from 13500f
		350f);

	PlanetAPI spire3 = system.addPlanet("spire3",
		voidsEye,
		"Spire Gamma",
		"barren-bombarded",
		244f,
		55f,
		7400f, // Reduced from 14000f
		360f);

	// MODIFIED: Asteroid fields adjusted for smaller system
	// Inner asteroid field 1 (close to black hole)
	SectorEntityToken darkweaveAF1 = system.addTerrain(Terrain.ASTEROID_FIELD,
		new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			400f, // min radius
			800f, // max radius
			15, // min asteroid count
			30, // max asteroid count
			4f, // min asteroid radius
			16f, // max asteroid radius
			"Inner Asteroids Field")); // name
	darkweaveAF1.setCircularOrbit(voidsEye, 60f, 1500f, 320f); // Moved closer

	// Inner asteroid field 2
	SectorEntityToken darkweaveAF2 = system.addTerrain(Terrain.ASTEROID_FIELD,
		new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			300f, // min radius
			600f, // max radius
			10, // min asteroid count
			20, // max asteroid count
			8f, // min asteroid radius
			16f, // max asteroid radius
			"Secondary Asteroids Field")); // name
	darkweaveAF2.setCircularOrbit(voidsEye, 150f, 2000f, 400f); // Moved closer

	// Dead zone edge asteroid field
	SectorEntityToken darkweaveAF3 = system.addTerrain(Terrain.ASTEROID_FIELD,
		new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			700f, // min radius
			1200f, // max radius
			25, // min asteroid count
			40, // max asteroid count
			8f, // min asteroid radius
			20f, // max asteroid radius
			"Dense Border Asteroids Field")); // name
	darkweaveAF3.setCircularOrbit(voidsEye, 200f, 3500f, 400f); // Moved closer

	// Outer asteroid field
	SectorEntityToken darkweaveAF4 = system.addTerrain(Terrain.ASTEROID_FIELD,
		new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
			500f, // min radius
			900f, // max radius
			20, // min asteroid count
			30, // max asteroid count
			6f, // min asteroid radius
			18f, // max asteroid radius
			"Outer Asteroids Belt")); // name
	darkweaveAF4.setCircularOrbit(voidsEye, 270f, 6000f, 400f); // Moved closer

	// MODIFIED: Debris fields adjusted for smaller system
	Random rand = new Random();

	// Inner intense debris field
	DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
		500f, // field radius
		1.2f, // density
		10000000f, // duration
		0f); // days the field will keep generating glowing pieces
	params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
	params1.baseSalvageXP = 750;
	SectorEntityToken debrisField1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random);
	debrisField1.setSensorProfile(800f);
	debrisField1.setDiscoverable(true);
	debrisField1.setCircularOrbit(voidsEye, rand.nextFloat() * 360f, 1000f, 120f); // Moved closer

	// Secondary debris field
	DebrisFieldTerrainPlugin.DebrisFieldParams params2 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
		400f, // field radius
		0.9f, // density
		10000000f, // duration
		0f);
	params2.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE;
	params2.baseSalvageXP = 600;
	SectorEntityToken debrisField2 = Misc.addDebrisField(system, params2, StarSystemGenerator.random);
	debrisField2.setSensorProfile(700f);
	debrisField2.setDiscoverable(true);
	debrisField2.setCircularOrbit(voidsEye, rand.nextFloat() * 360f, 2000f, 180f); // Moved closer

	// Third debris field
	DebrisFieldTerrainPlugin.DebrisFieldParams params3 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
		350f, // field radius
		0.8f, // density
		10000000f, // duration
		0f);
	params3.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
	params3.baseSalvageXP = 500;
	SectorEntityToken debrisField3 = Misc.addDebrisField(system, params3, StarSystemGenerator.random);
	debrisField3.setSensorProfile(600f);
	debrisField3.setDiscoverable(true);
	debrisField3.setCircularOrbit(voidsEye, rand.nextFloat() * 360f, 3000f, 200f); // Moved closer

	// MODIFIED: Navigation entities moved closer
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

	// MODIFIED: Jump points positioned near colonies but closer in
	JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint(
		"inner_jump",
		"Darkweave Inner Jump Point");
	jumpPoint1.setCircularOrbit(voidsEye, 150f, jumpInnerDist, 200f);
	jumpPoint1.setStandardWormholeToHyperspaceVisual();
	system.addEntity(jumpPoint1);

	JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint(
		"outer_jump",
		"Darkweave Outer Jump Point");
	jumpPoint2.setCircularOrbit(voidsEye, 260f, jumpFringeDist, 600f);
	jumpPoint2.setStandardWormholeToHyperspaceVisual();
	system.addEntity(jumpPoint2);

	// Generate hyperspace connections
	system.autogenerateHyperspaceJumpPoints(true, false);
	system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "ELYS_worldvoid");

    }
}