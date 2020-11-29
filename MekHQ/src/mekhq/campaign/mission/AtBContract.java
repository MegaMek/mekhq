/*
 * AtBContract.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import megamek.common.icons.Camouflage;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.generator.RandomSkillsGenerator;
import megamek.client.generator.RandomUnitGenerator;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;

/**
 * Contract class for use with Against the Bot rules
 *
 * @author Neoancient
 */
public class AtBContract extends Contract implements Serializable {
    private static final long serialVersionUID = 1491090021356604379L;

    public static final int MT_GARRISONDUTY = 0;
    public static final int MT_CADREDUTY = 1;
    public static final int MT_SECURITYDUTY = 2;
    public static final int MT_RIOTDUTY = 3;
    public static final int MT_PLANETARYASSAULT = 4;
    public static final int MT_RELIEFDUTY = 5;
    public static final int MT_GUERRILLAWARFARE = 6;
    public static final int MT_PIRATEHUNTING = 7;
    public static final int MT_DIVERSIONARYRAID = 8;
    public static final int MT_OBJECTIVERAID = 9;
    public static final int MT_RECONRAID = 10;
    public static final int MT_EXTRACTIONRAID = 11;
    public static final int MT_NUM = 12;

    public static final String[] missionTypeNames = {
        "Garrison Duty", "Cadre Duty", "Security Duty", "Riot Duty",
        "Planetary Assault", "Relief Duty", "Guerrilla Warfare",
        "Pirate Hunting", "Diversionary Raid", "Objective Raid",
        "Recon Raid", "Extraction Raid"
    };

    public static final int MORALE_ROUT = 0;
    public static final int MORALE_VERYLOW = 1;
    public static final int MORALE_LOW = 2;
    public static final int MORALE_NORMAL = 3;
    public static final int MORALE_HIGH = 4;
    public static final int MORALE_INVINCIBLE = 5;
    public static final int MORALE_NUM = 6;

    public static final String[] moraleLevelNames = {
        "Rout", "Very Low", "Low", "Normal", "High", "Invincible"
    };

    public static final int EVT_NOEVENT = -1;
    public static final int EVT_BONUSROLL = 0;
    public static final int EVT_SPECIALMISSION = 1;
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

    protected int missionType;
    protected int allySkill;
    protected int allyQuality;
    protected int enemySkill;
    protected int enemyQuality;
    protected String allyBotName;
    protected String enemyBotName;
    protected String allyCamoCategory;
    protected String allyCamoFileName;
    protected int allyColorIndex;
    protected String enemyCamoCategory;
    protected String enemyCamoFileName;
    protected int enemyColorIndex;

    protected int extensionLength;

    protected int requiredLances;
    protected int moraleLevel;
    protected LocalDate routEnd;
    protected int partsAvailabilityLevel;
    protected int sharesPct;

    protected int playerMinorBreaches;
    protected int employerMinorBreaches;
    protected int contractScoreArbitraryModifier;

    protected int moraleMod = 0;
    protected int numBonusParts;

    /* lasts for a month, then removed at next events roll */
    protected boolean priorLogisticsFailure;
    /* If the date is non-null, there will be a special mission or big battle
     * on that date, but the scenario is not generated until the other battle
     * rolls for the week.
     */
    protected LocalDate specialEventScenarioDate;
    protected int specialEventScenarioType;
    /* Lasts until end of contract */
    protected int battleTypeMod;
    /* Only applies to next week */
    protected int nextWeekBattleTypeMod;

    protected AtBContract() {
        this(null);
    }

    public AtBContract(String name) {
        super(name, "Independent");
        employerCode = "IND";
        enemyCode = "IND";

        parentContract = null;
        mercSubcontract = false;

        missionType = MT_GARRISONDUTY;
        allySkill = RandomSkillsGenerator.L_REG;
        allyQuality = IUnitRating.DRAGOON_C;
        enemySkill = RandomSkillsGenerator.L_REG;
        enemyQuality = IUnitRating.DRAGOON_C;
        allyBotName = "Ally";
        enemyBotName = "Enemy";
        allyCamoCategory = Camouflage.NO_CAMOUFLAGE;
        allyCamoFileName = null;
        allyColorIndex = 1;
        enemyCamoCategory = Camouflage.NO_CAMOUFLAGE;
        enemyCamoFileName = null;
        enemyColorIndex = 2;

        extensionLength = 0;

        sharesPct = 0;
        moraleLevel = MORALE_NORMAL;
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

        allyBotName = getEmployerName(campaign.getGameYear());
        enemyBotName = getEnemyName(campaign.getGameYear());
    }

    public void calculateLength(boolean variable) {
        if (variable) {
            calculateVariableLength();
        } else {
            switch (missionType) {
                case AtBContract.MT_CADREDUTY:
                    setLength(12);
                    break;
                case AtBContract.MT_GARRISONDUTY:
                    setLength(18);
                    break;
                case AtBContract.MT_SECURITYDUTY:
                case AtBContract.MT_PIRATEHUNTING:
                    setLength(6);
                    break;
                case AtBContract.MT_DIVERSIONARYRAID:
                case AtBContract.MT_EXTRACTIONRAID:
                case AtBContract.MT_OBJECTIVERAID:
                case AtBContract.MT_RECONRAID:
                    setLength(3);
                    break;
                case AtBContract.MT_GUERRILLAWARFARE:
                    setLength(24);
                    break;
                case AtBContract.MT_PLANETARYASSAULT:
                case AtBContract.MT_RELIEFDUTY:
                    setLength(9);
                    break;
                case AtBContract.MT_RIOTDUTY:
                    setLength(4);
                    break;
            }
        }
    }

    /* Variable contract lengths taken from AtB v. 2.25 */
    private void calculateVariableLength() {
        switch (missionType) {
            case MT_CADREDUTY:
            case MT_SECURITYDUTY:
                setLength(4);
                break;
            case MT_GARRISONDUTY:
                setLength(9 + Compute.d6(3));
                break;
            case MT_DIVERSIONARYRAID:
            case MT_RECONRAID:
                setLength(1);
                break;
            case MT_EXTRACTIONRAID:
                setLength(3 + enemySkill);
                break;
            case MT_GUERRILLAWARFARE:
            case MT_RIOTDUTY:
                setLength(6);
                break;
            case MT_OBJECTIVERAID:
            case MT_PIRATEHUNTING:
                setLength(3 + Compute.randomInt(3));
                break;
            case MT_PLANETARYASSAULT:
            case MT_RELIEFDUTY:
                setLength(4 + Compute.randomInt(3));
                break;
        }
    }

    public void calculatePartsAvailabilityLevel(Campaign campaign) {
        /* AtB rules apply -1 from 2950 to 3040, but MekHQ accounts
         * for era variations already
         */
        switch (missionType) {
            case MT_GUERRILLAWARFARE:
                partsAvailabilityLevel = 0;
                break;
            case MT_DIVERSIONARYRAID:
            case MT_OBJECTIVERAID:
            case MT_RECONRAID:
            case MT_EXTRACTIONRAID:
                partsAvailabilityLevel = 1;
                break;
            case MT_PLANETARYASSAULT:
            case MT_RELIEFDUTY:
                partsAvailabilityLevel = 2;
                break;
            case MT_PIRATEHUNTING:
                partsAvailabilityLevel = 3;
                break;
            default:
                partsAvailabilityLevel = 4;
        }
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
                case UnitType.AERO:
                    if (campaign.getCampaignOptions().getUseAero()) {
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

    public static boolean isMinorPower(String fName) {
        Faction faction = Faction.getFaction(fName);
        if (null != faction) {
            return !RandomFactionGenerator.getInstance().getFactionHints().isISMajorPower(faction) &&
                    !faction.isClan();
        } else {
            return false;
        }
    }

    public void calculatePaymentMultiplier(Campaign campaign) {
        int unitRatingMod = campaign.getUnitRatingMod();
        double multiplier = 1.0;
        // IntOps reputation factor then Dragoons rating
        if (campaign.getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            multiplier *= (unitRatingMod * 0.2) + 0.5;
        } else {
            if (unitRatingMod >= IUnitRating.DRAGOON_A) {
                multiplier *= 2.0;
            } else if (unitRatingMod == IUnitRating.DRAGOON_B) {
                multiplier *= 1.5;
            } else if (unitRatingMod == IUnitRating.DRAGOON_D) {
                multiplier *= 0.8;
            } else if (unitRatingMod == IUnitRating.DRAGOON_F) {
                multiplier *= 0.5;
            }
        }

        switch (missionType) {
            case MT_CADREDUTY:
                multiplier *= 0.8;
                break;
            case MT_SECURITYDUTY:
                multiplier *= 1.2;
                break;
            case MT_DIVERSIONARYRAID:
                multiplier *= 1.8;
                break;
            case MT_EXTRACTIONRAID:
                multiplier *= 1.6;
                break;
            case MT_GUERRILLAWARFARE:
                multiplier *= 2.1;
                break;
            case MT_OBJECTIVERAID:
                multiplier *= 1.6;
                break;
            case MT_PLANETARYASSAULT:
                multiplier *= 1.5;
                break;
            case MT_RECONRAID:
                multiplier *= 1.6;
                break;
            case MT_RELIEFDUTY:
                multiplier *= 1.4;
                break;
        }

        Faction employer = Faction.getFaction(employerCode);
        if ((null != employer)
                && (RandomFactionGenerator.getInstance().getFactionHints().isISMajorPower(employer)
                || employer.isClan())) {
            multiplier *= 1.2;
        } else if (enemyCode.equals("IND") ||
                enemyCode.equals("PIND")) {
            multiplier *= 1.0;
        } else {
            multiplier *= 1.1;
        }
        if (enemyCode.equals("REB") || enemyCode.equals("PIR")) {
            multiplier *= 1.1;
        }

        int cmdrStrategy = 0;
        if (campaign.getFlaggedCommander() != null &&
                campaign.getFlaggedCommander().getSkill(SkillType.S_STRATEGY) != null) {
            cmdrStrategy = campaign.getFlaggedCommander().
                    getSkill(SkillType.S_STRATEGY).getLevel();
        }
        int maxDeployedLances =
            campaign.getCampaignOptions().getBaseStrategyDeployment() +
            campaign.getCampaignOptions().getAdditionalStrategyDeployment() *
            cmdrStrategy;

        if (isSubcontract()) {
            requiredLances = 1;
        } else {
            requiredLances = Math.max(getEffectiveNumUnits(campaign) / 6, 1);
            if (requiredLances > maxDeployedLances && campaign.getCampaignOptions().getAdjustPaymentForStrategy()) {
                multiplier *= (double)maxDeployedLances / (double)requiredLances;
                requiredLances = maxDeployedLances;
            }
        }

        setMultiplier(multiplier);
    }

    public void checkMorale(LocalDate today, int dragoonRating) {
        if (null != routEnd) {
            if (today.isAfter(routEnd)) {
                moraleLevel = MORALE_NORMAL;
                routEnd = null;
            } else {
                moraleLevel = MORALE_ROUT;
            }
            return;
        }
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        for (Scenario s : getScenarios()) {
            if (lastMonth.isAfter(s.getDate())) {
                continue;
            }

            if ((s.getStatus() == Scenario.S_VICTORY) ||
                    (s.getStatus() == Scenario.S_MVICTORY)) {
                victories++;
            } else if ((s.getStatus() == Scenario.S_DEFEAT) ||
                    (s.getStatus() == Scenario.S_MDEFEAT)) {
                defeats++;
            }
        }

        //
        // From: Official AtB Rules 2.31
        //

        // Enemy skill rating: Green -1, Veteran +1, Elite +2
        int mod = Math.max(enemySkill - 2, -1);

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
        if (enemyCode.equals("PIR")) {
            mod -= 2;
        } else if (enemyCode.equals("REB") ||
                isMinorPower(enemyCode) ||
                enemyCode.equals("MERC")) {
            mod -= 1;
        } else if (Faction.getFaction(enemyCode).isClan()) {
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
        int roll = Compute.d6(2) + mod;

        // 1 or less: Morale level decreases 2 levels
        if (roll <= 1) {
            moraleLevel -= 2;
        }
        // 2 – 5: Morale level decreases 1 level
        else if (roll <= 5) {
            moraleLevel -= 1;
        }
        // 6 – 8: Morale level remains the same
        // 9 - 12: Morale level increases 1 level
        else if ((roll >= 9) && (roll <= 12)) {
            moraleLevel += 1;
        }
        // 13 or more: Morale increases 2 levels
        else if (roll >= 13) {
            moraleLevel += 2;
        }

        // Clamp morale to 0 - 5
        if (moraleLevel < 0) {
            moraleLevel = 0;
        } else if (moraleLevel > 5) {
            moraleLevel = 5;
        }

        // Enemy defeated, retreats or do not offer opposition to the player
        // forces, equal to a early victory for contracts that are not
        // Garrison-type, and a 1d6-3 (minimum 1) months without enemy
        // activity for Garrison-type contracts.
        if ((moraleLevel == 0) && (missionType <= MT_RIOTDUTY)) {
            routEnd = today.plusMonths(Math.max(1, Compute.d6() - 3)).minusDays(1);
        }

        moraleMod = 0;
    }

    public int getRepairLocation(int dragoonRating) {
        int retval = Unit.SITE_BAY;
        if ((missionType == MT_GUERRILLAWARFARE) ||
                (missionType >= MT_DIVERSIONARYRAID)) {
            retval = Unit.SITE_FIELD;
        } else if (missionType > MT_RIOTDUTY) {
            retval = Unit.SITE_MOBILE_BASE;
        }

        if (dragoonRating >= IUnitRating.DRAGOON_B) {
            retval++;
        }
        return Math.min(retval, Unit.SITE_BAY);
    }

    public void addMoraleMod(int mod) {
        moraleMod += mod;
    }

    public AtBLanceRole getRequiredLanceType() {
        return getRequiredLanceType(missionType);
    }

    public static AtBLanceRole getRequiredLanceType(int missionType) {
        switch (missionType) {
            case MT_CADREDUTY:
                return AtBLanceRole.TRAINING;
            case MT_GARRISONDUTY:
            case MT_SECURITYDUTY:
            case MT_RIOTDUTY:
                return AtBLanceRole.DEFENCE;
            case MT_GUERRILLAWARFARE:
            case MT_PIRATEHUNTING:
            case MT_PLANETARYASSAULT:
            case MT_RELIEFDUTY:
                return AtBLanceRole.FIGHTING;
            case MT_DIVERSIONARYRAID:
            case MT_EXTRACTIONRAID:
            case MT_OBJECTIVERAID:
            case MT_RECONRAID:
                return AtBLanceRole.SCOUTING;
            default:
                return AtBLanceRole.UNASSIGNED;
        }
    }

    public int getScore() {
        int score = employerMinorBreaches - playerMinorBreaches;
        int battles = 0;
        boolean earlySuccess = false;
        for (Scenario s : getScenarios()) {

            /* Special Missions get no points for victory and and only -1
             * for defeat.
             */
            if (s instanceof AtBScenario && ((AtBScenario)s).isSpecialMission()) {
                if (s.getStatus() == Scenario.S_DEFEAT ||
                        s.getStatus() == Scenario.S_MDEFEAT) {
                    score--;
                }
            } else {
                switch (s.getStatus()) {
                    case Scenario.S_VICTORY:
                    case Scenario.S_MVICTORY:
                        score++;
                        battles++;
                        break;
                    case Scenario.S_DEFEAT:
                        score -= 2;
                        battles++;
                        break;
                    case Scenario.S_MDEFEAT:
                        //special mission defeat
                        score--;
                        break;
                }
            }
            if (s instanceof AtBScenario
                    && ((AtBScenario)s).getScenarioType() == AtBScenario.BASEATTACK
                    && ((AtBScenario)s).isAttacker()
                    && (s.getStatus() == Scenario.S_VICTORY ||
                    s.getStatus() == Scenario.S_MVICTORY)) {
                earlySuccess = true;
            }
            if (missionType > MT_RIOTDUTY && moraleLevel == MORALE_ROUT) {
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
        final String METHOD_NAME = "doBonusRoll(Campaign)"; //$NON-NLS-1$

        int number;
        String rat = null;
        int roll = Compute.d6();
        switch (roll) {
        case 1: /* 1d6 dependents */
            if (c.getCampaignOptions().canAtBAddDependents()) {
                number = Compute.d6();
                c.addReport("Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));
                for (int i = 0; i < number; i++) {
                    Person p = c.newDependent(Person.T_ASTECH, false);
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
            c.addReport("Bonus: " + number + " part" + ((number>1)?"s":""));
            break;
        case 4: /* civilian vehicle */
            rat = "CivilianUnits_CivVeh";
            c.addReport("Bonus: civilian vehicle");
            break;
        case 5: /* APC */
            rat = "CivilianUnits_APC";
            c.addReport("Bonus: civilian APC");
            break;
        case 6: /* civilian 'Mech */
            rat = "CivilianUnits_PrimMech";
            c.addReport("Bonus: civilian Mek");
            break;
        }
        if (null != rat) {
            Entity en = null;
            RandomUnitGenerator.getInstance().setChosenRAT(rat);
            ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
            if ((msl.size() > 0) && (msl.get(0) != null)) {
                try {
                    en = new MechFileParser(msl.get(0).getSourceFile(), msl.get(0).getEntryName()).getEntity();
                } catch (EntityLoadingException ex) {
                    MekHQ.getLogger().error(this, "Unable to load entity: " + msl.get(0).getSourceFile()
                            + ": " + msl.get(0).getEntryName() + ": " + ex.getMessage(), ex);
                }
            }

            if (null != en) {
                c.addNewUnit(en, false, 0);
            } else {
                c.addReport("<html><font color='red'>Could not load unit</font></html>");
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

    public void checkEvents(Campaign c) {
        if (c.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            nextWeekBattleTypeMod = 0;
        }

        if (c.getLocalDate().getDayOfMonth() == 1) {
            if (priorLogisticsFailure) {
                partsAvailabilityLevel++;
                priorLogisticsFailure = false;
            }

            int event;

            int roll = Compute.randomInt(20) + 1;

            switch (missionType) {
                case MT_DIVERSIONARYRAID:
                case MT_OBJECTIVERAID:
                case MT_RECONRAID:
                case MT_EXTRACTIONRAID:
                    if (roll < 10) event = EVT_BONUSROLL;
                    else if (roll < 14) event = EVT_SPECIALMISSION;
                    else if (roll < 16) event = EVT_BETRAYAL;
                    else if (roll < 17) event = EVT_TREACHERY;
                    else if (roll < 18) event = EVT_LOGISTICSFAILURE;
                    else if (roll < 19) event = EVT_REINFORCEMENTS;
                    else if (roll < 20) event = EVT_SPECIALEVENTS;
                    else event = EVT_BIGBATTLE;
                    break;
                case MT_GARRISONDUTY:
                    if (roll < 8) event = EVT_BONUSROLL;
                    else if (roll < 12) event = EVT_SPECIALMISSION;
                    else if (roll < 13) event = EVT_CIVILDISTURBANCE;
                    else if (roll < 14) event = EVT_SPORADICUPRISINGS;
                    else if (roll < 15) event = EVT_REBELLION;
                    else if (roll < 16) event = EVT_BETRAYAL;
                    else if (roll < 17) event = EVT_TREACHERY;
                    else if (roll < 18) event = EVT_LOGISTICSFAILURE;
                    else if (roll < 19) event = EVT_REINFORCEMENTS;
                    else if (roll < 20) event = EVT_SPECIALEVENTS;
                    else event = EVT_BIGBATTLE;
                    break;
                case MT_RIOTDUTY:
                    if (roll < 8) event = EVT_BONUSROLL;
                    else if (roll < 11) event = EVT_SPECIALMISSION;
                    else if (roll < 12) event = EVT_CIVILDISTURBANCE;
                    else if (roll < 13) event = EVT_SPORADICUPRISINGS;
                    else if (roll < 15) event = EVT_REBELLION;
                    else if (roll < 16) event = EVT_BETRAYAL;
                    else if (roll < 17) event = EVT_TREACHERY;
                    else if (roll < 18) event = EVT_LOGISTICSFAILURE;
                    else if (roll < 19) event = EVT_REINFORCEMENTS;
                    else if (roll < 20) event = EVT_SPECIALEVENTS;
                    else event = EVT_BIGBATTLE;
                    break;
                case MT_PIRATEHUNTING:
                    if (roll < 10) event = EVT_BONUSROLL;
                    else if (roll < 14) event = EVT_SPECIALMISSION;
                    else if (roll < 15) event = EVT_CIVILDISTURBANCE;
                    else if (roll < 16) event = EVT_BETRAYAL;
                    else if (roll < 17) event = EVT_TREACHERY;
                    else if (roll < 18) event = EVT_LOGISTICSFAILURE;
                    else if (roll < 19) event = EVT_REINFORCEMENTS;
                    else if (roll < 20) event = EVT_SPECIALEVENTS;
                    else event = EVT_BIGBATTLE;
                    break;
                default:
                    if (roll < 10) event = EVT_BONUSROLL;
                    else if (roll < 15) event = EVT_SPECIALMISSION;
                    else if (roll < 16) event = EVT_BETRAYAL;
                    else if (roll < 17) event = EVT_TREACHERY;
                    else if (roll < 18) event = EVT_LOGISTICSFAILURE;
                    else if (roll < 19) event = EVT_REINFORCEMENTS;
                    else if (roll < 20) event = EVT_SPECIALEVENTS;
                    else event = EVT_BIGBATTLE;
            }
            switch (event) {
                case EVT_BONUSROLL:
                    c.addReport("<b>Special Event:</b> ");
                    doBonusRoll(c);
                    break;
                case EVT_SPECIALMISSION:
                    c.addReport("<b>Special Event:</b> Special mission this month");
                    specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
                    specialEventScenarioType = findSpecialMissionType();
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
                    c.addReport("<b>Special Event:</b> Treachery<br />Bad information from employer. Next Enemy Morale roll gets +1. Employer minor breach.");
                    moraleMod++;
                    employerMinorBreaches++;
                    break;
                case EVT_LOGISTICSFAILURE:
                    c.addReport("<b>Special Event:</b> Logistics Failure<br />Parts availability for the next month are one level lower.");
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
                            String unit = c.getUnitMarket().addSingleUnit(c, UnitMarketType.EMPLOYER,
                                UnitType.MEK, getEmployerCode(), IUnitRating.DRAGOON_F, 50);
                            if (unit != null) {
                                text += String.format("Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>", unit);
                            }
                            break;
                    }
                    c.addReport(text);
                    break;
                case EVT_BIGBATTLE:
                    c.addReport("<b>Special Event:</b> Big battle this month");
                    specialEventScenarioDate = getRandomDayOfMonth(c.getLocalDate());
                    specialEventScenarioType = findBigBattleType();
                    break;
            }
        }
        /* If the campaign somehow gets past the scheduled date (such as by
         * changing the date in the campaign options), ignore it rather
         * than generating a new scenario in the past. The event will still be
         * available (if the campaign date is restored) until another special mission
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
                if (c.getCampaignOptions().getUsePlanetaryConditions()) {
                    s.setPlanetaryConditions(this, c);
                }
                s.setForces(c);
                specialEventScenarioDate = null;
            }
        }
    }

    public LocalDate getRandomDayOfMonth(LocalDate today) {
        return LocalDate.of(today.getYear(), today.getMonth(), Compute.randomInt(today.getMonth().length(today.isLeapYear())) + 1);
    }

    public int findSpecialMissionType() {
        int roll = Compute.randomInt(20) + 1;
        if (missionType >= MT_DIVERSIONARYRAID) {
            if (roll <= 1) return AtBScenario.OFFICERDUEL;
            if (roll <= 2) return AtBScenario.ACEDUEL;
            if (roll <= 6) return AtBScenario.AMBUSH;
            if (roll <= 7) return AtBScenario.CIVILIANHELP;
            if (roll <= 8) return AtBScenario.ALLIEDTRAITORS;
            if (roll <= 12) return AtBScenario.PRISONBREAK;
            if (roll <= 16) return AtBScenario.STARLEAGUECACHE1;
            return AtBScenario.STARLEAGUECACHE2;
        } else if (missionType == MT_GARRISONDUTY) {
            if (roll <= 2) return AtBScenario.OFFICERDUEL;
            if (roll <= 4) return AtBScenario.ACEDUEL;
            if (roll <= 6) return AtBScenario.AMBUSH;
            if (roll <= 10) return AtBScenario.CIVILIANHELP;
            if (roll <= 12) return AtBScenario.ALLIEDTRAITORS;
            if (roll <= 16) return AtBScenario.STARLEAGUECACHE1;
            return AtBScenario.STARLEAGUECACHE2;
        } else if (missionType == MT_RIOTDUTY) {
            if (roll <= 1) return AtBScenario.OFFICERDUEL;
            if (roll <= 3) return AtBScenario.ACEDUEL;
            if (roll <= 7) return AtBScenario.AMBUSH;
            if (roll <= 8) return AtBScenario.CIVILIANHELP;
            if (roll <= 12) return AtBScenario.ALLIEDTRAITORS;
            if (roll <= 16) return AtBScenario.STARLEAGUECACHE1;
            return AtBScenario.STARLEAGUECACHE2;
        } else if (missionType == MT_PIRATEHUNTING) {
            if (roll <= 1) return AtBScenario.OFFICERDUEL;
            if (roll <= 4) return AtBScenario.ACEDUEL;
            if (roll <= 7) return AtBScenario.AMBUSH;
            if (roll <= 11) return AtBScenario.CIVILIANHELP;
            if (roll <= 12) return AtBScenario.ALLIEDTRAITORS;
            if (roll <= 16) return AtBScenario.STARLEAGUECACHE1;
            return AtBScenario.STARLEAGUECACHE2;
        } else {
            if (roll <= 2) return AtBScenario.OFFICERDUEL;
            if (roll <= 4) return AtBScenario.ACEDUEL;
            if (roll <= 6) return AtBScenario.AMBUSH;
            if (roll <= 8) return AtBScenario.CIVILIANHELP;
            if (roll <= 10) return AtBScenario.ALLIEDTRAITORS;
            if (roll <= 12) return AtBScenario.PRISONBREAK;
            if (roll <= 16) return AtBScenario.STARLEAGUECACHE1;
            return AtBScenario.STARLEAGUECACHE2;
        }
    }

    public int findBigBattleType() {
        int roll = Compute.d6();
        if (missionType >= MT_DIVERSIONARYRAID) {
            if (roll <= 1) return AtBScenario.ALLYRESCUE;
            if (roll <= 2) return AtBScenario.CONVOYRESCUE;
            if (roll <= 5) return AtBScenario.CONVOYATTACK;
            return AtBScenario.PIRATEFREEFORALL;
        } else if (missionType == MT_GARRISONDUTY) {
            if (roll <= 2) return AtBScenario.ALLYRESCUE;
            if (roll <= 3) return AtBScenario.CIVILIANRIOT;
            if (roll <= 5) return AtBScenario.CONVOYRESCUE;
            return AtBScenario.PIRATEFREEFORALL;
        } else if (missionType == MT_RIOTDUTY) {
            if (roll <= 1) return AtBScenario.ALLYRESCUE;
            if (roll <= 4) return AtBScenario.CIVILIANRIOT;
            if (roll <= 5) return AtBScenario.CONVOYRESCUE;
            return AtBScenario.PIRATEFREEFORALL;
        } else if (missionType == MT_PIRATEHUNTING) {
            if (roll <= 1) return AtBScenario.ALLYRESCUE;
            if (roll <= 3) return AtBScenario.CONVOYRESCUE;
            if (roll <= 4) return AtBScenario.CONVOYATTACK;
            return AtBScenario.PIRATEFREEFORALL;
        } else {
            if (roll <= 2) return AtBScenario.ALLYRESCUE;
            if (roll <= 3) return AtBScenario.CIVILIANRIOT;
            if (roll <= 4) return AtBScenario.CONVOYRESCUE;
            if (roll <= 5) return AtBScenario.CONVOYATTACK;
            return AtBScenario.PIRATEFREEFORALL;
        }
    }

    public boolean contractExtended (Campaign campaign) {
        if ((getMissionType() != MT_PIRATEHUNTING) && (getMissionType() != MT_RIOTDUTY)) {
            String warName = RandomFactionGenerator.getInstance()
                    .getFactionHints().getCurrentWar(Faction.getFaction(getEmployerCode()),
                    Faction.getFaction(getEnemyCode()), campaign.getLocalDate());
            if (null != warName) {
                int extension = 0;
                int roll = Compute.d6();
                if (roll == 1) {
                    extension = Math.max(1, getLength() / 2);
                }
                if (roll == 2) {
                    extension = 1;
                }
                if (extension > 0) {
                    campaign.addReport("Due to the " + warName +
                            " crisis your employer has invoked the emergency clause and extended the contract " +
                            extension + ((extension == 1) ? " month" : " months"));
                    setEndDate(getEndingDate().plusMonths(extension));
                    extensionLength += extension;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Money getMonthlyPayOut() {
        if (extensionLength == 0) {
            return super.getMonthlyPayOut();
        }
        /* The transport clause and the advance monies have already been
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
        if ((getMissionType() == AtBContract.MT_DIVERSIONARYRAID)
                || (getMissionType() == AtBContract.MT_RECONRAID)
                || (getMissionType() == AtBContract.MT_RIOTDUTY)) {
            int roll = Compute.d6();
            if (roll == 6) {
                campaign.getContractMarket().addFollowup(campaign, this);
                campaign.addReport("Your employer has offered a follow-up contract (available on the <a href=\"CONTRACT_MARKET\">contract market</a>).");
            }
        }
    }

    protected void writeToXmlBegin(PrintWriter pw1, int indent) {
        super.writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<employerCode>"
                +employerCode
                +"</employerCode>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyCode>"
                +enemyCode
                +"</enemyCode>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<missionType>"
                +missionType
                +"</missionType>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allySkill>"
                +allySkill
                +"</allySkill>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allyQuality>"
                +allyQuality
                +"</allyQuality>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemySkill>"
                +enemySkill
                +"</enemySkill>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyQuality>"
                +enemyQuality
                +"</enemyQuality>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allyBotName>"
                +allyBotName
                +"</allyBotName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyBotName>"
                +enemyBotName
                +"</enemyBotName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allyCamoCategory>"
                +allyCamoCategory
                +"</allyCamoCategory>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allyCamoFileName>"
                +allyCamoFileName
                +"</allyCamoFileName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<allyColorIndex>"
                +allyColorIndex
                +"</allyColorIndex>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyCamoCategory>"
                +enemyCamoCategory
                +"</enemyCamoCategory>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyCamoFileName>"
                +enemyCamoFileName
                +"</enemyCamoFileName>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<enemyColorIndex>"
                +enemyColorIndex
                +"</enemyColorIndex>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<requiredLances>"
                +requiredLances
                +"</requiredLances>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<moraleLevel>"
                +moraleLevel
                +"</moraleLevel>");
        if (null != routEnd) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "routEnd",
                    MekHqXmlUtil.saveFormattedDate(routEnd));
        }
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<numBonusParts>"
                +numBonusParts
                +"</numBonusParts>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<partsAvailabilityLevel>"
                +partsAvailabilityLevel
                +"</partsAvailabilityLevel>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<extensionLength>"
                +extensionLength
                +"</extensionLength>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<sharesPct>"
                +sharesPct
                +"</sharesPct>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<playerMinorBreaches>"
                +playerMinorBreaches
                +"</playerMinorBreaches>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<employerMinorBreaches>"
                +employerMinorBreaches
                +"</employerMinorBreaches>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<contractScoreArbitraryModifier>"
                +contractScoreArbitraryModifier
                +"</contractScoreArbitraryModifier>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<priorLogisticsFailure>"
                +priorLogisticsFailure
                +"</priorLogisticsFailure>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<battleTypeMod>"
                +battleTypeMod
                +"</battleTypeMod>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<nextWeekBattleTypeMod>"
                +nextWeekBattleTypeMod
                +"</nextWeekBattleTypeMod>");

        if (null != specialEventScenarioDate) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "specialEventScenarioDate",
                    MekHqXmlUtil.saveFormattedDate(specialEventScenarioDate));
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "specialEventScenarioType", specialEventScenarioType);
        }
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn) throws ParseException {
        super.loadFieldsFromXmlNode(wn);
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("employerCode")) {
                employerCode = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyCode")) {
                enemyCode = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("missionType")) {
                missionType = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("allySkill")) {
                allySkill = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("allyQuality")) {
                allyQuality = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("enemySkill")) {
                enemySkill = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyQuality")) {
                enemyQuality = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("allyBotName")) {
                allyBotName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyBotName")) {
                enemyBotName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("allyCamoCategory")) {
                allyCamoCategory = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("allyCamoFileName")) {
                allyCamoFileName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("allyColorIndex")) {
                allyColorIndex = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoCategory")) {
                enemyCamoCategory = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoFileName")) {
                enemyCamoFileName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("enemyColorIndex")) {
                enemyColorIndex = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("requiredLances")) {
                requiredLances = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("moraleLevel")) {
                moraleLevel = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("routEnd")) {
                routEnd = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("partsAvailabilityLevel")) {
                partsAvailabilityLevel = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("extensionLength")) {
                extensionLength = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("sharesPct")) {
                sharesPct = Integer.parseInt(wn2.getTextContent());
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
                specialEventScenarioDate = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("specialEventScenarioType")) {
                specialEventScenarioType = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    public String getEmployerCode() {
        return employerCode;
    }

    public void setEmployerCode(String code, int year) {
        employerCode = code;
        setEmployer(getEmployerName(year));
    }

    public String getEmployerName(int year) {
        if (mercSubcontract) {
            return "Mercenary (" +
                    Faction.getFaction(employerCode).getFullName(year) + ")";
        }
        return Faction.getFaction(employerCode).getFullName(year);
    }

    public String getEnemyCode() {
        return enemyCode;
    }

    public String getEnemyName(int year) {
        return Faction.getFaction(enemyCode).getFullName(year);
    }

    public void setEnemyCode(String enemyCode) {
        this.enemyCode = enemyCode;
    }

    public int getMissionType() {
        return missionType;
    }

    public void setMissionType(int missionType) {
        this.missionType = missionType;
        setType(missionTypeNames[missionType]);
    }

    public String getMissionTypeName() {
        return missionTypeNames[missionType];
    }

    public int getAllySkill() {
        return allySkill;
    }

    public void setAllySkill(int allySkill) {
        this.allySkill = allySkill;
    }

    public int getEnemySkill() {
        return enemySkill;
    }

    public void setEnemySkill(int enemySkill) {
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

    public String getAllyCamoCategory() {
        return allyCamoCategory;
    }

    public void setAllyCamoCategory(String category) {
        allyCamoCategory = category;
    }

    public String getAllyCamoFileName() {
        return allyCamoFileName;
    }

    public void setAllyCamoFileName(String fileName) {
        allyCamoFileName = fileName;
    }

    public int getAllyColorIndex() {
        return allyColorIndex;
    }

    public void setAllyColorIndex(int index) {
        allyColorIndex = index;
    }

    public String getEnemyCamoCategory() {
        return enemyCamoCategory;
    }

    public void setEnemyCamoCategory(String category) {
        enemyCamoCategory = category;
    }

    public String getEnemyCamoFileName() {
        return enemyCamoFileName;
    }

    public void setEnemyCamoFileName(String fileName) {
        enemyCamoFileName = fileName;
    }

    public int getEnemyColorIndex() {
        return enemyColorIndex;
    }

    public void setEnemyColorIndex(int index) {
        enemyColorIndex = index;
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

    public void adjustPartsAvailabilityLevel(int mod) {
        partsAvailabilityLevel += mod;
    }

    public int getMoraleLevel() {
        return moraleLevel;
    }

    public void setMoraleLevel(int level) {
        moraleLevel = level;
    }

    public String getMoraleLevelName() {
        return moraleLevelNames[moraleLevel];
    }

    public int getSharesPct() {
        return sharesPct;
    }

    public void setSharesPct(int pct) {
        sharesPct = pct;
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
        /*Set ending date; the other calculated values will be replaced
         * from the original contract */
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
        missionType = -1;
        for (int i = 0; i < MT_NUM; i++) {
            if (c.getType().equalsIgnoreCase(missionTypeNames[i])) {
                missionType = i;
                break;
            }
        }
        /* Make a rough guess */
        if (missionType < 0) {
            if (c.getLength() <= 3) {
                missionType = MT_OBJECTIVERAID;
            } else if (c.getLength() >= 12) {
                missionType = MT_GARRISONDUTY;
            } else {
                missionType = MT_PLANETARYASSAULT;
            }
        }
        Faction f = Faction.getFactionFromFullNameAndYear(c.getEmployer(), campaign.getGameYear());
        if (null == f) {
            employerCode = "IND";
        } else {
            employerCode = f.getShortName();
        }

        if (missionType == MT_PIRATEHUNTING) {
            enemyCode = "PIR";
        }
        if (missionType == MT_RIOTDUTY) {
            enemyCode = "REB";
        }

        requiredLances = Math.max(getEffectiveNumUnits(campaign) / 6, 1);
        calculatePartsAvailabilityLevel(campaign);
        allyBotName = getEmployerName(campaign.getGameYear());
        enemyBotName = getEnemyName(campaign.getGameYear());
    }

    public static AtBContract getContractExtension(AtBContract c, int length, Campaign campaign) {
        AtBContract retVal = new AtBContract(c.getName() + " (Ext)");
        retVal.setType(c.getType());
        retVal.setSystemId(c.getSystemId());
        retVal.setDesc(c.getDescription());
        retVal.setStatus(Mission.S_ACTIVE);
        retVal.setLength(length);
        retVal.setStartDate(campaign.getLocalDate());
        /*Set ending date; the other calculated values will be replaced
         * from the original contract */
        retVal.calculateContract(campaign);
        retVal.setMultiplier(c.getMultiplier() * 1.5);
        retVal.setTransportComp(c.getTransportComp());
        retVal.setStraightSupport(c.getStraightSupport());
        retVal.setOverheadComp(c.getOverheadComp());
        retVal.setCommandRights(c.getCommandRights());
        retVal.setBattleLossComp(c.getBattleLossComp());
        retVal.setSalvagePct(c.getSalvagePct());
        retVal.setSalvageExchange(c.isSalvageExchange());
        retVal.setSalvagedByUnit(c.getSalvagedByUnit());
        retVal.setSalvagedByEmployer(c.getSalvagedByEmployer());
        retVal.setSigningBonusPct(c.getSigningBonusPct());
        retVal.setAdvancePct(c.getAdvancePct());
        retVal.setMRBCFee(c.payMRBCFee());

        retVal.setMissionType(c.getMissionType());
        retVal.setEmployerCode(c.getEmployerCode(), campaign.getGameYear());
        retVal.setEnemyCode(c.getEnemyCode());
        retVal.requiredLances = c.getRequiredLances();
        retVal.calculatePartsAvailabilityLevel(campaign);
        retVal.setAllyBotName(c.getAllyBotName());
        retVal.setEnemyBotName(c.getEnemyBotName());

        return retVal;
    }

}
