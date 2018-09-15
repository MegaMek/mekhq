package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomSkillsGenerator;
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
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.personnel.Bloodname;
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
        // fix the player force BV at the current time.
        int playerForceBV = calculateEffectiveBV(scenario, campaign);
        int playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign);
        int playerForceWeightClass = calculatePlayerForceWeightClass(scenario, campaign);
        
        // at this point, only the player forces are present and contributing to BV/unit count
        int generatedLanceCount = generateForces(scenario, contract, campaign,
                        playerForceBV, playerForceUnitCount, playerForceWeightClass, ForceAlignment.Player);
        
        // now, we recalculate the BV/unit count to account for the attached allied units, 
        // then generate the bot-controlled allies
        playerForceBV = calculateEffectiveBV(scenario, campaign);
        playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign);
        generatedLanceCount += generateForces(scenario, contract, campaign,
                playerForceBV, playerForceUnitCount, playerForceWeightClass, ForceAlignment.Allied);
        
        // now, we recalculate the BV/unit count to account for the bot-controlled allies 
        // then generate the hostiles
        playerForceBV = calculateEffectiveBV(scenario, campaign);
        playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign);
        generatedLanceCount += generateForces(scenario, contract, campaign,
                playerForceBV, playerForceUnitCount, playerForceWeightClass, ForceAlignment.Opposing);
                
        // approximate estimate, anyway.
        scenario.setLanceCount(generatedLanceCount + (playerForceUnitCount / 4));
        setScenarioMapSize(scenario);
        setDeploymentZones(scenario);
    }
    
    public static int generateForces(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign,
            int effectiveBV, int effectiveUnitCount, int weightClass, ForceAlignment alignment) { 
        
        int generatedLanceCount = 0;
        // these are some properties that need to be determined prior to generating the force
        List<ScenarioForceTemplate> forceTemplates = new ArrayList<>();
        String factionCode = "";
        int skill = 0;
        int quality = 0;
        
        switch(alignment) {
        case Allied:
            forceTemplates = scenario.getTemplate().getAllBotControlledAllies();
            factionCode = contract.getEmployerCode();
            skill = contract.getAllySkill();
            quality = contract.getAllyQuality();
            break;
        case Player:
            forceTemplates = scenario.getTemplate().getAllPlayerControlledAllies();
            factionCode = contract.getEmployerCode();
            skill = contract.getAllySkill();
            quality = contract.getAllyQuality();
            break;
        case Opposing:
        case Third:
            forceTemplates = scenario.getTemplate().getAllPlayerControlledHostiles();
            factionCode = contract.getEnemyCode();
            skill = contract.getEnemySkill();
            quality = contract.getEnemyQuality();
            break;
        }
        
        for(ScenarioForceTemplate forceTemplate : forceTemplates) {
            //  While force has not surpassed BV cap || unit cap
            //      get me a unit types array
            //      get me a unit weight string
            //      use unit weight string to generate a list of entities
            //  Step 2.1 If force has surpassed unit cap, remove randomly selected units until it's at unit cap
            
            // don't generate forces flagged as player-supplied
            if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerDeployed.ordinal()) {
                continue;
            }
        
            // determine generation parameters
            int forceBV = 0;
            int forceBVBudget = (int) (effectiveBV * forceTemplate.getForceMultiplier());
            int forceUnitBudget = 0;
            if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.UnitCountScaled.ordinal()) {
                forceUnitBudget = (int) (effectiveUnitCount * forceTemplate.getForceMultiplier());
            } else if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.FixedUnitCount.ordinal()) {
                forceUnitBudget = (int) forceTemplate.getForceMultiplier(); // this should really be another field
            }
            
            ArrayList<Entity> generatedEntities = new ArrayList<>();
            
            boolean stopGenerating = false;
            
            while(!stopGenerating) {
                List<Integer> unitTypes = generateUnitTypes(forceTemplate, 4, campaign);
                String unitWeights = generateUnitWeights(unitTypes, factionCode, 
                        weightClass, EntityWeightClass.WEIGHT_ASSAULT, campaign);
     
                List<Entity> generatedLance = generateLance(factionCode, skill, 
                        quality, unitTypes, unitWeights, campaign);
                
                if(generatedLance.isEmpty()) {
                    stopGenerating = true;
                    continue;
                }
                
                // if force contributes to map size
                generatedLanceCount++;
                
                for(Entity ent : generatedLance) {
                    forceBV += ent.calculateBattleValue();
                    generatedEntities.add(ent);
                }
                
                if(forceTemplate.getGenerationMethod() == ForceGenerationMethod.BVScaled.ordinal()) {
                    stopGenerating = forceBV > forceBVBudget;
                } else {
                    stopGenerating = generatedEntities.size() >= forceUnitBudget; 
                }
            }
            
            // chop out random units until we drop down to our unit count budget
            while(forceUnitBudget > 0 && generatedEntities.size() > forceUnitBudget) {
                generatedEntities.remove(Compute.randomInt(generatedEntities.size()));
            }
            
            BotForce generatedForce = new BotForce();
            generatedForce.setEntityList(generatedEntities);
            setBotForceParameters(generatedForce, forceTemplate, contract);
            scenario.addBotForce(generatedForce, forceTemplate);
        }
        
        return generatedLanceCount;
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
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, Campaign campaign) {
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
        
        return createEntityWithCrew(faction, skill, campaign, ms);
    }

    /**
     * @param faction Faction to use for name generation
     * @param skill Skill rating of the crew
     * @param campaign The campaign instance
     * @param ms Which entity to generate
     * @return An crewed entity
     */
    public static Entity createEntityWithCrew(String faction, int skill, Campaign campaign, MechSummary ms) {
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
    
    private static List<Integer> generateUnitTypes(ScenarioForceTemplate template, int unitCount, Campaign campaign) {
        List<Integer> unitTypes = new ArrayList<>(unitCount);
        int actualUnitType = template.getAllowedUnitType(); 
        
        if(template.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
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
                    }
                }
            }
        }
        
        for(int x = 0; x < unitCount; x++) {
            unitTypes.add(actualUnitType);
        }
        
        return unitTypes;
    }
    
    private static String generateUnitWeights(List<Integer> unitTypes, String faction, int weightClass, int maxWeight, Campaign campaign) {
        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_IS, weightClass), maxWeight);
        
        weights = adjustWeightsForFaction(weights, faction);
        
        return weights;
    }
    
    /**
     * Calculates from scratch the current effective player and allied BV present in the given scenario.
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario resides.
     * @return Effective BV.
     */
    private static int calculateEffectiveBV(AtBDynamicScenario scenario, Campaign campaign) {
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
    private static int calculateEffectiveUnitCount(AtBDynamicScenario scenario, Campaign campaign) {
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
    
    /**
     * Generates a "lance" of entities given some parameters. Doesn't have to be a lance, could be 
     * @param faction The faction from which to generate entities.
     * @param skill Skill level of the crew.
     * @param quality Quality of the units.
     * @param unitTypes The types of units. Length had better be equal to the length of weights.
     * @param weights 
     * @param campaign
     * @return
     */
    private static List<Entity> generateLance(String faction, int skill, int quality, List<Integer> unitTypes, String weights, Campaign campaign) {
        List<Entity> retval = new ArrayList<>();
        
        for(int i = 0; i < unitTypes.size(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes.get(i), AtBConfiguration.decodeWeightStr(weights, i), campaign);
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
            generatedForce.setName(contract.getAllyBotName());
            generatedForce.setColorIndex(contract.getAllyColorIndex());
            generatedForce.setCamoCategory(contract.getAllyCamoCategory());
            generatedForce.setCamoFileName(contract.getAllyCamoFileName());
        } else if(forceTemplate.getForceAlignment() == ScenarioForceTemplate.ForceAlignment.Opposing.ordinal()) {
            generatedForce.setName(contract.getEnemyBotName());
            generatedForce.setColorIndex(contract.getEnemyColorIndex());
            generatedForce.setCamoCategory(contract.getEnemyCamoCategory());
            generatedForce.setCamoFileName(contract.getEnemyCamoFileName());
        } else {
            generatedForce.setName("Unknown Hostiles");
        }
        
        generatedForce.setTeam(ScenarioForceTemplate.TEAM_IDS.get(forceTemplate.getForceAlignment()));
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
                edges.add(Board.START_N);
                edges.add(Board.START_S);
            } else {
                edges.add(Board.START_E);
                edges.add(Board.START_W);
            }
            
            calculatedEdge = edges.get(Compute.randomInt(2));
        }
        
        forceTemplate.setActualDeploymentZone(calculatedEdge);
        return calculatedEdge;
    }
    
    /**
     * Method to compute an "arc" of deployment zones next to or opposite a particular edge.
     * e.g. Northeast comes back with a list of north, northeast, east
     * @param edge The edge to process
     * @param same Whether the arc is on the same side or the opposite side.
     * @return Three edges that form the arc, as defined in Board.java
     */
    public static List<Integer> getArc(int edge, boolean same) {
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
    public static int getOppositeEdge(int edge) {
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
}
