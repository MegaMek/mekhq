/*
 * Copyright (c) 2019-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.generator.*;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.ModifiedConstantSkillGenerator;
import megamek.client.ratgenerator.MissionRole;
import megamek.codeUtilities.ObjectUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.containers.MunitionTree;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario.BenchedEntityData;
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
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.stratcon.StratconBiomeManifest;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.enums.EraFlag;
import mekhq.campaign.universe.fameAndInfamy.BatchallFactions;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.round;
import static megamek.client.ratgenerator.MissionRole.CIVILIAN;
import static megamek.common.planetaryconditions.Wind.TORNADO_F4;
import static mekhq.campaign.mission.Scenario.T_GROUND;
import static mekhq.campaign.mission.ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS;
import static mekhq.campaign.mission.ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX;

/**
 * This class handles the creation and substantive manipulation of
 * AtBDynamicScenarios
 *
 * @author NickAragua
 */
public class AtBDynamicScenarioFactory {
    private static final MMLogger logger = MMLogger.create(AtBDynamicScenarioFactory.class);
    /**
     * Unspecified weight class for units, used when the unit type doesn't support
     * weight classes
     */
    public static final int UNIT_WEIGHT_UNSPECIFIED = -1;

    // target number for 2d6 roll of infantry being upgraded to battle armor,
    // indexed by dragoons rating
    private static final int[] infantryToBAUpgradeTNs = { 12, 10, 8, 6, 4, 2 };

    private static final int IS_LANCE_SIZE = 4;
    private static final int CLAN_MH_LANCE_SIZE = 5;
    private static final int COMSTAR_LANCE_SIZE = 6;

    private static final double STRICT = 0.75;
    private static final double OPPORTUNISTIC = 1.0;
    private static final double LIBERAL = 1.25;

    private static final int REINFORCEMENT_ARRIVAL_SCALE = 30;

    private static final ResourceBundle resources = ResourceBundle.getBundle(
            "mekhq.resources.AtBDynamicScenarioFactory",
            MekHQ.getMHQOptions().getLocale());

    /**
     * Method that sets some initial scenario parameters from the given template,
     * prior to force generation and such.
     *
     * @param template The template to use when populating the new scenario.
     * @param contract The contract in which the scenario is to occur.
     * @param campaign The current campaign.
     * @return A new Scenario object with the provided settings
     */
    public static AtBDynamicScenario initializeScenarioFromTemplate(ScenarioTemplate template, AtBContract contract,
            Campaign campaign) {
        AtBDynamicScenario scenario = new AtBDynamicScenario();

        scenario.setName(template.name);
        scenario.setDesc(template.detailedBriefing);
        scenario.setTemplate(template);
        scenario.setEffectiveOpforSkill(contract.getEnemySkill());
        scenario.setEffectiveOpforQuality(contract.getEnemyQuality());
        scenario.setMissionId(contract.getId());

        // apply any fixed modifiers
        for (String modifierName : template.scenarioModifiers) {
            if (AtBScenarioModifier.getScenarioModifiers().containsKey(modifierName)) {
                scenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifiers().get(modifierName));
            }
        }

        boolean planetsideScenario = template.isPlanetSurface();

        if (campaign.getCampaignOptions().isUsePlanetaryConditions() && planetsideScenario) {
            setPlanetaryConditions(scenario, contract, campaign);
        }

        setTerrain(scenario);

        // set lighting conditions if the user wants to play with them and is on a
        // ground map
        // theoretically some lighting conditions apply to space maps as well, but
        // requires additional work to implement properly
        if (campaign.getCampaignOptions().isUseLightConditions() && planetsideScenario) {
            setLightConditions(scenario);
        }

        // set weather conditions if the user wants to play with them and is on a ground
        // map
        if (campaign.getCampaignOptions().isUseWeatherConditions() && planetsideScenario) {
            setWeather(scenario);
        }

        // apply a default "reinforcements" force template if a scenario-specific one
        // does not already exist
        if (!template.getScenarioForces().containsKey(ScenarioForceTemplate.REINFORCEMENT_TEMPLATE_ID)) {
            ScenarioForceTemplate defaultReinforcements = ScenarioForceTemplate.getDefaultReinforcementsTemplate();

            // the default template should not allow the user to deploy ground units as
            // reinforcements to aerospace battles
            // space battles are even more restrictive
            if (template.mapParameters.getMapLocation() == MapLocation.LowAtmosphere) {
                defaultReinforcements.setAllowedUnitType(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX);
            } else if (template.mapParameters.getMapLocation() == MapLocation.Space) {
                defaultReinforcements.setAllowedUnitType(UnitType.AEROSPACEFIGHTER);
            }

            template.getScenarioForces().put(defaultReinforcements.getForceName(), defaultReinforcements);
        }

        return scenario;
    }

    /**
     * Method that should be called when all "required" player forces have been
     * assigned to a scenario.
     * It will generate all primary allied-player, allied-bot and enemy forces,
     * as well as rolling and applying scenario modifiers.
     *
     * @param scenario Scenario to finalize
     * @param contract Contract in which the scenario is occurring
     * @param campaign Current campaign.
     */
    public static void finalizeScenario(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign) {
        // if scenario already had bots, then we need to reset the briefing to remove
        // text related to old scenario modifiers
        if (scenario.getNumBots() > 0) {
            scenario.setDesc(String.format("%s", scenario.getTemplate().detailedBriefing));
        }
        // just in case, clear old bot forces.
        for (int x = scenario.getNumBots() - 1; x >= 0; x--) {
            scenario.removeBotForce(x);
        }

        applyScenarioModifiers(scenario, campaign, EventTiming.PreForceGeneration);

        // Now we can clear the other related lists
        scenario.getAlliesPlayer().clear();
        scenario.getExternalIDLookup().clear();
        scenario.getBotUnitTemplates().clear();

        // fix the force unit count at the current time.
        int playerForceUnitCount = calculateEffectiveUnitCount(scenario, campaign, false);

        // at this point, only the player forces are present and contributing to BV/unit count
        int generatedLanceCount = generateForces(scenario, contract, campaign);

        // approximate estimate, anyway.
        scenario.setLanceCount(generatedLanceCount + (playerForceUnitCount / 4));
        setScenarioMapSize(scenario);
        scenario.setScenarioMap(campaign.getCampaignOptions().getFixedMapChance());
        setDeploymentZones(scenario);
        setDestinationZones(scenario);

        applyScenarioModifiers(scenario, campaign, EventTiming.PostForceGeneration);

        setScenarioRerolls(scenario, campaign);

        setDeploymentTurns(scenario, campaign);
        translatePlayerNPCsToAttached(scenario, campaign);
        translateTemplateObjectives(scenario, campaign);
        scaleObjectiveTimeLimits(scenario, campaign);

        if (campaign.getCampaignOptions().isUseAbilities()) {
            upgradeBotCrews(scenario, campaign);
        }

        scenario.setFinalized(true);
    }

    /**
     * "Meaty" function that generates a set of forces for the given scenario of the
     * given force alignment.
     *
     * @param scenario    Scenario for which we're generating forces
     * @param contract    The contract on which we're currently working. Used for
     *                    skill/quality/planetary info parameters
     * @param campaign    The current campaign
     * @return How many "lances" or other individual units were generated?
     */
    private static int generateForces(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign) {
        logger.info(String.format("GENERATING FORCES FOR: %s", scenario.getName().toUpperCase()));
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
        // generate all forces in a specific order level taking into account previously
        // generated but not current order levels.
        // recalculate effective BV and unit count each time we change levels

        // how close to the allowances do we want to get?
        int targetPercentage = 100 + ((Compute.randomInt(8) - 3) * 5);
        logger.info(String.format("Target Percentage: %s", targetPercentage));

        for (int generationOrder : generationOrders) {
            List<ScenarioForceTemplate> currentForceTemplates = orderedForceTemplates.get(generationOrder);
            effectiveBV = calculateEffectiveBV(scenario, campaign, false);
            effectiveUnitCount = calculateEffectiveUnitCount(scenario, campaign, false);

            for (ScenarioForceTemplate forceTemplate : currentForceTemplates) {
                if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.FixedMUL.ordinal()) {
                    generatedLanceCount += generateFixedForce(scenario, contract, campaign, forceTemplate);
                } else {
                    int weightClass = randomForceWeight();

                    generatedLanceCount += generateForce(scenario, contract, campaign,
                            effectiveBV, effectiveUnitCount, weightClass, forceTemplate, false);
                }
            }
        }

        return generatedLanceCount;
    }

    /**
     * "Meaty" function that generates a force for the given scenario using the
     * fixed MUL
     */
    public static int generateFixedForce(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign,
            ScenarioForceTemplate forceTemplate) {
        File mulFile = new File(MHQConstants.STRATCON_MUL_FILES_DIRECTORY + forceTemplate.getFixedMul());
        if (!mulFile.exists()) {
            logger.error(String.format("MUL file %s does not exist", mulFile.getAbsolutePath()));
            return 0;
        }

        LocalDate currentDate = campaign.getLocalDate();
        ForceAlignment forceAlignment = ForceAlignment.getForceAlignment(forceTemplate.getForceAlignment());

        // planet owner logic requires some special handling
        if (forceAlignment == ForceAlignment.PlanetOwner) {
            String factionCode = getPlanetOwnerFaction(contract, currentDate);
            forceAlignment = getPlanetOwnerAlignment(contract, factionCode, currentDate);
            // updates the force alignment for the template for later examination
            forceTemplate.setForceAlignment(forceAlignment.ordinal());
        }

        Vector<Entity> generatedEntities;

        try {
            MULParser mp = new MULParser(mulFile, campaign.getGameOptions());
            generatedEntities = mp.getEntities();
        } catch (Exception e) {
            logger.error(String.format("Unable to parse MUL file %s", mulFile.getAbsolutePath()), e);
            return 0;
        }

        BotForce generatedForce = new BotForce();
        generatedForce.setFixedEntityList(generatedEntities);
        setBotForceParameters(generatedForce, forceTemplate, forceAlignment, contract);
        scenario.addBotForce(generatedForce, forceTemplate, campaign);

        return generatedEntities.size() / 4;
    }

    /**
     * "Meaty" function that generates a set of forces for the given scenario from
     * the given force template,
     * subject to several other restrictions
     *
     * @param scenario           Scenario for which we're generating forces
     * @param contract           The contract on which we're currently working. Used
     *                           for skill/quality/planetary info parameters
     * @param campaign           The current campaign
     * @param effectiveBV        The effective battle value, up to this point, of
     *                           player and allied units
     * @param effectiveUnitCount The effective unit count, up to this point, of
     *                           player and allied units
     * @param weightClass        The average weight class to generate this force at
     * @param forceTemplate      The force template to use to generate the force
     * @param isScenarioModifier true if the source of generateForce() was a
     *                           scenario modifier
     * @return How many "lances" or other individual units were generated.
     */
    public static int generateForce(AtBDynamicScenario scenario, AtBContract contract, Campaign campaign,
            int effectiveBV, int effectiveUnitCount, int weightClass,
            ScenarioForceTemplate forceTemplate, boolean isScenarioModifier) {
        // don't generate forces flagged as player-supplied
        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) {
            return 0;
        }

        String factionCode = "";
        SkillLevel skill = SkillLevel.GREEN;
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

        boolean unidentifiedThirdPartyPresent = false;

        switch (forceAlignment) {
            case Allied:
            case Player:
                factionCode = contract.getEmployerCode();
                skill = contract.getAllySkill();
                quality = contract.getAllyQuality();
                break;
            case Opposing:
                factionCode = contract.getEnemyCode();
                // Intentional fall-through: opposing third parties are either the contracted
                // enemy or
                // "Unidentified Hostiles" which are considered pirates or bandit caste with
                // random
                // quality and skill
            case Third:
                skill = scenario.getEffectiveOpforSkill();
                quality = scenario.getEffectiveOpforQuality();
                if (forceTemplate.getForceName().toLowerCase().contains("unidentified")) {
                    unidentifiedThirdPartyPresent = true;

                    if (Factions.getInstance().getFaction(getPlanetOwnerFaction(contract, currentDate)).isClan()) {
                        factionCode = "BAN";
                    } else {
                        factionCode = "PIR";
                    }

                    int randomInt = Compute.randomInt(6);

                    skill = switch (randomInt) {
                        case 1, 2, 3 -> SkillLevel.REGULAR;
                        case 4 -> SkillLevel.VETERAN;
                        default -> SkillLevel.GREEN;
                    };

                    quality = switch (randomInt) {
                        case 2, 3 -> IUnitRating.DRAGOON_D;
                        case 4 -> IUnitRating.DRAGOON_C;
                        default -> IUnitRating.DRAGOON_F;
                    };
                }
                break;
            default:
                logger.warn(String.format("Invalid force alignment %d", forceTemplate.getForceAlignment()));
        }

        final Faction faction = Factions.getInstance().getFaction(factionCode);
        String parentFactionType = AtBConfiguration.getParentFactionType(faction);
        boolean isPlanetOwner = isPlanetOwner(contract, currentDate, factionCode);

        // Get the number of units in the typical ground tactical formation.
        // This will differ depending on whether the owner uses Inner Sphere lances,
        // Clan stars, or CS/WOB Level II formations.
        int lanceSize = getLanceSize(factionCode);

        // determine generation parameters
        int forceBV = 0;

        int forceBVBudget = (int) (effectiveBV * forceTemplate.getForceMultiplier());

        if (isScenarioModifier) {
            forceBVBudget = (int) (forceBVBudget * ((double) campaign.getCampaignOptions().getScenarioModBV() / 100)
                * forceTemplate.getForceMultiplier());
        }

        int forceUnitBudget = 0;

        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.UnitCountScaled.ordinal()) {
            forceUnitBudget = (int) (effectiveUnitCount * forceTemplate.getForceMultiplier());
        } else if ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.FixedUnitCount.ordinal())
            || (forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal())) {
            forceUnitBudget = forceTemplate.getFixedUnitCount() == ScenarioForceTemplate.FIXED_UNIT_SIZE_LANCE ? lanceSize : forceTemplate.getFixedUnitCount();
        }

        // Conditions parameters - atmospheric pressure, toxic atmosphere, and gravity
        boolean isLowGravity = false;
        boolean isLowPressure = false;
        boolean isTainted = false;
        boolean allowsConvInfantry = true;
        boolean allowsBattleArmor = true;
        boolean allowsTanks = true;

        if (campaign.getCampaignOptions().isUsePlanetaryModifiers()) {
            if (scenario.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
                isLowPressure = true;
                allowsTanks = false;
            } else {
                mekhq.campaign.universe.Atmosphere specific_atmosphere =
                    contract.getSystem().getPrimaryPlanet().getAtmosphere(currentDate);

                switch (specific_atmosphere) {
                    case TOXICPOISON:
                    case TOXICCAUSTIC:
                        allowsConvInfantry = false;
                        allowsTanks = false;
                        break;
                    case TAINTEDPOISON:
                    case TAINTEDCAUSTIC:
                        isTainted = true;
                        break;
                    default:
                        break;
                }
            }

            if (scenario.getGravity() <= 0.2) {
                allowsTanks = false;
                isLowGravity = true;
            }
        }

        if (campaign.getCampaignOptions().isUseWeatherConditions()) {
            if (scenario.getWind().isTornadoF1ToF3() || scenario.getWind().isTornadoF4()) {
                allowsConvInfantry = false;
                if (scenario.getWind().isTornadoF4()) {
                    allowsTanks = false;
                    allowsBattleArmor = false;
                }
            }
        }

        // Required roles for units in this force. Because these can vary by unit type,
        // each unit type tracks them separately.
        Map<Integer, Collection<MissionRole>> requiredRoles = new HashMap<>();

        // If the force template has one or more preferred roles, get one
        Collection<MissionRole> baseRoles = forceTemplate.getRequiredRoles();

        if (!baseRoles.isEmpty()) {
            if (forceTemplate.getAllowedUnitType() == SPECIAL_UNIT_TYPE_ATB_MIX) {
                requiredRoles.put(UnitType.MEK, new ArrayList<>(baseRoles));
                requiredRoles.put(UnitType.TANK, new ArrayList<>(baseRoles));
            } else if (forceTemplate.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
                requiredRoles.put(UnitType.CONV_FIGHTER, new ArrayList<>(baseRoles));
                requiredRoles.put(UnitType.AEROSPACEFIGHTER, new ArrayList<>(baseRoles));
            } else if (forceTemplate.getAllowedUnitType() == SPECIAL_UNIT_TYPE_ATB_CIVILIANS) {
                // TODO: this will need to be adjusted to cover SUPPORT and CIVILIAN separately
                for (int i = 0; i <= UnitType.AERO; i++) {
                    if (CIVILIAN.fitsUnitType(i)) {
                        requiredRoles.put(i, new ArrayList<>(baseRoles));
                    }
                }
            } else {
                requiredRoles.put(forceTemplate.getAllowedUnitType(), new ArrayList<>(baseRoles));
            }
        }

        // Parameters for infantry - check if XCT or marines are required
        if (allowsConvInfantry && (isTainted || isLowPressure || isLowGravity)) {
            Collection<MissionRole> infantryRoles = new HashSet<>();
            if (isLowGravity) {
                infantryRoles.add(MissionRole.MARINE);
            } else {
                infantryRoles.add(MissionRole.XCT);
            }
            if (requiredRoles.containsKey(UnitType.INFANTRY)) {
                requiredRoles.get(UnitType.INFANTRY).addAll(infantryRoles);
            } else {
                requiredRoles.put(UnitType.INFANTRY, infantryRoles);
            }
        }

        // If the force template is set up for artillery, add the role to all applicable
        // unit types including the dynamic Mek/vehicle mixed type
        if (forceTemplate.getUseArtillery()) {
            int artilleryCarriers = forceTemplate.getAllowedUnitType();

            if (artilleryCarriers == SPECIAL_UNIT_TYPE_ATB_MIX || artilleryCarriers == UnitType.MEK) {
                if (!requiredRoles.containsKey(UnitType.MEK)) {
                    requiredRoles.put(UnitType.MEK, new HashSet<>());
                }
                requiredRoles.get(UnitType.MEK).add((MissionRole.ARTILLERY));
            }
            if (artilleryCarriers == SPECIAL_UNIT_TYPE_ATB_MIX || artilleryCarriers == UnitType.TANK) {
                if (!requiredRoles.containsKey(UnitType.TANK)) {
                    requiredRoles.put(UnitType.TANK, new HashSet<>());
                }
                requiredRoles.get(UnitType.TANK).add((MissionRole.ARTILLERY));
            }
            if (artilleryCarriers == UnitType.INFANTRY) {
                if (!requiredRoles.containsKey(UnitType.INFANTRY)) {
                    requiredRoles.put(UnitType.INFANTRY, new HashSet<>());
                }
                requiredRoles.get(UnitType.INFANTRY).add((MissionRole.ARTILLERY));
            }
        }

        ArrayList<Entity> generatedEntities = new ArrayList<>();
        boolean stopGenerating = false;
        String currentLanceWeightString = "";

        // Generate a tactical formation (lance/star/etc.) until the BV or unit count
        // limits are exceeded
        while (!stopGenerating) {
            List<Entity> generatedLance;

            // Generate a number of tactical formations for this force based on the desired
            // average weight class.
            // This may generate higher numbers of lighter formations, or fewer
            // (minimum of one) of heavier formations.
            if (currentLanceWeightString.isEmpty()) {
                currentLanceWeightString = campaign.getAtBConfig().selectBotLances(parentFactionType, weightClass);
            }

            int actualUnitType = forceTemplate.getAllowedUnitType();

            // The SPECIAL_UNIT_TYPE_ATB_AERO_MIX value allows for random selection of
            // aerospace or conventional fighters.
            // Only allow for conventional fighters where this force controls the system, and where
            // there is an atmosphere. Aerospace fighters are added in single flights/points, while
            // conventional fighters are added in full squadrons (1-3 flights, 2-6 total).
            if (isPlanetOwner && actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX
                    && scenario.getTemplate().mapParameters.getMapLocation() != MapLocation.Space && scenario.getAtmosphere().isDenserThan(Atmosphere.THIN)) {
                actualUnitType = Compute.d6() > 3 ? UnitType.AEROSPACEFIGHTER : UnitType.CONV_FIGHTER;
                lanceSize = getAeroLanceSize(actualUnitType, isPlanetOwner, factionCode);
            } else if (actualUnitType == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
                actualUnitType = UnitType.AEROSPACEFIGHTER;
                lanceSize = getAeroLanceSize(actualUnitType, isPlanetOwner, factionCode);
            }

            // If there are no weight classes available, something went wrong so don't
            // bother trying to generate units
            if (currentLanceWeightString == null) {
                generatedLance = new ArrayList<>();
            } else {
                // Hazardous conditions may prohibit deploying infantry or vehicles
                if ((actualUnitType == UnitType.INFANTRY && !allowsConvInfantry)
                    || (actualUnitType == UnitType.BATTLE_ARMOR && !allowsBattleArmor)) {
                    logger.warn("Unable to generate Infantry due to hostile conditions." +
                        " Switching to Tank.");
                    actualUnitType = UnitType.TANK;
                }

                if (actualUnitType == UnitType.TANK && !allowsTanks) {
                    logger.warn("Unable to generate Tank due to hostile conditions." +
                        " Switching to Mek.");
                    actualUnitType = UnitType.MEK;
                }

                // Gun emplacements use fixed tables instead of the force generator system
                if (actualUnitType == UnitType.GUN_EMPLACEMENT) {
                    generatedLance = generateTurrets(4, skill, quality, campaign, faction);

                // All other unit types use the force generator system to randomly select units
                } else {
                    // Determine unit types for each unit of the formation. Normally this is all one
                    // type, but SPECIAL_UNIT_TYPE_ATB_MIX may generate all Meks, all vehicles, or
                    // a Mek/vehicle mixed formation.
                    List<Integer> unitTypes = generateUnitTypes(actualUnitType, lanceSize, quality,
                        factionCode, allowsTanks, campaign);

                    // Formations composed entirely of Meks, aerospace fighters (but not conventional),
                    // and ground vehicles use weight categories as do SPECIAL_UNIT_TYPE_ATB_MIX.
                    // Formations of other types, plus artillery formations do not use weight classes.
                    if ((actualUnitType == SPECIAL_UNIT_TYPE_ATB_MIX
                        || actualUnitType == SPECIAL_UNIT_TYPE_ATB_CIVILIANS
                        || IUnitGenerator.unitTypeSupportsWeightClass(actualUnitType))
                        && !forceTemplate.getUseArtillery()) {

                        // Generate a specific weight class for each unit based on the formation weight
                        // class and lower/upper bounds
                        final String unitWeights = generateUnitWeights(unitTypes, factionCode,
                            AtBConfiguration.decodeWeightStr(currentLanceWeightString, 0),
                            forceTemplate.getMaxWeightClass(), forceTemplate.getMinWeightClass(),
                            requiredRoles, campaign);

                        if (unitWeights != null) {
                            generatedLance = generateLance(factionCode, skill, quality, unitTypes, unitWeights,
                                requiredRoles, campaign, scenario);
                        } else {
                            generatedLance = new ArrayList<>();
                        }

                        if (!generatedLance.isEmpty() && forceTemplate.isEnemyBotForce()) {
                            logger.info(String.format("Force Weights: %s (%s)",
                                currentLanceWeightString, unitWeights));
                        }
                    } else {
                        generatedLance = generateLance(factionCode, skill, quality, unitTypes, requiredRoles,
                            campaign, scenario);

                        // If extreme temperatures are present and XCT infantry is not being generated,
                        // swap out standard armor for snowsuits or heat suits as appropriate
                        if (actualUnitType == UnitType.INFANTRY) {
                            for (Entity curPlatoon : generatedLance) {
                                changeInfantryKit((Infantry) curPlatoon, isLowPressure, isTainted, scenario.getTemperature());
                            }
                        }
                    }
                }
            }

            // If something went wrong with unit generation, stop generating formations and
            // work with what is already generated
            if (generatedLance.isEmpty()) {
                stopGenerating = true;
                logger.warn(String.format("Unable to generate units from RAT: %s, type %d, max weight %d",
                    factionCode, forceTemplate.getAllowedUnitType(), weightClass));
                continue;
            }

            if (campaign.getCampaignOptions().isAutoConfigMunitions() || forceTemplate.getAllowAeroBombs()) {
                MapLocation mapLocation = scenario.getTemplate().mapParameters.getMapLocation();
                boolean onGround = (mapLocation != MapLocation.LowAtmosphere && mapLocation != MapLocation.Space);
                int ownerBaseQuality;
                boolean isPirate = faction.isRebelOrPirate();

                // Use the raw quality values rather than the diluted 'effective' rating
                switch (forceAlignment) {
                    case Allied:
                        ownerBaseQuality = contract.getAllyQuality();
                        break;
                    case Opposing:
                        ownerBaseQuality = contract.getEnemyQuality();
                        break;
                    case Third:
                        // Slight hack, assume "Unidentified Hostiles" are pirates with variable
                        // quality
                        ownerBaseQuality = Compute.randomInt(3);
                        isPirate = forceTemplate.getForceName().toLowerCase().contains("unidentified");
                        break;
                    default:
                        ownerBaseQuality = quality;
                        break;
                }

                if (campaign.getCampaignOptions().isAutoConfigMunitions()) {
                    // Configure *all* generated units with appropriate munitions (for BV calcs)
                    Game cGame = campaign.getGame();
                    TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(cGame);
                    ArrayList<Entity> arrayGeneratedLance = new ArrayList<>(generatedLance);
                    // bin fill ratio will be adjusted by the load out generator based on piracy and
                    // quality
                    ReconfigurationParameters rp = TeamLoadOutGenerator.generateParameters(cGame,
                        cGame.getOptions(), arrayGeneratedLance, factionCode, new ArrayList<>(),
                        new ArrayList<>(), ownerBaseQuality, ((isPirate) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f));
                    rp.isPirate = isPirate;
                    rp.groundMap = onGround;
                    rp.spaceEnvironment = (mapLocation == MapLocation.Space);
                    MunitionTree mt = TeamLoadOutGenerator.generateMunitionTree(rp, arrayGeneratedLance, "");
                    tlg.reconfigureEntities(arrayGeneratedLance, factionCode, mt, rp);
                } else {
                    // Load the fighters with bombs
                    TeamLoadOutGenerator.populateAeroBombs(generatedLance, campaign.getGameYear(), onGround, ownerBaseQuality, isPirate);
                }
            }

            if (forceTemplate.getUseArtillery() && forceTemplate.getDeployOffboard()) {
                deployArtilleryOffBoard(generatedLance);
            }

            setStartingAltitude(generatedLance, forceTemplate.getStartingAltitude());
            correctNonAeroFlyerBehavior(generatedLance, scenario.getBoardType());

            // If force contributes to map size, increment the generated count of formations added
            if (forceTemplate.getContributesToMapSize()) {
                generatedLanceCount++;
            }

            // Check for mekanized battle armor added to Clan star formations (must be exactly 5
            // OmniMeks, no more, no less)
            generatedLance.addAll(generateBAForNova(scenario, generatedLance, factionCode, skill, quality, campaign, false));

            // Add the formation member BVs to the running total, and the entities to the tracking
            // list
            for (Entity ent : generatedLance) {
                if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
                    forceBV += ent.getGenericBattleValue();
                } else {
                    forceBV += ent.calculateBattleValue();
                }
                generatedEntities.add(ent);
            }

            // Terminate force generation if we've gone over the unit count or BV budget.
            // For BV-scaled forces, check whether to stop generating after each formation is
            // generated.
            if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.BVScaled.ordinal()) {
                // Check random number vs. percentage of the BV budget already generated, with
                // the percentage chosen based on unit rating
                double currentPercentage = ((double) forceBV / forceBVBudget) * 100;

                stopGenerating = currentPercentage > 100;
            } else {
                // For generation methods other than scaled BV, compare to the overall budget
                stopGenerating = generatedEntities.size() >= forceUnitBudget;
            }

            currentLanceWeightString = currentLanceWeightString.substring(1);
        }

        // If over budget for BV or unit count, pull units until it works
        while (forceUnitBudget > 0 && generatedEntities.size() > forceUnitBudget) {
            int targetUnit = Compute.randomInt(generatedEntities.size());
            generatedEntities.remove(targetUnit);
        }

        if (forceTemplate.getGenerationMethod() == ForceGenerationMethod.BVScaled.ordinal()) {
            String balancingType = "";
            if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
                balancingType = " Generic";
            }
            logger.info(String.format("%s generated a force with %s / %s %s BV",
                forceTemplate.getForceName(), forceBV, forceBVBudget, balancingType));

            int adjustedBvBudget = (int) (forceBVBudget * 1.1);

            while ((forceBV > adjustedBvBudget) && (generatedEntities.size() > 1)) {
                int targetUnit = Compute.randomInt(generatedEntities.size());

                int battleValue;
                if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
                    battleValue = generatedEntities.get(targetUnit).getGenericBattleValue();
                } else {
                    battleValue = generatedEntities.get(targetUnit).calculateBattleValue();
                }

                forceBV -= battleValue;

                logger.info(String.format("Culled %s (%s %s BV)",
                        generatedEntities.get(targetUnit).getDisplayName(), battleValue, balancingType));

                generatedEntities.remove(targetUnit);
            }

            logger.info(String.format("Final force %s / %s %s BV (adjusted for bounds)",
                    forceBV, adjustedBvBudget, balancingType));
        }

        // Units with infantry bays get conventional infantry or battle armor added
        List<Entity> transportedEntities = fillTransports(scenario, generatedEntities, factionCode,
                skill, quality, requiredRoles, allowsConvInfantry, campaign);
        generatedEntities.addAll(transportedEntities);

        if (!transportedEntities.isEmpty()) {
            // Transported units need to filter out battle armor before applying armor changes
            for (Entity curPlatoon : transportedEntities.stream().filter(i -> i.getUnitType() == UnitType.INFANTRY).toList()) {
                changeInfantryKit((Infantry) curPlatoon, isLowPressure, isTainted, scenario.getTemperature());
            }
        }

        // Generate the force
        BotForce generatedForce = new BotForce();
        generatedForce.setFixedEntityList(generatedEntities);
        setBotForceParameters(generatedForce, forceTemplate, forceAlignment, contract);
        if (unidentifiedThirdPartyPresent) {
            generatedForce.setCamouflage(AtBContract.pickRandomCamouflage(currentDate.getYear(), factionCode));
        }
        scenario.addBotForce(generatedForce, forceTemplate, campaign);

        if (generatedForce.getTeam() != 1
            && campaign.getCampaignOptions().isUseGenericBattleValue()
            && BatchallFactions.usesBatchalls(factionCode)
            && contract.isBatchallAccepted()) {
            // Simulate bidding away of forces
            List<String> bidAwayForces = new ArrayList<>();
            int supplementedForces = 0;

            if (generatedForce.getTeam() != 1
                && campaign.getCampaignOptions().isUseGenericBattleValue()
                && BatchallFactions.usesBatchalls(factionCode)
                && contract.isBatchallAccepted()) {

                // Add dialog
                bidAwayForces = new ArrayList<>();

                // Player force values
                int playerBattleValue = calculateEffectiveBV(scenario, campaign, true);
                int playerUnitValue = calculateEffectiveUnitCount(scenario, campaign, true);

                // Bot force values
                int botBattleValue = 0;
                for (Entity entity : generatedForce.getFullEntityList(campaign)) {
                    botBattleValue += entity.calculateBattleValue();
                }

                // First bid away units that exceed the player's estimated Battle Value
                while ((botBattleValue > (playerBattleValue * getHonorRating(campaign, factionCode)))
                        && (generatedForce.getFullEntityList(campaign).size() > 1)) {
                    int targetUnit = Compute.randomInt(generatedForce.getFullEntityList(campaign).size());
                    bidAwayForces.add(generatedForce.getFullEntityList(campaign).get(targetUnit).getShortNameRaw());
                    botBattleValue -= generatedForce.getFullEntityList(campaign).get(targetUnit).calculateBattleValue();
                    generatedForce.removeEntity(targetUnit);
                }

                // There is no point in adding extra Battle Armor to non-ground scenarios
                // Similarly, there is no point adding Battle Armor to scenarios they cannot survive in.
                if (scenario.getBoardType() == T_GROUND && scenario.getWind() != TORNADO_F4) {
                    // We want to purposefully exclude off-board artillery, to stop them being
                    // assigned random units of Battle Armor.
                    // If we ever implement the ability to move those units on-board, or for players
                    // to intercept off-board units, we'll probably want to remove this exclusion,
                    // so they can have some bodyguards.
                    if (!forceTemplate.getUseArtillery() && !forceTemplate.getDeployOffboard()) {
                        // Similarly, there is no value in adding random Battle Armor to aircraft forces
                        if (forceTemplate.getAllowedUnitType() != SPECIAL_UNIT_TYPE_ATB_MIX) {
                            // Next, if the size of the forces results in a Player:Bot unit count ratio of >= 2:1,
                            // add additional units of Battle Armor to compensate.
                            int sizeDisparity = playerUnitValue - generatedForce.getFullEntityList(campaign).size();
                            sizeDisparity = (int) round(sizeDisparity * 0.5);

                            List<Entity> allRemainingUnits = new ArrayList<>(generatedForce.getFullEntityList(campaign));
                            Collections.shuffle(allRemainingUnits);

                            // First, attempt to add Mechanized Battle Armor
                            Iterator<Entity> entityIterator = allRemainingUnits.iterator();
                            while (entityIterator.hasNext() && sizeDisparity > 0) {
                                Entity entity = entityIterator.next();
                                if (!entity.isOmni()) {
                                    continue;
                                }

                                List<Entity> generatedBA = generateBAForNova(scenario, List.of(entity),
                                        factionCode, skill, quality, campaign, true);

                                if (!generatedBA.isEmpty()) {
                                    for (Entity battleArmor : generatedBA) {
                                        generatedForce.addEntity(battleArmor);
                                    }
                                    supplementedForces += generatedBA.size();
                                    sizeDisparity -= generatedBA.size();
                                }
                            }

                            // If there is still a disproportionate size disparity, add loose Battle Armor
                            for (int i = 0; i < sizeDisparity; i++) {
                                Entity newEntity = getEntity(factionCode, skill, quality,
                                        UnitType.BATTLE_ARMOR, UNIT_WEIGHT_UNSPECIFIED, campaign);
                                if (newEntity != null) {
                                    generatedForce.addEntity(newEntity);
                                    supplementedForces++;
                                }
                            }
                        }
                    }
                }
            }

            // Report the bidding results (if any) to the player
            if (generatedForce.getTeam() != 1
                && campaign.getCampaignOptions().isUseGenericBattleValue()
                && BatchallFactions.usesBatchalls(factionCode)
                && contract.isBatchallAccepted()) {
                reportResultsOfBidding(scenario, campaign, bidAwayForces, generatedForce,
                    supplementedForces, factionCode);
            }
        }

        return generatedLanceCount;
    }

    /**
     * Calculates the honor rating for a given Clan.
     *
     * @param campaign    the ongoing campaign
     * @param factionCode the faction code for which to calculate honor rating
     * @return the honor rating as a double value
     */
    private static double getHonorRating(Campaign campaign, String factionCode) {
        // Our research showed the post-Invasion shift in Clan doctrine to occur between 3053 and 3055
        boolean isPostInvasion = campaign.getLocalDate().getYear() >= 3053 + Compute.randomInt(2);

        // This is based on the table found on page 274 of Total Warfare
        // Any Clan not mentioned on that table is assumed to be Strict → Opportunistic
        return switch (factionCode) {
            case "CCC", "CHH", "CIH", "CNC", "CSR" -> OPPORTUNISTIC;
            case "CCO", "CGS", "CSV" -> STRICT;
            case "CGB", "CWIE" -> {
                if (isPostInvasion) {
                    yield LIBERAL;
                } else {
                    yield STRICT;
                }
            }
            case "CDS" -> LIBERAL;
            case "CW" -> {
                if (isPostInvasion) {
                    yield LIBERAL;
                } else {
                    yield OPPORTUNISTIC;
                }
            }
            default -> {
                if (isPostInvasion) {
                    yield OPPORTUNISTIC;
                } else {
                    yield STRICT;
                }
            }
        };
    }

    /**
     * Reports the results of Clan bidding for a scenario.
     *
     * @param scenario           the scenario for which bidding was done
     * @param campaign           the campaign in which the bidding took place
     * @param bidAwayForces      the list of forces bid away by the generated force
     * @param generatedForce     the force that generated the bid
     * @param supplementedForces the number of additional Battle Armor units supplemented to the force
     */
    private static void reportResultsOfBidding(AtBDynamicScenario scenario, Campaign campaign,
                                               List<String> bidAwayForces, BotForce generatedForce,
                                               int supplementedForces, String factionCode) {
        double honor = getHonorRating(campaign, factionCode);
        String honorLevel;

        if (honor == STRICT) {
            honorLevel = "STRICT";
        } else if (honor == OPPORTUNISTIC) {
            honorLevel = "OPPORTUNISTIC";
        } else {
            honorLevel = "LIBERAL";
        }

        logger.info(String.format("The honor of %s is rated as %s",
            Factions.getInstance().getFaction(factionCode).getFullName(campaign.getGameYear()),
            honorLevel));

        boolean useVerboseBidding = campaign.getCampaignOptions().isUseVerboseBidding();
        StringBuilder report = new StringBuilder();

        if (useVerboseBidding) {
            for (String unitName : bidAwayForces) {
                if (report.isEmpty()) {
                    report.append(String.format(resources.getString("bidAwayForcesVerbose.text"),
                            generatedForce.getName(), unitName));
                } else {
                    report.append(unitName).append("<br>");
                }
            }
        } else {
            if (!bidAwayForces.isEmpty()) {
                report.append(String.format(resources.getString("bidAwayForces.text"),
                        generatedForce.getName(), bidAwayForces.size(), bidAwayForces.size() > 1 ? "s" : ""));

                boolean isUseLoggerHeader = true;
                for (String unitName : bidAwayForces) {
                    if (isUseLoggerHeader) {
                        logger.info(String.format(resources.getString("bidAwayForcesLogger.text"),
                            generatedForce.getName()));
                        isUseLoggerHeader = false;
                    }
                    logger.info(unitName);
                }
            }
        }

        if (supplementedForces > 0) {
            if (report.isEmpty()) {
                report.append(String.format(resources.getString("addedBattleArmorNewReport.text"),
                        generatedForce.getName(), supplementedForces, supplementedForces > 1 ? "s" : ""));
            } else {
                report.append(String.format(
                        resources.getString("addedBattleArmorContinueReport.text"),
                        supplementedForces, supplementedForces > 1 ? "s" : ""));
            }
        }

        if (supplementedForces == 0 && bidAwayForces.isEmpty()) {
            report.append(String.format(resources.getString("nothingBidAway.text"),
                generatedForce.getName()));
        }

        campaign.addReport(report.toString());
    }

    /**
     * Generates the indicated number of civilian entities.
     *
     * @param num      The number of civilian entities to generate
     * @param campaign Current campaign
     */
    public static List<Entity> generateCivilianUnits(int num, Campaign campaign) {
        RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits");
        ArrayList<MekSummary> msl = RandomUnitGenerator.getInstance().generate(num);
        return msl.stream().map(ms -> createEntityWithCrew("IND", SkillLevel.GREEN, campaign, ms))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Generates the indicated number of turret entities.
     * Lifted from AtBScenario.java
     *
     * @param num      The number of turrets to generate
     * @param skill    The skill level of the turret operators
     * @param quality  The quality level of the turrets
     * @param campaign The campaign for which the turrets are being generated.
     * @param faction  The faction to generate turrets for
     */
    public static List<Entity> generateTurrets(int num, SkillLevel skill, int quality, Campaign campaign,
            Faction faction) {
        return campaign.getUnitGenerator().generateTurrets(num, skill, quality, campaign.getGameYear()).stream()
                .map(ms -> createEntityWithCrew(faction, skill, campaign, ms))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Takes all the "bot" forces where the template says they should be
     * player-controlled
     * and transforms them into attached units.
     *
     * @param scenario The scenario for which to translate units
     * @param campaign Current campaign
     */
    private static void translatePlayerNPCsToAttached(AtBDynamicScenario scenario, Campaign campaign) {
        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);

            if ((forceTemplate != null) && forceTemplate.isAlliedPlayerForce()) {
                final Camouflage camouflage = scenario.getContract(campaign).getAllyCamouflage();
                for (Entity en : botForce.getFullEntityList(campaign)) {
                    scenario.getAlliesPlayer().add(en);
                    scenario.getBotUnitTemplates().put(UUID.fromString(en.getExternalIdAsString()), forceTemplate);

                    if (!campaign.getCampaignOptions().isAttachedPlayerCamouflage()) {
                        en.setCamouflage(camouflage.clone());
                    }
                }

                scenario.getBotForces().remove(botIndex);
                botIndex--;
            }
        }
    }

    /**
     * Translates the template's objectives, filling them in with actual forces from
     * the scenario.
     */
    public static void translateTemplateObjectives(AtBDynamicScenario scenario, Campaign campaign) {
        scenario.getScenarioObjectives().clear();

        for (ScenarioObjective templateObjective : scenario.getTemplate().scenarioObjectives) {
            ScenarioObjective actualObjective = translateTemplateObjective(scenario, campaign, templateObjective);

            scenario.getScenarioObjectives().add(actualObjective);
        }
    }

    /**
     * Translates a single objective, filling it in with actual forces from the
     * scenario.
     */
    public static ScenarioObjective translateTemplateObjective(AtBDynamicScenario scenario,
            Campaign campaign, ScenarioObjective templateObjective) {
        ScenarioObjective actualObjective = new ScenarioObjective(templateObjective);
        actualObjective.clearAssociatedUnits();
        actualObjective.clearForces();

        List<String> objectiveForceNames = new ArrayList<>();
        List<String> objectiveUnitIDs = new ArrayList<>();

        OffBoardDirection calculatedDestinationZone = OffBoardDirection.NONE;

        // for each of the objective's force names loop through all of the following:
        // bot forces
        // assigned player forces
        // assigned player units
        // for each one, if the item is associated with a template that has the template
        // objective's force name
        // add it to the list of actual objective force names
        // this needs to happen because template force names aren't the same as the
        // generated force names

        // additionally, while we're looping through forces, we'll attempt to calculate
        // a destination zone, which we will need
        // if the objective is a reach edge/prevent reaching edge and the direction is
        // "destination edge" ("None").

        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce botForce = scenario.getBotForce(x);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            boolean botForceIsHostile = botForce.getTeam() == ForceAlignment.Opposing.ordinal() ||
                    botForce.getTeam() == ForceAlignment.Third.ordinal();

            // if the bot force's force template's name is included in the objective's force
            // names
            // or if the bot force is hostile and we're including all enemy forces
            if (templateObjective.isApplicableToForceTemplate(forceTemplate, scenario) ||
                    (botForceIsHostile && templateObjective.getAssociatedForceNames()
                            .contains(ScenarioObjective.FORCE_SHORTCUT_ALL_ENEMY_FORCES))) {
                objectiveForceNames.add(botForce.getName());
                calculatedDestinationZone = OffBoardDirection
                        .translateBoardStart(getOppositeEdge(forceTemplate.getActualDeploymentZone()));
            }
        }

        for (int forceID : scenario.getPlayerForceTemplates().keySet()) {
            ScenarioForceTemplate playerForceTemplate = scenario.getPlayerForceTemplates().get(forceID);

            if (templateObjective.isApplicableToForceTemplate(playerForceTemplate, scenario) ||
                    templateObjective.getAssociatedForceNames()
                            .contains(ScenarioObjective.FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES)) {
                objectiveForceNames.add(campaign.getForce(forceID).getName());
                calculatedDestinationZone = OffBoardDirection
                        .translateBoardStart(getOppositeEdge(playerForceTemplate.getActualDeploymentZone()));
            }
        }

        for (UUID unitID : scenario.getPlayerUnitTemplates().keySet()) {
            ScenarioForceTemplate playerForceTemplate = scenario.getPlayerUnitTemplates().get(unitID);

            if (templateObjective.isApplicableToForceTemplate(playerForceTemplate, scenario) ||
                    templateObjective.getAssociatedForceNames()
                            .contains(ScenarioObjective.FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES)) {
                objectiveUnitIDs.add(unitID.toString());
                calculatedDestinationZone = OffBoardDirection
                        .translateBoardStart(getOppositeEdge(playerForceTemplate.getActualDeploymentZone()));
            }
        }

        // this handles generated units that have been put under the player's control
        for (UUID unitID : scenario.getBotUnitTemplates().keySet()) {
            ScenarioForceTemplate botForceTemplate = scenario.getBotUnitTemplates().get(unitID);

            if (templateObjective.isApplicableToForceTemplate(botForceTemplate, scenario)) {
                objectiveUnitIDs.add(unitID.toString());
                calculatedDestinationZone = OffBoardDirection
                        .translateBoardStart(getOppositeEdge(botForceTemplate.getActualDeploymentZone()));
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
     * Scale the scenario's objective time limits, if called for, by the number of
     * units
     * that have associated force templates that "contribute to the unit count".
     */
    private static void scaleObjectiveTimeLimits(AtBDynamicScenario scenario, Campaign campaign) {
        int primaryUnitCount = 0;

        for (int forceID : scenario.getPlayerForceTemplates().keySet()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);

            if ((forceTemplate != null) && forceTemplate.getContributesToUnitCount()) {
                primaryUnitCount += campaign.getForce(forceID).getAllUnits(true).size();
            }
        }

        for (BotForce botForce : scenario.getBotForceTemplates().keySet()) {
            if (scenario.getBotForceTemplates().get(botForce).getContributesToUnitCount()) {
                primaryUnitCount += botForce.getFullEntityList(campaign).size();
            }
        }

        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {
            if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
                objective.setTimeLimit(primaryUnitCount * objective.getTimeLimitScaleFactor());
            }
        }
    }

    /**
     * Handles random determination of light conditions for the given scenario, as
     * per AtB rules
     *
     * @param scenario The scenario for which to set lighting conditions.
     */
    private static void setLightConditions(AtBDynamicScenario scenario) {
        scenario.setLightConditions();
    }

    /**
     * Handles random determination of weather/wind/fog conditions for the given
     * scenario, as per AtB rules
     *
     * @param scenario The scenario for which to set weather conditions.
     */
    private static void setWeather(AtBDynamicScenario scenario) {
        scenario.setWeatherConditions();
    }

    /**
     * Handles random determination of terrain and corresponding map file from
     * allowed terrain types
     *
     * @param scenario The scenario to work on.
     */
    public static void setTerrain(AtBDynamicScenario scenario) {
        // if we are allowing all terrain types, then pick one from the list
        // otherwise, pick one from the allowed ones
        if (scenario.getTemplate().mapParameters.getMapLocation() == MapLocation.AllGroundTerrain) {
            scenario.setBoardType(T_GROUND);
            StratconBiomeManifest biomeManifest = StratconBiomeManifest.getInstance();
            int kelvinTemp = scenario.getTemperature() + StratconContractInitializer.ZERO_CELSIUS_IN_KELVIN;
            List<String> allowedTerrain = biomeManifest.getTempMap(StratconBiomeManifest.TERRAN_BIOME)
                    .floorEntry(kelvinTemp).getValue().allowedTerrainTypes;

            int terrainIndex = Compute.randomInt(allowedTerrain.size());
            scenario.setTerrainType(allowedTerrain.get(terrainIndex));
            scenario.setMapFile();
        } else if (scenario.getTemplate().mapParameters.getMapLocation() == MapLocation.Space) {
            scenario.setBoardType(AtBScenario.T_SPACE);
            scenario.setTerrainType("Space");
        } else if (scenario.getTemplate().mapParameters.getMapLocation() == MapLocation.LowAtmosphere) {
            scenario.setBoardType(AtBScenario.T_ATMOSPHERE);
            // low atmosphere actually makes use of the terrain, so we generate some here as
            // well
            scenario.setTerrain();
            scenario.setMapFile();
        } else {
            StratconBiomeManifest biomeManifest = StratconBiomeManifest.getInstance();
            int kelvinTemp = scenario.getTemperature() + StratconContractInitializer.ZERO_CELSIUS_IN_KELVIN;
            List<String> allowedFacility = biomeManifest.getTempMap(StratconBiomeManifest.TERRAN_FACILITY_BIOME)
                    .floorEntry(kelvinTemp).getValue().allowedTerrainTypes;
            List<String> allowedTerrain = biomeManifest.getTempMap(StratconBiomeManifest.TERRAN_BIOME)
                    .floorEntry(kelvinTemp).getValue().allowedTerrainTypes;
            List<String> allowedTemplate = scenario.getTemplate().mapParameters.allowedTerrainTypes;
            // try to filter on temp
            allowedTerrain.addAll(allowedFacility);
            allowedTemplate.retainAll(allowedTerrain);
            allowedTemplate = !allowedTemplate.isEmpty() ? allowedTemplate
                    : scenario.getTemplate().mapParameters.allowedTerrainTypes;

            int terrainIndex = Compute.randomInt(allowedTemplate.size());
            scenario.setTerrainType(scenario.getTemplate().mapParameters.allowedTerrainTypes.get(terrainIndex));
            scenario.setMapFile();
        }
    }

    /**
     * Method that handles setting planetary conditions - atmospheric pressure and
     * gravity currently -
     * based on the planet on which the scenario is taking place.
     *
     * @param scenario The scenario to manipulate
     * @param mission  The active mission for the scenario
     * @param campaign The current campaign
     */
    private static void setPlanetaryConditions(AtBDynamicScenario scenario, AtBContract mission, Campaign campaign) {
        if (scenario.getBoardType() == AtBScenario.T_SPACE) {
            return;
        }

        if (null != mission) {
            PlanetarySystem pSystem = Systems.getInstance().getSystemById(mission.getSystemId());
            Planet p = pSystem.getPrimaryPlanet();
            if (null != p) {
                Atmosphere atmosphere = Atmosphere.getAtmosphere(ObjectUtility
                        .nonNull(p.getPressure(campaign.getLocalDate()), scenario.getAtmosphere().ordinal()));
                float gravity = ObjectUtility.nonNull(p.getGravity(), scenario.getGravity()).floatValue();
                int temperature = ObjectUtility.nonNull(p.getTemperature(campaign.getLocalDate()),
                        scenario.getTemperature());

                scenario.setAtmosphere(atmosphere);
                scenario.setGravity(gravity);
                scenario.setTemperature(temperature);
            }
        }
    }

    /**
     * Sets dynamic AtB-sized base map size for the given scenario.
     *
     * @param scenario The scenario to process.
     */
    public static void setScenarioMapSize(AtBDynamicScenario scenario) {
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
     * Randomly generates the number of scenario modifiers for a scenario,
     * for each random scenario in the count a random modifier is applied to the
     * scenario.
     *
     * @param campaignOptions The prior defined campaign options
     * @param scenario        The scenario to receive the modifiers.
     */
    public static void setScenarioModifiers(CampaignOptions campaignOptions, AtBDynamicScenario scenario) {
        int numMods = 0;
        boolean addMods = true;
        int modMax = campaignOptions.getScenarioModMax();
        int modChance = campaignOptions.getScenarioModChance();

        if (modMax != 0) {
            while (addMods) {
                if (Compute.randomInt(100) < modChance) {
                    numMods++;

                    if (numMods >= modMax) {
                        addMods = false;
                    }
                } else {
                    addMods = false;
                }
            }

            for (int x = 0; x < numMods; x++) {
                AtBScenarioModifier scenarioMod = AtBScenarioModifier
                        .getRandomBattleModifier(scenario.getTemplate().mapParameters.getMapLocation());

                scenario.addScenarioModifier(scenarioMod);

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
     * Determines the most appropriate RAT and uses it to generate a random Entity.
     * This overload is a convenience to allow calling the main getEntity without
     * providing
     * a specific set of roles.
     *
     * @param faction     The faction code to use for locating the correct RAT and
     *                    assigning a crew name
     * @param skill       The {@link SkillLevel} of the overall force.
     * @param quality     The equipment rating of the force.
     * @param unitType    The {@link UnitType} constant of the type of unit to
     *                    generate.
     * @param weightClass The {@link EntityWeightClass} constant of the unit to
     *                    generate.
     * @param campaign    Campaign data
     * @return A randomly selected Entity from the parameters specified, with crew.
     *         May return null.
     */
    public static Entity getEntity(String faction,
            SkillLevel skill,
            int quality,
            int unitType,
            int weightClass,
            Campaign campaign) {
        return getEntity(faction,
                skill,
                quality,
                unitType,
                weightClass,
                null,
                campaign);
    }

    /**
     * Use the force generator system to randomly select a unit based on parameters
     *
     * @param faction     The faction code to use for locating the correct RAT and
     *                    assigning a crew name
     * @param skill       The {@link SkillLevel} of the overall force.
     * @param quality     The equipment rating of the force.
     * @param unitType    The {@link UnitType} constant of the type of unit to
     *                    generate.
     * @param weightClass The {@link EntityWeightClass} constant of the unit to
     *                    generate.
     * @param rolesByType Collections of roles required for each unit type, or null
     * @param campaign    The current campaign
     * @return A randomly selected Entity from the parameters specified, with crew.
     *         May return null.
     */
    public static @Nullable Entity getEntity(String faction,
            SkillLevel skill,
            int quality,
            int unitType,
            int weightClass,
            @Nullable Collection<MissionRole> rolesByType,
            Campaign campaign) {
        MekSummary unitData;

        // Set up random unit generation parameters
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(faction);
        params.setQuality(quality);
        params.setUnitType(unitType);
        params.setWeightClass(weightClass);
        params.setYear(campaign.getGameYear());

        if (rolesByType != null && !rolesByType.isEmpty()) {
            params.setMissionRoles(rolesByType);
        }

        // Vehicles and infantry require some additional processing
        if (unitType == UnitType.TANK) {
            return getTankEntity(params, skill, campaign);
        } else if (unitType == UnitType.INFANTRY) {
            return getInfantryEntity(params, skill, true, campaign);
        } else {
            unitData = campaign.getUnitGenerator().generate(params);
        }


        if (unitData == null) {
            if (!params.getMissionRoles().isEmpty()) {
                Entity secondChanceEntity = getEntity(faction, skill, quality, unitType, weightClass, campaign);

                if (secondChanceEntity == null) {
                    logger.warn(String.format("Unable to randomly generate %s %s with roles: %s." +
                            " Second chance generation also failed.",
                        EntityWeightClass.getClassName(params.getWeightClass()),
                        UnitType.getTypeName(unitType),
                        params.getMissionRoles().stream().map(Enum::name).collect(Collectors.joining(","))));
                } else {
                    return secondChanceEntity;
                }
            }
            return null;
        }

        return createEntityWithCrew(faction, skill, campaign, unitData);
    }

    /**
     * Randomly creates a ground vehicle, or VTOL if campaign options allows, with a
     * randomly
     * generated crew. Selection of specific functions such as artillery are handled
     * through the
     * roles contained in the UnitGeneratorParameters object.
     *
     * @param params   {@link UnitGeneratorParameters} with random generation
     *                 parameters
     * @param skill    {@link SkillLevel} target for crew
     * @param campaign Campaign object for accessing game options and force
     *                 generator
     * @return randomly generated Entity with crew, or null
     */
    public static Entity getTankEntity(UnitGeneratorParameters params,
            SkillLevel skill,
            Campaign campaign) {
        MekSummary ms;

        // useful debugging statement that forces generation of specific units rather
        // than random ones
        // return getEntityByName("Heavy Tracked APC", params.getFaction(), skill,
        // campaign);
        // return getEntityByName("Badger (C) Tracked Transport B", params.getFaction(),
        // skill, campaign);

        if (campaign.getCampaignOptions().isOpForUsesVTOLs()) {
            params.getMovementModes().addAll(IUnitGenerator.MIXED_TANK_VTOL);
        } else {
            params.setFilter(v -> !v.getUnitType().equals("VTOL"));
        }
        MekSummary unitData = campaign.getUnitGenerator().generate(params);

        if (unitData == null) {
            if (!params.getMissionRoles().isEmpty()) {
                logger.warn(String.format("Unable to randomly generate %s %s with roles: %s",
                        EntityWeightClass.getClassName(params.getWeightClass()),
                        UnitType.getTypeName(UnitType.TANK),
                        params.getMissionRoles().stream().map(Enum::name).collect(Collectors.joining(","))));
            }
            return null;
        }

        return createEntityWithCrew(params.getFaction(), skill, campaign, unitData);
    }

    /**
     * Randomly generates an infantry unit, with a randomly generated 'crew'.
     * Selection of specific
     * functions such as artillery are handled through the roles contained in the
     * UnitGeneratorParameters object.
     * Certain roles in the UnitGeneratorParameters object are uncommon and may
     * result in no unit
     * being generated.
     *
     * @param params     {@link UnitGeneratorParameters} with random generation
     *                   parameters
     * @param skill      {@link SkillLevel} target for crew
     * @param useTempXCT true to swap armor for hostile environment suit if XCT role
     *                   is required
     *                   but no units generate
     * @param campaign   Campaign object for access to force generator
     * @return randomly generated Entity with crew, or null
     */
    public static Entity getInfantryEntity(UnitGeneratorParameters params,
            SkillLevel skill,
            boolean useTempXCT,
            Campaign campaign) {
        UnitGeneratorParameters noXCTParams;
        boolean temporaryXCT = false;

        // Select from all infantry movement types
        params.getMovementModes().addAll(IUnitGenerator.ALL_INFANTRY_MODES);

        MekSummary unitData = campaign.getUnitGenerator().generate(params);

        if (unitData == null) {

            // If XCT troops were requested but none were found, generate without the role
            if (useTempXCT && params.getMissionRoles().contains(MissionRole.XCT)) {
                noXCTParams = params.clone();
                noXCTParams.getMissionRoles().remove(MissionRole.XCT);
                unitData = campaign.getUnitGenerator().generate(noXCTParams);
                temporaryXCT = true;
            }
            if (unitData == null) {
                if (!params.getMissionRoles().isEmpty()) {
                    logger.warn(String.format("Unable to randomly generate %s with roles: %s",
                            UnitType.getTypeName(UnitType.INFANTRY),
                            params.getMissionRoles().stream().map(Enum::name).collect(Collectors.joining(","))));
                }
                return null;
            }
        }

        Entity crewedPlatoon = createEntityWithCrew(params.getFaction(), skill, campaign, unitData);

        // If needed, temporarily assign troops hostile environmental suits
        if (temporaryXCT) {
            changeInfantryKit((Infantry) crewedPlatoon, false, true, 25);
        }

        return crewedPlatoon;
    }

    /**
     * Swaps out infantry armor kit based on provided conditions. Alternate armor
     * kits are
     * snow/heat suits (temperature only), light environment suits (low pressure
     * only),
     * and hostile environment suit (tainted or multiple conditions).
     *
     * @param platoon       Conventional infantry platoon to configure
     * @param isLowPressure true if atmosphere is too thin to breathe
     * @param isTainted     true if atmosphere has contaminants
     * @param temperature   Scenario temperature, in degrees C
     */
    private static void changeInfantryKit(Infantry platoon,
            boolean isLowPressure,
            boolean isTainted,
            int temperature) {
        boolean isHot = temperature > 50;
        boolean isCold = temperature < -30;

        if (isTainted) {
            platoon.setArmorKit(MiscType.createISEnvironmentSuitHostileInfArmor());
        } else if (!isLowPressure) {

            // Normal pressure, with extreme temperature
            if (isHot || isCold) {
                platoon.setArmorKit(isHot ? MiscType.createISHeatSuitInfArmor() : MiscType.createSnowSuitInfArmor());
            }

        } else {

            // Low/no atmosphere, with or without extreme temperature
            if (isHot || isCold) {
                platoon.setArmorKit(MiscType.createISEnvironmentSuitHostileInfArmor());
            } else {
                platoon.setArmorKit(MiscType.createISEnvironmentSuitLightInfArmor());
            }

        }
    }

    /**
     * Identify all units that can carry infantry, and attempt to generate infantry
     * or battle
     * armor to fill them.
     *
     * @param scenario      current scenario, for accessing transport linkages
     * @param transports    list of potential transports
     * @param factionCode   Faction code for generating infantry
     * @param skill         {@link SkillLevel} target skill for crews of generated
     *                      units
     * @param quality       {@link IUnitRating} Base quality for selection of
     *                      infantry
     * @param requiredRoles Lists of required roles for generated units
     * @param allowInfantry false if conventional infantry shouldn't be generated
     * @param campaign      current campaign
     * @return a list of newly created and crewed infantry or battle armor.
     *         Entities may be empty but should not be null
     */
    public static List<Entity> fillTransports(AtBScenario scenario,
            List<Entity> transports,
            String factionCode,
            SkillLevel skill,
            int quality,
            Map<Integer, Collection<MissionRole>> requiredRoles,
            boolean allowInfantry,
            Campaign campaign) {

        // Don't bother processing if various non-useful conditions are present
        if (transports == null ||
                transports.isEmpty() ||
                transports.stream().map(Entity::getUnitType).allMatch(curType -> curType != UnitType.TANK &&
                        curType != UnitType.VTOL &&
                        curType != UnitType.NAVAL &&
                        curType != UnitType.CONV_FIGHTER)) {
            return new ArrayList<>();
        }

        // Strip roles that are not infantry or battle armor, and remove the artillery
        // role
        Map<Integer, Collection<MissionRole>> transportedRoles = new HashMap<>();

        if (requiredRoles != null) {

            transportedRoles.put(UnitType.INFANTRY,
                    requiredRoles.containsKey(UnitType.INFANTRY) ? new ArrayList<>(requiredRoles.get(UnitType.INFANTRY))
                            : new ArrayList<>());
            transportedRoles.get(UnitType.INFANTRY).remove((MissionRole.ARTILLERY));

            transportedRoles.put(UnitType.BATTLE_ARMOR,
                    requiredRoles.containsKey(UnitType.BATTLE_ARMOR)
                            ? new ArrayList<>(requiredRoles.get(UnitType.BATTLE_ARMOR))
                            : new ArrayList<>());
            transportedRoles.get(UnitType.BATTLE_ARMOR).remove((MissionRole.ARTILLERY));
        }

        List<Entity> transportedUnits = new ArrayList<>();

        // Set base parameters
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setYear(campaign.getGameYear());

        // Only check unit types that can have an infantry bay
        for (Entity transport : transports) {
            if (IntStream.of(UnitType.TANK,
                    UnitType.VTOL,
                    UnitType.NAVAL,
                    UnitType.CONV_FIGHTER).anyMatch(i -> transport.getUnitType() == i)) {
                transportedUnits.addAll(fillTransport(scenario,
                        transport,
                        params,
                        skill,
                        transportedRoles,
                        allowInfantry,
                        campaign));
            }
        }

        return transportedUnits;
    }

    /**
     * Identify if the provided entity can carry infantry, and if not already doing
     * so try adding
     * battle armor or conventional infantry
     *
     * @param scenario      current scenario, for accessing transport linkages
     * @param transport     Entity to generate infantry for
     * @param params        {@link UnitGeneratorParameters} for passing settings to
     *                      random generation
     * @param skill         {@link SkillLevel} target skill for crews of generated
     *                      units
     * @param requiredRoles Lists of required roles for generated units
     * @param allowInfantry false if conventional infantry should not be generated
     * @param campaign      current campaign
     * @return List of Entities, containing infantry to load onto this transport.
     *         Might be empty but should not be null.
     */
    private static List<Entity> fillTransport(AtBScenario scenario,
            Entity transport,
            UnitGeneratorParameters params,
            SkillLevel skill,
            Map<Integer, Collection<MissionRole>> requiredRoles,
            boolean allowInfantry,
            Campaign campaign) {

        List<Entity> transportedUnits = new ArrayList<>();

        // Only check transports that are not loaded
        if (scenario.getTransportLinkages().containsKey(transport.getExternalIdAsString())) {
            return transportedUnits;
        }

        for (Transporter bay : transport.getTransports()) {
            // If unit has an infantry bay
            if (bay instanceof TroopSpace) {

                double bayCapacity = bay.getUnused();
                int remainingCount = (int) Math.max(1, Math.floor(bayCapacity / IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT));
                while (remainingCount > 0) {

                    // Set base random generation parameters
                    UnitGeneratorParameters newParams = params.clone();
                    newParams.clearMovementModes();
                    newParams.setWeightClass(AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED);

                    Entity transportedUnit = null;
                    Entity mechanizedBAUnit = null;

                    // If a roll against the battle armor target number succeeds, try to generate a
                    // battle armor unit first
                    if (Compute.d6(2) >= infantryToBAUpgradeTNs[params.getQuality()]) {
                        newParams.setMissionRoles(requiredRoles.getOrDefault(UnitType.BATTLE_ARMOR, new HashSet<>()));
                        transportedUnit = generateTransportedBAUnit(newParams, bayCapacity, skill, false, campaign);

                        // If the transporter has both bay space and is an omni unit, try to add a
                        // second battle armor unit on the outside
                        if (transport.isOmni()) {
                            mechanizedBAUnit = generateTransportedBAUnit(newParams, IUnitGenerator.NO_WEIGHT_LIMIT,
                                    skill, false, campaign);
                        }
                    }

                    // If a battle armor unit wasn't generated and conditions permit, try generating
                    // conventional infantry. Generate air assault infantry for VTOL transports.
                    if (transportedUnit == null && allowInfantry) {
                        newParams.setMissionRoles(requiredRoles.getOrDefault(UnitType.INFANTRY, new HashSet<>()));
                        if (transport.getUnitType() == UnitType.VTOL
                                && !newParams.getMissionRoles().contains(MissionRole.XCT)) {
                            UnitGeneratorParameters paratrooperParams = newParams.clone();
                            paratrooperParams.addMissionRole(MissionRole.PARATROOPER);
                            transportedUnit = generateTransportedInfantryUnit(paratrooperParams, bayCapacity, skill,
                                    true, campaign);
                        } else {
                            transportedUnit = generateTransportedInfantryUnit(newParams, bayCapacity, skill, true,
                                    campaign);
                        }
                    }

                    // If no suitable battle armor or infantry
                    if (transportedUnit == null) {
                        break;
                    }

                    // Set the infantry deployment to the same deployment round as the transport
                    transportedUnit.setDeployRound(transport.getDeployRound());
                    scenario.addTransportRelationship(transport.getExternalIdAsString(),
                            transportedUnit.getExternalIdAsString());

                    if (mechanizedBAUnit != null) {
                        mechanizedBAUnit.setDeployRound((transport.getDeployRound()));
                        scenario.addTransportRelationship(transport.getExternalIdAsString(),
                                mechanizedBAUnit.getExternalIdAsString());
                    }

                    transportedUnits.add(transportedUnit);
                    remainingCount--;
                    bayCapacity -= transportedUnit.getWeight();
                    if (bayCapacity < IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT) {
                        remainingCount = 0;
                    }

                }

            }

        }

        return transportedUnits;
    }

    /**
     * Randomly select a conventional infantry unit with crew. Small bays (under 3
     * tons) may
     * reduce the number of squads below the unit standard. If the XCT role is
     * required,
     * normal infantry may have a hostile environmental suit substituted for their
     * normal armor.
     *
     * @param params      {@link UnitGeneratorParameters} for passing settings to
     *                    random generation
     * @param bayCapacity Remaining bay capacity for internal transport
     * @param skill       {@link SkillLevel} target skill for crews of generated
     *                    units
     * @param useTempXCT  true to swap standard armor for hostile environmental suit
     *                    if XCT role is
     *                    required but no unit is generated
     * @param campaign    current campaign
     * @return Generated infantry unit, or null if one cannot be generated
     */
    private static Entity generateTransportedInfantryUnit(UnitGeneratorParameters params,
            double bayCapacity,
            SkillLevel skill,
            boolean useTempXCT,
            Campaign campaign) {

        UnitGeneratorParameters newParams = params.clone();
        newParams.setUnitType(UnitType.INFANTRY);
        MekSummary unitData;
        boolean temporaryXCT = false;
        UnitGeneratorParameters noXCTParams;
        Entity crewedPlatoon;

        // Limit small bays (3 tons and less) to foot infantry, except for air assault
        // which may
        // include other types
        if (bayCapacity <= IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT) {

            if (newParams.getMissionRoles().contains(MissionRole.PARATROOPER)) {
                newParams.setMovementModes(IUnitGenerator.ALL_INFANTRY_MODES);
            } else {
                newParams.getMovementModes().add(EntityMovementMode.INF_LEG);
            }
            newParams.setFilter(inf -> inf.getTons() <= IUnitGenerator.FOOT_PLATOON_INFANTRY_WEIGHT);
            unitData = campaign.getUnitGenerator().generate(newParams);

            if (unitData == null) {

                // If XCT troops were requested but none were found, generate without the role
                if (useTempXCT && newParams.getMissionRoles().contains(MissionRole.XCT)) {
                    noXCTParams = newParams.clone();
                    noXCTParams.getMissionRoles().remove(MissionRole.XCT);
                    unitData = campaign.getUnitGenerator().generate(noXCTParams);
                    temporaryXCT = true;
                }
                if (unitData == null) {
                    return null;
                }

            }

            crewedPlatoon = createEntityWithCrew(newParams.getFaction(), skill, campaign, unitData);

            // If needed, reduce the weight even further by trimming the number of squads
            while (crewedPlatoon.getWeight() > bayCapacity) {
                if (((Infantry) crewedPlatoon).getSquadCount() - 1 == 0) {
                    return null;
                }
                ((Infantry) crewedPlatoon).setSquadCount(((Infantry) crewedPlatoon).getSquadCount() - 1);
                crewedPlatoon.autoSetInternal();
            }

        } else {
            newParams.getMovementModes().addAll(IUnitGenerator.ALL_INFANTRY_MODES);
            newParams.setFilter(inf -> inf.getTons() <= bayCapacity);
            unitData = campaign.getUnitGenerator().generate(newParams);

            if (unitData == null) {

                // If XCT troops were requested but none were found, generate without the role
                if (useTempXCT && newParams.getMissionRoles().contains(MissionRole.XCT)) {
                    noXCTParams = newParams.clone();
                    noXCTParams.getMissionRoles().remove(MissionRole.XCT);
                    unitData = campaign.getUnitGenerator().generate(noXCTParams);
                    temporaryXCT = true;
                }
                if (unitData == null) {
                    return null;
                }
            }

            crewedPlatoon = createEntityWithCrew(newParams.getFaction(), skill, campaign, unitData);
        }

        // If needed, temporarily assign troops hostile environmental suits
        if (temporaryXCT) {
            changeInfantryKit(((Infantry) crewedPlatoon), false, true, 25);
        }

        return crewedPlatoon;
    }

    /**
     * Worker function that generates a battle armor unit for transport in a bay or
     * riding as
     * mechanized BA
     *
     * @param params            {@link UnitGeneratorParameters} for passing settings
     *                          to random generation
     * @param bayCapacity       Remaining bay capacity for internal transport, or
     *                          IUnitGenerator.NO_WEIGHT_LIMIT
     *                          for circumstances such as mechanized battle armor
     * @param skill             {@link SkillLevel} target skill for crews of
     *                          generated units
     * @param retryAsMechanized true to retry failed bay transport as mechanized
     *                          transport
     * @param campaign          current campaign
     * @return Generated battle armor entity with crew, null if one cannot be
     *         generated
     */
    private static Entity generateTransportedBAUnit(UnitGeneratorParameters params,
            double bayCapacity,
            SkillLevel skill,
            boolean retryAsMechanized,
            Campaign campaign) {

        // Ensure a proposed non-mechanized carrier has enough bay space
        if (bayCapacity != IUnitGenerator.NO_WEIGHT_LIMIT &&
                bayCapacity < IUnitGenerator.BATTLE_ARMOR_MIN_WEIGHT) {
            return null;
        }

        UnitGeneratorParameters newParams = params.clone();
        newParams.setUnitType(UnitType.BATTLE_ARMOR);

        newParams.getMovementModes().addAll(IUnitGenerator.ALL_BATTLE_ARMOR_MODES);

        // Set the parameters to filter out types that are too heavy for the provided
        // bay space, or those that cannot use mechanized BA travel
        if (bayCapacity != IUnitGenerator.NO_WEIGHT_LIMIT) {
            newParams.setFilter(inf -> inf.getTons() <= bayCapacity);
        } else {
            newParams.addMissionRole(MissionRole.MECHANIZED_BA);
        }

        MekSummary unitData = campaign.getUnitGenerator().generate(newParams);

        // If generating for an internal bay fails, try again as mechanized if the flag is set
        if (unitData == null) {
            if (bayCapacity != IUnitGenerator.NO_WEIGHT_LIMIT && retryAsMechanized) {
                newParams.setFilter(null);
                newParams.addMissionRole((MissionRole.MECHANIZED_BA));
                unitData = campaign.getUnitGenerator().generate(newParams);
            }
            if (unitData == null) {
                return null;
            }
        }

        // Add an appropriate crew
        return createEntityWithCrew(newParams.getFaction(), skill, campaign, unitData);
    }

    /**
     * Generates and associates Battle Armor units with a designated transport.
     *
     * @param scenario              The current scenario.
     * @param starUnits             List of {@link Entity} objects representing the star units.
     * @param factionCode           The code of the faction.
     * @param skill                 The skill level.
     * @param quality               The quality level.
     * @param campaign              The current campaign.
     * @param forceBASpawn          Flag to determine whether to bypass the Star size check and BA
     *                              spawn dice roll
     * @return List of {@link Entity} objects representing the transported Battle Armor.
     */
    public static List<Entity> generateBAForNova(AtBScenario scenario, List<Entity> starUnits,
            String factionCode, SkillLevel skill, int quality, Campaign campaign,
                                                 boolean forceBASpawn) {
        List<Entity> transportedUnits = new ArrayList<>();

        // determine if this should be a nova
        // if yes, then pick the fastest mek and load it up, adding the generated BA to
        // the transport relationships.

        // non-clan forces and units that aren't stars don't become novas
        // TODO: allow for non-Clan integrated mechanized formations, like WOB choirs,
        // as well as stars that are short one or more omnis
        if (!Factions.getInstance().getFaction(factionCode).isClan()
                && ((starUnits.size() != 5) || forceBASpawn)) {
            return transportedUnits;
        }

        if (!forceBASpawn) {
            // logic copied from AtBScenario.addStar() to randomly determine if the given
            // unit is actually going to be a nova adjusted from 11/8 to 8/6 so that players
            // actually encounter novas.
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
        }

        Entity actualTransport = null;
        for (Entity transport : starUnits) {
            if (transport instanceof Mek && transport.isOmni()) {
                if ((actualTransport == null) || (actualTransport.getWalkMP() < transport.getWalkMP())) {
                    actualTransport = transport;
                }
            }
        }

        // no extra battle armor if there's nothing to put it on
        if (actualTransport == null) {
            return transportedUnits;
        }

        // if we're generating a riding BA, do so now, then associate it with the
        // designated transport
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setYear(campaign.getGameYear());
        params.addMissionRole(MissionRole.MECHANIZED_BA);
        params.setWeightClass(UNIT_WEIGHT_UNSPECIFIED);

        Entity transportedUnit = generateTransportedBAUnit(params, IUnitGenerator.NO_WEIGHT_LIMIT, skill, false,
                campaign);
        // if we fail to generate battle armor, the rest is meaningless
        if (transportedUnit == null) {
            return transportedUnits;
        }

        transportedUnit.setDeployRound(actualTransport.getDeployRound());
        scenario.addTransportRelationship(actualTransport.getExternalIdAsString(),
                transportedUnit.getExternalIdAsString());
        transportedUnits.add(transportedUnit);

        return transportedUnits;
    }

    /**
     * Generates a new Entity without using a RAT. Useful for "persistent" or fixed
     * units.
     *
     * @param name        Full name (chassis + model) of the entity to generate.
     * @param factionCode Faction code to use for name generation
     * @param skill       {@link SkillLevel} for the average crew skill level
     * @param campaign    The campaign instance
     * @return The newly generated Entity
     * @note This is a debugging method
     */
    @SuppressWarnings(value = "unused")
    private static Entity getEntityByName(String name, String factionCode, SkillLevel skill,
            Campaign campaign) {
        MekSummary mekSummary = MekSummaryCache.getInstance().getMek(name);
        if (mekSummary == null) {
            return null;
        }

        return createEntityWithCrew(factionCode, skill, campaign, mekSummary);
    }

    /**
     * Overloaded method provided to generate a crewed unit using a faction short
     * name rather than
     * a Faction object.
     *
     * @param factionCode String with faction short name
     * @param skill       the {@link SkillLevel} for the average crew skill level
     * @param campaign    The campaign instance
     * @param ms          Which entity to generate
     * @return A crewed entity
     */
    public static @Nullable Entity createEntityWithCrew(String factionCode, SkillLevel skill, Campaign campaign,
            MekSummary ms) {
        return createEntityWithCrew(Factions.getInstance().getFaction(factionCode), skill, campaign, ms);
    }

    /**
     * @param faction  Faction for selection of crew name(s)
     * @param skill    {@link SkillLevel} for the average crew skill level
     * @param campaign Current campaign
     * @param unitData Chassis/model data of unit
     * @return A crewed entity
     */
    public static @Nullable Entity createEntityWithCrew(Faction faction,
            SkillLevel skill,
            Campaign campaign,
            MekSummary unitData) {
        Entity en;
        try {
            en = new MekFileParser(unitData.getSourceFile(), unitData.getEntryName()).getEntity();
        } catch (Exception ex) {
            logger.error("Unable to load entity: {}: {}",
                    unitData.getSourceFile(),
                    unitData.getEntryName(), ex);
            return null;
        }

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());

        Gender gender;
        int nonBinaryDiceSize = campaign.getCampaignOptions().getNonBinaryDiceSize();

        if ((nonBinaryDiceSize > 0) && (Compute.randomInt(nonBinaryDiceSize) == 0)) {
            gender = RandomGenderGenerator.generateOther();
        } else {
            gender = RandomGenderGenerator.generate();
        }

        String[] crewNameArray = rng.generateGivenNameSurnameSplit(gender, faction.isClan(), faction.getShortName());
        String crewName = crewNameArray[0];
        crewName += !StringUtility.isNullOrBlank(crewNameArray[1]) ? ' ' + crewNameArray[1] : "";

        Map<Integer, Map<String, String>> extraData = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put(Crew.MAP_GIVEN_NAME, crewNameArray[0]);
        innerMap.put(Crew.MAP_SURNAME, crewNameArray[1]);

        final AbstractSkillGenerator skillGenerator = new ModifiedConstantSkillGenerator();

        int skillValue = skill.ordinal();
        int skillRoll = Compute.d6(1);

        if (skillRoll == 1) {
            skillValue = Math.max(1, skillValue - 1);
        } else if (skillRoll == 6) {
            skillValue = Math.min(7, skillValue + 1);
        }

        skill = SkillLevel.parseFromInteger(skillValue);

        skillGenerator.setLevel(skill);
        int[] skills = skillGenerator.generateRandomSkills(en);

        if (faction.isClan() && (Compute.d6(2) > (6 - skill.ordinal() + skills[0] + skills[1]))) {
            Phenotype phenotype = Phenotype.NONE;
            switch (en.getUnitType()) {
                case UnitType.MEK:
                    phenotype = Phenotype.MEKWARRIOR;
                    break;
                case UnitType.TANK:
                case UnitType.VTOL:
                    // The Vehicle Phenotype is unique to Clan Hell's Horses
                    if (faction.getShortName().equals("CHH")) {
                        phenotype = Phenotype.VEHICLE;
                    }
                    break;
                case UnitType.BATTLE_ARMOR:
                    phenotype = Phenotype.ELEMENTAL;
                    break;
                case UnitType.AEROSPACEFIGHTER:
                case UnitType.CONV_FIGHTER:
                    phenotype = Phenotype.AEROSPACE;
                    break;
                case UnitType.PROTOMEK:
                    phenotype = Phenotype.PROTOMEK;
                    break;
                case UnitType.SMALL_CRAFT:
                case UnitType.DROPSHIP:
                case UnitType.JUMPSHIP:
                case UnitType.WARSHIP:
                    // The Naval Phenotype is unique to Clan Snow Raven and the Raven Alliance
                    if (faction.getShortName().equals("CSR") || faction.getShortName().equals("RA")) {
                        phenotype = Phenotype.NAVAL;
                    }
                    break;
            }

            if (!phenotype.isNone()) {
                String bloodname = Bloodname.randomBloodname(faction.getShortName(), phenotype,
                        campaign.getGameYear()).getName();
                crewName += ' ' + bloodname;
                innerMap.put(Crew.MAP_BLOODNAME, bloodname);
                innerMap.put(Crew.MAP_PHENOTYPE, phenotype.name());
            }
        }

        extraData.put(0, innerMap);

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName, Compute.getFullCrewSize(en),
                skills[0], skills[1], gender, faction.isClan(), extraData));

        en.setExternalIdAsString(UUID.randomUUID().toString());

        return en;
    }

    /**
     * Modifies the provided string-list of weight classes to not exceed the
     * indicated weight class
     *
     * @param weights   A string of single-character letter codes for the weights of
     *                  the units in
     *                  the formation, e.g. "LMMH" is one light, two medium, one
     *                  heavy
     * @param maxWeight {@link EntityWeightClass} constant with maximum weight class
     *                  allowed for the
     *                  formation
     * @return An updated version of the string with weight values replaced and/or
     *         added
     */
    private static String adjustForMaxWeight(String weights, int maxWeight) {
        if (maxWeight == EntityWeightClass.WEIGHT_HEAVY) {
            return weights.replaceAll("A", "LM");
        } else if (maxWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            return weights.replaceAll("A", "MM")
                    .replaceAll("H", "LM");
        } else if (maxWeight == EntityWeightClass.WEIGHT_LIGHT) {
            return weights.replaceAll(".", "L");
        } else {
            return weights;
        }
    }

    /**
     * Modifies the provided string-list of weight classes to not go below the
     * indicated weight
     * class
     *
     * @param weights   A string of single-character letter codes for the weights of
     *                  the units in
     *                  the formation, e.g. "LMMH" is one light, two medium, one
     *                  heavy
     * @param minWeight {@link EntityWeightClass} constant with minimum weight class
     *                  allowed for
     *                  the formation
     */
    private static String adjustForMinWeight(String weights, int minWeight) {
        if (minWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            return weights.replaceAll("L", "M");
        } else if (minWeight == EntityWeightClass.WEIGHT_HEAVY) {
            return weights.replaceAll("[LM]", "H");
        } else if (minWeight == EntityWeightClass.WEIGHT_ASSAULT) {
            return weights.replaceAll("[LMH]", "A");
        } else {
            return weights;
        }
    }

    /**
     * Adjust the weights of units in a formation for factions that do not fit the
     * typical
     * weight distribution.
     *
     * @param weights A string of single-character letter codes for the weights of
     *                the units in the lance (e.g. "LMMH")
     * @param faction The code of the faction to which the force belongs.
     * @return A new String of the same format as weights
     */
    private static String adjustWeightsForFaction(String weights, String faction) {
        /*
         * Official AtB rules only specify DC, LA, and FWL; I have added
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
     * Generates a selection of unit types, typically composing a lance, star, Level
     * II, or similar
     * tactical formation.
     * TODO: generate ProtoMek points when Clan mixed stars are called for
     * TODO: generate Clan mixed nova stars e.g. two points of Meks, two of
     * vehicles, one ProtoMek
     * point
     *
     * @param unitTypeCode The type of units to generate, also accepts
     *                     SPECIAL_UNIT_TYPE_ATB_MIX for
     *                     random Mek/vehicle/mixed lance generation
     * @param unitCount    Number of units to generate
     * @param forceQuality The equipment rating of the formation
     * @param factionCode  Short faction name
     * @param allowTanks   false to prohibit selecting ground vehicles
     * @param campaign     Current campaign
     * @return List of UnitType enum integer equivalents, length equal to unitCount.
     *         May
     *         contain duplicates.
     */
    private static List<Integer> generateUnitTypes(int unitTypeCode,
            int unitCount,
            int forceQuality,
            String factionCode,
            boolean allowTanks,
            Campaign campaign) {
        List<Integer> unitTypes = new ArrayList<>(unitCount);
        int actualUnitType = unitTypeCode;

        // This special unit type code randomly selects between all Mek, all vehicle, or
        // mixed
        // Mek/vehicle formations
        if (unitTypeCode == SPECIAL_UNIT_TYPE_ATB_MIX) {
            Faction faction = Factions.getInstance().getFaction(factionCode);

            // If ground vehicles are permitted in general and by environmental conditions,
            // and
            // for Clans if this is a Clan faction, then use them. Otherwise, only use Meks.
            if (campaign.getCampaignOptions().isUseVehicles() &&
                    allowTanks &&
                    (!faction.isClan() ||
                            (faction.isClan() && campaign.getCampaignOptions().isClanVehicles()))) {

                // some specialized logic for clan opfors
                // if we're in the late republic or dark ages, clans no longer have the luxury
                // of mek only stars
                boolean clanEquipmentScarcity = campaign.getEra()
                        .hasFlag(EraFlag.LATE_REPUBLIC, EraFlag.DARK_AGES, EraFlag.ILCLAN);
                if (faction.isClan() && !clanEquipmentScarcity) {
                    return generateClanUnitTypes(unitCount, forceQuality, factionCode, campaign);
                }

                // Use the Mek/Vehicle/Mixed ratios from campaign options as weighted values for
                // random unit types.
                // Then modify based on faction.
                int mekLanceWeight = campaign.getCampaignOptions().getOpForLanceTypeMeks();
                int mixedLanceWeight = campaign.getCampaignOptions().getOpForLanceTypeMixed();
                int vehicleLanceWeight = campaign.getCampaignOptions().getOpForLanceTypeVehicles();

                if (faction.isClan()) {
                    if (Objects.equals(factionCode, "CHH")) {
                        mixedLanceWeight++;
                        vehicleLanceWeight++;
                    } else {
                        mekLanceWeight++;
                        mixedLanceWeight = Math.max(0, mixedLanceWeight - 1);
                        vehicleLanceWeight = Math.max(0, vehicleLanceWeight - 1);
                    }
                } else if (faction.isMinorPower() || faction.isPirate()) {
                    mekLanceWeight = Math.max(0, mekLanceWeight - 1);
                    mixedLanceWeight++;
                    vehicleLanceWeight++;
                }

                int totalWeight = mekLanceWeight + mixedLanceWeight + vehicleLanceWeight;

                // Roll for unit types
                if (totalWeight <= 0) {
                    actualUnitType = UnitType.MEK;
                } else {
                    int roll = Compute.randomInt(totalWeight);
                    if (roll < vehicleLanceWeight) {
                        actualUnitType = UnitType.TANK;
                    // Mixed units randomly select between Mek or ground vehicle
                    } else if (roll < vehicleLanceWeight + mixedLanceWeight) {
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
            } else {
                actualUnitType = UnitType.MEK;
            }
        } else if (unitTypeCode == SPECIAL_UNIT_TYPE_ATB_CIVILIANS) {
            // Use the Vehicle/Mixed ratios from campaign options as weighted values for
            // random unit types.
            int vehicleLanceWeight = campaign.getCampaignOptions().getOpForLanceTypeVehicles();
            int mixedLanceWeight = campaign.getCampaignOptions().getOpForLanceTypeMixed();

            int totalWeight = mixedLanceWeight + vehicleLanceWeight;

            // Roll for unit types
            if (totalWeight <= 0) {
                actualUnitType = UnitType.TANK;
            } else {
                int roll = Compute.randomInt(totalWeight);
                if (roll < vehicleLanceWeight) {
                    actualUnitType = UnitType.TANK;
                // Mixed units randomly select between Mek or ground vehicle
                } else {
                    for (int x = 0; x < unitCount; x++) {
                        boolean addTank = Compute.randomInt(2) == 0;
                        if (addTank) {
                            unitTypes.add(UnitType.TANK);
                        } else {
                            unitTypes.add(UnitType.MEK);
                        }
                    }
                    return unitTypes;
                }
            }
        }

        // Add unit types to the list of actual unity types
        for (int x = 0; x < unitCount; x++) {
            unitTypes.add(actualUnitType);
        }

        return unitTypes;
    }

    /**
     * Generates a selection of unit types, typically for a Clan star of five
     * points. May generate
     * ground vehicles, provided the option for Clan vehicles is set in Campaign
     * options.
     * TODO: Clan vehicle points are two vehicles, and vehicle stars are 10 vehicles
     * total
     *
     * @param unitCount    Number of units to generate (typically 'points')
     * @param forceQuality {@link IUnitRating} constant with equipment rating of the
     *                     formation
     * @param factionCode  Short faction name
     * @param campaign     Current campaign
     * @return List of UnitType constants, one for each requested
     */
    private static List<Integer> generateClanUnitTypes(int unitCount,
            int forceQuality,
            String factionCode,
            Campaign campaign) {
        // Certain clans are more likely to use vehicles, while the rest relegate them
        // to the
        // lowest rated
        int vehicleTarget = 6;
        if (factionCode.equals("CHH") || factionCode.equals("CSL") || factionCode.equals("CBS")) {
            vehicleTarget = 8;
        } else {
            vehicleTarget -= forceQuality;
        }

        // Random determination of Mek or ground vehicle
        int roll = Compute.d6(2);
        int unitType = campaign.getCampaignOptions().isClanVehicles() && (roll <= vehicleTarget) ? UnitType.TANK
                : UnitType.MEK;
        List<Integer> unitTypes = new ArrayList<>();
        for (int x = 0; x < unitCount; x++) {
            unitTypes.add(unitType);
        }
        return unitTypes;
    }

    /**
     * Generates a string indicating the weights of individual units in a formation,
     * such as "LLMH"
     * (two light class, one medium class, one heavy class), based on AtB
     * guidelines. The selected
     * weight classes include adjustments for unit types:
     * <ul>
     * <li>Aerospace fighters do not have an assault weight class</li>
     * </ul>
     * <br/>
     * Certain roles have either explicit or implicit weight restrictions:
     * <ul>
     * <li>RECON with Meks and ProtoMeks is limited to light and medium weight
     * classes</li>
     * <li>APC should include light and medium weights, as few heavy/assault weight
     * classes
     * include infantry bays</li>
     * <li>CAVALRY is limited to heavy weight class and lighter with Meks and
     * ProtoMeks, and
     * medium weight class and lighter with vehicles. Heavy cavalry Meks are rare
     * without
     * advanced technology and may fail to generate a random unit.</li>
     * <li>RAIDER is limited to heavy weight class and lighter for Meks and
     * ProtoMeks</li>
     * </ul>
     *
     * @param unitTypes     List of unit types (mek, tank, etc.)
     * @param faction       Faction for unit generation
     * @param weightClass   "Base" weight class, drives the generated weights with
     *                      some variation
     * @param minWeight     Fixed minimum weight class
     * @param maxWeight     Fixed maximum weight class
     * @param requiredRoles Lists of required roles for generated units
     * @param campaign      Current campaign
     * @return String with number of characters equal to standard formation size for
     *         faction parameter
     */
    private static @Nullable String generateUnitWeights(List<Integer> unitTypes,
            String faction,
            int weightClass,
            int maxWeight,
            int minWeight,
            Map<Integer, Collection<MissionRole>> requiredRoles,
            Campaign campaign) {

        Faction genFaction = Factions.getInstance().getFaction(faction);
        final String factionWeightString;
        if (genFaction.isClan() || genFaction.isMarianHegemony()) {
            factionWeightString = AtBConfiguration.ORG_CLAN;
        } else if (genFaction.isComStarOrWoB()) {
            factionWeightString = AtBConfiguration.ORG_CS;
        } else {
            factionWeightString = AtBConfiguration.ORG_IS;
        }
        String weights = campaign.getAtBConfig().selectBotUnitWeights(factionWeightString, weightClass);
        if (weights == null) {
            logger.error(String.format("Failed to generate weights for faction %s with weight class %s",
                    factionWeightString, weightClass));
            return null;
        }

        // Modify weight classes to account for the provided maximum/minimum
        weights = adjustForMaxWeight(weights, maxWeight);
        weights = adjustForMinWeight(weights, minWeight);

        // Aerospace fighter weight cap
        if (unitTypes.contains(UnitType.AEROSPACEFIGHTER)) {
            weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_HEAVY);
        }

        // Role handling

        if (requiredRoles != null && !requiredRoles.isEmpty()) {
            for (int curType : requiredRoles.keySet()) {

                if (requiredRoles.get(curType).contains(MissionRole.RECON)) {
                    if (curType == UnitType.MEK || curType == UnitType.PROTOMEK) {
                        weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_MEDIUM);
                    }
                }

                if (requiredRoles.get(curType).contains(MissionRole.APC)) {
                    if (curType == UnitType.TANK || curType == UnitType.VTOL) {
                        weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_MEDIUM);
                    }
                }

                if (requiredRoles.get(curType).contains(MissionRole.CAVALRY)) {
                    if (curType == UnitType.MEK) {
                        weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_HEAVY);
                    } else if (curType == UnitType.TANK || curType == UnitType.PROTOMEK) {
                        weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_MEDIUM);
                    }
                }

                if (requiredRoles.get(curType).contains(MissionRole.RAIDER)) {
                    if (curType == UnitType.MEK || curType == UnitType.PROTOMEK) {
                        weights = adjustForMaxWeight(weights, EntityWeightClass.WEIGHT_HEAVY);
                    }
                }

            }
        }

        if (campaign.getCampaignOptions().isRegionalMekVariations()) {
            weights = adjustWeightsForFaction(weights, faction);
        }

        return weights;
    }

    /**
     * Calculates from scratch the current effective player and allied BV present in
     * the given scenario.
     *
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario resides.
     * @return Effective BV.
     */
    public static int calculateEffectiveBV(AtBDynamicScenario scenario, Campaign campaign,
                                           boolean forceStandardBattleValue) {
        // for each deployed player and bot force that's marked as contributing to the
        // BV budget
        int bvBudget = 0;
        double difficultyMultiplier = getDifficultyMultiplier(campaign);

        String generationMethod = campaign.getCampaignOptions().isUseGenericBattleValue() ?
            "Generic BV" : "BV2";

        // deployed player forces:
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            if (forceTemplate != null && forceTemplate.getContributesToBV()) {
                bvBudget += campaign.getForce(forceID).getTotalBV(campaign, forceStandardBattleValue);
            }
        }

        // deployed individual player units
        for (UUID unitID : scenario.getIndividualUnitIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerUnitTemplates().get(unitID);
            if ((forceTemplate != null) && forceTemplate.getContributesToBV()) {
                if (campaign.getCampaignOptions().isUseGenericBattleValue() && !forceStandardBattleValue) {
                    bvBudget += campaign.getUnit(unitID).getEntity().getGenericBattleValue();
                } else {
                    bvBudget += campaign.getUnit(unitID).getEntity().calculateBattleValue();
                }
            }
        }

        double bvMultiplier = scenario.getEffectivePlayerBVMultiplier();

        if (bvMultiplier > 0) {
            bvBudget = (int) round(bvBudget * scenario.getEffectivePlayerBVMultiplier() * difficultyMultiplier);
        } else {
            bvBudget = (int) round(bvBudget * difficultyMultiplier);
        }

        logger.info(String.format("Total Player %s: %s (adjusted for campaign difficulty)", generationMethod, bvBudget));

        // allied bot forces that contribute to BV do not get multiplied by the
        // difficulty even if the player is perfect, the AI doesn't get any better
        for (int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if (forceTemplate != null && forceTemplate.getContributesToBV()) {
                bvBudget += botForce.getTotalBV(campaign);

                logger.info(String.format("%s %s: %s",
                    botForce.getName(), generationMethod, botForce.getTotalBV(campaign)));
            }
        }

        logger.info(String.format("Total Base %s Budget: %s", generationMethod, bvBudget));

        return bvBudget;
    }

    /**
     * Calculates the current effective player and allied unit count present in the given scenario.
     *
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario resides.
     * @param isClanBidding {@link Boolean} flag indicating if Clan bidding is enabled.
     * @return The effective unit count for the scenario.
     */
    public static int calculateEffectiveUnitCount(AtBDynamicScenario scenario, Campaign campaign,
                                                  boolean isClanBidding) {
        // for each deployed player and bot force that's marked as contributing to the
        // unit count budget
        int unitCount = 0;
        double difficultyMultiplier = getDifficultyMultiplier(campaign);

        // deployed player forces:
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            Force force = campaign.getForce(forceID);

            if (forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
                unitCount += force.getTotalUnitCount(campaign, isClanBidding);
            }
        }

        // deployed individual player units
        for (UUID unitID : scenario.getIndividualUnitIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerUnitTemplates().get(unitID);
            if ((forceTemplate != null) && forceTemplate.getContributesToUnitCount()) {
                unitCount++;
            }
        }

        // the player unit count is now multiplied by the difficulty multiplier
        unitCount = (int) Math.floor((double) unitCount * difficultyMultiplier);

        // allied bot forces that contribute to BV do not get multiplied by the
        // difficulty even if the player is good, the AI doesn't get any better
        for (int index = 0; index < scenario.getNumBots(); index++) {
            BotForce botForce = scenario.getBotForce(index);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(botForce);
            if (forceTemplate != null && forceTemplate.getContributesToUnitCount()) {
                unitCount += botForce.getFullEntityList(campaign).size();
            }
        }

        return unitCount;
    }

    /**
     * Calculates the difficulty multiplier based on campaign options.
     *
     * @param campaign the campaign for which to calculate the difficulty multiplier
     * @return the difficulty multiplier
     */
    private static double getDifficultyMultiplier(Campaign campaign) {
        return switch (campaign.getCampaignOptions().getSkillLevel()) {
            case NONE, ULTRA_GREEN -> 0.5;
            case GREEN -> 0.75;
            case REGULAR -> 1.0;
            case VETERAN -> 1.25;
            case ELITE -> 1.5;
            case HEROIC -> 1.75;
            case LEGENDARY -> 2.0;
        };
    }

    /**
     * Generates a random force weight for a given scenario.
     *
     * @return the randomly generated {@link EntityWeightClass}
     */
    public static int randomForceWeight() {
        int roll = Compute.randomInt(89);

        // These values are based the random force weight table found on page 265 of Total Warfare
        if (roll < 19) { // 19%
            return EntityWeightClass.WEIGHT_LIGHT;
        } else if (roll < 50) { // 31%
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (roll < 75) { // 25%
            return EntityWeightClass.WEIGHT_HEAVY;
        } else { // 14%
            return EntityWeightClass.WEIGHT_ASSAULT;
        }
    }

    /**
     * Generates a lance or similar tactical grouping (star, Level II, etc.) of
     * entities with given
     * parameters.
     * This overload is included as a convenience for when weight class is not
     * important or doesn't
     * apply to the desired entity types.
     *
     * @param faction     The faction from which to generate entities.
     * @param skill       {@link SkillLevel} target for all units
     * @param quality     Quality of the units.
     * @param unitTypes   List of {@link UnitType}, one for each unit to generate
     * @param rolesByType Collections of roles required for each unit type
     * @param campaign    working campaign.
     * @return List of entities created for this lance/tactical group
     */
    private static List<Entity> generateLance(String faction,
            SkillLevel skill,
            int quality,
            List<Integer> unitTypes,
            Map<Integer, Collection<MissionRole>> rolesByType,
            Campaign campaign, Scenario scenario) {

        List<Entity> generatedEntities = new ArrayList<>();

        for (int i = 0; i < unitTypes.size(); i++) {
            Entity newEntity = getEntity(faction,
                    skill,
                    quality,
                    unitTypes.get(i),
                    UNIT_WEIGHT_UNSPECIFIED,
                    rolesByType.getOrDefault(unitTypes.get(i), new ArrayList<>()),
                    campaign);

            if (newEntity == null) {
                // We reset the count locally, so that we're passing through the entire list.
                // The idea is that this will give us the best chance of hitting a valid unit.
                int freshIteration = 0;

                while (freshIteration < unitTypes.size()) {
                    newEntity = getEntity(faction,
                        skill,
                        quality,
                        unitTypes.get(i),
                        UNIT_WEIGHT_UNSPECIFIED,
                        rolesByType.getOrDefault(unitTypes.get(i), new ArrayList<>()),
                        campaign);

                    if (newEntity != null) {
                        break;
                    }

                    freshIteration++;
                }

                // If we still haven't got a valid entity, use hardcoded fallbacks.
                if (newEntity == null) {
                    if (unitTypes.get(0) == UnitType.DROPSHIP) {
                        newEntity = getEntity(faction,
                            skill,
                            quality,
                            UnitType.DROPSHIP,
                            UNIT_WEIGHT_UNSPECIFIED,
                            List.of(CIVILIAN),
                            campaign);
                    } else {
                        if (scenario.getBoardType() == T_GROUND) {
                            getEntity(faction,
                                skill,
                                quality,
                                UnitType.TANK,
                                UNIT_WEIGHT_UNSPECIFIED,
                                null,
                                campaign);
                        }
                    }
                }
            }

            if (newEntity != null) {
                generatedEntities.add(newEntity);
            }
        }

        return generatedEntities;
    }

    /**
     * Generates a lance or similar tactical grouping (star, Level II, etc.) of
     * entities with given
     * parameters. The number of entities generated is the lowest of number of unit
     * types or number
     * of provided weight classes.
     *
     * @param faction     The faction from which to generate entities.
     * @param skill       {@link SkillLevel} target for all units
     * @param quality     Quality of the units.
     * @param unitTypes   List of {@link UnitType}, one for each unit to generate;
     *                    should be the same
     *                    length as list of weights
     * @param weights     Weight class string suitable for
     *                    AtBConfiguration.decodeWeightStr
     *                    e.g. "LMMH" generates one light, two medium, and one heavy
     * @param rolesByType Collections of roles required for each unit type
     * @param campaign    Working campaign
     * @return List of entities created for this lance/tactical group
     */
    private static List<Entity> generateLance(String faction,
            SkillLevel skill,
            int quality,
            List<Integer> unitTypes,
            String weights,
            Map<Integer, Collection<MissionRole>> rolesByType,
            Campaign campaign, AtBScenario scenario) {
        List<Entity> generatedEntities = new ArrayList<>();

        // If the number of unit types and number of weight classes don't match,
        // generate the lower
        // of the two counts
        int unitTypeSize = unitTypes.size();
        if (unitTypeSize > weights.length()) {
            logger.error(
                    String.format("More unit types (%d) provided than weights (%d). Truncating generated lance.",
                            unitTypes.size(),
                            weights.length()));
            unitTypeSize = weights.length();
        }

        for (int i = 0; i < unitTypeSize; i++) {
            Entity newEntity = getNewEntity(faction, skill, quality, unitTypes, weights, rolesByType,
                campaign, i);

            if (newEntity == null) {
                // We reset the count locally, so that we're passing through the entire list.
                // The idea is that this will give us the best chance of hitting a valid unit.
                int freshIteration = 0;

                while (freshIteration < unitTypeSize) {
                    newEntity = getNewEntity(faction, skill, quality, unitTypes, weights, rolesByType,
                        campaign, freshIteration);

                    if (newEntity != null) {
                        break;
                    }

                    freshIteration++;
                }

                // If we still haven't got a valid entity, use hardcoded fallbacks.
                if (newEntity == null) {
                    if (unitTypes.get(0) == UnitType.DROPSHIP) {
                        newEntity = getNewEntity(faction, skill, quality, List.of(UnitType.TANK),
                            weights, Map.of(UnitType.DROPSHIP, List.of(CIVILIAN)),
                            campaign, 0);
                    } else {
                        if (scenario.getBoardType() == T_GROUND) {
                            newEntity = getNewEntity(faction, skill, quality, List.of(UnitType.TANK),
                                weights, null, campaign, 0);
                        }
                    }
                }
            }

            if (newEntity != null) {
                generatedEntities.add(newEntity);
            }
        }

        return generatedEntities;
    }

    /**
     * Retrieve a new instance of Entity with the given parameters.
     *
     * @param faction       the faction of the entity
     * @param skill         the skill level of the entity
     * @param quality       the quality of the entity
     * @param unitTypes     the list of unit types
     * @param weights       the unit weights string
     * @param rolesByType   the mapping of unit types to mission roles
     * @param campaign      the campaign associated with the entity
     * @param i             the index of the unit type in the unitTypes list
     *
     * @return a new instance of Entity with the specified parameters
     */
    private static Entity getNewEntity(String faction, SkillLevel skill, int quality,
                                       List<Integer> unitTypes, String weights,
                                       @Nullable Map<Integer, Collection<MissionRole>> rolesByType,
                                       Campaign campaign, int i) {
        Collection<MissionRole> roles;

        if (rolesByType != null) {
            roles = rolesByType.getOrDefault(unitTypes.get(i), new ArrayList<>());
        } else {
            roles = null;
        }
        return getEntity(faction, skill, quality, unitTypes.get(i),
            AtBConfiguration.decodeWeightStr(weights, i), roles, campaign);
    }

    /**
     * Worker method that sets bot force properties such as name, color, team
     *
     * @param generatedForce The force for which to set parameters
     * @param forceTemplate  The force template from which to set parameters
     * @param contract       The contract from which to set parameters
     */
    private static void setBotForceParameters(BotForce generatedForce, ScenarioForceTemplate forceTemplate,
            ForceAlignment forceAlignment, AtBContract contract) {
        if (forceAlignment == ForceAlignment.Allied) {
            generatedForce.setName(String.format("%s %s", contract.getAllyBotName(), forceTemplate.getForceName()));
            generatedForce.setColour(contract.getAllyColour());
            generatedForce.setCamouflage(contract.getAllyCamouflage().clone());
        } else if (forceAlignment == ForceAlignment.Opposing) {
            generatedForce.setName(String.format("%s %s", contract.getEnemyBotName(), forceTemplate.getForceName()));
            generatedForce.setColour(contract.getEnemyColour());
            generatedForce.setCamouflage(contract.getEnemyCamouflage().clone());
        } else {
            generatedForce.setName("Unknown Hostiles");
        }

        generatedForce.setTeam(ScenarioForceTemplate.TEAM_IDS.get(forceAlignment.ordinal()));
    }

    /**
     * Worker method that calculates the destination zones for all the bot forces in
     * a given scenario.
     * Note that it is advisable to call it only after setDeploymentZones has been
     * called on the scenario,
     * as otherwise you'll have "unpredictable" destination zones.
     *
     * @param scenario
     */
    private static void setDestinationZones(AtBDynamicScenario scenario) {
        for (BotForce generatedForce : scenario.getBotForceTemplates().keySet()) {
            setDestinationZone(generatedForce, scenario.getBotForceTemplates().get(generatedForce));
        }
    }

    /**
     * Worker method that calculates the deployment zones for all templates in the
     * given scenario
     * and applies the results to the scenario's bot forces
     *
     * @param scenario The scenario to process
     */
    private static void setDeploymentZones(AtBDynamicScenario scenario) {
        for (ScenarioForceTemplate forceTemplate : scenario.getTemplate().getAllScenarioForces()) {
            calculateDeploymentZone(forceTemplate, scenario, forceTemplate.getForceName());
        }

        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce botForce = scenario.getBotForce(botIndex);
            botForce.setStartingPos(scenario.getBotForceTemplates().get(botForce).getActualDeploymentZone());
        }
    }

    /**
     * Worker method that calculates the deployment zone of a given force template
     * and any force templates with which it is synced.
     *
     * @param forceTemplate           The force template for which to generate
     *                                deployment zone
     * @param scenario                The scenario on which we're working
     * @param originalForceTemplateID The ID of the force template where we started.
     * @return Deployment zone as defined in Board.java
     */
    public static int calculateDeploymentZone(ScenarioForceTemplate forceTemplate, AtBDynamicScenario scenario,
            String originalForceTemplateID) {
        int calculatedEdge = Board.START_ANY;

        // if we got in here without a force template somehow, just return a random
        // start zone
        if (forceTemplate == null) {
            return Compute.randomInt(Board.START_CENTER);
            // if we have a specific calculated deployment zone already
        } else if (forceTemplate.getActualDeploymentZone() != Board.START_NONE) {
            return forceTemplate.getActualDeploymentZone();
            // if we have a chain of deployment-synced forces that forms a loop and have
            // looped around once, avoid endless loops
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.None ||
                Objects.equals(forceTemplate.getSyncedForceName(), originalForceTemplateID)) {
            calculatedEdge = forceTemplate.getDeploymentZones()
                    .get(Compute.randomInt(forceTemplate.getDeploymentZones().size()));
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.SameEdge) {
            calculatedEdge = calculateDeploymentZone(
                    scenario.getTemplate().getScenarioForces().get(forceTemplate.getSyncedForceName()), scenario,
                    originalForceTemplateID);
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.OppositeEdge) {
            int syncDeploymentZone = calculateDeploymentZone(
                    scenario.getTemplate().getScenarioForces().get(forceTemplate.getSyncedForceName()), scenario,
                    originalForceTemplateID);
            calculatedEdge = getOppositeEdge(syncDeploymentZone);
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.SameArc) {
            int syncDeploymentZone = calculateDeploymentZone(
                    scenario.getTemplate().getScenarioForces().get(forceTemplate.getSyncedForceName()), scenario,
                    originalForceTemplateID);
            List<Integer> arc = getArc(syncDeploymentZone, true);
            calculatedEdge = arc.get(Compute.randomInt(arc.size()));
        } else if (forceTemplate.getSyncDeploymentType() == SynchronizedDeploymentType.OppositeArc) {
            int syncDeploymentZone = calculateDeploymentZone(
                    scenario.getTemplate().getScenarioForces().get(forceTemplate.getSyncedForceName()), scenario,
                    originalForceTemplateID);
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
     * Determines and sets the destination edge for a given bot force that follows a
     * given force template.
     *
     * @param force         The bot force for which to set the edge.
     * @param forceTemplate The template which governs the destination edge.
     */
    public static void setDestinationZone(BotForce force, ScenarioForceTemplate forceTemplate) {
        int actualDestinationEdge = forceTemplate.getDestinationZone();

        // set the 'auto flee' flag to true if the bot has a destination edge
        if (actualDestinationEdge != CardinalEdge.NONE.getIndex()) {
            force.getBehaviorSettings().setAutoFlee(true);
        }

        if (forceTemplate.getDestinationZone() == ScenarioForceTemplate.DESTINATION_EDGE_RANDOM) {
            // compute a random cardinal edge between 0 and 3 to avoid None
            actualDestinationEdge = Compute.randomInt(CardinalEdge.values().length - 1);
        } else if (forceTemplate.getDestinationZone() == ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT) {
            actualDestinationEdge = getOppositeEdge(force.getStartingPos());
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
        // assemble a list of all entities that have an "STAGGERED" arrival turn into a
        // list
        // then run setDeploymentTurnsStaggered on them
        List<Entity> staggeredEntities = new ArrayList<>();

        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce currentBotForce = scenario.getBotForce(x);
            for (Entity entity : currentBotForce.getFullEntityList(campaign)) {
                if (entity.getDeployRound() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED) {
                    staggeredEntities.add(entity);
                }
            }
        }

        for (int forceID : scenario.getForceIDs()) {
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits(true)) {
                Unit currentUnit = campaign.getUnit(unitID);
                if (currentUnit != null
                        && (currentUnit.getEntity().getDeployRound() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED)) {
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
     *
     * @param scenario The scenario to process
     * @param campaign A pointer to the campaign
     */
    private static void setDeploymentTurns(AtBDynamicScenario scenario, Campaign campaign) {
        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce currentBotForce = scenario.getBotForce(x);
            ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(currentBotForce);
            setDeploymentTurns(currentBotForce, forceTemplate, scenario, campaign);
        }
    }

    /**
     * Sets up deployment turns for all bot units within the specified bot force
     * according to the specified force template's rules.
     * Also makes use of the given scenarios reinforcement delay modifier.
     *
     * ARRIVAL_TURN_STAGGERED_BY_LANCE is not implemented.
     * ARRIVAL_TURN_STAGGERED is processed just prior to scenario start instead (?)
     */
    public static void setDeploymentTurns(BotForce botForce, ScenarioForceTemplate forceTemplate,
            AtBDynamicScenario scenario, Campaign campaign) {
        // deployment turns don't matter for transported entities
        List<Entity> untransportedEntities = scenario.filterUntransportedUnits(botForce.getFullEntityList(campaign));

        if (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_STAGGERED_BY_LANCE) {
            setDeploymentTurnsStaggeredByLance(untransportedEntities);
        } else if (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS) {
            if (forceTemplate.getForceAlignment() == ForceAlignment.Opposing.ordinal()) {
                setDeploymentTurnsForReinforcements(untransportedEntities,
                        scenario.getHostileReinforcementDelayReduction());
            } else if (forceTemplate.getForceAlignment() != ForceAlignment.Third.ordinal()) {
                setDeploymentTurnsForReinforcements(untransportedEntities,
                        scenario.getFriendlyReinforcementDelayReduction());
            } else {
                setDeploymentTurnsForReinforcements(untransportedEntities, 0);
            }
        } else {
            for (Entity entity : untransportedEntities) {
                entity.setDeployRound(forceTemplate.getArrivalTurn());
            }
        }
    }

    /**
     * Set up deployment turns for player units as specified in the scenario's
     * template.
     * Note that this is currently invoked during the BriefingTab.startScenario()
     * method,
     * as that method resets all properties of for each player entity. Hence it
     * being public.
     *
     * @param scenario The scenario to process.
     * @param campaign The campaign in which the scenario is occurring.
     */
    public static void setPlayerDeploymentTurns(AtBDynamicScenario scenario, Campaign campaign) {
        // make note of battle commander strategy
        int strategy = scenario.getLanceCommanderSkill(SkillType.S_STRATEGY, campaign);

        // for player forces where there's an associated force template, we can set the
        // deployment turn explicitly
        // or use a stagger algorithm.
        // for player forces where there's not an associated force template, we
        // calculate the deployment turn
        // as if they were reinforcements
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            List<Entity> forceEntities = new ArrayList<>();
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits(true)) {
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
                    setDeploymentTurnsForReinforcements(forceEntities,
                            strategy + scenario.getFriendlyReinforcementDelayReduction());
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
        // for player forces where there's an associated force template, we can set the
        // deployment zone explicitly
        for (int forceID : scenario.getForceIDs()) {
            ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);
            List<Entity> forceEntities = new ArrayList<>();
            Force playerForce = campaign.getForce(forceID);

            for (UUID unitID : playerForce.getAllUnits(true)) {
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
     * Uses the "individual staggered deployment" algorithm to determine individual
     * deployment turns
     *
     * @param entityList   List of entities to process. May be from many players.
     * @param turnModifier The deployment round is reduced by this amount
     */
    private static void setDeploymentTurnsStaggered(List<Entity> entityList, int turnModifier) {
        // loop through all the entities
        // highest movement entity deploys on turn 0
        // other entities deploy on highest move - "walk" MP.
        int maxWalkMP = -1;
        List<Integer> entityWalkMPs = new ArrayList<>();

        for (Entity entity : entityList) {
            // AtB has a legacy mekanism where units with jump jets are counted a little
            // faster
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

            // since we're iterating through the same unchanged collection, we can use
            // implicit indexing.
            entity.setDeployRound(Math.max(0, maxWalkMP - entityWalkMPs.get(x) - actualTurnModifier));
        }
    }

    /**
     * Given a list of entities, set the arrival turns for them as if they were all
     * reinforcements on the same side.
     *
     * @param entityList   List of entities to process
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

            // don't reduce minimum speed to 0, since dividing by zero further down is
            // problematic
            if ((speed < minimumSpeed) && (speed > 0)) {
                minimumSpeed = speed;
            }
        }

        // the actual arrival turn will be the scale divided by the slowest speed.
        // so, a group of Atlases (3/5) should arrive on turn 10 (30 / 3)
        // a group of jump-capable Griffins (5/8/5) should arrive on turn 5 (30 / 6)
        // a group of Ostscouts (8/12/8) should arrive on turn 3 (30 / 9, rounded down)
        // we then subtract the passed-in turn modifier, which is usually the
        // commander's strategy skill level.
        int actualArrivalTurn = Math.max(0, (REINFORCEMENT_ARRIVAL_SCALE / minimumSpeed) - turnModifier);

        for (Entity entity : entityList) {
            entity.setDeployRound(actualArrivalTurn);
        }
    }

    /**
     * Uses the "lance staggered deployment" algorithm to determine individual
     * deployment turns
     * Not actually implemented currently.
     *
     * @param entityList The list of entities to process.
     */
    private static void setDeploymentTurnsStaggeredByLance(List<Entity> entityList) {
        logger.warn("Deployment Turn - Staggered by Lance not implemented");
    }

    /**
     * Worker function that calculates the AtB-rules walk MP for an entity, for
     * deployment purposes.
     *
     * @param entity The entity to examine.
     * @return The walk MP.
     */
    private static int calculateAtBSpeed(Entity entity) {
        int speed = entity.getWalkMP();
        if (entity.getJumpMP() > 0) {
            if (entity instanceof Infantry) {
                speed = entity.getJumpMP();
            } else {
                speed++;
            }
        }

        return speed;
    }

    /**
     * Method to compute an "arc" of deployment zones next to or opposite a
     * particular edge.
     * e.g. Northeast comes back with a list of north, northeast, east
     *
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

        switch (tempEdge) {
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
     *
     * @param edge The starting edge
     * @return Opposite edge, as defined in Board.java
     */
    public static int getOppositeEdge(int edge) {
        return switch (edge) {
            case Board.START_EDGE -> Board.START_CENTER;
            case Board.START_CENTER -> Board.START_EDGE;
            case Board.START_ANY -> Board.START_ANY;
            default ->
                // directional edges start at 1
                ((edge + 3) % 8) + 1;
        };
    }

    /**
     * Worker function that calculates the appropriate number of rerolls to use for
     * the scenario.
     *
     * @param scenario The scenario for which to set rerolls
     * @param campaign Campaign in which the scenario is occurring
     */
    private static void setScenarioRerolls(AtBDynamicScenario scenario, Campaign campaign) {
        int tacticsSkill = scenario.getLanceCommanderSkill(SkillType.S_TACTICS, campaign);

        scenario.setRerolls(tacticsSkill);
    }

    /**
     * Convenience function to get the standard ground tactical formation size,
     * based on faction. In
     * the case of Clan factions, this returns the number of points rather than a
     * number of units,
     * as points may be 2 ground vehicles or 5 ProtoMeks.
     * TODO: conventional infantry typically uses 3 units per formation (company) -
     * make a separate method
     *
     * @param factionCode string with faction short name/lookup key
     * @return Number of units (points for Clan) in the formation
     */
    public static int getLanceSize(String factionCode) {
        Faction faction = Factions.getInstance().getFaction(factionCode);
        if (faction != null) {
            if (faction.isClan() || faction.isMarianHegemony()) {
                // Clans and the Marian Hegemony use a fundamental unit size of 5.
                return CLAN_MH_LANCE_SIZE;
            } else if (faction.isComStarOrWoB()) {
                // ComStar and WoB use a fundamental unit size of 6.
                return COMSTAR_LANCE_SIZE;
            }
        }

        return IS_LANCE_SIZE;
    }

    /**
     * Worker function to determine the formation size of fixed wing aircraft.
     * Directly calling for
     * aerospace fighters will return a single flight/point size, normally 2 except
     * for CC which
     * uses 3 per flight. Conventional fighters return 1-3 flights/2-6 total. The
     * SPECIAL_UNIT_TYPE_ATB_AERO_MIX unit type randomly returns an aerospace flight
     * or conventional
     * squadron.
     *
     * @param unitTypeCode  type of unit may be aerospace, conventional, or ATB
     *                      'special'
     * @param isPlanetOwner true if the generating faction controls the system,
     *                      which is required
     *                      to generate conventional fighters
     * @param factionCode   Short name of faction
     * @return Number of fighters to use as a formation size
     */
    public static int getAeroLanceSize(int unitTypeCode, boolean isPlanetOwner, String factionCode) {
        int numFightersPerFlight = factionCode.equals("CC") ? 3 : 2;

        // If this is the planet owner, it may generate a full squadron of conventional
        // fighters
        int useASFRoll = isPlanetOwner ? Compute.d6() : 6;
        int weightCountRoll = (Compute.randomInt(3) + 1) * numFightersPerFlight;
        return getAeroLanceSize(unitTypeCode, numFightersPerFlight, weightCountRoll, useASFRoll);
    }

    /**
     * Unwrapped inner logic of above function to be deterministic, for testing
     * purposes.
     *
     * @param unitTypeCode         {@link UnitType} value, should be
     *                             AEROSPACEFIGHTER,
     *                             CONV_FIGHTER, or SPECIAL_UNIT_TYPE_ATB_AERO_MIX.
     * @param numFightersPerFlight Number of fighters per flight/point, typically 2
     * @param weightCountRoll      Number of fighters per squadron/star, typically 6
     *                             or 10
     * @param useASFRoll           test value for dynamic generation of aerospace or
     *                             conventional
     * @return flight size for aerospace, squadron size for conventional
     */
    public static int getAeroLanceSize(int unitTypeCode, int numFightersPerFlight, int weightCountRoll,
            int useASFRoll) {
        if (unitTypeCode == UnitType.AEROSPACEFIGHTER) {
            return numFightersPerFlight;
        } else if (unitTypeCode == UnitType.CONV_FIGHTER) {
            return weightCountRoll;
        } else {
            // if we are the planet owner, we may use ASF or conventional fighters
            boolean useASF = useASFRoll >= 4;
            // if we are using ASF, we "always" use 2 at a time, otherwise, use the # of
            // conventional fighters
            return useASF ? numFightersPerFlight : weightCountRoll;
        }
    }

    /**
     * Helper function that deploys the given units off board a random distance 1-2
     * boards in a random direction
     *
     * @param entityList
     */
    private static void deployArtilleryOffBoard(List<Entity> entityList) {
        OffBoardDirection direction = OffBoardDirection.getDirection(Compute.randomInt(4));
        int distance = (Compute.randomInt(2) + 1) * 17;

        for (Entity entity : entityList) {
            entity.setOffBoard(distance, direction);
        }
    }

    /**
     * Helper function that puts the units in the given list at the given altitude.
     * Use with caution, as may lead to splattering or aerospace units starting on
     * the ground.
     *
     * @param entityList       The entity list to process.
     * @param startingAltitude Starting altitude.
     */
    private static void setStartingAltitude(List<Entity> entityList, int startingAltitude) {
        for (Entity entity : entityList) {
            if (entity instanceof IAero) {
                entity.setAltitude(startingAltitude);

                // there's a lot of stuff that happens whan an aerospace unit
                // "lands", so let's make sure it all happens
                if (startingAltitude == 0) {
                    ((IAero) entity).land();
                }
            }
        }
    }

    /**
     * This method contains various hacks intended to put "special units"
     * such as LAMs, VTOLs and WIGEs into a reasonable state that the bot can use
     */
    private static void correctNonAeroFlyerBehavior(List<Entity> entityList, int boardType) {
        for (Entity entity : entityList) {
            boolean inSpace = boardType == AtBScenario.T_SPACE;
            boolean inAtmo = boardType == AtBScenario.T_ATMOSPHERE;

            // hack for land-air meks
            if (entity instanceof LandAirMek) {
                if (inSpace || inAtmo) {
                    entity.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
                } else {
                    // for now, the bot does not know how to use WIGEs, so go as a mek
                    entity.setConversionMode(LandAirMek.CONV_MODE_MEK);
                }
            }

            // hack - set helis and WIGEs to an explicit altitude of 1
            // currently there is no support for setting elevation for "ground" units
            // in the scenario template editor, but it looks dumb to have choppers
            // start out on the ground
            if ((entity.getMovementMode() == EntityMovementMode.VTOL) ||
                    (entity.getMovementMode() == EntityMovementMode.WIGE)) {
                entity.setElevation(1);
            }
        }
    }

    /**
     * Worker function that returns the faction code of the first owner of the
     * planet where the contract is taking place.
     *
     * @param contract    Current contract.
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
            Faction ownerFaction = Factions.getInstance().getFaction(factionCode);

            if (ownerFaction.is(Tag.ABANDONED)) {
                factionCode = "MERC";
            }
        }

        return factionCode;
    }

    /**
     * Worker function that determines the ForceAlignment of the specified faction.
     *
     * @param contract    Current contract, for determining the planet we're on.
     * @param factionCode Faction code to check.
     * @param currentDate Current date.
     * @return ForceAlignment.
     */
    private static ForceAlignment getPlanetOwnerAlignment(AtBContract contract, String factionCode,
            LocalDate currentDate) {
        // if the faction is one of the planet owners, see if it's either the employer
        // or opfor. If it's not, third-party.
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
     * Runs all the bot-controlled entities in the scenario through a skill
     * upgrader,
     * potentially giving the SPAs.
     *
     * @param scenario The scenario to process.
     * @param campaign A pointer to the campaign
     */
    public static void upgradeBotCrews(AtBScenario scenario, Campaign campaign) {
        CrewSkillUpgrader csu = new CrewSkillUpgrader(campaign.getCampaignOptions().getSpaUpgradeIntensity());

        for (int forceIndex = 0; forceIndex < scenario.getNumBots(); forceIndex++) {
            for (Entity entity : scenario.getBotForce(forceIndex).getFullEntityList(campaign)) {
                csu.upgradeCrew(entity);
            }
        }

        for (Entity entity : scenario.getAlliesPlayer()) {
            csu.upgradeCrew(entity);
        }
    }

    /**
     * Highly paranoid function that will check if the given faction is one of the
     * owners of the contract's location at the current date.
     */
    private static boolean isPlanetOwner(AtBContract contract, LocalDate currentDate, String factionCode) {
        if ((contract == null) || (contract.getSystem() == null) ||
                (contract.getSystem().getFactions(currentDate) == null)) {
            return false;
        }

        return contract.getSystem().getFactions(currentDate).contains(factionCode);
    }

    /**
     * Given a player unit ID and a template name, if the player unit type matches
     * the template's unit type and the template generation method is
     * PlayerOrAllied,
     * take the first unit that we find in the given scenario that's a part of that
     * template and "put it away".
     */
    public static void benchAllyUnit(UUID playerUnitID, String templateName, AtBDynamicScenario scenario) {
        ScenarioForceTemplate destinationTemplate = null;
        if (scenario.getTemplate().getScenarioForces().containsKey(templateName)) {
            destinationTemplate = scenario.getTemplate().getScenarioForces().get(templateName);
        }

        if ((destinationTemplate == null) ||
                (destinationTemplate.getGenerationMethod() != ForceGenerationMethod.PlayerOrFixedUnitCount.ordinal())) {
            return;
        }

        // two possible situations here:
        // 1 - the unit is an "attached" unit. This requires a mapping between template
        // name and
        // individual attached units. At this point, we remove the first unit matching
        // the template
        // from the attached units list. The benched unit should have the player unit's
        // ID
        // stored so that if the player unit is detached, the benched unit comes back.
        // 2 - the unit is part of a bot force. In this case, we need a mapping between
        // template names
        // and bot forces.

        if (destinationTemplate.getForceAlignment() == ForceAlignment.Player.ordinal()) {
            Entity swapTarget = null;

            // look through the "allies" player to see a unit that was put there
            // under a matching template
            for (Entity entity : scenario.getAlliesPlayer()) {
                UUID unitID = UUID.fromString(entity.getExternalIdAsString());

                if (scenario.getBotUnitTemplates().get(unitID).getForceName().equals(templateName)) {
                    swapTarget = entity;
                    break;
                }
            }

            if (swapTarget == null) {
                return;
            }

            BenchedEntityData benchedEntity = new BenchedEntityData();
            benchedEntity.entity = swapTarget;
            benchedEntity.templateName = "";

            scenario.getAlliesPlayer().remove(swapTarget);
            scenario.getPlayerUnitSwaps().put(playerUnitID, benchedEntity);
            swapUnitInObjectives(playerUnitID.toString(), benchedEntity.entity.getExternalIdAsString(), "", scenario);
        } else {
            BotForce botForce = null;

            // slightly inefficient to loop through all bot forces looking for our template
            // but it is also difficult to create a reverse lookup, so we avoid that problem
            // for now
            for (int x = 0; x < scenario.getNumBots(); x++) {
                BotForce candidateForce = scenario.getBotForce(x);
                if (candidateForce.getTemplateName().equals(templateName)) {
                    botForce = candidateForce; // found a matching force, end the loop and move on.
                    break;
                }
            }

            if ((botForce != null) && !botForce.getFixedEntityList().isEmpty()) {
                Entity swapTarget = botForce.getFixedEntityList().get(0);
                BenchedEntityData benchedEntity = new BenchedEntityData();
                benchedEntity.entity = swapTarget;
                benchedEntity.templateName = destinationTemplate.getForceName();

                botForce.removeEntity(0);
                scenario.getPlayerUnitSwaps().put(playerUnitID, benchedEntity);
                swapUnitInObjectives(playerUnitID.toString(), benchedEntity.entity.getExternalIdAsString(),
                        botForce.getName(), scenario);
            }
        }
    }

    /**
     * Given a scenario and a pair of unit IDs (and a force), swap the first one for
     * the second one.
     * Or, add the unit to all objectives containing the given force.
     */
    private static void swapUnitInObjectives(String subIn, String subOut, String subOutForceName,
            AtBDynamicScenario scenario) {
        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {
            // if the sub-out unit is explicitly referenced, do a direct substitution
            if (objective.getAssociatedUnitIDs().contains(subOut)) {
                objective.removeUnit(subOut);

                // don't want to add an empty unit to the objective
                if (!subIn.isEmpty()) {
                    objective.addUnit(subIn);
                }

                continue;
            }

            // if the sub-out unit is replacing a unit that's part of a force,
            // just add it individually
            if (objective.getAssociatedForceNames().contains(subOutForceName)) {
                objective.addUnit(subIn);
            }
        }
    }

    /**
     * Given a player unit ID and a scenario, return a benched allied unit, if one
     * exists
     * that was benched in favor of the player's unit.
     */
    public static void unbenchAttachedAlly(UUID playerUnitID, AtBDynamicScenario scenario) {
        // get entity from temporary store (big battle allies?), if it exists
        // add it to to bot force being worked with or attached ally list
        if (scenario.getPlayerUnitSwaps().containsKey(playerUnitID)) {
            BenchedEntityData benchedEntityData = scenario.getPlayerUnitSwaps().get(playerUnitID);

            if (benchedEntityData.templateName.isEmpty()) {
                scenario.getAlliesPlayer().add(benchedEntityData.entity);
                swapUnitInObjectives(benchedEntityData.entity.getExternalIdAsString(), playerUnitID.toString(), "",
                        scenario);
            } else {
                for (int x = 0; x < scenario.getNumBots(); x++) {
                    BotForce botForce = scenario.getBotForce(x);
                    if (botForce.getTemplateName().equals(benchedEntityData.templateName)) {
                        botForce.addEntity(benchedEntityData.entity);
                        // in this situation, the entity is being added back to a force,
                        // so we just want to clear out the player unit.
                        swapUnitInObjectives("", playerUnitID.toString(), "", scenario);
                        break;
                    }
                }
            }

            scenario.getPlayerUnitSwaps().remove(playerUnitID);
        }
    }
}
