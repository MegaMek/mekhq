package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomSkillsGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.PlanetaryConditions;
import megamek.common.UnitType;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.AtBConfiguration;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;

/**
 * This class handles the creation and substantive manipulation of AtBDynamicScenarios
 * @author NickAragua
 *
 */
public class AtBDynamicScenarioFactory {
    public static AtBDynamicScenario initializeScenarioFromTemplate(ScenarioTemplate template, AtBContract contract, Campaign campaign) {        
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        
        scenario.setName(template.name);
        scenario.setDesc(template.detailedBriefing);
        scenario.setScenarioTemplate(template);
        
        setLightConditions(scenario);
        setWeather(scenario);
        setPlanetaryConditions(scenario, contract, campaign);
        
        return scenario;
    }
    
    /**
     * Method that should be called when all "required" player forces have been assigned to a scenario.
     * It will generate all primary allied-player, allied-bot and enemy forces,
     * as well as rolling and applying scenario modifiers. 
     * @param scenario
     * @param contract
     * @param campaign
     */
    public static void finalizeScenario(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign) {
        // step 1: loop through all force templates, in the following order:
        //  any allied-player-controlled templates
        //  any allied-bot-controlled templates
        //  any enemy-bot-controlled templates
        for(ScenarioForceTemplate forceTemplate : scenario.getTemplate().scenarioForces.values()) {
            //if(forceTemplate.)
        }
        
        //setScenarioMapSize(scenario);
    }
    
    /**
     * Handles random determination of light conditions for the given scenario, as per AtB rules
     * @param scenario The scenario for which to set lighting conditions.
     */
    public static void setLightConditions(AtBDynamicScenario scenario) {
        int light = PlanetaryConditions.L_DAY;

        int roll = Compute.randomInt(10) + 1;
        if (roll < 6) light = PlanetaryConditions.L_DAY;
        else if (roll < 8) light = PlanetaryConditions.L_DUSK;
        else if (roll == 8) light = PlanetaryConditions.L_FULL_MOON;
        else if (roll == 9) light = PlanetaryConditions.L_MOONLESS;
        else light = PlanetaryConditions.L_PITCH_BLACK;
        
        scenario.setLight(light);
    }
    
    /**
     * Handles random determination of weather/wind/fog conditions for the given scenario, as per AtB rules
     * @param scenario The scenario for which to set weather conditions.
     */
    public static void setWeather(AtBDynamicScenario scenario) {
        int weather = PlanetaryConditions.WE_NONE;
        int wind = PlanetaryConditions.WI_NONE;
        int fog = PlanetaryConditions.FOG_NONE;

        int roll = Compute.randomInt(10) + 1;
        int r2 = Compute.d6();
        if (roll < 6) return;
        else if (roll == 6) {
            if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_RAIN;
            else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_RAIN;
            else weather = PlanetaryConditions.WE_HEAVY_RAIN;
        } else if (roll == 7) {
            if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_SNOW;
            else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_SNOW;
            else weather = PlanetaryConditions.WE_HEAVY_SNOW;
        } else if (roll == 8) {
            if (r2 < 4) wind = PlanetaryConditions.WI_LIGHT_GALE;
            else if (r2 < 6) wind = PlanetaryConditions.WI_MOD_GALE;
            else wind = PlanetaryConditions.WI_STRONG_GALE;
        } else if (roll == 9) {
            if (r2 == 1) wind = PlanetaryConditions.WI_STORM;
            else if (r2 == 2) weather = PlanetaryConditions.WE_DOWNPOUR;
            else if (r2 == 3) weather = PlanetaryConditions.WE_SLEET;
            else if (r2 == 4) weather = PlanetaryConditions.WE_ICE_STORM;
            else if (r2 == 5) wind = PlanetaryConditions.WI_TORNADO_F13; // tornadoes are classified as wind rather than weather.
            else if (r2 == 6) wind = PlanetaryConditions.WI_TORNADO_F4;
        } else {
            if (r2 < 5) fog = PlanetaryConditions.FOG_LIGHT;
            else fog = PlanetaryConditions.FOG_HEAVY;
        }
        
        scenario.setWeather(weather);
        scenario.setWind(wind);
        scenario.setFog(fog);
    }
    
    /**
     * Method that handles setting planetary conditions - atmospheric pressure and gravity currently -
     * based on the planet on which the scenario is taking place.
     * @param scenario The scenario to manipulate
     * @param mission The active mission for the scenario 
     * @param campaign The current campaign
     */
    public static void setPlanetaryConditions(AtBDynamicScenario scenario, AtBContract mission, Campaign campaign) {
        if (null != mission) {
            Planet p = Planets.getInstance().getPlanets().get(mission.getPlanetId());
            if (null != p) {
                int atmosphere = Utilities.nonNull(p.getPressure(Utilities.getDateTimeDay(campaign.getCalendar())), scenario.getAtmosphere());
                float gravity = Utilities.nonNull(p.getGravity(), scenario.getGravity()).floatValue();
                
                scenario.setAtmosphere(atmosphere);
                scenario.setGravity(gravity);
            }
        }
    }
    
    /**
     * Sets dynamic AtB-sized base map size for the given scenario.
     * @param scenario The scenario to process.
     */
    public static void setScenarioMapSize(AtBDynamicScenario scenario) {
        int mapSizeX;
        int mapSizeY;
        ScenarioTemplate template = scenario.getTemplate();
        
        // if the template says to use standard AtB sizing, determine it randomly here
        if(template.mapParameters.useStandardAtBSizing) {
            int roll = Compute.randomInt(20) + 1;
            if (roll < 6) {
                mapSizeX = 20;
                mapSizeY = 10;
            } else if (roll < 11) {
                mapSizeX = 10;
                mapSizeY = 20;
            } else if (roll < 13) {
                mapSizeX = 30;
                mapSizeY = 10;
            } else if (roll < 15) {
                mapSizeX = 10;
                mapSizeY = 30;
            } else if (roll < 19) {
                mapSizeX = 20;
                mapSizeY = 20;
            } else if (roll == 19) {
                mapSizeX = 40;
                mapSizeY = 10;
            } else {
                mapSizeX = 10;
                mapSizeY = 40;
            }
        // otherwise, the map width/height have been specified explicitly 
        } else {
            mapSizeX = template.mapParameters.baseWidth;
            mapSizeY = template.mapParameters.baseHeight;
        }
        
        // increment map size by template-specified increments
        mapSizeX += template.mapParameters.widthScalingIncrement * scenario.getLanceCount();
        mapSizeY += template.mapParameters.heightScalingIncrement * scenario.getLanceCount();
        
        // 50/50 odds to rotate the map 90 degrees if specified.
        if(template.mapParameters.allowRotation) {
            int roll = Compute.randomInt(20) + 1;
            if(roll <= 10) {
                int swap = mapSizeX;
                mapSizeX = mapSizeY;
                mapSizeY = swap;
            }
        }
        
        scenario.setMapSizeX(mapSizeX);
        scenario.setMapSizeY(mapSizeY);
    }

    public static List<Entity> generateForce(ScenarioForceTemplate sft, AtBContract mission, Campaign campaign) {
        return null;
    }
    
    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction        The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill            The RandomSkillGenerator constant that represents the skill level of the overall force.
     * @param quality        The equipment rating of the force.
     * @param unitType        The UnitTableData constant for the type of unit to generate.
     * @param weightClass    The weight class of the unit to generate
     * @param campaign
     * @return                A new Entity with crew.
     */
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, 
            AtBContract contract, Campaign campaign) {
        MechSummary ms = null;
        if (unitType == UnitType.TANK) {
            if (campaign.getCampaignOptions().getOpforUsesVTOLs()) {
                ms = campaign.getUnitGenerator()
                        .generate(faction, unitType, weightClass, campaign.getCalendar()
                                .get(Calendar.YEAR), quality, IUnitGenerator.MIXED_TANK_VTOL, null);            
            } else {
                ms = campaign.getUnitGenerator()
                        .generate(faction, unitType, weightClass, campaign.getCalendar()
                                .get(Calendar.YEAR), quality, v -> !v.getUnitType().equals("VTOL"));
            }
        } else {
            ms = campaign.getUnitGenerator()
                    .generate(faction, unitType, weightClass, campaign.getCalendar()
                            .get(Calendar.YEAR), quality);
        }

        if (ms == null) {
            return null;
        }
        
        return createEntityWithCrew(faction, skill, contract, campaign, ms);
    }

    /**
     * @param faction Faction to use for name generation
     * @param skill Skill rating of the crew
     * @param campaign The campaign instance
     * @param ms Which entity to generate
     * @return An crewed entity
     */
    public static Entity createEntityWithCrew(String faction, int skill,
            AtBContract contract, Campaign campaign, MechSummary ms) {
        final String METHOD_NAME = "createEntityWithCrew(String,int,Campaign,MechSummary)"; //$NON-NLS-1$
        Entity en = null;
        try {
            en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception ex) {
            en = null;
            MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, METHOD_NAME, LogLevel.ERROR,
                    "Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$
            MekHQ.getLogger().error(AtBDynamicScenarioFactory.class, METHOD_NAME, ex);
            return null;
        }

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        Faction f = Faction.getFaction(faction);

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(f.getNameGenerator());
        String crewName = rng.generate();

        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(skill);

        if (f.isClan()) {
            rsg.setType(RandomSkillsGenerator.T_CLAN);
        }
        int[] skills = rsg.getRandomSkills(en);

        if (f.isClan() && Compute.d6(2) > 8 - contract.getEnemySkill()
                + skills[0] + skills[1]) {
            int phenotype;
            switch (UnitType.determineUnitTypeCode(en)) {
            case UnitType.MEK:
                phenotype = Bloodname.P_MECHWARRIOR;
                break;
            case UnitType.BATTLE_ARMOR:
                phenotype = Bloodname.P_ELEMENTAL;
                break;
            case UnitType.AERO:
                phenotype = Bloodname.P_AEROSPACE;
                break;
            case UnitType.PROTOMEK:
                phenotype = Bloodname.P_PROTOMECH;
                break;
            default:
                phenotype = -1;
            }
            if (phenotype >= 0) {
                crewName += " " + Bloodname.randomBloodname(faction, phenotype, campaign.getCalendar().get(Calendar.YEAR)).getName();
            }
        }

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName,
                            Compute.getFullCrewSize(en),
                            skills[0], skills[1]));

        UUID id = UUID.randomUUID();
        en.setExternalIdAsString(id.toString());
        
        return en;
    }

    /**
     * Generates a new Entity without using a RAT. Used for turrets and employer-assigned
     * Leopard Dropships.
     *
     * @param name            Full name (chassis + model) of the entity to generate.
     * @param fName            Faction code to use for crew name generation
     * @param skill            RandomSkillsGenerator.L_* constant for the average force skill level.
     * @param campaign
     * @return                A new Entity
     */
    public static Entity getEntityByName(String name, String fName, int skill, Campaign campaign) {
        final String METHOD_NAME = "getEntityByName(String,String,int,Campaign"; //$NON-NLS-1$
        
        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(
                name);
        if (mechSummary == null) {
            return null;
        }

        MechFileParser mechFileParser = null;
        try {
            mechFileParser = new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName());
        } catch (Exception ex) {
            MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, METHOD_NAME, LogLevel.ERROR,
                    "Unable to load unit: " + name); //$NON-NLS-1$
            MekHQ.getLogger().error(AtBDynamicScenarioFactory.class, METHOD_NAME, ex);
        }
        if (mechFileParser == null) {
            return null;
        }

        Entity en = mechFileParser.getEntity();

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        Faction faction = Faction.getFaction(fName);

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());

        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(skill);

        if (faction.isClan()) rsg.setType(RandomSkillsGenerator.T_CLAN);
        int[] skills = rsg.getRandomSkills(en);
        en.setCrew(new Crew(en.getCrew().getCrewType(), rng.generate(),
                            Compute.getFullCrewSize(en),
                            skills[0], skills[1]));

        UUID id = UUID.randomUUID();
        en.setExternalIdAsString(id.toString());

        return en;
    }

    /**
     * Units that exceed the maximum weight for individual entities in the scenario
     * are replaced in the lance by two lighter units.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param maxWeight        The maximum weight allowed for the force by the parameters of the scenario type
     * @return                A new String of the same format as weights
     */
    private static String adjustForMaxWeight(String weights, int maxWeight) {
        String retVal = weights;
        if (maxWeight == EntityWeightClass.WEIGHT_HEAVY) {
            //Hide and Seek (defender)
            retVal = weights.replaceAll("A", "LM");
        } else if (maxWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            //Probe, Recon Raid (attacker)
            retVal = weights.replaceAll("A", "MM");
            retVal = retVal.replaceAll("H", "LM");
        }
        return retVal;
    }

    /**
     * Adjust weights of units in a lance for factions that do not fit the typical
     * weight distribution.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param faction        The code of the faction to which the force belongs.
     * @return                A new String of the same format as weights
     */
    private static String adjustWeightsForFaction(String weights, String faction) {
        /* Official AtB rules only specify DC, LA, and FWL; I have added
         * variations for some Clans.
         */
        String retVal = weights;
        if (faction.equals("DC")) {
            retVal = weights.replaceFirst("MM", "LH");
        }
        if ((faction.equals("LA") ||
                faction.equals("CCO") || faction.equals("CGB"))
                && weights.matches("[LM]{3,}")) {
            retVal = weights.replaceFirst("M", "H");
        }
        if (faction.equals("FWL") || faction.equals("CIH")) {
            retVal = weights.replaceFirst("HA", "HH");
        }
        return retVal;
    }
}
