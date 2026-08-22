/*
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.turnoverAndRetention;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.Person.getLoyaltyName;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_MEDIATOR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.Payout.isBreakingContract;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.options.IOption;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Neoancient
 *       <p>
 *       Against the Bot Utility class that handles Employee Turnover rolls and final payments to personnel who
 *       retire/defect/get sacked and families of those killed in battle.
 */
public class RetirementDefectionTracker {
    private static final MMLogger LOGGER = MMLogger.create(RetirementDefectionTracker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RetirementDefectionTracker";

    public static final int RETIREMENT_AGE = 50;
    public static final int HR_DEFAULT_NOADMIN_PENALTY = 10;

    /*
     * In case the dialog is closed after making the retirement rolls
     * and determining payouts, but before the retirees have been paid,
     * we store those results to avoid making the rolls again.
     */
    final private Set<UUID> rollRequired;
    final private Map<UUID, HashSet<UUID>> unresolvedPersonnel;
    final private Map<UUID, Payout> payouts;
    private LocalDate lastRetirementRoll;

    /**
     * Contract references loaded from a pre-UUID save, held until the campaign loader can map their legacy integer
     * mission ids onto the converted contracts' {@link UUID}s (see {@link #relinkLegacyMissionIds(Map)}). Empty for
     * modern saves.
     */
    final private transient Set<Integer> legacyRollRequired = new HashSet<>();
    final private transient Map<Integer, HashSet<UUID>> legacyUnresolvedPersonnel = new HashMap<>();

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

    private final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_BUNDLE);

    public RetirementDefectionTracker() {
        rollRequired = new HashSet<>();
        unresolvedPersonnel = new HashMap<>();
        payouts = new HashMap<>();
        lastRetirementRoll = LocalDate.now();
    }

    /**
     * Calculates the administrative strain for a given campaign.
     *
     * @param campaign the campaign for which to calculate the administrative strain
     *
     * @return the total administrative strain of the campaign
     */
    public static int getHRStrain(Campaign campaign) {
        double personnel = 0;

        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            PersonnelRole primaryRole = person.getPrimaryRole();

            if (primaryRole.isCivilian()) {
                personnel += 0.1;
            } else if (!(primaryRole.isAssistant() && person.getSecondaryRole().isNone())) {
                personnel++;
            }
        }

        return (int) round(personnel);
    }

    /**
     * Determines whether the campaign is in the middle of a contract in hostile territory. If AtB is disabled, this
     * method only checks whether there is an active contract.
     *
     * @param campaign the campaign to check for hostile territory modifier
     *
     * @return true if the campaign is in hostile territory modifier or (if AtB is disabled) whether the campaign is in
     *       an active contract, false otherwise
     */
    private boolean isHostileTerritory(Campaign campaign) {
        List<ContractObjectiveType> defensiveContracts = Arrays.asList(ContractObjectiveType.GARRISON_DUTY,
              ContractObjectiveType.CADRE_DUTY,
              ContractObjectiveType.SECURITY_DUTY,
              ContractObjectiveType.RIOT_DUTY);

        List<AbstractContract> activeContracts = campaign.getActiveContracts();

        if (!activeContracts.isEmpty()) {
            if (campaign.getCampaignOptions().isUseStratCon()) {
                Optional<AbstractContract> defensiveContract = activeContracts.stream()
                                                                     .filter(mission -> !defensiveContracts.contains(
                                                                           mission.getObjectiveType()))
                                                                     .findFirst();

                return defensiveContract.isPresent();
            } else {
                return true;
            }
        }

        return false;
    }

    public static List<TargetRollModifier> getFactionModifiers(Person person, Campaign campaign) {
        ArrayList<TargetRollModifier> result = new ArrayList<>();
        Faction campaignFaction = campaign.getPlayerForce().getFaction();

        // campaign faction modifiers
        if (campaignFaction.isPirate()) {
            result.add(new TargetRollModifier(1, getTextAt(RESOURCE_BUNDLE, "factionPirateCompany.text")));
        } else if (campaignFaction.isComStarOrWoB()) {
            if (person.getOriginFaction().isComStarOrWoB()) {
                result.add(new TargetRollModifier(-2, getTextAt(RESOURCE_BUNDLE, "factionComStarOrWob.text")));
            }
        } else if ((!campaignFaction.isClan()) && (!campaignFaction.isMercenary())) {
            if (campaignFaction.equals(person.getOriginFaction())) {
                result.add(new TargetRollModifier(-1, getTextAt(RESOURCE_BUNDLE, "factionLoyalty.text")));
            }
        }

        // origin faction modifiers
        if ((!campaignFaction.isPirate()) && (person.getOriginFaction().isPirate())) {
            result.add(new TargetRollModifier(1, getTextAt(RESOURCE_BUNDLE, "factionPirate.text")));
        }

        if (person.getOriginFaction().isMercenary()) {
            result.add(new TargetRollModifier(1, getTextAt(RESOURCE_BUNDLE, "factionMercenary.text")));
        }

        if (person.getOriginFaction().isClan()) {
            result.add(new TargetRollModifier(-2, getTextAt(RESOURCE_BUNDLE, "factionClan.text")));
        }

        // wartime modifier
        if (FactionHints.getInstance()
                  .isAtWarWith(campaign.getPlayerForce().getFaction(), person.getOriginFaction(), campaign.getLocalDate())) {
            result.add(new TargetRollModifier(4, getTextAt(RESOURCE_BUNDLE, "factionEnemy.text")));
        }
        return result;
    }

    /**
     * Calculates the combined skill values of active Admin personnel.
     *
     * @param campaign the campaign for which to calculate the combined skill values
     *
     * @return the combined skill values of active Admin personnel in the campaign
     */
    public static int getCombinedSkillValues(Campaign campaign, String skillType) {
        int combinedSkillValues = 0;

        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            boolean isAdmin = person.isAdministrator();
            if (!isAdmin) {
                continue;
            }

            PersonnelOptions options = person.getOptions();
            int mediatorModifier = options.booleanOption(ADMIN_MEDIATOR) ? 1 : 0;

            Skill skill = person.getSkill(skillType);
            if (skill == null) {
                continue;
            }

            SkillModifierData skillModifierData = person.getSkillModifierData();
            int skillLevel = skill.getTotalSkillLevel(skillModifierData);

            combinedSkillValues += skillLevel + mediatorModifier;
        }

        return combinedSkillValues;
    }

    /**
     * Calculates the management skill modifier for a person
     *
     * @param person the individual we're fetching the modifier for
     *
     * @return the management skill modifier
     */
    private static int getManagementSkillModifier(Person person) {
        if ((person.getPrimaryRole().isCivilian()) || (!person.getPrisonerStatus().isFree())) {
            return 0;
        }

        if (person.getSecondaryRole() == PersonnelRole.NONE) {
            return getCommanderManagementSkill(person.getPrimaryRole());
        } else {
            return ((getCommanderManagementSkill(person.getPrimaryRole()) +
                           getCommanderManagementSkill(person.getSecondaryRole())) / 2);
        }
    }

    /**
     * Returns the management skill modifier for a commander based on the given personnel role.
     *
     * @param role the personnel role of the person we're fetching the modifier for
     *
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
            case ADMINISTRATOR, CIVILIAN -> administrationCommanderModifier;
            case MEKWARRIOR -> mekWarriorCommanderModifier;
        };
    }

    /**
     * @param campaign the campaign to get share values for
     *
     * @return The value of each share in C-bills
     */
    public static Money getShareValue(Campaign campaign) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_SHARE_SYSTEM)) {
            return Money.zero();
        }

        Money profits = campaign.getPlayerForce().getFinances().getProfits();

        int totalShares = campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, true)
                                .stream()
                                .mapToInt(p -> p.getNumShares(campaign, campaign.getCampaignOptions().get(CampaignOption.SHARES_FOR_ALL)))
                                .sum();

        if (totalShares <= 0) {
            return Money.zero();
        }

        return profits.dividedBy(totalShares);
    }

    /**
     * Calculates the individual commander Leadership skill based on the provided commander.
     *
     * @param commander the commander for which the skill is being calculated
     *
     * @return the Leadership skill
     */
    private static int getIndividualCommanderLeadership(Person commander) {
        if (commander.hasSkill(SkillType.S_LEADER)) {
            SkillModifierData skillModifierData = commander.getSkillModifierData();

            return commander.getSkill(SkillType.S_LEADER).getTotalSkillLevel(skillModifierData);
        } else {
            return 0;
        }
    }

    /**
     * use {@link #getHRStrainModifier(Campaign)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static int getAdministrativeStrainModifier(Campaign campaign) {
        return getHRStrainModifier(campaign);
    }

    /**
     * This method calculates the combatant strain modifier based on the active personnel assigned to units.
     *
     * @param campaign the campaign for which to calculate the strain modifier
     *
     * @return the strain modifier
     */
    public static int getHRStrainModifier(Campaign campaign) {
        int personnel = getHRStrain(campaign);

        int maximumStrain = campaign.getCampaignOptions().get(CampaignOption.HR_CAPACITY) *
                                  getCombinedSkillValues(campaign, SkillType.S_ADMIN);

        // divide by zero protection - uses HR_DEFAULT_NOADMIN_PENALTY
        if (maximumStrain != 0) {
            double personnelPct = (double) personnel / maximumStrain;

            // return modifier of 1 per 100% over hr capacity limit
            if (personnelPct >= 1) {
                return (int) Math.floor(personnelPct);
            } else {
                return 0; // personnel is within capacity, no modifier
            }
        } else {
            // return penalty here on no Admin staff, based on constant
            return HR_DEFAULT_NOADMIN_PENALTY;
        }
    }

    /**
     * use {@link #getHRStrain(Campaign)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static int getAdministrativeStrain(Campaign campaign) {
        return getHRStrain(campaign);
    }

    /**
     * Computes the target for retirement rolls for all eligible personnel; this includes all active personnel who
     * aren’t dependents, prisoners, or bondsmen.
     *
     * @param mission  The contract that is being resolved; if the retirement roll is not due to contract resolutions
     *                 (e.g., &gt; 12 months since last roll), this can be null.
     * @param campaign The campaign to calculate target numbers for
     *
     * @return A map with person ids as key and calculated target roll as value.
     */
    public Map<UUID, TargetRoll> getTargetNumbers(@Nullable AbstractContract mission, final Campaign campaign) {
        final Map<UUID, TargetRoll> targets = new HashMap<>();

        if (null != mission) {
            rollRequired.add(mission.getId());
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.get(CampaignOption.USE_MANAGEMENT_SKILL)) {
            refreshManagementSkillValues(campaign);
        }

        boolean includeCivilians = campaignOptions.get(CampaignOption.INCLUDE_CIVILIANS);
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (!includeCivilians && person.isCivilian()) {
                continue;
            }

            if (person.isDeployed()) {
                continue;
            }

            if (person.isFounder()) {
                if (person.getAge(campaign.getLocalDate()) < RETIREMENT_AGE) {
                    if (!campaignOptions.get(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER)) {
                        continue;
                    }
                } else if (!campaignOptions.get(CampaignOption.USE_FOUNDER_RETIREMENT)) {
                    continue;
                }
            }

            if (campaignOptions.get(CampaignOption.USE_SUB_CONTRACT_SOLDIERS)) {
                if ((person.getUnit() != null) &&
                          (person.getUnit().usesSoldiers()) &&
                          (!person.getUnit().isCommander(person))) {
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
            if (isBreakingContract(person,
                  campaign.getLocalDate(),
                  campaignOptions.get(CampaignOption.SERVICE_CONTRACT_DURATION))) {
                targetNumber.addModifier(-campaignOptions.get(CampaignOption.SERVICE_CONTRACT_MODIFIER),
                      resources.getString("contract.text"));
            }

            // Desirability modifier
            if ((campaignOptions.get(CampaignOption.USE_SKILL_MODIFIERS)) &&
                      (person.getAge(campaign.getLocalDate()) < RETIREMENT_AGE)) {
                targetNumber.addModifier(min(EXP_ELITE - 2,
                            person.getExperienceLevel(campaignOptions,
                                  campaign.getPlayerForce().isClanForce(),
                                  campaign.getLocalDate(),
                                  false,
                                  true) - 2),
                      resources.getString("desirability.text"));
            }

            // Recent Promotion Modifier
            if (campaignOptions.get(CampaignOption.USE_TIME_IN_RANK)) {
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
            if ((campaignOptions.get(CampaignOption.USE_FATIGUE)) &&
                      (campaignOptions.get(CampaignOption.USE_FATIGUE_MODIFIERS))) {
                int fatigueModifier = Math.clamp(((person.getAdjustedFatigue() - 1) / 4) - 1, 0, 3);

                if (fatigueModifier > 0) {
                    targetNumber.addModifier(fatigueModifier, resources.getString("fatigue.text"));
                }
            }

            // HR Strain Modifiers
            if (campaignOptions.get(CampaignOption.USE_HR_STRAIN)) {
                int hrStrainModifier = getHRStrainModifier(campaign);

                if (hrStrainModifier > 0) {
                    targetNumber.addModifier(hrStrainModifier,
                          resources.getString("hrStrain.text"));
                }
            }

            // Management Skill Modifier
            if (campaignOptions.get(CampaignOption.USE_MANAGEMENT_SKILL)) {
                int modifier = getManagementSkillPenalty(person, campaign);
                targetNumber.addModifier(modifier, resources.getString("managementSkill.text"));
            }

            // Shares Modifiers
            if (campaignOptions.get(CampaignOption.USE_SHARE_SYSTEM)) {
                // If this retirement roll is not being made at the end of a contract (e.g. >12
                // months since last roll),
                // the share percentage should still apply.
                // In the case of multiple active contracts, pick the one with the best
                // percentage.
                if (mission == null) {
                    List<AbstractContract> atbContracts = campaign.getActiveContracts();

                    if (!atbContracts.isEmpty()) {
                        for (AbstractContract contract : atbContracts) {
                            if ((contract == null) || (contract.getSharesPercent() > contract.getSharesPercent())) {
                                mission = contract;
                            }
                        }
                    }
                }

                if (mission != null) {
                    targetNumber.addModifier(-max(0, ((mission.getSharesPercent() / 10) - 2)),
                          resources.getString("shares.text"));
                }
            }

            // Unit Rating modifier
            if (campaignOptions.get(CampaignOption.USE_UNIT_RATING_MODIFIERS)) {
                int unitRatingModifier = getUnitRatingModifier(campaign);
                targetNumber.addModifier(unitRatingModifier, resources.getString("unitRating.text"));
            }

            // Active Mission modifier
            if (campaignOptions.get(CampaignOption.USE_HOSTILE_TERRITORY_MODIFIERS)) {
                if (isHostileTerritory(campaign)) {
                    targetNumber.addModifier(-2, resources.getString("hostileTerritory.text"));
                }
            }

            // Mission completion status modifiers
            if ((mission != null) && (campaignOptions.get(CampaignOption.USE_MISSION_STATUS_MODIFIERS))) {
                if (mission.getStatus().isSuccess()) {
                    targetNumber.addModifier(-1, resources.getString("missionSuccess.text"));
                } else if (mission.getStatus().isFailed()) {
                    targetNumber.addModifier(1, resources.getString("missionFailure.text"));
                } else if (mission.getStatus().isBreach()) {
                    targetNumber.addModifier(2, resources.getString("missionBreach.text"));
                }
            }

            // Shares modifier: a share-heavy contract gives the crew a larger stake, suppressing turnover.
            if ((mission != null) && campaignOptions.get(CampaignOption.USE_SHARE_SYSTEM)) {
                int sharesModifier = max(0, (mission.getSharesPercent() / 10) - 2);
                if (sharesModifier > 0) {
                    targetNumber.addModifier(-sharesModifier, resources.getString("shares.text"));
                }
            }

            // Loyalty
            if ((campaignOptions.get(CampaignOption.USE_LOYALTY_MODIFIERS)) &&
                      (!campaignOptions.get(CampaignOption.USE_HIDE_LOYALTY))) {

                int loyaltyScore = person.getAdjustedLoyalty(campaign.getPlayerForce().getFaction(),
                      campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL));

                if (person.isCommander()) {
                    loyaltyScore += 2;
                }

                int loyaltyModifier = person.getLoyaltyModifier(loyaltyScore);

                if (loyaltyModifier != 0) {
                    targetNumber.addModifier(loyaltyModifier, getLoyaltyName(loyaltyModifier));
                }
            }

            // Faction Modifiers
            if (campaignOptions.get(CampaignOption.USE_FACTION_MODIFIERS)) {
                List<TargetRollModifier> factionModifiers = getFactionModifiers(person, campaign);
                factionModifiers.forEach(targetNumber::addModifier);
            }

            // Age Modifiers
            if (campaignOptions.get(CampaignOption.USE_AGE_MODIFIERS)) {
                int ageMod = getAgeMod(person.getAge(campaign.getLocalDate()));

                if (ageMod < 0) {
                    targetNumber.addModifier(ageMod, resources.getString("ageYoung.text"));
                } else if ((ageMod > 0) &&
                                 (!isBreakingContract(person,
                                       campaign.getLocalDate(),
                                       campaignOptions.get(CampaignOption.SERVICE_CONTRACT_DURATION)))) {
                    targetNumber.addModifier(ageMod, resources.getString("ageRetirement.text"));
                }
            }

            // Family Modifier
            if (campaignOptions.get(CampaignOption.USE_FAMILY_MODIFIERS)) {
                Person spouse = person.getGenealogy().getSpouse();
                List<Person> children = person.getGenealogy().getChildren();

                int modifier = 0;

                // if 'person' is married to a non-civilian, apply a -1 modifier
                if ((spouse != null) &&
                          (!spouse.getPrimaryRole().isCivilian()) &&
                          (!spouse.getStatus().isDepartedUnit())) {
                    modifier--;
                }

                // if 'person' has any non-civilian children in the unit, apply a -1 modifier
                if ((!children.isEmpty()) && (spouse == null)) {
                    if (children.stream()
                              .filter(child -> !child.isChild(campaign.getLocalDate()))
                              .anyMatch(child -> (!child.isChild(campaign.getLocalDate())) &&
                                                       (!child.getPrimaryRole().isCivilian()) &&
                                                       (!child.getStatus().isDepartedUnit()))) {
                        modifier--;
                    }
                }

                if (modifier != 0) {
                    targetNumber.addModifier(modifier, resources.getString("family.text"));
                }
            }

            // Injury Modifiers
            int injuryMod = getInjuryTurnoverModifier(person);

            if (injuryMod > 0) {
                targetNumber.addModifier(injuryMod, resources.getString("injuries.text"));
            }

            // Officer Modifiers
            if (person.getRank().isOfficer()) {
                targetNumber.addModifier(-1, resources.getString("officer.text"));
            } else {
                for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES);
                      i.hasMoreElements(); ) {
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

    public int getManagementSkillPenalty(Person person, Campaign campaign) {
        if (asfCommanderModifier == null) {
            // calculate the modifiers if they're not populated yet
            refreshManagementSkillValues(campaign);
        }
        int modifier = campaign.getCampaignOptions().get(CampaignOption.MANAGEMENT_SKILL_PENALTY);

        if (campaign.getCampaignOptions().get(CampaignOption.USE_COMMANDER_LEADERSHIP_ONLY)) {
            Person commander = campaign.getPlayerForce().getHumanResources()
                                     .getCommander(campaign.getCampaignOptions(),
                                           campaign.getPlayerForce().isClanForce(),
                                           campaign.getLocalDate());
            if (commander != null && commander.hasSkill((SkillType.S_LEADER))) {
                SkillModifierData skillModifierData = commander.getSkillModifierData(true);

                modifier -= commander.getSkill(SkillType.S_LEADER)
                                  .getTotalSkillLevel(skillModifierData);
            }
        } else {
            modifier -= getManagementSkillModifier(person);
        }
        return modifier;
    }

    /**
     * This method calculates the base target number.
     *
     * @param campaign the campaign for which the base target number is calculated
     *
     * @return the base target number
     */
    private int getBaseTargetNumber(Campaign campaign, Person person) {
        if ((campaign.getCampaignOptions().get(CampaignOption.USE_LOYALTY_MODIFIERS)) &&
                  (campaign.getCampaignOptions().get(CampaignOption.USE_HIDE_LOYALTY))) {
            int loyaltyScore = person.getAdjustedLoyalty(campaign.getPlayerForce().getFaction(),
                  campaign.getCampaignOptions().get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL));

            if (person.isCommander()) {
                loyaltyScore += 2;
            }

            int loyaltyModifier = person.getLoyaltyModifier(loyaltyScore);

            return campaign.getCampaignOptions().get(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER) + loyaltyModifier;
        } else {
            return campaign.getCampaignOptions().get(CampaignOption.TURNOVER_FIXED_TARGET_NUMBER);
        }
    }

    /**
     * Returns the unit rating modifier for the campaign.
     *
     * @param campaign the campaign from which to derive the unit rating modifier
     *
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
     * This method calculates the management skill values for the different commanding officers. Each commander's
     * management skill value is calculated based on their role and rank within the campaign. The management skill
     * modifier is calculated by adding the base modifier (retrieved from campaign options) and the commander's
     * individual leadership skill. If no suitable commander is found for a particular role, the management skill
     * modifier for that role remains the same as the base modifier.
     *
     * @param campaign The Campaign object for which to calculate the management skill values.
     */
    private void refreshManagementSkillValues(Campaign campaign) {
        for (Person person : campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isCivilian()) {
                continue;
            }

            switch (Profession.getProfessionFromPersonnelRole(person.getPrimaryRole())) {
                case AEROSPACE -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, asfCommander)) {
                        asfCommander = person;
                        asfCommanderModifier = getIndividualCommanderLeadership(asfCommander);
                    }
                }
                case VEHICLE -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, vehicleCrewCommander)) {
                        vehicleCrewCommander = person;
                        vehicleCrewCommanderModifier = getIndividualCommanderLeadership(vehicleCrewCommander);
                    }
                }
                case INFANTRY -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, infantryCommander)) {
                        infantryCommander = person;
                        infantryCommanderModifier = getIndividualCommanderLeadership(infantryCommander);
                    }
                }
                case NAVAL -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, navalCommander)) {
                        navalCommander = person;
                        navalCommanderModifier = getIndividualCommanderLeadership(navalCommander);
                    }
                }
                case TECH -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, techCommander)) {
                        techCommander = person;
                        techCommanderModifier = getIndividualCommanderLeadership(techCommander);
                    }
                }
                case MEDICAL -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, medicalCommander)) {
                        medicalCommander = person;
                        medicalCommanderModifier = getIndividualCommanderLeadership(medicalCommander);
                    }
                }
                case ADMINISTRATOR, CIVILIAN -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, administrationCommander)) {
                        administrationCommander = person;
                        administrationCommanderModifier = getIndividualCommanderLeadership(administrationCommander);
                    }
                }
                case MEKWARRIOR -> {
                    if (person.outRanksUsingSkillTiebreaker(campaign, mekWarriorCommander)) {
                        mekWarriorCommander = person;
                        mekWarriorCommanderModifier = getIndividualCommanderLeadership(mekWarriorCommander);
                    }
                }
            }
        }

        for (Profession profession : Profession.values()) {
            switch (profession) {
                case AEROSPACE -> {
                    if (asfCommander == null) {
                        asfCommanderModifier = 0;
                    }
                }
                case VEHICLE -> {
                    if (vehicleCrewCommander == null) {
                        vehicleCrewCommanderModifier = 0;
                    }
                }
                case INFANTRY -> {
                    if (infantryCommander == null) {
                        infantryCommanderModifier = 0;
                    }
                }
                case NAVAL -> {
                    if (navalCommander == null) {
                        navalCommanderModifier = 0;
                    }
                }
                case TECH -> {
                    if (techCommander == null) {
                        techCommanderModifier = 0;
                    }
                }
                case MEDICAL -> {
                    if (medicalCommander == null) {
                        medicalCommanderModifier = 0;
                    }
                }
                case ADMINISTRATOR, CIVILIAN -> {
                    if (administrationCommander == null) {
                        administrationCommanderModifier = 0;
                    }
                }
                case MEKWARRIOR -> {
                    if (mekWarriorCommander == null) {
                        mekWarriorCommanderModifier = 0;
                    }
                }
            }
        }
    }

    /**
     * @param age the age of the employee
     *
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
     * Makes rolls for Employee Turnover based on previously calculated target rolls, and tracks all retirees in the
     * unresolvedPersonnel hash in case the dialog is closed before payments are resolved, to avoid re-rolling the
     * results.
     *
     * @param mission    Nullable mission value
     * @param targets    The hash previously generated by getTargetNumbers.
     * @param shareValue The value of each share in the unit; if not using the share system, this is zero.
     * @param campaign   the current campaign
     */
    public void rollRetirement(final @Nullable AbstractContract mission, final Map<UUID, TargetRoll> targets,
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

                Person person = campaign.getPlayerForce().getHumanResources().getPerson(id);

                // if the retiree is the commander of an infantry platoon, all non-founders in
                // the platoon follow them into retirement
                if (campaign.getCampaignOptions().get(CampaignOption.USE_SUB_CONTRACT_SOLDIERS)) {
                    if ((person.getUnit() != null) &&
                              (person.getUnit().usesSoldiers()) &&
                              (person.getUnit().isCommander(person))) {
                        for (Person soldier : person.getUnit().getAllInfantry()) {
                            if ((!soldier.isFounder()) ||
                                      (campaign.getCampaignOptions().get(CampaignOption.USE_RANDOM_FOUNDER_TURNOVER))) {
                                // this shouldn't be an issue, but we include it here as insurance
                                if (!payouts.containsKey(id)) {
                                    final java.util.UUID id1 = soldier.getId();
                                    payouts.put(soldier.getId(),
                                          new Payout(campaign,
                                                campaign.getPlayerForce().getHumanResources().getPerson(id1),
                                                shareValue,
                                                false,
                                                false,
                                                campaign.getCampaignOptions().get(CampaignOption.SHARES_FOR_ALL)));
                                }
                            }
                        }

                        continue;
                    }
                }

                payouts.put(id,
                      new Payout(campaign,
                            campaign.getPlayerForce().getHumanResources().getPerson(id),
                            shareValue,
                            false,
                            false,
                            campaign.getCampaignOptions().get(CampaignOption.SHARES_FOR_ALL)));
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
     * @param contract The contract associated with the event trigger, if applicable.
     *
     * @return True if the person was successfully removed from the campaign, false otherwise.
     */
    public boolean removeFromCampaign(Person person, boolean killed, boolean sacked, Campaign campaign,
          AbstractContract contract) {
        if (!person.getPrisonerStatus().isFree()) {
            return false;
        }

        payouts.put(person.getId(),
              new Payout(campaign,
                    person,
                    getShareValue(campaign),
                    killed,
                    sacked,
                    campaign.getCampaignOptions().get(CampaignOption.SHARES_FOR_ALL)));

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

        for (UUID contractID : unresolvedPersonnel.keySet()) {
            unresolvedPersonnel.get(contractID).remove(person.getId());
        }
    }

    /**
     * Worker function that clears out any orphan Employee Turnover records
     */
    public void cleanupOrphans(Campaign campaign) {
        payouts.keySet().removeIf(personID -> {
            return campaign.getPlayerForce().getHumanResources().getPerson(personID) == null;
        });

        for (UUID contractID : unresolvedPersonnel.keySet()) {
            unresolvedPersonnel.get(contractID).removeIf(personID -> {
                return campaign.getPlayerForce().getHumanResources().getPerson(personID) == null;
            });
        }
    }

    public boolean isOutstanding(UUID id) {
        return unresolvedPersonnel.containsKey(id);
    }

    /**
     * Clears every outstanding payout, for all contracts at once. Called when the turnover dialog was opened without a
     * specific contract, so settling it settles everything.
     *
     * <p>Each contract is resolved individually before the map is emptied, so the roll-required flags go with it -
     * {@link #resolveContract(UUID)} keys off a contract id and so cannot stand in for "all of them".</p>
     */
    public void resolveAllContracts() {
        unresolvedPersonnel.keySet().forEach(this::resolveContract);
        unresolvedPersonnel.clear();
        payouts.clear();
    }

    private void resolveContract(UUID contractId) {
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

    public Set<UUID> getRetirees(final @Nullable AbstractContract mission) {
        return (mission == null) ? payouts.keySet() : unresolvedPersonnel.get(mission.getId());
    }

    public Payout getPayout(UUID id) {
        return payouts.get(id);
    }

    /**
     * @param campaign the campaign the person is a part of
     * @param person   the person to get the bonus cost for
     *
     * @return The amount in C-bills required to get a bonus to the Employee Turnover roll
     */
    public static Money getPayoutOrBonusValue(final Campaign campaign, Person person) {
        double bonusMultiplier = campaign.getCampaignOptions().get(CampaignOption.PAYOUT_RATE_ENLISTED);

        if (person.getRank().isOfficer()) {
            bonusMultiplier = campaign.getCampaignOptions().get(CampaignOption.PAYOUT_RATE_OFFICER);
        }

        if (campaign.getCampaignOptions().get(CampaignOption.USE_PAYOUT_SERVICE_BONUS)) {
            bonusMultiplier += person.getYearsInService(campaign) *
                                     ((double) campaign.getCampaignOptions().get(CampaignOption.PAYOUT_SERVICE_BONUS_RATE) / 100);
        }

        return person.getSalary(campaign).multipliedBy(bonusMultiplier);
    }

    /**
     * Returns the number of permanent, non-prosthetic injuries for turnover modifier calculation.
     *
     * <p>Prosthetics and implants are excluded because they are elective modifications,
     * not debilitating injuries that would cause a person to leave a unit.</p>
     *
     * @param person the person to evaluate
     *
     * @return count of permanent injuries excluding prosthetics and implants
     */
    static int getInjuryTurnoverModifier(final Person person) {
        return (int) person.getInjuries().stream()
                           .filter(i -> !i.getSubType().isPermanentModification())
                           .filter(Injury::isPermanent)
                           .count();
    }

    /**
     * Returns whether a person has permanent injuries (excluding prosthetics/implants) that qualify them for medical
     * discharge.
     *
     * @param person the person to evaluate
     *
     * @return {@code true} if the person has at least one permanent non-prosthetic injury
     */
    static boolean hasMedicalDischargeInjuries(final Person person) {
        return person.getInjuries().stream()
                     .filter(i -> !i.getSubType().isPermanentModification())
                     .anyMatch(Injury::isPermanent);
    }

    /**
     * Class used to record the required payout to each retired/defected/killed/sacked person.
     */
    public static class Payout {
        private int weightClass = 0;
        private Money payoutAmount = Money.zero();
        private boolean wasKilled = false;
        private boolean wasSacked = false;

        public Payout() {

        }

        public Payout(final Campaign campaign, final Person person, final Money shareValue, final boolean killed,
              final boolean sacked, final boolean sharesForAll) {
            if (killed) {
                setWasKilled(true);
            } else if (sacked) {
                setWasSacked(true);
            }

            calculatePayout(campaign, person, killed, sacked, shareValue.isPositive());

            if ((shareValue.isPositive()) && (campaign.getCampaignOptions().get(CampaignOption.USE_SHARE_SYSTEM))) {
                payoutAmount = payoutAmount.plus(shareValue.multipliedBy(person.getNumShares(campaign, sharesForAll)));
            }
        }

        private void calculatePayout(final Campaign campaign, final Person person, final boolean killed,
              final boolean sacked, final boolean shareSystem) {
            final Profession profession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());

            // person was killed
            if (killed) {
                payoutAmount = getPayoutOrBonusValue(campaign, person).multipliedBy(campaign.getCampaignOptions()
                                                                                          .get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER));
                // person is getting medically discharged
            } else if (hasMedicalDischargeInjuries(person)) {
                payoutAmount = getPayoutOrBonusValue(campaign, person).multipliedBy(campaign.getCampaignOptions()
                                                                                          .get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER));
                // person is defecting
            } else if (isBreakingContract(person,
                  campaign.getLocalDate(),
                  campaign.getCampaignOptions().get(CampaignOption.SERVICE_CONTRACT_DURATION))) {
                payoutAmount = Money.of(0);
                // person is retiring
            } else if (person.getAge(campaign.getLocalDate()) >= RETIREMENT_AGE) {
                payoutAmount = getPayoutOrBonusValue(campaign, person).multipliedBy(campaign.getCampaignOptions()
                                                                                          .get(CampaignOption.PAYOUT_RETIREMENT_MULTIPLIER));
                // person was sacked
            } else if (sacked) {
                payoutAmount = Money.of(0);
                // person is resigning
            } else {
                payoutAmount = getPayoutOrBonusValue(campaign, person);
            }

            if (!shareSystem &&
                      (profession.isMekWarrior() || profession.isAerospace()) &&
                      (person.getOriginalUnitWeight() > 0)) {
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
            LocalDate recruitmentDate = person.getRecruitment();

            // There is no contract to break
            if (recruitmentDate == null) {
                return false;
            }

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
        for (UUID id : unresolvedPersonnel.keySet()) {
            MHQXMLUtility.writeSimpleXMLAttributedTag(pw,
                  indent,
                  "contract",
                  "id",
                  id,
                  createCsv(unresolvedPersonnel.get(id)));
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

    /**
     * Parses a contract reference, returning {@code null} when the value is not a {@link UUID} (i.e. a legacy integer
     * mission id from a pre-UUID save) rather than throwing.
     */
    private static @Nullable UUID parseMissionId(final String text) {
        if ((text == null) || text.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(text.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Parses a legacy integer mission id, or {@code null} when the value is not an integer. */
    private static @Nullable Integer parseLegacyMissionId(final String text) {
        if ((text == null) || text.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Re-hooks contract references loaded from a pre-UUID save onto the contracts those legacy missions were converted
     * into. Called by the campaign loader once every mission has been read, so it does not matter whether the tracker
     * or the missions were parsed first. References whose mission was not converted are discarded (the tracker's own
     * orphan-record cleanup would drop them anyway).
     *
     * @param legacyMissionIdMap legacy integer mission id to converted contract {@link UUID}
     *
     * @return how many references were held and how many of them were re-hooked
     */
    public LegacyRelinkResult relinkLegacyMissionIds(final Map<Integer, UUID> legacyMissionIdMap) {
        final int attempted = legacyRollRequired.size() + legacyUnresolvedPersonnel.size();
        int relinked = 0;

        for (final Integer legacyId : legacyRollRequired) {
            final UUID missionId = legacyMissionIdMap.get(legacyId);
            if (missionId != null) {
                rollRequired.add(missionId);
                relinked++;
            }
        }
        legacyRollRequired.clear();

        for (final Map.Entry<Integer, HashSet<UUID>> entry : legacyUnresolvedPersonnel.entrySet()) {
            final UUID missionId = legacyMissionIdMap.get(entry.getKey());
            if (missionId != null) {
                unresolvedPersonnel.put(missionId, entry.getValue());
                relinked++;
            }
        }
        legacyUnresolvedPersonnel.clear();

        return new LegacyRelinkResult(attempted, relinked);
    }

    /**
     * The outcome of a {@link #relinkLegacyMissionIds(Map)} pass.
     *
     * <p>Both counts are reported because they differ whenever a legacy reference has no converted contract to hook
     * onto; a caller that logs only the successes cannot tell how many were dropped.</p>
     *
     * @param attempted the number of legacy references the tracker held
     * @param relinked  how many of them resolved to a converted contract
     */
    public record LegacyRelinkResult(int attempted, int relinked) {}

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
                            // Pre-UUID saves stored a legacy integer mission id here; hold it for re-hooking rather
                            // than throwing (which would abort the rest of the tracker's parse).
                            UUID missionId = parseMissionId(id);
                            if (missionId != null) {
                                retVal.rollRequired.add(missionId);
                            } else {
                                Integer legacyId = parseLegacyMissionId(id);
                                if (legacyId != null) {
                                    retVal.legacyRollRequired.add(legacyId);
                                }
                            }
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
                            String rawId = wn3.getAttributes().getNamedItem("id").getTextContent();
                            String[] ids = wn3.getTextContent().split(",");
                            HashSet<UUID> pids = Arrays.stream(ids)
                                                       .map(UUID::fromString)
                                                       .collect(Collectors.toCollection(HashSet::new));

                            UUID id = parseMissionId(rawId);
                            if (id != null) {
                                retVal.unresolvedPersonnel.put(id, pids);
                            } else {
                                Integer legacyId = parseLegacyMissionId(rawId);
                                if (legacyId != null) {
                                    retVal.legacyUnresolvedPersonnel.put(legacyId, pids);
                                }
                            }
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
            LOGGER.error(
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
