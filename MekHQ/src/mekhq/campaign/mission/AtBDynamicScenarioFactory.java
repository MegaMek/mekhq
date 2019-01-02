package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomSkillsGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ratgenerator.MissionRole;
import megamek.common.Board;
import megamek.common.BombType;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.OffBoardDirection;
import megamek.common.PlanetaryConditions;
import megamek.common.UnitType;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.AtBConfiguration;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.campaign.universe.UnitGeneratorParameters;

/**
 * This class handles the creation and substantive manipulation of AtBDynamicScenarios
 * @author NickAragua
 *
 */
public class AtBDynamicScenarioFactory {
    /**
     * Unspecified weight class for units, used when the unit type doesn't support weight classes 
     */
    public static int UNIT_WEIGHT_UNSPECIFIED = -1;
    
    private static int[] validBotBombs = { BombType.B_HE, BombType.B_CLUSTER, BombType.B_RL, BombType.B_INFERNO, BombType.B_THUNDER };
    
    private static int IS_LANCE_SIZE = 4;
    private static int CLAN_MH_LANCE_SIZE = 5;
    private static int COMSTAR_LANCE_SIZE = 6;
    
    /**
     * Method that sets some initial scenario parameters from the given template, prior to force generation and such.
     * @param template The template to use when populating the new scenario.
     * @param contract The contract in which the scenario is to occur.
     * @param campaign The current campaign.
     * @return
     */
    public static AtBDynamicScenario initializeScenarioFromTemplate(ScenarioTemplate template, AtBContract contract, Campaign campaign) {        
        AtBDynamicScenario scenario = new AtBDynamicScenario();
        
        scenario.setName(template.name);
        scenario.setDesc(template.detailedBriefing);
        scenario.setScenarioTemplate(template);
        scenario.setEffectiveOpforSkill(contract.getEnemySkill());
        scenario.setEffectiveOpforQuality(contract.getEnemyQuality());
        
        if(campaign.getCampaignOptions().getUseLightConditions()) {
            setLightConditions(scenario);
        }
        
        // set weather conditions if the user wants to play with them and is on a ground map
        if(campaign.getCampaignOptions().getUseWeatherConditions() &&
                (template.mapParameters.mapLocation == MapLocation.AllGroundTerrain ||
                template.mapParameters.mapLocation == MapLocation.SpecificGroundTerrain)) {
            setWeather(scenario);
        }
        
        if(campaign.getCampaignOptions().getUsePlanetaryConditions()) {
            setPlanetaryConditions(scenario, contract, campaign);
        }
        
        setTerrain(scenario);
        
        return scenario;
    }
    
    /**
     * Method that should be called when all "required" player forces have been assigned to a scenario.
     * It will generate all primary allied-player, allied-bot and enemy forces,
     * as well as rolling and applying scenario modifiers. 
     * @param scenario Scenario to finalize
     * @param contract Contract in which the scenario is occurring
     * @param campaign Current campaign.
     */
    public static void finalizeScenario(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign) {
        // just in case, clear old bot forces.
        for(int x = scenario.getNumBots() - 1; x >= 0; x--) {
            scenario.removeBotForce(x);
        }
        
        // fix the player force weight class and unit count at the current time.
        int playerForceWeightClass = calculatePlayerForceWeightClass(scenario, campaign);
        int playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign);
        
        // at this point, only the player forces are present and contributing to BV/unit count
        int generatedLanceCount = generateForces(scenario, contract, campaign, playerForceWeightClass);
                
        // approximate estimate, anyway.
        scenario.setLanceCount(generatedLanceCount + (playerForceUnitCount / 4));
        setScenarioMapSize(scenario);
        setDeploymentZones(scenario);
        
        setScenarioRerolls(scenario, campaign);
        
        translatePlayerNPCsToAttached(scenario, campaign);
        setDeploymentTurns(scenario, campaign);
    }
    
    /**
     * "Meaty" function that generates a set of forces for the given scenario of the given force alignment.
     * @param scenario Scenario for which we're generating forces
     * @param contract The contract on which we're currently working. Used for skill/quality/planetary info parameters
     * @param campaign The current campaign
     * @param weightClass The maximum weight class of the units to generate (ignored )
     * @return How many "lances" or other individual units were generated.
     */
    private static int generateForces(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign, int weightClass) { 
        
        int generatedLanceCount = 0;
        List<ScenarioForceTemplate> forceTemplates = scenario.getTemplate().getAllScenarioForces();
        
        // organize the forces by bucket.
        Map<Integer, List<ScenarioForceTemplate>> orderedForceTemplates = new HashMap<>();
        List<Integer> generationOrders = new ArrayList<>();
        
        for(ScenarioForceTemplate forceTemplate : forceTemplates) {
            if(!orderedForceTemplates.containsKey(forceTemplate.getGenerationOrder())) {
                orderedForceTemplates.put(forceTemplate.getGenerationOrder(), new ArrayList<>());
                generationOrders.add(forceTemplate.getGenerationOrder());
            }
            
            orderedForceTemplates.get(forceTemplate.getGenerationOrder()).add(forceTemplate);
        }
        
        // sort it by bucket in ascending order just in case
        Collections.sort(generationOrders);
        int effectiveBV;
        int effectiveUnitCount;
        
        // loop through all the generation orders we have, in ascending order
        // generate all forces in a specific order level taking into account previously generated but not current order levels.
        // recalculate effective BV and unit count each time we change levels
        for(int generationOrder : generationOrders) {
            List<ScenarioForceTemplate> currentForceTemplates = orderedForceTemplates.get(generationOrder);
            effectiveBV = calculateEffectiveBV(scenario, campaign);
            effectiveUnitCount = calculateEffectiveUnitCount(scenario, campaign);
            
            for(ScenarioForceTemplate forceTemplate : currentForceTemplates) {
                generatedLanceCount += generateForce(scenario, contract, campaign, 
                    effectiveBV, effectiveUnitCount, weightClass, forceTemplate);
            }
        }
        
        return generatedLanceCount;
    }
    
    /** "Meaty" function that generates a set of forces for the given scenario of the given force alignment.
    * @param scenario Scenario for which we're generating forces
    * @param contract The contract on which we're currently working. Used for skill/quality/planetary info parameters
    * @param campaign The current campaign
    * @param effectiveBV The effective battle value, up to this point, of player and allied units
    * @param effectiveUnitCount The effective unit count, up to this point, of player and allied units
    * @param weightClass The maximum weight class of the units to generate (ignored )
    * @param forceTemplate The force template to use to generate the force
    * @return How many "lances" or other individual units were generated. 
    */
    public static int generateForce(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign,
            int effectiveBV, int effectiveUnitCount, int weightClass, ScenarioForceTemplate forceTemplate) {
        String factionCode = "";
        int skill = 0;
        int quality = 0;
        int generatedLanceCount = 0;
        DateTime currentDate = Utilities.getDateTimeDay(campaign.getCalendar());
        ForceAlignment forceAlignment = ForceAlignment.getForceAlignment(forceTemplate.getForceAlignment());
        
        switch(forceAlignment) {
        case Allied:
        case Player:
            factionCode = contract.getEmployerCode();
            skill = contract.getAllySkill();
            quality = contract.getAllyQuality();
            break;
        case Opposing:
        case Third:
            factionCode = contract.getEnemyCode();
            skill = scenario.getEffectiveOpforSkill();
            quality = scenario.getEffectiveOpforQuality();
            break;
        case PlanetOwner:
            // planet owner is the first of the factions that owns the current planet.
            // if there's no such thing, then mercenaries.
            List<String> planetFactions = contract.getPlanet().getFactions(currentDate);
            if(planetFactions != null && !planetFactions.isEmpty()) {
                factionCode = planetFactions.get(0);
                Faction ownerFaction = Faction.getFaction(factionCode);
                
                if(ownerFaction.is(Tag.ABANDONED)) {
                    factionCode = "MERC";
                }
            } else {
                factionCode = "MERC";
            }
            
            skill = scenario.getEffectiveOpforSkill();
            quality = scenario.getEffectiveOpforQuality();
            break;
        default:
            MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, "generateForce", LogLevel.WARNING, 
                    String.format("Invalid force alignment %d", forceTemplate.getForceAlignment()));
        }
        
        String parentFactionType = AtBConfiguration.getParentFactionType(factionCode);
        boolean isPlanetOwner = contract.getPlanet().getFactions(currentDate).contains(factionCode);
        boolean usingAerospace = forceTemplate.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX ||
                                    forceTemplate.getAllowedUnitType() == UnitType.CONV_FIGHTER ||
                                    forceTemplate.getAllowedUnitType() == UnitType.AERO;
        
        // here we determine the "lance size". Aircraft almost always come in pairs, mechs and tanks, not so much.
        int lanceSize = usingAerospace ? 2 : getLanceSize(factionCode);
        
        
        // don't generate forces flagged as player-supplied
        if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) {
            return 0;
        }
    
        // determine generation parameters
        int forceBV = 0;
        int forceBVBudget = (int) (effectiveBV * forceTemplate.getForceMultiplier());
        int forceUnitBudget = 0;
        if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.UnitCountScaled.ordinal()) {
            forceUnitBudget = (int) (effectiveUnitCount * forceTemplate.getForceMultiplier());
        } else if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.FixedUnitCount.ordinal()) {
            forceUnitBudget = forceTemplate.getFixedUnitCount() == ScenarioForceTemplate.FIXED_UNIT_SIZE_LANCE ?
                    lanceSize : forceTemplate.getFixedUnitCount();
        }
        
        ArrayList<Entity> generatedEntities = new ArrayList<>();
        
        boolean stopGenerating = false;
        String currentLanceWeightString = "";
        
        //  While force has not surpassed BV cap || unit cap
        //      get me a unit types array
        //      get me a unit weight string
        //      use unit weight string to generate a list of entities
        //  Step 2.1 If force has surpassed unit cap, remove randomly selected units until it's at unit cap        
        while(!stopGenerating) {
            List<Entity> generatedLance;
        
            // atb generates between 1 and 3 lances at a time
            // so we generate a new batch each time we run out
            if(currentLanceWeightString.isEmpty()) {
                currentLanceWeightString = campaign.getAtBConfig().selectBotLances(parentFactionType, weightClass);
            }
            
            // if we are using the 'atb aero mix', let's decide now whether it's aero or conventional fighter
            int actualUnitType = forceTemplate.getAllowedUnitType();
            if(isPlanetOwner && actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
                actualUnitType = Compute.d6() > 3 ? UnitType.AERO : UnitType.CONV_FIGHTER;
            } else if(actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
                actualUnitType = UnitType.AERO;
            }
            
            // some special cases that don't fit into the regular RAT generation mechanism
            // gun emplacements use a separate set of rats
            if(actualUnitType == UnitType.GUN_EMPLACEMENT) {
                generatedLance = generateTurrets(4, skill, quality, campaign);
            // atb civilians use a separate rat
            } else if(actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS) {
                generatedLance = generateCivilianUnits(4, campaign);
            // meks, asf and tanks support weight class specification, as does the "standard atb mix"
            } else if(IUnitGenerator.unitTypeSupportsWeightClass(actualUnitType) ||
                    (actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX)) { 
                List<Integer> unitTypes = generateUnitTypes(actualUnitType, lanceSize, campaign);
                
                // special case: if we're generating artillery, there's not a lot of variety
                // in artillery unit weight classes, so we ignore that specification
                if(!forceTemplate.getUseArtillery()) {
                    String unitWeights = generateUnitWeights(unitTypes, factionCode, 
                            AtBConfiguration.decodeWeightStr(currentLanceWeightString, 0), forceTemplate.getMaxWeightClass(), campaign);
         
                    generatedLance = generateLance(factionCode, skill, 
                            quality, unitTypes, unitWeights, false, campaign);
                } else {
                    generatedLance = generateLance(factionCode, skill, 
                            quality, unitTypes, true, campaign);
                }
            // everything else doesn't support weight class specification
            } else {
                List<Integer> unitTypes = generateUnitTypes(actualUnitType, lanceSize, campaign);
                generatedLance = generateLance(factionCode, skill, quality, unitTypes, forceTemplate.getUseArtillery(), campaign);
            }
            
            // no reason to go into an endless loop if we can't generate a lance
            if(generatedLance.isEmpty()) {
                stopGenerating = true;
                MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, "generateForces", LogLevel.WARNING, 
                        String.format("Unable to generate units from RAT: %s, type %d, max weight %d", 
                                factionCode, forceTemplate.getAllowedUnitType(), weightClass));
                continue;
            }
            
            if(forceTemplate.getAllowAeroBombs()) {
                populateAeroBombs(generatedLance, campaign);
            }
            
            if(forceTemplate.getUseArtillery() && forceTemplate.getDeployOffboard()) {
                deployArtilleryOffBoard(generatedLance);
            }
            
            setStartingAltitude(generatedLance, forceTemplate.getStartingAltitude());
                        
            // if force contributes to map size, increment the generated "lance" count
            if(forceTemplate.getContributesToMapSize()) {
                generatedLanceCount++;
            }
            
            for(Entity ent : generatedLance) {
                forceBV += ent.calculateBattleValue();
                generatedEntities.add(ent);
            }
            
            // terminate force generation if we've gone over our unit count or bv budget
            if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.BVScaled.ordinal()) {
                stopGenerating = forceBV > forceBVBudget;
            } else {
                stopGenerating = generatedEntities.size() >= forceUnitBudget; 
            }
            
            currentLanceWeightString = currentLanceWeightString.substring(1);
        }
        
        // chop out random units until we drop down to our unit count budget
        while(forceUnitBudget > 0 && generatedEntities.size() > forceUnitBudget) {
            generatedEntities.remove(Compute.randomInt(generatedEntities.size()));
        }
        
        BotForce generatedForce = new BotForce();
        generatedForce.setEntityList(generatedEntities);
        setBotForceParameters(generatedForce, forceTemplate, contract);
        scenario.addBotForce(generatedForce, forceTemplate);
        
        return generatedLanceCount;
    }
    
    /**
     * Generates the indicated number of civilian entities.
     *
     * @param num        The number of civilian entities to generate
     * @param campaign  Current campaign
     */
    public static List<Entity> generateCivilianUnits(int num, Campaign campaign) {
        RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits");
        ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(num);
        List<Entity> retval = new ArrayList<>();
        
        List<Entity> entities = msl.stream().map(ms -> createEntityWithCrew("IND",
                RandomSkillsGenerator.L_GREEN, campaign, ms))
                .collect(Collectors.<Entity>toList());
        retval.addAll(entities);
        return retval;
    }
    
    /**
     * Generates the indicated number of turret entities.
     * Lifted from AtBScenario.java
     *
     * @param num        The number of turrets to generate
     * @param skill     The skill level of the turret operators
     * @param quality   The quality level of the turrets
     * @param campaign  The campaign for which the turrets are being generated.
     */
    public static List<Entity> generateTurrets(int num, int skill, int quality, Campaign campaign) {
        int currentYear = campaign.getCalendar().get(Calendar.YEAR);
        List<Entity> retval = new ArrayList<>();
        
        List<MechSummary> msl = campaign.getUnitGenerator().generateTurrets(num, skill, quality, currentYear);
        List<Entity> entities = msl.stream().map(ms -> createEntityWithCrew("IND",
                skill, campaign, ms))
                .collect(Collectors.<Entity>toList());
        retval.addAll(entities);
        return retval;
    }

    /**
     * Takes all the "bot" forces where the template says they should be player-controlled
     * and transforms them into attached units.
     * @param scenario The scenario for which to translate units
     * @param campaign Current campaign
     */
    private static void translatePlayerNPCsToAttached(AtBDynamicScenario scenario, Campaign campaign) {
        for(int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            
            if(forceTemplate != null && forceTemplate.isAlliedPlayerForce()) {
                for(Entity en : botForce.getEntityList()) {
                    scenario.getAlliesPlayer().add(en);
                }
                
                scenario.botForces.remove(botIndex);
                botIndex--;
            }
        }
    }
    
    /**
     * Handles random determination of light conditions for the given scenario, as per AtB rules
     * @param scenario The scenario for which to set lighting conditions.
     */
    private static void setLightConditions(AtBDynamicScenario scenario) {
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
    private static void setWeather(AtBDynamicScenario scenario) {
        int weather = PlanetaryConditions.WE_NONE;
        int wind = PlanetaryConditions.WI_NONE;
        int fog = PlanetaryConditions.FOG_NONE;
        
        // weather is irrelevant in these situations.
        if(scenario.getTerrainType() == AtBScenario.TER_SPACE ||
                scenario.getTerrainType() == AtBScenario.TER_LOW_ATMO) {
            return;
        }

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
     * Handles random determination of terrain and corresponding map file from allowed terrain types
     * @param scenario The scenario to work on.
     */
    public static void setTerrain(AtBDynamicScenario scenario) {
        int terrainIndex = 0;
        
        // if we are allowing all terrain types, then pick one from the list
        // otherwise, pick one from the allowed ones
        if(scenario.getTemplate().mapParameters.mapLocation == ScenarioMapParameters.MapLocation.AllGroundTerrain) {
            terrainIndex = Compute.randomInt(AtBScenario.terrainTypes.length);
            scenario.setTerrainType(terrainIndex);
            scenario.setMapFile();
        } else if (scenario.getTemplate().mapParameters.mapLocation == ScenarioMapParameters.MapLocation.Space) {
            scenario.setTerrainType(AtBScenario.TER_SPACE);
        } else if (scenario.getTemplate().mapParameters.mapLocation == ScenarioMapParameters.MapLocation.LowAtmosphere) {
            // low atmo actually makes use of the terrain, so we generate some here as well
            terrainIndex = Compute.randomInt(AtBScenario.terrainTypes.length);
            scenario.setTerrainType(terrainIndex);
            scenario.setMapFile();
            
            // but then we set the terrain to low atmosphere
            scenario.setTerrainType(AtBScenario.TER_LOW_ATMO);
        } else {
            terrainIndex = Compute.randomInt(scenario.getTemplate().mapParameters.allowedTerrainTypes.size());
            scenario.setTerrainType(scenario.getTemplate().mapParameters.allowedTerrainTypes.get(terrainIndex));
            scenario.setMapFile();
        }
    }
    
    /**
     * Method that handles setting planetary conditions - atmospheric pressure and gravity currently -
     * based on the planet on which the scenario is taking place.
     * @param scenario The scenario to manipulate
     * @param mission The active mission for the scenario 
     * @param campaign The current campaign
     */
    private static void setPlanetaryConditions(AtBDynamicScenario scenario, AtBContract mission, Campaign campaign) {
        if(scenario.getTerrainType() == AtBScenario.TER_SPACE) {
            return;
        }
        
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
    private static void setScenarioMapSize(AtBDynamicScenario scenario) {
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
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, Campaign campaign) {
        return getEntity(faction, skill, quality, unitType, weightClass, false, campaign);
    }
    
    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction        The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill            The RandomSkillGenerator constant that represents the skill level of the overall force.
     * @param quality        The equipment rating of the force.
     * @param unitType        The UnitTableData constant for the type of unit to generate.
     * @param weightClass    The weight class of the unit to generate
     * @param artillery     Whether the unit should be artillery or not. Use with caution, as some unit types simply do not have
     *                      support artillery.                      
     * @param campaign
     * @return                A new Entity with crew.
     */
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, boolean artillery, Campaign campaign) {
        MechSummary ms = null;
        
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.faction = faction;
        params.quality = quality;
        params.unitType = unitType;
        params.weightClass = weightClass;
        params.year = campaign.getCalendar().get(Calendar.YEAR);
                
        if (unitType == UnitType.TANK) {
            return getTankEntity(params, skill, artillery, campaign);
        } else if (unitType == UnitType.INFANTRY) {
            return getInfantryEntity(params, skill, artillery, campaign);
        }
        else {
            ms = campaign.getUnitGenerator().generate(params);
        }

        if (ms == null) {
            return null;
        }
        
        return createEntityWithCrew(faction, skill, campaign, ms);
    }

    /**
     * Generates a tank entity, either artillery or normal.
     * @param params Unit generation parameters.
     * @param skill skill level
     * @param artillery whether or not the unit generated should be artillery
     * @return Entity or null if unable to generate.
     */
    public static Entity getTankEntity(UnitGeneratorParameters params, int skill, boolean artillery, Campaign campaign) {
        MechSummary ms = null;
        
        if(artillery) {
            params.missionRoles.add(MissionRole.ARTILLERY);
        }
        
        if (campaign.getCampaignOptions().getOpforUsesVTOLs()) {
            params.movementModes.addAll(IUnitGenerator.MIXED_TANK_VTOL);
            ms = campaign.getUnitGenerator().generate(params);            
        } else {
            params.filter = v -> !v.getUnitType().equals("VTOL");
            ms = campaign.getUnitGenerator().generate(params);
        }
        
        if (ms == null) {
            return null;
        }
        
        return createEntityWithCrew(params.faction, skill, campaign, ms);
    }
    
    /**
     * Generates an infantry entity, either artillery or normal with a 33% chance of field guns.
     * @param params Unit generation parameters.
     * @param skill skill level
     * @param artillery whether or not the unit generated should be artillery
     * @return Entity or null if unable to generate.
     */
    public static Entity getInfantryEntity(UnitGeneratorParameters params, int skill, boolean artillery, Campaign campaign) {
        // note that the "ARTILLERY" mission role appears mutually exclusive with the "FIELD_GUN" mission role
        if(artillery) {
            params.missionRoles.add(MissionRole.ARTILLERY);
        } else {
            boolean useFieldGuns = Compute.d6() <= 2;
            if(useFieldGuns) {
                params.missionRoles.add(MissionRole.FIELD_GUN);
            }
        }
        
        params.movementModes.addAll(IUnitGenerator.ALL_INFANTRY_MODES);
        
        MechSummary ms = campaign.getUnitGenerator().generate(params);
        
        if (ms == null) {
            return null;
        }
        
        return createEntityWithCrew(params.faction, skill, campaign, ms);
    }
    
    /**
     * @param faction Faction to use for name generation
     * @param skill Skill rating of the crew
     * @param campaign The campaign instance
     * @param ms Which entity to generate
     * @return An crewed entity
     */
    private static Entity createEntityWithCrew(String faction, int skill, Campaign campaign, MechSummary ms) {
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

        if (f.isClan() && Compute.d6(2) > 8 - skill + skills[0] + skills[1]) {
            int phenotype;
            switch (en.getUnitType()) {
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
     * Generates a new Entity without using a RAT. Useful for "persistent" or fixed units.
     *
     * @param name            Full name (chassis + model) of the entity to generate.
     * @param fName            Faction code to use for crew name generation
     * @param skill            RandomSkillsGenerator.L_* constant for the average force skill level.
     * @param campaign
     * @return                A new Entity
     */
    private static Entity getEntityByName(String name, String fName, int skill, Campaign campaign) {
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
    
    /**
     * Generates a list of integers corresponding to megamek unit type constants (defined in Megamek.common.UnitType)
     * TODO: Update AtB mix for clans, marians, wobbies, etc.
     * @param unitTypeCode The type of units to generate, .
     * @param unitCount How many units to generate.
     * @param campaign Current campaign
     * @return Array list of unit type integers.
     */
    private static List<Integer> generateUnitTypes(int unitTypeCode, int unitCount, Campaign campaign) {
        List<Integer> unitTypes = new ArrayList<>(unitCount);
        int actualUnitType = unitTypeCode; 
        
        if(unitTypeCode == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
            // logic mostly lifted from AtBScenario.java, uses campaign config to determine tank/mech mixture
            if (campaign.getCampaignOptions().getUseVehicles()) {
                int totalWeight = campaign.getCampaignOptions().getOpforLanceTypeMechs() +
                        campaign.getCampaignOptions().getOpforLanceTypeMixed() +
                        campaign.getCampaignOptions().getOpforLanceTypeVehicles();
                if (totalWeight <= 0) {
                    actualUnitType = UnitType.MEK;
                } else {
                    int roll = Compute.randomInt(totalWeight);
                    if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles()) {
                        actualUnitType = UnitType.TANK;
                    // if we actually rolled a mixed unit, apply "random" distribution of tank/mech
                    } else if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles() +
                            campaign.getCampaignOptions().getOpforLanceTypeMixed()) {
                        for(int x = 0; x < unitCount; x++) {
                            boolean addTank = Compute.randomInt(2) == 0;
                            if(addTank) {
                                unitTypes.add(UnitType.TANK);
                            } else {
                                unitTypes.add(UnitType.MEK);
                            }
                        }
                        
                        return unitTypes;
                    } else {
                        actualUnitType = UnitType.MEK;
                    }
                }
            }
        }
        
        for(int x = 0; x < unitCount; x++) {
            unitTypes.add(actualUnitType);
        }
        
        return unitTypes;
    }
   
    /**
     * Logic that generates a "unit weights" string according to AtB rules.
     * @param unitTypes List of unit types (mek, tank, etc)
     * @param faction Faction for unit generation
     * @param weightClass "Base" weight class, drives the generated weights with some variation
     * @param maxWeight Maximum weight class
     * @param campaign Current campaign
     * @return Unit weight string.
     */
    private static String generateUnitWeights(List<Integer> unitTypes, String faction, int weightClass, int maxWeight, Campaign campaign) {
        Faction genFaction = Faction.getFaction(faction);
        String factionWeightString = AtBConfiguration.ORG_IS;
        if(genFaction.isClan() || faction == "MH") {
            factionWeightString = AtBConfiguration.ORG_CLAN;
        } else if (genFaction.isComstar()) {
            factionWeightString = AtBConfiguration.ORG_CS;
        }
        
        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(factionWeightString, weightClass), maxWeight);
        
        weights = adjustWeightsForFaction(weights, faction);
        
        return weights;
    }
    
    /**
     * Calculates from scratch the current effective player and allied BV present in the given scenario.
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario resides.
     * @return Effective BV.
     */
    public static int calculateEffectiveBV(AtBDynamicScenario scenario, Campaign campaign) {
        // for each deployed player and bot force that's marked as contributing to the BV budget
        int bvBudget = 0;
        double difficultyMultiplier = getDifficultyMultiplier(campaign);
        
        // deployed player forces:
        for(int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            if(forceTemplate != null && forceTemplate.getContributesToBV()) {
                int forceBVBudget = (int) (campaign.getForce(forceID).getTotalBV(campaign) * difficultyMultiplier);
                bvBudget += forceBVBudget;
            }
        }
        
        // allied bot forces that contribute to BV do not get multiplied by the difficulty
        // even if the player is super good, the AI doesn't get any better
        for(int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if(forceTemplate != null && forceTemplate.getContributesToBV()) {
                bvBudget += botForce.getTotalBV();
            }
        }
        
        return bvBudget;
    }
    
    /**
     * Calculates from scratch the current effective player and allied unit count present in the given scenario.
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario resides.
     * @return Effective BV.
     */
    public static int calculateEffectiveUnitCount(AtBDynamicScenario scenario, Campaign campaign) {
        // for each deployed player and bot force that's marked as contributing to the BV budget
        int unitCount = 0;
        double difficultyMultiplier = getDifficultyMultiplier(campaign);
        
        // deployed player forces:
        for(int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            if(forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
                int forceUnitCount = (int) (campaign.getForce(forceID).getUnits().size() * difficultyMultiplier);
                unitCount += forceUnitCount;
            }
        }
        
        // allied bot forces that contribute to BV do not get multiplied by the difficulty
        // even if the player is super good, the AI doesn't get any better
        for(int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if(forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
                unitCount += botForce.getEntityList().size();
            }
        }
        
        return unitCount;
    }
    
    /**
     * Helper function that calculates the BV budget multiplier based on AtB skill level 
     * @param c 
     * @return
     */
    private static double getDifficultyMultiplier(Campaign c) {
        // skill level is between 0 and 4 inclusive
        // We want a number between .8 and 1.2, so the formula is 1 + ((skill level - 2) / 10)
        return 1.0 + ((c.getCampaignOptions().getSkillLevel() - 2) * .1);
    }
    
    /**
     * Helper function that calculates the "weight class" of the player force.
     * Putting any kind of dropship or other unit that doesn't fit into the "light-medium-heavy-assault" pattern
     * will probably cause it to return "ASSAULT". 
     * @param scenario
     * @param campaign
     * @return
     */
    private static int calculatePlayerForceWeightClass(AtBDynamicScenario scenario, Campaign campaign) {
        double weight = 0.0;
        int unitCount = 0;
        
        for(int forceID : scenario.getForceIDs()) {
            weight += Lance.calculateTotalWeight(campaign, forceID);
            unitCount += campaign.getForce(forceID).getUnits().size();
        } 
        
        int normalizedWeight = (int) (weight / unitCount);
        
        if (normalizedWeight < 20) {
            return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        }
        if (normalizedWeight < 40) {
            return EntityWeightClass.WEIGHT_LIGHT;
        }
        if (normalizedWeight < 60) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }
        if (normalizedWeight < 80) {
            return EntityWeightClass.WEIGHT_HEAVY;
        }
        if (normalizedWeight < 100) {
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
        return EntityWeightClass.WEIGHT_SUPER_HEAVY;
    }
    
    /*private static void generateBasicForceUnit(String faction, int unitType, int weightClass, int year, int skill, int rating) {
        ForceDescriptor fd = new ForceDescriptor();
        fd.setUnitType(unitType);
        fd.setWeightClass(weightClass);
        fd.setYear(year);
        fd.setExperience(skill);
        fd.setEschelon(3); // this is a "lance", "star" or "maniple"
        
        if (!Ruleset.isInitialized()) {
            Ruleset.loadData();
        }
        
        Ruleset ruleSet = Ruleset.findRuleset(faction);
        ruleSet.processRoot(fd, null);
        fd.getAllChildren()
    }*/
    
    /**
     * Generates a "lance" of entities given some parameters, with weight not specified. Doesn't have to be a lance, could be any number.
     * @param faction The faction from which to generate entities.
     * @param skill Skill level of the crew.
     * @param quality Quality of the units.
     * @param unitTypes The types of units. Length had better be equal to the length of weights.
     * @param campaign working campaign.
     * @return Generated entity list.
     */
    private static List<Entity> generateLance(String faction, int skill, int quality, List<Integer> unitTypes, 
            boolean artillery, Campaign campaign) {
        List<Entity> retval = new ArrayList<>();
        
        for(int i = 0; i < unitTypes.size(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes.get(i),
                    UNIT_WEIGHT_UNSPECIFIED, artillery, campaign);
            if(en != null) {
                retval.add(en);
            }
        }
        
        return retval;
    }
    
    /**
     * Generates a "lance" of entities given some parameters. Doesn't have to be a lance, could be any number.
     * @param faction The faction from which to generate entities.
     * @param skill Skill level of the crew.
     * @param quality Quality of the units.
     * @param unitTypes The types of units. Length had better be equal to the length of weights.
     * @param weights Weight class string
     * @param campaign Working campaign
     * @return List of generated entities.
     */
    private static List<Entity> generateLance(String faction, int skill, int quality, List<Integer> unitTypes, 
            String weights, boolean artillery, Campaign campaign) {
        List<Entity> retval = new ArrayList<>();
        
        for(int i = 0; i < unitTypes.size(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes.get(i), 
                    AtBConfiguration.decodeWeightStr(weights, i), artillery, campaign);
            if(en != null) {
                retval.add(en);
            }
        }
        
        return retval;
    }
    
    /**
     * Worker method that sets bot force properties such as name, color, team
     * @param generatedForce The force for which to set parameters
     * @param forceTemplate The force template from which to set parameters
     * @param contract The contract from which to set parameters
     */
    private static void setBotForceParameters(BotForce generatedForce, ScenarioForceTemplate forceTemplate, AtBContract contract) {
        if(forceTemplate.getForceAlignment() == ScenarioForceTemplate.ForceAlignment.Allied.ordinal()) {
            generatedForce.setName(String.format("%s %s", contract.getAllyBotName(), forceTemplate.getForceName()));
            generatedForce.setColorIndex(contract.getAllyColorIndex());
            generatedForce.setCamoCategory(contract.getAllyCamoCategory());
            generatedForce.setCamoFileName(contract.getAllyCamoFileName());
        } else if(forceTemplate.getForceAlignment() == ScenarioForceTemplate.ForceAlignment.Opposing.ordinal()) {
            generatedForce.setName(String.format("%s %s", contract.getEnemyBotName(), forceTemplate.getForceName()));
            generatedForce.setColorIndex(contract.getEnemyColorIndex());
            generatedForce.setCamoCategory(contract.getEnemyCamoCategory());
            generatedForce.setCamoFileName(contract.getEnemyCamoFileName());
        } else {
            generatedForce.setName("Unknown Hostiles");
        }
        
        generatedForce.setTeam(ScenarioForceTemplate.TEAM_IDS.get(forceTemplate.getForceAlignment()));
        setDestinationZone(generatedForce, forceTemplate);
    }
    
    /**
     * Worker method that sets deployment zones for the currently-existing forces in a scenario.
     * Best called after primary player forces have been assigned to the scenario.
     * @param scenario The scenario to process
     */
    private static void setDeploymentZones(AtBDynamicScenario scenario) {
        // loop through all scenario player forces
        //  for each one, look up the template. If none, random? If yes, calculateDeploymentZone
        //  for 
        // repeat for bot forces
        
        for(int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            
            if(forceTemplate != null) {
                calculateDeploymentZone(forceTemplate, scenario, forceTemplate.getForceName());
            }
        }
        
        for(int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            
            if(forceTemplate != null) {
                botForce.setStart(calculateDeploymentZone(forceTemplate, scenario, forceTemplate.getForceName()));
            }
        }
    }
    
    /**
     * Worker method that calculates the deployment zone of a given force template
     * and any force templates with which it is synced.
     * @param forceTemplate The force template for which to generate deployment zone
     * @param scenario The scenario on which we're working
     * @param originalForceTemplateID The ID of the force template where we started.
     * @return Deployment zone as defined in Board.java
     */
    private static int calculateDeploymentZone(ScenarioForceTemplate forceTemplate, AtBDynamicScenario scenario, String originalForceTemplateID) {
        int calculatedEdge = Board.START_ANY;
        
        // if we have a specific deployment zone OR have looped around
        if(forceTemplate.getActualDeploymentZone() != Board.START_NONE) {
            return forceTemplate.getActualDeploymentZone();
        } else if(forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.None ||
                forceTemplate.getSyncedForceName() == originalForceTemplateID) {
            calculatedEdge = forceTemplate.getDeploymentZones().get(Compute.randomInt(forceTemplate.getDeploymentZones().size()));
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.SameEdge) {
            calculatedEdge = calculateDeploymentZone(scenario.getTemplate().scenarioForces.get(forceTemplate.getSyncedForceName()), scenario, originalForceTemplateID);
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.OppositeEdge) {
            int syncDeploymentZone = calculateDeploymentZone(scenario.getTemplate().scenarioForces.get(forceTemplate.getSyncedForceName()), scenario, originalForceTemplateID);
            calculatedEdge = getOppositeEdge(syncDeploymentZone);
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.SameArc) {
            int syncDeploymentZone = calculateDeploymentZone(scenario.getTemplate().scenarioForces.get(forceTemplate.getSyncedForceName()), scenario, originalForceTemplateID);
            List<Integer> arc = getArc(syncDeploymentZone, true);
            calculatedEdge = arc.get(Compute.randomInt(arc.size()));
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.OppositeArc) {
            int syncDeploymentZone = calculateDeploymentZone(scenario.getTemplate().scenarioForces.get(forceTemplate.getSyncedForceName()), scenario, originalForceTemplateID);
            List<Integer> arc = getArc(syncDeploymentZone, false);
            calculatedEdge = arc.get(Compute.randomInt(arc.size()));
        }
        
        if(calculatedEdge == ScenarioForceTemplate.DEPLOYMENT_ZONE_NARROW_EDGE) {
            List<Integer> edges = new ArrayList<>();
            
            if(scenario.getMapSizeX() > scenario.getMapSizeY()) {
                edges.add(Board.START_E);
                edges.add(Board.START_W);
            } else {
                edges.add(Board.START_N);
                edges.add(Board.START_S);
            }
            
            calculatedEdge = edges.get(Compute.randomInt(2));
        }
        
        forceTemplate.setActualDeploymentZone(calculatedEdge);
        return calculatedEdge;
    }
    
    /**
     * Determines and sets the destination edge for a given bot force that follows a given force template. 
     * @param force The bot force for which to set the edge.
     * @param forceTemplate The template which governs the destination edge.
     */
    private static void setDestinationZone(BotForce force, ScenarioForceTemplate forceTemplate) {
        int actualDestinationEdge = forceTemplate.getDestinationZone();
        
        // set the 'auto flee' flag to true if the bot has a destination edge
        if(actualDestinationEdge != CardinalEdge.NEAREST_OR_NONE.ordinal()) {
            force.getBehaviorSettings().setAutoFlee(true);
        }
        
        if(forceTemplate.getDestinationZone() == ScenarioForceTemplate.DESTINATION_EDGE_RANDOM) {
            // compute a random cardinal edge between 0 and 3 to avoid None
            actualDestinationEdge = Compute.randomInt(CardinalEdge.values().length - 1);
        } else if (forceTemplate.getDestinationZone() == ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT) {
            actualDestinationEdge = getOppositeEdge(force.getStart());
        } else {
            force.getBehaviorSettings().setDestinationEdge(CardinalEdge.getCardinalEdge(actualDestinationEdge));
            return;
        }
        
        force.setDestinationEdge(actualDestinationEdge);
    }
    
    /**
     * Sets up the deployment turns for bot units as they are specified
     * @param scenario
     */
    private static void setDeploymentTurns(AtBDynamicScenario scenario, Campaign campaign) {
        for(int x = 0; x < scenario.getNumBots(); x++) {
            BotForce currentBotForce = scenario.getBotForce(x);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(currentBotForce);
            int deployRound = forceTemplate.getArrivalTurn();
            
            if(deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED) {
                setDeploymentTurnsStaggered(currentBotForce.getEntityList(), 0);
            } else if(deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
                setDeploymentTurnsStaggeredByLance(currentBotForce.getEntityList());
            } else {                
                for(Entity entity : currentBotForce.getEntityList()) {
                    entity.setDeployRound(deployRound);
                }
            }
        }
    }
    
    /**
     * Set up deployment turns for player units as specified in the scenario's template.
     * Note that this is currently invoked during the BriefingTab.startScenario() method,
     * as that method resets all properties of for each player entity. Hence it being public.
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario is occurring.
     */
    public static void setPlayerDeploymentTurns(AtBDynamicScenario scenario, Campaign campaign) {
        // for player forces where there's an associated force template, we can set the deployment turn explicitly
        // or use a stagger algorithm.
        // for player forces where there's not an associated force template, we calculate the deployment turn
        // as if they were reinforcements
        for(int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            List<Entity> forceEntities = new ArrayList<>();            
            Force playerForce = campaign.getForce(forceID);
            
            for(UUID unitID : playerForce.getAllUnits()) {
                Unit currentUnit = campaign.getUnit(unitID);
                if(currentUnit != null) {
                    forceEntities.add(currentUnit.getEntity());
                }
            }
            
            // make note of battle commander strategy
            int strategy = scenario.getLanceCommanderSkill(SkillType.S_STRATEGY, campaign);
            
            // now, attempt to set deployment turns
            // if the force has a template, then use the appropriate algorithm
            // otherwise, treat it as reinforcements
            if(forceTemplate != null) {
                int deployRound = forceTemplate.getArrivalTurn();
                
                if(deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED) {
                    setDeploymentTurnsStaggered(forceEntities, strategy);
                } else if(deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
                    setDeploymentTurnsStaggeredByLance(forceEntities);
                } else {                
                    for(Entity entity : forceEntities) {
                        entity.setDeployRound(deployRound);
                    }
                }
            } else {
                int minWalkMP = 999;
                
                // calculate the slowest walk speed
                for(Entity entity : forceEntities) {                    
                    int speed = calculateAtBSpeed(entity);
                    
                    if(speed < minWalkMP) {
                        minWalkMP = speed;
                    }
                }

                // can't have a deployment round earlier than 0
                int deployRound = Math.max(0, 12 - minWalkMP - strategy);
                
                // set deployment round
                for(Entity entity : forceEntities) {
                    entity.setDeployRound(deployRound);
                }
            }
        }
    }
    
    /**
     * Uses the "individual staggered deployment" algorithm to determine individual deployment turns 
     * @param botForce The bot force whose entities to process.
     * @param turnModifier The deployment round is reduced by this amount
     */
    private static void setDeploymentTurnsStaggered(List<Entity> entityList, int turnModifier) {
        // loop through all the entities
        // highest movement entity deploys on turn 0
        // other entities deploy on highest move - "walk" MP.
        int maxWalkMP = -1;
        List<Integer> entityWalkMPs = new ArrayList<>();
        
        for(Entity entity : entityList) {
            // AtB has a legacy mechanism where units with jump jets are counted a little faster
            // for arrival times. We calculate it once and store it.
            int speed = calculateAtBSpeed(entity);
            
            entityWalkMPs.add(speed);
            if(speed > maxWalkMP) {
                maxWalkMP = speed;
            }
        }
        
        for(int x = 0; x < entityList.size(); x++) {
            // since we're iterating through the same unchanged collection, we can use implicit indexing.
            entityList.get(x).setDeployRound(Math.max(0, maxWalkMP - entityWalkMPs.get(x) - turnModifier));
        }
    }
    
    /**
     * Uses the "lance staggered deployment" algorithm to determine individual deployment turns 
     * @param botForce The bot force whose entities to process.
     */
    private static void setDeploymentTurnsStaggeredByLance(List<Entity> entityList) {
        // loop through all the entities
        // for every four entities, determine the lowest walk MP.
        // 
        // then apply the individual staggered deployment algorithm to each lance
        // other entities deploy on highest move - "walk" MP.
        
        // TODO: Stick a pin in this for now, the algorithm is annoyingly complicated
        
        /*int maxWalkMP = -1;
        int lanceMinWalkMP = 999;
        List<Integer> lanceWalkMPs = new ArrayList<>(); 
        
        for(int x = 0; x < botForce.getEntityList().size(); x++) {
            Entity entity = botForce.getEntityList().get(x);
            int speed = calculateAtBSpeed(entity);
            int lanceIndex = x / 4;
            
            if()
        }
        
        for(Entity entity : botForce.getEntityList()) {
            entity.setDeployRound(maxWalkMP - entity.getWalkMP());
        }*/
    }

    /** 
     * Worker function that calculates the AtB-rules walk MP for an entity, for deployment purposes.
     * @param entity The entity to examine.
     * @return The walk MP.
     */
    private static int calculateAtBSpeed(Entity entity) {
        int speed = entity.getWalkMP();
        if (entity.getJumpMP() > 0) {
            if (entity instanceof megamek.common.Infantry) {
                speed = entity.getJumpMP();
            } else {
                speed++;
            }
        }
        
        return speed;
    }
    
    /**
     * Method to compute an "arc" of deployment zones next to or opposite a particular edge.
     * e.g. Northeast comes back with a list of north, northeast, east
     * @param edge The edge to process
     * @param same Whether the arc is on the same side or the opposite side.
     * @return Three edges that form the arc, as defined in Board.java
     */
    private static List<Integer> getArc(int edge, boolean same) {
        ArrayList<Integer> edges = new ArrayList<>();
        
        int tempEdge = edge;
        if(!same) {
            tempEdge = getOppositeEdge(edge);
        }
        
        switch(tempEdge) {
        case Board.START_EDGE:
            edges.add(Board.START_EDGE);
            break;
        case Board.START_CENTER:
            edges.add(Board.START_CENTER);
            break;
        case Board.START_ANY:
            edges.add(Board.START_ANY);
            break;
        default:
            // directional edges start at 1
            edges.add(((tempEdge + 6) % 8) + 1);
            edges.add(((tempEdge - 1) % 8) + 1);
            edges.add((tempEdge % 8) + 1);
            break;
        }
        
        return edges;
    }
    
    /**
     * Computes the "opposite" edge of a given board start edge.
     * @param edge The starting edge
     * @return Opposite edge, as defined in Board.java
     */
    private static int getOppositeEdge(int edge) {
        switch(edge) {
        case Board.START_EDGE:
            return Board.START_CENTER;
        case Board.START_CENTER:
            return Board.START_EDGE;
        case Board.START_ANY:
            return Board.START_ANY;
        default:
            // directional edges start at 1
            return ((edge + 3) % 8) + 1;
        }
    }
    
    /**
     * Worker function that calculates the appropriate number of rerolls to use for the scenario.
     * @param scenario The scenario for which to set rerolls
     * @param campaign Campaign in which the scenario is occurring
     */
    private static void setScenarioRerolls(AtBDynamicScenario scenario, Campaign campaign) {
        int tacticsSkill = scenario.getLanceCommanderSkill(SkillType.S_TACTICS, campaign);

        scenario.setRerolls(tacticsSkill);
    }
    
    /**
     * Convenience function to get the "lance" (basic unit) size, based on faction.
     * @param factionCode The faction code.
     * @return "Lance" size.
     */
    public static int getLanceSize(String factionCode) {
        Faction faction = Faction.getFaction(factionCode);
        if(faction != null) {
            // clans and marian hegemony use a fundamental unit size of 5.
            if(faction.isClan() ||
                    factionCode == "MH") {
                return CLAN_MH_LANCE_SIZE;
            // comstar and wobbies use a fundamental unit size of 6.
            } else if(faction.isComstar()) {
                return COMSTAR_LANCE_SIZE;
            }
        }
        
        return IS_LANCE_SIZE;
    }
    
    /**
     * Worker function to determine the "lance size" of a group of aircraft.
     * Either 2 for ASF
     * @param isPlanetOwner
     * @return
     */
    public static int getAeroLanceSize(int unitTypeCode, boolean isPlanetOwner) {
        if(unitTypeCode == UnitType.AERO) {
            return 2;
        } else if(unitTypeCode == UnitType.CONV_FIGHTER) {
            return (Compute.randomInt(3) + 1) * 2;
        } else {
            int useASFRoll = isPlanetOwner ? Compute.d6() : 6;
            int weightCountRoll = (Compute.randomInt(3) + 1 * 2); // # of conventional fighters, just in case
            
            // if we are the planet owner, we may use ASF or conventional fighters 
            boolean useASF = useASFRoll >= 4;
            // if we are using ASF, we "always" use 2 at a time, otherwise, use the # of conventional fighters 
            int unitCount = useASF ? 2 : weightCountRoll;
            
            return unitCount;
        }
    }
    
    /**
     * Helper function that deploys the given units off board a random distance 1-2 boards in a random direction
     * @param entityList
     */
    private static void deployArtilleryOffBoard(List<Entity> entityList) {
        OffBoardDirection direction = OffBoardDirection.getDirection(Compute.randomInt(4) + 1);
        int distance = Compute.randomInt(2) + 1;
        
        for(Entity entity : entityList) {
            entity.setOffBoard(distance, direction);
        }
    }
    
    /**
     * Helper function that puts the units in the given list at the given altitude, and
     * VTOLs at the elevation. Use with caution, as may lead to splattering.
     * @param entityList The entity list to process.
     * @param startingAltitude Starting altitude.
     */
    private static void setStartingAltitude(List<Entity> entityList, int startingAltitude) {
        for(Entity entity : entityList) {
            if(entity.hasETypeFlag(Entity.ETYPE_VTOL)) {
                entity.setElevation(startingAltitude);
            } else {
                entity.setAltitude(startingAltitude);
            }
        }
    }
    
    /**
     * Helper function that makes some of the units in the given list of entities
     * carry bombs.
     * @param entityList The list of entities to process
     * @param campaign Campaign object. In the future, may be used to check list of bombs
     * for technological availability.
     */
    private static void populateAeroBombs(List<Entity> entityList, Campaign campaign) {
        int maxBombers = Compute.randomInt(entityList.size()) + 1;
        int numBombers = 0;
        
        for(Entity entity : entityList) {
            if(numBombers >= maxBombers) {
                break;
            }
            
            if(entity.isBomber()) {
                int[] bombChoices = new int[BombType.B_NUM];
                int numBombs = (int) (entity.getWeight() / 5);
                int bombIndex = validBotBombs[(Compute.randomInt(validBotBombs.length))];
                bombChoices[bombIndex] = numBombs;
                
                ((megamek.common.IBomber) entity).setBombChoices(bombChoices);
                numBombers++;
            }
        }
    }
}
