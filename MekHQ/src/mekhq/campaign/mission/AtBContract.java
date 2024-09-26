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

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.rating.IUnitRating;
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
import java.io.File;
import java.io.PrintWriter;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

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
    protected int numBonusParts;

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

    protected AtBContract() {
        this(null);
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
        setAllySkill(SkillLevel.REGULAR);
        allyQuality = IUnitRating.DRAGOON_C;
        setEnemySkill(SkillLevel.REGULAR);
        enemyQuality = IUnitRating.DRAGOON_C;
        allyBotName = "Ally";
        enemyBotName = "Enemy";
        setAllyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.RED.name()));
        setAllyColour(PlayerColour.RED);
        setEnemyCamouflage(new Camouflage(Camouflage.COLOUR_CAMOUFLAGE, PlayerColour.GREEN.name()));
        setEnemyColour(PlayerColour.GREEN);

        extensionLength = 0;

        sharesPct = 0;
        batchallAccepted = true;
        setMoraleLevel(AtBMoraleLevel.NORMAL);
        routEnd = null;
        numBonusParts = 0;
        priorLogisticsFailure = false;
        specialEventScenarioDate = null;
        battleTypeMod = 0;
        nextWeekBattleTypeMod = 0;
    }

    public void initContractDetails(Campaign campaign) {
        if (getEffectiveNumUnits(campaign) <= 12) {
            setOverheadComp(OH_FULL);
        } else if (getEffectiveNumUnits(campaign) <= 48) {
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
        String camouflageDirectory = getCamouflageDirectory(currentYear, factionCode);

        // Use Java File to represent directories
        File workingDirectory = new File(ROOT_DIRECTORY + camouflageDirectory + '/');

        // List subdirectories and loose files in working directory
        File[] folders = workingDirectory.listFiles(File::isDirectory);
        File[] looseFiles = workingDirectory.listFiles();

        // Gather all files
        List<File> allFiles = new ArrayList<>();
        if (looseFiles != null) {
            Collections.addAll(allFiles, looseFiles);
        }
        if (folders != null) {
            for (File folder : folders) {
                File[] folderFiles = folder.listFiles();
                if (folderFiles != null) {
                    Collections.addAll(allFiles, folderFiles);
                }
            }
        }

        // Select a random file to set camouflage, if there are files available
        if (!allFiles.isEmpty()) {
            File randomFile = allFiles.get(new Random().nextInt(allFiles.size()));

            String fileName = randomFile.getName();
            String fileCategory = randomFile.getParent().replaceAll(ROOT_DIRECTORY, "");

            return new Camouflage(fileCategory, fileName);
        } else {
            // Log if no files were found in the directory
            logger.warn(String.format("No files in directory %s - using default camouflage",
                workingDirectory));
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

    public static int getEffectiveNumUnits(Campaign campaign) {
        double numUnits = 0;
        for (UUID uuid : campaign.getForces().getAllUnits(true)) {
            if (null == campaign.getUnit(uuid)) {
                continue;
            }
            switch (campaign.getUnit(uuid).getEntity().getUnitType()) {
                case UnitType.MEK:
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
     * Checks the morale level of the campaign based on various factors.
     *
     * @param campaign The ongoing campaign.
     * @param today The current date.
     * @param dragoonRating The player's dragoon rating
     */
    public void checkMorale(Campaign campaign, LocalDate today, int dragoonRating) {
        if (null != routEnd) {
            if (today.isAfter(routEnd)) {
                setMoraleLevel(AtBMoraleLevel.NORMAL);
                routEnd = null;
                updateEnemy(campaign, today); // mix it up a little
            } else {
                setMoraleLevel(AtBMoraleLevel.BROKEN);
            }
            return;
        }
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        for (Scenario s : getScenarios()) {
            if ((s.getDate() != null) && lastMonth.isAfter(s.getDate())) {
                continue;
            }

            if (s.getStatus().isOverallVictory()) {
                victories++;
            } else if (s.getStatus().isOverallDefeat()) {
                defeats++;
            }
        }

        //
        // From: Official AtB Rules 2.31
        //

        // Enemy skill rating: Green -1, Veteran +1, Elite +2
        int mod = Math.max(getEnemySkill().ordinal() - 3, -1);

        // Player Dragoon/MRBC rating: F +2, D +1, B -1, A -2
        mod -= dragoonRating - IUnitRating.DRAGOON_C;

        // For every 5 player victories in last month: -1
        mod -= victories / 5;

        // For every 2 player defeats in last month: +1
        mod += defeats / 2;

        // "Several weekly events affect the morale roll, so, beyond the
        // modifiers presented here, notice that some events add
        // bonuses/minuses to this roll."
        mod += moraleMod;

        // Enemy type: Pirates: -2
        // Rebels/Mercs/Minor factions: -1
        // Clans: +2
        if (Factions.getInstance().getFaction(enemyCode).isPirate()) {
            mod -= 2;
        } else if (Factions.getInstance().getFaction(enemyCode).isRebel() ||
                isMinorPower(enemyCode) ||
                Factions.getInstance().getFaction(enemyCode).isMercenary()) {
            mod -= 1;
        } else if (Factions.getInstance().getFaction(enemyCode).isClan()) {
            mod += 2;
        }

        // If no player victories in last month: +1
        if (victories == 0) {
            mod++;
        }

        // If no player defeats in last month: -1
        if (defeats == 0) {
            mod--;
        }

        // After finding the applicable modifiers, roll according to the
        // following table to find the new morale level:
        // 1 or less: Morale level decreases 2 levels
        // 2 – 5: Morale level decreases 1 level
        // 6 – 8: Morale level remains the same
        // 9 - 12: Morale level increases 1 level
        // 13 or more: Morale increases 2 levels
        int roll = Compute.d6(2) + mod;

        final AtBMoraleLevel[] moraleLevels = AtBMoraleLevel.values();
        if (roll <= 1) {
            setMoraleLevel(moraleLevels[Math.max(getMoraleLevel().ordinal() - 2, 0)]);
        } else if (roll <= 5) {
            setMoraleLevel(moraleLevels[Math.max(getMoraleLevel().ordinal() - 1, 0)]);
        } else if ((roll >= 9) && (roll <= 12)) {
            setMoraleLevel(moraleLevels[Math.min(getMoraleLevel().ordinal() + 1, moraleLevels.length - 1)]);
        } else if (roll >= 13) {
            setMoraleLevel(moraleLevels[Math.min(getMoraleLevel().ordinal() + 2, moraleLevels.length - 1)]);
        }

        // Enemy defeated, retreats or do not offer opposition to the player
        // forces, equal to a early victory for contracts that are not
        // Garrison-type, and a 1d6-3 (minimum 1) months without enemy
        // activity for Garrison-type contracts.
        if (getMoraleLevel().isRout() && getContractType().isGarrisonType()) {
            routEnd = today.plusMonths(Math.max(1, Compute.d6() - 3)).minusDays(1);
        }

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
        if (campaign.getCampaignOptions().isUseGenericBattleValue()) {
            if (getEnemy().isClan()) {
                setBatchallAccepted(initiateBatchall(campaign));
            }
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

        if (unitRating >= IUnitRating.DRAGOON_B) {
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
        for (Scenario s : getCompletedScenarios()) {
            // Special Scenarios get no points for victory and only -1 for defeat.
            if ((s instanceof AtBScenario) && ((AtBScenario) s).isSpecialScenario()) {
                if (s.getStatus().isOverallDefeat()) {
                    score--;
                }
            } else {
                switch (s.getStatus()) {
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

            if ((s instanceof AtBScenario)
                    && (((AtBScenario) s).getScenarioType() == AtBScenario.BASEATTACK)
                    && ((AtBScenario) s).isAttacker() && s.getStatus().isOverallVictory()) {
                earlySuccess = true;
            } else if (getMoraleLevel().isRout() && !getContractType().isGarrisonType()) {
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

    public void doBonusRoll(Campaign c) {
        int number;
        String rat = null;
        int roll = Compute.d6();
        switch (roll) {
            case 1: /* 1d6 dependents */
                if (c.getCampaignOptions().isUseRandomDependentAddition()) {
                    number = Compute.d6();
                    c.addReport("Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));

                    for (int i = 0; i < number; i++) {
                        Person p = c.newDependent(false, Gender.RANDOMIZE);
                        c.recruitPerson(p);
                    }
                }
                break;
            case 2: /* Recruit (choose) */
                c.addReport("Bonus: hire one recruit of your choice.");
                break;
            case 3: /* 1d6 parts */
                number = Compute.d6();
                numBonusParts += number;
                c.addReport("Bonus: " + number + " part" + ((number > 1) ? "s" : ""));
                break;
            case 4: /* civilian vehicle */
                rat = "CivilianUnits_CivVeh";
                c.addReport("Bonus: civilian vehicle");
                break;
            case 5: /* APC */
                rat = "CivilianUnits_APC";
                c.addReport("Bonus: civilian APC");
                break;
            case 6: /* civilian 'Mek */
                rat = "CivilianUnits_PrimMek";
                c.addReport("Bonus: civilian Mek");
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/mission/AtBContract.java/doBonusRoll: " + roll);
        }

        if (null != rat) {
            Entity en = null;
            RandomUnitGenerator.getInstance().setChosenRAT(rat);
            ArrayList<MekSummary> msl = RandomUnitGenerator.getInstance().generate(1);

            int quality = 3;

            if (c.getCampaignOptions().isUseRandomUnitQualities()) {
                quality = Unit.getRandomUnitQuality(0);
            }

            if (!msl.isEmpty() && (msl.get(0) != null)) {
                try {
                    en = new MekFileParser(msl.get(0).getSourceFile(), msl.get(0).getEntryName()).getEntity();
                } catch (EntityLoadingException ex) {
                    logger.error("Unable to load entity: {}: {}: {}",
                            msl.get(0).getSourceFile(),
                            msl.get(0).getEntryName(),
                            ex.getMessage(),
                            ex);
                }
            }

            if (null != en) {
                c.addNewUnit(en, false, 0, quality);
            } else {
                c.addReport("<html><font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                        + "'>Could not load unit</font></html>");
            }
        }
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

    public void checkEvents(Campaign c) {
        if (c.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        if (c.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                partsAvailabilityLevel++;
                priorLogisticsFailure = false;
            }

            switch (getContractType().generateEventType()) {
                case EVT_BONUSROLL:
                    c.addReport("<b>Special Event:</b> ");
                    doBonusRoll(c);
                    break;
                case EVT_SPECIAL_SCENARIO:
                    c.addReport("<b>Special Event:</b> Special scenario this month");
                    specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
                    specialEventScenarioType = getContractType().generateSpecialScenarioType(c);
                    break;
                case EVT_CIVILDISTURBANCE:
                    c.addReport("<b>Special Event:</b> Civil disturbance<br />Next enemy morale roll gets +1 modifier");
                    moraleMod++;
                    break;
                case EVT_SPORADICUPRISINGS:
                    c.addReport("<b>Special Event:</b> Sporadic uprisings<br />+2 to next enemy morale roll");
                    moraleMod += 2;
                    break;
                case EVT_REBELLION:
                    c.addReport("<b>Special Event:</b> Rebellion<br />+2 to next enemy morale roll");
                    specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
                    specialEventScenarioType = AtBScenario.CIVILIANRIOT;
                    break;
                case EVT_BETRAYAL:
                    String text = "<b>Special Event:</b> Betrayal (employer minor breach)<br />";
                    switch (Compute.d6()) {
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
                    c.addReport(text);
                    break;
                case EVT_TREACHERY:
                    c.addReport(
                            "<b>Special Event:</b> Treachery<br />Bad information from employer. Next Enemy Morale roll gets +1. Employer minor breach.");
                    moraleMod++;
                    employerMinorBreaches++;
                    break;
                case EVT_LOGISTICSFAILURE:
                    c.addReport(
                            "<b>Special Event:</b> Logistics Failure<br />Parts availability for the next month are one level lower.");
                    partsAvailabilityLevel--;
                    priorLogisticsFailure = true;
                    break;
                case EVT_REINFORCEMENTS:
                    c.addReport("<b>Special Event:</b> Reinforcements<br />The next Enemy Morale roll gets a -1.");
                    moraleMod--;
                    break;
                case EVT_SPECIALEVENTS:
                    text = "<b>Special Event:</b> ";
                    switch (Compute.d6()) {
                        case 1:
                            text += "Change of Alliance: Next Enemy Morale roll gets a +1 modifier.";
                            moraleMod++;
                            break;
                        case 2:
                            text += "Internal Dissension";
                            specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
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
                            final String unitName = c.getUnitMarket().addSingleUnit(c,
                                    UnitMarketType.EMPLOYER, UnitType.MEK, getEmployerFaction(),
                                    IUnitRating.DRAGOON_F, 50);
                            if (unitName != null) {
                                text += String.format(
                                        "Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>",
                                        unitName);
                            }
                            break;
                    }
                    c.addReport(text);
                    break;
                case EVT_BIGBATTLE:
                    c.addReport("<b>Special Event:</b> Big battle this month");
                    specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
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
                && !specialEventScenarioDate.isBefore(c.getLocalDate())) {
            LocalDate nextMonday = c.getLocalDate().plusDays(8 - c.getLocalDate().getDayOfWeek().getValue());

            if (specialEventScenarioDate.isBefore(nextMonday)) {
                AtBScenario s = AtBScenarioFactory.createScenario(c, null,
                        specialEventScenarioType, false,
                        specialEventScenarioDate);

                c.addScenario(s, this);
                if (c.getCampaignOptions().isUsePlanetaryConditions()) {
                    s.setPlanetaryConditions(this, c);
                }
                s.setForces(c);
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
        final int roll = Compute.d6();
        if (roll == 1) {
            extension = Math.max(1, getLength() / 2);
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

    public void checkForFollowup(Campaign campaign) {
        if (getContractType().isDiversionaryRaid() || getContractType().isReconRaid()
                || getContractType().isRiotDuty()) {
            int roll = Compute.d6();
            if (roll == 6) {
                campaign.getContractMarket().addFollowup(campaign, this);
                campaign.addReport(
                        "Your employer has offered a follow-up contract (available on the <a href=\"CONTRACT_MARKET\">contract market</a>).");
            }
        }
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
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "numBonusParts", getNumBonusParts());
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
                    setAllySkill(SkillLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("allyQuality")) {
                    allyQuality = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("enemySkill")) {
                    setEnemySkill(SkillLevel.parseFromString(wn2.getTextContent().trim()));
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
                } else if (wn2.getNodeName().equalsIgnoreCase("numBonusParts")) {
                    numBonusParts = Integer.parseInt(wn2.getTextContent());
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

    public int getNumBonusParts() {
        return numBonusParts;
    }

    public void addBonusParts(int num) {
        numBonusParts += num;
    }

    public void useBonusPart() {
        numBonusParts--;
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

        requiredLances = Math.max(getEffectiveNumUnits(campaign) / 6, 1);

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

        // Constants for the directory of the portraits and the file type
        final String PORTRAIT_DIRECTORY = "data/images/force/Pieces/Logos/Clan/";
        final String PORTRAIT_FILE_TYPE = ".png";

        // An ImageIcon to hold the clan's faction icon
        ImageIcon icon;

        // A switch statement that selects the icon based on the enemy code
        switch (enemyCode) {
            // Each case sets the icon to the corresponding image
            case "CBS" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Blood Spirit"
                + PORTRAIT_FILE_TYPE);
            case "CB" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Burrock"
                + PORTRAIT_FILE_TYPE);
            case "CCC" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Cloud Cobra"
                + PORTRAIT_FILE_TYPE);
            case "CCO" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Coyote"
                + PORTRAIT_FILE_TYPE);
            case "CDS" -> {
                if (campaign.getGameYear() >= 3100) {
                    icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Sea Fox"
                        + PORTRAIT_FILE_TYPE);
                } else {
                    icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Diamond Shark"
                        + PORTRAIT_FILE_TYPE);
                }
            }
            case "CFM" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Fire Mandrill"
                + PORTRAIT_FILE_TYPE);
            case "CGB" -> {
                if (campaign.getGameYear() >= 3060) {
                    icon = new ImageIcon(PORTRAIT_DIRECTORY + "Ghost Bear Dominion"
                        + PORTRAIT_FILE_TYPE);
                } else {
                    icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Ghost Bear"
                        + PORTRAIT_FILE_TYPE);
                }
            }
            case "CGS" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Goliath Scorpion"
                + PORTRAIT_FILE_TYPE);
            case "CHH" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Hell's Horses"
                + PORTRAIT_FILE_TYPE);
            case "CIH" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Ice Hellion"
                + PORTRAIT_FILE_TYPE);
            case "CJF" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Jade Falcon"
                + PORTRAIT_FILE_TYPE);
            case "CMG" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Mongoose"
                + PORTRAIT_FILE_TYPE);
            case "CNC" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Nova Cat"
                + PORTRAIT_FILE_TYPE);
            case "CSJ" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Smoke Jaguar"
                + PORTRAIT_FILE_TYPE);
            case "CSR" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Snow Raven"
                + PORTRAIT_FILE_TYPE);
            case "CSA" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Star Adder"
                + PORTRAIT_FILE_TYPE);
            case "CSV" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Steel Viper"
                + PORTRAIT_FILE_TYPE);
            case "CSL" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Stone Lion"
                + PORTRAIT_FILE_TYPE);
            case "CWI" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Widowmaker"
                + PORTRAIT_FILE_TYPE);
            case "CW", "CWE" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Wolf"
                + PORTRAIT_FILE_TYPE);
            case "CWIE" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Clan Wolf-in-Exile"
                + PORTRAIT_FILE_TYPE);
            case "CEI" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Scorpion Empire"
                + PORTRAIT_FILE_TYPE);
            case "RD" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Rasalhague Dominion"
                + PORTRAIT_FILE_TYPE);
            case "RA" -> icon = new ImageIcon(PORTRAIT_DIRECTORY + "Raven Alliance"
                + PORTRAIT_FILE_TYPE);
            default -> icon = new ImageIcon("data/images/force/Pieces/Logos/Inner Sphere/Star League.png");
        }

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
        JButton refuseButton = new JButton(resources.getString("responseRefuse.text"));
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
}
