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

    protected boolean initialized = false;
    protected List<Person> commanderList = new ArrayList<Person>();
    protected BigDecimal numberUnits = BigDecimal.ZERO;
    protected BigDecimal numberIS2 = BigDecimal.ZERO;
    protected BigDecimal numberClan = BigDecimal.ZERO;
    protected BigDecimal totalSkillLevels = BigDecimal.ZERO;
    protected int numberMech = 0;
    protected int numberAero = 0;
    protected int numberVee = 0;
    protected int numberBa = 0;
    protected int numberInf = 0;
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

    /**
     * Default constructor.
     *
     * @param campaign The MekHQ {@code Campaign}
     */
    public AbstractDragoonsRating(Campaign campaign) {
        this.campaign = campaign;
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
        return getTotalSkillLevels().divide(numberUnits, PRECISION, HALF_EVEN);
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
        if ((commander == null) || !initialized) {

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
     * Returns the value of the initialized flag.  This flag should get set by the initData method.
     * @return
     */
    public boolean isInitialized() {
        return initialized;
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
            } else {
                numberIS2 = numberIS2.add(value);
            }
        }
    }

    @Override
    public int getTechValue() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        highTechPercent = numberIS2.add(numberClan.multiply(new BigDecimal(2))).divide(getNumberUnits(), PRECISION, HALF_EVEN);
        highTechPercent = highTechPercent.multiply(new BigDecimal(100));

        BigDecimal scoredPercent = highTechPercent.subtract(new BigDecimal(30));
        if (scoredPercent.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }

        return scoredPercent.multiply(new BigDecimal(2)).setScale(0, RoundingMode.HALF_UP).intValue();
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
        value += scoredPercent.multiply(new BigDecimal(2)).setScale(0, RoundingMode.DOWN).intValue();
        return Math.max(value, 25);
    }

    @Override
    public BigDecimal getTransportPercent() {
        if (!initialized) {

            //Find out how short of transport bays we are.
            int numberWithoutTransport = Math.max((numberMech - numberMechBays), 0);
            numberWithoutTransport += Math.max((numberVee - numberVeeBays), 0);
            numberWithoutTransport += Math.max((numberAero - numberAero), 0);
            numberWithoutTransport += Math.max((numberBa - numberBaBays), 0);
            numberWithoutTransport += Math.max(((numberInf/28) - numberInfBays), 0);
            BigDecimal transportNeeded = new BigDecimal(numberWithoutTransport);

            //Find the percentage of units that are transported.
            if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            transportPercent = BigDecimal.ONE.subtract(transportNeeded.divide(getNumberUnits(), PRECISION, HALF_EVEN)).multiply(new BigDecimal(100));
        }

        return transportPercent;
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
    public String getDragoonRating(boolean recalculate) {
        int score = calculateDragoonRatingScore(recalculate);
        return getDragoonRatingName(getDragoonRating(score)) + " (" + score + ")";
    }

    /**
     * Calculates the weighted value of the unit based on if it is Infantry, Battle Armor or something else.
     * @param u The {@code Unit} to be evaluated.
     * @return
     */
    protected BigDecimal getUnitValue(Unit u) {
        BigDecimal value = BigDecimal.ONE;
        if ((u.getEntity() instanceof Infantry)
                && !(u.getEntity() instanceof BattleArmor)
                && (((Infantry)u.getEntity()).getSquadN() == 1)) {
            value = new BigDecimal("0.25");
        }
        return value;
    }

    /**
     * Returns the sum of all experience ratings for all combat units.
     *
     * @return
     */
    protected BigDecimal getTotalSkillLevels() {
        initValues(false);
        return totalSkillLevels;
    }

    /**
     * Returns the total number of combat units.
     *
     * @return
     */
    protected BigDecimal getNumberUnits() {
        initValues(false);
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
     *
     * @param recalculate
     * @return
     */
    protected abstract int calculateDragoonRatingScore(boolean recalculate);

    /**
     * Recalculates the dragoons rating.  If this has already been done, the initialized flag should already
     * be set true and this method will immediately exit.
     *
     * @param reInitialize Pass a value of TRUE to force a recalculation regardless of the value of the initialied flag.
     */
    protected abstract void initValues(boolean reInitialize);

}
