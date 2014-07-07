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

    private Campaign campaign = null;

    protected static final BigDecimal greenThreshold = new BigDecimal("5.5");
    protected static final BigDecimal regularThreshold = new BigDecimal("4.0");
    protected static final BigDecimal veteranThreshold = new BigDecimal("2.5");

    private List<Person> commanderList = new ArrayList<>();
    private BigDecimal numberUnits = BigDecimal.ZERO;
    private BigDecimal numberIS2 = BigDecimal.ZERO;
    private BigDecimal numberClan = BigDecimal.ZERO;
    private BigDecimal totalSkillLevels = BigDecimal.ZERO;
    private int mechCount = 0;
    private int superHeavyMechCount = 0;
    private int protoCount = 0;
    private int fighterCount = 0;
    private int lightVeeCount = 0;
    private int heavyVeeCount = 0;
    private int superHeavyVeeCount = 0;
    private int battleArmorCount = 0;
    private int numberBaSquads = 0;
    private int infantryCount = 0;
    private int numberInfSquads = 0;
    private int dropshipCount = 0;
    private int warshipCount = 0;
    private int jumpshipCount = 0;
    private int numberOther = 0; // Dropships, Jumpships, Warships, etc.
    private int countIS2 = 0;
    private int countClan = 0;
    private int mechBayCount = 0;
    private int superHeavyMechBayCount = 0;
    private int protoBayCount = 0;
    private int fighterBayCount = 0;
    private int smallCraftBayCount = 0;
    private int lightVeeBayCount = 0;
    private int heavyVeeBayCount = 0;
    private int baBayCount = 0;
    private int infantryBayCount = 0;
    private int dockingCollarCount = 0;
    private boolean warhipWithDocsOwner = false;
    private boolean warshipOwner = false;
    private boolean jumpshipOwner = false;
    private Person commander = null;
    private int breachCount = 0;
    private int successCount = 0;
    private int failCount = 0;
    private BigDecimal supportPercent = BigDecimal.ZERO;
    private BigDecimal transportPercent = BigDecimal.ZERO;
    private BigDecimal highTechPercent = BigDecimal.ZERO;

    private static boolean initialized = false;

    /**
     * Default constructor.
     *
     * @param campaign The MekHQ {@code Campaign}
     */
    public AbstractUnitRating(Campaign campaign) {
        this.setCampaign(campaign);
        setInitialized(false);
    }

    protected static boolean isInitialized() {
        return initialized;
    }

    protected static void setInitialized(boolean initialized) {
        AbstractUnitRating.initialized = initialized;
    }

    public void reInitialize() {
        setInitialized(false);
        initValues();
    }

    public String getAverageExperience() {
        return SkillType.getExperienceLevelName(calcAverageExperience().setScale(0, RoundingMode.HALF_UP).intValue());
    }

    public int getCombatRecordValue() {
        setSuccessCount(0);
        setFailCount(0);
        setBreachCount(0);
        for (Mission m : getCampaign().getMissions()) {

            //Skip ongoing missions.
            if (m.isActive()) {
                continue;
            }

            if (m.getStatus() == Mission.S_SUCCESS) {
                setSuccessCount(getSuccessCount() + 1);
            } else if (m.getStatus() == Mission.S_FAILED) {
                setFailCount(getFailCount() + 1);
            } else if (m.getStatus() == Mission.S_BREACH) {
                setBreachCount(getBreachCount() + 1);
            }
        }

        return (getSuccessCount() * 5) - (getFailCount() * 10) - (getBreachCount() * 25);
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
            commander = getCampaign().getFlaggedCommander();
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
                    int p1Rank = p1.getRankNumeric();
                    int p2Rank = p2.getRankNumeric();
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
                setNumberClan(getNumberClan().add(value));
                if (!isConventionalInfanry(u)) {
                    setCountClan(getCountClan() + 1);
                }
            } else {
                setNumberIS2(getNumberIS2().add(value));
                if (!isConventionalInfanry(u)) {
                    setCountIS2(getCountIS2() + 1);
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
        BigDecimal highTechNumber = new BigDecimal(getCountIS2() + (getCountClan() * 2));

        //Conventional infantry does not count.
        int numberUnits = getFighterCount() + getNumberBaSquads() + getMechCount() + getLightVeeCount() + getNumberOther();
        if (numberUnits <= 0) {
            return 0;
        }

        //Calculate the percentage of high-tech units.
        setHighTechPercent(highTechNumber.divide(new BigDecimal(numberUnits), PRECISION, HALF_EVEN));
        setHighTechPercent(getHighTechPercent().multiply(ONE_HUNDRED));

        //Cannot go above 100 percent.
        if (getHighTechPercent().compareTo(ONE_HUNDRED) > 0) {
            setHighTechPercent(ONE_HUNDRED);
        }

        //Score is calculated from percentage above 30%.
        BigDecimal scoredPercent = getHighTechPercent().subtract(new BigDecimal(30));

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
        setTransportPercent(getTransportPercent());

        //Compute the score.
        BigDecimal scoredPercent = getTransportPercent().subtract(new BigDecimal(50));
        if (scoredPercent.compareTo(BigDecimal.ZERO) < 0) {
            return value;
        }
        BigDecimal percentageScore = scoredPercent.divide(new BigDecimal(10), 0, RoundingMode.DOWN);
        value += percentageScore.multiply(new BigDecimal(5)).setScale(0, RoundingMode.DOWN).intValue();
        value = Math.min(value, 25);

        //Only the highest of these values should be used, regardless of how many are actually owned.
        if (isWarhipWithDocsOwner()) {
            value += 30;
        } else if (isWarshipOwner()) {
            value += 20;
        } else if (isJumpshipOwner()) {
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
    	return getTotalSkillLevels(true);
    }

    /**
     * Returns the sum of all experience rating for all combat units.
     *
     * @param canInit Whether or not this method may initialize the values
     * @return
     */
    protected BigDecimal getTotalSkillLevels(boolean canInit) {
    	if (canInit && !isInitialized()) {
    		initValues();
    	}
        return totalSkillLevels;
    }

    /**
     * Returns the total number of combat units.
     *
     * @return
     */
    protected BigDecimal getNumberUnits() {
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
        setCommanderList(new ArrayList<Person>());
        setNumberUnits(BigDecimal.ZERO);
        setNumberIS2(BigDecimal.ZERO);
        setNumberClan(BigDecimal.ZERO);
        setTotalSkillLevels(BigDecimal.ZERO);
        setMechCount(0);
        setFighterCount(0);
        setLightVeeCount(0);
        setBattleArmorCount(0);
        setInfantryCount(0);
        setNumberInfSquads(0);
        setCountClan(0);
        setCountIS2(0);
        setMechBayCount(0);
        setFighterBayCount(0);
        setBaBayCount(0);
        setLightVeeBayCount(0);
        setInfantryBayCount(0);
        setWarhipWithDocsOwner(false);
        setWarshipOwner(false);
        setJumpshipOwner(false);
        setCommander(null);
        setBreachCount(0);
        setSuccessCount(0);
        setFailCount(0);
        setSupportPercent(BigDecimal.ZERO);
        setTransportPercent(BigDecimal.ZERO);
        setHighTechPercent(BigDecimal.ZERO);
        setInitialized(true);
    }

    protected void updateBayCount(Dropship ds) {
        // ToDo Superheavy Bays.
        for (Bay bay : ds.getTransportBays()) {
            if (bay instanceof MechBay) {
                setMechBayCount(getMechBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof BattleArmorBay) {
                setBaBayCount(getBaBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof InfantryBay) {
                setInfantryBayCount(getInfantryBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof LightVehicleBay) {
                setLightVeeBayCount(getLightVeeBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof HeavyVehicleBay) {
                setHeavyVeeBayCount(getHeavyVeeBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof ASFBay) {
                setFighterBayCount(getFighterBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof SmallCraftBay) {
                setSmallCraftBayCount(getSmallCraftBayCount() + (int)bay.getCapacity());
            }
        }
    }

    protected void updateBayCount(Jumpship jumpship) {
        for (Bay bay : jumpship.getTransportBays()) {
            if (bay instanceof ASFBay) {
                setFighterBayCount(getFighterBayCount() + 1);
            } else if (bay instanceof SmallCraftBay) {
                setSmallCraftBayCount(getSmallCraftBayCount() + 1);
            }
        }
    }

    protected void updateDockingCollarCount(Jumpship jumpship) {
        setDockingCollarCount(getDockingCollarCount() + jumpship.getDockingCollars().size());
    }

    protected Campaign getCampaign() {
        return campaign;
    }

    protected void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    protected void setCommanderList(List<Person> commanderList) {
        this.commanderList = commanderList;
    }

    protected void setNumberUnits(BigDecimal numberUnits) {
        this.numberUnits = numberUnits;
    }

    protected BigDecimal getNumberIS2() {
        return numberIS2;
    }

    protected void setNumberIS2(BigDecimal numberIS2) {
        this.numberIS2 = numberIS2;
    }

    protected BigDecimal getNumberClan() {
        return numberClan;
    }

    protected void setNumberClan(BigDecimal numberClan) {
        this.numberClan = numberClan;
    }

    protected void setTotalSkillLevels(BigDecimal totalSkillLevels) {
        this.totalSkillLevels = totalSkillLevels;
    }

    protected int getMechCount() {
        return mechCount;
    }

    protected void setMechCount(int mechCount) {
        this.mechCount = mechCount;
    }

    protected int getSuperHeavyMechCount() {
        return superHeavyMechCount;
    }

    protected void setSuperHeavyMechCount(int superHeavyMechCount) {
        this.superHeavyMechCount = superHeavyMechCount;
    }

    protected int getProtoCount() {
        return protoCount;
    }

    protected void setProtoCount(int protoCount) {
        this.protoCount = protoCount;
    }

    protected int getFighterCount() {
        return fighterCount;
    }

    protected void setFighterCount(int fighterCount) {
        this.fighterCount = fighterCount;
    }

    protected int getLightVeeCount() {
        return lightVeeCount;
    }

    protected void setLightVeeCount(int lightVeeCount) {
        this.lightVeeCount = lightVeeCount;
    }

    protected int getHeavyVeeCount() {
        return heavyVeeCount;
    }

    protected void setHeavyVeeCount(int heavyVeeCount) {
        this.heavyVeeCount = heavyVeeCount;
    }

    protected int getSuperHeavyVeeCount() {
        return superHeavyVeeCount;
    }

    protected void setSuperHeavyVeeCount(int superHeavyVeeCount) {
        this.superHeavyVeeCount = superHeavyVeeCount;
    }

    protected int getBattleArmorCount() {
        return battleArmorCount;
    }

    protected void setBattleArmorCount(int battleArmorCount) {
        this.battleArmorCount = battleArmorCount;
    }

    protected int getNumberBaSquads() {
        return numberBaSquads;
    }

    protected void setNumberBaSquads(int numberBaSquads) {
        this.numberBaSquads = numberBaSquads;
    }

    protected int getInfantryCount() {
        return infantryCount;
    }

    protected void setInfantryCount(int infantryCount) {
        this.infantryCount = infantryCount;
    }

    protected int getNumberInfSquads() {
        return numberInfSquads;
    }

    protected void setNumberInfSquads(int numberInfSquads) {
        this.numberInfSquads = numberInfSquads;
    }

    protected int getDropshipCount() {
        return dropshipCount;
    }

    protected void setDropshipCount(int dropshipCount) {
        this.dropshipCount = dropshipCount;
    }

    protected int getWarshipCount() {
        return warshipCount;
    }

    protected void setWarshipCount(int warshipCount) {
        this.warshipCount = warshipCount;
    }

    protected int getJumpshipCount() {
        return jumpshipCount;
    }

    protected void setJumpshipCount(int jumpshipCount) {
        this.jumpshipCount = jumpshipCount;
    }

    protected int getNumberOther() {
        return numberOther;
    }

    protected void setNumberOther(int numberOther) {
        this.numberOther = numberOther;
    }

    protected int getCountIS2() {
        return countIS2;
    }

    protected void setCountIS2(int countIS2) {
        this.countIS2 = countIS2;
    }

    protected int getCountClan() {
        return countClan;
    }

    protected void setCountClan(int countClan) {
        this.countClan = countClan;
    }

    protected int getMechBayCount() {
        return mechBayCount;
    }

    protected void setMechBayCount(int mechBayCount) {
        this.mechBayCount = mechBayCount;
    }

    protected int getSuperHeavyMechBayCount() {
        return superHeavyMechBayCount;
    }

    protected void setSuperHeavyMechBayCount(int superHeavyMechBayCount) {
        this.superHeavyMechBayCount = superHeavyMechBayCount;
    }

    protected int getProtoBayCount() {
        return protoBayCount;
    }

    protected void setProtoBayCount(int protoBayCount) {
        this.protoBayCount = protoBayCount;
    }

    protected int getFighterBayCount() {
        return fighterBayCount;
    }

    protected void setFighterBayCount(int fighterBayCount) {
        this.fighterBayCount = fighterBayCount;
    }

    protected int getSmallCraftBayCount() {
        return smallCraftBayCount;
    }

    protected void setSmallCraftBayCount(int smallCraftBayCount) {
        this.smallCraftBayCount = smallCraftBayCount;
    }

    protected int getLightVeeBayCount() {
        return lightVeeBayCount;
    }

    protected void setLightVeeBayCount(int lightVeeBayCount) {
        this.lightVeeBayCount = lightVeeBayCount;
    }

    protected int getHeavyVeeBayCount() {
        return heavyVeeBayCount;
    }

    protected void setHeavyVeeBayCount(int heavyVeeBayCount) {
        this.heavyVeeBayCount = heavyVeeBayCount;
    }

    protected int getBaBayCount() {
        return baBayCount;
    }

    protected void setBaBayCount(int baBayCount) {
        this.baBayCount = baBayCount;
    }

    protected int getInfantryBayCount() {
        return infantryBayCount;
    }

    protected void setInfantryBayCount(int infantryBayCount) {
        this.infantryBayCount = infantryBayCount;
    }

    protected int getDockingCollarCount() {
        return dockingCollarCount;
    }

    protected void setDockingCollarCount(int dockingCollarCount) {
        this.dockingCollarCount = dockingCollarCount;
    }

    protected boolean isWarhipWithDocsOwner() {
        return warhipWithDocsOwner;
    }

    protected void setWarhipWithDocsOwner(boolean warhipWithDocsOwner) {
        this.warhipWithDocsOwner = warhipWithDocsOwner;
    }

    protected boolean isWarshipOwner() {
        return warshipOwner;
    }

    protected void setWarshipOwner(boolean warshipOwner) {
        this.warshipOwner = warshipOwner;
    }

    protected boolean isJumpshipOwner() {
        return jumpshipOwner;
    }

    protected void setJumpshipOwner(boolean jumpshipOwner) {
        this.jumpshipOwner = jumpshipOwner;
    }

    protected void setCommander(Person commander) {
        this.commander = commander;
    }

    protected void setBreachCount(int breachCount) {
        this.breachCount = breachCount;
    }

    protected void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    protected void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    protected void setSupportPercent(BigDecimal supportPercent) {
        this.supportPercent = supportPercent;
    }

    @Override
    public BigDecimal getTransportPercent() {
        return transportPercent;
    }

    protected void setTransportPercent(BigDecimal transportPercent) {
        this.transportPercent = transportPercent;
    }

    protected BigDecimal getHighTechPercent() {
        return highTechPercent;
    }

    protected void setHighTechPercent(BigDecimal highTechPercent) {
        this.highTechPercent = highTechPercent;
    }
}
