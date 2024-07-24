/*
 * CampaignOpsRating.java
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
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.randomEvents.personalities.*;
import mekhq.campaign.unit.Unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @since 3/12/2012
 */
public class CampaignOpsReputation extends AbstractUnitRating {

    private int nonAdminPersonnelCount = 0;

    // Tech Support & Admins.
    private int mechTechTeamsNeeded = 0;
    private int mechanicTeamsNeeded = 0;
    private int battleArmorTechTeamsNeeded = 0;
    private int aeroTechTeamsNeeded = 0;
    private int adminsNeeded = 0;

    private int totalTechTeams = 0;
    private int mechTechTeams = 0;
    private int aeroTechTeams = 0;
    private int mechanicTeams = 0;
    private int baTechTeams = 0;
    private int generalTechTeams = 0;
    private final List<String> craftWithoutCrew = new ArrayList<>();
    private int technicians = 0;

    private static final MMLogger logger = MMLogger.create(CampaignOpsReputation.class);

    public CampaignOpsReputation(Campaign campaign) {
        super(campaign);
    }

    @Override
    public UnitRatingMethod getUnitRatingMethod() {
        return UnitRatingMethod.CAMPAIGN_OPS;
    }

    int getNonAdminPersonnelCount() {
        return nonAdminPersonnelCount;
    }

    int getAdminsNeeded() {
        return adminsNeeded;
    }

    int getVeeCount() {
        return getLightVeeCount() + getHeavyVeeCount();
    }

    int getMechTechTeamsNeeded() {
        return mechTechTeamsNeeded;
    }

    int getMechanicTeamsNeeded() {
        return mechanicTeamsNeeded;
    }

    int getBattleArmorTechTeamsNeeded() {
        return battleArmorTechTeamsNeeded;
    }

    int getAeroTechTeamsNeeded() {
        return aeroTechTeamsNeeded;
    }

    private void countUnits() {
        // Reset counts
        setTotalSkillLevels(BigDecimal.ZERO);

        for (Unit u : getCampaign().getHangar().getUnits()) {
            if (u.isMothballed()) {
                continue;
            }
            if (!u.isPresent()) {
                continue;
            }

            updateUnitCounts(u);

            Person p = u.getCommander();
            if (p != null) {
                getCommanderList().add(p);
            }

            Entity entity = u.getEntity();

            if (u.isFullyCrewed()) {
                updateBayCount(entity);
            }

            int unitType = entity.getUnitType();
            if (UnitType.INFANTRY == unitType ||
                UnitType.BATTLE_ARMOR == unitType) {
                updateTotalSkill((Infantry) entity);
            } else {
                updateTotalSkill(u.getEntity().getCrew(), entity.getUnitType());
            }

            // todo: Add Mobile Structure when MegaMek supports it.
            switch (unitType) {
                case UnitType.SPACE_STATION, UnitType.NAVAL, UnitType.DROPSHIP -> {
                    if (u.getFullCrewSize() < u.getActiveCrew().size()) {
                        addCraftWithoutCrew(u);
                    }
                }
                case UnitType.WARSHIP, UnitType.JUMPSHIP -> {
                    if (u.isFullyCrewed()) {
                        updateDockingCollarCount((Jumpship) entity);
                    } else {
                        addCraftWithoutCrew(u);
                    }
                }
                default -> {}
            }

            // UnitType doesn't include FixedWingSupport.
            if (entity instanceof FixedWingSupport) {
                if (u.getFullCrewSize() < u.getActiveCrew().size()) {
                    addCraftWithoutCrew(u);
                }
            }
        }
    }

    private void updateTotalSkill(Infantry infantry) {
        Crew crew = infantry.getCrew();
        if (null == crew) {
            return;
        }

        int gunnery = crew.getGunnery();
        int antiMek = infantry.getAntiMekSkill();
        if (antiMek == 0 || antiMek == 8) {
            antiMek = gunnery + 1;
        }

        BigDecimal skillLevel = BigDecimal.valueOf(gunnery).add(BigDecimal.valueOf(antiMek));

        incrementSkillRatingCounts(getExperienceLevelName(skillLevel));
        setTotalSkillLevels(getTotalSkillLevels(false).add(skillLevel));
    }

    private void updateTotalSkill(Crew crew, int unitType) {
        // Make sure we have a crew.
        if (crew == null) {
            return;
        }

        boolean hasPilot = false;
        int gunnery;
        int piloting = 0;

        switch (unitType) {
            case UnitType.MEK:
            case UnitType.WARSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.DROPSHIP:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.VTOL:
            case UnitType.TANK:
                gunnery = crew.getGunnery();
                piloting = crew.getPiloting();
                hasPilot = true;
                break;
            case UnitType.PROTOMEK:
                gunnery = crew.getGunnery();
                break;
            default:
                return;
        }

        BigDecimal skillLevel = BigDecimal.valueOf(gunnery);
        if (hasPilot) {
            skillLevel = skillLevel.add(BigDecimal.valueOf(piloting));
        } else {
            // Assume a piloting equal to Gunnery +1.
            skillLevel = skillLevel.add(BigDecimal.valueOf(gunnery)).add(BigDecimal.ONE);
        }

        incrementSkillRatingCounts(getExperienceLevelName(skillLevel));
        setTotalSkillLevels(getTotalSkillLevels(false).add(skillLevel));
    }

    @Override
    protected BigDecimal getNumberUnits() {
        return new BigDecimal(getTotalCombatUnits());
    }

    private int getTotalCombatUnits() {
        int totalCombatUnits = getMechCount();
        totalCombatUnits += getFighterCount();
        totalCombatUnits += getProtoCount();
        totalCombatUnits += getVeeCount();
        totalCombatUnits += getNumberBaSquads();
        totalCombatUnits += getInfantryUnitCount();
        totalCombatUnits += getDropShipCount();
        totalCombatUnits += getSmallCraftCount();
        return totalCombatUnits;
    }

    private int getTotalForceUnits() {
        int totalGround = 0;
        int totalAero = 0;
        int totalInfantry = 0;
        int totalBattleArmor = 0;

        // Count total units for transport
        getTotalCombatUnits();

        for (Unit u : getCampaign().getHangar().getUnits()) {
            if (u == null) {
                continue;
            }
            if (u.isMothballed() || !u.hasPilot()) {
                continue;
            }

            if ((u.getEntity().getEntityType() &
                 Entity.ETYPE_WARSHIP) == Entity.ETYPE_WARSHIP) {
                totalAero++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_JUMPSHIP) == Entity.ETYPE_JUMPSHIP) {
                //noinspection UnnecessaryContinue
                continue;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_AEROSPACEFIGHTER) == Entity.ETYPE_AEROSPACEFIGHTER) {
                totalAero++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_DROPSHIP) == Entity.ETYPE_DROPSHIP) {
                totalAero++;
            } else if (((u.getEntity().getEntityType() &
                         Entity.ETYPE_MECH) == Entity.ETYPE_MECH)
                       || ((u.getEntity().getEntityType() &
                            Entity.ETYPE_TANK) == Entity.ETYPE_TANK)) {
                totalGround++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_BATTLEARMOR) == Entity.ETYPE_BATTLEARMOR) {
                totalBattleArmor++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_INFANTRY) == Entity.ETYPE_INFANTRY) {
                totalInfantry++;
            }

        }

        return totalGround + totalAero + totalInfantry + totalBattleArmor;
    }

    @Override
    protected BigDecimal calcAverageExperience() {
        int totalCombatUnits = getTotalForceUnits();

        if (totalCombatUnits == 0) {
            return BigDecimal.ZERO;
        }

        return getTotalSkillLevels().divide(BigDecimal.valueOf(totalCombatUnits),
                                            2,RoundingMode.HALF_DOWN);
    }

    private void calcNeededTechs() {
        int protoTeamCount = BigDecimal.valueOf(getProtoCount())
                                       .divide(BigDecimal.valueOf(5), 0,
                                               RoundingMode.HALF_UP)
                                       .intValue();
        setMechTechTeamsNeeded(getMechCount() + protoTeamCount);
        setAeroTechTeamsNeeded(getFighterCount() + getSmallCraftCount());
        int infantryTeamCount = BigDecimal.valueOf(getInfantryCount())
                                          .divide(BigDecimal.valueOf(112),
                                                  0,
                                                  RoundingMode.HALF_UP)
                                          .intValue();
        setMechanicTeamsNeeded(getSuperHeavyVeeCount() + getVeeCount() +
                               infantryTeamCount);
        setBattleArmorTechTeamsNeeded(BigDecimal.valueOf(getBattleArmorCount())
                                                .divide(BigDecimal.valueOf(5),
                                                        0,
                                                        RoundingMode.HALF_UP)
                                                .intValue());
    }

    private void updatePersonnelCounts() {
        setNonAdminPersonnelCount(0);
        technicians = 0;

        // We count all active personnel in the force provided they are not:
        // 1) A Dependent
        // 2) Administrative Personnel: Administrator, or Doctor
        // 3) A Prisoner
        for (Person p : getCampaign().getActivePersonnel()) {
            if (p.getPrimaryRole().isDependent()
                    || p.isAdministrator()
                    || p.isDoctor()
                    || !p.getPrisonerStatus().isFree()) {
                continue;
            }

            if (p.isTech()) {
                technicians++;
            }

            setNonAdminPersonnelCount(getNonAdminPersonnelCount() + 1);
        }

        setNonAdminPersonnelCount(getNonAdminPersonnelCount() + getCampaign().getAstechPool());
    }

    private void calcNeededAdmins() {
        int calculatedAdmin = BigDecimal.valueOf(getNonAdminPersonnelCount())
                .divide(BigDecimal.TEN, 0, RoundingMode.UP)
                .intValue();

        if (getCampaign().getFaction().isMercenary() || getCampaign().getFaction().isPirate()) {
            setAdminsNeeded(calculatedAdmin);
        } else {
            setAdminsNeeded((int) Math.ceil(calculatedAdmin / 2d));
        }
    }

    @Override
    protected void initValues() {
        super.initValues();

        setMechCount(0);
        setProtoCount(0);
        setLightVeeCount(0);
        setBattleArmorCount(0);
        setInfantryCount(0);
        setFighterCount(0);
        setDropShipCount(0);
        setSmallCraftCount(0);
        setJumpShipCount(0);
        setDockingCollarCount(0);
        clearCraftWithoutCrew();

        countUnits();
        calcNeededTechs();
        updatePersonnelCounts();
        calcNeededAdmins();
    }

    @Override
    protected int calculateUnitRatingScore() {
        logger.info("Starting to calculate Unit Rating Score");

        // Step One: derive the campaign's average experience value,
        // based on the experience levels of combat personnel
        logger.info("Evaluating Average Experience Rating");
        int totalScore = getExperienceValue();
        logger.info("Running Total: {}", totalScore);

        // Step Two: derive the commander's value based on skills, personality characteristics, and (eventually) AToW traits
        logger.info("Evaluating Command Rating");
        totalScore += getCommanderValue();
        logger.info("Running Total: {}", totalScore);

        // Step Three: derive the combat record value from success/failed/breached Missions
        logger.info("Evaluating Combat Record");
        totalScore += getCombatRecordValue();
        logger.info("Running Total: {}", totalScore);

        // Step Four:
        logger.info("Evaluating Transportation Rating");
        totalScore += getTransportValue();
        logger.info("Running Total: {}", totalScore);

        // Step Five:
        logger.info("Evaluating Support Rating");
        totalScore += getSupportValue();
        logger.info("Running Total: {}", totalScore);

        // Step Six:
        logger.info("Evaluating Financial Rating");
        totalScore += getFinancialValue();
        logger.info("Running Total: {}", totalScore);

        // Step Seven:
        logger.info("Evaluating Crimes");
        totalScore += getCrimesPenalty();
        logger.info("Running Total: {}", totalScore);

        // Step Eight: Derive any final modifiers.
        // Currently, this is just a modifier for being idle,
        // but more may be added later by CGL
        logger.info("Evaluating Other Modifiers");
        totalScore += getIdleTimeModifier();
        logger.info("Running Total: {}", totalScore);

        // Step Nine: add the manual modifier set in campaign options
        int manualModifier = getCampaign().getCampaignOptions().getManualUnitRatingModifier();
        totalScore += manualModifier;
        logger.info("Applying Manual Modifier: {}", manualModifier);

        // Finish Up
        logger.info("Grand Total: {}", totalScore);

        return totalScore;
    }

    @Override
    public SkillLevel getAverageExperience() {
        return getExperienceLevelName(calcAverageExperience());
    }

    /**
     * Calculates the experience level name based on the given average experience score in the campaign.
     *
     * @param experience the average experience score as a BigDecimal value
     * @return the experience level name as a SkillLevel enum
     */
    @Override
    protected SkillLevel getExperienceLevelName(BigDecimal experience) {
        if (!hasUnits()) {
            return SkillLevel.NONE;
        }

        final BigDecimal eliteThreshold = new BigDecimal("5.00");
        final BigDecimal vetThreshold = new BigDecimal("7.00");
        final BigDecimal regThreshold = new BigDecimal("9.00");

        if (experience.compareTo(regThreshold) > 0) {
            return SkillLevel.GREEN;
        } else if (experience.compareTo(vetThreshold) > 0) {
            return SkillLevel.REGULAR;
        } else if (experience.compareTo(eliteThreshold) > 0) {
            return SkillLevel.VETERAN;
        } else {
            return SkillLevel.ELITE;
        }
    }

    /**
     * Retrieves the experience value based on the average experience level of combat personnel in the campaign.
     *
     * @return The experience value as an integer.
     * @throws IllegalArgumentException If an invalid experience level is provided
     */
    @Override
    public int getExperienceValue() {
        if (!hasUnits()) {
            return 0;
        }
        BigDecimal averageExp = calcAverageExperience();
        SkillLevel level = getExperienceLevelName(averageExp);
        return switch (level) {
            case NONE -> 0;
            case GREEN -> 5;
            case REGULAR -> 10;
            case VETERAN -> 20;
            default -> 40;
        };
    }

    /**
     * @return the value of the commander as an integer
     */
    @Override
    public int getCommanderValue() {
        Person commander = getCommander();

        if (commander == null) {
            logger.info("No commander found. Skipping.");

            return 0;
        }

        int score = getCommanderSkillLevelWithBonus(SkillType.S_LEADER);
        score += getCommanderSkillLevelWithBonus(SkillType.S_TACTICS);
        score += getCommanderSkillLevelWithBonus(SkillType.S_STRATEGY);
        score += getCommanderSkillLevelWithBonus(SkillType.S_NEG);

        // TODO make this a campaign option
        if (getCampaign().getCampaignOptions().isUseRandomPersonalities()) {
            int personalityScore = getPersonalityScore(commander);
            score += personalityScore;
        }

        // ToDo AToW Traits.
        // ToDo MHQ would need  to support: Combat Sense, Connections,
        // ToDo                             Reputation, Wealth, High CHA,
        // ToDo                             Combat Paralysis,
        // ToDo                             Unlucky & Low CHA.

        logger.info("AToW Traits are not currently tracked: Skipping");

        return Math.max(1, score);
    }

    /**
     * Calculates the personality score of a commander based on their intelligence, aggression, ambition, greed, and social characteristics.
     *
     * @param commander the Person object representing the commander for whom the personality score is calculated
     * @return the personality score as an integer
     */
    private static int getPersonalityScore(Person commander) {
        Aggression aggression = commander.getAggression();
        Ambition ambition = commander.getAmbition();
        Greed greed = commander.getGreed();
        Social social = commander.getSocial();
        Intelligence intelligence = commander.getIntelligence();

        int personalityScore = 0;
        if (!intelligence.isAverage()) {
            personalityScore = (Intelligence.parseToInt(intelligence) - 12) / 4;
        }

        // while this uses a lot of repetitions, we can't simplify it further as each characteristic is a different Enum type.
        if (!aggression.isNone()) {
            personalityScore += getPersonalityModifier(aggression.isTraitMajor(), aggression.isTraitPositive());
        }

        if (!ambition.isNone()) {
            personalityScore += getPersonalityModifier(ambition.isTraitMajor(), ambition.isTraitPositive());
        }

        if (!greed.isNone()) {
            personalityScore += getPersonalityModifier(greed.isTraitMajor(), greed.isTraitPositive());
        }

        if (!social.isNone()) {
            personalityScore += getPersonalityModifier(social.isTraitMajor(), social.isTraitPositive());
        }

        return personalityScore;
    }

    /**
     * Calculates the personality modifier based on the given parameters.
     *
     * @param isMajor            a boolean indicating if the trait is major
     * @param isPositive         a boolean indicating if the trait is positive
     * @return the personality modifier as an integer
     */
    private static int getPersonalityModifier(boolean isMajor, boolean isPositive) {
        int modifier = 1;

        if (isMajor) {
            modifier ++;
        }

        return isPositive ? modifier : -modifier;
    }

    @Override
    public String getUnitRating() {
        // Campaign Operations does not use letter-grades.
        return getModifier() + " (" + calculateUnitRatingScore() + ')';
    }

    @Override
    public int getUnitRating(int score) {
        // Campaign Operations does not use letter-grades.
        return 0;
    }

    @Override
    public String getUnitRatingName(int rating) {
        // Campaign Operations does not use letter-grades.
        return "";
    }

    @Override
    public int getTransportValue() {
        if (!hasUnits()) {
            return 0;
        }

        int totalValue = 0;

        TransportCapacityIndicators tci = new TransportCapacityIndicators();
        tci.updateCapacityIndicators(getMechBayCount(), getMechCount());
        tci.updateCapacityIndicators(getProtoBayCount(), getProtoCount());
        tci.updateCapacityIndicators(getBaBayCount(), getBattleArmorCount() / 5); // battle armor bays can hold 5 suits of battle armor per bay
        tci.updateCapacityIndicators(getInfantryBayCount(), calcInfantryPlatoons());

        // Heavy vehicles can use heavy or super heavy vehicle bays and light vehicles can use light, heavy, or super heavy vehicle bays,
        // while fighters can use fighter or small craft bays
        // We put all possible super heavy vehicles into super heavy vehicle bays.
        // If we have some super heavy vehicle bays left over, add them to the heavy vehicle bay count, and then calculate the
        // number of heavy vehicle bays that are still empty. We then add these to the light vehicle bay count
        // The same is done for small craft and fighters, just replace heavy vehicle with small craft and light vehicle with fighters,
        // and remove references to super heavy vehicles
        int excessSuperHeavyVeeBays = Math.max(getSuperHeavyVeeBayCount() - getSuperHeavyVeeCount(), 0);
        int excessHeavyVeeBays = Math.max(getHeavyVeeBayCount() + excessSuperHeavyVeeBays - getHeavyVeeCount(), 0);
        int excessSmallCraftBays = Math.max(getSmallCraftBayCount() - getSmallCraftCount(), 0);

        // We need to subtract any filled bays from the count. This follows the following logic:
        // Assume you have 2 heavy vehicle bays, and 4 light vehicle bays, and are trying to store 1 heavy and 5 light vehicles
        // You have 1 more light vehicle than light vehicle bays to store them in, so you check how many free heavy vehicle bays
        // there are. Finding 1, you can store the light vehicle there, and it doesn't count as having excess
        int excessHeavyVees = Math.max(getHeavyVeeCount() - getHeavyVeeBayCount(), 0);
        int excessLightVees = Math.max(getLightVeeCount() - getLightVeeBayCount(), 0);
        int excessFighters = Math.max(getFighterCount() - getFighterBayCount(), 0);

        int superHeavyVeeBaysFilledByLighterVees = Math.min(excessHeavyVees + excessLightVees, excessSuperHeavyVeeBays);
        int heavyVeeBaysFilledByLights = Math.min(excessLightVees, excessHeavyVeeBays);
        int smallCraftBaysFilledByFighters = Math.min(excessFighters, excessSmallCraftBays);

        tci.updateCapacityIndicators(getSuperHeavyVeeBayCount() - superHeavyVeeBaysFilledByLighterVees, getSuperHeavyVeeCount());
        tci.updateCapacityIndicators(getHeavyVeeBayCount() + excessSuperHeavyVeeBays - heavyVeeBaysFilledByLights, getHeavyVeeCount());
        tci.updateCapacityIndicators(getLightVeeBayCount() + excessHeavyVeeBays, getLightVeeCount());
        tci.updateCapacityIndicators(getSmallCraftBayCount() - smallCraftBaysFilledByFighters, getSmallCraftCount());
        tci.updateCapacityIndicators(getFighterBayCount() + excessSmallCraftBays, getFighterCount());

        //Find the percentage of units that are transported.
        if (tci.hasDoubleCapacity()) {
            totalValue += 10;
        } else if (tci.hasExcessCapacity()) {
            totalValue += 5;
        } else if (tci.hasSufficientCapacity()) {
            totalValue += 0;
        } else {
            totalValue -= 5;
        }

        // next, calculate whether there is enough transport for support personnel
        int supportPersonnelCount = getSupportPersonnelCount(false);
        int personnelTransportCapacity = getPersonnelTransportCapacity();

        if (personnelTransportCapacity >= supportPersonnelCount) {
            totalValue += 3;
        } else if (tci.hasAtLeastSufficientCapacity()) {
            totalValue -= 3;
        }

        if (getDropShipCount() < 1) {
            totalValue -= 5;
        }

        if (getJumpShipCount() > 0) {
            totalValue += 10;
        }

        if (getWarShipCount() > 0) {
            totalValue += 10;

            if (getCampaign().getLocalDate().isAfter(LocalDate.of(2800, 1, 1))) {
                totalValue += 5;
            }
        }

        if ((getDropShipCount() > 0) && (getDockingCollarCount() >= getDropShipCount())) {
            totalValue += 5;
        }

        return totalValue;
    }

    /**
     * Retrieves the count of support personnel in the campaign.
     *
     * @param excludeCivilians a boolean indicating whether to exclude civilian personnel from the count
     * @return the count of support personnel as an integer
     */
    public int getSupportPersonnelCount(boolean excludeCivilians) {
        int count = 0;

        for (Person person : getCampaign().getPersonnel()) {
            if ((!person.getStatus().isDepartedUnit()) && (!person.getStatus().isAbsent())) {
                // we're treating combat personnel without a unit as being non-combat personnel for transport requirements
                if (person.getUnit() == null) {
                    count++;
                    continue;
                }

                if ((person.getPrimaryRole().isSupport(excludeCivilians)) && (person.getSecondaryRole().isSupport(excludeCivilians))) {
                    count++;
                }
            }
        }

        return count;
    }

    int calcTechSupportValue() {
        int totalValue = 0;
        setTotalTechTeams(0);
        setMechTechTeams(0);
        setAeroTechTeams(0);
        setMechanicTeams(0);
        setBaTechTeams(0);
        setGeneralTechTeams(0);

        // How many astech teams do we have?
        int astechTeams = getCampaign().getNumberAstechs() / 6;

        for (Person tech : getCampaign().getTechs()) {
            // If we're out of astech teams, the rest of the techs are
            // unsupported and don't count.
            if (astechTeams <= 0) {
                break;
            }

            if ((tech.getPrimaryRole().isMechTech() || tech.getSecondaryRole().isMechTech())
                    && (tech.getSkill(SkillType.S_TECH_MECH) != null)) {
                setMechTechTeams(getMechTechTeams() + 1);
                astechTeams--;
            } else if ((tech.getPrimaryRole().isAeroTech() || tech.getSecondaryRole().isAeroTech())
                    && (tech.getSkill(SkillType.S_TECH_AERO) != null)) {
                setAeroTechTeams(getAeroTechTeams() + 1);
                astechTeams--;
            } else if ((tech.getPrimaryRole().isMechanic() || tech.getSecondaryRole().isMechanic())
                    && (tech.getSkill(SkillType.S_TECH_MECHANIC) != null)) {
                setMechanicTeams(getMechanicTeams() + 1);
                astechTeams--;
            } else if ((tech.getPrimaryRole().isBATech() || tech.getSecondaryRole().isBATech())
                    && (tech.getSkill(SkillType.S_TECH_BA) != null)) {
                setBaTechTeams(getBaTechTeams() + 1);
                astechTeams--;
            } else {
                setGeneralTechTeams(getGeneralTechTeams() + 1);
                astechTeams--;
            }
        }

        boolean techShortage = getMechTechTeamsNeeded() > getMechTechTeams();
        if (getAeroTechTeamsNeeded() > getAeroTechTeams()) {
            techShortage = true;
        }
        if (getMechanicTeamsNeeded() > getMechanicTeams()) {
            techShortage = true;
        }
        if (getBattleArmorTechTeamsNeeded() > getBaTechTeams()) {
            techShortage = true;
        }

        setTotalTechTeams(getMechTechTeams() + getAeroTechTeams() +
                          getMechanicTeams() + getBaTechTeams() +
                          getGeneralTechTeams());
        int totalTechTeamsNeeded = getMechTechTeamsNeeded() +
                                   getAeroTechTeamsNeeded() +
                                   getMechanicTeamsNeeded() +
                                   getBattleArmorTechTeamsNeeded();
        setSupportPercent(BigDecimal.ZERO);
        if (totalTechTeamsNeeded != 0) {
            setSupportPercent(BigDecimal.valueOf(getTotalTechTeams())
                                        .divide(BigDecimal.valueOf(totalTechTeamsNeeded),
                                                5,
                                                RoundingMode.HALF_UP)
                                        .multiply(HUNDRED));
        }

        if (techShortage) {
            totalValue -= 5;
        } else {
            if (getSupportPercent().compareTo(BigDecimal.valueOf(200)) > 0) {
                totalValue += 15;
            } else if (getSupportPercent().compareTo(BigDecimal.valueOf(175)) > 0) {
                totalValue += 10;
            } else if (getSupportPercent().compareTo(BigDecimal.valueOf(149)) > 0) {
                totalValue += 5;
            }
        }

        return totalValue;
    }

    private int calcAdminSupportValue() {
        int adminsRequired = getAdminsNeeded();
        int totalAdmins = getTotalAdmins();
        boolean hasInsufficientSupport = adminsRequired > totalAdmins;

        logger.info("Admins: {}/{} ({}) [{} combat personnel pulling double duty]",
                totalAdmins,
                adminsRequired,
                (hasInsufficientSupport ? "-5" : "+0"),
                (int) Math.ceil((double) getCampaign().getActiveCombatPersonnel().size() / 4));

        return hasInsufficientSupport ? -5 : 0;
    }

    private int calcLargeCraftSupportValue() {
        for (Unit unit : getCampaign().getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }

            if ((unit.getEntity() instanceof SmallCraft)
                    || (unit.getEntity() instanceof Jumpship)
                    || (unit.getEntity() instanceof Warship)
                    || (unit.getEntity() instanceof Dropship)) {
                if (!unit.isFullyCrewed()) {
                    return -5;
                }
            }
        }

        return 0;
    }

    @Override
    public int getSupportValue() {
        int value = calcTechSupportValue();

        value += calcAdminSupportValue();

        value += calcLargeCraftSupportValue();

        return value;
    }

    @Override
    public BigDecimal getTransportPercent() {
        // Handled under getTransportValue()
        return BigDecimal.ZERO;
    }

    @Override
    public int getFinancialValue() {
        return getCampaign().getFinances().isInDebt() ? -10 : 0;
    }

    // ToDo: MekHQ doesn't currently support recording crimes.
    private int getCrimesPenalty() {
        return 0;
    }

    private int getIdleTimeModifier() {
        if (getCampaign().getCampaignOptions().isUseAtB()) {
            if (getCampaign().hasActiveContract()) {
                return 0;
            }

            LocalDate newestEndDate = getNewestEndDate();
            LocalDate currentDate = getCampaign().getLocalDate();

            Period period = Period.between(currentDate, newestEndDate);

            int inactiveYears = period.getYears();

            return inactiveYears > 0 ? inactiveYears * 5 : 0;
        }

        return 0;
    }

    private LocalDate getNewestEndDate() {
        LocalDate newestEndDate = null;

        for (AtBContract contract : getCampaign().getAtBContracts()) {
            // CamOps explicitly calls out Garrison type contracts as not counting towards activity
            if (contract.getContractType().isGarrisonType()) {
                continue;
            }

            LocalDate endDate = contract.getEndingDate();

            if ((newestEndDate == null) || (endDate.isAfter(newestEndDate))) {
                newestEndDate = contract.getEndingDate();
            }
        }

        // if this is still null, it means no valid contract was found, so we use the campaign start date
        if (newestEndDate == null) {
            newestEndDate = getCampaign().getCampaignStartDate();
        }

        return newestEndDate;
    }

    @Override
    public int getModifier() {
        BigDecimal reputation = new BigDecimal(calculateUnitRatingScore());
        return reputation.divide(BigDecimal.TEN, 0,
                                 RoundingMode.DOWN).intValue();
    }

    private String getExperienceDetails() {
        return String.format("%-" + HEADER_LENGTH + "s %3d", "Experience:", getExperienceValue())
                + '\n'
                + String.format("    %-" + SUBHEADER_LENGTH + "s %3s", "Average Experience:", getAverageExperience())
                + '\n'
                + getSkillLevelCounts()
                .entrySet()
                .stream()
                .map(entry -> String.format("        #%-" + CATEGORY_LENGTH + "s %3d", entry.getKey().toString() + ':', entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String getCommanderDetails() {
        StringBuilder out = new StringBuilder();
        String commanderName = null == getCommander() ? "" : '(' + getCommander().getFullTitle() + ')';
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d %s",
                                 "Commander:", getCommanderValue(),
                                 commanderName));

        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";
        out.append('\n').append(String.format(TEMPLATE, "Leadership:",
                                                getCommanderSkillLevelWithBonus(SkillType.S_LEADER)));
        out.append('\n').append(String.format(TEMPLATE, "Negotiation:",
                                                getCommanderSkillLevelWithBonus(SkillType.S_NEG)));
        out.append('\n').append(String.format(TEMPLATE, "Strategy:",
                                                getCommanderSkillLevelWithBonus(SkillType.S_STRATEGY)));
        out.append('\n').append(String.format(TEMPLATE, "Tactics:",
                                                getCommanderSkillLevelWithBonus(SkillType.S_TACTICS)));

        if ((getCampaign().getCampaignOptions().isUseRandomPersonalities()) && (getCommander() != null)) {
            out.append('\n').append(String.format(TEMPLATE, "Personality:",
                    getCommander() != null ? getPersonalityScore(getCommander()) : 0));
        }

        return out.toString();
    }

    private String getCombatRecordDetails() {
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";

        return String.format("%-" + HEADER_LENGTH + "s %3d", "Combat Record:",
                             getCombatRecordValue()) + '\n' + String.format(TEMPLATE, "Successful Missions:",
                                    getSuccessCount()) + '\n' + String.format(TEMPLATE, "Partial Missions:",
                                    getPartialCount()) + '\n' + String.format(TEMPLATE, "Failed Missions:",
                                    getFailCount()) + '\n' + String.format(TEMPLATE, "Contract Breaches:",
                                    getBreachCount());
    }

    String getTransportationDetails() {
        final String TEMPLATE = "    %-" + CATEGORY_LENGTH +
                                "s %3d needed / %3d available";

        int superHeavyVeeBayCount = getSuperHeavyVeeBayCount();
        int heavyVeeBayCount = getHeavyVeeBayCount();
        int smallCraftBayCount = getSmallCraftBayCount();

        int excessSuperHeavyVeeBays = Math.max(superHeavyVeeBayCount - getSuperHeavyVeeCount(),0);
        int excessHeavyVeeBays = Math.max(heavyVeeBayCount - getHeavyVeeCount(), 0);
        int excessSmallCraftBays = Math.max(smallCraftBayCount - getSmallCraftCount(), 0);

        String out = String.format("%-" + HEADER_LENGTH + "s %3d", "Transportation:", getTransportValue())
                + '\n' + String.format(TEMPLATE, "BattleMech Bays:", getMechCount(), getMechBayCount())
                + '\n' + String.format(TEMPLATE, "Fighter Bays:", getFighterCount(), getFighterBayCount()) + " (plus " + excessSmallCraftBays + " excess Small Craft)"
                + '\n' + String.format(TEMPLATE, "Small Craft Bays:", getSmallCraftCount(), smallCraftBayCount)
                + '\n' + String.format(TEMPLATE, "ProtoMech Bays:", getProtoCount(), getProtoBayCount())
                + '\n' + String.format(TEMPLATE, "Super Heavy Vehicle Bays:", getSuperHeavyVeeCount(), superHeavyVeeBayCount)
                + '\n' + String.format(TEMPLATE, "Heavy Vehicle Bays:", getHeavyVeeCount(), heavyVeeBayCount) + " (plus " + excessSuperHeavyVeeBays + " excess Super Heavy)"
                + '\n' + String.format(TEMPLATE, "Light Vehicle Bays:", getLightVeeCount(), getLightVeeBayCount()) + " (plus " + excessHeavyVeeBays + " excess Heavy and " + excessSuperHeavyVeeBays + " excess Super Heavy)"
                + '\n' + String.format(TEMPLATE, "Battle Armor Bays:", getBattleArmorCount() / 5, getBaBayCount())
                + '\n' + String.format(TEMPLATE, "Infantry Bays:", calcInfantryPlatoons(), getInfantryBayCount())
                + '\n' + String.format(TEMPLATE, "Docking Collars:", getDropShipCount(), getDockingCollarCount())
                + '\n' + String.format(TEMPLATE, "Passenger Capacity:", getSupportPersonnelCount(false), getPersonnelTransportCapacity());

        final String TEMPLATE_2 = "    %-" + CATEGORY_LENGTH + "s %3s";
        out += '\n' + String.format(TEMPLATE_2, "Has JumpShips?", getJumpShipCount() > 0 ? "Yes" : "No");
        out += '\n' + String.format(TEMPLATE_2, "Has WarShips?", getWarShipCount() > 0 ? "Yes" : "No");

        return out;
    }

    private String getSupportDetails() {
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d",
                                 "Support:", getSupportValue()));

        final String TEMPLATE_CAT = "        %-" + CATEGORY_LENGTH +
                                    "s %4d needed / %4d available";
        out.append("\n    Tech Support:");
        out.append('\n').append(String.format(TEMPLATE_CAT, "Mech Techs:",
                                              getMechTechTeamsNeeded(),
                                              getMechTechTeams()));
        out.append("\n            NOTE: ProtoMechs and BattleMechs use same techs.");
        out.append('\n').append(String.format(TEMPLATE_CAT,
                                              "Aero Techs:",
                                              getAeroTechTeamsNeeded(),
                                              getAeroTechTeams()));
        out.append('\n').append(String.format(TEMPLATE_CAT, "Mechanics:",
                                              getMechanicTeamsNeeded(),
                                              getMechanicTeams()));
        out.append("\n            NOTE: Vehicles and Infantry use the same" +
                   " mechanics.");
        out.append('\n').append(String.format(TEMPLATE_CAT, "Battle Armor Techs:",
                                              getBattleArmorTechTeamsNeeded(),
                                              getBaTechTeams()));
        out.append('\n').append(String.format(TEMPLATE_CAT, "Astechs:",
                                              technicians * 6,
                                              getCampaign().getNumberAstechs()));
        out.append('\n').append(String.format("    %-" + (CATEGORY_LENGTH + 4) +
                                              "s %4d needed / %4d available",
                                              "Admin Support:",
                                              getAdminsNeeded(),
                                              getTotalAdmins()));
        out.append("\n    Large Craft Crew:");
        if (getCraftWithoutCrew().isEmpty()) {
            out.append("\n        All fully crewed.");
        } else {
            for (String s : getCraftWithoutCrew()) {
                out.append("\n        ").append(s).append(" short crew.");
            }
        }

        return out.toString();
    }

    private int getTotalAdmins() {
        // CamOps states that combat personnel can pull double-duty as admin personnel.
        // As there is no downside to doing this, we assume it's always being done.
        int combatAdmins = (int) Math.ceil((double) getCampaign().getActiveCombatPersonnel().size() / 4);

        // Doctors also fall under the Administrators category
        return getCampaign().getAdmins().size()
                + getCampaign().getDoctors().size()
                + (combatAdmins / (getCampaign().hasActiveContract() ? 2 : 3));
    }

    @Override
    public String getDetails() {
        final String TEMPLATE = "%-" + HEADER_LENGTH + "s %s";
        initValues();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(TEMPLATE, "Unit Reputation:", calculateUnitRatingScore()));
        sb.append('\n').append("    Method: Campaign Operations\n");
        if (getCampaign().getCampaignOptions().getManualUnitRatingModifier() != 0) {
            sb.append("    Manual Modifier: ")
                    .append(getCampaign().getCampaignOptions().getManualUnitRatingModifier())
                    .append('\n');
        }
        sb.append('\n');
        sb.append(getExperienceDetails()).append("\n\n");
        sb.append(getCommanderDetails()).append("\n\n");
        sb.append(getCombatRecordDetails()).append("\n\n");
        sb.append(getTransportationDetails()).append("\n\n");
        sb.append(getSupportDetails()).append("\n\n");

        sb.append(String.format(TEMPLATE, "Financial", getFinancialValue()));
        sb.append('\n').append(String.format("    %-" + SUBHEADER_LENGTH +
                                             "s %3s",
                                             "In Debt?",
                                             getCampaign().getFinances()
                                                          .isInDebt() ? "Yes" :
                                             "No"));

        sb.append("\n\n")
          .append(String.format(TEMPLATE, "Criminal Activity:", 0))
          .append(" (MHQ does not currently track criminal activity.)");

        sb.append("\n\n")
          .append(String.format(TEMPLATE, "Inactivity Modifier:", getIdleTimeModifier()));

        return new String(sb);
    }

    @Override
    public String getHelpText() {
        return """
                Method: Campaign Ops
                An attempt to match the Campaign Ops method for calculating \
                the Reputation as closely as possible.
                Known differences include the following:
                + Command: Does not incorporate any positive or negative \
                traits from AToW or BRPG3.\
                + Criminal Activity: MHQ does not currently track criminal \
                activity.""";
    }

    private void setNonAdminPersonnelCount(int nonAdminPersonnelCount) {
        this.nonAdminPersonnelCount = nonAdminPersonnelCount;
    }

    private void setMechTechTeamsNeeded(int mechTechTeamsNeeded) {
        this.mechTechTeamsNeeded = mechTechTeamsNeeded;
    }

    private void setMechanicTeamsNeeded(int mechanicTeamsNeeded) {
        this.mechanicTeamsNeeded = mechanicTeamsNeeded;
    }

    private void setBattleArmorTechTeamsNeeded(int battleArmorTechTeamsNeeded) {
        this.battleArmorTechTeamsNeeded = battleArmorTechTeamsNeeded;
    }

    private void setAeroTechTeamsNeeded(int aeroTechTeamsNeeded) {
        this.aeroTechTeamsNeeded = aeroTechTeamsNeeded;
    }

    private void setAdminsNeeded(int adminsNeeded) {
        this.adminsNeeded = adminsNeeded;
    }

    private int getTotalTechTeams() {
        return totalTechTeams;
    }

    private void setTotalTechTeams(int totalTechTeams) {
        this.totalTechTeams = totalTechTeams;
    }

    private int getMechTechTeams() {
        return mechTechTeams;
    }

    private void setMechTechTeams(int mechTechTeams) {
        this.mechTechTeams = mechTechTeams;
    }

    private int getAeroTechTeams() {
        return aeroTechTeams;
    }

    private void setAeroTechTeams(int aeroTechTeams) {
        this.aeroTechTeams = aeroTechTeams;
    }

    private int getMechanicTeams() {
        return mechanicTeams;
    }

    private void setMechanicTeams(int mechanicTeams) {
        this.mechanicTeams = mechanicTeams;
    }

    private int getBaTechTeams() {
        return baTechTeams;
    }

    private void setBaTechTeams(int baTechTeams) {
        this.baTechTeams = baTechTeams;
    }

    private int getGeneralTechTeams() {
        return generalTechTeams;
    }

    private void setGeneralTechTeams(int generalTechTeams) {
        this.generalTechTeams = generalTechTeams;
    }

    private List<String> getCraftWithoutCrew() {
        return new ArrayList<>(craftWithoutCrew);
    }

    private void addCraftWithoutCrew(Unit u) {
        craftWithoutCrew.add(u.getName());
    }

    private void clearCraftWithoutCrew() {
        craftWithoutCrew.clear();
    }

    /**
     * Data structure that holds transport capacity indicators
     * @author NickAragua
     *
     */
    private static class TransportCapacityIndicators {
        private boolean sufficientCapacity = true;
        private boolean excessCapacity = true;
        private boolean doubleCapacity = true;

        public boolean hasSufficientCapacity() {
            return sufficientCapacity;
        }

        public boolean hasExcessCapacity() {
            return excessCapacity;
        }

        public boolean hasDoubleCapacity() {
            return doubleCapacity;
        }

        public boolean hasAtLeastSufficientCapacity() {
            return (sufficientCapacity || excessCapacity || doubleCapacity);
        }

        /**
         * Updates the transport capacity indicators
         * @param bayCount The number of available bays
         * @param unitCount The number of units using the given type of bay
         */
        public void updateCapacityIndicators(int bayCount, int unitCount) {
            // per CamOps, if we don't have any of a given type of unit but have bays for it
            // the force doesn't count as having excess capacity for that unit type
            if (unitCount == 0) {
                return;
            }

            // examples:
            //  1 infantry platoon, 1 bay = sufficient capacity
            //  1 infantry platoon, 1 tank, 1 infantry bay, 1 tank bay = excess capacity
            //  1 infantry platoon, 1 tank, 2 infantry bay, 1 tank bay = double capacity
            //  1 infantry platoon, no infantry bays, 1 tank, 1 tank bay = insufficient capacity

            // we have enough capacity if there are as many or more bays than units
            sufficientCapacity &= (bayCount >= unitCount);

            // we have excess capacity if there are more bays than units for at least one unit type AND
            // we have sufficient capacity for everything else
            excessCapacity &= ((bayCount > unitCount) || sufficientCapacity);

            // we have double capacity if there are more than twice as many bays as units for every unit type
            doubleCapacity &= (bayCount > (unitCount * 2));
        }
    }
}
