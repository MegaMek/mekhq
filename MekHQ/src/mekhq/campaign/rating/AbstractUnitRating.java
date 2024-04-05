/*
 * AbstractUnitRating.java
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
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @since 3/15/2012
 */
@SuppressWarnings(value = "SameParameterValue")
public abstract class AbstractUnitRating implements IUnitRating {

    static final int HEADER_LENGTH = 19;
    static final int SUBHEADER_LENGTH = 23;
    static final int CATEGORY_LENGTH = 26;
    static final int SUBCATEGORY_LENGTH = 31;

    static final BigDecimal HUNDRED = new BigDecimal(100);

    private Campaign campaign = null;

    static final BigDecimal greenThreshold = new BigDecimal("5.5");
    static final BigDecimal regularThreshold = new BigDecimal("4.0");
    static final BigDecimal veteranThreshold = new BigDecimal("2.5");
    private final Map<SkillLevel, Integer> skillLevelCounts = new HashMap<>();

    private List<Person> commanderList = new ArrayList<>();
    private BigDecimal numberUnits = BigDecimal.ZERO;
    private BigDecimal totalSkillLevels = BigDecimal.ZERO;
    private int mechCount = 0;
    private int protoCount = 0;
    private int fighterCount = 0;
    private int lightVeeCount = 0;
    private int heavyVeeCount = 0;
    private int superHeavyVeeCount = 0;
    private int battleArmorCount = 0;
    private int numberBaSquads = 0;
    private int infantryCount = 0;
    private int infantryUnitCount = 0;
    private int smallCraftCount = 0;
    private int dropShipCount = 0;
    private int warShipCount = 0;
    private int jumpShipCount = 0;
    private int mechBayCount = 0;
    private int protoBayCount = 0;
    private int fighterBayCount = 0;
    private int smallCraftBayCount = 0;
    private int lightVeeBayCount = 0;
    private int heavyVeeBayCount = 0;
    private int superHeavyVeeBayCount = 0;
    private int baBayCount = 0;
    private int infantryBayCount = 0;
    private int dockingCollarCount = 0;
    private boolean warShipWithDocsOwner = false;
    private boolean warShipOwner = false;
    private boolean jumpShipOwner = false;
    private Person commander = null;
    private int breachCount = 0;
    private int successCount = 0;
    private int failCount = 0;
    private int partialCount = 0;
    private BigDecimal supportPercent = BigDecimal.ZERO;
    private BigDecimal transportPercent = BigDecimal.ZERO;

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

    static boolean isInitialized() {
        return initialized;
    }

    private static void setInitialized(boolean initialized) {
        AbstractUnitRating.initialized = initialized;
    }

    @Override
    public void reInitialize() {
        setInitialized(false);
        initValues();
    }

    @Override
    public SkillLevel getAverageExperience() {
        return getExperienceLevelName(calcAverageExperience());
    }

    protected abstract SkillLevel getExperienceLevelName(BigDecimal experience);

    @Override
    public int getCombatRecordValue() {
        setSuccessCount(0);
        setPartialCount(0);
        setFailCount(0);
        setBreachCount(0);
        for (Mission m : getCampaign().getCompletedMissions()) {
            switch (m.getStatus()) {
                case SUCCESS:
                    setSuccessCount(getSuccessCount() + 1);
                    break;
                case PARTIAL:
                    setPartialCount(getPartialCount() + 1);
                    break;
                case FAILED:
                    setFailCount(getFailCount() + 1);
                    break;
                case BREACH:
                    setBreachCount(getBreachCount() + 1);
                    break;
                default:
                    break;
            }
        }

        /* getPartialCount() x 0 is still 0, not needed to calculate final score. */
        return (getSuccessCount() * 5) - (getFailCount() * 10) - (getBreachCount() * 25);
    }

    /**
     * Returns the average experience level for all combat personnel.
     */
    protected BigDecimal calcAverageExperience() {
        return hasUnits() ? getTotalSkillLevels().divide(getNumberUnits(), PRECISION, HALF_EVEN)
                : BigDecimal.ZERO;
    }

    /**
     * Returns the number of breached contracts.
     */
    int getBreachCount() {
        return breachCount;
    }

    List<Person> getCommanderList() {
        return commanderList;
    }

    /**
     * Returns the commander (highest ranking person) for this force.
     */
    @Override
    public Person getCommander() {
        if ((commander == null)) {

            // First, check to see if a commander as been flagged.
            commander = getCampaign().getFlaggedCommander();
            if (commander != null) {
                return commander;
            }

            // If we don't have a list of potential commanders, we cannot
            // determine a commander.
            List<Person> commanderList = getCommanderList();
            if (commanderList == null || commanderList.isEmpty()) {
                commander = null;
                return null;
            }

            //Sort the list of personnel by rank from highest to lowest
            // Whoever has the highest rank is the commander
            commanderList.sort((p1, p2) -> {
                // Active personnel outrank inactive personnel
                if (p1.getStatus().isActive() && !p2.getStatus().isActive()) {
                    return -1;
                } else if (!p1.getStatus().isActive() && p2.getStatus().isActive()) {
                    return 1;
                }

                // Compare rank
                int p1Rank = p1.getRankNumeric();
                int p2Rank = p2.getRankNumeric();
                if (p1Rank > p2Rank) {
                    return -1;
                } else if (p1Rank < p2Rank) {
                    return 1;
                }

                // Compare experience
                int p1ExperienceLevel = p1.getExperienceLevel(getCampaign(), false);
                int p2ExperienceLevel = p2.getExperienceLevel(getCampaign(), false);
                if (p1ExperienceLevel > p2ExperienceLevel) {
                    return -1;
                } else if (p1ExperienceLevel < p2ExperienceLevel) {
                    return 1;
                }
                return 0;
            });
            commander = commanderList.get(0);
        }

        return commander;
    }

    /**
     *  Returns the Commander's Skill Level with Bonus for the specified skill
     */
    int getCommanderSkillLevelWithBonus(String skillName) {
        Person commander = getCommander();
        if (commander == null) {
            return 0;
        }
        Skill skill = commander.getSkill(skillName);
        if (skill == null) {
            return 0;
        }
        return skill.getLevel() + skill.getBonus();
    }

    /**
     * Returns the number of failed contracts.
     */
    int getFailCount() {
        return failCount;
    }

    @Override
    public int getTechValue() {
        return 0;
    }

    /**
     * Returns the number of successfully completed contracts.
     */
    int getSuccessCount() {
        return successCount;
    }

    /**
     * Returns number of partially completed contracts.
     */
    int getPartialCount() {
        return partialCount;
    }

    /**
     * Returns the overall percentage of fully supported units.
     */
    @Override
    public BigDecimal getSupportPercent() {
        return supportPercent;
    }

    @Override
    public int getTransportValue() {
        int value = 0;

        if (!hasUnits()) {
            return 0;
        }

        //Find the percentage of units that are transported.
        setTransportPercent(getTransportPercent());

        //Compute the score.
        BigDecimal scoredPercent = getTransportPercent().subtract(
                new BigDecimal(50));
        if (scoredPercent.compareTo(BigDecimal.ZERO) < 0) {
            return value;
        }
        BigDecimal percentageScore = scoredPercent.divide(new BigDecimal(10),
                                                          0,
                                                          RoundingMode.DOWN);
        value += percentageScore.multiply(new BigDecimal(5))
                                .setScale(0, RoundingMode.DOWN)
                                .intValue();
        value = Math.min(value, 25);

        //Only the highest of these values should be used, regardless of how
        // many are actually owned.
        if (isWarShipWithDocsOwner()) {
            value += 30;
        } else if (isWarShipOwner()) {
            value += 20;
        } else if (isJumpShipOwner()) {
            value += 10;
        }

        return value;
    }

    @Override
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

    @Override
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

    @Override
    public String getUnitRating() {
        int score = calculateUnitRatingScore();
        return getUnitRatingName(getUnitRating(score)) + " (" + score + ")";
    }

    @Override
    public int getUnitRatingAsInteger() {
        return getUnitRating(calculateUnitRatingScore());
    }

    @Override
    public int getScore() {
        return calculateUnitRatingScore();
    }

    @Override
    public int getModifier() {
        return (calculateUnitRatingScore() / 10);
    }

    /**
     * Calculates the weighted value of the unit based on if it is Infantry,
     * Battle Armor or something else.
     *
     * @param u The {@code Unit} to be evaluated.
     */
    BigDecimal getUnitValue(Unit u) {
        BigDecimal value = BigDecimal.ONE;
        if (u.isConventionalInfantry() && (((Infantry) u.getEntity()).getSquadCount() == 1)) {
            value = new BigDecimal("0.25");
        }
        return value;
    }

    /**
     * Returns the sum of all experience rating for all combat units.
     */
    BigDecimal getTotalSkillLevels() {
        return getTotalSkillLevels(true);
    }

    /**
     * Returns the sum of all experience rating for all combat units.
     *
     * @param canInit Whether or not this method may initialize the values
     */
    BigDecimal getTotalSkillLevels(boolean canInit) {
        if (canInit && !isInitialized()) {
            initValues();
        }
        return totalSkillLevels;
    }

    /**
     * Returns the total number of combat units.
     */
    protected BigDecimal getNumberUnits() {
        return numberUnits;
    }

    /**
     * @return if the unit has any units
     */
    protected boolean hasUnits() {
        return getNumberUnits().compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Calculates the unit's rating score.
     */
    protected abstract int calculateUnitRatingScore();

    /**
     * Recalculates the dragoons rating.  If this has already been done, the
     * initialized flag should already be set true
     * and this method will immediately exit.
     */
    protected void initValues() {
        setCommanderList(new ArrayList<>());
        setNumberUnits(BigDecimal.ZERO);
        setTotalSkillLevels(BigDecimal.ZERO);

        setMechCount(0);
        setFighterCount(0);
        setSmallCraftCount(0);
        setProtoCount(0);
        setLightVeeCount(0);
        setHeavyVeeCount(0);
        setSuperHeavyVeeCount(0);
        setBattleArmorCount(0);
        setNumberBaSquads(0);
        setInfantryCount(0);
        setInfantryUnitCount(0);
        setDropShipCount(0);
        setJumpShipCount(0);
        setWarShipCount(0);

        setMechBayCount(0);
        setFighterBayCount(0);
        setSmallCraftBayCount(0);
        setProtoBayCount(0);
        setSuperHeavyVeeBayCount(0);
        setHeavyVeeBayCount(0);
        setLightVeeBayCount(0);
        setBaBayCount(0);
        setInfantryBayCount(0);
        setDockingCollarCount(0);

        setWarShipWithDocsOwner(false);
        setWarShipOwner(false);
        setJumpShipOwner(false);

        setCommander(null);
        setBreachCount(0);
        setSuccessCount(0);
        setFailCount(0);
        setPartialCount(0);
        setSupportPercent(BigDecimal.ZERO);
        setTransportPercent(BigDecimal.ZERO);
        setInitialized(true);
        clearSkillRatingCounts();
    }

    /**
     * Updates the count of storage bays that may be used in Interstellar transport (part of transport capacity calculations)
     *
     * @param e is the unit that may or may not contain bays that need to be included in the count
     */
    void updateBayCount(Entity e) {
        if (((e instanceof Jumpship) || (e instanceof Dropship)) && !(e instanceof SpaceStation)) {
            for (Bay bay : e.getTransportBays()) {
                if (bay instanceof MechBay) {
                    setMechBayCount(getMechBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof ASFBay) {
                    setFighterBayCount(getFighterBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof SmallCraftBay) {
                    setSmallCraftBayCount(getSmallCraftBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof ProtomechBay) {
                    setProtoBayCount(getProtoBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof SuperHeavyVehicleBay) {
                    setSuperHeavyVeeBayCount(getSuperHeavyVeeBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof HeavyVehicleBay) {
                    setHeavyVeeBayCount(getHeavyVeeBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof LightVehicleBay) {
                    setLightVeeBayCount(getLightVeeBayCount() + (int) bay.getCapacity());
                }  else if (bay instanceof BattleArmorBay) {
                    setBaBayCount(getBaBayCount() + (int) bay.getCapacity());
                } else if (bay instanceof InfantryBay) {
                    setInfantryBayCount(getInfantryBayCount() + (int)
                            Math.floor(bay.getCapacity() / ((InfantryBay) bay).getPlatoonType().getWeight()));
                }
            }
        }
    }

    void updateDockingCollarCount(Jumpship jumpShip) {
        setDockingCollarCount(getDockingCollarCount() + jumpShip.getDockingCollars().size());
    }

    protected Campaign getCampaign() {
        return campaign;
    }

    protected void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    private void setCommanderList(List<Person> commanderList) {
        this.commanderList = commanderList;
    }

    void setNumberUnits(BigDecimal numberUnits) {
        this.numberUnits = numberUnits;
    }

    void setTotalSkillLevels(BigDecimal totalSkillLevels) {
        this.totalSkillLevels = totalSkillLevels;
    }

    /**
     * Increments the count of the given skill level by one.
     *
     * @param skillLevel The skill level to be incremented.
     */
    void incrementSkillRatingCounts(final SkillLevel skillLevel) {
        int count = 1;
        if (skillLevelCounts.containsKey(skillLevel)) {
            count += skillLevelCounts.get(skillLevel);
        }
        skillLevelCounts.put(skillLevel, count);
    }

    /**
     * Returns a map of skill levels and their counts.
     */
    Map<SkillLevel, Integer> getSkillLevelCounts() {
        // defensive copy
        return new HashMap<>(skillLevelCounts);
    }

    private void clearSkillRatingCounts() {
        skillLevelCounts.clear();
    }

    int getMechCount() {
        return mechCount;
    }

    void setMechCount(int mechCount) {
        this.mechCount = mechCount;
    }

    private void incrementMechCount() {
        mechCount++;
    }

    private void setInfantryUnitCount(int count) {
        infantryUnitCount = count;
    }

    private void incrementInfantryUnitCount() {
        infantryUnitCount++;
    }

    public int getInfantryUnitCount() {
        return infantryUnitCount;
    }

    int getProtoCount() {
        return protoCount;
    }

    void setProtoCount(int protoCount) {
        this.protoCount = protoCount;
    }

    private void incrementProtoCount() {
        protoCount++;
    }

    int getFighterCount() {
        return fighterCount;
    }

    void setFighterCount(int fighterCount) {
        this.fighterCount = fighterCount;
    }

    private void incrementFighterCount() {
        fighterCount++;
    }

    int getLightVeeCount() {
        return lightVeeCount;
    }

    void setLightVeeCount(int lightVeeCount) {
        this.lightVeeCount = lightVeeCount;
    }

    private void incrementLightVeeCount() {
        lightVeeCount++;
    }

    int getHeavyVeeCount() {
        return heavyVeeCount;
    }

    private void setHeavyVeeCount(int heavyVeeCount) {
        this.heavyVeeCount = heavyVeeCount;
    }

    private void incrementHeavyVeeCount() {
        heavyVeeCount++;
    }

    int getSuperHeavyVeeCount() {
        return superHeavyVeeCount;
    }

    private void setSuperHeavyVeeCount(int superHeavyVeeCount) {
        this.superHeavyVeeCount = superHeavyVeeCount;
    }

    private void incrementSuperHeavyVeeCount() {
        superHeavyVeeCount++;
    }

    int getBattleArmorCount() {
        return battleArmorCount;
    }

    void setBattleArmorCount(int battleArmorCount) {
        this.battleArmorCount = battleArmorCount;
    }

    private void incrementBattleArmorCount(int amount) {
        battleArmorCount += amount;
    }

    int getNumberBaSquads() {
        return numberBaSquads;
    }

    private void setNumberBaSquads(int numberBaSquads) {
        this.numberBaSquads = numberBaSquads;
    }

    private void incrementNumberBaSquads() {
        numberBaSquads++;
    }

    int getInfantryCount() {
        return infantryCount;
    }

    void setInfantryCount(int infantryCount) {
        this.infantryCount = infantryCount;
    }

    private void incrementInfantryCount(int amount) {
        infantryCount += amount;
    }

    /**
     * Calculate the number of infantry "platoons" present in the company, based on the numbers
     * of various infantry present. Per CamOps, the simplification is that an infantry cube can
     * house 28 infantry.
     * @return Number of infantry "platoons" in the company.
     */
    int calcInfantryPlatoons() {
        return (int) Math.ceil((double) getInfantryCount() / 28);
    }

    int getDropShipCount() {
        return dropShipCount;
    }

    void setDropShipCount(int dropShipCount) {
        this.dropShipCount = dropShipCount;
    }

    private void incrementDropShipCount() {
        dropShipCount++;
    }

    int getSmallCraftCount() {
        return smallCraftCount;
    }

    void setSmallCraftCount(int smallCraftCount) {
        this.smallCraftCount = smallCraftCount;
    }

    private void incrementSmallCraftCount() {
        smallCraftCount++;
    }

    int getWarShipCount() {
        return warShipCount;
    }

    private void setWarShipCount(int warShipCount) {
        this.warShipCount = warShipCount;
    }

    private void incrementWarShipCount() {
        warShipCount++;
    }

    int getJumpShipCount() {
        return jumpShipCount;
    }

    void setJumpShipCount(int jumpShipCount) {
        this.jumpShipCount = jumpShipCount;
    }

    private void incrementJumpShipCount() {
        jumpShipCount++;
    }

    int getMechBayCount() {
        return mechBayCount;
    }

    protected void setMechBayCount(int mechBayCount) {
        this.mechBayCount = mechBayCount;
    }

    int getProtoBayCount() {
        return protoBayCount;
    }

    protected void setProtoBayCount(int protoBayCount) {
        this.protoBayCount = protoBayCount;
    }

    int getFighterBayCount() {
        return fighterBayCount;
    }

    protected void setFighterBayCount(int fighterBayCount) {
        this.fighterBayCount = fighterBayCount;
    }

    int getSmallCraftBayCount() {
        return smallCraftBayCount;
    }

    protected void setSmallCraftBayCount(int smallCraftBayCount) {
        this.smallCraftBayCount = smallCraftBayCount;
    }

    int getLightVeeBayCount() {
        return lightVeeBayCount;
    }

    protected void setLightVeeBayCount(int lightVeeBayCount) {
        this.lightVeeBayCount = lightVeeBayCount;
    }

    int getHeavyVeeBayCount() {
        return heavyVeeBayCount;
    }

    protected void setHeavyVeeBayCount(int heavyVeeBayCount) {
        this.heavyVeeBayCount = heavyVeeBayCount;
    }

    int getSuperHeavyVeeBayCount() {
        return superHeavyVeeBayCount;
    }

    protected void setSuperHeavyVeeBayCount(int superHeavyVeeBayCount) {
        this.superHeavyVeeBayCount = superHeavyVeeBayCount;
    }

    int getBaBayCount() {
        return baBayCount;
    }

    protected void setBaBayCount(int baBayCount) {
        this.baBayCount = baBayCount;
    }

    int getInfantryBayCount() {
        return infantryBayCount;
    }

    protected void setInfantryBayCount(int infantryBayCount) {
        this.infantryBayCount = infantryBayCount;
    }

    int getDockingCollarCount() {
        return dockingCollarCount;
    }

    void setDockingCollarCount(int dockingCollarCount) {
        this.dockingCollarCount = dockingCollarCount;
    }

    boolean isWarShipWithDocsOwner() {
        return warShipWithDocsOwner;
    }

    void setWarShipWithDocsOwner(boolean warShipWithDocsOwner) {
        this.warShipWithDocsOwner = warShipWithDocsOwner;
    }

    boolean isWarShipOwner() {
        return warShipOwner;
    }

    void setWarShipOwner(boolean warShipOwner) {
        this.warShipOwner = warShipOwner;
    }

    boolean isJumpShipOwner() {
        return jumpShipOwner;
    }

    void setJumpShipOwner(boolean jumpShipOwner) {
        this.jumpShipOwner = jumpShipOwner;
    }

    protected void setCommander(Person commander) {
        this.commander = commander;
    }

    private void setBreachCount(int breachCount) {
        this.breachCount = breachCount;
    }

    private void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    private void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    private void setPartialCount(int partialCount) {
        this.partialCount = partialCount;
    }
    void setSupportPercent(BigDecimal supportPercent) {
        this.supportPercent = supportPercent;
    }

    @Override
    public BigDecimal getTransportPercent() {
        return transportPercent;
    }

    void setTransportPercent(BigDecimal transportPercent) {
        this.transportPercent = transportPercent;
    }

    void updateUnitCounts(Unit u) {
        if (u.isMothballed()) {
            return;
        }
        LogManager.getLogger().debug("Adding " + u.getName() + " to unit counts.");

        Entity e = u.getEntity();
        if (null == e) {
            LogManager.getLogger().debug("Unit " + u.getName() + " is not an Entity.  Skipping.");
            return;
        }

        int unitType = e.getUnitType();
        LogManager.getLogger().debug("Unit " + u.getName() + " is a " + UnitType.getTypeDisplayableName(unitType));
        // TODO : Add Airship when MegaMek supports it.
        switch (unitType) {
            case UnitType.MEK:
                incrementMechCount();
                break;
            case UnitType.PROTOMEK:
                incrementProtoCount();
                break;
            case UnitType.GUN_EMPLACEMENT:
            case UnitType.VTOL:
            case UnitType.TANK:
                LogManager.getLogger().debug("Unit " + u.getName() + " weight is " + e.getWeight());
                if (e.getWeight() <= 50f) {
                    incrementLightVeeCount();
                } else if (e.getWeight() <= 100f) {
                    incrementHeavyVeeCount();
                } else {
                    incrementSuperHeavyVeeCount();
                }
                break;
            case UnitType.DROPSHIP:
                incrementDropShipCount();
                break;
            case UnitType.SMALL_CRAFT:
                incrementSmallCraftCount();
                break;
            case UnitType.WARSHIP:
                incrementWarShipCount();
                break;
            case UnitType.JUMPSHIP:
                incrementJumpShipCount();
                break;
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.CONV_FIGHTER:
                incrementFighterCount();
                break;
            case UnitType.BATTLE_ARMOR:
                incrementNumberBaSquads();
                incrementBattleArmorCount(((BattleArmor) e).getSquadSize());
                break;
            case UnitType.INFANTRY:
                Infantry i = (Infantry) e;

                incrementInfantryCount(i.getSquadSize() * i.getSquadCount());
                incrementInfantryUnitCount();
                break;
        }
    }
}
