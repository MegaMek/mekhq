/*
 * Copyright (c) 2019 - The Megamek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.IPlayer;
import megamek.common.MechSummaryCache;
import megamek.common.enums.Gender;
import megamek.common.util.StringUtil;

import megamek.client.Client;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomSkillsGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ratgenerator.MissionRole;
import megamek.common.Board;
import megamek.common.BombType;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.IBomber;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.OffBoardDirection;
import megamek.common.PlanetaryConditions;
import megamek.common.Transporter;
import megamek.common.TroopSpace;
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
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.UnitGeneratorParameters;

/**
 * This class handles the creation and substantive manipulation of AtBDynamicScenarios
 * @author NickAragua
 */
public class AtBDynamicScenarioFactory {
    /**
     * Unspecified weight class for units, used when the unit type doesn't support weight classes
     */
    public static final int UNIT_WEIGHT_UNSPECIFIED = -1;

    private static final int[] validBotBombs = { BombType.B_HE, BombType.B_CLUSTER, BombType.B_RL, BombType.B_INFERNO, BombType.B_THUNDER, BombType.B_FAE_SMALL, BombType.B_FAE_LARGE };
    private static final int[] validBotAABombs = { BombType.B_RL };

    private static final int[] minimumBVPercentage = { 50, 60, 70, 80, 90, 100 };
    // target number for 2d6 roll of infantry being upgraded to battle armor, indexed by dragoons rating
    private static final int[] infantryToBAUpgradeTNs = { 12, 10, 8, 6, 4, 2 };

    private static final int IS_LANCE_SIZE = 4;
    private static final int CLAN_MH_LANCE_SIZE = 5;
    private static final int COMSTAR_LANCE_SIZE = 6;

    private static final int REINFORCEMENT_ARRIVAL_SCALE = 30;

    /**
     * Method that sets some initial scenario parameters from the given template, prior to force generation and such.
     *
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
        scenario.setMissionId(contract.getId());

        // apply any fixed modifiers
        for (String modifierName : template.scenarioModifiers) {
            if (AtBScenarioModifier.getScenarioModifiers().containsKey(modifierName)) {
                scenario.getScenarioModifiers().add(AtBScenarioModifier.getScenarioModifiers().get(modifierName));
            }
        }

        boolean planetsideScenario = template.mapParameters.getMapLocation() == MapLocation.AllGroundTerrain ||
                template.mapParameters.getMapLocation() == MapLocation.SpecificGroundTerrain;

        // set lighting conditions if the user wants to play with them and is on a ground map
        // theoretically some lighting conditions apply to space maps as well, but requires additional work to implement properly
        if (campaign.getCampaignOptions().getUseLightConditions() && planetsideScenario) {
            setLightConditions(scenario);
        }

        // set weather conditions if the user wants to play with them and is on a ground map
        if (campaign.getCampaignOptions().getUseWeatherConditions() && planetsideScenario) {
            setWeather(scenario);
        }

        if (campaign.getCampaignOptions().getUsePlanetaryConditions() && planetsideScenario) {
            setPlanetaryConditions(scenario, contract, campaign);
        }

        setTerrain(scenario);

        applyScenarioModifiers(scenario, campaign, EventTiming.PreForceGeneration);

        // apply a default "reinforcements" force template if a scenario-specific one does not already exist
        if (!template.scenarioForces.containsKey(ScenarioForceTemplate.REINFORCEMENT_TEMPLATE_ID)) {
            ScenarioForceTemplate defaultReinforcements = ScenarioForceTemplate.getDefaultReinforcementsTemplate();
            template.scenarioForces.put(defaultReinforcements.getForceName(), defaultReinforcements);
        }

        return scenario;
    }

    /**
     * Method that should be called when all "required" player forces have been assigned to a scenario.
     * It will generate all primary allied-player, allied-bot and enemy forces,
     * as well as rolling and applying scenario modifiers.
     *
     * @param scenario Scenario to finalize
     * @param contract Contract in which the scenario is occurring
     * @param campaign Current campaign.
     */
    public static void finalizeScenario(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign) {
        // just in case, clear old bot forces.
        for (int x = scenario.getNumBots() - 1; x >= 0; x--) {
            scenario.removeBotForce(x);
        }

        // Now we can clear the other related lists
        scenario.getAlliesPlayer().clear();
        scenario.getExternalIDLookup().clear();
        scenario.getBotUnitTemplates().clear();

        // fix the player force weight class and unit count at the current time.
        int playerForceWeightClass = calculatePlayerForceWeightClass(scenario, campaign);
        int playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign);

        // at this point, only the player forces are present and contributing to BV/unit count
        int generatedLanceCount = generateForces(scenario, contract, campaign, playerForceWeightClass);

        // approximate estimate, anyway.
        scenario.setLanceCount(generatedLanceCount + (playerForceUnitCount / 4));
        setScenarioMapSize(scenario);
        setDeploymentZones(scenario);
        setDestinationZones(scenario);

        applyScenarioModifiers(scenario, campaign, EventTiming.PostForceGeneration);

        setScenarioRerolls(scenario, campaign);

        setDeploymentTurns(scenario);
        translatePlayerNPCsToAttached(scenario, campaign);
        translateTemplateObjectives(scenario, campaign);
        scaleObjectiveTimeLimits(scenario, campaign);

        if (campaign.getCampaignOptions().useAbilities()) {
            upgradeBotCrews(scenario);
        }
    }

    /**
     * "Meaty" function that generates a set of forces for the given scenario of the given force alignment.
     *
     * @param scenario    Scenario for which we're generating forces
     * @param contract    The contract on which we're currently working. Used for skill/quality/planetary info parameters
     * @param campaign    The current campaign
     * @param weightClass The maximum weight class of the units to generate (ignored )
     * @return How many "lances" or other individual units were generated.
     */
    private static int generateForces(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign, int weightClass) {

        int generatedLanceCount = 0;
        List<ScenarioForceTemplate> forceTemplates = scenario.getTemplate().getAllScenarioForces();

        // organize the forces by bucket.
        Map<Integer, List<ScenarioForceTemplate>> orderedForceTemplates = new HashMap<>();
        List<Integer> generationOrders = new ArrayList<>();

        for (ScenarioForceTemplate forceTemplate : forceTemplates) {
            if (!orderedForceTemplates.containsKey(forceTemplate.getGenerationOrder())) {
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
        for (int generationOrder : generationOrders) {
            List<ScenarioForceTemplate> currentForceTemplates = orderedForceTemplates.get(generationOrder);
            effectiveBV = calculateEffectiveBV(scenario, campaign);
            effectiveUnitCount = calculateEffectiveUnitCount(scenario, campaign);

            for (ScenarioForceTemplate forceTemplate : currentForceTemplates) {
                generatedLanceCount += generateForce(scenario, contract, campaign,
                        effectiveBV, effectiveUnitCount, weightClass, forceTemplate);
            }
        }

        return generatedLanceCount;
    }

    /**
     * "Meaty" function that generates a set of forces for the given scenario of the given force alignment.
     *
     * @param scenario           Scenario for which we're generating forces
     * @param contract           The contract on which we're currently working. Used for skill/quality/planetary info parameters
     * @param campaign           The current campaign
     * @param effectiveBV        The effective battle value, up to this point, of player and allied units
     * @param effectiveUnitCount The effective unit count, up to this point, of player and allied units
     * @param weightClass        The maximum weight class of the units to generate (ignored )
     * @param forceTemplate      The force template to use to generate the force
     * @return How many "lances" or other individual units were generated.
     */
    public static int generateForce(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign,
                                    int effectiveBV, int effectiveUnitCount, int weightClass, ScenarioForceTemplate forceTemplate) {
        // don't generate forces flagged as player-supplied
        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) {
            return 0;
        }

        String factionCode = "";
        int skill = 0;
        int quality = 0;
        int generatedLanceCount = 0;
        LocalDate currentDate = campaign.getLocalDate();
        ForceAlignment forceAlignment = ForceAlignment.getForceAlignment(forceTemplate.getForceAlignment());

        // planet owner logic requires some special handling
        if (forceAlignment == ForceAlignment.PlanetOwner) {
            factionCode = getPlanetOwnerFaction(contract, currentDate);
            forceAlignment = getPlanetOwnerAlignment(contract, factionCode, currentDate);
            // updates the force alignment for the template for later examination
            forceTemplate.setForceAlignment(forceAlignment.ordinal());
        }

        switch (forceAlignment) {
            case Allied:
            case Player:
                factionCode = contract.getEmployerCode();
                skill = contract.getAllySkill();
                quality = contract.getAllyQuality();
                break;
            case Opposing:
                factionCode = contract.getEnemyCode();
            // intentional fall-through: "third" parties have already had their faction code set.
            case Third:
                skill = scenario.getEffectiveOpforSkill();
                quality = scenario.getEffectiveOpforQuality();
                break;
            default:
                MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, "generateForce", LogLevel.WARNING,
                        String.format("Invalid force alignment %d", forceTemplate.getForceAlignment()));
        }

        String parentFactionType = AtBConfiguration.getParentFactionType(factionCode);
        boolean isPlanetOwner = contract.getSystem().getFactions(currentDate).contains(factionCode);
        boolean usingAerospace = forceTemplate.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX ||
                forceTemplate.getAllowedUnitType() == UnitType.CONV_FIGHTER ||
                forceTemplate.getAllowedUnitType() == UnitType.AERO;

        // here we determine the "lance size". Aircraft almost always come in pairs, mechs and tanks, not so much.
        int lanceSize = usingAerospace ? getAeroLanceSize(forceTemplate.getAllowedUnitType(), isPlanetOwner, factionCode) :
                getLanceSize(factionCode);

        // determine generation parameters
        int forceBV = 0;
        int forceBVBudget = (int) (effectiveBV * forceTemplate.getForceMultiplier());
        int forceUnitBudget = 0;
        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.UnitCountScaled.ordinal()) {
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
        while (!stopGenerating) {
            List<Entity> generatedLance;

            // atb generates between 1 and 3 lances at a time
            // so we generate a new batch each time we run out
            if (currentLanceWeightString.isEmpty()) {
                currentLanceWeightString = campaign.getAtBConfig().selectBotLances(parentFactionType, weightClass);
            }

            // if we are using the 'atb aero mix', let's decide now whether it's aero or conventional fighter
            // if we are in space, let's not put conventional fighters there
            int actualUnitType = forceTemplate.getAllowedUnitType();
            if (isPlanetOwner && actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX &&
                    scenario.getTemplate().mapParameters.getMapLocation() != MapLocation.Space) {
                actualUnitType = Compute.d6() > 3 ? UnitType.AERO : UnitType.CONV_FIGHTER;
            } else if (actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
                actualUnitType = UnitType.AERO;
            }

            // some special cases that don't fit into the regular RAT generation mechanism
            // gun emplacements use a separate set of rats
            if (actualUnitType == UnitType.GUN_EMPLACEMENT) {
                generatedLance = generateTurrets(4, skill, quality, campaign);
            // atb civilians use a separate rat
            } else if (actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS) {
                generatedLance = generateCivilianUnits(4, campaign);
            // meks, asf and tanks support weight class specification, as does the "standard atb mix"
            } else if (IUnitGenerator.unitTypeSupportsWeightClass(actualUnitType) ||
                    (actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX)) {
                List<Integer> unitTypes = generateUnitTypes(actualUnitType, lanceSize, quality, factionCode, campaign);

                // special case: if we're generating artillery, there's not a lot of variety
                // in artillery unit weight classes, so we ignore that specification
                if (!forceTemplate.getUseArtillery()) {
                    String unitWeights = generateUnitWeights(unitTypes, factionCode,
                            AtBConfiguration.decodeWeightStr(currentLanceWeightString, 0),
                            forceTemplate.getMaxWeightClass(), forceTemplate.getMinWeightClass(), campaign);

                    generatedLance = generateLance(factionCode, skill,
                            quality, unitTypes, unitWeights, false, campaign);
                } else {
                    generatedLance = generateLance(factionCode, skill,
                            quality, unitTypes, true, campaign);
                }
            // everything else doesn't support weight class specification
            } else {
                List<Integer> unitTypes = generateUnitTypes(actualUnitType, lanceSize, quality, factionCode, campaign);
                generatedLance = generateLance(factionCode, skill, quality, unitTypes, forceTemplate.getUseArtillery(), campaign);
            }

            // no reason to go into an endless loop if we can't generate a lance
            if (generatedLance.isEmpty()) {
                stopGenerating = true;
                MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, "generateForces", LogLevel.WARNING,
                        String.format("Unable to generate units from RAT: %s, type %d, max weight %d",
                                factionCode, forceTemplate.getAllowedUnitType(), weightClass));
                continue;
            }

            if (forceTemplate.getAllowAeroBombs()) {
                boolean isAeroMap = scenario.getTemplate().mapParameters.getMapLocation() == MapLocation.AllGroundTerrain;
                populateAeroBombs(generatedLance, campaign, !isAeroMap);
            }

            if (forceTemplate.getUseArtillery() && forceTemplate.getDeployOffboard()) {
                deployArtilleryOffBoard(generatedLance);
            }

            setStartingAltitude(generatedLance, forceTemplate.getStartingAltitude());

            // if force contributes to map size, increment the generated "lance" count
            if (forceTemplate.getContributesToMapSize()) {
                generatedLanceCount++;
            }

            // if appropriate, generate an extra BA unit for clan novas
            generatedLance.addAll(generateBAForNova(scenario, generatedLance, factionCode, skill, quality, campaign));

            for (Entity ent : generatedLance) {
                forceBV += ent.calculateBattleValue();
                generatedEntities.add(ent);
            }

            // terminate force generation if we've gone over our unit count or bv budget
            if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.BVScaled.ordinal()) {
                // for bv-scaled forces, we check whether to stop generating after every lance
                // the target number is the percentage of the bv budget generated so far
                // if we roll below it, we stop
                int roll = Compute.randomInt(100);
                double rollTarget = ((double) forceBV / forceBVBudget) * 100;
                stopGenerating = rollTarget > minimumBVPercentage[campaign.getUnitRating().getUnitRatingAsInteger()]
                        && roll < rollTarget;
            } else {
                stopGenerating = generatedEntities.size() >= forceUnitBudget;
            }

            currentLanceWeightString = currentLanceWeightString.substring(1);
        }

        // chop out random units until we drop down to our unit count budget
        while (forceUnitBudget > 0 && generatedEntities.size() > forceUnitBudget) {
            generatedEntities.remove(Compute.randomInt(generatedEntities.size()));
        }

        // "flavor" feature - fill up APCs with infantry
        List<Entity> transportedEntities = fillTransports(scenario, generatedEntities, factionCode, skill, quality, campaign);
        generatedEntities.addAll(transportedEntities);

        BotForce generatedForce = new BotForce();
        generatedForce.setEntityList(generatedEntities);
        setBotForceParameters(generatedForce, forceTemplate, forceAlignment, contract);
        scenario.addBotForce(generatedForce, forceTemplate);

        return generatedLanceCount;
    }

    /**
     * Generates the indicated number of civilian entities.
     *
     * @param num      The number of civilian entities to generate
     * @param campaign Current campaign
     */
    public static List<Entity> generateCivilianUnits(int num, Campaign campaign) {
        RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits");
        ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(num);
        return msl.stream().map(ms -> createEntityWithCrew("IND", RandomSkillsGenerator.L_GREEN, campaign, ms)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Generates the indicated number of turret entities.
     * Lifted from AtBScenario.java
     *
     * @param num      The number of turrets to generate
     * @param skill    The skill level of the turret operators
     * @param quality  The quality level of the turrets
     * @param campaign The campaign for which the turrets are being generated.
     */
    public static List<Entity> generateTurrets(int num, int skill, int quality, Campaign campaign) {
        int currentYear = campaign.getGameYear();
        List<MechSummary> msl = campaign.getUnitGenerator().generateTurrets(num, skill, quality, currentYear);
        return msl.stream().map(ms -> createEntityWithCrew("IND", skill, campaign, ms)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Takes all the "bot" forces where the template says they should be player-controlled
     * and transforms them into attached units.
     *
     * @param scenario The scenario for which to translate units
     * @param campaign Current campaign
     */
    private static void translatePlayerNPCsToAttached(AtBDynamicScenario scenario, Campaign campaign) {
        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);

            if (forceTemplate != null && forceTemplate.isAlliedPlayerForce()) {

                for (Entity en : botForce.getEntityList()) {
                    scenario.getAlliesPlayer().add(en);
                    scenario.getBotUnitTemplates().put(UUID.fromString(en.getExternalIdAsString()), forceTemplate);

                    if (!campaign.getCampaignOptions().getAttachedPlayerCamouflage()) {
                        en.setCamoCategory(IPlayer.NO_CAMO);
                        en.setCamoFileName(IPlayer.colorNames[scenario.getContract(campaign).getAllyColorIndex()]);
                    }
                }

                scenario.botForces.remove(botIndex);
                botIndex--;
            }
        }
    }

    /**
     * Translates the template's objectives, filling them in with actual forces from the scenario.
     */
    public static void translateTemplateObjectives(AtBDynamicScenario scenario, Campaign campaign) {
        scenario.getScenarioObjectives().clear();

        for (ScenarioObjective templateObjective : scenario.getTemplate().scenarioObjectives) {
            ScenarioObjective actualObjective = translateTemplateObjective(scenario, campaign, templateObjective);

            scenario.getScenarioObjectives().add(actualObjective);
        }
    }

    /**
     * Translates a single objective, filling it in with actual forces from the scenario.
     */
    public static ScenarioObjective translateTemplateObjective(AtBDynamicScenario scenario,
                                                               Campaign campaign, ScenarioObjective templateObjective) {
        ScenarioObjective actualObjective = new ScenarioObjective(templateObjective);
        actualObjective.clearAssociatedUnits();
        actualObjective.clearForces();

        List<String> objectiveForceNames = new ArrayList<>();
        List<String> objectiveUnitIDs = new ArrayList<>();

        OffBoardDirection calculatedDestinationZone = OffBoardDirection.NONE;

        //for each of the objective's force names loop through all of the following:
        // bot forces
        // assigned player forces
        // assigned player units
        // for each one, if the item is associated with a template that has the template objective's force name
        // add it to the list of actual objective force names
        // this needs to happen because template force names aren't the same as the generated force names

        // additionally, while we're looping through forces, we'll attempt to calculate a destination zone, which we will need
        // if the objective is a reach edge/prevent reaching edge and the direction is "destination edge" ("None").

        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce botForce = scenario.getBotForce(x);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            boolean botForceIsHostile = botForce.getTeam() == ForceAlignment.Opposing.ordinal() ||
                    botForce.getTeam() == ForceAlignment.Third.ordinal();

            // if the bot force's force template's name is included in the objective's force names
            // or if the bot force is hostile and we're including all enemy forces
            if (templateObjective.isApplicableToForceTemplate(forceTemplate, scenario) ||
                    (botForceIsHostile && templateObjective.getAssociatedForceNames().contains(ScenarioObjective.FORCE_SHORTCUT_ALL_ENEMY_FORCES))) {
                objectiveForceNames.add(botForce.getName());
                calculatedDestinationZone = OffBoardDirection.translateBoardStart(getOppositeEdge(forceTemplate.getActualDeploymentZone()));
            }
        }

        for (int forceID : scenario.getPlayerForceTemplates().keySet()) {
            ScenarioForceTemplate playerForceTemplate = scenario.getPlayerForceTemplates().get(forceID);

            if (templateObjective.isApplicableToForceTemplate(playerForceTemplate, scenario) ||
                    templateObjective.getAssociatedForceNames().contains(ScenarioObjective.FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES)) {
                objectiveForceNames.add(campaign.getForce(forceID).getName());
                calculatedDestinationZone = OffBoardDirection.translateBoardStart(getOppositeEdge(playerForceTemplate.getActualDeploymentZone()));
            }
        }

        for (UUID unitID : scenario.getPlayerUnitTemplates().keySet()) {
            ScenarioForceTemplate playerForceTemplate = scenario.getPlayerUnitTemplates().get(unitID);

            if (templateObjective.isApplicableToForceTemplate(playerForceTemplate, scenario) ||
                    templateObjective.getAssociatedForceNames().contains(ScenarioObjective.FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES)) {
                objectiveUnitIDs.add(unitID.toString());
                calculatedDestinationZone = OffBoardDirection.translateBoardStart(getOppositeEdge(playerForceTemplate.getActualDeploymentZone()));
            }
        }

        // this handles generated units that have been put under the player's control
        for (UUID unitID : scenario.getBotUnitTemplates().keySet()) {
            ScenarioForceTemplate botForceTemplate = scenario.getBotUnitTemplates().get(unitID);

            if (templateObjective.isApplicableToForceTemplate(botForceTemplate, scenario)) {
                objectiveUnitIDs.add(unitID.toString());
                calculatedDestinationZone = OffBoardDirection.translateBoardStart(getOppositeEdge(botForceTemplate.getActualDeploymentZone()));
            }
        }

        for (String forceName : objectiveForceNames) {
            actualObjective.addForce(forceName);
        }

        for (String unitID : objectiveUnitIDs) {
            actualObjective.addUnit(unitID);
        }

        // if the objective specifies that it's to reach or prevent reaching a map edge
        // and has been set to "force destination edge", set that here
        if (actualObjective.getDestinationEdge() == OffBoardDirection.NONE &&
                calculatedDestinationZone != OffBoardDirection.NONE &&
                (actualObjective.getObjectiveCriterion() == ObjectiveCriterion.ReachMapEdge ||
                        actualObjective.getObjectiveCriterion() == ObjectiveCriterion.PreventReachMapEdge)) {
            actualObjective.setDestinationEdge(calculatedDestinationZone);
        }

        return actualObjective;
    }

    /**
     * Scale the scenario's objective time limits, if called for, by the number of units
     * that have associated force templates that "contribute to the unit count".
     */
    private static void scaleObjectiveTimeLimits(AtBDynamicScenario scenario, Campaign campaign) {
        int primaryUnitCount = 0;

        for (int forceID : scenario.getPlayerForceTemplates().keySet()) {
            if (scenario.getPlayerForceTemplates().get(forceID).getContributesToUnitCount()) {
                primaryUnitCount += campaign.getForce(forceID).getAllUnits().size();
            }
        }

        for (BotForce botForce : scenario.getBotForceTemplates().keySet()) {
            if (scenario.getBotForceTemplates().get(botForce).getContributesToUnitCount()) {
                primaryUnitCount += botForce.getEntityList().size();
            }
        }

        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {
            if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
                objective.setTimeLimit(primaryUnitCount * objective.getTimeLimitScaleFactor());
            }
        }
    }

    /**
     * Handles random determination of light conditions for the given scenario, as per AtB rules
     *
     * @param scenario The scenario for which to set lighting conditions.
     */
    private static void setLightConditions(AtBDynamicScenario scenario) {
        int roll = Compute.randomInt(10) + 1;
        int light;

        if (roll < 6) {
            light = PlanetaryConditions.L_DAY;
        } else if (roll < 8) {
            light = PlanetaryConditions.L_DUSK;
        } else if (roll == 8) {
            light = PlanetaryConditions.L_FULL_MOON;
        } else if (roll == 9) {
            light = PlanetaryConditions.L_MOONLESS;
        } else {
            light = PlanetaryConditions.L_PITCH_BLACK;
        }

        scenario.setLight(light);
    }

    /**
     * Handles random determination of weather/wind/fog conditions for the given scenario, as per AtB rules
     *
     * @param scenario The scenario for which to set weather conditions.
     */
    private static void setWeather(AtBDynamicScenario scenario) {
        int weather = PlanetaryConditions.WE_NONE;
        int wind = PlanetaryConditions.WI_NONE;
        int fog = PlanetaryConditions.FOG_NONE;

        // weather is irrelevant in these situations.
        if (scenario.getTerrainType() == AtBScenario.TER_SPACE ||
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
            if (r2 == 1) {
                wind = PlanetaryConditions.WI_STORM;
            } else if (r2 == 2) {
                weather = PlanetaryConditions.WE_DOWNPOUR;
            } else if (r2 == 3) {
                weather = PlanetaryConditions.WE_SLEET;
            } else if (r2 == 4) {
                weather = PlanetaryConditions.WE_ICE_STORM;
            } else if (r2 == 5) {
                wind = PlanetaryConditions.WI_TORNADO_F13; // tornadoes are classified as wind rather than weather.
            } else if (r2 == 6) {
                wind = PlanetaryConditions.WI_TORNADO_F4;
            }
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
     *
     * @param scenario The scenario to work on.
     */
    public static void setTerrain(AtBDynamicScenario scenario) {
        int terrainIndex;

        // if we are allowing all terrain types, then pick one from the list
        // otherwise, pick one from the allowed ones
        if (scenario.getTemplate().mapParameters.getMapLocation() == ScenarioMapParameters.MapLocation.AllGroundTerrain) {
            terrainIndex = Compute.randomInt(AtBScenario.terrainTypes.length);
            scenario.setTerrainType(terrainIndex);
            scenario.setMapFile();
        } else if (scenario.getTemplate().mapParameters.getMapLocation() == ScenarioMapParameters.MapLocation.Space) {
            scenario.setTerrainType(AtBScenario.TER_SPACE);
        } else if (scenario.getTemplate().mapParameters.getMapLocation() == ScenarioMapParameters.MapLocation.LowAtmosphere) {
            // low atmosphere actually makes use of the terrain, so we generate some here as well
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
     *
     * @param scenario The scenario to manipulate
     * @param mission  The active mission for the scenario
     * @param campaign The current campaign
     */
    private static void setPlanetaryConditions(AtBDynamicScenario scenario, AtBContract mission, Campaign campaign) {
        if (scenario.getTerrainType() == AtBScenario.TER_SPACE) {
            return;
        }

        if (null != mission) {
            PlanetarySystem pSystem = Systems.getInstance().getSystemById(mission.getSystemId());
            Planet p = pSystem.getPrimaryPlanet();
            if (null != p) {
                int atmosphere = Utilities.nonNull(p.getPressure(campaign.getLocalDate()), scenario.getAtmosphere());
                float gravity = Utilities.nonNull(p.getGravity(), scenario.getGravity()).floatValue();

                scenario.setAtmosphere(atmosphere);
                scenario.setGravity(gravity);
            }
        }
    }

    /**
     * Sets dynamic AtB-sized base map size for the given scenario.
     *
     * @param scenario The scenario to process.
     */
    private static void setScenarioMapSize(AtBDynamicScenario scenario) {
        int mapSizeX;
        int mapSizeY;
        ScenarioTemplate template = scenario.getTemplate();

        // if the template says to use standard AtB sizing, determine it randomly here
        if (template.mapParameters.isUseStandardAtBSizing()) {
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
            mapSizeX = template.mapParameters.getBaseWidth();
            mapSizeY = template.mapParameters.getBaseHeight();
        }

        // increment map size by template-specified increments
        mapSizeX += template.mapParameters.getWidthScalingIncrement() * scenario.getLanceCount();
        mapSizeY += template.mapParameters.getHeightScalingIncrement() * scenario.getLanceCount();

        // 50/50 odds to rotate the map 90 degrees if specified.
        if (template.mapParameters.isAllowRotation()) {
            int roll = Compute.randomInt(20) + 1;
            if (roll <= 10) {
                int swap = mapSizeX;
                mapSizeX = mapSizeY;
                mapSizeY = swap;
            }
        }

        scenario.setMapSizeX(mapSizeX);
        scenario.setMapSizeY(mapSizeY);
    }

    /**
     * Sets up scenario modifiers for this scenario.
     *
     * @param scenario
     */
    public static void setScenarioModifiers(AtBDynamicScenario scenario) {
        // this is hardcoded for now, but the eventual plan is to let the user configure how many modifiers
        // they want applied
        int numMods = Compute.randomInt(2);

        for (int x = 0; x < numMods; x++) {
            AtBScenarioModifier scenarioMod = AtBScenarioModifier.getRandomScenarioModifier();

            if ((scenarioMod.getAllowedMapLocations() == null) ||
                    scenarioMod.getAllowedMapLocations().contains(scenario.getTemplate().mapParameters.getMapLocation())) {
                scenario.getScenarioModifiers().add(scenarioMod);

                if (scenarioMod.getBlockFurtherEvents()) {
                    break;
                }
            }
        }
    }

    /**
     * Simple method to process all scenario modifiers for a given scenario.
     *
     * @param scenario The scenario to modify
     * @param campaign The campaign
     * @param when     Before or after force generation
     */
    public static void applyScenarioModifiers(AtBDynamicScenario scenario, Campaign campaign, EventTiming when) {
        for (AtBScenarioModifier scenarioMod : scenario.getScenarioModifiers()) {
            scenarioMod.processModifier(scenario, campaign, when);
        }
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction     The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill       The RandomSkillGenerator constant that represents the skill level of the overall force.
     * @param quality     The equipment rating of the force.
     * @param unitType    The UnitTableData constant for the type of unit to generate.
     * @param weightClass The weight class of the unit to generate
     * @param campaign
     * @return A new Entity with crew.
     */
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, Campaign campaign) {
        return getEntity(faction, skill, quality, unitType, weightClass, false, campaign);
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction     The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill       The RandomSkillGenerator constant that represents the skill level of the overall force.
     * @param quality     The equipment rating of the force.
     * @param unitType    The UnitTableData constant for the type of unit to generate.
     * @param weightClass The weight class of the unit to generate
     * @param artillery   Whether the unit should be artillery or not. Use with caution, as some unit types simply do not have
     *                    support artillery.
     * @param campaign
     * @return A new Entity with crew.
     */
    public static Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, boolean artillery, Campaign campaign) {
        MechSummary ms;

        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(faction);
        params.setQuality(quality);
        params.setUnitType(unitType);
        params.setWeightClass(weightClass);
        params.setYear(campaign.getGameYear());

        if (unitType == UnitType.TANK) {
            return getTankEntity(params, skill, artillery, campaign);
        } else if (unitType == UnitType.INFANTRY) {
            return getInfantryEntity(params, skill, artillery, campaign);
        } else {
            ms = campaign.getUnitGenerator().generate(params);
        }

        if (ms == null) {
            return null;
        }

        return createEntityWithCrew(faction, skill, campaign, ms);
    }

    /**
     * Generates a tank entity, either artillery or normal.
     *
     * @param params    Unit generation parameters.
     * @param skill     skill level
     * @param artillery whether or not the unit generated should be artillery
     * @return Entity or null if unable to generate.
     */
    public static Entity getTankEntity(UnitGeneratorParameters params, int skill, boolean artillery, Campaign campaign) {
        MechSummary ms;

        // useful debugging statement that forces generation of specific units rather than random ones
        //return getEntityByName("Badger (C) Tracked Transport B", params.getFaction(), skill, campaign);

        if (artillery) {
            params.getMissionRoles().add(MissionRole.ARTILLERY);
        }

        if (campaign.getCampaignOptions().getOpforUsesVTOLs()) {
            params.getMovementModes().addAll(IUnitGenerator.MIXED_TANK_VTOL);
        } else {
            params.setFilter(v -> !v.getUnitType().equals("VTOL"));
        }
        ms = campaign.getUnitGenerator().generate(params);

        if (ms == null) {
            return null;
        }

        return createEntityWithCrew(params.getFaction(), skill, campaign, ms);
    }

    /**
     * Generates an infantry entity, either artillery or normal with a 33% chance of field guns.
     *
     * @param params    Unit generation parameters.
     * @param skill     skill level
     * @param artillery whether or not the unit generated should be artillery
     * @return Entity or null if unable to generate.
     */
    public static Entity getInfantryEntity(UnitGeneratorParameters params, int skill, boolean artillery, Campaign campaign) {
        // note that the "ARTILLERY" mission role appears mutually exclusive with the "FIELD_GUN" mission role
        if (artillery) {
            params.getMissionRoles().add(MissionRole.ARTILLERY);
        } else {
            boolean useFieldGuns = Compute.d6() <= 2;
            if (useFieldGuns) {
                params.getMissionRoles().add(MissionRole.FIELD_GUN);
            }
        }

        params.getMovementModes().addAll(IUnitGenerator.ALL_INFANTRY_MODES);

        MechSummary ms = campaign.getUnitGenerator().generate(params);

        if (ms == null) {
            return null;
        }

        return createEntityWithCrew(params.getFaction(), skill, campaign, ms);
    }

    /**
     * Fill the given transport entity with a bunch of units that it can carry.
     * Currently only works for infantry transports.
     *
     * @param transport
     * @param params
     * @param skill
     * @param campaign
     */
    private static List<Entity> fillTransport(AtBScenario scenario, Entity transport, UnitGeneratorParameters params, int skill, Campaign campaign) {
        List<Entity> transportedUnits = new ArrayList<>();

        // if we've already filled the transport, no need to do it again.
        if (scenario.getTransportLinkages().containsKey(transport.getExternalIdAsString())) {
            return transportedUnits;
        }

        for (Transporter bay : transport.getTransports()) {
            if (bay instanceof TroopSpace) {
                double bayCapacity = bay.getUnused();

                UnitGeneratorParameters newParams = params.clone();
                newParams.clearMovementModes();
                newParams.setWeightClass(AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED);

                Entity transportedUnit = null;

                // for now, we'll assign BA units with greater likelihood to units with higher-rated equipment
                int baRoll = Compute.d6(2);
                if (baRoll >= infantryToBAUpgradeTNs[params.getQuality()]) {
                    transportedUnit = generateTransportedBAUnit(newParams, bayCapacity, skill, campaign);
                }

                // if we can't or won't generate battle armor, try to generate infantry
                if (transportedUnit == null) {
                    transportedUnit = generateTransportedInfantryUnit(newParams, bayCapacity, skill, campaign);
                }

                // if we can't generate anything to transport, move on to the next transport
                if (transportedUnit == null) {
                    continue;
                }

                // sometimes something crazy will happen and we will not be able to load the unit into the transport
                // so let's at least have it deploy at the same time as the transport
                transportedUnit.setDeployRound(transport.getDeployRound());
                scenario.addTransportRelationship(transport.getExternalIdAsString(), transportedUnit.getExternalIdAsString());

                transportedUnits.add(transportedUnit);
            }
        }

        return transportedUnits;
    }

    /**
     * Worker function that generates a conventional infantry unit for transport
     *
     * @return Generated infantry unit, or null if one cannot be generated
     */
    private static Entity generateTransportedInfantryUnit(UnitGeneratorParameters params, double bayCapacity, int skill, Campaign campaign) {
        UnitGeneratorParameters newParams = params.clone();
        newParams.setUnitType(UnitType.INFANTRY);

        // to save ourselves having to re-generate a bunch of infantry for smaller bays (3 tons and lower)
        // we will limit ourselves to generating low-weight foot platoons
        if (bayCapacity <= IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT) {
            newParams.getMovementModes().add(EntityMovementMode.INF_LEG);
            newParams.setFilter(inf -> inf.getTons() <= IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT);
        } else {
            newParams.getMovementModes().addAll(IUnitGenerator.ALL_INFANTRY_MODES);
            newParams.setFilter(inf -> inf.getTons() <= bayCapacity);
        }

        MechSummary ms = campaign.getUnitGenerator().generate(newParams);

        if (ms == null) {
            return null;
        }

        Entity infantry = createEntityWithCrew(newParams.getFaction(), skill, campaign, ms);

        // if we're dealing with a *really* small bay, drop the # squads down until we can fit it in
        while (infantry.getWeight() > bayCapacity) {
            ((Infantry) infantry).setSquadN(((Infantry) infantry).getSquadN() - 1);
            infantry.autoSetInternal();
        }

        // unlikely but theoretically possible
        if (((Infantry) infantry).getSquadN() == 0) {
            return null;
        }

        return infantry;
    }

    /**
     * Worker function that generates a battle armor unit for transport
     *
     * @return Generated battle armor unit, null if one cannot be generated
     */
    private static Entity generateTransportedBAUnit(UnitGeneratorParameters params, double bayCapacity, int skill, Campaign campaign) {
        UnitGeneratorParameters newParams = params.clone();
        newParams.setUnitType(UnitType.BATTLE_ARMOR);

        // battle armor needs a minimum amount of transport capacity if specified
        // if our bay does not have that capacity, we cannot generate BA and return null
        if (bayCapacity >= IUnitGenerator.BATTLE_ARMOR_MIN_WEIGHT || bayCapacity == IUnitGenerator.NO_WEIGHT_LIMIT) {
            newParams.getMovementModes().addAll(IUnitGenerator.ALL_BATTLE_ARMOR_MODES);

            if (bayCapacity != IUnitGenerator.NO_WEIGHT_LIMIT) {
                newParams.setFilter(inf -> inf.getTons() <= bayCapacity);
            }
        } else {
            return null;
        }

        MechSummary ms = campaign.getUnitGenerator().generate(newParams);

        if (ms == null) {
            return null;
        }

        Entity battleArmor = createEntityWithCrew(newParams.getFaction(), skill, campaign, ms);

        return battleArmor;
    }

    /**
     * Fill the provided transports with randomly generated units that
     * can fit into their bays.
     *
     * @param scenario
     * @param transports  list of potential transports
     * @param factionCode
     * @param skill
     * @param quality
     * @param campaign
     * @return transportedUnits List of units being transported
     */
    public static List<Entity> fillTransports(AtBScenario scenario, List<Entity> transports, String factionCode, int skill, int quality, Campaign campaign) {
        List<Entity> transportedUnits = new ArrayList<>();

        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setYear(campaign.getGameYear());

        for (Entity transport : transports) {
            transportedUnits.addAll(fillTransport(scenario, transport, params, skill, campaign));
        }

        return transportedUnits;
    }

    /**
     * Worker function that generates a battle armor unit to attach to a unit of clan mechs
     */
    public static List<Entity> generateBAForNova(AtBScenario scenario, List<Entity> starUnits, String factionCode, int skill, int quality, Campaign campaign) {
        List<Entity> transportedUnits = new ArrayList<>();

        // determine if this should be a nova
        // if yes, then pick the fastest mech and load it up, adding the generated BA to the transport relationships.

        // non-clan forces and units that aren't stars don't become novas
        if (!Faction.getFaction(factionCode).isClan() && (starUnits.size() != 5)) {
            return transportedUnits;
        }

        // logic copied from AtBScenario.addStar() to randomly determine if the given unit is actually going to be a nova
        // adjusted from 11/8 to 8/6 (distribution of novas in newest AtB doc is a lot higher) so that players actually encounter novas
        // whatever CBS is still gets no novas, so there
        int roll = Compute.d6(2);
        int novaTarget = 8;
        if (factionCode.equals("CHH") || factionCode.equals("CSL")) {
            novaTarget = 6;
        } else if (factionCode.equals("CBS")) {
            novaTarget = 13;
        }

        if (roll < novaTarget) {
            return transportedUnits;
        }

        Entity actualTransport = null;
        for (Entity transport : starUnits) {
            if (transport instanceof Mech && transport.isOmni()) {
                if ((actualTransport == null) || (actualTransport.getWalkMP() < transport.getWalkMP())) {
                    actualTransport = transport;
                }
            }
        }

        // no extra battle armor if there's nothing to put it on
        if (actualTransport == null) {
            return transportedUnits;
        }

        // if we're generating a riding BA, do so now, then associate it with the designated transport
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setYear(campaign.getGameYear());
        params.addMissionRole(MissionRole.MECHANIZED_BA);
        params.setWeightClass(UNIT_WEIGHT_UNSPECIFIED);

        Entity transportedUnit = generateTransportedBAUnit(params, IUnitGenerator.NO_WEIGHT_LIMIT, skill, campaign);
        // if we fail to generate battle armor, the rest is meaningless
        if (transportedUnit == null) {
            return transportedUnits;
        }

        transportedUnit.setDeployRound(actualTransport.getDeployRound());
        scenario.addTransportRelationship(actualTransport.getExternalIdAsString(), transportedUnit.getExternalIdAsString());
        transportedUnits.add(transportedUnit);

        return transportedUnits;
    }

    /**
     * Handles loading transported units onto their transports once a megamek scenario has actually started;
     *
     * @param scenario
     */
    public static void loadTransports(AtBScenario scenario, Client client) {
        Map<String, Integer> idMap = new HashMap<>();
        // this is a bit inefficient, should really give the client/game the ability to look up an entity by external ID
        for (Entity entity : client.getEntitiesVector()) {
            if (entity.getOwnerId() == client.getLocalPlayerNumber()) {
                idMap.put(entity.getExternalIdAsString(), entity.getId());
            }
        }

        for (Entity potentialTransport : client.getEntitiesVector()) {
            if ((potentialTransport.getOwnerId() == client.getLocalPlayerNumber()) &&
                    scenario.getTransportLinkages().containsKey(potentialTransport.getExternalIdAsString())) {
                for (String cargoID : scenario.getTransportLinkages().get(potentialTransport.getExternalIdAsString())) {
                    Entity cargo = scenario.getExternalIDLookup().get(cargoID);

                    // if the game contains the potential cargo unit
                    // and the potential transport can actually load it, send the load command to the server
                    if ((cargo != null) &&
                            idMap.containsKey(cargo.getExternalIdAsString()) &&
                            potentialTransport.canLoad(cargo)) {
                        client.sendLoadEntity(idMap.get(cargo.getExternalIdAsString()),
                                idMap.get(potentialTransport.getExternalIdAsString()), -1);
                    }
                }
            }
        }
    }
    /**
     * Generates a new Entity without using a RAT. Useful for "persistent" or fixed units.
     *
     * @param name Full name (chassis + model) of the entity to generate.
     * @param factionCode Faction code to use for name generation
     * @param skill RandomSkillsGenerator.L_* constant for the average crew skill level
     * @param campaign The campaign instance
     * @return The newly generated Entity
     * @note This is a debugging method
     */
    @SuppressWarnings(value = "unused")
    private static Entity getEntityByName(String name, String factionCode, int skill, Campaign campaign) {
        final String METHOD_NAME = "getEntityByName(String,String,int,Campaign)";

        MechSummary mechSummary = MechSummaryCache.getInstance().getMech(name);
        if (mechSummary == null) {
            return null;
        }

        return createEntityWithCrew(factionCode, skill, campaign, mechSummary);
    }

    /**
     * @param factionCode Faction code to use for name generation
     * @param skill RandomSkillsGenerator.L_* constant for the average crew skill level
     * @param campaign The campaign instance
     * @param ms Which entity to generate
     * @return An crewed entity
     */
    public static Entity createEntityWithCrew(String factionCode, int skill, Campaign campaign, MechSummary ms) {
        final String METHOD_NAME = "createEntityWithCrew(String,int,Campaign,MechSummary)";
        Entity en;
        try {
            en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception ex) {
            MekHQ.getLogger().log(AtBDynamicScenarioFactory.class, METHOD_NAME, LogLevel.ERROR,
                    "Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
            MekHQ.getLogger().error(AtBDynamicScenarioFactory.class, METHOD_NAME, ex);
            return null;
        }

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        Faction faction = Faction.getFaction(factionCode);

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());
        Gender gender = RandomGenderGenerator.generate();
        String[] crewNameArray = rng.generateGivenNameSurnameSplit(gender, faction.isClan(), factionCode);
        String crewName = crewNameArray[0];
        crewName += !StringUtil.isNullOrEmpty(crewNameArray[1]) ?  " " + crewNameArray[1] : "";

        Map<Integer, Map<String, String>> extraData = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put(Crew.MAP_GIVEN_NAME, crewNameArray[0]);
        innerMap.put(Crew.MAP_SURNAME, crewNameArray[1]);

        RandomSkillsGenerator rsg = new RandomSkillsGenerator();
        rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
        rsg.setLevel(skill);

        if (faction.isClan()) {
            rsg.setType(RandomSkillsGenerator.T_CLAN);
        }
        int[] skills = rsg.getRandomSkills(en);

        if (faction.isClan() && (Compute.d6(2) > (8 - skill + skills[0] + skills[1]))) {
            Phenotype phenotype = Phenotype.NONE;
            switch (en.getUnitType()) {
                case UnitType.MEK:
                    phenotype = Phenotype.MECHWARRIOR;
                    break;
                case UnitType.TANK:
                case UnitType.VTOL:
                    // The Vehicle Phenotype is unique to Clan Hell's Horses
                    if (factionCode.equals("CHH")) {
                        phenotype = Phenotype.VEHICLE;
                    }
                    break;
                case UnitType.BATTLE_ARMOR:
                    phenotype = Phenotype.ELEMENTAL;
                    break;
                case UnitType.AERO:
                case UnitType.CONV_FIGHTER:
                    phenotype = Phenotype.AEROSPACE;
                    break;
                case UnitType.PROTOMEK:
                    phenotype = Phenotype.PROTOMECH;
                    break;
                case UnitType.SMALL_CRAFT:
                case UnitType.DROPSHIP:
                case UnitType.JUMPSHIP:
                case UnitType.WARSHIP:
                    // The Naval Phenotype is unique to Clan Snow Raven and the Raven Alliance
                    if (factionCode.equals("CSR") || factionCode.equals("RA")) {
                        phenotype = Phenotype.NAVAL;
                    }
                    break;
            }

            if (phenotype != Phenotype.NONE) {
                String bloodname = Bloodname.randomBloodname(factionCode, phenotype,
                        campaign.getGameYear()).getName();
                crewName += " " + bloodname;
                innerMap.put(Crew.MAP_BLOODNAME, bloodname);
                innerMap.put(Crew.MAP_PHENOTYPE, phenotype.name());
            }
        }

        extraData.put(0, innerMap);

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName, Compute.getFullCrewSize(en),
                skills[0], skills[1], gender, extraData));

        en.setExternalIdAsString(UUID.randomUUID().toString());

        return en;
    }

    /**
     * Units that exceed the maximum weight for individual entities in the scenario
     * are replaced in the lance by two lighter units.
     *
     * @param weights   A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param maxWeight The maximum weight allowed for the force by the parameters of the scenario type
     * @return          A new String of the same format as weights
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
        } else if (maxWeight == EntityWeightClass.WEIGHT_LIGHT) {
            retVal = weights.replaceAll(".", "L");
        }
        return retVal;
    }

    /**
     * Adjust a weight string for a minimum weight value
     */
    private static String adjustForMinWeight(String weights, int minWeight) {
        String retVal = weights;

        if (minWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            retVal = weights.replaceAll("L", "M");
        } else if (minWeight == EntityWeightClass.WEIGHT_HEAVY) {
            retVal = weights.replaceAll("[LM]", "H");
        } else if (minWeight == EntityWeightClass.WEIGHT_ASSAULT) {
            retVal = weights.replaceAll("[LMH]", "A");
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
    private static List<Integer> generateUnitTypes(int unitTypeCode, int unitCount, int forceQuality, String factionCode, Campaign campaign) {
        List<Integer> unitTypes = new ArrayList<>(unitCount);
        int actualUnitType = unitTypeCode;

        if (unitTypeCode == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
            Faction faction = Faction.getFaction(factionCode);

            // "AtB Mix" will skip vehicles if the "use vehicles" checkbox is turned off
            // or if the faction is clan and "clan opfors use vehicles" is turned off
            boolean useVehicles = campaign.getCampaignOptions().getUseVehicles() &&
                    (!faction.isClan() || (faction.isClan() && campaign.getCampaignOptions().getClanVehicles()));

            // logic mostly lifted from AtBScenario.java, uses campaign config to determine tank/mech mixture
            if (useVehicles) {
                // some specialized logic for clan opfors
                int era = Era.getEra(campaign.getGameYear());

                // if we're in the late republic or dark ages, clans no longer have the luxury of mech only stars
                boolean clanEquipmentScarcity = era == Era.E_LATE_REPUBLIC || era == Era.E_DARK_AGES;

                if (faction.isClan() && !clanEquipmentScarcity) {
                    return generateClanUnitTypes(unitCount, forceQuality, factionCode, campaign);
                }

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
                        for (int x = 0; x < unitCount; x++) {
                            boolean addTank = Compute.randomInt(2) == 0;
                            if (addTank) {
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
            // if we're not using vehicles, just generate meks
            } else {
                actualUnitType = UnitType.MEK;
            }
        }

        for (int x = 0; x < unitCount; x++) {
            unitTypes.add(actualUnitType);
        }

        return unitTypes;
    }

    /**
     * Specialized logic for generating clan units
     * @return
     */
    private static List<Integer> generateClanUnitTypes(int unitCount, int forceQuality, String factionCode, Campaign campaign) {
        // logic inspired by AtBScenario.addStar
        // for fluff reasons, hell's horses + pals use more vehicles
        // higher-rated clan units become increasingly unlikely to use vehicles
        int vehicleTarget = 6;
        if (factionCode.equals("CHH") || factionCode.equals("CSL") || factionCode.equals("CBS")) {
            vehicleTarget = 8;
        } else {
            vehicleTarget -= forceQuality;
        }

        // we randomly determine tank or mek
        int roll = Compute.d6(2);
        int unitType = campaign.getCampaignOptions().getClanVehicles() && (roll <= vehicleTarget) ?
                UnitType.TANK : UnitType.MEK;

        List<Integer> unitTypes = new ArrayList<>();

        for (int x = 0; x < unitCount; x++) {
            unitTypes.add(unitType);
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
    private static String generateUnitWeights(List<Integer> unitTypes, String faction, int weightClass, int maxWeight, int minWeight, Campaign campaign) {
        Faction genFaction = Faction.getFaction(faction);
        String factionWeightString = AtBConfiguration.ORG_IS;
        if (genFaction.isClan() || faction.equals("MH")) {
            factionWeightString = AtBConfiguration.ORG_CLAN;
        } else if (genFaction.isComstar()) {
            factionWeightString = AtBConfiguration.ORG_CS;
        }

        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(factionWeightString, weightClass), maxWeight);
        weights = adjustForMinWeight(weights, minWeight);

        if (campaign.getCampaignOptions().getRegionalMechVariations()) {
            weights = adjustWeightsForFaction(weights, faction);
        }

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
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            if (forceTemplate != null && forceTemplate.getContributesToBV()) {
                int forceBVBudget = (int) (campaign.getForce(forceID).getTotalBV(campaign) * difficultyMultiplier);
                bvBudget += forceBVBudget;
            }
        }

        // allied bot forces that contribute to BV do not get multiplied by the difficulty
        // even if the player is super good, the AI doesn't get any better
        for (int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if (forceTemplate != null && forceTemplate.getContributesToBV()) {
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
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            if (forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
                int forceUnitCount = (int) (campaign.getForce(forceID).getUnits().size() * difficultyMultiplier);
                unitCount += forceUnitCount;
            }
        }

        // allied bot forces that contribute to BV do not get multiplied by the difficulty
        // even if the player is super good, the AI doesn't get any better
        for (int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if (forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
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

        for (int forceID : scenario.getForceIDs()) {
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

        for (int i = 0; i < unitTypes.size(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes.get(i),
                    UNIT_WEIGHT_UNSPECIFIED, artillery, campaign);
            if (en != null) {
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
        int unitTypeSize = unitTypes.size();

        // it's possible that a unit type list will be longer than the passed-in weights string
        // if so, we log a warning, then generate what we can.
        // having a longer weight string is not an issue, as we simply generate the first N units where N is the size of unitTypes.
        if (unitTypeSize > weights.length()) {
            MekHQ.getLogger().error(AtBDynamicScenarioFactory.class, "generateLance",
                    String.format("More unit types (%d) provided than weights (%d). Truncating generated lance.", unitTypes.size(), weights.length()));
            unitTypeSize = weights.length();
        }

        for (int i = 0; i < unitTypeSize; i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes.get(i),
                    AtBConfiguration.decodeWeightStr(weights, i), artillery, campaign);
            if (en != null) {
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
    private static void setBotForceParameters(BotForce generatedForce, ScenarioForceTemplate forceTemplate,
            ForceAlignment forceAlignment, AtBContract contract) {
        if (forceAlignment == ScenarioForceTemplate.ForceAlignment.Allied) {
            generatedForce.setName(String.format("%s %s", contract.getAllyBotName(), forceTemplate.getForceName()));
            generatedForce.setColorIndex(contract.getAllyColorIndex());
            generatedForce.setCamoCategory(contract.getAllyCamoCategory());
            generatedForce.setCamoFileName(contract.getAllyCamoFileName());
        } else if (forceAlignment == ScenarioForceTemplate.ForceAlignment.Opposing) {
            generatedForce.setName(String.format("%s %s", contract.getEnemyBotName(), forceTemplate.getForceName()));
            generatedForce.setColorIndex(contract.getEnemyColorIndex());
            generatedForce.setCamoCategory(contract.getEnemyCamoCategory());
            generatedForce.setCamoFileName(contract.getEnemyCamoFileName());
        } else {
            generatedForce.setName("Unknown Hostiles");
        }

        generatedForce.setTeam(ScenarioForceTemplate.TEAM_IDS.get(forceAlignment.ordinal()));
    }

    /**
     * Worker method that calculates the destination zones for all the bot forces in a given scenario.
     * Note that it is advisable to call it only after setDeploymentZones has been called on the scenario,
     * as otherwise you'll have "unpredictable" destination zones.
     * @param scenario
     */
    private static void setDestinationZones(AtBDynamicScenario scenario) {
        for (BotForce generatedForce : scenario.getBotForceTemplates().keySet()) {
            setDestinationZone(generatedForce, scenario.getBotForceTemplates().get(generatedForce));
        }
    }

    /**
     * Worker method that calculates the deployment zones for all templates in the given scenario
     * and applies the results to the scenario's bot forces
     * @param scenario The scenario to process
     */
    private static void setDeploymentZones(AtBDynamicScenario scenario) {
        for (ScenarioForceTemplate forceTemplate : scenario.getTemplate().scenarioForces.values()) {
            calculateDeploymentZone(forceTemplate, scenario, forceTemplate.getForceName());
        }

        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            botForce.setStart(scenario.getBotForceTemplates().get(botForce).getActualDeploymentZone());
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
    public static int calculateDeploymentZone(ScenarioForceTemplate forceTemplate, AtBDynamicScenario scenario, String originalForceTemplateID) {
        int calculatedEdge = Board.START_ANY;

        // if we have a specific deployment zone OR have looped around
        if (forceTemplate.getActualDeploymentZone() != Board.START_NONE) {
            return forceTemplate.getActualDeploymentZone();
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.None ||
                Objects.equals(forceTemplate.getSyncedForceName(), originalForceTemplateID)) {
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

        if (calculatedEdge == ScenarioForceTemplate.DEPLOYMENT_ZONE_NARROW_EDGE) {
            List<Integer> edges = new ArrayList<>();

            if (scenario.getMapSizeX() > scenario.getMapSizeY()) {
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
    public static void setDestinationZone(BotForce force, ScenarioForceTemplate forceTemplate) {
        int actualDestinationEdge = forceTemplate.getDestinationZone();

        // set the 'auto flee' flag to true if the bot has a destination edge
        if (actualDestinationEdge != CardinalEdge.NEAREST_OR_NONE.ordinal()) {
            force.getBehaviorSettings().setAutoFlee(true);
        }

        if (forceTemplate.getDestinationZone() == ScenarioForceTemplate.DESTINATION_EDGE_RANDOM) {
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
     *
     * @param scenario Dynamic scenario to process.
     * @param campaign Campaign
     */
    public static void finalizeStaggeredDeploymentTurns(AtBDynamicScenario scenario, Campaign campaign) {
        // assemble a list of all entities that have an "STAGGERED" arrival turn into a list
        // then run setDeploymentTurnsStaggered on them
        List<Entity> staggeredEntities = new ArrayList<>();

        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce currentBotForce = scenario.getBotForce(x);
            for (Entity entity : currentBotForce.getEntityList()) {
                if (entity.getDeployRound() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED) {
                    staggeredEntities.add(entity);
                }
            }
        }

        for (int forceID : scenario.getForceIDs()) {
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits()) {
                Unit currentUnit = campaign.getUnit(unitID);
                if (currentUnit != null && (currentUnit.getEntity().getDeployRound() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED)) {
                    staggeredEntities.add(currentUnit.getEntity());
                }
            }
        }

        for (Entity entity : scenario.getAlliesPlayer()) {
            if (entity.getDeployRound() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED) {
                staggeredEntities.add(entity);
            }
        }

        int strategy = scenario.getLanceCommanderSkill(SkillType.S_STRATEGY, campaign);

        setDeploymentTurnsStaggered(staggeredEntities, strategy);
    }

    /**
     * Sets up the deployment turns for all bot units within the specified scenario
     * @param scenario The scenario to process
     */
    private static void setDeploymentTurns(AtBDynamicScenario scenario) {
        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce currentBotForce = scenario.getBotForce(x);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(currentBotForce);
            setDeploymentTurns(currentBotForce, forceTemplate.getArrivalTurn());
        }
    }

    /**
     * Sets up deployment turns for all bot units within the specified bot force according to the specified force template's rules.
     * @param botForce The bot force to process
     * @param deployRound The specific deployment round, or a special constant.
     * ARRIVAL_TURN_STAGGERED is processed just prior to scenario start instead (?)
     */
    public static void setDeploymentTurns(BotForce botForce, int deployRound) {
        if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
            setDeploymentTurnsStaggeredByLance(botForce.getEntityList());
        } else if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) {
            setDeploymentTurnsForReinforcements(botForce.getEntityList(), 0);
        } else {
            for (Entity entity : botForce.getEntityList()) {
                entity.setDeployRound(deployRound);
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
        // make note of battle commander strategy
        int strategy = scenario.getLanceCommanderSkill(SkillType.S_STRATEGY, campaign);

        // for player forces where there's an associated force template, we can set the deployment turn explicitly
        // or use a stagger algorithm.
        // for player forces where there's not an associated force template, we calculate the deployment turn
        // as if they were reinforcements
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            List<Entity> forceEntities = new ArrayList<>();
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits()) {
                Unit currentUnit = campaign.getUnit(unitID);
                if (currentUnit != null) {
                    forceEntities.add(currentUnit.getEntity());
                }
            }

            // now, attempt to set deployment turns
            // if the force has a template, then use the appropriate algorithm
            // otherwise, treat it as reinforcements
            if (forceTemplate != null) {
                int deployRound = forceTemplate.getArrivalTurn();

                if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
                    setDeploymentTurnsStaggeredByLance(forceEntities);
                } else if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) {
                    setDeploymentTurnsForReinforcements(forceEntities, strategy);
                } else {
                    for (Entity entity : forceEntities) {
                        entity.setDeployRound(deployRound);
                    }
                }
            } else {
                setDeploymentTurnsForReinforcements(forceEntities, strategy);
            }
        }

        // loop through individual units as well
        for (UUID unitID : scenario.getIndividualUnitIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerUnitTemplates().get(unitID);
            Entity entity = campaign.getUnit(unitID).getEntity();

            // now, attempt to set deployment turns
            // if the force has a template, then use the appropriate algorithm
            // otherwise, treat it as reinforcements
            if (forceTemplate != null) {
                int deployRound = forceTemplate.getArrivalTurn();

                if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
                    setDeploymentTurnsStaggeredByLance(Collections.singletonList(entity));
                } else if (deployRound == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) {
                    setDeploymentTurnsForReinforcements(Collections.singletonList(entity), strategy);
                } else {
                    entity.setDeployRound(deployRound);
                }
            } else {
                setDeploymentTurnsForReinforcements(Collections.singletonList(entity), strategy);
            }
        }
    }

    /**
     * Given a dynamic scenario, sets the deployment zones of player units
     */
    public static void setPlayerDeploymentZones(AtBDynamicScenario scenario, Campaign campaign) {
        // for player forces where there's an associated force template, we can set the deployment zone explicitly
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            List<Entity> forceEntities = new ArrayList<>();
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits()) {
                Unit currentUnit = campaign.getUnit(unitID);
                if (currentUnit != null) {
                    forceEntities.add(currentUnit.getEntity());
                }
            }

            // now, attempt to set deployment turns
            if (forceTemplate != null) {
                for (Entity entity : forceEntities) {
                    if (entity.getDeployRound() > 0) {
                        entity.setStartingPos(forceTemplate.getActualDeploymentZone());
                    }
                }
            }
        }

        // loop through individual units as well
        for (UUID unitID : scenario.getIndividualUnitIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerUnitTemplates().get(unitID);
            Entity entity = campaign.getUnit(unitID).getEntity();

            if (forceTemplate != null) {
                if (entity.getDeployRound() > 0) {
                    entity.setStartingPos(forceTemplate.getActualDeploymentZone());
                }
            }
        }
    }

    /**
     * Uses the "individual staggered deployment" algorithm to determine individual deployment turns
     * @param entityList List of entities to process. May be from many players.
     * @param turnModifier The deployment round is reduced by this amount
     */
    private static void setDeploymentTurnsStaggered(List<Entity> entityList, int turnModifier) {
        // loop through all the entities
        // highest movement entity deploys on turn 0
        // other entities deploy on highest move - "walk" MP.
        int maxWalkMP = -1;
        List<Integer> entityWalkMPs = new ArrayList<>();

        for (Entity entity : entityList) {
            // AtB has a legacy mechanism where units with jump jets are counted a little faster
            // for arrival times. We calculate it once and store it.
            int speed = calculateAtBSpeed(entity);

            entityWalkMPs.add(speed);
            if (speed > maxWalkMP) {
                maxWalkMP = speed;
            }
        }

        for (int x = 0; x < entityList.size(); x++) {
            int actualTurnModifier = 0;

            Entity entity = entityList.get(x);
            // the turn modifier is only applicable to player-controlled units
            if (entity.getOwner().getTeam() == ScenarioForceTemplate.TEAM_IDS.get(ForceAlignment.Player.ordinal())) {
                actualTurnModifier = turnModifier;
            }

            // since we're iterating through the same unchanged collection, we can use implicit indexing.
            entity.setDeployRound(Math.max(0, maxWalkMP - entityWalkMPs.get(x) - actualTurnModifier));
        }
    }

    /**
     * Given a list of entities, set the arrival turns for them as if they were all reinforcements on the same side.
     *
     * @param entityList List of entities to process
     * @param turnModifier A number to subtract from the deployment turn.
     */
    public static void setDeploymentTurnsForReinforcements(List<Entity> entityList, int turnModifier) {
        int minimumSpeed = 999;

        // first, we figure out the slowest "atb speed" of this group.
        for (Entity entity : entityList) {
            // don't include transported units in this calculation
            if (entity.getTransportId() != Entity.NONE) {
                continue;
            }

            int speed = calculateAtBSpeed(entity);

            // don't reduce minimum speed to 0, since dividing by zero further down is problematic
            if ((speed < minimumSpeed) && (speed > 0)) {
                minimumSpeed = speed;
            }
        }

        // the actual arrival turn will be the scale divided by the slowest speed.
        // so, a group of Atlases (3/5) should arrive on turn 10 (30 / 3)
        // a group of jump-capable Griffins (5/8/5) should arrive on turn 5 (30 / 6)
        // a group of Ostscouts (8/12/8) should arrive on turn 3 (30 / 9, rounded down)
        // we then subtract the passed-in turn modifier, which is usually the commander's strategy skill level.
        int actualArrivalTurn = Math.max(0, (REINFORCEMENT_ARRIVAL_SCALE / minimumSpeed) - turnModifier);

        for (Entity entity : entityList) {
            entity.setDeployRound(actualArrivalTurn);
        }
    }

    /**
     * Uses the "lance staggered deployment" algorithm to determine individual deployment turns
     * Not actually implemented currently.
     * @param entityList The list of entities to process.
     */
    private static void setDeploymentTurnsStaggeredByLance(List<Entity> entityList) {
        MekHQ.getLogger().warning(AtBDynamicScenarioFactory.class, "setDeploymentTurnsStaggeredByLance", "Deployment Turn - Staggered by Lance not implemented");
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
        if (!same) {
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
        if (faction != null) {
            // clans and marian hegemony use a fundamental unit size of 5.
            if (faction.isClan() || factionCode.equals("MH")) {
                return CLAN_MH_LANCE_SIZE;
            // comstar and wobbies use a fundamental unit size of 6.
            } else if (faction.isComstar()) {
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
    public static int getAeroLanceSize(int unitTypeCode, boolean isPlanetOwner, String factionCode) {
        // capellans use units of three aircraft at a time, others use two
        // TODO: except maybe clans?
        int numFightersPerFlight = factionCode.equals("CC") ? 3 : 2;

        if (unitTypeCode == UnitType.AERO) {
            return numFightersPerFlight;
        } else if (unitTypeCode == UnitType.CONV_FIGHTER) {
            return (Compute.randomInt(3) + 1) * numFightersPerFlight;
        } else {
            int useASFRoll = isPlanetOwner ? Compute.d6() : 6;
            int weightCountRoll = (Compute.randomInt(3) + 1) * numFightersPerFlight; // # of conventional fighters, just in case

            // if we are the planet owner, we may use ASF or conventional fighters
            boolean useASF = useASFRoll >= 4;
            // if we are using ASF, we "always" use 2 at a time, otherwise, use the # of conventional fighters
            return useASF ? numFightersPerFlight : weightCountRoll;
        }
    }

    /**
     * Helper function that deploys the given units off board a random distance 1-2 boards in a random direction
     * @param entityList
     */
    private static void deployArtilleryOffBoard(List<Entity> entityList) {
        OffBoardDirection direction = OffBoardDirection.getDirection(Compute.randomInt(4) + 1);
        int distance = (Compute.randomInt(2) + 1) * 17;

        for (Entity entity : entityList) {
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
        for (Entity entity : entityList) {
            if (!entity.hasETypeFlag(Entity.ETYPE_VTOL)) {
                entity.setAltitude(startingAltitude);
            }

            entity.setElevation(startingAltitude);
        }
    }

    /**
     * Helper function that makes some of the units in the given list of entities
     * carry bombs.
     * @param entityList The list of entities to process
     * @param campaign Campaign object. In the future, may be used to check list of bombs
     * for technological availability.
     */
    public static void populateAeroBombs(List<Entity> entityList, Campaign campaign, boolean groundMap) {
        int maxBombers = Compute.randomInt(entityList.size()) + 1;
        int numBombers = 0;

        int[] validBombChoices = groundMap ? validBotBombs : validBotAABombs;

        for (Entity entity : entityList) {
            if (entity.isBomber()) {
                // if this entity has no guns (e.g. is a Boeing Jump Bomber)
                if (entity.getIndividualWeaponList().size() == 0) {
                    loadBombs(entity, validBombChoices);
                    continue;
                }

                if (numBombers >= maxBombers) {
                    break;
                }

                loadBombs(entity, validBombChoices);
                numBombers++;
            }
        }
    }

    // todo: determine how many bombs can fit on this entity, and either pick
    // a random amount or fill 'er up
    private static void loadBombs(Entity entity, int[] validBombChoices) {
        int[] bombChoices = new int[BombType.B_NUM];
        int numBombs = (int) (entity.getWeight() / 5);
        int bombIndex = validBombChoices[(Compute.randomInt(validBombChoices.length))];
        bombChoices[bombIndex] = numBombs;

        ((IBomber) entity).setBombChoices(bombChoices);
    }

    /**
     * Worker function that returns the faction code of the first owner of the planet where the contract is taking place.
     * @param contract Current contract.
     * @param currentDate Current date.
     * @return Faction code.
     */
    private static String getPlanetOwnerFaction(AtBContract contract, LocalDate currentDate) {
        String factionCode = "MERC";

        // planet owner is the first of the factions that owns the current planet.
        // if there's no such thing, then mercenaries.
        List<String> planetFactions = contract.getSystem().getFactions(currentDate);
        if (planetFactions != null && !planetFactions.isEmpty()) {
            factionCode = planetFactions.get(0);
            Faction ownerFaction = Faction.getFaction(factionCode);

            if (ownerFaction.is(Tag.ABANDONED)) {
                factionCode = "MERC";
            }
        }

        return factionCode;
    }

    /**
     * Worker function that determines the ForceAlignment of the specified faction.
     * @param contract Current contract, for determining the planet we're on.
     * @param factionCode Faction code to check.
     * @param currentDate Current date.
     * @return ForceAlignment.
     */
    private static ForceAlignment getPlanetOwnerAlignment(AtBContract contract, String factionCode, LocalDate currentDate) {
        // if the faction is one of the planet owners, see if it's either the employer or opfor. If it's not, third-party.
        if (contract.getSystem().getFactions(currentDate).contains(factionCode)) {
            if (factionCode.equals(contract.getEmployerCode())) {
                return ForceAlignment.Allied;
            } else if (factionCode.equals(contract.getEnemyCode())) {
                return ForceAlignment.Opposing;
            }
        }

        return ForceAlignment.Third;
    }

    /**
     * Runs all the bot-controlled entities in the scenario through a skill upgrader,
     * potentially giving the SPAs.
     * @param scenario The scenario to process.
     */
    public static void upgradeBotCrews(AtBScenario scenario) {
        CrewSkillUpgrader csu = new CrewSkillUpgrader();

        for (int forceIndex = 0; forceIndex < scenario.getNumBots(); forceIndex++) {
            for (Entity entity : scenario.getBotForce(forceIndex).getEntityList()) {
                csu.upgradeCrew(entity);
            }
        }

        for (Entity entity : scenario.getAlliesPlayer()) {
            csu.upgradeCrew(entity);
        }
    }
}
