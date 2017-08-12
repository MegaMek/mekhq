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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.ASFBay;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.MechBay;
import megamek.common.SmallCraftBay;
import megamek.common.UnitType;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.unit.Unit;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/15/2012
 */
@SuppressWarnings("SameParameterValue")
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
    private final Map<String, Integer> skillRatingCounts = new HashMap<>();

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
    private int smallCraftCount = 0;
    private int dropshipCount = 0;
    private int warshipCount = 0;
    private int jumpshipCount = 0;
    private int mechBayCount = 0;
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

    public void reInitialize() {
        setInitialized(false);
        initValues();
    }

    public String getAverageExperience() {
        return getExperienceLevelName(calcAverageExperience());
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

        return (getSuccessCount() * 5) - (getFailCount() * 10) -
               (getBreachCount() * 25);
    }

    /**
     * Returns the average experience level for all combat personnel.
     */
    protected BigDecimal calcAverageExperience() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) > 0) {
            return getTotalSkillLevels().divide(getNumberUnits(), PRECISION,
                                                HALF_EVEN);
        }

        return BigDecimal.ZERO;
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

            //Sort the list of personnel by rank from highest to lowest.  
            // Whoever has the highest rank is the commander.
            commanderList.sort((p1, p2) -> {
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
            });
            commander = commanderList.get(0);
        }

        return commander;
    }

    int getCommanderSkill(String skillName) {
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
     */
    int getFailCount() {
        return failCount;
    }

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
     * Returns the overall percentage of fully supported units.
     */
    public BigDecimal getSupportPercent() {
        return supportPercent;
    }

    public int getTransportValue() {
        int value = 0;

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
     * Calculates the weighted value of the unit based on if it is Infantry, 
     * Battle Armor or something else.
     *
     * @param u The {@code Unit} to be evaluated.
     */
    BigDecimal getUnitValue(Unit u) {
        BigDecimal value = BigDecimal.ONE;
        if (isConventionalInfanry(u) &&
            (((Infantry) u.getEntity()).getSquadN() == 1)) {
            value = new BigDecimal("0.25");
        }
        return value;
    }

    boolean isConventionalInfanry(Unit u) {
        return (u.getEntity() instanceof Infantry) &&
               !(u.getEntity() instanceof BattleArmor);
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

    protected abstract String getExperienceLevelName(BigDecimal experience);

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
        getLogger().methodBegin(getClass(), "initValues()");
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
        setJumpshipCount(0);
        setWarshipCount(0);

        setMechBayCount(0);
        setFighterBayCount(0);
        setSmallCraftBayCount(0);
        setProtoBayCount(0);
        setBaBayCount(0);
        setLightVeeBayCount(0);
        setHeavyVeeBayCount(0);
        setInfantryBayCount(0);
        setDockingCollarCount(0);

        setWarhipWithDocsOwner(false);
        setWarshipOwner(false);
        setJumpshipOwner(false);

        setCommander(null);
        setBreachCount(0);
        setSuccessCount(0);
        setFailCount(0);
        setSupportPercent(BigDecimal.ZERO);
        setTransportPercent(BigDecimal.ZERO);
        setInitialized(true);
        clearSkillRatingCounts();
        getLogger().methodEnd(getClass(), "initValues()");
    }

    private void updateBayCount(Dropship ds) {
        // ToDo Superheavy Bays.
        for (Bay bay : ds.getTransportBays()) {
            if (bay instanceof MechBay) {
                setMechBayCount(getMechBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof BattleArmorBay) {
                setBaBayCount(getBaBayCount() + (int)bay.getCapacity());
            } else if (bay instanceof InfantryBay) {
                setInfantryBayCount(getInfantryBayCount() +
                                    (int) bay.getCapacity());
            } else if (bay instanceof LightVehicleBay) {
                setLightVeeBayCount(getLightVeeBayCount() +
                                    (int) bay.getCapacity());
            } else if (bay instanceof HeavyVehicleBay) {
                setHeavyVeeBayCount(getHeavyVeeBayCount() +
                                    (int) bay.getCapacity());
            } else if (bay instanceof ASFBay) {
                setFighterBayCount(getFighterBayCount() +
                                   (int) bay.getCapacity());
            } else if (bay instanceof SmallCraftBay) {
                setSmallCraftBayCount(getSmallCraftBayCount() +
                                      (int) bay.getCapacity());
            }
        }
    }

    void updateBayCount(Entity e) {
        if (e instanceof Dropship) {
            updateBayCount((Dropship) e);
        } else if (e instanceof Jumpship) {
            updateBayCount((Jumpship) e);
        }
    }

    private void updateBayCount(Jumpship jumpship) {
        for (Bay bay : jumpship.getTransportBays()) {
            if (bay instanceof ASFBay) {
                setFighterBayCount(getFighterBayCount() +
                                   (int) bay.getCapacity());
            } else if (bay instanceof SmallCraftBay) {
                setSmallCraftBayCount(getSmallCraftBayCount() +
                                      (int) bay.getCapacity());
            }
        }
    }

    void updateDockingCollarCount(Jumpship jumpship) {
        setDockingCollarCount(getDockingCollarCount() +
                              jumpship.getDockingCollars().size());
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
     * Increments the count of the given skill rating by one.
     *
     * @param rating The skill rating to be incremented.
     */
    void incrementSkillRatingCounts(final String rating) {
        int count = 1;
        if (skillRatingCounts.containsKey(rating)) {
            count += skillRatingCounts.get(rating);
        }
        skillRatingCounts.put(rating, count);
    }

    /**
     * Returns a map of skill ratings and their counts.
     */
    Map<String, Integer> getSkillRatingCounts() {
        // defensive copy
        return new HashMap<>(skillRatingCounts);
    }

    private void clearSkillRatingCounts() {
        skillRatingCounts.clear();
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

    int calcInfantryPlatoons() {
        return (int) Math.ceil(getInfantryCount() / 28);
    }

    int getDropshipCount() {
        return dropshipCount;
    }

    void setDropshipCount(int dropshipCount) {
        this.dropshipCount = dropshipCount;
    }

    private void incrementDropshipCount() {
        dropshipCount++;
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

    int getWarshipCount() {
        return warshipCount;
    }

    private void setWarshipCount(int warshipCount) {
        this.warshipCount = warshipCount;
    }

    private void incrementWarshipCount() {
        warshipCount++;
    }

    int getJumpshipCount() {
        return jumpshipCount;
    }

    void setJumpshipCount(int jumpshipCount) {
        this.jumpshipCount = jumpshipCount;
    }

    private void incrementJumpshipCount() {
        jumpshipCount++;
    }

    int getMechBayCount() {
        return mechBayCount;
    }

    private void setMechBayCount(int mechBayCount) {
        this.mechBayCount = mechBayCount;
    }

    int getProtoBayCount() {
        return protoBayCount;
    }

    private void setProtoBayCount(int protoBayCount) {
        this.protoBayCount = protoBayCount;
    }

    int getFighterBayCount() {
        return fighterBayCount;
    }

    private void setFighterBayCount(int fighterBayCount) {
        this.fighterBayCount = fighterBayCount;
    }

    int getSmallCraftBayCount() {
        return smallCraftBayCount;
    }

    private void setSmallCraftBayCount(int smallCraftBayCount) {
        this.smallCraftBayCount = smallCraftBayCount;
    }

    int getLightVeeBayCount() {
        return lightVeeBayCount;
    }

    private void setLightVeeBayCount(int lightVeeBayCount) {
        this.lightVeeBayCount = lightVeeBayCount;
    }

    int getHeavyVeeBayCount() {
        return heavyVeeBayCount;
    }

    private void setHeavyVeeBayCount(int heavyVeeBayCount) {
        this.heavyVeeBayCount = heavyVeeBayCount;
    }

    int getBaBayCount() {
        return baBayCount;
    }

    private void setBaBayCount(int baBayCount) {
        this.baBayCount = baBayCount;
    }

    int getInfantryBayCount() {
        return infantryBayCount;
    }

    private void setInfantryBayCount(int infantryBayCount) {
        this.infantryBayCount = infantryBayCount;
    }

    int getDockingCollarCount() {
        return dockingCollarCount;
    }

    void setDockingCollarCount(int dockingCollarCount) {
        this.dockingCollarCount = dockingCollarCount;
    }

    boolean isWarhipWithDocsOwner() {
        return warhipWithDocsOwner;
    }

    void setWarhipWithDocsOwner(boolean warhipWithDocsOwner) {
        this.warhipWithDocsOwner = warhipWithDocsOwner;
    }

    boolean isWarshipOwner() {
        return warshipOwner;
    }

    void setWarshipOwner(boolean warshipOwner) {
        this.warshipOwner = warshipOwner;
    }

    boolean isJumpshipOwner() {
        return jumpshipOwner;
    }

    void setJumpshipOwner(boolean jumpshipOwner) {
        this.jumpshipOwner = jumpshipOwner;
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
        final String METHOD_NAME = "updateUnitCounts(Unit)";
        
        if (u.isMothballed()) {
            return;
        }
        getLogger().log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                        "Adding " + u.getName() + " to unit counts.");

        Entity e = u.getEntity();
        if (null == e) {
            getLogger().log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                            "Unit " + u.getName() +
                            " is not an Entity.  Skipping.");
            return;
        }

        int unitType = UnitType.determineUnitTypeCode(e);
        getLogger().log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                        "Unit " + u.getName() + " is a " +
                        UnitType.getTypeDisplayableName(unitType));
        //todo: Add Airship when Megamek supports it.
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
                getLogger().log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                                "Unit " + u.getName() + " weight is " +
                                e.getWeight());
                if (e.getWeight() <= 50f) {
                    incrementLightVeeCount();
                } else if (e.getWeight() <= 100f) {
                    incrementHeavyVeeCount();
                } else {
                    incrementSuperHeavyVeeCount();
                }
                break;
            case UnitType.DROPSHIP:
                incrementDropshipCount();
                break;
            case UnitType.SMALL_CRAFT:
                incrementSmallCraftCount();
                break;
            case UnitType.WARSHIP:
                incrementWarshipCount();
                break;
            case UnitType.JUMPSHIP:
                incrementJumpshipCount();
                break;
            case UnitType.AERO:
            case UnitType.CONV_FIGHTER:
                incrementFighterCount();
                break;
            case UnitType.BATTLE_ARMOR:
                incrementNumberBaSquads();
                incrementBattleArmorCount(((BattleArmor) e).getSquadSize());
                break;
            case UnitType.INFANTRY:
                Infantry i = (Infantry) e;
                incrementInfantryCount(i.getSquadSize() * i.getSquadN());
                break;
        }
    }

    MMLogger getLogger() {
        return MekHQ.getLogger();
    }
}
