/*
 * AbstractMrbcRating.java
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

import megamek.common.BattleArmor;
import megamek.common.Infantry;
import megamek.common.TechConstants;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %I% %G%
 * @since 3/15/2012
 */
public abstract class AbstractDragoonsRating implements IDragoonsRating {

    protected Campaign campaign = null;

    protected static final BigDecimal greenThreshold = new BigDecimal("5.5");
    protected static final BigDecimal regularThreshold = new BigDecimal("4.0");
    protected static final BigDecimal veteranThreshold = new BigDecimal("2.5");

    protected List<Person> commanderList = new ArrayList<Person>();
    protected BigDecimal numberUnits = BigDecimal.ZERO;
    protected BigDecimal numberIS2 = BigDecimal.ZERO;
    protected BigDecimal numberClan = BigDecimal.ZERO;
    protected BigDecimal totalSkillLevels = BigDecimal.ZERO;
    protected int numberMech = 0;
    protected int numberAero = 0;
    protected int numberVee = 0;
    protected int numberBa = 0;
    protected int numberBaSquads = 0;
    protected int numberSoldiers = 0;
    protected int numberInfSquads = 0;
    protected int countIS2 = 0;
    protected int countClan = 0;
    protected BigDecimal mechTech = BigDecimal.ZERO;
    protected BigDecimal aeroTech = BigDecimal.ZERO;
    protected BigDecimal veeTech = BigDecimal.ZERO;
    protected BigDecimal baTech = BigDecimal.ZERO;
    protected int numberMechBays = 0;
    protected int numberAeroBays = 0;
    protected int numberVeeBays = 0;
    protected int numberBaBays = 0;
    protected int numberInfBays = 0;
    protected boolean warhipWithDocsOwner = false;
    protected boolean warshipOwner = false;
    protected boolean jumpshipOwner = false;
    protected Person commander = null;
    protected int breachCount = 0;
    protected int successCount = 0;
    protected int failCount = 0;
    protected BigDecimal supportPercent = BigDecimal.ZERO;
    protected BigDecimal transportPercent = BigDecimal.ZERO;
    protected BigDecimal highTechPercent = BigDecimal.ZERO;

    protected static boolean initialized = false;

    /**
     * Default constructor.
     *
     * @param campaign The MekHQ {@code Campaign}
     */
    public AbstractDragoonsRating(Campaign campaign) {
        this.campaign = campaign;
        initialized = false;
    }

    public void reInitialize() {
        initialized = false;
        initValues();
        initialized = true;
    }

    @Override
    public String getAverageExperience() {
        return SkillType.getExperienceLevelName(calcAverageExperience().setScale(0, RoundingMode.HALF_UP).intValue());
    }

    @Override
    public int getCombatRecordValue() {
        successCount = 0;
        failCount = 0;
        breachCount = 0;
        for (Mission m : campaign.getMissions()) {

            //Skip ongoing missions.
            if (m.isActive()) {
                continue;
            }

            if (m.getStatus() == Mission.S_SUCCESS) {
                successCount++;
            } else if (m.getStatus() == Mission.S_FAILED) {
                failCount++;
            } else if (m.getStatus() == Mission.S_BREACH) {
                breachCount++;
            }
        }

        return (successCount * 5) - (failCount * 10) - (breachCount * 25);
    }

    /**
     * Returns the average experience level for all combat personnel.
     *
     * @return
     */
    protected BigDecimal calcAverageExperience() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) > 0) {
            return getTotalSkillLevels().divide(getNumberUnits(), PRECISION, HALF_EVEN);
        }

		return BigDecimal.ZERO;
    }

    /**
     * Returns the number of breached contracts.
     *
     * @return
     */
    public int getBreachCount() {
        return breachCount;
    }

    /**
     * Returns the commander (highest ranking person) for this force.
     *
     * @return
     */
    public Person getCommander() {
        if ((commander == null)) {

            //If the list is null, we cannot determine a commander.
            if (commanderList == null || commanderList.isEmpty()) {
                commander = null;
                return null;
            }


            //Sort the list of personnel by rank.  Whoever has the highest rank is the commander.
            Collections.sort(commanderList, new Comparator<Person>() {
                @Override
                public int compare(Person p1, Person p2) {
                    return ((Comparable<Integer>) p2.getRank()).compareTo(p1.getRank());
                }
            });
            commander = commanderList.get(0);
        }

        return commander;
    }
    
    public int getCommanderSkill(String skillName) {
        Skill skill = getCommander().getSkill(skillName);
        if (skill == null) {
            return 0;
        }
        return skill.getLevel();
    }

    /**
     * Returns the number of failed contracts.
     *
     * @return
     */
    public int getFailCount() {
        return failCount;
    }

    /**
     * Adds the tech level of the passed unit to the number of Clan or IS Advanced units in the list (as appropriate).
     *
     * @param u The {@code Unit} to be evaluated.
     * @param value The unit's value.  Most have a value of '1' but infantry and battle armor are less.
     */
    protected void updateAdvanceTechCount(Unit u, BigDecimal value) {
        int techLevel = u.getEntity().getTechLevel();
        if (techLevel > TechConstants.T_INTRO_BOXSET) {
            if (TechConstants.isClan(techLevel)) {
                numberClan = numberClan.add(value);
                if (!isConventionalInfanry(u)) {
                    countClan++;
                }
            } else {
                numberIS2 = numberIS2.add(value);
                if (!isConventionalInfanry(u)) {
                    countIS2++;
                }
            }
        }
    }

    @Override
    public int getTechValue() {

        //Make sure we have units.
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        //Number of high-tech units is equal to the number of IS2 units plus twice the number of Clan units.
        BigDecimal highTechNumber = new BigDecimal(countIS2 + (countClan * 2));

        //Conventional infantry does not count.
        int numberUnits = numberAero + numberBaSquads + numberMech + numberVee;
        if (numberUnits <= 0) {
            return 0;
        }

        //Calculate the percentage of high-tech units.
        highTechPercent = highTechNumber.divide(new BigDecimal(numberUnits), PRECISION, HALF_EVEN);
        highTechPercent = highTechPercent.multiply(ONE_HUNDRED);

        //Cannot go above 100 percent.
        if (highTechPercent.compareTo(ONE_HUNDRED) > 0) {
            highTechPercent = ONE_HUNDRED;
        }

        //Score is calculated from percentage above 30%.
        BigDecimal scoredPercent = highTechPercent.subtract(new BigDecimal(30));

        //If we have a negative value (hi-tech percent was < 30%) return a value of zero.
        if (scoredPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        //Round down to the nearest whole percentage.
        scoredPercent = scoredPercent.setScale(0, RoundingMode.DOWN);

        //Add +5 points for every 10% remaining.
        BigDecimal oneTenth = scoredPercent.divide(new BigDecimal(10), PRECISION, HALF_EVEN);
        BigDecimal score = oneTenth.multiply(new BigDecimal(5));

        return score.intValue();
    }

    /**
     * Returns the number of successfully completed contracts.
     * @return
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * Returns the overall percentage of fully supported units.
     *
     * @return
     */
    public BigDecimal getSupportPercent() {
        return supportPercent;
    }

    @Override
    public int getTransportValue() {
        int value = 0;

        //Only the highest of these values should be used, regardless of how many are actually owned.
        if (warhipWithDocsOwner) {
            value += 30;
        } else if (warshipOwner) {
            value += 20;
        } else if (jumpshipOwner) {
            value += 10;
        }

        //Find the percentage of units that are transported.
        transportPercent = getTransportPercent();

        //Compute the score.
        BigDecimal scoredPercent = transportPercent.subtract(new BigDecimal(50));
        if (scoredPercent.compareTo(BigDecimal.ZERO) < 0) {
            return value;
        }
        value += scoredPercent.multiply(new BigDecimal(5)).setScale(0, RoundingMode.DOWN).intValue();
        return Math.min(value, 25);
    }

    @Override
    public int getDragoonRating(int score) {
        if(score < 0) {
            return DRAGOON_F;
        }
        else if(score < 46) {
            return DRAGOON_D;
        }
        else if(score < 86) {
            return DRAGOON_C;
        }
        else if(score < 121) {
            return DRAGOON_B;
        }
        else if(score < 151) {
            return DRAGOON_A;
        }
        else {
            return DRAGOON_ASTAR;
        }
    }

    @Override
    public String getDragoonRatingName(int rating) {
        switch(rating) {
        case DRAGOON_F:
            return "F";
        case DRAGOON_D:
            return "D";
        case DRAGOON_C:
            return "C";
        case DRAGOON_B:
            return "B";
        case DRAGOON_A:
            return "A";
        case DRAGOON_ASTAR:
            return "A*";
        default:
            return "Unrated";
        }
    }

    @Override
    public String getDragoonRating() {
        int score = calculateDragoonRatingScore();
        return getDragoonRatingName(getDragoonRating(score)) + " (" + score + ")";
    }

    /**
     * Calculates the weighted value of the unit based on if it is Infantry, Battle Armor or something else.
     * @param u The {@code Unit} to be evaluated.
     * @return
     */
    protected BigDecimal getUnitValue(Unit u) {
        BigDecimal value = BigDecimal.ONE;
        if (isConventionalInfanry(u) && (((Infantry)u.getEntity()).getSquadN() == 1)) {
            value = new BigDecimal("0.25");
        }
        return value;
    }

    protected boolean isConventionalInfanry(Unit u) {
        return (u.getEntity() instanceof Infantry) && !(u.getEntity() instanceof BattleArmor);
    }

    /**
     * Returns the sum of all experience ratings for all combat units.
     *
     * @return
     */
    protected BigDecimal getTotalSkillLevels() {
        initValues();
        return totalSkillLevels;
    }

    /**
     * Returns the total number of combat units.
     *
     * @return
     */
    protected BigDecimal getNumberUnits() {
        if (numberUnits.compareTo(BigDecimal.ZERO) <= 0) {
            reInitialize();
        }
        return numberUnits;
    }

    protected String getExperienceLevelName(BigDecimal experience) {
        if (experience.compareTo(greenThreshold) >= 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_GREEN);
        }
        if (experience.compareTo(regularThreshold) >= 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_REGULAR);
        }
        if (experience.compareTo(veteranThreshold) >= 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_VETERAN);
        }

        return SkillType.getExperienceLevelName(SkillType.EXP_ELITE);
    }

    /**
     * Calculates the unit's Dragoon's rating.  If recalculate is TRUE, then the calculations will start over
     * from the beginning.  If FALSE, pre-calculated score will be returned.
     */
    protected abstract int calculateDragoonRatingScore();

    /**
     * Recalculates the dragoons rating.  If this has already been done, the initialized flag should already
     * be set true and this method will immediately exit.
     */
    protected void initValues() {
        commanderList = new ArrayList<Person>();
        numberUnits = BigDecimal.ZERO;
        numberIS2 = BigDecimal.ZERO;
        numberClan = BigDecimal.ZERO;
        totalSkillLevels = BigDecimal.ZERO;
        numberMech = 0;
        numberAero = 0;
        numberVee = 0;
        numberBa = 0;
        numberSoldiers = 0;
        numberInfSquads = 0;
        countClan = 0;
        countIS2 = 0;
        mechTech = BigDecimal.ZERO;
        aeroTech = BigDecimal.ZERO;
        veeTech = BigDecimal.ZERO;
        baTech = BigDecimal.ZERO;
        numberMechBays = 0;
        numberAeroBays = 0;
        numberBaBays = 0;
        numberVeeBays = 0;
        numberInfBays = 0;
        warhipWithDocsOwner = false;
        warshipOwner = false;
        jumpshipOwner = false;
        commander = null;
        breachCount = 0;
        successCount = 0;
        failCount = 0;
        supportPercent = BigDecimal.ZERO;
        transportPercent = BigDecimal.ZERO;
        highTechPercent = BigDecimal.ZERO;
    }

}
