/*
 * FieldManualMercRevMrbcRating.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.Pilot;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.Warship;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %I% %G%
 * @since 3/12/2012
 */
public class FieldManualMercRevDragoonsRating extends AbstractDragoonsRating {

    private final BigDecimal HUNDRED = new BigDecimal(100);

    private int techSupportNeeded = 0;
    private int medSupportNeeded = 0;
    private int hrSupportNeeded = 0;
    private int techSupportAvailable = 0;
    private int medSupportAvailable = 0;
    private int hrSupportAvailable;
    private int yearsInDebt = 0;

    public FieldManualMercRevDragoonsRating(Campaign campaign) {
        super(campaign);
    }

    @Override
    protected void initValues(boolean reInitialize) {
        if (isInitialized() && !reInitialize) {
            return;
        }

        for (UUID uid : campaign.getForces().getAllUnits()) {
            Unit u = campaign.getUnit(uid);
            if (null == u) {
                continue;
            }

            Person p = u.getCommander();
            if (null != p) {
                commanderList.add(p);
            }

            if (!u.isRepairable()) {
                continue;
            }

            BigDecimal value = getUnitValue(u);
            numberUnits = numberUnits.add(value);

            updateAdvanceTechCount(u, value);

            updateSkillLevel(u, value);

            updateUnitCounts(u.getEntity());

            if (u.getEntity() instanceof Dropship) {
                updateBayCount((Dropship) u.getEntity());
            }

            updateJumpships(u.getEntity());

            updateTechSupportNeeds(u.getEntity());
        }

        updateAvailableSupport();
        updateMedicalAndHrSupportNeeds();
        initialized = true;
    }

    private void updateAvailableSupport() {
        for (Person p : campaign.getPersonnel()) {
            if (p.isTech()) {
                updateTechSupportAvailable(p);
            } else if (p.isDoctor()) {
                updateMedicalSupportAvailable(p);
            } else if ((p.getPrimaryRole() == Person.T_ADMIN_HR) || (p.getPrimaryRole() == Person.T_ADMIN_HR)) {
                updateHumanResourcesSupportAvailable(p);
            }
        }
    }

    private void updateJumpships(Entity en) {
        if (en instanceof Warship) {
            if (en.getDocks() > 0) {
                warhipWithDocsOwner = true;
            } else {
                warshipOwner = true;
            }
        } else if (en instanceof Jumpship) {
            jumpshipOwner = true;
        }
    }

    private void updateBayCount(Dropship ds) {
        for (Bay bay : ds.getTransportBays()) {
            if (bay instanceof MechBay) {
                numberMechBays += bay.getCapacity();
            } else if (bay instanceof BattleArmorBay) {
                numberBaBays += bay.getCapacity() * 4;
            } else if (bay instanceof InfantryBay) {
                numberInfBays += bay.getCapacity() * 28;
            } else if ((bay instanceof LightVehicleBay) || (bay instanceof HeavyVehicleBay)) {
                numberVeeBays += bay.getCapacity();
            } else if ((bay instanceof ASFBay) || (bay instanceof SmallCraftBay)) {
                numberAeroBays += bay.getCapacity();
            }
        }
    }

    private void updateUnitCounts(Entity en) {
        if (en instanceof Mech) {
            numberMech++;
        } else if (en instanceof Tank) {
            numberVee++;
        } else if ((en instanceof Aero) && !(en instanceof Dropship) && !(en instanceof Jumpship)) {
            numberAero++;
        } else if (en instanceof BattleArmor) {
            numberBa += ((Infantry)en).getSquadSize();
        } else if (en instanceof Infantry) {
            numberInf += ((Infantry)en).getSquadN() * ((Infantry)en).getSquadSize();
        }
    }

    /**
     * todo Figure out how to incorporate Naval vessel's & artillery. 
     */
    private void updateTechSupportNeeds(Entity en) {
        double hoursNeeded = 0;
        if (en instanceof Mech) {
            hoursNeeded = Math.floor(en.getWeight() / 5) + 40;
        } else if ((en instanceof SmallCraft)) {
            if (en.getWeight() >= 50000) {
                hoursNeeded = Math.floor(en.getWeight() / 50) + 20;
            } else if (en.getWeight() >= 16000) {
                hoursNeeded = Math.floor(en.getWeight() / 25) + 40;
            } else {
                hoursNeeded = Math.floor(en.getWeight() / 10) + 80;
            }
        } else if (en instanceof Warship) {
            hoursNeeded = Math.floor(en.getWeight() / 125) + 600;
        } else if (en instanceof Jumpship) {
            hoursNeeded = Math.floor(en.getWeight() / 400) + 40;
        } else if (en instanceof ConvFighter) {
            hoursNeeded = Math.floor(en.getWeight() / 2.5) + 20;
        } else if (en instanceof Aero) {
            hoursNeeded = Math.floor(en.getWeight() / 2.5) + 40;
        } else if (en instanceof VTOL) {
            hoursNeeded = Math.floor(en.getWeight() / 5) + 30;
        } else if (en instanceof Tank) {
            hoursNeeded = Math.floor(en.getWeight() / 5) + 20;
        } else if (en instanceof BattleArmor) {
            hoursNeeded = (en.getTotalArmor() * 2) + 5;
        } else if ((en instanceof Infantry) &&
                (EntityMovementMode.HOVER.equals(en.getMovementMode()))
                || EntityMovementMode.INF_MOTORIZED.equals(en.getMovementMode())
                || EntityMovementMode.TRACKED.equals(en.getMovementMode())
                || EntityMovementMode.VTOL.equals(en.getMovementMode())
                || EntityMovementMode.WHEELED.equals(en.getMovementMode())
                || EntityMovementMode.WIGE.equals(en.getMovementMode())) {
            hoursNeeded = Math.floor(en.getWeight() / 5) + 20;
        }

        if (campaign.getCampaignOptions().useQuirks()) {
            if (en.getQuirks().booleanOption("easy_maintain")) {
                hoursNeeded -= hoursNeeded * 0.2;
            } else if (en.getQuirks().booleanOption("difficult_maintain")) {
                hoursNeeded += hoursNeeded * 0.2;
            }
        }

        if (campaign.getCampaignOptions().useFactionModifiers() && en.isClan()) {
            hoursNeeded *= 2;
        } else if (campaign.getCampaignOptions().useEraMods() && (en.getTechLevel() > TechConstants.T_INTRO_BOXSET)) {
            hoursNeeded *= 1.5;
        }

        techSupportNeeded += hoursNeeded;
    }

    private void updateMedicalAndHrSupportNeeds() {
        medSupportNeeded = campaign.getPersonnel().size() / 5;
        hrSupportNeeded = campaign.getPersonnel().size() / 2;
    }

    private int getSupportHours(Person p) {
        switch (p.getExperienceLevel(false)) {
            case(SkillType.EXP_ULTRA_GREEN):
                return 20;
            case(SkillType.EXP_GREEN):
                return 30;
            case(SkillType.EXP_REGULAR):
                return 40;
            case(SkillType.EXP_VETERAN):
                return 45;
            default:
                return 50;
        }
    }

    private void updateTechSupportAvailable(Person p) {
        int hours = getSupportHours(p);
        if (p.isTechSecondary()) {
            hours = (int)Math.floor(hours/2);
        }
        techSupportAvailable += hours;
    }

    private void updateMedicalSupportAvailable(Person p) {
        int hours = getSupportHours(p);
        if (p.getSecondaryRole() == Person.T_DOCTOR) {
            hours = (int)Math.floor(hours/2);
        }
        medSupportAvailable += hours;
    }

    private void updateHumanResourcesSupportAvailable(Person p) {
        int hours = getSupportHours(p);
        if (p.getSecondaryRole() == Person.T_ADMIN_HR) {
            hours = (int)Math.floor(hours/2);
        }
        hrSupportAvailable += hours;
    }

    private void updateSkillLevel(Unit u, BigDecimal value) {

        //Make sure this is a combat unit.
        if ((null == u.getEntity()) || (null == u.getEntity().getCrew())) {
            return;
        }

        //Calculate the unit's average combat skill.
        Pilot p = u.getEntity().getCrew();
        BigDecimal combatSkillAverage;

        //Infantry and Protos do not have a piloting skill.
        if ((u.getEntity() instanceof Infantry) || (u.getEntity() instanceof Protomech)) {
            combatSkillAverage = new BigDecimal(p.getGunnery());

        //All other units use an average of piloting and gunnery.
        } else {
            combatSkillAverage = new BigDecimal(p.getGunnery() + p.getPiloting()).divide(new BigDecimal(2), PRECISION, HALF_EVEN);
        }

        //Add to the running total.
        totalSkillLevels = totalSkillLevels.add(value.multiply(combatSkillAverage));
    }

    @Override
    protected int calculateDragoonRatingScore(boolean recalculate) {
        initValues(recalculate);

        int score = 0;

        score += getExperienceValue();
        score += getCommanderValue();
        score += getCombatRecordValue();
        score += getSupportValue();
        score += getTransportValue();
        score += getTechValue();
        score += getFinancialValue();

        return score;
    }

    @Override
    public int getExperienceValue() {
        BigDecimal averageExperience = calcAverageExperience();
        if (averageExperience.compareTo(greenThreshold) >= 0) {
            return 5;
        } else if (averageExperience.compareTo(regularThreshold) >= 0) {
            return 10;
        } else if (averageExperience.compareTo(veteranThreshold) >= 0) {
            return 20;
        }
        return 40;
    }

    @Override
    public int getCommanderValue() {
        if (getCommander() == null) {
            return 0;
        }

        int value = 0;

        Skill test = getCommander().getSkill(SkillType.S_LEADER);
        if (test != null)
            value += test.getLevel();

        test = getCommander().getSkill(SkillType.S_NEG);
        if (test != null)
            value += test.getLevel();

        test = getCommander().getSkill(SkillType.S_STRATEGY);
        if (test != null)
            value += test.getLevel();

        test = getCommander().getSkill(SkillType.S_TACTICS);
        if (test != null)
            value += test.getLevel();

        /**
         * todo consider adding rpg traits in MekHQ (they would have no impact on megamek).
         * value += (total positive - total negative)
         * See FM: Mercs (rev) pg 154 for a full list.
         */

        return value;
    }

    private BigDecimal getMedicalSupportPercentage() {
        if (medSupportAvailable <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = new BigDecimal(medSupportAvailable).divide(new BigDecimal(medSupportNeeded), PRECISION, HALF_EVEN).multiply(
                HUNDRED).setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0 ? HUNDRED : percent);
    }

    private int getMedicalSupportValue() {
        BigDecimal percent = getMedicalSupportPercentage();
        BigDecimal threshold = new BigDecimal(75);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        percent = percent.subtract(threshold).divide(new BigDecimal(5), PRECISION, HALF_EVEN);
        return percent.setScale(0, RoundingMode.DOWN).intValue() * 2;
    }

    private BigDecimal getHumanResourcePercentage() {
        if (hrSupportAvailable <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = new BigDecimal(hrSupportAvailable).divide(new BigDecimal(hrSupportNeeded), PRECISION, HALF_EVEN).multiply(
                HUNDRED).setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0 ? HUNDRED : percent);
    }

    private int getHumanResourcesValue() {
        BigDecimal percent = getHumanResourcePercentage();
        BigDecimal threshold = new BigDecimal(60);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        percent = percent.subtract(threshold).divide(new BigDecimal(10), PRECISION, HALF_EVEN);
        return percent.setScale(0, RoundingMode.DOWN).intValue();
    }

    private BigDecimal getTechSupportPercentage() {
        if (techSupportAvailable <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = new BigDecimal(techSupportAvailable).divide(new BigDecimal(techSupportNeeded), PRECISION, HALF_EVEN).multiply(
                HUNDRED).setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0 ? HUNDRED : percent);
    }

    private int getTechSupportValue() {
        BigDecimal percent = getTechSupportPercentage();
        BigDecimal threshold = new BigDecimal(60);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        percent = percent.subtract(threshold).divide(new BigDecimal(10), PRECISION, HALF_EVEN);
        return percent.setScale(0, RoundingMode.DOWN).intValue() * 5;
    }

    @Override
    public int getSupportValue() {
        return getTechSupportValue() + getMedicalSupportValue() + getHumanResourcesValue();
    }

    @Override
    public int getYearsInDebt() {
        return getYearsInDebt(true);
    }

    public int getYearsInDebt(boolean recalculate) {
        if (!recalculate) {
            return yearsInDebt;
        }

        //If we're not in debt, no penalty.
        if (!campaign.getFinances().isInDebt()) {
            return 0;
        }

        //Sort the transactions in reverse date order.
        List<Transaction> transactions = campaign.getFinances().getAllTransactions();
        Comparator transactionDateCompare = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return (t2.getDate()).compareTo(t1.getDate());
            }
        };
        Collections.sort(transactions, Collections.reverseOrder(transactionDateCompare));

        //Loop through the transaction list, counting all consecutive years in debt.
        int years = 1;
        for (Transaction t : transactions) {

            //Only count yearly carryovers.
            if (!t.getDescription().equalsIgnoreCase("Carryover from previous year")) {
                continue;
            }

            //If the carryover was negative, count it.  If not, end the cycle.  We only care about the number
            //of years since we were last in the black.
            if (t.getAmount() < 0) {
                years++;
            } else {
                break;
            }
        }

        yearsInDebt = years;
        return years;
    }

    @Override
    public int getFinancialValue() {
        return getYearsInDebt() * -10;
    }

    @Override
    public String getDetails() {
        StringBuffer sb = new StringBuffer("Dragoons Rating:                " + getDragoonRating(false) + "\n");
        sb.append("    Method: FM: Mercenaries (rev)\n\n");

        sb.append("Quality:                        ").append(getExperienceValue()).append("\n");
        sb.append("    Average Experience:   ").append(getExperienceLevelName(calcAverageExperience())).append("\n\n");

        sb.append("Command:                        ").append(getCommanderValue()).append("\n");
        sb.append("    Leadership:           ").append(getCommanderSkill(SkillType.S_LEADER)).append("\n");
        sb.append("    Negotiation:          ").append(getCommanderSkill(SkillType.S_NEG)).append("\n");
        sb.append("    Strategy:             ").append(getCommanderSkill(SkillType.S_STRATEGY)).append("\n");
        sb.append("    Tactics:              ").append(getCommanderSkill(SkillType.S_TACTICS)).append("\n\n");

        sb.append("Combat Record:                  ").append(getCombatRecordValue()).append("\n");
        sb.append("    Successful Missions:  ").append(getSuccessCount()).append("\n");
        sb.append("    Failed Missions:      ").append(getFailCount()).append("\n");
        sb.append("    Contract Breaches:    ").append(getBreachCount()).append("\n\n");

        sb.append("Transportation:                 ").append(getTransportValue()).append("\n");
        sb.append("    Dropship Capacity:    ").append(getTransportPercent().toPlainString()).append("%\n");
        sb.append("    Jumpship?             ").append(jumpshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/out Dock?   ").append(warshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/ Dock?      ").append(warhipWithDocsOwner ? "Yes" : "No").append("\n\n");

        sb.append("Technology:                     ").append(getTechValue()).append("\n");
        sb.append("    # Clan Units:         ").append(numberClan.toPlainString()).append("\n");
        sb.append("    # IS2 Units:          ").append(numberIS2.toPlainString()).append("\n");
        sb.append("    Total # Units:        ").append(numberUnits.toPlainString()).append("\n\n");

        sb.append("Support:                        ").append(getSupportValue()).append("\n");
        sb.append("    Tech Support:         ").append(getTechSupportPercentage().toPlainString()).append("%\n");
        sb.append("    Medical Support:      ").append(getMedicalSupportPercentage().toPlainString()).append("%\n");
        sb.append("    HR Support:           ").append(getHumanResourcePercentage().toPlainString()).append("%\n\n");

        sb.append("Financial:                      ").append(getFinancialValue()).append("\n");
        sb.append("    Years in Debt:        ").append(getYearsInDebt()).append("\n");

        return new String(sb);
    }

    @Override
    public String getHelpText() {
        return "Method: FM: Mercenaries (rev)\n" +
                "An attempt to match the FM: Mercenaries (rev) method for calculating the Dragoon's rating as closely as possible.\n" +
                "Known differences include the following:\n" +
                "+ Command: Does not incorporate any positive or negative traits from AToW or BRPG3." +
                "+ Transportation: This is computed by individual unit rather than by lance/star/squadron.\n" +
                "    Auxiliary vessels are not accounted for.\n" +
                "+ Support: Artillery weapons & Naval vessels are not accounted for.";
    }
}
