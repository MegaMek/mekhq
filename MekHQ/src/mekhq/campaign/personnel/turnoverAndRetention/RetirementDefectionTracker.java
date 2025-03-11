/*
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.campaign.personnel.turnoverAndRetention;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static mekhq.campaign.personnel.Person.getLoyaltyName;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;

/**
 * @author Neoancient
 *
 *         Against the Bot
 *         Utility class that handles Employee Turnover rolls and final payments
 *         to personnel who retire/defect/get sacked and families of those
 *         killed
 *         in battle.
 */
public class RetirementDefectionTracker {
    private static final MMLogger logger = MMLogger.create(RetirementDefectionTracker.class);

    /*
     * In case the dialog is closed after making the retirement rolls
     * and determining payouts, but before the retirees have been paid,
     * we store those results to avoid making the rolls again.
     */
    final private Set<Integer> rollRequired;
    final private Map<Integer, HashSet<UUID>> unresolvedPersonnel;
    final private Map<UUID, Payout> payouts;
    private LocalDate lastRetirementRoll;

    private static Person asfCommander;
    private static Integer asfCommanderModifier;
    private static Person vehicleCrewCommander;
    private static Integer vehicleCrewCommanderModifier;
    private static Person infantryCommander;
    private static Integer infantryCommanderModifier;
    private static Person navalCommander;
    private static Integer navalCommanderModifier;
    private static Person techCommander;
    private static Integer techCommanderModifier;
    private static Person medicalCommander;
    private static Integer medicalCommanderModifier;
    private static Person administrationCommander;
    private static Integer administrationCommanderModifier;
    private static Person mekWarriorCommander;
    private static Integer mekWarriorCommanderModifier;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionTracker");

    public RetirementDefectionTracker() {
        rollRequired = new HashSet<>();
        unresolvedPersonnel = new HashMap<>();
        payouts = new HashMap<>();
        lastRetirementRoll = LocalDate.now();
    }

    /**
     * Computes the target for retirement rolls for all eligible personnel; this
     * includes
     * all active personnel who aren’t dependents, prisoners, or bondsmen.
     *
     * @param mission  The contract that is being resolved; if the retirement roll
     *                 is not due to
     *                 contract resolutions (e.g., &gt; 12 months since last roll),
     *                 this can be null.
     * @param campaign The campaign to calculate target numbers for
     * @return A map with person ids as key and calculated target roll as value.
     */
    public Map<UUID, TargetRoll> getTargetNumbers(final @Nullable Mission mission, final Campaign campaign) {
        final Map<UUID, TargetRoll> targets = new HashMap<>();

        if (null != mission) {
            rollRequired.add(mission.getId());
        }

        if (campaign.getCampaignOptions().isUseManagementSkill()) {
            getManagementSkillValues(campaign);
        }

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isCivilian()) || (!person.getPrisonerStatus().isFree())
                    || (person.isDeployed())) {
                continue;
            }

            if (person.isFounder()) {
                if (person.getAge(campaign.getLocalDate()) < 50) {
                    if (!campaign.getCampaignOptions().isUseRandomFounderTurnover()) {
                        continue;
                    }
                } else if (!campaign.getCampaignOptions().isUseFounderRetirement()) {
                    continue;
                }
            }

            if (campaign.getCampaignOptions().isUseSubContractSoldiers()) {
                if ((person.getUnit() != null) && (person.getUnit().usesSoldiers())
                        && (!person.getUnit().isCommander(person))) {
                    continue;
                }
            }

            TargetRoll targetNumber = new TargetRoll(getBaseTargetNumber(campaign, person),
                    resources.getString("base.text"));

            // Founder Modifier
            if (person.isFounder()) {
                targetNumber.addModifier(-2, resources.getString("founder.text"));
            }

            // Service Contract
            if (isBreakingContract(person, campaign.getLocalDate(),
                    campaign.getCampaignOptions().getServiceContractDuration())) {
                targetNumber.addModifier(-campaign.getCampaignOptions().getServiceContractModifier(),
                        resources.getString("contract.text"));
            }

            // Desirability modifier
            if ((campaign.getCampaignOptions().isUseSkillModifiers())
                    && (person.getAge(campaign.getLocalDate()) < 50)) {
                targetNumber.addModifier(person.getExperienceLevel(campaign, false) - 2,
                        resources.getString("desirability.text"));
            }

            // Recent Promotion Modifier
            if (campaign.getCampaignOptions().isUseSkillModifiers()) {
                LocalDate today = campaign.getLocalDate();
                LocalDate lastPromotionDate = person.getLastRankChangeDate();

                if (lastPromotionDate != null) {
                    long monthsBetween = ChronoUnit.MONTHS.between(lastPromotionDate, today);

                    if (monthsBetween <= 6) {
                        targetNumber.addModifier(-1, resources.getString("recentPromotion.text"));
                    }
                }
            }

            // Fatigue modifier
            if ((campaign.getCampaignOptions().isUseFatigue())
                    && (campaign.getCampaignOptions().isUseFatigueModifiers())) {
                int fatigueModifier = MathUtility.clamp(((person.getFatigue() - 1) / 4) - 1, 0, 3);

                if (fatigueModifier > 0) {
                    targetNumber.addModifier(fatigueModifier, resources.getString("fatigue.text"));
                }
            }

            // Administrative Strain Modifiers
            if (campaign.getCampaignOptions().isUseAdministrativeStrain()) {
                int administrativeStrainModifier = getAdministrativeStrainModifier(campaign);

                if (administrativeStrainModifier > 0) {
                    targetNumber.addModifier(administrativeStrainModifier,
                            resources.getString("administrativeStrain.text"));
                }
            }

            // Management Skill Modifier
            if (campaign.getCampaignOptions().isUseManagementSkill()) {
                int modifier = campaign.getCampaignOptions().getManagementSkillPenalty();

                if (campaign.getCampaignOptions().isUseCommanderLeadershipOnly()) {
                    if (campaign.getFlaggedCommander().hasSkill((SkillType.S_LEADER))) {
                        modifier -= campaign.getFlaggedCommander().getSkill(SkillType.S_LEADER).getFinalSkillValue();
                    }
                } else {
                    modifier -= getManagementSkillModifier(person);
                }

                targetNumber.addModifier(modifier, resources.getString("managementSkill.text"));
            }

            // Shares Modifiers
            if (campaign.getCampaignOptions().isUseShareSystem()) {
                // If this retirement roll is not being made at the end of a contract (e.g. >12
                // months since last roll),
                // the share percentage should still apply.
                // In the case of multiple active contracts, pick the one with the best
                // percentage.

                AtBContract contract;

                try {
                    contract = (AtBContract) mission;
                } catch (Exception e) {
                    contract = null;
                }

                if (contract == null) {
                    List<AtBContract> atbContracts = campaign.getActiveAtBContracts();

                    if (!atbContracts.isEmpty()) {
                        for (AtBContract atbContract : atbContracts) {
                            if ((contract == null) || (contract.getSharesPercent() > atbContract.getSharesPercent())) {
                                contract = atbContract;
                            }
                        }
                    }
                }

                if (contract != null) {
                    targetNumber.addModifier(-Math.max(0, ((contract.getSharesPercent() / 10) - 2)),
                            resources.getString("shares.text"));
                }
            }

            // Unit Rating modifier
            if (campaign.getCampaignOptions().isUseUnitRatingModifiers()) {
                int unitRatingModifier = getUnitRatingModifier(campaign);
                targetNumber.addModifier(unitRatingModifier, resources.getString("unitRating.text"));
            }

            // Active Mission modifier
            if (campaign.getCampaignOptions().isUseHostileTerritoryModifiers()) {
                if (isHostileTerritory(campaign)) {
                    targetNumber.addModifier(-2, resources.getString("hostileTerritory.text"));
                }
            }

            // Mission completion status modifiers
            if ((mission != null) && (campaign.getCampaignOptions().isUseMissionStatusModifiers())) {
                if (mission.getStatus().isSuccess()) {
                    targetNumber.addModifier(-1, resources.getString("missionSuccess.text"));
                } else if (mission.getStatus().isFailed()) {
                    targetNumber.addModifier(1, resources.getString("missionFailure.text"));
                } else if (mission.getStatus().isBreach()) {
                    targetNumber.addModifier(2, resources.getString("missionBreach.text"));
                }
            }

            // Loyalty
            if ((campaign.getCampaignOptions().isUseLoyaltyModifiers())
                    && (!campaign.getCampaignOptions().isUseHideLoyalty())) {

                int loyaltyScore = person.getLoyalty();

                if (person.isCommander()) {
                    loyaltyScore += 2;
                }

                int loyaltyModifier = person.getLoyaltyModifier(loyaltyScore);

                if (loyaltyModifier != 0) {
                    targetNumber.addModifier(loyaltyModifier, getLoyaltyName(loyaltyModifier));
                }
            }

            // Faction Modifiers
            if (campaign.getCampaignOptions().isUseFactionModifiers()) {
                Faction campaignFaction = campaign.getFaction();

                // campaign faction modifiers
                if (campaignFaction.isPirate()) {
                    targetNumber.addModifier(1, resources.getString("factionPirateCompany.text"));
                } else if (campaignFaction.isComStarOrWoB()) {
                    if (person.getOriginFaction().isComStarOrWoB()) {
                        targetNumber.addModifier(-2, resources.getString("factionComStarOrWob.text"));
                    }
                } else if ((!campaignFaction.isClan()) && (!campaignFaction.isMercenary())) {
                    if (campaignFaction.equals(person.getOriginFaction())) {
                        targetNumber.addModifier(-1, resources.getString("factionLoyalty.text"));
                    }
                }

                // origin faction modifiers
                if ((!campaignFaction.isPirate()) && (person.getOriginFaction().isPirate())) {
                    targetNumber.addModifier(1, resources.getString("factionPirate.text"));
                }

                if (person.getOriginFaction().isMercenary()) {
                    targetNumber.addModifier(1, resources.getString("factionMercenary.text"));
                }

                if (person.getOriginFaction().isClan()) {
                    targetNumber.addModifier(-2, resources.getString("factionClan.text"));
                }

                // wartime modifier
                if (FactionHints.defaultFactionHints().isAtWarWith(campaign.getFaction(), person.getOriginFaction(),
                        campaign.getLocalDate())) {
                    targetNumber.addModifier(4, resources.getString("factionEnemy.text"));
                }
            }

            // Age Modifiers
            if (campaign.getCampaignOptions().isUseAgeModifiers()) {
                int ageMod = getAgeMod(person.getAge(campaign.getLocalDate()));

                if (ageMod < 0) {
                    targetNumber.addModifier(ageMod, resources.getString("ageYoung.text"));
                } else if ((ageMod > 0) && (!isBreakingContract(person, campaign.getLocalDate(),
                        campaign.getCampaignOptions().getServiceContractDuration()))) {
                    targetNumber.addModifier(ageMod, resources.getString("ageRetirement.text"));
                }
            }

            // Family Modifier
            if (campaign.getCampaignOptions().isUseFamilyModifiers()) {
                Person spouse = person.getGenealogy().getSpouse();
                List<Person> children = person.getGenealogy().getChildren();

                int modifier = 0;

                // if 'person' is married to a non-civilian, apply a -1 modifier
                if ((spouse != null)
                        && (!spouse.getPrimaryRole().isCivilian())
                        && (!spouse.getStatus().isDepartedUnit())) {
                    modifier--;
                }

                // if 'person' has any non-civilian children in the unit, apply a -1 modifier
                if ((!children.isEmpty()) && (spouse == null)) {
                    if (children.stream()
                            .filter(child -> !child.isChild(campaign.getLocalDate()))
                            .anyMatch(child -> (!child.isChild(campaign.getLocalDate()))
                                    && (!child.getPrimaryRole().isCivilian())
                                    && (!child.getStatus().isDepartedUnit()))) {
                        modifier--;
                    }
                }

                if (modifier != 0) {
                    targetNumber.addModifier(modifier, resources.getString("family.text"));
                }
            }

            // Injury Modifiers
            int injuryMod = (int) person.getInjuries()
                    .stream()
                    .filter(Injury::isPermanent).count();

            if (injuryMod > 0) {
                targetNumber.addModifier(injuryMod, resources.getString("injuries.text"));
            }

            // Officer Modifiers
            if (person.getRank().isOfficer()) {
                targetNumber.addModifier(-1, resources.getString("officer.text"));
            } else {
                for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i
                        .hasMoreElements();) {
                    IOption ability = i.nextElement();
                    if (ability.booleanValue()) {
                        if (ability.getName().equals("tactical_genius")) {
                            targetNumber.addModifier(1, resources.getString("tacticalGenius.text"));
                            break;
                        }
                    }
                }
            }

            targets.put(person.getId(), targetNumber);
        }

        // we trim personnel so that anyone who has an impossible to fail TN doesn't
        // appear on the table
        targets.entrySet().removeIf(entry -> entry.getValue().getValue() <= 2);

        return targets;
    }

    /**
     * Determines whether the campaign is in the middle of a contract in hostile
     * territory.
     * If AtB is disabled, this method only checks whether there is an active
     * contract.
     *
     * @param campaign the campaign to check for hostile territory modifier
     * @return true if the campaign is in hostile territory modifier
     *         or (if AtB is disabled) whether the campaign is in an active
     *         contract, false otherwise
     */
    private boolean isHostileTerritory(Campaign campaign) {
        List<AtBContractType> defensiveContracts = Arrays.asList(
                AtBContractType.GARRISON_DUTY,
                AtBContractType.CADRE_DUTY,
                AtBContractType.SECURITY_DUTY,
                AtBContractType.RIOT_DUTY);

        List<Contract> activeContracts = campaign.getActiveContracts();

        if (!activeContracts.isEmpty()) {
            if (campaign.getCampaignOptions().isUseAtB()) {
                Optional<Contract> defensiveContract = activeContracts.stream()
                        .filter(contract -> contract instanceof AtBContract)
                        .filter(atBContract -> !defensiveContracts
                                .contains(((AtBContract) atBContract).getContractType()))
                        .findFirst();

                return defensiveContract.isPresent();
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the management skill modifier for a person
     *
     * @param person the individual we're fetching the modifier for
     * @return the management skill modifier
     */
    private static int getManagementSkillModifier(Person person) {
        if ((person.getPrimaryRole().isCivilian()) || (!person.getPrisonerStatus().isFree())) {
            return 0;
        }

        if (person.getSecondaryRole() == PersonnelRole.NONE) {
            return getCommanderManagementSkill(person.getPrimaryRole());
        } else {
            return ((getCommanderManagementSkill(person.getPrimaryRole())
                    + getCommanderManagementSkill(person.getSecondaryRole())) / 2);
        }
    }

    /**
     * Returns the management skill modifier for a commander based on the given
     * personnel role.
     *
     * @param role the personnel role of the person we're fetching the modifier for
     * @return the management skill modifier for the commander
     */
    private static int getCommanderManagementSkill(PersonnelRole role) {
        return switch (Profession.getProfessionFromPersonnelRole(role)) {
            case AEROSPACE -> asfCommanderModifier;
            case VEHICLE -> vehicleCrewCommanderModifier;
            case INFANTRY -> infantryCommanderModifier;
            case NAVAL -> navalCommanderModifier;
            case TECH -> techCommanderModifier;
            case MEDICAL -> medicalCommanderModifier;
            case ADMINISTRATOR -> administrationCommanderModifier;
            case MEKWARRIOR -> mekWarriorCommanderModifier;
            case CIVILIAN -> 0;
        };
    }

    /**
     * This method calculates the management skill values for the different
     * commanding officers.
     * Each commander's management skill value is calculated based on their role and
     * rank within the campaign.
     * The management skill modifier is calculated by adding the base modifier
     * (retrieved from campaign options) and the commander's individual leadership
     * skill.
     * If no suitable commander is found for a particular role,
     * the management skill modifier for that role remains the same as the base
     * modifier.
     *
     * @param campaign The Campaign object for which to calculate the management
     *                 skill values.
     */
    private void getManagementSkillValues(Campaign campaign) {
        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isCivilian())
                    || (person.getPrisonerStatus().isPrisoner())
                    || (person.getPrisonerStatus().isPrisonerDefector())) {
                continue;
            }

            switch (Profession.getProfessionFromPersonnelRole(person.getPrimaryRole())) {
                case AEROSPACE:
                    if (person.outRanksUsingSkillTiebreaker(campaign, asfCommander)) {
                        asfCommander = person;
                        asfCommanderModifier = getIndividualCommanderLeadership(asfCommander);
                    }
                    break;
                case VEHICLE:
                    if (person.outRanksUsingSkillTiebreaker(campaign, vehicleCrewCommander)) {
                        vehicleCrewCommander = person;
                        vehicleCrewCommanderModifier = getIndividualCommanderLeadership(vehicleCrewCommander);
                    }
                    break;
                case INFANTRY:
                    if (person.outRanksUsingSkillTiebreaker(campaign, infantryCommander)) {
                        infantryCommander = person;
                        infantryCommanderModifier = getIndividualCommanderLeadership(infantryCommander);
                    }
                    break;
                case NAVAL:
                    if (person.outRanksUsingSkillTiebreaker(campaign, navalCommander)) {
                        navalCommander = person;
                        navalCommanderModifier = getIndividualCommanderLeadership(navalCommander);
                    }
                    break;
                case TECH:
                    if (person.outRanksUsingSkillTiebreaker(campaign, techCommander)) {
                        techCommander = person;
                        techCommanderModifier = getIndividualCommanderLeadership(techCommander);
                    }
                    break;
                case MEDICAL:
                    if (person.outRanksUsingSkillTiebreaker(campaign, medicalCommander)) {
                        medicalCommander = person;
                        medicalCommanderModifier = getIndividualCommanderLeadership(medicalCommander);
                    }
                    break;
                case ADMINISTRATOR:
                    if (person.outRanksUsingSkillTiebreaker(campaign, administrationCommander)) {
                        administrationCommander = person;
                        administrationCommanderModifier = getIndividualCommanderLeadership(administrationCommander);
                    }
                    break;
                case MEKWARRIOR:
                    if (person.outRanksUsingSkillTiebreaker(campaign, mekWarriorCommander)) {
                        mekWarriorCommander = person;
                        mekWarriorCommanderModifier = getIndividualCommanderLeadership(mekWarriorCommander);
                    }
                    break;
                case CIVILIAN:
                    break;
            }
        }

        for (Profession profession : Profession.values()) {
            switch (profession) {
                case AEROSPACE:
                    if (asfCommander == null) {
                        asfCommanderModifier = 0;
                    }
                    break;
                case VEHICLE:
                    if (vehicleCrewCommander == null) {
                        vehicleCrewCommanderModifier = 0;
                    }
                    break;
                case INFANTRY:
                    if (infantryCommander == null) {
                        infantryCommanderModifier = 0;
                    }
                    break;
                case NAVAL:
                    if (navalCommander == null) {
                        navalCommanderModifier = 0;
                    }
                    break;
                case TECH:
                    if (techCommander == null) {
                        techCommanderModifier = 0;
                    }
                    break;
                case MEDICAL:
                    if (medicalCommander == null) {
                        medicalCommanderModifier = 0;
                    }
                    break;
                case ADMINISTRATOR:
                    if (administrationCommander == null) {
                        administrationCommanderModifier = 0;
                    }
                    break;
                case MEKWARRIOR:
                    if (mekWarriorCommander == null) {
                        mekWarriorCommanderModifier = 0;
                    }
                    break;
                case CIVILIAN:
                    break;
            }
        }
    }

    /**
     * Calculates the individual commander Leadership skill based on the provided
     * commander.
     *
     * @param commander the commander for which the skill is being calculated
     * @return the Leadership skill
     */
    private static int getIndividualCommanderLeadership(Person commander) {
        if (commander.hasSkill(SkillType.S_LEADER)) {
            return commander.getSkill(SkillType.S_LEADER).getFinalSkillValue();
        } else {
            return 0;
        }
    }

    /**
     * This method calculates the combatant strain modifier based on the active
     * personnel assigned to units.
     *
     * @param campaign the campaign for which to calculate the strain modifier
     * @return the strain modifier
     */
    public static int getAdministrativeStrainModifier(Campaign campaign) {
        int personnel = getAdministrativeStrain(campaign);

        int maximumStrain = campaign.getCampaignOptions().getAdministrativeCapacity()
                * getCombinedSkillValues(campaign, SkillType.S_ADMIN);

        if (maximumStrain != 0) {
            return personnel / maximumStrain;
        } else {
            return personnel;
        }
    }

    /**
     * Calculates the administrative strain for a given campaign.
     *
     * @param campaign the campaign for which to calculate the administrative strain
     * @return the total administrative strain of the campaign
     */
    public static int getAdministrativeStrain(Campaign campaign) {
        int personnel = 0;
        int proto = 0;
        int multiCrew = 0;
        int other = 0;

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isCivilian()) || (person.getPrisonerStatus().isPrisoner())
                    || (person.getPrisonerStatus().isPrisonerDefector())) {
                other++;
            } else if (person.getUnit() != null) {
                if (person.getUnit().isCommander(person)) {
                    if (person.getUnit().getEntity().isProtoMek()) {
                        proto++;
                    } else {
                        multiCrew++;
                    }
                }
            } else {
                if ((person.getPrimaryRole().isAstech()) && person.getSecondaryRole().isNone()) {
                    continue;
                } else if ((person.getPrimaryRole().isMedic()) && person.getSecondaryRole().isNone()) {
                    continue;
                } else if ((person.getPrimaryRole().isMedic()) && person.getSecondaryRole().isAstech()) {
                    continue;
                } else if ((person.getPrimaryRole().isAstech()) && person.getSecondaryRole().isMedic()) {
                    continue;
                }

                personnel++;
            }
        }

        personnel += proto / 5;
        personnel += multiCrew / campaign.getCampaignOptions().getMultiCrewStrainDivider();
        personnel += other / (campaign.getCampaignOptions().getMultiCrewStrainDivider() * 2);

        return personnel;
    }

    /**
     * Calculates the combined skill values of active Admin/HR personnel.
     *
     * @param campaign the campaign for which to calculate the combined skill values
     * @return the combined skill values of active Admin/HR personnel in the
     *         campaign
     */
    public static int getCombinedSkillValues(Campaign campaign, String skillType) {
        int combinedSkillValues = 0;

        for (Person person : campaign.getActivePersonnel()) {
            if ((!person.getPrisonerStatus().isPrisoner()) || (!person.getPrisonerStatus().isPrisonerDefector())) {
                if (person.getPrimaryRole().isAdministratorHR()) {
                    if (person.hasSkill(skillType)) {
                        combinedSkillValues += person.getSkill(skillType).getLevel();
                        combinedSkillValues += person.getSkill(skillType).getBonus();
                    }
                } else if (person.getSecondaryRole().isAdministratorHR()) {
                    if (person.hasSkill(skillType)) {
                        combinedSkillValues += person.getSkill(skillType).getLevel();
                        combinedSkillValues += person.getSkill(skillType).getBonus();
                    }
                }
            }
        }
        return combinedSkillValues;
    }

    /**
     * This method calculates the base target number.
     *
     * @param campaign the campaign for which the base target number is calculated
     * @return the base target number
     */
    private int getBaseTargetNumber(Campaign campaign, Person person) {
        if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseHideLoyalty())) {
            int loyaltyScore = person.getLoyalty();

            if (person.isCommander()) {
                loyaltyScore += 2;
            }

            int loyaltyModifier = person.getLoyaltyModifier(loyaltyScore);

            return campaign.getCampaignOptions().getTurnoverFixedTargetNumber() + loyaltyModifier;
        } else {
            return campaign.getCampaignOptions().getTurnoverFixedTargetNumber();
        }
    }

    /**
     * Returns the unit rating modifier for the campaign.
     *
     * @param campaign the campaign from which to derive the unit rating modifier
     * @return the unit rating modifier
     */
    private static int getUnitRatingModifier(Campaign campaign) {
        int unitRating = 0;

        if (campaign.getAtBUnitRatingMod() < 1) {
            unitRating = 2;
        } else if (campaign.getAtBUnitRatingMod() == 1) {
            unitRating = 1;
        } else if (campaign.getAtBUnitRatingMod() > 3) {
            unitRating = -1;
        }
        return unitRating;
    }

    /**
     * @param campaign the campaign to get share values for
     * @return The value of each share in C-bills
     */
    public static Money getShareValue(Campaign campaign) {
        if (!campaign.getCampaignOptions().isUseShareSystem()) {
            return Money.zero();
        }

        Money profits = campaign.getFinances().getProfits();

        int totalShares = campaign.getActivePersonnel()
                .stream()
                .mapToInt(p -> p.getNumShares(campaign, campaign.getCampaignOptions().isSharesForAll()))
                .sum();

        if (totalShares <= 0) {
            return Money.zero();
        }

        return profits.dividedBy(totalShares);
    }

    /**
     * @param age the age of the employee
     * @return the age-based modifier
     */
    private static int getAgeMod(int age) {
        int ageMod = 0;

        if (age <= 20) {
            ageMod = -1;
        } else if ((age >= 50) && (age < 65)) {
            ageMod = 3;
        } else if ((age >= 65) && (age < 75)) {
            ageMod = 4;
        } else if ((age >= 75) && (age < 85)) {
            ageMod = 5;
        } else if ((age >= 85) && (age < 95)) {
            ageMod = 6;
        } else if ((age >= 95) && (age < 105)) {
            ageMod = 7;
        } else if (age >= 105) {
            ageMod = 8;
        }

        return ageMod;
    }

    /**
     * Makes rolls for Employee Turnover based on previously calculated target
     * rolls,
     * and tracks all retirees in the unresolvedPersonnel hash in case the dialog
     * is closed before payments are resolved, to avoid re-rolling the results.
     *
     * @param mission    Nullable mission value
     * @param targets    The hash previously generated by getTargetNumbers.
     * @param shareValue The value of each share in the unit; if not using the share
     *                   system, this is zero.
     * @param campaign   the current campaign
     */
    public void rollRetirement(final @Nullable Mission mission, final Map<UUID, TargetRoll> targets,
            final Money shareValue, final Campaign campaign) {
        if ((mission != null) && !unresolvedPersonnel.containsKey(mission.getId())) {
            unresolvedPersonnel.put(mission.getId(), new HashSet<>());
        }

        for (UUID id : targets.keySet()) {
            // it's possible the person has already been added by soldier or marriage
            // special handlers
            if (payouts.containsKey(id)) {
                continue;
            }

            if (Compute.d6(2) < targets.get(id).getValue()) {
                if (mission != null) {
                    unresolvedPersonnel.get(mission.getId()).add(id);
                }

                Person person = campaign.getPerson(id);

                // if the retiree is the commander of an infantry platoon, all non-founders in
                // the platoon follow them into retirement
                if (campaign.getCampaignOptions().isUseSubContractSoldiers()) {
                    if ((person.getUnit() != null) && (person.getUnit().usesSoldiers())
                            && (person.getUnit().isCommander(person))) {
                        for (Person soldier : person.getUnit().getAllInfantry()) {
                            if ((!soldier.isFounder())
                                    || (campaign.getCampaignOptions().isUseRandomFounderTurnover())) {
                                // this shouldn't be an issue, but we include it here as insurance
                                if (!payouts.containsKey(id)) {
                                    payouts.put(soldier.getId(),
                                            new Payout(campaign, campaign.getPerson(soldier.getId()),
                                                    shareValue, false, false,
                                                    campaign.getCampaignOptions().isSharesForAll()));
                                }
                            }
                        }

                        continue;
                    }
                }

                payouts.put(id, new Payout(campaign, campaign.getPerson(id),
                        shareValue, false, false, campaign.getCampaignOptions().isSharesForAll()));
            }
        }

        if (mission != null) {
            rollRequired.remove(mission.getId());
        }

        lastRetirementRoll = campaign.getLocalDate();
    }

    public LocalDate getLastRetirementRoll() {
        return lastRetirementRoll;
    }

    public void setLastRetirementRoll(LocalDate lastRetirementRoll) {
        this.lastRetirementRoll = lastRetirementRoll;
    }

    /**
     * Removes a person from a campaign and updates relevant data.
     *
     * @param person   The person to be removed from the campaign.
     * @param killed   Indicates whether the person was killed.
     * @param sacked   Indicates whether the person was sacked.
     * @param campaign The campaign from which to remove the person.
     * @param contract The contract associated with the event trigger, if
     *                 applicable.
     * @return True if the person was successfully removed from the campaign, false
     *         otherwise.
     */
    public boolean removeFromCampaign(Person person, boolean killed, boolean sacked, Campaign campaign,
            Mission contract) {
        if (!person.getPrisonerStatus().isFree()) {
            return false;
        }

        payouts.put(person.getId(), new Payout(campaign, person, getShareValue(campaign),
                killed, sacked, campaign.getCampaignOptions().isSharesForAll()));

        if (null != contract) {
            unresolvedPersonnel.computeIfAbsent(contract.getId(), k -> new HashSet<>());
            unresolvedPersonnel.get(contract.getId()).add(person.getId());
        }

        return true;
    }

    public void removePayout(Person person) {
        payouts.remove(person.getId());
    }

    /**
     * Clears out an individual entirely from this tracker.
     *
     * @param person The person to remove
     */
    public void removePerson(Person person) {
        payouts.remove(person.getId());

        for (int contractID : unresolvedPersonnel.keySet()) {
            unresolvedPersonnel.get(contractID).remove(person.getId());
        }
    }

    /**
     * Worker function that clears out any orphan Employee Turnover records
     */
    public void cleanupOrphans(Campaign campaign) {
        payouts.keySet().removeIf(personID -> campaign.getPerson(personID) == null);

        for (int contractID : unresolvedPersonnel.keySet()) {
            unresolvedPersonnel.get(contractID).removeIf(personID -> campaign.getPerson(personID) == null);
        }
    }

    public boolean isOutstanding(int id) {
        return unresolvedPersonnel.containsKey(id);
    }

    /**
     * Called by when all payouts have been resolved for the contract.
     * If the contract is null, the dialog has been invoked without a
     * specific contract and all outstanding payouts have been resolved.
     */
    public void resolveAllContracts() {
        resolveContract(null);
        payouts.clear();
    }

    public void resolveContract(final @Nullable Mission mission) {
        if (mission == null) {
            unresolvedPersonnel.keySet().forEach(this::resolveContract);
            unresolvedPersonnel.clear();
        } else {
            resolveContract(mission.getId());
            unresolvedPersonnel.remove(mission.getId());
        }
    }

    private void resolveContract(int contractId) {
        if (null != unresolvedPersonnel.get(contractId)) {
            for (UUID pid : unresolvedPersonnel.get(contractId)) {
                payouts.remove(pid);
            }
        }
        rollRequired.remove(contractId);
    }

    public Set<UUID> getRetirees() {
        return getRetirees(null);
    }

    public Set<UUID> getRetirees(final @Nullable Mission mission) {
        return (mission == null) ? payouts.keySet() : unresolvedPersonnel.get(mission.getId());
    }

    public Payout getPayout(UUID id) {
        return payouts.get(id);
    }

    /**
     * @param campaign the campaign the person is a part of
     * @param person   the person to get the bonus cost for
     * @return The amount in C-bills required to get a bonus to the Employee
     *         Turnover roll
     */
    public static Money getPayoutOrBonusValue(final Campaign campaign, Person person) {
        double bonusMultiplier = campaign.getCampaignOptions().getPayoutRateEnlisted();

        if (person.getRank().isOfficer()) {
            bonusMultiplier = campaign.getCampaignOptions().getPayoutRateOfficer();
        }

        if (campaign.getCampaignOptions().isUsePayoutServiceBonus()) {
            bonusMultiplier += person.getYearsInService(campaign)
                    * ((double) campaign.getCampaignOptions().getPayoutServiceBonusRate() / 100);
        }

        return person.getSalary(campaign).multipliedBy(bonusMultiplier);
    }

    /**
     * Class used to record the required payout to each
     * retired/defected/killed/sacked
     * person.
     */
    public static class Payout {
        private int weightClass = 0;
        private Money payoutAmount = Money.zero();
        private boolean wasKilled = false;
        private boolean wasSacked = false;

        public Payout() {

        }

        public Payout(final Campaign campaign, final Person person, final Money shareValue, final boolean killed,
                final boolean sacked,
                final boolean sharesForAll) {
            if (killed) {
                setWasKilled(true);
            } else if (sacked) {
                setWasSacked(true);
            }

            calculatePayout(campaign, person, killed, sacked, shareValue.isPositive());

            if ((shareValue.isPositive()) && (campaign.getCampaignOptions().isUseShareSystem())) {
                payoutAmount = payoutAmount.plus(shareValue.multipliedBy(person.getNumShares(campaign, sharesForAll)));
            }
        }

        private void calculatePayout(final Campaign campaign, final Person person, final boolean killed,
                final boolean sacked, final boolean shareSystem) {
            final Profession profession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());

            // person was killed
            if (killed) {
                payoutAmount = getPayoutOrBonusValue(campaign, person)
                        .multipliedBy(campaign.getCampaignOptions().getPayoutRetirementMultiplier());
                // person is getting medically discharged
            } else if (!person.getPermanentInjuries().isEmpty()) {
                payoutAmount = getPayoutOrBonusValue(campaign, person)
                        .multipliedBy(campaign.getCampaignOptions().getPayoutRetirementMultiplier());
                // person is defecting
            } else if (isBreakingContract(person, campaign.getLocalDate(),
                    campaign.getCampaignOptions().getServiceContractDuration())) {
                payoutAmount = Money.of(0);
                // person is retiring
            } else if (person.getAge(campaign.getLocalDate()) >= 50) {
                payoutAmount = getPayoutOrBonusValue(campaign, person)
                        .multipliedBy(campaign.getCampaignOptions().getPayoutRetirementMultiplier());
                // person was sacked
            } else if (sacked) {
                payoutAmount = Money.of(0);
                // person is resigning
            } else {
                payoutAmount = getPayoutOrBonusValue(campaign, person);
            }

            if (!shareSystem && (profession.isMekWarrior() || profession.isAerospace())
                    && (person.getOriginalUnitWeight() > 0)) {
                weightClass = person.getOriginalUnitWeight() + person.getOriginalUnitTech();
            }
        }

        public int getWeightClass() {
            return weightClass;
        }

        public void setWeightClass(int weight) {
            weightClass = weight;
        }

        public Money getPayoutAmount() {
            return payoutAmount;
        }

        public void setPayoutAmount(Money payoutAmount) {
            this.payoutAmount = payoutAmount;
        }

        public boolean isWasKilled() {
            return wasKilled;
        }

        public void setWasKilled(boolean wasKilled) {
            this.wasKilled = wasKilled;
        }

        public boolean isWasSacked() {
            return wasSacked;
        }

        public void setWasSacked(boolean wasSacked) {
            this.wasSacked = wasSacked;
        }

        public static boolean isBreakingContract(Person person, LocalDate localDate, int ContractDuration) {
            return ChronoUnit.MONTHS.between(person.getRecruitment(), localDate) < ContractDuration;
        }
    }

    private String createCsv(Collection<?> coll) {
        return StringUtils.join(coll, ",");
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "retirementDefectionTracker");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rollRequired", createCsv(rollRequired));
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "unresolvedPersonnel");
        for (Integer i : unresolvedPersonnel.keySet()) {
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent, "contract", "id", i,
                    createCsv(unresolvedPersonnel.get(i)));
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unresolvedPersonnel");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "payouts");
        for (UUID pid : payouts.keySet()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "payout", "id", pid);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weightClass", payouts.get(pid).getWeightClass());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cbills", payouts.get(pid).getPayoutAmount());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wasKilled", payouts.get(pid).isWasKilled());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wasSacked", payouts.get(pid).isWasSacked());
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "payout");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "payouts");

        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastRetirementRoll", lastRetirementRoll);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "retirementDefectionTracker");
    }

    public static RetirementDefectionTracker generateInstanceFromXML(Node wn, Campaign c) {
        RetirementDefectionTracker retVal = null;

        try {
            // Instantiate the correct child class, and call its parsing function.
            retVal = new RetirementDefectionTracker();

            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            // Loop through the nodes and load our contract offers
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (wn2.getNodeName().equalsIgnoreCase("rollRequired")) {
                    if (!wn2.getTextContent().isBlank()) {
                        String[] ids = wn2.getTextContent().split(",");
                        for (String id : ids) {
                            retVal.rollRequired.add(Integer.parseInt(id));
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("unresolvedPersonnel")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        if (wn3.getNodeName().equalsIgnoreCase("contract")) {
                            int id = Integer.parseInt(wn3.getAttributes().getNamedItem("id").getTextContent());
                            String[] ids = wn3.getTextContent().split(",");
                            HashSet<UUID> pids = Arrays
                                    .stream(ids)
                                    .map(UUID::fromString)
                                    .collect(Collectors.toCollection(HashSet::new));
                            retVal.unresolvedPersonnel.put(id, pids);
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("payouts")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        if (wn3.getNodeName().equalsIgnoreCase("payout")) {
                            UUID pid = UUID.fromString(wn3.getAttributes().getNamedItem("id").getTextContent());
                            Payout payout = new Payout();
                            NodeList nl3 = wn3.getChildNodes();
                            for (int z = 0; z < nl3.getLength(); z++) {
                                Node wn4 = nl3.item(z);
                                if (wn4.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                }
                                if (wn4.getNodeName().equalsIgnoreCase("weightClass")) {
                                    payout.setWeightClass(Integer.parseInt(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("cbills")) {
                                    payout.setPayoutAmount(Money.fromXmlString(wn4.getTextContent().trim()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("wasKilled")) {
                                    payout.setWasKilled(Boolean.parseBoolean(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("wasSacked")) {
                                    payout.setWasSacked(Boolean.parseBoolean(wn4.getTextContent()));
                                }
                            }
                            retVal.payouts.put(pid, payout);
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("lastRetirementRoll")) {
                    retVal.setLastRetirementRoll(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            logger.error(
                    "RetirementDefectionTracker: either the class name is invalid or the listed name doesn't exist.",
                    ex);
        }

        if (retVal != null) {
            // sometimes, a campaign may be loaded with orphan records in the Employee
            // Turnover tracker
            // let's clean those up here.
            retVal.cleanupOrphans(c);
        }

        return retVal;
    }
}
