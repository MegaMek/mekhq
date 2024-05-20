/*
 * RetirementDefectionTracker.java
 *
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
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
package mekhq.campaign.personnel;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.universe.FactionHints;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Neoancient
 *
 * Against the Bot
 * Utility class that handles Employee Turnover rolls and final payments
 * to personnel who retire/defect/get sacked and families of those killed
 * in battle.
 */
public class RetirementDefectionTracker {
    /* In case the dialog is closed after making the retirement rolls
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
    private static Person mechWarriorCommander;
    private static Integer mechWarriorCommanderModifier;

    private Integer hrSkill;
    private Integer difficulty;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionTracker");

    public RetirementDefectionTracker() {
        rollRequired = new HashSet<>();
        unresolvedPersonnel = new HashMap<>();
        payouts = new HashMap<>();
        lastRetirementRoll = LocalDate.now();
    }

    /**
     * Computes the target for retirement rolls for all eligible personnel; this includes
     * all active personnel who are not dependents, prisoners, or bondsmen.
     *
     * @param mission The contract that is being resolved; if the retirement roll is not due to
     *                 contract resolutions (e.g., &gt; 12 months since last roll), this can be null.
     * @param campaign  The campaign to calculate target numbers for
     * @return A map with person ids as key and calculated target roll as value.
     */
    public Map<UUID, TargetRoll> getTargetNumbers(final @Nullable Mission mission, final Campaign campaign) {
        final Map <UUID, TargetRoll> targets = new HashMap<>();

        if (null != mission) {
            rollRequired.add(mission.getId());
        }

        if (!campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isFixed()) {
            setHrSkill(campaign);
            getDifficultyModifier(campaign);
        }

        if (campaign.getCampaignOptions().isUseManagementSkill()) {
            getManagementSkillValues(campaign);
        }

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isCivilian()) || (!person.getPrisonerStatus().isFree()) || (person.isDeployed())) {
                continue;
            }

            if ((person.isFounder()) && (!campaign.getCampaignOptions().isUseRandomFounderRetirement())) {
                continue;
            }

            if (campaign.getCampaignOptions().isUseSubContractSoldiers()) {
                if ((person.getUnit() != null) && (person.getUnit().usesSoldiers()) && (!person.getUnit().isCommander(person))) {
                    continue;
                }
            }

            TargetRoll targetNumber = new TargetRoll(getBaseTargetNumber(campaign, person), resources.getString("base.text"));

            // Service Contract
            if (ChronoUnit.MONTHS.between(person.getRecruitment(), campaign.getLocalDate()) < campaign.getCampaignOptions().getServiceContractDuration()) {
                targetNumber.addModifier(-campaign.getCampaignOptions().getServiceContractModifier(), resources.getString("contract.text"));
            }

            // Desirability modifier
            if (campaign.getCampaignOptions().isUseSkillModifiers()) {
                targetNumber.addModifier(person.getExperienceLevel(campaign, false), resources.getString("desirability.text"));
            }

            // Fatigue modifier
            if ((campaign.getCampaignOptions().isUseFatigue()) && (campaign.getCampaignOptions().isUseFatigueModifiers())) {
                int fatigueModifier = MathUtility.clamp(((person.getFatigue() - 1) / 4) - 1, 0, 3);

                if (fatigueModifier > 0) {
                    targetNumber.addModifier(fatigueModifier, resources.getString("fatigue.text"));
                }
            }

            // Administrative Strain Modifiers
            if (campaign.getCampaignOptions().isUseAdministrativeStrain()) {
                int administrativeStrainModifier = getAdministrativeStrainModifier(campaign);

                if (administrativeStrainModifier > 0) {
                    targetNumber.addModifier(administrativeStrainModifier, resources.getString("administrativeStrain.text"));
                }
            }

            // Management Skill Modifier
            if (campaign.getCampaignOptions().isUseManagementSkill()) {
                targetNumber.addModifier(getManagementSkillModifier(person), resources.getString("managementSkill.text"));
            }

            // Shares Modifiers
            if (campaign.getCampaignOptions().isUseShareSystem()) {
                // If this retirement roll is not being made at the end of a contract (e.g. >12 months since last roll),
                // the share percentage should still apply.
                // In the case of multiple active contracts, pick the one with the best percentage.

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
                            if ((contract == null) || (contract.getSharesPct() < atbContract.getSharesPct())) {
                                contract = atbContract;
                            }
                        }
                    }
                }

                if (contract != null) {
                    targetNumber.addModifier(- (contract.getSharesPct() / 10), resources.getString("shares.text"));
                }
            }

            // Unit Rating modifier
            if (campaign.getCampaignOptions().isUseUnitRatingModifiers()) {
                int unitRatingModifier = getUnitRatingModifier(campaign);
                targetNumber.addModifier(unitRatingModifier, resources.getString("unitRating.text"));
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
                    && (!campaign.getCampaignOptions().isUseHideLoyalty())
                    && (person.getLoyalty() != 0)) {
                targetNumber.addModifier(-person.getLoyalty(), resources.getString("loyalty.text"));
            }

            // Faction Modifiers
            if (campaign.getCampaignOptions().isUseFactionModifiers()) {
                if (campaign.getFaction().isPirate()) {
                    targetNumber.addModifier(1, resources.getString("factionPirateCompany.text"));
                } else if (person.getOriginFaction().isPirate()) {
                    targetNumber.addModifier(1, resources.getString("factionPirate.text"));
                }

                if (person.getOriginFaction().isMercenary()) {
                    targetNumber.addModifier(1, resources.getString("factionMercenary.text"));
                }

                if (person.getOriginFaction().isClan()) {
                    targetNumber.addModifier(-2, resources.getString("factionClan.text"));
                }

                if (FactionHints.defaultFactionHints().isAtWarWith(campaign.getFaction(), person.getOriginFaction(), campaign.getLocalDate())) {
                    targetNumber.addModifier(1, resources.getString("factionEnemy.text"));
                }
            }

            // Age modifiers
            if (campaign.getCampaignOptions().isUseAgeModifiers()) {
                int age = person.getAge(campaign.getLocalDate());
                int ageMod = getAgeMod(age);

                if (ageMod != 0) {
                    targetNumber.addModifier(ageMod, resources.getString("age.text"));
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
                for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
                    IOption ability = i.nextElement();
                    if (ability.booleanValue()) {
                        if (ability.getName().equals("tactical_genius")) {
                            targetNumber.addModifier(1, resources.getString("tacticalGenius.text"));
                            break;
                        }
                    }
                }
            }

            // Founder Modifier
            if (person.isFounder()) {
                targetNumber.addModifier(2, resources.getString("founder.text"));
            }

            targets.put(person.getId(), targetNumber);
        }
        return targets;
    }

    /**
     * Sets the HR skill averaged across all Admin/HR personnel.
     *
     * @param campaign the Campaign object to get personnel from.
     */
    private void setHrSkill(Campaign campaign) {
        int hrPersonnelCount = (int) campaign.getActivePersonnel().stream()
                .filter(person -> (!person.getPrisonerStatus().isPrisoner()) && (!person.getPrisonerStatus().isPrisonerDefector()))
                .filter(person -> (person.getPrimaryRole().isAdministratorHR()) || (person.getSecondaryRole().isAdministratorHR()))
                .count();

        if (hrPersonnelCount != 0) {
            if (campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isNegotiation()) {
                hrSkill = getCombinedSkillValues(campaign, SkillType.S_NEG) / hrPersonnelCount;
            } else if (campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isAdministration()) {
                hrSkill = getCombinedSkillValues(campaign, SkillType.S_ADMIN) / hrPersonnelCount;
            }
        } else {
            hrSkill = 0;
        }
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
            return -getCommanderManagementSkill(person.getPrimaryRole());
        } else {
            return -((getCommanderManagementSkill(person.getPrimaryRole()) + getCommanderManagementSkill(person.getSecondaryRole())) / 2);
        }
    }

    /**
     * Returns the management skill modifier for a commander based on the given personnel role.
     *
     * @param role the personnel role of the person we're fetching the modifier for
     * @return the management skill modifier for the commander
     */
    private static int getCommanderManagementSkill(PersonnelRole role) {
        switch (Profession.getProfessionFromPersonnelRole(role)) {
            case AEROSPACE:
                return asfCommanderModifier;
            case VEHICLE:
                return vehicleCrewCommanderModifier;
            case INFANTRY:
                return infantryCommanderModifier;
            case NAVAL:
                return navalCommanderModifier;
            case TECH:
                return techCommanderModifier;
            case MEDICAL:
                return medicalCommanderModifier;
            case ADMINISTRATOR:
                return administrationCommanderModifier;
            case MECHWARRIOR:
                return mechWarriorCommanderModifier;
            case CIVILIAN:
                return 0;
        }
        return 0;
    }

    /**
     * This method calculates the management skill values for the different commanding officers.
     * Each commander's management skill value is calculated based on their role and rank within the campaign.
     * The management skill modifier is calculated by adding the base modifier
     * (retrieved from campaign options) and the commander's individual leadership skill.
     * If no suitable commander is found for a particular role,
     * the management skill modifier for that role remains the same as the base modifier.
     *
     * @param campaign The Campaign object for which to calculate the management skill values.
     */
    private void getManagementSkillValues(Campaign campaign) {
        int baseModifier = campaign.getCampaignOptions().getManagementSkillPenalty();

        if (campaign.getCampaignOptions().isUseCommanderLeadershipOnly()) {
            Person commander = campaign.getFlaggedCommander();

            if ((commander != null) && (commander.hasSkill(SkillType.S_LEADER))) {
                int commanderSkill = baseModifier + commander.getSkill(SkillType.S_LEADER).getFinalSkillValue();

                asfCommanderModifier = commanderSkill;
                vehicleCrewCommanderModifier = commanderSkill;
                infantryCommanderModifier = commanderSkill;
                navalCommanderModifier = commanderSkill;
                techCommanderModifier = commanderSkill;
                medicalCommanderModifier = commanderSkill;
                administrationCommanderModifier = commanderSkill;
                mechWarriorCommanderModifier = commanderSkill;
            } else {
                asfCommanderModifier = baseModifier;
                vehicleCrewCommanderModifier = baseModifier;
                infantryCommanderModifier = baseModifier;
                navalCommanderModifier = baseModifier;
                techCommanderModifier = baseModifier;
                medicalCommanderModifier = baseModifier;
                administrationCommanderModifier = baseModifier;
                mechWarriorCommanderModifier = baseModifier;
            }

            return;
        }

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
                        asfCommanderModifier = baseModifier + getIndividualCommanderLeadership(asfCommander);
                    }
                    break;
                case VEHICLE:
                    if (person.outRanksUsingSkillTiebreaker(campaign, vehicleCrewCommander)) {
                        vehicleCrewCommander = person;
                        vehicleCrewCommanderModifier = baseModifier + getIndividualCommanderLeadership(vehicleCrewCommander);
                    }
                    break;
                case INFANTRY:
                    if (person.outRanksUsingSkillTiebreaker(campaign, infantryCommander)) {
                        infantryCommander = person;
                        infantryCommanderModifier = baseModifier + getIndividualCommanderLeadership(infantryCommander);
                    }
                    break;
                case NAVAL:
                    if (person.outRanksUsingSkillTiebreaker(campaign, navalCommander)) {
                        navalCommander = person;
                        navalCommanderModifier = baseModifier + getIndividualCommanderLeadership(navalCommander);
                    }
                    break;
                case TECH:
                    if (person.outRanksUsingSkillTiebreaker(campaign, techCommander)) {
                        techCommander = person;
                        techCommanderModifier = baseModifier + getIndividualCommanderLeadership(techCommander);
                    }
                    break;
                case MEDICAL:
                    if (person.outRanksUsingSkillTiebreaker(campaign, medicalCommander)) {
                        medicalCommander = person;
                        medicalCommanderModifier = baseModifier + getIndividualCommanderLeadership(medicalCommander);
                    }
                    break;
                case ADMINISTRATOR:
                    if (person.outRanksUsingSkillTiebreaker(campaign, administrationCommander)) {
                        administrationCommander = person;
                        administrationCommanderModifier = baseModifier + getIndividualCommanderLeadership(administrationCommander);
                    }
                    break;
                case MECHWARRIOR:
                    if (person.outRanksUsingSkillTiebreaker(campaign, mechWarriorCommander)) {
                        mechWarriorCommander = person;
                        mechWarriorCommanderModifier = baseModifier + getIndividualCommanderLeadership(mechWarriorCommander);
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
                        asfCommanderModifier = baseModifier;
                    }
                    break;
                case VEHICLE:
                    if (vehicleCrewCommander == null) {
                        vehicleCrewCommanderModifier = baseModifier;
                    }
                    break;
                case INFANTRY:
                    if (infantryCommander == null) {
                        infantryCommanderModifier = baseModifier;
                    }
                    break;
                case NAVAL:
                    if (navalCommander == null) {
                        navalCommanderModifier = baseModifier;
                    }
                    break;
                case TECH:
                    if (techCommander == null) {
                        techCommanderModifier = baseModifier;
                    }
                    break;
                case MEDICAL:
                    if (medicalCommander == null) {
                        medicalCommanderModifier = baseModifier;
                    }
                    break;
                case ADMINISTRATOR:
                    if (administrationCommander == null) {
                        administrationCommanderModifier = baseModifier;
                    }
                    break;
                case MECHWARRIOR:
                    if (mechWarriorCommander == null) {
                        mechWarriorCommanderModifier = baseModifier;
                    }
                    break;
                case CIVILIAN:
                    break;
            }
        }
    }

    /**
     * Calculates the individual commander Leadership skill based on the provided commander.
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
     * This method calculates the combatant strain modifier based on the active personnel assigned to units.
     *
     * @param campaign the campaign for which to calculate the strain modifier
     * @return the strain modifier
     */
    public static int getAdministrativeStrainModifier(Campaign campaign) {
        int personnel = getAdministrativeStrain(campaign);

        int maximumStrain = campaign.getCampaignOptions().getAdministrativeStrain() * getCombinedSkillValues(campaign, SkillType.S_ADMIN);

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

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isCivilian()) || (person.getPrisonerStatus().isPrisoner()) || (person.getPrisonerStatus().isPrisonerDefector())) {
                continue;
            }

            if (person.getUnit() != null) {
                if (person.getUnit().isCommander(person)) {
                    if (person.getUnit().getEntity().isProtoMek()) {
                        proto++;
                    } else {
                        personnel += Math.max(1, person.getUnit().getCrew().size() / campaign.getCampaignOptions().getMultiCrewStrainDivider());
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

        personnel += proto / campaign.getCampaignOptions().getMultiCrewStrainDivider();
        return personnel;
    }

    /**
     * Calculates the combined skill values of active Admin/HR personnel.
     *
     * @param campaign the campaign for which to calculate the combined skill values
     * @return the combined skill values of active Admin/HR personnel in the campaign
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
     * Returns a difficulty modifier based on the turnover difficulty campaign setting.
     *
     * @param campaign the current campaign
     */
    private void getDifficultyModifier(Campaign campaign) {
        switch (campaign.getCampaignOptions().getTurnoverDifficulty()) {
            case NONE:
                difficulty = -6;
                break;
            case ULTRA_GREEN:
                difficulty = -5;
                break;
            case GREEN:
                difficulty = -4;
                break;
            case REGULAR:
                difficulty = -3;
                break;
            case VETERAN:
                difficulty = -2;
                break;
            case ELITE:
                difficulty = -1;
                break;
            case HEROIC:
                difficulty = 0;
                break;
            case LEGENDARY:
                difficulty = 1;
                break;
        }
    }

    /**
     * This method calculates the base target number.
     *
     * @param campaign the campaign for which the base target number is calculated
     * @return the base target number
     */
    private int getBaseTargetNumber(Campaign campaign, Person person) {
        if (!campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isFixed()) {
            int targetNumber;

            // we use 'shellPerson' as we have no way to ensure 'person' has the necessary skills, and we'll get an NPE if they don't
            Person shellPerson = new Person(campaign);

            if (campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isNegotiation()) {
                shellPerson.addSkill(SkillType.S_NEG, 1, 0);
                targetNumber = shellPerson.getSkills().getSkill(SkillType.S_NEG).getType().getTarget();
            } else {
                shellPerson.addSkill(SkillType.S_ADMIN, 1, 0);
                targetNumber = shellPerson.getSkills().getSkill(SkillType.S_ADMIN).getType().getTarget();
            }

            if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseHideLoyalty())) {
                return targetNumber - hrSkill + difficulty - person.getLoyalty();
            } else {
                return targetNumber - hrSkill + difficulty;
            }
        } else {
            if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseHideLoyalty())) {
                return campaign.getCampaignOptions().getTurnoverFixedTargetNumber() - person.getLoyalty();
            } else {
                return campaign.getCampaignOptions().getTurnoverFixedTargetNumber();
            }
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

        if (campaign.getUnitRatingMod() < 1) {
            unitRating = 2;
        } else if (campaign.getUnitRatingMod() == 1) {
            unitRating = 1;
        } else if (campaign.getUnitRatingMod() > 3) {
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

        FinancialReport r = FinancialReport.calculate(campaign);

        Money netWorth = r.getNetWorth();
        if (campaign.getCampaignOptions().isSharesExcludeLargeCraft()) {
            netWorth = netWorth.minus(r.getLargeCraftValue());
        }

        int totalShares = campaign.getActivePersonnel()
                .stream()
                .mapToInt(p -> p.getNumShares(campaign, campaign.getCampaignOptions().isSharesForAll()))
                .sum();

        if (totalShares <= 0) {
            return Money.zero();
        }

        return netWorth.dividedBy(totalShares);
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
            ageMod = 1;
        } else if ((age >= 65) && (age < 75)) {
            ageMod = 2;
        } else if ((age >= 75) && (age < 85)) {
            ageMod = 3;
        } else if ((age >= 85) && (age < 95)) {
            ageMod = 4;
        } else if ((age >= 95) && (age < 105)) {
            ageMod = 5;
        } else if (age >= 105) {
            ageMod = 6;
        }

        return ageMod;
    }

    /**
     * Makes rolls for Employee Turnover based on previously calculated target rolls,
     * and tracks all retirees in the unresolvedPersonnel hash in case the dialog
     * is closed before payments are resolved, to avoid re-rolling the results.
     *
     * @param mission Nullable mission value
     * @param targets The hash previously generated by getTargetNumbers.
     * @param shareValue The value of each share in the unit; if not using the share system, this is zero.
     * @param campaign the current campaign
     */
    public void rollRetirement(final @Nullable Mission mission, final Map<UUID, TargetRoll> targets,
                               final Money shareValue, final Campaign campaign) {
        if ((mission != null) && !unresolvedPersonnel.containsKey(mission.getId())) {
            unresolvedPersonnel.put(mission.getId(), new HashSet<>());
        }

        for (UUID id : targets.keySet()) {
            if (Compute.d6(2) < targets.get(id).getValue()) {
                if (mission != null) {
                    unresolvedPersonnel.get(mission.getId()).add(id);
                }

                Person p = campaign.getPerson(id);

                // TODO differentiate between retirement and defection, here.
                //  This behavior only makes sense for defection.
                // if the retiree is the commander of an infantry platoon, all non-founders in the platoon follow them into retirement
                if (campaign.getCampaignOptions().isUseSubContractSoldiers()) {
                    if ((p.getUnit() != null) && (p.getUnit().usesSoldiers()) && (p.getUnit().isCommander(p))) {
                        for (Person person : p.getUnit().getSoldiers()) {
                            if ((!person.isFounder()) || (campaign.getCampaignOptions().isUseRandomFounderRetirement())) {
                                payouts.put(person.getId(), new Payout(campaign, campaign.getPerson(person.getId()),
                                        shareValue, false, campaign.getCampaignOptions().isSharesForAll()));
                            }
                        }

                        continue;
                    }
                }

                payouts.put(id, new Payout(campaign, campaign.getPerson(id),
                            shareValue, false, campaign.getCampaignOptions().isSharesForAll()));
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
     * Handles final payout to any personnel who are sacked or killed in battle
     *
     * @param person The person to be removed from the campaign
     * @param killed True if killed in battle, false if sacked
     * @param campaign the ongoing campaign
     * @param contract If not null, the payout must be resolved before the contract can be resolved.
     * @return true, if the person is due a payout, otherwise false
     */
    public boolean removeFromCampaign(Person person, boolean killed, Campaign campaign,
                                      AtBContract contract) {
        if (!person.getPrisonerStatus().isFree()) {
            return false;
        }

        payouts.put(person.getId(), new Payout(campaign, person, getShareValue(campaign),
                killed, campaign.getCampaignOptions().isSharesForAll()));

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

    /** Called by when all payouts have been resolved for the contract.
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
     * @param person the person to get the bonus cost for
     * @return The amount in C-bills required to get a bonus to the Employee Turnover roll
     */
    public static Money getPayoutOrBonusValue(final Campaign campaign, Person person) {
        int bonusMultiplier = campaign.getCampaignOptions().getPayoutRateEnlisted();

        if (person.getRank().isOfficer()) {
            bonusMultiplier = campaign.getCampaignOptions().getPayoutRateOfficer();
        }

        if (campaign.getCampaignOptions().isUsePayoutServiceBonus()) {
            bonusMultiplier += person.getYearsInService(campaign) * (campaign.getCampaignOptions().getPayoutServiceBonusRate() / 100);
        }

        return person.getSalary(campaign).multipliedBy(bonusMultiplier);
    }

    /**
     * Class used to record the required payout to each retired/defected/killed/sacked
     * person.
     */
    public static class Payout {
        private int weightClass = 0;
        private int dependents = 0;
        private Money payoutAmount = Money.zero();
        private boolean recruit = false;
        private PersonnelRole recruitRole = PersonnelRole.NONE;
        private boolean heir = false;
        private boolean stolenUnit = false;
        private UUID stolenUnitId = null;

        public Payout() {

        }

        public Payout(final Campaign campaign, final Person person, final Money shareValue, final boolean killed, final boolean sharesForAll) {
            calculatePayout(campaign, person, killed, shareValue.isPositive());

            if ((shareValue.isPositive()) && (campaign.getCampaignOptions().isUseShareSystem())) {
                payoutAmount = payoutAmount.plus(shareValue.multipliedBy(person.getNumShares(campaign, sharesForAll)));
            }

            // TODO investigate if these actually do anything
            if (killed) {
                switch (Compute.d6()) {
                    case 2:
                        dependents = 1;
                        break;
                    case 3:
                        dependents = Compute.d6();
                        break;
                    case 4:
                    case 5:
                        recruit = true;
                        break;
                    case 6:
                        heir = true;
                        break;
                    default:
                        break;
                }
            }
        }

        private void calculatePayout(final Campaign campaign, final Person person, final boolean killed, final boolean shareSystem) {
            int roll;

            if (killed) {
                roll = Utilities.dice(1, 5);
            } else {
                roll = Compute.d6() + Math.max(-1, person.getExperienceLevel(campaign, false) - 2);
                if (person.getRank().isOfficer()) {
                    roll += 1;
                }
            }

            if (roll >= 6 && (person.getPrimaryRole().isAerospacePilot() || person.getSecondaryRole().isAerospacePilot())) {
                stolenUnit = true;
            } else {
                final Profession profession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());

                // TODO when we differentiate between types of retirement we'll need to edit this.
                payoutAmount = getPayoutOrBonusValue(campaign, person).multipliedBy(campaign.getCampaignOptions().getPayoutRetirementMultiplier());

                if (!shareSystem && (profession.isMechWarrior() || profession.isAerospace())
                        && (person.getOriginalUnitWeight() > 0)) {
                    weightClass = person.getOriginalUnitWeight() + person.getOriginalUnitTech();
                    if (roll <= 1) {
                        weightClass--;
                    } else if (roll >= 5) {
                        weightClass++;
                    }
                }
            }
        }

        public int getWeightClass() {
            return weightClass;
        }

        public void setWeightClass(int weight) {
            weightClass = weight;
        }

        public int getDependents() {
            return dependents;
        }

        public void setDependents(int d) {
            dependents = d;
        }

        public Money getPayoutAmount() {
            return payoutAmount;
        }

        public void setPayoutAmount(Money payoutAmount) {
            this.payoutAmount = payoutAmount;
        }

        public boolean hasRecruit() {
            return recruit;
        }

        public void setRecruit(boolean r) {
            recruit = r;
        }

        public PersonnelRole getRecruitRole() {
            return recruitRole;
        }

        public void setRecruitRole(PersonnelRole role) {
            recruitRole = role;
        }

        public boolean hasHeir() {
            return heir;
        }

        public void setHeir(boolean h) {
            heir = h;
        }

        public boolean hasStolenUnit() {
            return stolenUnit;
        }

        public void setStolenUnit(boolean stolen) {
            stolenUnit = stolen;
        }

        public UUID getStolenUnitId() {
            return stolenUnitId;
        }

        public void setStolenUnitId(UUID id) {
            stolenUnitId = id;
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
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent, "contract", "id", i, createCsv(unresolvedPersonnel.get(i)));
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "unresolvedPersonnel");

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "payouts");
        for (UUID pid : payouts.keySet()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "payout", "id", pid);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "weightClass", payouts.get(pid).getWeightClass());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dependents", payouts.get(pid).getDependents());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cbills", payouts.get(pid).getPayoutAmount());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "recruit", payouts.get(pid).hasRecruit());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "heir", payouts.get(pid).hasHeir());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "stolenUnit", payouts.get(pid).hasStolenUnit());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "stolenUnitId", payouts.get(pid).getStolenUnitId());
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
                        String [] ids = wn2.getTextContent().split(",");
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
                            String [] ids = wn3.getTextContent().split(",");
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
                                } else if (wn4.getNodeName().equalsIgnoreCase("dependents")) {
                                    payout.setDependents(Integer.parseInt(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("c-bills")) {
                                    payout.setPayoutAmount(Money.fromXmlString(wn4.getTextContent().trim()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("recruit")) {
                                    payout.setRecruit(Boolean.parseBoolean(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("heir")) {
                                    payout.setHeir(Boolean.parseBoolean(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("stolenUnit")) {
                                    payout.setStolenUnit(Boolean.parseBoolean(wn4.getTextContent()));
                                } else if (wn4.getNodeName().equalsIgnoreCase("stolenUnitId")) {
                                    payout.setStolenUnitId(UUID.fromString(wn4.getTextContent()));
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
            LogManager.getLogger().error("RetirementDefectionTracker: either the class name is invalid or the listed name doesn't exist.", ex);
        }

        if (retVal != null) {
            // sometimes, a campaign may be loaded with orphan records in the Employee Turnover tracker
            // let's clean those up here.
            retVal.cleanupOrphans(c);
        }

        return retVal;
    }
}
