/*
 * AtBContract.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconContractDefinition;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.fameAndInfamy.BatchallFactions;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.client.ratgenerator.ModelRecord.NETWORK_NONE;
import static megamek.client.ratgenerator.UnitTable.findTable;
import static megamek.common.Compute.d6;
import static megamek.common.UnitType.MEK;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.parseFromInteger;
import static megamek.common.enums.SkillLevel.parseFromString;
import static mekhq.campaign.force.CombatTeam.getStandardForceSize;
import static mekhq.campaign.force.FormationLevel.BATTALION;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.getEntity;
import static mekhq.campaign.mission.BotForceRandomizer.UNIT_WEIGHT_UNSPECIFIED;
import static mekhq.campaign.rating.IUnitRating.*;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.campaign.universe.fameAndInfamy.BatchallFactions.BATCHALL_FACTIONS;
import static mekhq.gui.dialog.HireBulkPersonnelDialog.overrideSkills;
import static mekhq.gui.dialog.HireBulkPersonnelDialog.reRollAdvantages;
import static mekhq.gui.dialog.HireBulkPersonnelDialog.reRollLoyalty;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * Contract class for use with Against the Bot rules
 *
 * @author Neoancient
 */
public class AtBContract extends Contract {
    private static final MMLogger logger = MMLogger.create(AtBContract.class);

    public static final int EVT_NOEVENT = -1;
    public static final int EVT_BONUSROLL = 0;
    public static final int EVT_SPECIAL_SCENARIO = 1;
    public static final int EVT_CIVILDISTURBANCE = 2;
    public static final int EVT_SPORADICUPRISINGS = 3;
    public static final int EVT_REBELLION = 4;
    public static final int EVT_BETRAYAL = 5;
    public static final int EVT_TREACHERY = 6;
    public static final int EVT_LOGISTICSFAILURE = 7;
    public static final int EVT_REINFORCEMENTS = 8;
    public static final int EVT_SPECIALEVENTS = 9;
    public static final int EVT_BIGBATTLE = 10;

    /** The minimum intensity below which no scenarios will be generated */
    public static final double MINIMUM_INTENSITY = 0.01;

    /* null unless subcontract */
    protected AtBContract parentContract;
    /* hired by another mercenary unit on contract to a third-party employer */
    boolean mercSubcontract;

    protected String employerCode;
    protected String enemyCode;
    protected String enemyName;

    protected AtBContractType contractType;
    protected SkillLevel allySkill;
    protected int allyQuality;
    protected SkillLevel enemySkill;
    protected int enemyQuality;
    protected String allyBotName;
    protected String enemyBotName;
    protected Camouflage allyCamouflage;
    protected PlayerColour allyColour;
    protected Camouflage enemyCamouflage;
    protected PlayerColour enemyColour;

    protected int extensionLength;

    protected int requiredLances;
    protected AtBMoraleLevel moraleLevel;
    protected LocalDate routEnd;
    protected int partsAvailabilityLevel;
    protected int sharesPct;
    private boolean batchallAccepted;

    protected int playerMinorBreaches;
    protected int employerMinorBreaches;
    protected int contractScoreArbitraryModifier;

    protected int moraleMod = 0;

    /* lasts for a month, then removed at next events roll */
    protected boolean priorLogisticsFailure;
    /**
     * If the date is non-null, there will be a special scenario or big battle
     * on that date, but the scenario is not generated until the other battle
     * rolls for the week.
     */
    protected LocalDate specialEventScenarioDate;
    protected int specialEventScenarioType;
    /* Lasts until end of contract */
    protected int battleTypeMod;
    /* Only applies to next week */
    protected int nextWeekBattleTypeMod;

    private StratconCampaignState stratconCampaignState;
    private boolean isAttacker;

    private static final ResourceBundle resources = ResourceBundle.getBundle(
            "mekhq.resources.AtBContract",
            MekHQ.getMHQOptions().getLocale());

    private int commandRoll;
    private int salvageRoll;
    private int supportRoll;
    private int transportRoll;

    protected AtBContract() {
        this(null);
    }

    /**
     * Sets the end date of the rout.
     * This should only be applied on contracts whose morale equals ROUTED
     *
     * @param routEnd the {@code LocalDate} representing the end date of the rout
     */
    public void setRoutEndDate(LocalDate routEnd) {
        this.routEnd = routEnd;
    }

    public AtBContract(String name) {
        super(name, "Independent");
        employerCode = "IND";
        enemyCode = "IND";
        enemyName = "Independent";

        parentContract = null;
        mercSubcontract = false;
        isAttacker = false;

        setContractType(AtBContractType.GARRISON_DUTY);
        setAllySkill(REGULAR);
        allyQuality = DRAGOON_C;
        setEnemySkill(REGULAR);
        enemyQuality = DRAGOON_C;
        allyBotName = "Ally";
        enemyBotName = "Enemy";
        setAllyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.RED.name()));
        setAllyColour(PlayerColour.RED);
        setEnemyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.GREEN.name()));
        setEnemyColour(PlayerColour.GREEN);

        extensionLength = 0;

        sharesPct = 0;
        batchallAccepted = true;
        setMoraleLevel(AtBMoraleLevel.STALEMATE);
        routEnd = null;
        priorLogisticsFailure = false;
        specialEventScenarioDate = null;
        battleTypeMod = 0;
        nextWeekBattleTypeMod = 0;
    }

    public void initContractDetails(Campaign campaign) {
        int companySize = getStandardForceSize(campaign.getFaction(), COMPANY.getDepth());
        int battalionSize = getStandardForceSize(campaign.getFaction(), BATTALION.getDepth());

        if (getEffectiveNumUnits(campaign) <= companySize) {
            setOverheadComp(OH_FULL);
        } else if (getEffectiveNumUnits(campaign) <= battalionSize) {
            setOverheadComp(OH_HALF);
        } else {
            setOverheadComp(OH_NONE);
        }

        int currentYear = campaign.getGameYear();
        allyBotName = getEmployerName(currentYear);
        allyCamouflage = pickRandomCamouflage(currentYear, employerCode);

        enemyBotName = getEnemyName(currentYear);
        enemyCamouflage = pickRandomCamouflage(currentYear, enemyCode);
    }

    /**
     * Selects a random camouflage for the given faction based on the faction code and year.
     * If there are no available files in the faction directory, it logs a warning and uses default
     * camouflage.
     *
     * @param currentYear the current year in the game.
     * @param factionCode the code representing the faction for which the camouflage is to be selected.
     */
    public static Camouflage pickRandomCamouflage(int currentYear, String factionCode) {
        // Define the root directory and get the faction-specific camouflage directory
        final String ROOT_DIRECTORY = "data/images/camo/";

        String camouflageDirectory = "Standard Camouflage";

        if (factionCode != null) {
            camouflageDirectory = getCamouflageDirectory(currentYear, factionCode);
        }

        // Gather all files
        List<Path> allPaths = null;

        try {
            allPaths = Files.find(Paths.get(ROOT_DIRECTORY + camouflageDirectory + '/'), Integer.MAX_VALUE,
                    (path, bfa) -> bfa.isRegularFile())
                .toList();

        } catch (IOException e) {
            logger.error("Error getting list of camouflages", e);
        }

        // Select a random file to set camouflage, if there are files available
        if ((null != allPaths) && (!allPaths.isEmpty())) {
            Path randomPath = allPaths.get(new Random().nextInt(allPaths.size()));

            String fileName = randomPath.getFileName().toString();
            String fileCategory = randomPath.getParent().toString()
                .replaceAll("\\\\", "/"); // This is necessary for windows machines
            fileCategory = fileCategory.replaceAll(ROOT_DIRECTORY, "");

            return new Camouflage(fileCategory, fileName);
        } else {
            // Log if no files were found in the directory
            logger.warn(String.format("No files in directory %s - using default camouflage",
                camouflageDirectory));
            return null;
        }
    }


    /**
     * Retrieves the directory for the camouflage of a faction based on the current year and faction code.
     *
     * @param currentYear The current year in the game.
     * @param factionCode The code representing the faction.
     * @return The directory for the camouflage of the faction.
     */
    private static String getCamouflageDirectory(int currentYear, String factionCode) {
        return switch (factionCode) {
            case "ARC" -> "Aurigan Coalition";
            case "CDP" -> "Calderon Protectorate";
            case "CC" -> "Capellan Confederation";
            case "CIR" -> "Circinus Federation";
            case "CS" -> "ComStar";
            case "DC" -> "Draconis Combine";
            case "CF" -> "Federated Commonwealth";
            case "FS" -> "Federated Suns";
            case "FVC" -> "Filtvelt Coalition";
            case "FRR" -> "Free Rasalhague Republic";
            case "FWL" -> "Free Worlds League";
            case "FR" -> "Fronc Reaches";
            case "HL" -> "Hanseatic League";
            case "LL" -> "Lothian League";
            case "LA" -> "Lyran Commonwealth";
            case "MOC" -> "Magistracy of Canopus";
            case "MH" -> "Marian Hegemony";
            case "MERC" -> "Mercs";
            case "OA" -> "Outworlds Alliance";
            case "PIR" -> "Pirates";
            case "ROS" -> "Republic of the Sphere";
            case "SL" -> "Star League Defense Force";
            case "TC" -> "Taurian Concordat";
            case "WOB" -> "Word of Blake";
            case "Root" -> "";
            default -> {
                Faction faction = Factions.getInstance().getFaction(factionCode);

                if (faction.isClan()) {
                    yield getClanCamouflageDirectory(currentYear, factionCode);
                } else {
                    yield "Standard Camouflage";
                }
            }
        };
    }

    /**
     * Retrieves the directory for the camouflage of a clan faction based on
     * the current year and faction code.
     *
     * @param currentYear The current year in the game.
     * @param factionCode The code representing the faction.
     * @return The directory for the camouflage of the clan faction.
     */
    private static String getClanCamouflageDirectory(int currentYear, String factionCode) {
        final String ROOT_DIRECTORY = "Clans/";

        return switch (factionCode) {
            case "CBS" -> ROOT_DIRECTORY + "Blood Spirit";
            case "CB" -> ROOT_DIRECTORY + "Burrock";
            case "CCC" -> ROOT_DIRECTORY + "Cloud Cobra";
            case "CCO" -> ROOT_DIRECTORY + "Coyote";
            case "CDS" -> {
                if (currentYear < 3100) {
                    yield ROOT_DIRECTORY + "Diamond Shark";
                } else {
                    yield ROOT_DIRECTORY + "Sea Fox (Dark Age)";
                }
            }
            case "CFM" -> ROOT_DIRECTORY + "Fire Mandrill";
            case "CGB", "RD" -> ROOT_DIRECTORY + "Ghost Bear";
            case "CGS" -> ROOT_DIRECTORY + "Goliath Scorpion";
            case "CHH" -> ROOT_DIRECTORY + "Hell's Horses";
            case "CIH" -> ROOT_DIRECTORY + "Ice Hellion";
            case "CJF" -> ROOT_DIRECTORY + "Jade Falcon";
            case "CMG" -> ROOT_DIRECTORY + "Mongoose";
            case "CNC" -> ROOT_DIRECTORY + "Nova Cat";
            case "CSJ" -> ROOT_DIRECTORY + "Smoke Jaguar";
            case "CSR" -> ROOT_DIRECTORY + "Snow Raven";
            case "SOC" -> ROOT_DIRECTORY + "Society";
            case "CSA" -> ROOT_DIRECTORY + "Star Adder";
            case "CSV" -> ROOT_DIRECTORY + "Steel Viper";
            case "CSL" -> ROOT_DIRECTORY + "Stone Lion";
            case "CWI" -> ROOT_DIRECTORY + "Widowmaker";
            case "CW", "CWE" -> ROOT_DIRECTORY + "Wolf";
            case "CWIE" -> ROOT_DIRECTORY + "Wolf-in-Exile";
            case "CWOV" -> ROOT_DIRECTORY + "Wolverine";
            default -> "Standard Camouflage";
        };
    }

    public void calculateLength(final boolean variable) {
        setLength(getContractType().calculateLength(variable, this));
    }

    /**
     * Calculates the number of lances required for this contract, based on [campaign].
     *
     * @param campaign The campaign to reference.
     * @return The number of lances required.
     */
    public static int calculateRequiredLances(Campaign campaign) {
        int formationSize = getStandardForceSize(campaign.getFaction());
        return max(getEffectiveNumUnits(campaign) / formationSize, 1);
    }

    public static int getEffectiveNumUnits(Campaign campaign) {
        double numUnits = 0;
        for (UUID uuid : campaign.getForces().getAllUnits(true)) {
            if (null == campaign.getUnit(uuid)) {
                continue;
            }
            switch (campaign.getUnit(uuid).getEntity().getUnitType()) {
                case MEK:
                    numUnits += 1;
                    break;
                case UnitType.TANK:
                case UnitType.VTOL:
                case UnitType.NAVAL:
                    numUnits += campaign.getFaction().isClan() ? 0.5 : 1;
                    break;
                case UnitType.CONV_FIGHTER:
                case UnitType.AEROSPACEFIGHTER:
                    if (campaign.getCampaignOptions().isUseAero()) {
                        numUnits += campaign.getFaction().isClan() ? 0.5 : 1;
                    }
                    break;
                case UnitType.PROTOMEK:
                    numUnits += 0.2;
                    break;
                case UnitType.BATTLE_ARMOR:
                case UnitType.INFANTRY:
                default:
                    /* don't count */
            }
        }
        return (int) numUnits;
    }

    public static boolean isMinorPower(final String factionCode) {
        // TODO : Windchild move me to AtBContractMarket
        final Faction faction = Factions.getInstance().getFaction(factionCode);
        return !faction.isMajorOrSuperPower() && !faction.isClan();
    }

    /**
     * Checks and updates the morale which depends on various conditions such as the rout end date,
     * skill levels, victories, defeats, etc. This method also updates the enemy status based on the
     * morale level.
     *
     * @param today       The current date in the context.
     */
    public void checkMorale(Campaign campaign, LocalDate today) {
        // Check whether enemy forces have been reinforced, and whether any current rout continues
        // beyond its expected date
        boolean routContinue = Compute.randomInt(4) == 0;

        // If there is a rout end date, and it's past today, update morale and enemy state accordingly
        if (routEnd != null && !routContinue) {
            if (today.isAfter(routEnd)) {
                setMoraleLevel(AtBMoraleLevel.STALEMATE);
                routEnd = null;

                updateEnemy(campaign, today); // mix it up a little
            } else {
                setMoraleLevel(AtBMoraleLevel.ROUTED);
            }
            return;
        }

        TargetRoll targetNumber = new TargetRoll();
        logger.info("Making Morale Check");
        logger.info(String.format("Current Morale: %s (%s)",
            getMoraleLevel().toString(), getMoraleLevel().ordinal()));

        // Confidence:
        int enemySkillRating = getEnemySkill().getAdjustedValue() - 2;
        int allySkillRating = getAllySkill().getAdjustedValue() - 2;

        if (getCommandRights().isIndependent()) {
            allySkillRating = (campaign.getCampaignOptions().getUnitRatingMethod().isFMMR() ? getAllySkill()
                : campaign.getReputation().getAverageSkillLevel()).getAdjustedValue();
            allySkillRating -= 2;
        }

        final LocalDate THE_GREAT_REFUSAL = LocalDate.of(3060, 4, 12);

        if (campaign.getLocalDate().isBefore(THE_GREAT_REFUSAL)) {
            if (getEnemy().isClan() && !getEmployerFaction().isClan()) {
                enemySkillRating++;
            } else if (!getEnemy().isClan() && getEmployerFaction().isClan()) {
                allySkillRating++;
            }
        }

        int confidence = enemySkillRating - allySkillRating;
        targetNumber.addModifier(confidence, "confidence");
        logger.info(String.format("Confidence: %s", confidence >= 0 ? "+" + confidence : confidence));

        // Reliability:
        int reliability = getEnemyQuality();

        Faction enemy = getEnemy();
        if (enemy.isClan()) {
            reliability = max(5, reliability + 1);
        }

        reliability = switch (reliability) {
            case DRAGOON_F -> -1;
            case DRAGOON_D -> {
                if (Compute.randomInt(1) == 0) {
                    yield -1;
                } else {
                    yield 0;
                }
            }
            case DRAGOON_C -> 0;
            case DRAGOON_B -> {
                if (Compute.randomInt(1) == 0) {
                    yield 0;
                } else {
                    yield +1;
                }
            }
            case DRAGOON_A -> +1;
            default -> { // DRAGOON_ASTAR
                if (Compute.randomInt(1) == 0) {
                    yield +1;
                } else {
                    yield +2;
                }
            }
        };

        if (enemy.isRebel()
            || enemy.isMinorPower()
            || enemy.isMercenary()
            || enemy.isPirate()) {
            reliability--;
        } else if (enemy.isClan()) {
            reliability++;
        }

        targetNumber.addModifier(reliability, "reliability");
        logger.info(String.format("Reliability: %s", reliability >= 0 ? "+" + reliability : reliability));

        // Force Type (unimplemented)
        // TODO once we have force types defined on the StratCon map, we should handle modifiers here.
        // 'Mek or Aircraft == +1
        // Vehicle == +0
        // Infantry == -1 (if unsupported)

        // Performance
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        // Loop through scenarios, counting victories and defeats that fall within the target month
        for (Scenario scenario : getScenarios()) {
            if ((scenario.getDate() != null) && lastMonth.isAfter(scenario.getDate())) {
                continue;
            }

            ScenarioStatus scenarioStatus = scenario.getStatus();

            if (scenarioStatus.isOverallVictory()) {
                victories++;
            } else if (scenarioStatus.isOverallDefeat() || scenarioStatus.isRefusedEngagement()) {
                defeats++;
            }

            if (scenarioStatus.isDecisiveVictory()) {
                victories++;
            } else if (scenarioStatus.isDecisiveDefeat()) {
                defeats++;
            } else if (scenarioStatus.isPyrrhicVictory()) {
                victories--;
            }
        }

        int performanceModifier = 0;

        if (victories > defeats) {
            if (victories >= (defeats * 2)) {
                performanceModifier -= 4;
            } else {
                performanceModifier -= 2;
            }
        } else if (defeats > victories) {
            if (defeats >= (victories * 2)) {
                performanceModifier += 4;
            } else {
                performanceModifier += 2;
            }
        }

        targetNumber.addModifier(performanceModifier, "performanceModifier");
        logger.info(String.format("Performance: %s", performanceModifier >= 0 ?
            "+" + performanceModifier : performanceModifier));

        // Total morale modifier calculation
        int roll = d6(2) + targetNumber.getValue();
        logger.info(String.format("Total Modifier: %s", targetNumber.getValue()));
        logger.info(String.format("Roll: %s", roll));

        // Morale level determination based on roll value
        final AtBMoraleLevel[] moraleLevels = AtBMoraleLevel.values();

        if (roll < 2) {
            setMoraleLevel(moraleLevels[max(getMoraleLevel().ordinal() - 2, 0)]);
            logger.info("Result: Morale Level -2");
        } else if (roll < 5) {
            setMoraleLevel(moraleLevels[max(getMoraleLevel().ordinal() - 1, 0)]);
            logger.info("Result: Morale Level -1");
        } else if ((roll > 12)) {
            setMoraleLevel(moraleLevels[Math.min(getMoraleLevel().ordinal() + 2, moraleLevels.length - 1)]);
            logger.info("Result: Morale Level +1");
        } else if ((roll > 9)) {
            setMoraleLevel(moraleLevels[Math.min(getMoraleLevel().ordinal() + 1, moraleLevels.length - 1)]);
            logger.info("Result: Morale Level +2");
        } else {
            logger.info("Result: Morale Unchanged");
        }

        // Additional morale updates if morale level is set to 'Routed' and contract type is a garrison type
        if (getMoraleLevel().isRouted()) {
            if (getContractType().isGarrisonType()) {
                routEnd = today.plusMonths(max(1, d6() - 3)).minusDays(1);
            } else {
                campaign.addReport("With the enemy routed, any remaining objectives have been successfully completed." +
                        " The contract will conclude tomorrow.");
                setEndDate(today.plusDays(1));
            }
        }

        // Reset external morale modifier
        moraleMod = 0;
    }

    /**
     * Updates the enemy faction and enemy bot name for this contract.
     *
     * @param campaign The current campaign.
     * @param today    The current LocalDate object.
     */
    private void updateEnemy(Campaign campaign, LocalDate today) {
        String enemyCode = RandomFactionGenerator.getInstance().getEnemy(
                Factions.getInstance().getFaction(employerCode), false, true);
        setEnemyCode(enemyCode);

        Faction enemyFaction = Factions.getInstance().getFaction(enemyCode);
        setEnemyBotName(enemyFaction.getFullName(today.getYear()));
        enemyName = ""; // wipe the old enemy name
        getEnemyName(today.getYear()); // we use this to update enemyName

        // We have a check in getEnemyName that prevents rolling over mercenary names,
        // so we add this extra step to force a mercenary name re-roll,
        // in the event one Mercenary faction is replaced with another.
        if (Factions.getInstance().getFaction(enemyCode).isMercenary()) {
            enemyBotName = BackgroundsController.randomMercenaryCompanyNameGenerator(null);
        }

        allyCamouflage = pickRandomCamouflage(today.getYear(), employerCode);
        enemyCamouflage = pickRandomCamouflage(today.getYear(), enemyCode);

        // Update the Batchall information
        batchallAccepted = true;
        if (campaign.getCampaignOptions().isUseGenericBattleValue()
            && BatchallFactions.usesBatchalls(enemyCode)) {
            setBatchallAccepted(initiateBatchall(campaign));
        }
    }

    /**
     * Retrieves the repair location based on the unit rating and contract type.
     *
     * @param unitRating The rating of the unit.
     * @return The repair location.
     */
    public int getRepairLocation(final int unitRating) {
        int repairLocation = Unit.SITE_FACILITY_BASIC;

        AtBContractType contractType = getContractType();

        if (contractType.isGuerrillaWarfare()) {
            repairLocation = Unit.SITE_IMPROVISED;
        } else if (contractType.isRaidType()) {
            repairLocation = Unit.SITE_FIELD_WORKSHOP;
        } else if (contractType.isGarrisonType()) {
            repairLocation = Unit.SITE_FACILITY_MAINTENANCE;
        }

        if (unitRating >= DRAGOON_B) {
            repairLocation++;
        }

        return Math.min(repairLocation, Unit.SITE_FACTORY_CONDITIONS);
    }

    public void addMoraleMod(int mod) {
        moraleMod += mod;
    }

    public int getScore() {
        int score = employerMinorBreaches - playerMinorBreaches;
        int battles = 0;
        boolean earlySuccess = false;
        for (Scenario scenario : getCompletedScenarios()) {
            // Special Scenarios get no points for victory and only -1 for defeat.
            if ((scenario instanceof AtBScenario) && ((AtBScenario) scenario).isSpecialScenario()) {
                if (scenario.getStatus().isOverallDefeat() || scenario.getStatus().isRefusedEngagement()) {
                    score--;
                }
            } else {
                switch (scenario.getStatus()) {
                    case DECISIVE_VICTORY:
                    case VICTORY:
                    case MARGINAL_VICTORY:
                    case PYRRHIC_VICTORY:
                        score++;
                        battles++;
                        break;
                    case DECISIVE_DEFEAT:
                    case DEFEAT:
                        score -= 2;
                        battles++;
                        break;
                    case MARGINAL_DEFEAT:
                        // special scenario defeat
                        score--;
                        break;
                    default:
                        break;
                }
            }

            if ((scenario instanceof AtBScenario)
                    && (((AtBScenario) scenario).getScenarioType() == AtBScenario.BASEATTACK)
                    && ((AtBScenario) scenario).isAttacker() && scenario.getStatus().isOverallVictory()) {
                earlySuccess = true;
            } else if (getMoraleLevel().isRouted() && !getContractType().isGarrisonType()) {
                earlySuccess = true;
            }
        }

        if (battles == 0) {
            score++;
        }

        if (earlySuccess) {
            score += 4;
        }
        score += contractScoreArbitraryModifier;
        return score;
    }

    public int getContractScoreArbitraryModifier() {
        return contractScoreArbitraryModifier;
    }

    /**
     * Performs a bonus roll to determine and execute a random campaign bonus.
     * The roll is simulated using 1d6, and the outcome triggers different bonus
     * effects based on the roll value. The effects may involve recruiting
     * dependents, adding new units, or other benefits as determined by the
     * campaign options and roll outcome.
     *
     * @param campaign       the current {@link Campaign} instance.
     * @param isPostScenario a {@code boolean} indicating if this roll occurs post-scenario
     *                       (used to determine specific behaviors for roll = 3).
     *
     * @return {@code true} if specific post-scenario behavior is triggered (roll = 3),
     *         otherwise {@code false}.
     *
     * @throws IllegalStateException if an unexpected roll value is encountered.
     */
    public boolean doBonusRoll(Campaign campaign, boolean isPostScenario) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        int number;
        int roll = d6();

        return switch (roll) {
            case 1 -> { /* 1d6 dependents */
                if (campaignOptions.isUseRandomDependentAddition()) {
                    number = d6();
                    campaign.addReport("Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));

                    for (int i = 0; i < number; i++) {
                        Person person = campaign.newDependent(Gender.RANDOMIZE);
                        campaign.recruitPerson(person);
                    }
                } else {
                    campaign.addReport("Bonus: Ronin");
                    recruitRonin(campaign);
                }
                yield false;
            }
            case 2 -> {
                campaign.addReport("Bonus: Ronin");
                recruitRonin(campaign);
                yield false;
            }
            case 3 -> { // Resupply
                if (campaignOptions.isUseAtB() && !campaignOptions.isUseStratCon()) {
                    campaign.addReport("Bonus: Ronin");
                    recruitRonin(campaign);
                    yield false;
                } else {
                    if (isPostScenario) {
                        yield true;
                    } else {
                        campaign.addReport("Bonus: Support Point");
                        stratconCampaignState.setSupportPoints(1);
                        yield false;
                    }
                }
            }
            case 4 -> {
                campaign.addReport("Bonus: Unit");
                addBonusUnit(campaign, UnitType.TANK);
                yield false;
            }
            case 5 -> {
                campaign.addReport("Bonus: Unit");
                addBonusUnit(campaign, UnitType.AEROSPACEFIGHTER);
                yield false;
            }
            case 6 -> {
                campaign.addReport("Bonus: Unit");
                addBonusUnit(campaign, MEK);
                yield false;
            }
            default -> throw new IllegalStateException(
                "Unexpected value in mekhq/campaign/mission/AtBContract.java/doBonusRoll: " + roll);
        };
    }

    /**
     * Generates a Ronin and adds them to the personnel roster.
     *
     * @param campaign the current campaign.
     */
    private static void recruitRonin(Campaign campaign) {
        Person ronin = campaign.newPerson(PersonnelRole.MEKWARRIOR);

        overrideSkills(campaign, ronin, PersonnelRole.MEKWARRIOR,
            Objects.requireNonNull(SkillLevel.VETERAN).ordinal());

        reRollLoyalty(ronin, ronin.getExperienceLevel(campaign, false));
        reRollAdvantages(campaign, ronin, ronin.getExperienceLevel(campaign, false));
        ronin.setCallsign(RandomCallsignGenerator.getInstance().generate());

        campaign.recruitPerson(ronin, true);
    }

    /**
     * Generates a bonus unit for a given campaign and unit type.
     *
     * @param campaign  the campaign object to add the bonus unit to
     * @param unitType  the type of unit for the bonus
     */
    private void addBonusUnit(Campaign campaign, int unitType) {
        String faction = employerCode;
        int quality = allyQuality;

        if (Compute.randomInt(2) > 0) {
            faction = enemyCode;
            quality = enemyQuality;
        }

        Entity newUnit = getEntity(faction, REGULAR, quality, unitType,
            UNIT_WEIGHT_UNSPECIFIED, null, campaign);
        campaign.addNewUnit(newUnit, false, 0);
    }

    public boolean isSubcontract() {
        return parentContract != null;
    }

    public AtBContract getParentContract() {
        return parentContract;
    }

    public void setParentContract(AtBContract parent) {
        parentContract = parent;
    }

    public boolean isMercSubcontract() {
        return mercSubcontract;
    }

    public void setMercSubcontract(boolean sub) {
        mercSubcontract = sub;
    }

    public boolean isAttacker() {
        return isAttacker;
    }

    public void setAttacker(boolean isAttacker) {
        this.isAttacker = isAttacker;
    }

    public void checkEvents(Campaign campaign) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        if (campaign.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                partsAvailabilityLevel++;
                priorLogisticsFailure = false;
            }

            switch (getContractType().generateEventType()) {
                case EVT_BONUSROLL:
                    campaign.addReport("<b>Special Event:</b> ");
                    doBonusRoll(campaign, false);
                    break;
                case EVT_SPECIAL_SCENARIO:
                    campaign.addReport("<b>Special Event:</b> Special scenario this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getContractType().generateSpecialScenarioType(campaign);
                    break;
                case EVT_CIVILDISTURBANCE:
                    campaign.addReport("<b>Special Event:</b> Civil disturbance<br />Next enemy morale roll gets +1 modifier");
                    moraleMod++;
                    break;
                case EVT_SPORADICUPRISINGS:
                    campaign.addReport("<b>Special Event:</b> Sporadic uprisings<br />+2 to next enemy morale roll");
                    moraleMod += 2;
                    break;
                case EVT_REBELLION:
                    campaign.addReport("<b>Special Event:</b> Rebellion<br />+2 to next enemy morale roll");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = AtBScenario.CIVILIANRIOT;
                    break;
                case EVT_BETRAYAL:
                    String text = "<b>Special Event:</b> Betrayal (employer minor breach)<br />";
                    switch (d6()) {
                        case 1:
                            text += "Major logistics problem: parts availability level for the rest of the contract becomes one level lower.";
                            partsAvailabilityLevel--;
                            break;
                        case 2:
                            text += "Transport: Player is abandoned in the field by employer transports; if he loses a Base Attack battle he loses all Meks on repair.";
                            break;
                        case 3:
                            text += "Diversion: All Battle Type rolls for the rest of the contract get a -5 modifier.";
                            battleTypeMod -= 5;
                            break;
                        case 4:
                            text += "False Intelligence: Next week Battle Type rolls get a -10 modifier.";
                            nextWeekBattleTypeMod -= 10;
                            break;
                        case 5:
                            text += "The Company Store: All equipment/supply prices are increased by 100% until the end of the contract.";
                            break;
                        case 6:
                            text += "False Alarm: No betrayal, but the employer still gets a minor breach.";
                            break;
                    }
                    employerMinorBreaches++;
                    campaign.addReport(text);
                    break;
                case EVT_TREACHERY:
                    campaign.addReport(
                            "<b>Special Event:</b> Treachery<br />Bad information from employer. Next Enemy Morale roll gets +1. Employer minor breach.");
                    moraleMod++;
                    employerMinorBreaches++;
                    break;
                case EVT_LOGISTICSFAILURE:
                    campaign.addReport(
                            "<b>Special Event:</b> Logistics Failure<br />Parts availability for the next month are one level lower.");
                    partsAvailabilityLevel--;
                    priorLogisticsFailure = true;
                    break;
                case EVT_REINFORCEMENTS:
                    campaign.addReport("<b>Special Event:</b> Reinforcements<br />The next Enemy Morale roll gets a -1.");
                    moraleMod--;
                    break;
                case EVT_SPECIALEVENTS:
                    text = "<b>Special Event:</b> ";
                    switch (d6()) {
                        case 1:
                            text += "Change of Alliance: Next Enemy Morale roll gets a +1 modifier.";
                            moraleMod++;
                            break;
                        case 2:
                            text += "Internal Dissension";
                            specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                            specialEventScenarioType = AtBScenario.AMBUSH;
                            break;
                        case 3:
                            text += "ComStar Interdict: Base availability level decreases one level for the rest of the contract.";
                            partsAvailabilityLevel--;
                            break;
                        case 4:
                            text += "Defectors: Next Enemy Morale roll gets a -1 modifier.";
                            moraleMod--;
                            break;
                        case 5:
                            text += "Free Trader: Base availability level increases one level for the rest of the contract.";
                            partsAvailabilityLevel++;
                            break;
                        case 6:
                            final String unitName = campaign.getUnitMarket().addSingleUnit(campaign,
                                    UnitMarketType.EMPLOYER, MEK, getEmployerFaction(),
                                    DRAGOON_F, 50);
                            if (unitName != null) {
                                text += String.format(
                                        "Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>",
                                        unitName);
                            }
                            break;
                    }
                    campaign.addReport(text);
                    break;
                case EVT_BIGBATTLE:
                    campaign.addReport("<b>Special Event:</b> Big battle this month");
                    specialEventScenarioDate = getRandomDayOfMonth(campaign.getLocalDate());
                    specialEventScenarioType = getContractType().generateBigBattleType();
                    break;
            }
        }

        /*
         * If the campaign somehow gets past the scheduled date (such as by
         * changing the date in the campaign options), ignore it rather
         * than generating a new scenario in the past. The event will still be
         * available (if the campaign date is restored) until another special scenario
         * or big battle event is rolled.
         */
        if ((specialEventScenarioDate != null)
                && !specialEventScenarioDate.isBefore(campaign.getLocalDate())) {
            LocalDate nextMonday = campaign.getLocalDate().plusDays(8 - campaign.getLocalDate().getDayOfWeek().getValue());

            if (specialEventScenarioDate.isBefore(nextMonday)) {
                AtBScenario s = AtBScenarioFactory.createScenario(campaign, null,
                        specialEventScenarioType, false,
                        specialEventScenarioDate);

                campaign.addScenario(s, this);
                if (campaign.getCampaignOptions().isUsePlanetaryConditions()) {
                    s.setPlanetaryConditions(this, campaign);
                }
                s.setForces(campaign);
                specialEventScenarioDate = null;
            }
        }
    }

    public LocalDate getRandomDayOfMonth(LocalDate today) {
        return LocalDate.of(today.getYear(), today.getMonth(),
                Compute.randomInt(today.getMonth().length(today.isLeapYear())) + 1);
    }

    public boolean contractExtended(final Campaign campaign) {
        if (getContractType().isPirateHunting() || getContractType().isRiotDuty()) {
            return false;
        }

        final String warName = RandomFactionGenerator.getInstance().getFactionHints().getCurrentWar(
                getEmployerFaction(), getEnemy(), campaign.getLocalDate());
        if (warName == null) {
            return false;
        }

        final int extension;
        final int roll = d6();
        if (roll == 1) {
            extension = max(1, getLength() / 2);
        } else if (roll == 2) {
            extension = 1;
        } else {
            return false;
        }

        campaign.addReport(String.format(
                "Due to the %s crisis your employer has invoked the emergency clause and extended the contract %d %s",
                warName, extension, ((extension == 1) ? " month" : " months")));
        setEndDate(getEndingDate().plusMonths(extension));
        extensionLength += extension;
        MekHQ.triggerEvent(new MissionChangedEvent(this));
        return true;
    }

    @Override
    public Money getMonthlyPayOut() {
        if (extensionLength == 0) {
            return super.getMonthlyPayOut();
        }
        /*
         * The transport clause and the advance monies have already been
         * accounted for over the original length of the contract. The extension
         * uses the base monthly amounts for support and overhead, with a
         * 50% bonus to the base amount.
         */

        if (getLength() <= 0) {
            return Money.zero();
        }

        return getBaseAmount()
                .multipliedBy(1.5)
                .plus(getSupportAmount())
                .plus(getOverheadAmount())
                .dividedBy(getLength());
    }

    @Override
    protected int writeToXMLBegin(final PrintWriter pw, int indent) {
        indent = super.writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerCode", getEmployerCode());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCode", getEnemyCode());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractType", getContractType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allySkill", getAllySkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyQuality", getAllyQuality());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemySkill", getEnemySkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyQuality", getEnemyQuality());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyBotName", getAllyBotName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyBotName", getEnemyBotName());
        if (!getAllyCamouflage().hasDefaultCategory()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyCamoCategory", getAllyCamouflage().getCategory());
        }

        if (!getAllyCamouflage().hasDefaultFilename()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyCamoFileName", getAllyCamouflage().getFilename());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "allyColour", getAllyColour().name());
        if (!getEnemyCamouflage().hasDefaultCategory()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCamoCategory", getEnemyCamouflage().getCategory());
        }

        if (!getEnemyCamouflage().hasDefaultFilename()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyCamoFileName", getEnemyCamouflage().getFilename());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "enemyColour", getEnemyColour().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "requiredLances", getRequiredLances());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "moraleLevel", getMoraleLevel().name());
        if (routEnd != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routEnd", routEnd);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partsAvailabilityLevel", getPartsAvailabilityLevel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "extensionLength", extensionLength);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sharesPct", sharesPct);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "batchallAccepted", batchallAccepted);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerMinorBreaches", playerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "employerMinorBreaches", employerMinorBreaches);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "contractScoreArbitraryModifier", contractScoreArbitraryModifier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "priorLogisticsFailure", priorLogisticsFailure);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleTypeMod", battleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nextWeekBattleTypeMod", nextWeekBattleTypeMod);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandRoll", commandRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvageRoll", salvageRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "supportRoll", supportRoll);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportRoll", transportRoll);

        if (parentContract != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "parentContractId", parentContract.getId());
        }

        if (specialEventScenarioDate != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioDate", specialEventScenarioDate);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specialEventScenarioType", specialEventScenarioType);
        }

        if (stratconCampaignState != null) {
            stratconCampaignState.Serialize(pw);
        }

        return indent;
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn) throws ParseException {
        super.loadFieldsFromXmlNode(wn);
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("employerCode")) {
                    employerCode = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyCode")) {
                    enemyCode = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("contractType")) {
                    setContractType(AtBContractType.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("allySkill")) {
                    setAllySkill(parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("allyQuality")) {
                    allyQuality = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("enemySkill")) {
                    setEnemySkill(parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyQuality")) {
                    enemyQuality = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("allyBotName")) {
                    allyBotName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyBotName")) {
                    enemyBotName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("allyCamoCategory")) {
                    getAllyCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("allyCamoFileName")) {
                    getAllyCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getTextContent().equalsIgnoreCase("allyColour")) {
                    setAllyColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoCategory")) {
                    getEnemyCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoFileName")) {
                    getEnemyCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getTextContent().equalsIgnoreCase("enemyColour")) {
                    setEnemyColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("requiredLances")) {
                    requiredLances = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("moraleLevel")) {
                    setMoraleLevel(AtBMoraleLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("routEnd")) {
                    routEnd = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("partsAvailabilityLevel")) {
                    partsAvailabilityLevel = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("extensionLength")) {
                    extensionLength = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("sharesPct")) {
                    sharesPct = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("batchallAccepted")) {
                    batchallAccepted = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("playerMinorBreaches")) {
                    playerMinorBreaches = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("employerMinorBreaches")) {
                    employerMinorBreaches = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("contractScoreArbitraryModifier")) {
                    contractScoreArbitraryModifier = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("priorLogisticsFailure")) {
                    priorLogisticsFailure = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("battleTypeMod")) {
                    battleTypeMod = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("nextWeekBattleTypeMod")) {
                    nextWeekBattleTypeMod = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("commandRoll")) {
                    commandRoll = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("salvageRoll")) {
                    salvageRoll = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("supportRoll")) {
                    supportRoll = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("transportRoll")) {
                    transportRoll = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("specialEventScenarioDate")) {
                    specialEventScenarioDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("specialEventScenarioType")) {
                    specialEventScenarioType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase(StratconCampaignState.ROOT_XML_ELEMENT_NAME)) {
                    stratconCampaignState = StratconCampaignState.Deserialize(wn2);
                    stratconCampaignState.setContract(this);
                    this.setStratconCampaignState(stratconCampaignState);
                } else if (wn2.getNodeName().equalsIgnoreCase("parentContractId")) {
                    parentContract = new AtBContractRef(Integer.parseInt(wn2.getTextContent()));
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    /**
     * Restores any references to other contracts.
     *
     * @param c The Campaign which holds this contract.
     */
    public void restore(Campaign c) {
        if (parentContract != null) {
            Mission m = c.getMission(parentContract.getId());
            if (m != null) {
                if (m instanceof AtBContract) {
                    setParentContract((AtBContract) m);
                } else {
                    logger.warn(String.format("Parent Contract reference #%d is not an AtBContract for contract %s",
                            parentContract.getId(), getName()));
                    setParentContract(null);
                }
            } else {
                logger.warn(String.format("Parent Contract #%d reference was not found for contract %s",
                        parentContract.getId(), getName()));
                setParentContract(null);
            }
        }
    }

    public Faction getEmployerFaction() {
        return Factions.getInstance().getFaction(getEmployerCode());
    }

    public String getEmployerCode() {
        return employerCode;
    }

    public void setEmployerCode(final String code, final LocalDate date) {
        employerCode = code;
        setEmployer(getEmployerName(date.getYear()));
        enemyCamouflage = pickRandomCamouflage(date.getYear(), enemyCode);
    }

    public void setEmployerCode(String code, int year) {
        employerCode = code;
        setEmployer(getEmployerName(year));
        allyCamouflage = pickRandomCamouflage(year, employerCode);
    }

    public String getEmployerName(int year) {
        return isMercSubcontract() ? "Mercenary (" + getEmployerFaction().getFullName(year) + ')'
                : getEmployerFaction().getFullName(year);
    }

    public Faction getEnemy() {
        return Factions.getInstance().getFaction(getEnemyCode());
    }

    public String getEnemyCode() {
        return enemyCode;
    }

    /**
     * Retrieves the name of the enemy for this contract.
     *
     * @param year The current year in the game.
     * @return The name of the enemy.
     */
    public String getEnemyName(int year) {
        Faction faction = Factions.getInstance().getFaction(enemyCode);

        if (faction.isMercenary()) {
            if (Objects.equals(enemyBotName, "Enemy")) {
                return BackgroundsController.randomMercenaryCompanyNameGenerator(null);
            } else {
                return enemyBotName;
            }
        } else {
            return faction.getFullName(year);
        }
    }

    public void setEnemyCode(String enemyCode) {
        this.enemyCode = enemyCode;
    }

    public AtBContractType getContractType() {
        return contractType;
    }

    public void setContractType(final AtBContractType contractType) {
        this.contractType = contractType;
        setType(contractType.toString());
    }

    public SkillLevel getAllySkill() {
        return allySkill;
    }

    public void setAllySkill(final SkillLevel allySkill) {
        this.allySkill = allySkill;
    }

    public SkillLevel getEnemySkill() {
        return enemySkill;
    }

    public void setEnemySkill(final SkillLevel enemySkill) {
        this.enemySkill = enemySkill;
    }

    public int getAllyQuality() {
        return allyQuality;
    }

    public void setAllyQuality(int allyQuality) {
        this.allyQuality = allyQuality;
    }

    public int getEnemyQuality() {
        return enemyQuality;
    }

    public void setEnemyQuality(int enemyQuality) {
        this.enemyQuality = enemyQuality;
    }

    public String getAllyBotName() {
        return allyBotName;
    }

    public void setAllyBotName(String name) {
        allyBotName = name;
    }

    public String getEnemyBotName() {
        return enemyBotName;
    }

    public void setEnemyBotName(String name) {
        enemyBotName = name;
    }

    public Camouflage getAllyCamouflage() {
        return allyCamouflage;
    }

    public void setAllyCamouflage(Camouflage allyCamouflage) {
        this.allyCamouflage = Objects.requireNonNull(allyCamouflage);
    }

    public PlayerColour getAllyColour() {
        return allyColour;
    }

    public void setAllyColour(PlayerColour allyColour) {
        this.allyColour = Objects.requireNonNull(allyColour);
    }

    public Camouflage getEnemyCamouflage() {
        return enemyCamouflage;
    }

    public void setEnemyCamouflage(Camouflage enemyCamouflage) {
        this.enemyCamouflage = enemyCamouflage;
    }

    public PlayerColour getEnemyColour() {
        return enemyColour;
    }

    public void setEnemyColour(PlayerColour enemyColour) {
        this.enemyColour = Objects.requireNonNull(enemyColour);
    }

    public int getRequiredLances() {
        return requiredLances;
    }

    public void setRequiredLances(int required) {
        requiredLances = required;
    }

    public int getPartsAvailabilityLevel() {
        return partsAvailabilityLevel;
    }

    public void setPartsAvailabilityLevel(final int partsAvailabilityLevel) {
        this.partsAvailabilityLevel = partsAvailabilityLevel;
    }

    public AtBMoraleLevel getMoraleLevel() {
        return moraleLevel;
    }

    public void setMoraleLevel(final AtBMoraleLevel moraleLevel) {
        this.moraleLevel = moraleLevel;
    }

    /**
     * Retrieves the dynamic shares percentage for this contract.
     * This method shouldn't be called directly,
     * instead use contract.getSharesPercent()
     *
     *
     * @return the dynamic shares percentage
     */
    public int getAtBSharesPercentage() {
        return sharesPct;
    }

    public void setAtBSharesPercent(int pct) {
        sharesPct = pct;
    }

    /**
     * Checks if the Batchall has been accepted for the contract.
     *
     * @return {@code true} if the Batchall has been accepted, {@code false} otherwise.
     */
    public boolean isBatchallAccepted() {
        return batchallAccepted;
    }

    /**
     * Sets the {@code batchallAccepted} flag for this contract.
     *
     * @param batchallAccepted The value to set for the {@code batchallAccepted} flag.
     */
    public void setBatchallAccepted(final boolean batchallAccepted) {
        this.batchallAccepted = batchallAccepted;
    }

    public void addPlayerMinorBreach() {
        playerMinorBreaches++;
    }

    public void addPlayerMinorBreaches(int num) {
        playerMinorBreaches += num;
    }

    public void addEmployerMinorBreach() {
        employerMinorBreaches++;
    }

    public void addEmployerMinorBreaches(int num) {
        employerMinorBreaches += num;
    }

    public void setContractScoreArbitraryModifier(int newModifier) {
        contractScoreArbitraryModifier = newModifier;
    }

    public int getBattleTypeMod() {
        return battleTypeMod + nextWeekBattleTypeMod;
    }

    public StratconCampaignState getStratconCampaignState() {
        return stratconCampaignState;
    }

    public void setStratconCampaignState(StratconCampaignState state) {
        stratconCampaignState = state;
    }

    @Override
    public void acceptContract(Campaign campaign) {
        if (campaign.getCampaignOptions().isUseStratCon()) {
            StratconContractInitializer.initializeCampaignState(this, campaign,
                    StratconContractDefinition.getContractDefinition(getContractType()));
        }
    }

    public AtBContract(Contract c, Campaign campaign) {
        this(c.getName());

        setType(c.getType());
        setSystemId(c.getSystemId());
        setDesc(c.getDescription());
        setStatus(c.getStatus());
        for (Scenario s : c.getScenarios()) {
            addScenario(s);
        }
        setId(c.getId());
        setLength(c.getLength());
        setStartDate(c.getStartDate());
        /*
         * Set ending date; the other calculated values will be replaced
         * from the original contract
         */
        calculateContract(campaign);
        setMultiplier(c.getMultiplier());
        setTransportComp(c.getTransportComp());
        setStraightSupport(c.getStraightSupport());
        setOverheadComp(c.getOverheadComp());
        setCommandRights(c.getCommandRights());
        setBattleLossComp(c.getBattleLossComp());
        setSalvagePct(c.getSalvagePct());
        setSalvageExchange(c.isSalvageExchange());
        setSalvagedByUnit(c.getSalvagedByUnit());
        setSalvagedByEmployer(c.getSalvagedByEmployer());
        setSigningBonusPct(c.getSigningBonusPct());
        setAdvancePct(c.getAdvancePct());
        setMRBCFee(c.payMRBCFee());
        setAdvanceAmount(c.getAdvanceAmount());
        setFeeAmount(c.getFeeAmount());
        setBaseAmount(c.getBaseAmount());
        setOverheadAmount(c.getOverheadAmount());
        setSupportAmount(c.getSupportAmount());
        setTransportAmount(c.getTransportAmount());
        setSigningBonusAmount(c.getSigningBonusAmount());

        /* Guess at AtBContract values */
        AtBContractType contractType = null;
        for (final AtBContractType type : AtBContractType.values()) {
            if (type.toString().equalsIgnoreCase(c.getType())) {
                contractType = type;
                break;
            }
        }
        /* Make a rough guess */
        if (contractType == null) {
            if (c.getLength() <= 3) {
                contractType = AtBContractType.OBJECTIVE_RAID;
            } else if (c.getLength() < 12) {
                contractType = AtBContractType.GARRISON_DUTY;
            } else {
                contractType = AtBContractType.PLANETARY_ASSAULT;
            }
        }
        setContractType(contractType);

        Faction f = Factions.getInstance().getFactionFromFullNameAndYear(c.getEmployer(), campaign.getGameYear());
        if (null == f) {
            employerCode = "IND";
        } else {
            employerCode = f.getShortName();
        }

        if (getContractType().isPirateHunting()) {
            enemyCode = "PIR";
        } else if (getContractType().isRiotDuty()) {
            enemyCode = "REB";
        }

        requiredLances = calculateRequiredLances(campaign);

        setPartsAvailabilityLevel(getContractType().calculatePartsAvailabilityLevel());

        int currentYear = campaign.getGameYear();
        allyBotName = getEmployerName(currentYear);
        allyCamouflage = pickRandomCamouflage(currentYear, employerCode);

        enemyBotName = getEnemyName(currentYear);
        enemyCamouflage = pickRandomCamouflage(currentYear, enemyCode);
    }

    /**
     * Represents a reference to another AtBContract.
     */
    protected static class AtBContractRef extends AtBContract {
        public AtBContractRef(int id) {
            setId(id);
        }
    }

    /**
     * This method initiates a batchall, a challenge/dialog to decide on the conduct of a campaign.
     * Prompts the player with a message and options to accept or refuse the batchall.
     *
     * @param campaign       The current campaign.
     * @return {@code true} if the batchall is accepted, {@code false} otherwise.
     */
    //
    public boolean initiateBatchall(Campaign campaign) {
        // Retrieves the title from the resources
        String title = resources.getString("incomingTransmission.title");

        // Retrieves the batchall statement based on infamy and enemy code
        String batchallStatement = BatchallFactions.getGreeting(campaign, enemyCode);

        // An ImageIcon to hold the clan's faction icon
        ImageIcon icon = getFactionLogo(campaign, enemyCode, false);

        // Set the commander's rank and use a name generator to generate the commander's name
        String rank = resources.getString("starColonel.text");
        RandomNameGenerator randomNameGenerator = new RandomNameGenerator();
        String commander = randomNameGenerator.generate(Gender.RANDOMIZE, true, enemyCode);
        commander += ' ' + Bloodname.randomBloodname(enemyCode, Phenotype.MEKWARRIOR,
            campaign.getGameYear()).getName();

        // Construct the batchall message
        String message = String.format(resources.getString("batchallOpener.text"),
            this.getName(), rank, commander, getEnemy().getFullName(campaign.getGameYear()),
            getSystemName(campaign.getLocalDate()));
        message = message + batchallStatement;

        // Append additional message text if the fame is less than 5
        if (campaign.getFameAndInfamy().getFameForFaction(enemyCode) < 5) {
            message = message + resources.getString("batchallCloser.text");
        }

        // Create a text pane to display the message
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(message);
        textPane.setEditable(false);

        // Create a panel to display the icon and the batchall message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(icon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(textPane, BorderLayout.SOUTH);

        // Choose dialog to display based on the fame
        if (campaign.getFameAndInfamy().getFameForFaction(enemyCode) > 4) {
            noBatchallOfferedDialog(panel, title);
            return false;
        } else {
            return batchallDialog(campaign, panel, title);
        }
    }

    /**
     * This function creates a dialog with accept and refuse buttons.
     *
     * @param campaign the current campaign
     * @param panel the panel to display in the dialog
     * @param title the title of the dialog
     * @return {@code true} if the batchall is accepted, {@code false} otherwise
     */
    private boolean batchallDialog(Campaign campaign, JPanel panel, String title) {
        // We use a single-element array to store the result, because we need to modify it inside
        // the action listeners, which requires the variable to be effectively final
        final boolean[] result = {false};

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);  // Set the title of the dialog
        dialog.setLayout(new BorderLayout());  // Set a border layout manager

        // Create an accept button and add its action listener. When clicked, it will set the result
        // to true and close the dialog
        JButton acceptButton = new JButton(resources.getString("responseAccept.text"));
        acceptButton.setToolTipText(resources.getString("responseAccept.tooltip"));
        acceptButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        // Create a refuse button and add its action listener.
        // When clicked, it will trigger a refusal confirmation dialog
        String refusalOption = resources.getString("responseRefuse.text");


        // If the campaign is not a Clan faction, check whether this is the first contact they've had
        // with the Clans.
        // If so, whether at least a month has passed since the Wolf's Dragoons conference on
        // Outreach (which explained who and what the Clans were).
        if (!campaign.getFaction().isClan()
            && campaign.getLocalDate().isBefore(LocalDate.of(3051, 2, 1))) {

            boolean isFirstClanEncounter = BATCHALL_FACTIONS.stream()
                .mapToDouble(factionCode -> campaign.getFameAndInfamy().getFameLevelForFaction(factionCode))
                .noneMatch(infamy -> infamy != 0);

            if (isFirstClanEncounter) {
                refusalOption = resources.getString("responseFirstEncounter.text");
            }
        }

        JButton refuseButton = new JButton(refusalOption);

        refuseButton.setToolTipText(resources.getString("responseRefuse.tooltip"));
        refuseButton.addActionListener(e -> {
            dialog.dispose();  // Close the current dialog
            // Use another method to show a refusal confirmation dialog and store the result
            result[0] = refusalConfirmationDialog(campaign);
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();  // Size the dialog to fit the preferred size and layouts of its components
        dialog.setLocationRelativeTo(null);  // Center the dialog on the screen
        dialog.setModal(true);  // Make the dialog block user input to other top-level windows
        dialog.setVisible(true);  // Show the dialog

        return result[0];  // Return the result when the dialog is disposed
    }

    /**
     * This function displays a dialog asking for final confirmation to refuse a batchall,
     * and performs related actions if the refusal is confirmed.
     *
     * @param campaign the current campaign
     * @return {@code true} if the user accepts the refusal, {@code false} if the user cancels the refusal
     */
    private boolean refusalConfirmationDialog(Campaign campaign) {
        // Create modal JDialog
        JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());

        // Buffer for storing user response (acceptance/refusal)
        final boolean[] response = {false};

        // "Accept" Button
        JButton acceptButton = new JButton(resources.getString("responseAccept.text"));
        acceptButton.setToolTipText(resources.getString("responseAccept.tooltip"));
        acceptButton.addActionListener(e -> {
            response[0] = true;  // User has accepted
            dialog.dispose();  // Close dialog
        });

        // "Refuse" Button
        JButton refuseButton = new JButton(resources.getString("responseRefuse.text"));
        refuseButton.setToolTipText(resources.getString("responseRefuse.tooltip"));
        refuseButton.addActionListener(e -> {
            // Update the campaign state on refusal
            campaign.addReport(resources.getString("refusalReport.text"));
            campaign.getFameAndInfamy().updateFameForFaction(campaign, enemyCode, -1);
            response[0] = false;  // User has refused
            dialog.dispose();  // Close dialog
        });

        // Panel for hosting buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Message Label
        JLabel messageLabel = new JLabel(String.format(resources.getString("refusalConfirmation.text"),
            getEnemy().getFullName(campaign.getGameYear())));

        // Add Message and Buttons to the dialog
        dialog.add(messageLabel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Configure and display dialog
        dialog.pack();  // Fit dialog to its contents
        dialog.setLocationRelativeTo(null);  // Center dialog
        dialog.setModal(true);  // Block access to other windows
        dialog.setVisible(true);  // Display dialog

        // Return user response
        return response[0];
    }

    /**
     * Displays a dialog with a message for when the faction has refused to offer a Batchall due to
     * past player refusals.
     *
     * @param panel The panel to display in the dialog.
     * @param title The title of the dialog.
     */
    private void noBatchallOfferedDialog(JPanel panel, String title) {
        // Create a new JDialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        JButton responseButton = new JButton(resources.getString("responseBringItOn.text"));
        responseButton.setToolTipText(resources.getString("responseBringItOn.tooltip"));
        responseButton.addActionListener(e -> dialog.dispose()); // Dispose the dialog when the button is clicked

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(responseButton); // Add the button to the panel

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack(); // Size the dialog to fit the preferred size and layouts of its components
        dialog.setLocationRelativeTo(null); // Center the dialog on the screen
        dialog.setModal(true); // Set the dialog to be modal
        dialog.setVisible(true); // Show the dialog
    }

    /**
     * This method returns a {@link JPanel} that represents the difficulty skulls for a given mission.
     *
     * @param campaign the campaign for which the difficulty skulls are calculated
     * @return a {@link JPanel} with the difficulty skulls displayed
     */
    public JPanel getContractDifficultySkulls(Campaign campaign) {
        final int ERROR = -99;
        int difficulty = calculateContractDifficulty(campaign);

        // Create a new JFrame
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a pane with FlowLayout
        JPanel panel = new JPanel(new FlowLayout());

        // Load and scale the images
        ImageIcon skullFull = scaleImageIconToWidth(
            new ImageIcon("data/images/misc/challenge_estimate_full.png"), 50);
        ImageIcon skullHalf = scaleImageIconToWidth(
            new ImageIcon("data/images/misc/challenge_estimate_half.png"), 50);

        int iterations = difficulty;

        if (difficulty == ERROR) {
            iterations = 5;
        }

        if (iterations % 2 == 1) {
            iterations--;
            iterations /= 2;

            for (int i = 0; i < iterations; i++) {
                panel.add(new JLabel(skullFull));
            }

            panel.add(new JLabel(skullHalf));
        } else {
            iterations /= 2;

            for (int i = 0; i < iterations; i++) {
                panel.add(new JLabel(skullFull));
            }
        }

        return panel;
    }

    /**
     * Calculates the contract difficulty based on the given campaign and parameters.
     *
     * @param campaign The campaign object containing the necessary data.
     * @return The contract difficulty as an integer value.
     */
    public int calculateContractDifficulty(Campaign campaign) {
        final int ERROR = -99;

        // Estimate the power of the enemy forces
        SkillLevel opposingSkill = modifySkillLevelBasedOnFaction(enemyCode, enemySkill);
        double enemySkillMultiplier = getSkillMultiplier(opposingSkill);
        double enemyPower = estimateMekStrength(campaign, enemyCode, enemyQuality);

        // If we cannot calculate enemy power, abort.
        if (enemyPower == 0) {
            return ERROR;
        }

        enemyPower = (int) round(enemyPower * enemySkillMultiplier);

        // Estimate player power
        double playerPower = estimatePlayerPower(campaign);

        // Estimate the power of allied forces
        // TODO pull these directly from Force Generation instead of using magic numbers
        // TODO estimate the LIAISON ratio by going through each combat lance and
        // getting the actual average (G)BV for an allied heavy/assault mek.
        double allyRatio = switch (getCommandRights()) {
            case INDEPENDENT    -> 0; // no allies
            case LIAISON        -> 0.4; // single allied heavy/assault mek, pure guess for now
            case HOUSE          -> 0.25; // allies with 25% the player's (G)BV budget
            case INTEGRATED     -> 0.5; // allies with 50% the player's (G)BV budget
        };

        if (allyRatio > 0) {
            SkillLevel alliedSkill = modifySkillLevelBasedOnFaction(employerCode, allySkill);
            double allySkillMultiplier = getSkillMultiplier(alliedSkill);
            double allyPower = estimateMekStrength(campaign, employerCode, allyQuality);
            allyPower = allyPower * allySkillMultiplier;
            // If we cannot calculate ally's power, use player power as a fallback.
            if (allyPower == 0) {
                allyPower = playerPower;
            }
            playerPower += allyRatio * allyPower;
            enemyPower += allyRatio * enemyPower;
        }

        // Calculate difficulty based on the percentage difference between the two forces.
        double difference = enemyPower - playerPower;
        double percentDifference = (difference / playerPower) * 100;

        int mappedValue = (int) ceil(Math.abs(percentDifference) / 20);
        if (percentDifference < 0) {
            mappedValue = 5 - mappedValue;
        } else {
            mappedValue = 5 + mappedValue;
        }

        return Math.min(max(mappedValue, 1), 10);
    }

    /**
     * Modifies the skill level based on the faction code.
     *
     * @param factionCode  the code of the faction
     * @param skillLevel   the original skill level
     * @return the modified skill level
     */
    private static SkillLevel modifySkillLevelBasedOnFaction(String factionCode, SkillLevel skillLevel) {
        if (Objects.equals(factionCode, "SOC")) {
            return ELITE;
        }

        if (Factions.getInstance().getFaction(factionCode).isClan()) {
            return parseFromInteger(skillLevel.ordinal() + 1);
        }

        return skillLevel;
    }

    /**
     * Estimates the power of the player in a campaign based on the battle values of their units.
     *
     * @param campaign the object containing the forces and units of the player
     * @return average battle value per player unit OR total BV2 divided by total GBV
     */
    private static double estimatePlayerPower(Campaign campaign) {
        int playerPower = 0;
        int playerGBV = 0;
        int playerUnitCount = 0;
        for (Force force : campaign.getAllForces()) {
            if (!force.isCombatForce()) {
                continue;
            }

            for (UUID unitID : force.getUnits()) {
                Entity entity = campaign.getUnit(unitID).getEntity();
                playerPower += entity.calculateBattleValue();
                playerGBV += entity.getGenericBattleValue();
                playerUnitCount ++;
            }
        }

        if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
            return ((double) playerPower) / playerGBV;
        } else {
            return ((double) playerPower) / playerUnitCount;
        }
    }

    /**
     * Returns the skill BV multiplier based on the given skill level.
     *
     * @param skillLevel the skill level to determine the multiplier
     * @return the skill multiplier
     */
    private static double getSkillMultiplier(SkillLevel skillLevel) {
        return switch (skillLevel) {
            case NONE -> 0.68;
            case ULTRA_GREEN -> 0.77;
            case GREEN -> 0.86;
            case REGULAR -> 1.00;
            case VETERAN -> 1.32;
            case ELITE -> 1.68;
            case HEROIC -> 2.02;
            case LEGENDARY -> 2.31;
        };
    }
    /**
     * Estimates the relative strength for Mek units of a specific faction and quality.
     * Excludes salvage.
     *
     * @param campaign the campaign to estimate the average Mek strength for
     * @param factionCode the code of the faction to estimate the average Mek strength for
     * @param quality the quality of the Meks to calculate the average strength for
     * @return the average battle value OR total BV2 divided by total GBV
     * for Meks of the specified faction and quality OR 0 on error
     */
    private static double estimateMekStrength(Campaign campaign, String factionCode, int quality) {
        final double ERROR = 0;

        RATGenerator ratGenerator = Factions.getInstance().getRATGenerator();
        FactionRecord faction = ratGenerator.getFaction(factionCode);

        if (faction == null) {
            return ERROR;
        }

        UnitTable unitTable;
        try {
            unitTable = findTable(
                faction,
                MEK,
                campaign.getGameYear(),
                String.valueOf(quality),
                new ArrayList<>(),
                NETWORK_NONE,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                0,
                faction);
        } catch (Exception ignored) {
            return ERROR;
        }

        // Otherwise, calculate the estimated power of the faction
        int entries = unitTable.getNumEntries();

        int totalBattleValue = 0;
        int totalGBV = 0;
        int rollingCount = 0;

        for (int i = 0; i < entries; i++) {
            int battleValue = unitTable.getBV(i); // 0 for salvage
            if (0 == battleValue) {
                // Removing this check will break things, see the other comments.
                continue;
            }
            // TODO implement getGBV(int index) in UnitTable to simplify this?
            // getMekSummary(int index) is NULL for salvage.
            int genericBattleValue = unitTable.getMekSummary(i).loadEntity().getGenericBattleValue();
            int weight = unitTable.getEntryWeight(i); // NOT 0 for salvage

            totalBattleValue += battleValue * weight;
            totalGBV += genericBattleValue * weight;
            rollingCount += weight;
        }

        if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
            return ((double) totalBattleValue) / totalGBV;
        } else {
            return ((double) totalBattleValue) / rollingCount;
        }
    }

    /**
     * Get the command roll that was used to determine command rights. Only used by CamOps Contract
     * Market.
     *
     * @return
     */
    public int getCommandRoll() {
        return commandRoll;
    }

    /**
     * Set the command roll that was used to determine command rights. Only used by CamOps Contract
     * Market.
     *
     * @param roll
     */
    public void setCommandRoll(int roll) {
        commandRoll = roll;
    }

    /**
     * Get the salvage roll that was used to determine salvage rights. Only used by CamOps Contract
     * Market.
     *
     * @return
     */
    public int getSalvageRoll() {
        return salvageRoll;
    }

    /**
     * Set the salvage roll that was used to determine salvage rights. Only used by CamOps Contract
     * Market.
     *
     * @param roll
     */
    public void setSalvageRoll(int roll) {
        salvageRoll = roll;
    }

    /**
     * Get the support roll that was used to determine support rights. Only used by CamOps Contract
     * Market.
     *
     * @return
     */
    public int getSupportRoll() {
        return supportRoll;
    }

    /**
     * Set the support roll that was used to determine support rights. Only used by CamOps Contract
     * Market.
     *
     * @param roll
     */
    public void setSupportRoll(int roll) {
        supportRoll = roll;
    }

    /**
     * Get the transport roll that was used to determine transport rights. Only used by CamOps Contract
     * Market.
     *
     * @return
     */
    public int getTransportRoll() {
        return transportRoll;
    }

    /**
     * Set the transport roll that was used to determine transport rights. Only used by CamOps Contract
     * Market.
     *
     * @param roll
     */
    public void setTransportRoll(int roll) {
        transportRoll = roll;
    }
}
