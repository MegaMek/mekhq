/*
 * FieldManualMercRevDragoonsRating.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.rating;

import megamek.common.*;
import megamek.common.enums.SkillLevel;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @since 3/12/2012
 */
public class FieldManualMercRevDragoonsRating extends AbstractUnitRating {
    private BigDecimal highTechPercent = BigDecimal.ZERO;
    private BigDecimal numberIS2 = BigDecimal.ZERO;
    private BigDecimal numberClan = BigDecimal.ZERO;
    private int countIS2 = 0;
    private int countClan = 0;
    private int mechSupportNeeded = 0;
    private int tankSupportNeeded = 0;
    private int vtolSupportNeeded = 0;
    private int baSupportNeeded = 0;
    private int convFighterSupportNeeded = 0;
    private int aeroFighterSupportNeeded = 0;
    private int smallCraftSupportNeeded = 0;
    private int medSupportNeeded = 0;
    private int adminSupportNeeded = 0;
    private int dropJumpShipSupportNeeded = 0;
    private int techSupportHours = 0;
    private int medSupportHours = 0;
    private int adminSupportHours;

    public FieldManualMercRevDragoonsRating(Campaign campaign) {
        super(campaign);
    }

    @Override
    protected void initValues() {
        if (isInitialized()) {
            return;
        }

        super.initValues();
        setHighTechPercent(BigDecimal.ZERO);
        setNumberIS2(BigDecimal.ZERO);
        setNumberClan(BigDecimal.ZERO);
        setCountClan(0);
        setCountIS2(0);

        for (Unit u : getCampaign().getHangar().getUnits()) {
            if (!u.isPresent()) {
                continue;
            }

            LogManager.getLogger().debug("Processing unit " + u.getName());
            if (u.isMothballed()) {
                LogManager.getLogger().debug("Unit " + u.getName() + " is mothballed. Skipping.");
                continue;
            }

            updateUnitCounts(u);
            BigDecimal value = getUnitValue(u);
            LogManager.getLogger().debug("Unit " + u.getName() + " -- Value = " + value.toPlainString());
            setNumberUnits(getNumberUnits().add(value));

            Person p = u.getCommander();
            if (null != p) {
                LogManager.getLogger().debug("Unit " + u.getName()
                        + " -- Adding commander (" + p.getFullTitle() + "" + ") to commander list.");
                getCommanderList().add(p);
            }

            updateAdvanceTechCount(u, value);

            updateSkillLevel(u, value);

            updateBayCount(u.getEntity());

            updateJumpShips(u.getEntity());

            updateTechSupportNeeds(u);
        }

        updateAvailableSupport();
        calcMedicalSupportHoursNeeded();
        calcAdminSupportHoursNeeded();
    }

    void updateAvailableSupport() {
        for (Person p : getCampaign().getActivePersonnel()) {
            if (p.isTech()) {
                updateTechSupportHours(p);
            } else if (p.isDoctor()) {
                updateMedicalSupportHours(p);
            } else if (p.isAdministrator()) {
                updateAdministrativeSupportHours(p);
            }
        }
    }

    private void updateJumpShips(Entity en) {
        if (en instanceof Warship) {
            if (en.getDocks() > 0) {
                setWarShipWithDocsOwner(true);
            } else {
                setWarShipOwner(true);
            }
        } else if (en instanceof Jumpship) {
            setJumpShipOwner(true);
        }
    }

    private void updateTechSupportNeeds(Unit u) {
        if (u.isMothballed()) {
            return;
        }

        Entity en = u.getEntity();
        if (null == en) {
            return;
        }

        LogManager.getLogger().debug("Unit " + u.getName() + " updating tech support needs.");

        double timeMult = 1.0;
        int needed = 0;
        if (getCampaign().getCampaignOptions().isUseQuirks()) {
            if (en.hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)) {
                LogManager.getLogger().debug("Unit " + u.getName() + " is easy to maintain.");
                timeMult = 0.8;
            } else if (en.hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
                LogManager.getLogger().debug("Unit " + u.getName() + " is difficult to maintain.");
                timeMult = 1.2;
            }
        }

        if (en instanceof Mech) {
            needed += (int) Math.ceil((Math.floor(en.getWeight() / 5) + 40) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " mech tech hours.");
            mechSupportNeeded += needed;
        } else if (en instanceof Jumpship || en instanceof Dropship) {
            // according to FM:M(r), this should be tracked separately because it only applies to admin support but not
            // technical support.
            updateDropJumpShipSupportNeeds(en);
        } else if ((en instanceof SmallCraft)) {
            if (en.getWeight() >= 50000) {
                needed += (int) Math.ceil((Math.floor(en.getWeight() / 50) + 20) * timeMult);
            } else if (en.getWeight() >= 16000) {
                needed += (int) Math.ceil((Math.floor(en.getWeight() / 25) + 40) * timeMult);
            } else {
                needed += (int) Math.ceil((Math.floor(en.getWeight() / 10) + 80) * timeMult);
            }
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " small craft tech hours.");
            smallCraftSupportNeeded += needed;
        } else if (en instanceof ConvFighter) {
            needed += (int) Math.ceil((Math.floor(en.getWeight() / 2.5) + 20) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " conv. fighter tech hours.");
            convFighterSupportNeeded += needed;
        } else if (en instanceof Aero) {
            needed += (int) Math.ceil((Math.floor(en.getWeight() / 2.5) + 40) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " aero tech hours.");
            aeroFighterSupportNeeded += needed;
        } else if (en instanceof VTOL) {
            needed += (int) Math.ceil((Math.floor(en.getWeight() / 5) + 30) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " VTOL tech hours.");
            vtolSupportNeeded += needed;
        } else if (en instanceof Tank) {
            needed += (int) Math.ceil((Math.floor(en.getWeight() / 5) + 20) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " tank tech hours.");
            tankSupportNeeded += needed;
        } else if (en instanceof BattleArmor) {
            needed += (int) Math.ceil(((en.getTotalArmor() * 2) + 5) * timeMult);
            LogManager.getLogger().debug("Unit " + u.getName() + " needs " + needed + " BA tech hours.");
            baSupportNeeded += needed;
        }

        // todo Decide if this should be an additional campaign option or if this is implicit in having Faction &
        // todo Era mods turned on for the campaign in the first place.
//        if (campaign.getCampaignOptions().useFactionModifiers() && en.isClan()) {
//            hoursNeeded *= 2;
//        } else if (campaign.getCampaignOptions().useEraMods() && (en.getTechLevel() > TechConstants.T_INTRO_BOXSET)) {
//            hoursNeeded *= 1.5;
//        }
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

        if (getCampaign().getCampaignOptions().isUseQuirks()) {
            if (en.hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)) {
                hours *= 0.8;
            } else if (en.hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
                hours *= 1.2;
            }
        }
        LogManager.getLogger().debug("Unit " + en.getId() + " needs " + hours + " ship tech hours.");

        dropJumpShipSupportNeeded += (int) Math.ceil(hours);
    }

    // The wording on this in FM:Mr is rather confusing. Near as I can parse
    // it out, you divide your total personnel into 7-man "squads". These each
    // require 4 hours of medical support (3 + (7/5) = 3 + 1.4 = 4.4 rounds to
    // 4). The left over personnel form a new "squad" which requires 3 hours
    // + (# left over / 5).  So, if you have 25 personnel that would be:
    //   25 / 7 = 3 squads of 7 and 1 squad of 4.
    //   3 * (3 + 7/5) = 3 * (3 + 1.4) = 3 * 4 = 12 hours
    //   3 + (4/5) = 3 + 0.8 = 3.8 = 4 hours.
    //   total = 16 hours.
    private void calcMedicalSupportHoursNeeded() {
        int activePersonnelCount = getCampaign().getActivePersonnel().size();

        // Calculated based on 7-person squads
        int numSquads = activePersonnelCount / 7; // integer division intended
        // each squad takes 4.4 hours (3 + 7/5) rounded to the nearest hour
        medSupportNeeded = numSquads * 4;
        int leftOver = activePersonnelCount - (numSquads * 7);
        if (leftOver > 0) {
            // the left overs form a squad which needs (3 + N / 5) rounded hours
            medSupportNeeded += (int) Math.round(3 + leftOver / 5.0);
        }
    }

    private int getMedicalSupportHoursNeeded() {
        return medSupportNeeded;
    }

    private void calcAdminSupportHoursNeeded() {
        int personnelCount = (int) getCampaign().getActivePersonnel().stream()
                .filter(p -> !p.isAdministrator())
                .count();
        int totalSupport = personnelCount + getTechSupportNeeded() + dropJumpShipSupportNeeded;
        adminSupportNeeded = new BigDecimal(totalSupport)
                .divide(new BigDecimal(30), 0, RoundingMode.HALF_EVEN)
                .intValue();
    }

    private static int getSupportHours(int skillLevel) {
        switch (skillLevel) {
            case SkillType.EXP_ULTRA_GREEN:
                return 20;
            case SkillType.EXP_GREEN:
                return 30;
            case SkillType.EXP_REGULAR:
                return 40;
            case SkillType.EXP_VETERAN:
                return 45;
            default:
                return 50;
        }
    }

    private void updateTechSupportHours(Person p) {
        String[] techSkills = new String[]{SkillType.S_TECH_MECH, SkillType.S_TECH_AERO,
                SkillType.S_TECH_BA, SkillType.S_TECH_VESSEL, SkillType.S_TECH_MECHANIC};

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
        if (p.getSecondaryRole().isTechSecondary()) {
            hours = (int) Math.floor(hours / 2.0);
        }

        LogManager.getLogger().debug("Person, " + p.getFullTitle() + ", provides " + hours + " tech support hours.");
        techSupportHours += hours;
    }

    private void updateMedicalSupportHours(Person p) {
        Skill doctorSkill = p.getSkill(SkillType.S_DOCTOR);
        if (doctorSkill == null) {
            return;
        }
        int hours = getSupportHours(doctorSkill.getExperienceLevel());
        if (p.getSecondaryRole().isDoctor()) {
            hours = (int) Math.floor(hours / 2.0);
        }

        LogManager.getLogger().debug("Person, " + p.getFullTitle() + " provides " + hours + " medical support hours.");
        medSupportHours += hours;
    }

    private void updateAdministrativeSupportHours(Person p) {
        Skill adminSkill = p.getSkill(SkillType.S_ADMIN);
        if (adminSkill == null) {
            return;
        }
        int hours = getSupportHours(adminSkill.getExperienceLevel());
        if (p.getSecondaryRole().isAdministrator()) {
            hours = (int) Math.floor(hours / 2.0);
        }

        LogManager.getLogger().debug("Person, " + p.getFullTitle() + ", provides " + hours + " admin support hours.");
        adminSupportHours += hours;
    }

    private void updateSkillLevel(Unit u, BigDecimal value) {
        // Make sure this is a combat unit.
        if ((null == u.getEntity()) || u.getCrew().isEmpty()) {
            return;
        }

        LogManager.getLogger().debug("Unit " + u.getName() + " updating unit skill rating.");

        // Calculate the unit's average combat skill.
        Crew p = u.getEntity().getCrew();
        BigDecimal combatSkillAverage;

        // Infantry and ProtoMechs do not have a piloting skill.
        if ((u.getEntity() instanceof Infantry) || (u.getEntity() instanceof Protomech)) {
            combatSkillAverage = new BigDecimal(p.getGunnery());

            // All other units use an average of piloting and gunnery.
        } else {
            combatSkillAverage = BigDecimal.valueOf(p.getGunnery() + p.getPiloting())
                    .divide(BigDecimal.valueOf(2), PRECISION, HALF_EVEN);
        }

        SkillLevel experience = getExperienceLevelName(combatSkillAverage);
        LogManager.getLogger().debug("Unit " + u.getName() + " combat skill average = "
                + combatSkillAverage.toPlainString() + "(" + experience + ")");

        // Add to the running total.
        setTotalSkillLevels(getTotalSkillLevels().add(value.multiply(combatSkillAverage)));
        incrementSkillRatingCounts(experience);
    }

    @Override
    public void reInitialize() {
        mechSupportNeeded = 0;
        tankSupportNeeded = 0;
        vtolSupportNeeded = 0;
        baSupportNeeded = 0;
        convFighterSupportNeeded = 0;
        aeroFighterSupportNeeded = 0;
        smallCraftSupportNeeded = 0;
        medSupportNeeded = 0;
        adminSupportNeeded = 0;
        dropJumpShipSupportNeeded = 0;
        techSupportHours = 0;
        medSupportHours = 0;
        adminSupportHours = 0;

        super.reInitialize();
    }

    @Override
    protected int calculateUnitRatingScore() {
        initValues();

        int score = 0;

        score += getCampaign().getCampaignOptions().getManualUnitRatingModifier();
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
        if (!hasUnits()) {
            return 0;
        }
        BigDecimal averageExperience = calcAverageExperience();
        if (averageExperience.compareTo(greenThreshold) >= 0) {
            return 5;
        } else if (averageExperience.compareTo(regularThreshold) >= 0) {
            return 10;
        } else if (averageExperience.compareTo(veteranThreshold) >= 0) {
            return 20;
        } else {
            return 40;
        }
    }

    @Override
    public int getCommanderValue() {
        if (getCommander() == null) {
            return 0;
        }

        int skillTotal = getCommanderSkillLevelWithBonus(SkillType.S_LEADER);
        skillTotal += getCommanderSkillLevelWithBonus(SkillType.S_TACTICS);
        skillTotal += getCommanderSkillLevelWithBonus(SkillType.S_STRATEGY);
        skillTotal += getCommanderSkillLevelWithBonus(SkillType.S_NEG);

        /*
          todo consider adding rpg traits in MekHQ (they would have no impact
          todo on MegaMek).
          value += (total positive - total negative)
          See FM: Mercs (rev) pg 154 for a full list.
         */

        return skillTotal;
    }

    private int getMedicPoolHours() {
        return getCampaign().getNumberMedics() * 20;
    }

    int getMedicalSupportAvailable() {
        return medSupportHours + getMedicPoolHours();
    }

    private int getAstechPoolHours() {
        return getCampaign().getNumberAstechs() * 20;
    }

    int getTechSupportHours() {
        return techSupportHours + getAstechPoolHours();
    }

    private BigDecimal getMedicalSupportPercentage() {
        if (getMedicalSupportHoursNeeded() <= 0) {
            // returns 100% if there is no need for medical support
            return HUNDRED;
        } else if (getMedicalSupportAvailable() <= 0) {
            // returns 0% if there are hours needed and no support available
            return BigDecimal.ZERO;
        }

        BigDecimal percent = new BigDecimal(getMedicalSupportAvailable())
                .divide(new BigDecimal(getMedicalSupportHoursNeeded()), PRECISION, HALF_EVEN)
                .multiply(HUNDRED)
                .setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0) ? HUNDRED : percent;
    }

    private int getMedicalSupportValue() {
        BigDecimal percent = getMedicalSupportPercentage();
        BigDecimal threshold = new BigDecimal(75);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        return percent.subtract(threshold)
                .divide(new BigDecimal(5), PRECISION, HALF_EVEN)
                .setScale(0, RoundingMode.DOWN)
                .intValue() * 2;
    }

    private BigDecimal getAdminSupportPercentage() {
        if (adminSupportNeeded <= 0) {
            // returns 100% if there is no need for administrative support
            return HUNDRED;
        } else if (adminSupportHours <= 0) {
            // returns 0% if there are hours needed and no support available
            return BigDecimal.ZERO;
        }
        BigDecimal percent = new BigDecimal(adminSupportHours)
                .divide(new BigDecimal(adminSupportNeeded), PRECISION, HALF_EVEN)
                .multiply(HUNDRED)
                .setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0) ? HUNDRED : percent;
    }

    private int getAdminValue() {
        BigDecimal percent = getAdminSupportPercentage();
        BigDecimal threshold = new BigDecimal(60);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        return percent.subtract(threshold)
                .divide(new BigDecimal(10), PRECISION, HALF_EVEN)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    private int getTechSupportNeeded() {
        return mechSupportNeeded + tankSupportNeeded + vtolSupportNeeded +
               baSupportNeeded + convFighterSupportNeeded +
               aeroFighterSupportNeeded + smallCraftSupportNeeded;
    }

    private BigDecimal getTechSupportPercentage() {
        int techSupportNeeded = getTechSupportNeeded();
        if (techSupportNeeded <= 0) {
            // returns 100% if there is no need for tech support
            return HUNDRED;
        } else if (getTechSupportHours() <= 0) {
            // returns 0% if there are hours needed and no support available
            return BigDecimal.ZERO;
        }

        BigDecimal percent = BigDecimal.valueOf(getTechSupportHours())
                .divide(BigDecimal.valueOf(techSupportNeeded), PRECISION, HALF_EVEN)
                .multiply(HUNDRED)
                .setScale(0, RoundingMode.DOWN);

        return (percent.compareTo(HUNDRED) > 0) ? HUNDRED : percent;
    }

    private int getTechSupportValue() {
        BigDecimal percent = getTechSupportPercentage();
        BigDecimal threshold = new BigDecimal(60);
        if (percent.compareTo(threshold) < 0) {
            return 0;
        }

        return percent.subtract(threshold)
                .divide(new BigDecimal(10), PRECISION, HALF_EVEN)
                .setScale(0, RoundingMode.DOWN)
                .intValue() * 5;
    }

    @Override
    public int getSupportValue() {
        return hasUnits() ? getTechSupportValue() + getMedicalSupportValue() + getAdminValue() : 0;
    }

    private int getYearsInDebt() {
        int yearsInDebt = getCampaign().getFinances().getFullYearsInDebt(getCampaign().getLocalDate());
        yearsInDebt += getCampaign().getFinances().getPartialYearsInDebt(getCampaign().getLocalDate());
        return yearsInDebt;
    }

    @Override
    public int getFinancialValue() {
        int score = getYearsInDebt() * -10;
        score -= 25 * getCampaign().getFinances().getLoanDefaults();
        score -= 10 * getCampaign().getFinances().getFailedCollateral();

        return score;
    }

    private String getQualityDetails() {
        return String.format("%-" + HEADER_LENGTH + "s %3d", "Quality:", getExperienceValue())
                + '\n'
                + String.format("    %-" + SUBHEADER_LENGTH + "s %3s", "Average Skill Rating:", getAverageExperience())
                + '\n'
                + getSkillLevelCounts()
                .entrySet()
                .stream()
                .map(entry -> String.format("        #%-" + CATEGORY_LENGTH + "s %3d", entry.getKey().toString() + ':', entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String getCommandDetails() {
        StringBuilder out = new StringBuilder();
        Person commander = getCommander();
        String commanderName = (null == commander) ? "" : " (" + commander.getFullTitle() + ")";
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d %s",
                "Command:", getCommanderValue(), commanderName)).append("\n");

        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";
        out.append(String.format(TEMPLATE, "Leadership:",
                        getCommanderSkillLevelWithBonus(SkillType.S_LEADER)))
           .append("\n");
        out.append(String.format(TEMPLATE, "Negotiation:",
                        getCommanderSkillLevelWithBonus(SkillType.S_NEG)))
           .append("\n");
        out.append(String.format(TEMPLATE, "Strategy:",
                        getCommanderSkillLevelWithBonus(SkillType.S_STRATEGY)))
           .append("\n");
        out.append(String.format(TEMPLATE, "Tactics:",
                getCommanderSkillLevelWithBonus(SkillType.S_TACTICS)));

        return out.toString();
    }

    private String getCombatRecordDetails() {
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";
        return String.format("%-" + HEADER_LENGTH + "s %3d", "Combat Record:",
                getCombatRecordValue()) + "\n" +
               String.format(TEMPLATE, "Successful Missions:",
                       getSuccessCount()) + "\n" +
               String.format(TEMPLATE, "Partial Missions:",
                       getPartialCount()) + "\n" +
               String.format(TEMPLATE, "Failed Missions:",
                       getFailCount()) + "\n" +
               String.format(TEMPLATE, "Contract Breaches:", getBreachCount());
    }

    String getTransportationDetails() {
        StringBuilder out = new StringBuilder();
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3s";
        final String TEMPLATE_TWO = "        #%-" + CATEGORY_LENGTH + "s %3d needed / %3d available";

        out.append(String.format("%-" + HEADER_LENGTH + "s %3d", "Transportation", getTransportValue())).append("\n");

        int excessSuperHeavyVeeBays = Math.max(getSuperHeavyVeeBayCount() - getSuperHeavyVeeCount(), 0);
        int excessHeavyVeeBays = Math.max(getHeavyVeeBayCount() - getHeavyVeeCount(), 0);
        int excessSmallCraftBays = Math.max(getSmallCraftBayCount() - getSmallCraftCount(), 0);

        out.append(String.format(TEMPLATE, "DropShip Capacity:", getTransportPercent().toPlainString() + "%"))
        .append("\n").append(String.format(TEMPLATE_TWO, "BattleMech Bays:", getMechCount(), getMechBayCount()))
        .append("\n").append(String.format(TEMPLATE_TWO, "Fighter Bays:", getFighterCount(), getFighterBayCount()))
        .append(" (plus ").append(excessSmallCraftBays).append(" excess Small Craft)")
        .append("\n").append(String.format(TEMPLATE_TWO, "Small Craft Bays:", getSmallCraftCount(), getSmallCraftBayCount()))
        .append("\n").append(String.format(TEMPLATE_TWO, "ProtoMech Bays:", getProtoCount(), getProtoBayCount()))
        .append("\n").append(String.format(TEMPLATE_TWO, "Super Heavy Vehicle Bays:", getSuperHeavyVeeCount(), getSuperHeavyVeeBayCount()))
        .append("\n").append(String.format(TEMPLATE_TWO, "Heavy Vehicle Bays:", getHeavyVeeCount(), getHeavyVeeBayCount()))
        .append(" (plus ").append(excessSuperHeavyVeeBays).append(" excess Super Heavy)")
        .append("\n").append(String.format(TEMPLATE_TWO, "Light Vehicle Bays:", getLightVeeCount(), getLightVeeBayCount()))
        .append(" (plus ").append(excessHeavyVeeBays).append(" excess Heavy and ").append(excessSuperHeavyVeeBays).append(" excess Super Heavy)")
        .append("\n").append(String.format(TEMPLATE_TWO, "Battle Armor Bays:", getNumberBaSquads(), getBaBayCount()))
        .append("\n").append(String.format(TEMPLATE_TWO, "Infantry Bays:", calcInfantryPlatoons(), getInfantryBayCount()))
        .append("\n").append(String.format(TEMPLATE, "JumpShip?", (isJumpShipOwner() ? "Yes" : "No")))
        .append("\n").append(String.format(TEMPLATE, "WarShip w/out Collar?", (isWarShipOwner() ? "Yes" : "No")))
        .append("\n").append(String.format(TEMPLATE, "WarShip w/ Collar?", (isWarShipWithDocsOwner() ? "Yes" : "No")));

        return out.toString();
    }

    private String getTechnologyDetails() {
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d", "Technology:", getTechValue()));

        int totalUnits = getTechRatedUnits();
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";
        out.append("\n").append(String.format(TEMPLATE, "#Clan Units:", getCountClan()));
        out.append("\n").append(String.format(TEMPLATE, "#IS2 Units:", getCountIS2()));
        out.append("\n").append(String.format(TEMPLATE, "Total # Units:", totalUnits));
        return out.toString();
    }

    private String getSupportDetails() {
        final String TEMPLATE_SUB = "    %-" + SUBHEADER_LENGTH + "s %3s";
        final String TEMPLATE_CAT = "        %-" + CATEGORY_LENGTH + "s %8s";
        final String TEMPLATE_SUBCAT = "          %-" + (SUBCATEGORY_LENGTH) + "s %4s";
        return String.format("%-" + HEADER_LENGTH + "s %3d", "Support:", getSupportValue()) +
               "\n" + String.format(TEMPLATE_SUB, "Tech Support:",
                getTechSupportPercentage().toPlainString()) + "%" +
               "\n" + String.format(TEMPLATE_CAT, "Total Hours Needed:", getTechSupportNeeded()) +
               "\n" + String.format(TEMPLATE_SUBCAT, "BattleMechs:", mechSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Vehicles:", tankSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "VTOL:", vtolSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Battle Armor:", baSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Conventional Fighters:", convFighterSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Aerospace Fighters:", aeroFighterSupportNeeded) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Small Craft:", smallCraftSupportNeeded) +
               "\n" + String.format(TEMPLATE_CAT, "Available:", getTechSupportHours()) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Techs:", techSupportHours) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Astechs:", getAstechPoolHours()) +
               "\n" + String.format(TEMPLATE_SUB, "Medical Support:",
                getMedicalSupportPercentage().toPlainString()) + "%" +
               "\n" + String.format(TEMPLATE_CAT, "Total Hours Needed:", getMedicalSupportHoursNeeded()) +
               "\n" + String.format(TEMPLATE_CAT, "Available:", getMedicalSupportAvailable()) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Doctors:", medSupportHours) +
               "\n" + String.format(TEMPLATE_SUBCAT, "Medics:", getMedicPoolHours()) +
               "\n" + String.format(TEMPLATE_SUB, "HR Support:",
                getAdminSupportPercentage().toPlainString()) + "%" +
               "\n" + String.format(TEMPLATE_CAT, "Total Hours Needed:", adminSupportNeeded) +
               "\n" + String.format(TEMPLATE_CAT, "Available:", adminSupportHours);
    }

    private String getFinancialDetails() {
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3s";
        return String.format("%-" + HEADER_LENGTH + "s %3d", "Financial:", getFinancialValue()) +
               "\n" + String.format(TEMPLATE, "Years in Debt:", getYearsInDebt()) +
               "\n" + String.format(TEMPLATE, "Loan Defaults:",
                getCampaign().getFinances().getLoanDefaults()) +
               "\n" + String.format(TEMPLATE, "No Collateral Payment:",
                getCampaign().getFinances().getFailedCollateral());
    }

    @Override
    public String getDetails() {
        final boolean useManualUnitRatingModifier = getCampaign().getCampaignOptions().getManualUnitRatingModifier() != 0;
        return String.format("%-" + HEADER_LENGTH + "s %s", "Dragoons Rating:", getUnitRating()) + "\n" +
                "    Method: FM: Mercenaries (rev)\n" +
                (useManualUnitRatingModifier
                        ? ("    Manual Modifier: " + getCampaign().getCampaignOptions().getManualUnitRatingModifier() + "\n")
                        : "") + "\n" +
                getQualityDetails() + "\n\n" +
                getCommandDetails() + "\n\n" +
                getCombatRecordDetails() + "\n\n" +
                getTransportationDetails() + "\n\n" +
                getTechnologyDetails() + "\n\n" +
                getSupportDetails() + "\n\n" +
                getFinancialDetails();
    }

    @Override
    public String getHelpText() {
        return "Method: FM: Mercenaries (rev)\n" +
               "An attempt to match the FM: Mercenaries (rev) method for " +
               "calculating the Dragoon's rating as closely as possible.\n" +
               "Known differences include the following:\n" +
               "+ Command: Does not incorporate any positive or negative " +
               "traits from AToW or BRPG3." +
               "+ Transportation: This is computed by individual unit rather " +
               "than by lance/star/squadron.\n" +
               "    Auxiliary vessels are not accounted for.\n" +
               "+ Support: Artillery weapons & Naval vessels are not accounted " +
               "for.\n" +
               "Note: The Dragoons Rating, RAW, does not account for ProtoMechs " +
               "at all and Infantry only require admin & medical support, not " +
               "tech support.";
    }

    @Override
    public BigDecimal getTransportPercent() {
        // battle armor bays can hold 5 suits of battle armor per bay, while units can be 6 in some lore examples
        // so this is calculated first, and all calculations are based on this number
        int numBaBaysRequired = getBattleArmorCount() / 5;
        // Find the current number of units that might require transport
        BigDecimal totalUnits = new BigDecimal(getMechCount() +
                getProtoCount() +
                getSuperHeavyVeeCount() +
                getHeavyVeeCount() +
                getLightVeeCount() +
                getSmallCraftCount() +
                getFighterCount() +
                numBaBaysRequired +
                calcInfantryPlatoons());
        if (totalUnits.compareTo(BigDecimal.ZERO) == 0) {
            // if you have no units, you don't need to transport them, return 100%
            return HUNDRED;
        }

        // Find out the excess bays that can be filled by other types of units
        int excessSuperHeavyVeeBays = Math.max(getSuperHeavyVeeBayCount() - getSuperHeavyVeeCount(), 0); //removes any filled super heavy vehicle bays, rest can be used to store heavy or light vehicles
        int excessHeavyVeeBays = Math.max(getHeavyVeeBayCount() + excessSuperHeavyVeeBays - getHeavyVeeCount(), 0); //removes any filled heavy vehicle bays and spare super heavy vehicle bays, rest can be used to store light vehicles
        int excessSmallCraftBays = Math.max(getSmallCraftBayCount() - getSmallCraftCount(), 0); //removes any filled small craft bays, rest can be used to store fighters

        int numberWithoutTransport = Math.max((getMechCount() - getMechBayCount()), 0);
        numberWithoutTransport += Math.max(getProtoCount() - getProtoBayCount(), 0);
        numberWithoutTransport += Math.max(getSuperHeavyVeeCount() - getSuperHeavyVeeBayCount(), 0);
        numberWithoutTransport += Math.max(getHeavyVeeCount() - getHeavyVeeBayCount() - excessSuperHeavyVeeBays, 0);
        numberWithoutTransport += Math.max(getLightVeeCount() - getLightVeeBayCount() - excessHeavyVeeBays, 0);
        numberWithoutTransport += Math.max(getSmallCraftCount() - getSmallCraftBayCount(), 0);
        numberWithoutTransport += Math.max(getFighterCount() - getFighterBayCount() - excessSmallCraftBays, 0);
        numberWithoutTransport += Math.max(numBaBaysRequired - getBaBayCount(), 0);
        numberWithoutTransport += Math.max(calcInfantryPlatoons() - getInfantryBayCount(), 0);

        BigDecimal transportNeeded = new BigDecimal(numberWithoutTransport);

        if (transportNeeded.compareTo(BigDecimal.ZERO) == 0) {
            // If all units are transported, return 100%
            return HUNDRED;
        }
        BigDecimal percentUntransported = transportNeeded.divide(totalUnits, PRECISION, HALF_EVEN);
        setTransportPercent(BigDecimal.ONE.subtract(percentUntransported)
                .multiply(HUNDRED)
                .setScale(0, HALF_EVEN));

        return super.getTransportPercent();
    }

    @Override
    protected SkillLevel getExperienceLevelName(BigDecimal experience) {
        if (!hasUnits()) {
            return SkillLevel.NONE;
        } else if (experience.compareTo(greenThreshold) >= 0) {
            return SkillLevel.GREEN;
        } else if (experience.compareTo(regularThreshold) >= 0) {
            return SkillLevel.REGULAR;
        } else if (experience.compareTo(veteranThreshold) >= 0) {
            return SkillLevel.VETERAN;
        } else {
            return SkillLevel.ELITE;
        }
    }

    private int getTechRatedUnits() {
        return getMechCount() + getProtoCount() + getFighterCount() +
               getLightVeeCount() +
               getHeavyVeeCount() + getSuperHeavyVeeCount() +
               getNumberBaSquads() + getSmallCraftCount() +
               getDropShipCount() + getWarShipCount() + getJumpShipCount();
    }

    @Override
    public int getTechValue() {
        // Make sure we have units.
        if (!hasUnits()) {
            return 0;
        }

        // Number of high-tech units is equal to the number of IS2 units plus
        // twice the number of Clan units.
        BigDecimal highTechNumber = new BigDecimal(getCountIS2() + (getCountClan() * 2));

        // Conventional infantry does not count.
        int numberUnits = getTechRatedUnits();
        if (numberUnits <= 0) {
            return 0;
        }

        // Calculate the percentage of high-tech units.
        setHighTechPercent(highTechNumber.divide(new BigDecimal(numberUnits), PRECISION, HALF_EVEN));
        setHighTechPercent(getHighTechPercent().multiply(ONE_HUNDRED));

        // Cannot go above 100 percent.
        if (getHighTechPercent().compareTo(ONE_HUNDRED) > 0) {
            setHighTechPercent(ONE_HUNDRED);
        }

        // Score is calculated from percentage above 30%.
        BigDecimal scoredPercent = getHighTechPercent().subtract(
                new BigDecimal(30));

        // If we have a negative value (hi-tech percent was < 30%) return a value of zero.
        if (scoredPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        // Round down to the nearest whole percentage.
        scoredPercent = scoredPercent.setScale(0, RoundingMode.DOWN);

        // Add +5 points for every 10% remaining.
        BigDecimal oneTenth = scoredPercent.divide(new BigDecimal(10), PRECISION, HALF_EVEN);
        BigDecimal score = oneTenth.multiply(new BigDecimal(5));

        return score.intValue();
    }

    private void setHighTechPercent(BigDecimal highTechPercent) {
        this.highTechPercent = highTechPercent;
    }

    private BigDecimal getHighTechPercent() {
        return highTechPercent;
    }

    /**
     * Adds the tech level of the passed unit to the number of Clan or IS
     * Advanced units in the list (as appropriate).
     *
     * @param u     The {@code Unit} to be evaluated.
     * @param value The unit's value.  Most have a value of '1' but infantry
     *              and battle armor are less.
     */
    private void updateAdvanceTechCount(Unit u, BigDecimal value) {
        if (u.isMothballed()) {
            return;
        }
        LogManager.getLogger().debug("Unit " + u.getName() + " updating advanced tech count.");

        int unitType = u.getEntity().getUnitType();
        switch (unitType) {
            case UnitType.MEK:
            case UnitType.PROTOMEK:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.TANK:
            case UnitType.VTOL:
            case UnitType.BATTLE_ARMOR:
            case UnitType.SMALL_CRAFT:
            case UnitType.DROPSHIP:
            case UnitType.WARSHIP:
            case UnitType.JUMPSHIP:
                int techLevel = u.getEntity().getTechLevel();
                LogManager.getLogger().debug("Unit " + u.getName() + " TL = " + TechConstants.getLevelDisplayableName(techLevel));
                if (techLevel > TechConstants.T_INTRO_BOXSET) {
                    if (TechConstants.isClan(techLevel)) {
                        setNumberClan(getNumberClan().add(value));
                        if (!u.isConventionalInfantry()) {
                            setCountClan(getCountClan() + 1);
                        }
                    } else {
                        setNumberIS2(getNumberIS2().add(value));
                        if (!u.isConventionalInfantry()) {
                            setCountIS2(getCountIS2() + 1);
                        }
                    }
                }
                break;
            default:
                // not counted for tech level purposes.
                LogManager.getLogger().debug("Unit " + u.getName() + " not counted for tech level.");
                break;
        }
    }

    private void setNumberClan(BigDecimal numberClan) {
        this.numberClan = numberClan;
    }

    private BigDecimal getNumberClan() {
        return numberClan;
    }

    private void setCountClan(int countClan) {
        this.countClan = countClan;
    }

    private void setNumberIS2(BigDecimal numberIS2) {
        this.numberIS2 = numberIS2;
    }

    private BigDecimal getNumberIS2() {
        return numberIS2;
    }

    private void setCountIS2(int countIS2) {
        this.countIS2 = countIS2;
    }

    private int getCountIS2() {
        return countIS2;
    }

    private int getCountClan() {
        return countClan;
    }

    @Override
    public UnitRatingMethod getUnitRatingMethod() {
        return UnitRatingMethod.FLD_MAN_MERCS_REV;
    }
}
