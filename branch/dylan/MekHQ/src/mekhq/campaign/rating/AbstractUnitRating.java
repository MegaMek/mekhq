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
package mekhq.campaign.rating;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import megamek.common.ASFBay;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.Dropship;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.MechBay;
import megamek.common.SmallCraftBay;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/15/2012
 */
public abstract class AbstractUnitRating implements IUnitRating {

    protected static final BigDecimal HUNDRED = new BigDecimal(100);

    protected Campaign campaign = null;

    protected static final BigDecimal greenThreshold = new BigDecimal("5.5");
    protected static final BigDecimal regularThreshold = new BigDecimal("4.0");
    protected static final BigDecimal veteranThreshold = new BigDecimal("2.5");

    protected List<Person> commanderList = new ArrayList<Person>();
    protected BigDecimal numberUnits = BigDecimal.ZERO;
    protected BigDecimal numberIS2 = BigDecimal.ZERO;
    protected BigDecimal numberClan = BigDecimal.ZERO;
    protected BigDecimal totalSkillLevels = BigDecimal.ZERO;
    protected int mechCount = 0;
    protected int superHeavyMechCount = 0;
    protected int protoCount = 0;
    protected int fighterCount = 0;
    protected int lightVeeCount = 0;
    protected int heavyVeeCount = 0;
    protected int superHeavyVeeCount = 0;
    protected int battleArmorCount = 0;
    protected int numberBaSquads = 0;
    protected int infantryCount = 0;
    protected int numberInfSquads = 0;
    protected int dropshipCount = 0;
    protected int warshipCount = 0;
    protected int jumpshipCount = 0;
    protected int numberOther = 0; // Dropships, Jumpships, Warships, etc.
    protected int countIS2 = 0;
    protected int countClan = 0;
    protected BigDecimal mechTechCount = BigDecimal.ZERO;
    protected BigDecimal aeroTech = BigDecimal.ZERO;
    protected BigDecimal veeTech = BigDecimal.ZERO;
    protected BigDecimal baTech = BigDecimal.ZERO;
    protected int mechBayCount = 0;
    protected int superHeavyMechBayCount = 0;
    protected int protoBayCount = 0;
    protected int fighterBayCount = 0;
    protected int smallCraftBayCount = 0;
    protected int lightVeeBayCount = 0;
    protected int heavyVeeBayCount = 0;
    protected int baBayCount = 0;
    protected int infantryBayCount = 0;
    protected int dockingCollarCount = 0;
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
    public AbstractUnitRating(Campaign campaign) {
        this.campaign = campaign;
        initialized = false;
    }

    public void reInitialize() {
        initialized = false;
        initValues();
        initialized = true;
    }

    public String getAverageExperience() {
        return SkillType.getExperienceLevelName(calcAverageExperience().setScale(0, RoundingMode.HALF_UP).intValue());
    }

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

    protected List<Person> getCommanderList() {
        return commanderList;
    }

    /**
     * Returns the commander (highest ranking person) for this force.
     *
     * @return
     */
    public Person getCommander() {
        if ((commander == null)) {

            // First, check to see if a commander as been flagged.
            commander = campaign.getFlaggedCommander();
            if (commander != null) {
                return commander;
            }

            // If we don't have a list of potential commanders, we cannot determine a commander.
            List<Person> commanderList = getCommanderList();
            if (commanderList == null || commanderList.isEmpty()) {
                commander = null;
                return null;
            }

            //Sort the list of personnel by rank from highest to lowest.  Whoever has the highest rank is the commander.
            Collections.sort(commanderList, new Comparator<Person>() {
                public int compare(Person p1, Person p2) {
                    // Active personnel outrank inactive personnel.
                    if (p1.isActive() && !p2.isActive()) {
                        return -1;
                    } else if (!p1.isActive() && p2.isActive()) {
                        return 1;
                    }

                    // Compare rank.
                    int p1Rank = p1.getRankOrder();
                    int p2Rank = p2.getRankOrder();
                    if (p1Rank > p2Rank) {
                        return -1;
                    } else if (p1Rank < p2Rank) {
                        return 1;
                    }

                    // Compare expreience.
                    int p1ExperienceLevel = p1.getExperienceLevel(false);
                    int p2ExperienceLevel = p2.getExperienceLevel(false);
                    if (p1ExperienceLevel > p2ExperienceLevel) {
                        return -1;
                    } else if (p1ExperienceLevel < p2ExperienceLevel) {
                        return 1;
                    }
                    return 0;
                }
            });
            commander = commanderList.get(0);
        }

        return commander;
    }

    public int getCommanderSkill(String skillName) {
        Person commander = getCommander();
        if (commander == null) {
            return 0;
        }
        Skill skill = commander.getSkill(skillName);
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
     * @param u     The {@code Unit} to be evaluated.
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

    public int getTechValue() {

        //Make sure we have units.
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        //Number of high-tech units is equal to the number of IS2 units plus twice the number of Clan units.
        BigDecimal highTechNumber = new BigDecimal(countIS2 + (countClan * 2));

        //Conventional infantry does not count.
        int numberUnits = fighterCount + numberBaSquads + mechCount + lightVeeCount + numberOther;
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
     *
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

    public int getTransportValue() {
        int value = 0;

        //Find the percentage of units that are transported.
        transportPercent = getTransportPercent();

        //Compute the score.
        BigDecimal scoredPercent = transportPercent.subtract(new BigDecimal(50));
        if (scoredPercent.compareTo(BigDecimal.ZERO) < 0) {
            return value;
        }
        BigDecimal percentageScore = scoredPercent.divide(new BigDecimal(10), 0, RoundingMode.DOWN);
        value += percentageScore.multiply(new BigDecimal(5)).setScale(0, RoundingMode.DOWN).intValue();
        value = Math.min(value, 25);

        //Only the highest of these values should be used, regardless of how many are actually owned.
        if (warhipWithDocsOwner) {
            value += 30;
        } else if (warshipOwner) {
            value += 20;
        } else if (jumpshipOwner) {
            value += 10;
        }

        return value;
    }

    public int getUnitRating(int score) {
        if (score < 0) {
            return DRAGOON_F;
        } else if (score < 46) {
            return DRAGOON_D;
        } else if (score < 86) {
            return DRAGOON_C;
        } else if (score < 121) {
            return DRAGOON_B;
        } else if (score < 151) {
            return DRAGOON_A;
        } else {
            return DRAGOON_ASTAR;
        }
    }

    public String getUnitRatingName(int rating) {
        switch (rating) {
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

    public String getUnitRating() {
        int score = calculateUnitRatingScore();
        return getUnitRatingName(getUnitRating(score)) + " (" + score + ")";
    }

    public int getUnitRatingAsInteger() {
        return getUnitRating(calculateUnitRatingScore());
    }

    public int getScore() {
        return calculateUnitRatingScore();
    }

    public int getModifier() {
        return (calculateUnitRatingScore() / 10);
    }

    /**
     * Calculates the weighted value of the unit based on if it is Infantry, Battle Armor or something else.
     *
     * @param u The {@code Unit} to be evaluated.
     * @return
     */
    protected BigDecimal getUnitValue(Unit u) {
        BigDecimal value = BigDecimal.ONE;
        if (isConventionalInfanry(u) && (((Infantry) u.getEntity()).getSquadN() == 1)) {
            value = new BigDecimal("0.25");
        }
        return value;
    }

    protected boolean isConventionalInfanry(Unit u) {
        return (u.getEntity() instanceof Infantry) && !(u.getEntity() instanceof BattleArmor);
    }

    /**
     * Returns the sum of all experience rating for all combat units.
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

    protected abstract String getExperienceLevelName(BigDecimal experience);

    /**
     * Calculates the unit's rating score.
     */
    protected abstract int calculateUnitRatingScore();

    /**
     * Recalculates the dragoons rating.  If this has already been done, the initialized flag should already be set true
     * and this method will immediately exit.
     */
    protected void initValues() {
        commanderList = new ArrayList<Person>();
        numberUnits = BigDecimal.ZERO;
        numberIS2 = BigDecimal.ZERO;
        numberClan = BigDecimal.ZERO;
        totalSkillLevels = BigDecimal.ZERO;
        mechCount = 0;
        fighterCount = 0;
        lightVeeCount = 0;
        battleArmorCount = 0;
        infantryCount = 0;
        numberInfSquads = 0;
        countClan = 0;
        countIS2 = 0;
        mechTechCount = BigDecimal.ZERO;
        aeroTech = BigDecimal.ZERO;
        veeTech = BigDecimal.ZERO;
        baTech = BigDecimal.ZERO;
        mechBayCount = 0;
        fighterBayCount = 0;
        baBayCount = 0;
        lightVeeBayCount = 0;
        infantryBayCount = 0;
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

    protected void updateBayCount(Dropship ds) {
        // ToDo Superheavy Bays.
        for (Bay bay : ds.getTransportBays()) {
            if (bay instanceof MechBay) {
                mechBayCount += bay.getCapacity();
            } else if (bay instanceof BattleArmorBay) {
                baBayCount += bay.getCapacity();
            } else if (bay instanceof InfantryBay) {
                infantryBayCount += bay.getCapacity();
            } else if (bay instanceof LightVehicleBay) {
                lightVeeBayCount += bay.getCapacity();
            } else if (bay instanceof HeavyVehicleBay) {
                heavyVeeBayCount += bay.getCapacity();
            } else if (bay instanceof ASFBay) {
                fighterBayCount += bay.getCapacity();
            } else if (bay instanceof SmallCraftBay) {
                smallCraftBayCount += bay.getCapacity();
            }
        }
    }

    protected void updateBayCount(Jumpship jumpship) {
        for (Bay bay : jumpship.getTransportBays()) {
            if (bay instanceof ASFBay) {
                fighterBayCount++;
            } else if (bay instanceof SmallCraftBay) {
                smallCraftBayCount++;
            }
        }
    }

    protected void updateDockingCollarCount(Jumpship jumpship) {
        dockingCollarCount += jumpship.getDockingCollars().size();
    }
}
