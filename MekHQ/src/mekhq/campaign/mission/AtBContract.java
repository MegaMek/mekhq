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

import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconContractDefinition;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * Contract class for use with Against the Bot rules
 *
 * @author Neoancient
 */
public class AtBContract extends Contract {
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

    protected AtBContract() {
        this(null);
    }

    public AtBContract(String name) {
        super(name, "Independent");
        employerCode = "IND";
        enemyCode = "IND";

        parentContract = null;
        mercSubcontract = false;

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

        allyBotName = getEmployerName(campaign.getGameYear());
        enemyBotName = getEnemyName(campaign.getGameYear());
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

        multiplier *= getContractType().getPaymentMultiplier();

        final Faction employer = Factions.getInstance().getFaction(employerCode);
        final Faction enemy = getEnemy();
        if (employer.isISMajorOrSuperPower() || employer.isClan()) {
            multiplier *= 1.2;
        } else if (enemy.isIndependent()) {
            multiplier *= 1.0;
        } else {
            multiplier *= 1.1;
        }

        if (enemy.isRebelOrPirate()) {
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
            if (requiredLances > maxDeployedLances && campaign.getCampaignOptions().isAdjustPaymentForStrategy()) {
                multiplier *= (double) maxDeployedLances / (double) requiredLances;
                requiredLances = maxDeployedLances;
            }
        }

        setMultiplier(multiplier);
    }

    public void checkMorale(LocalDate today, int dragoonRating) {
        if (null != routEnd) {
            if (today.isAfter(routEnd)) {
                setMoraleLevel(AtBMoraleLevel.NORMAL);
                routEnd = null;
                updateEnemy(today); // mix it up a little
            } else {
                setMoraleLevel(AtBMoraleLevel.ROUT);
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
        if (enemyCode.equals("PIR")) {
            mod -= 2;
        } else if (enemyCode.equals("REB") ||
                isMinorPower(enemyCode) ||
                enemyCode.equals("MERC")) {
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
     * Changes the enemy to a randomly selected faction that's an enemy of
     * the current employer
     */
    private void updateEnemy(LocalDate today) {
        String enemyCode = RandomFactionGenerator.getInstance().getEnemy(
                Factions.getInstance().getFaction(employerCode), false, true);
        setEnemyCode(enemyCode);

        Faction enemyFaction = Factions.getInstance().getFaction(enemyCode);
        setEnemyBotName(enemyFaction.getFullName(today.getYear()));
    }

    public int getRepairLocation(final int unitRating) {
        int repairLocation;
        if (getContractType().isGuerrillaWarfare() || getContractType().isRaidType()) {
            repairLocation = Unit.SITE_FIELD;
        } else if (!getContractType().isGarrisonType()) {
            repairLocation = Unit.SITE_MOBILE_BASE;
        } else {
            repairLocation = Unit.SITE_BAY;
        }

        if (unitRating >= IUnitRating.DRAGOON_B) {
            repairLocation++;
        }

        return Math.min(repairLocation, Unit.SITE_BAY);
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
                if (c.getCampaignOptions().getRandomDependentMethod().isAgainstTheBot()
                        && c.getCampaignOptions().isUseRandomDependentAddition()) {
                    number = Compute.d6();
                    c.addReport("Bonus: " + number + " dependent" + ((number > 1) ? "s" : ""));
                    for (int i = 0; i < number; i++) {
                        Person p = c.newDependent(false);
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
            if (!msl.isEmpty() && (msl.get(0) != null)) {
                try {
                    en = new MechFileParser(msl.get(0).getSourceFile(), msl.get(0).getEntryName()).getEntity();
                } catch (EntityLoadingException ex) {
                    LogManager.getLogger().error("Unable to load entity: " + msl.get(0).getSourceFile()
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
                            final String unitName = c.getUnitMarket().addSingleUnit(c,
                                    UnitMarketType.EMPLOYER, UnitType.MEK, getEmployerFaction(),
                                    IUnitRating.DRAGOON_F, 50);
                            if (unitName != null) {
                                text += String.format("Surplus Sale: %s offered by employer on the <a href='UNIT_MARKET'>unit market</a>", unitName);
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
        return LocalDate.of(today.getYear(), today.getMonth(), Compute.randomInt(today.getMonth().length(today.isLeapYear())) + 1);
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
        if (getContractType().isDiversionaryRaid() || getContractType().isReconRaid()
                || getContractType().isRiotDuty()) {
            int roll = Compute.d6();
            if (roll == 6) {
                campaign.getContractMarket().addFollowup(campaign, this);
                campaign.addReport("Your employer has offered a follow-up contract (available on the <a href=\"CONTRACT_MARKET\">contract market</a>).");
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
                } else if (wn2.getNodeName().equalsIgnoreCase("contractType")
                        || wn2.getNodeName().equalsIgnoreCase("missionType")) { // Mission Type is Legacy - 0.49.2 removal
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
                } else if (wn2.getNodeName().equalsIgnoreCase("allyColorIndex")) { // Legacy - 0.47.15 removal
                    setAllyColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                    if (Camouflage.NO_CAMOUFLAGE.equals(getAllyCamouflage().getCategory())) {
                        getAllyCamouflage().setCategory(Camouflage.COLOUR_CAMOUFLAGE);
                        getAllyCamouflage().setFilename(getAllyColour().name());
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoCategory")) {
                    getEnemyCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyCamoFileName")) {
                    getEnemyCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getTextContent().equalsIgnoreCase("enemyColour")) {
                    setEnemyColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("enemyColorIndex")) { // Legacy - 0.47.15 removal
                    setEnemyColour(PlayerColour.parseFromString(wn2.getTextContent().trim()));
                    if (Camouflage.NO_CAMOUFLAGE.equals(getEnemyCamouflage().getCategory())) {
                        getEnemyCamouflage().setCategory(Camouflage.COLOUR_CAMOUFLAGE);
                        getEnemyCamouflage().setFilename(getEnemyColour().name());
                    }
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
                LogManager.getLogger().error("", e);
            }
        }
    }

    /**
     * Restores any references to other contracts.
     * @param c The Campaign which holds this contract.
     */
    public void restore(Campaign c) {
        if (parentContract != null) {
            Mission m = c.getMission(parentContract.getId());
            if (m != null) {
                if (m instanceof AtBContract) {
                    setParentContract((AtBContract) m);
                } else {
                    LogManager.getLogger().warn(String.format("Parent Contract reference #%d is not an AtBContract for contract %s",
                            parentContract.getId(), getName()));
                    setParentContract(null);
                }
            } else {
                LogManager.getLogger().warn(String.format("Parent Contract #%d reference was not found for contract %s",
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
    }

    public void setEmployerCode(String code, int year) {
        employerCode = code;
        setEmployer(getEmployerName(year));
    }

    public String getEmployerName(int year) {
        return isMercSubcontract() ? "Mercenary (" + getEmployerFaction().getFullName(year) + ")"
                : getEmployerFaction().getFullName(year);
    }

    public Faction getEnemy() {
        return Factions.getInstance().getFaction(getEnemyCode());
    }

    public String getEnemyCode() {
        return enemyCode;
    }

    public String getEnemyName(int year) {
        return Factions.getInstance().getFaction(enemyCode).getFullName(year);
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
        allyBotName = getEmployerName(campaign.getGameYear());
        enemyBotName = getEnemyName(campaign.getGameYear());
    }

    /**
     * Represents a reference to another AtBContract.
     */
    protected static class AtBContractRef extends AtBContract {
        public AtBContractRef(int id) {
            setId(id);
        }
    }
}
