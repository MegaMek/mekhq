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
package mekhq.campaign.rating;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.ConvFighter;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.SmallCraftBay;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.Warship;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/12/2012
 */
public class FieldManualMercRevDragoonsRating extends AbstractUnitRating {


    private int techSupportNeeded = 0;
    private int medSupportNeeded = 0;
    private int adminSupportNeeded = 0;
    private int dropJumpShipSupportNeeded = 0;
    private int techSupportAvailable = 0;
    private int medSupportAvailable = 0;
    private int adminSupportAvailable;

    public FieldManualMercRevDragoonsRating(Campaign campaign) {
        super(campaign);
    }

    @Override
    protected void initValues() {
        if (initialized) {
            return;
        }

        super.initValues();
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

            updateTechSupportNeeds(u);
        }

        updateAvailableSupport();
        calcMedicalSupportHoursNeeded();
        calcAdminSupportHoursNeeded();
    }

    protected void updateAvailableSupport() {
        for (Person p : campaign.getPersonnel()) {
            if (!p.isActive()) {
                continue;
            }
            if (p.isTech()) {
                updateTechSupportAvailable(p);
            } else if (p.isDoctor()) {
                updateMedicalSupportAvailable(p);
            } else if (p.isAdmin()) {
                updateAdministrativeSupportAvailable(p);
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

    private void updateUnitCounts(Entity en) {
        if (en instanceof Mech) {
            mechCount++;
        } else if (en instanceof Tank) {
            lightVeeCount++;
        } else if ((en instanceof Aero) && !(en instanceof Dropship) && !(en instanceof Jumpship)) {
            fighterCount++;
        } else if (en instanceof BattleArmor) {
            battleArmorCount += ((Infantry) en).getSquadSize();
            numberBaSquads++;
        } else if (en instanceof Infantry) {
            infantryCount += ((Infantry) en).getSquadN() * ((Infantry) en).getSquadSize();
            numberInfSquads++;
        } else {
            numberOther++;
        }
    }

    /**
     * todo Figure out how to incorporate Naval vessel's & artillery.
     */
    private void updateTechSupportNeeds(Unit u) {
        Entity en = u.getEntity();
        double hoursNeeded = 0;
        if (en instanceof Mech) {
            hoursNeeded = Math.floor(en.getWeight() / 5) + 40;
        } else if (en instanceof Warship ||
                   en instanceof Jumpship ||
                   en instanceof Dropship) {
            // according to FMMR, this should be tracked separately because it only applies to admin support but not
            // technical support.
            updateDropJumpShipSupportNeeds(en);
            return;
        } else if ((en instanceof SmallCraft)) {
            if (en.getWeight() >= 50000) {
                hoursNeeded = Math.floor(en.getWeight() / 50) + 20;
            } else if (en.getWeight() >= 16000) {
                hoursNeeded = Math.floor(en.getWeight() / 25) + 40;
            } else {
                hoursNeeded = Math.floor(en.getWeight() / 10) + 80;
            }
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
        } else if (en instanceof Infantry) {
            //according to FMMR, they provide their own support so skip
            return;
        }

        if (campaign.getCampaignOptions().useQuirks()) {
            if (en.hasQuirk("easy_maintain")) {
                hoursNeeded -= hoursNeeded * 0.2;
            } else if (en.hasQuirk("difficult_maintain")) {
                hoursNeeded += hoursNeeded * 0.2;
            }
        }


        // todo Descide if this should be an additional campaign option or if this is implicit in having Faction &
        // todo Era mods turned on for the campiagn in the first place.
//        if (campaign.getCampaignOptions().useFactionModifiers() && en.isClan()) {
//            hoursNeeded *= 2;
//        } else if (campaign.getCampaignOptions().useEraMods() && (en.getTechLevel() > TechConstants.T_INTRO_BOXSET)) {
//            hoursNeeded *= 1.5;
//        }

        techSupportNeeded += hoursNeeded;
    }

    private void updateDropJumpShipSupportNeeds(Entity en) {
        double hours = 0;
        if (en instanceof Warship) {
            hours += 5000;
        } else if (en instanceof Jumpship) {
            hours += 800;
        } else if (en instanceof Dropship) {
            if (en.getWeight() >= 50000) {
                hours = Math.floor(en.getWeight() / 50) + 20;
            } else if (en.getWeight() >= 16000) {
                hours = Math.floor(en.getWeight() / 25) + 40;
            } else {
                hours = Math.floor(en.getWeight() / 10) + 80;
            }
        }

        if (campaign.getCampaignOptions().useQuirks()) {
            if (en.hasQuirk("easy_maintain")) {
                hours -= hours * 0.2;
            } else if (en.hasQuirk("difficult_maintain")) {
                hours += hours * 0.2;
            }
        }

//        if (campaign.getCampaignOptions().useFactionModifiers() && en.isClan()) {
//            hours *= 2;
//        } else if (campaign.getCampaignOptions().useEraMods() && (en.getTechLevel() > TechConstants.T_INTRO_BOXSET)) {
//            hours *= 1.5;
//        }

        dropJumpShipSupportNeeded += hours;
    }

    // The wording on this in FM:Mr is rather confusing.  Near as I can parse it out, you divide your total personnel
    // into 7-man "squads".  These each require 4 hours of medical support (3 + (7/5) = 3 + 1.4 = 4.4 rounds to 4).
    // The left over personnel form a new "squad" which requires 3 hours + (# left over / 5).  So, if you have 25
    // personnel that would be:
    //   25 / 7 = 3 squads of 7 and 1 squad of 4.
    //   3 * (3 + 7/5) = 3 * (3 + 1.4) = 3 * 4 = 12 hours
    //   3 + (4/5) = 3 + 0.8 = 3.8 = 4 hours.
    //   total = 16 hours.
    private void calcMedicalSupportHoursNeeded() {
        int numSquads = new BigDecimal(campaign.getPersonnel().size())
                .divide(new BigDecimal(7), 0, RoundingMode.DOWN).intValue();
        int leftOver = campaign.getPersonnel().size() - (numSquads * 7);

        medSupportNeeded = (numSquads * 4) +
                           (3 + (new BigDecimal(leftOver).divide(new BigDecimal(5), 0, RoundingMode.HALF_EVEN).intValue()));
    }

    protected int getMedicalSupportHoursNeeded() {
        return medSupportNeeded;
    }

    private void calcAdminSupportHoursNeeded() {
        int personnelCount = 0;
        for (Person p : campaign.getPersonnel()) {
            if ((p.getPrimaryRole() == Person.T_ADMIN_TRA) ||
                (p.getPrimaryRole() == Person.T_ADMIN_COM) ||
                (p.getPrimaryRole() == Person.T_ADMIN_LOG) ||
                (p.getPrimaryRole() == Person.T_ADMIN_HR) ||
                (p.getSecondaryRole() == Person.T_ADMIN_HR) ||
                (p.getSecondaryRole() == Person.T_ADMIN_TRA) ||
                (p.getSecondaryRole() == Person.T_ADMIN_COM) ||
                (p.getSecondaryRole() == Person.T_ADMIN_LOG)) {
                continue;
            }
            personnelCount++;
        }
        int totalSupport = personnelCount + techSupportNeeded + dropJumpShipSupportNeeded;
        adminSupportNeeded = new BigDecimal(totalSupport).divide(new BigDecimal(30), 0,
                                                                 RoundingMode.HALF_EVEN).intValue();
    }

    private static int getSupportHours(int skillLevel) {
        switch (skillLevel) {
            case (SkillType.EXP_ULTRA_GREEN):
                return 20;
            case (SkillType.EXP_GREEN):
                return 30;
            case (SkillType.EXP_REGULAR):
                return 40;
            case (SkillType.EXP_VETERAN):
                return 45;
            default:
                return 50;
        }
    }

    private void updateTechSupportAvailable(Person p) {
        String[] techSkills = new String[]{SkillType.S_TECH_MECH,
                                           SkillType.S_TECH_AERO,
                                           SkillType.S_TECH_BA,
                                           SkillType.S_TECH_MECHANIC};

        // Get the highest tech skill this person has.
        int highestSkill = SkillType.EXP_ULTRA_GREEN;
        for (String s : techSkills) {
            if (p.hasSkill(s)) {
                int rank = p.getSkill(s).getExperienceLevel();
                if (rank > highestSkill) {
                    highestSkill = rank;
                }
            }
        }

        // Get the number of support hours this person contributes.
        int hours = getSupportHours(highestSkill);
        if (p.isTechSecondary()) {
            hours = (int) Math.floor(hours / 2D);
        }
        techSupportAvailable += hours;
    }

    private void updateMedicalSupportAvailable(Person p) {
        int hours = getSupportHours(p.getSkill(SkillType.S_DOCTOR).getExperienceLevel());
        if (p.getSecondaryRole() == Person.T_DOCTOR) {
            hours = (int) Math.floor(hours / 2D);
        }
        medSupportAvailable += hours;
    }

    private void updateAdministrativeSupportAvailable(Person p) {
        int hours = getSupportHours(p.getSkill(SkillType.S_ADMIN).getExperienceLevel());
        if (p.isAdminSecondary()) {
            hours = (int) Math.floor(hours / 2D);
        }
        adminSupportAvailable += hours;
    }

    private void updateSkillLevel(Unit u, BigDecimal value) {

        //Make sure this is a combat unit.
        if ((null == u.getEntity()) || (null == u.getEntity().getCrew())) {
            return;
        }

        //Calculate the unit's average combat skill.
        Crew p = u.getEntity().getCrew();
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
    protected int calculateUnitRatingScore() {
        initValues();

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

    public int getCommanderValue() {
        if (getCommander() == null) {
            return 0;
        }

        int value = 0;

        Skill test = getCommander().getSkill(SkillType.S_LEADER);
        if (test != null) {
            value += test.getLevel();
        }

        test = getCommander().getSkill(SkillType.S_NEG);
        if (test != null) {
            value += test.getLevel();
        }

        test = getCommander().getSkill(SkillType.S_STRATEGY);
        if (test != null) {
            value += test.getLevel();
        }

        test = getCommander().getSkill(SkillType.S_TACTICS);
        if (test != null) {
            value += test.getLevel();
        }

        /**
         * todo consider adding rpg traits in MekHQ (they would have no impact on megamek).
         * value += (total positive - total negative)
         * See FM: Mercs (rev) pg 154 for a full list.
         */

        return value;
    }

    public int getMedicalSupportAvailable() {
        int medicPoolMinutes = campaign.getNumberMedics() * 20;
        return medSupportAvailable + medicPoolMinutes;
    }

    public int getTechSupportAvailable() {
        int astechPoolMinutes = campaign.getNumberAstechs() * 20;
        return techSupportAvailable + astechPoolMinutes;
    }

    protected BigDecimal getMedicalSupportPercentage() {
        if (getMedicalSupportAvailable() <= 0) {
            return BigDecimal.ZERO;
        }
        if (getMedicalSupportHoursNeeded() <= 0) {
            return HUNDRED;
        }

        BigDecimal percent = new BigDecimal(getMedicalSupportAvailable())
                .divide(new BigDecimal(getMedicalSupportHoursNeeded()), PRECISION, HALF_EVEN)
                .multiply(HUNDRED).setScale(0, RoundingMode.DOWN);

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

    private BigDecimal getAdminSupportPercentage() {
        if (adminSupportAvailable <= 0) {
            return BigDecimal.ZERO;
        }
        if (adminSupportNeeded <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = new BigDecimal(adminSupportAvailable).divide(new BigDecimal(adminSupportNeeded), PRECISION, HALF_EVEN).multiply(
                HUNDRED).setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0 ? HUNDRED : percent);
    }

    private int getAdminValue() {
        BigDecimal percent = getAdminSupportPercentage();
        BigDecimal threshold = new BigDecimal(60);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        percent = percent.subtract(threshold).divide(new BigDecimal(10), PRECISION, HALF_EVEN);
        return percent.setScale(0, RoundingMode.DOWN).intValue();
    }

    private BigDecimal getTechSupportPercentage() {
        if (getTechSupportAvailable() <= 0) {
            return BigDecimal.ZERO;
        }
        if (techSupportNeeded <= 0) {
            return HUNDRED;
        }

        BigDecimal percent = new BigDecimal(getTechSupportAvailable())
                .divide(new BigDecimal(techSupportNeeded), PRECISION, HALF_EVEN)
                .multiply(HUNDRED).setScale(0, RoundingMode.DOWN);

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

    public int getSupportValue() {
        return getTechSupportValue() + getMedicalSupportValue() + getAdminValue();
    }

    /*
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
        Comparator<Transaction> transactionDateCompare = new Comparator<Transaction>() {
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
            if (!"Carryover from previous year".equalsIgnoreCase(t.getDescription())) {
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
    */

    public int getFinancialValue() {
        int score = campaign.getFinances().getFullYearsInDebt(campaign.getCalendar()) * -10;
        score -= 25 * campaign.getFinances().getLoanDefaults();
        score -= 10 * campaign.getFinances().getFailedCollateral();

        return score;
    }

    public String getDetails() {
        StringBuffer sb = new StringBuffer("Dragoons Rating:                " + getUnitRating() + "\n");
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
        sb.append("    Dropship Capacity:    ").append(getTransportPercent().toPlainString()).append("\n");
        sb.append("    Jumpship?             ").append(jumpshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/out Dock?   ").append(warshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/ Dock?      ").append(warhipWithDocsOwner ? "Yes" : "No").append("\n\n");

        sb.append("Technology:                     ").append(getTechValue()).append("\n");
        sb.append("    # Clan Units:         ").append(countClan).append("\n");
        sb.append("    # IS2 Units:          ").append(countIS2).append("\n");
        sb.append("    Total # Units:        ")
          .append(fighterCount + numberBaSquads + mechCount + lightVeeCount + numberOther).append("\n\n");

        sb.append("Support:                        ").append(getSupportValue()).append("\n");
        sb.append("    Tech Support:         ").append(getTechSupportPercentage().toPlainString()).append("%\n");
        sb.append("    Medical Support:      ").append(getMedicalSupportPercentage().toPlainString()).append("%\n");
        sb.append("    HR Support:           ").append(getAdminSupportPercentage().toPlainString()).append("%\n\n");

        sb.append("Financial:                      ").append(getFinancialValue()).append("\n");
        sb.append("    Years in Debt:        ").append(campaign.getFinances().getFullYearsInDebt(campaign.getCalendar())).append("\n");
        sb.append("    Loan Defaults:        ").append(campaign.getFinances().getLoanDefaults()).append("\n");
        sb.append("    No Collateral Payment:").append(campaign.getFinances().getFailedCollateral()).append("\n\n");

        return new String(sb);
    }

    public String getHelpText() {
        return "Method: FM: Mercenaries (rev)\n" +
               "An attempt to match the FM: Mercenaries (rev) method for calculating the Dragoon's rating as closely as possible.\n" +
               "Known differences include the following:\n" +
               "+ Command: Does not incorporate any positive or negative traits from AToW or BRPG3." +
               "+ Transportation: This is computed by individual unit rather than by lance/star/squadron.\n" +
               "    Auxiliary vessels are not accounted for.\n" +
               "+ Support: Artillery weapons & Naval vessels are not accounted for.";
    }

    public BigDecimal getTransportPercent() {
        // todo Superheavys.
        //Find out how short of transport bays we are.
        int numberWithoutTransport = Math.max((mechCount - mechBayCount), 0);
        numberWithoutTransport += Math.max(protoCount - protoBayCount, 0);
        numberWithoutTransport += Math.max((lightVeeCount - lightVeeBayCount), 0);
        numberWithoutTransport += Math.max(heavyVeeCount - heavyVeeBayCount, 0);
        numberWithoutTransport += Math.max((fighterCount - fighterCount), 0);
        numberWithoutTransport += Math.max((numberBaSquads - baBayCount), 0);
        numberWithoutTransport += Math.max((numberInfSquads - infantryBayCount), 0);
        BigDecimal transportNeeded = new BigDecimal(numberWithoutTransport);

        //Find the percentage of units that are transported.
        BigDecimal totalUnits = new BigDecimal(mechCount + lightVeeCount + heavyVeeCount + fighterCount +
                                               numberBaSquads + numberInfSquads);
        if (totalUnits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percentUntransported = transportNeeded.divide(totalUnits, PRECISION, HALF_EVEN);
        transportPercent = BigDecimal.ONE.subtract(percentUntransported).multiply(HUNDRED).setScale(0, HALF_EVEN);

        return transportPercent;
    }
}
