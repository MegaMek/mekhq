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

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionTracker");

    public RetirementDefectionTracker() {
        rollRequired = new HashSet<>();
        unresolvedPersonnel = new HashMap<>();
        payouts = new HashMap<>();
        lastRetirementRoll = LocalDate.now();
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
     * Computes the target for retirement rolls for all eligible personnel; this includes
     * all active personnel who are not dependents, prisoners, or bondsmen.
     *
     * @param contract The contract that is being resolved; if the retirement roll is not due to
     *                 contract resolutions (e.g., &gt; 12 months since last roll), this can be null.
     * @param campaign  The campaign to calculate target numbers for
     * @return A map with person ids as key and calculated target roll as value.
     */
    public Map<UUID, TargetRoll> calculateTargetNumbers(final @Nullable AtBContract contract, final Campaign campaign) {
        final Map <UUID, TargetRoll> targets = new HashMap<>();

        if (null != contract) {
            rollRequired.add(contract.getId());
        }

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrimaryRole().isDependent()) || (!person.getPrisonerStatus().isFree()) || (person.isDeployed())) {
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

            TargetRoll targetNumber = new TargetRoll(getBaseTargetNumber(campaign), resources.getString("base.text"));

            // Skill Rating modifier
            if (campaign.getCampaignOptions().isUseSkillModifiers()) {
                int skillRating;
                String skillRatingDescription;

                try {
                    skillRating = person.getExperienceLevel(campaign, true);
                } catch (Exception e) {
                    skillRating = -1;
                }

                switch (skillRating) {
                    case -1:
                        targetNumber.addModifier(0, resources.getString("skillUnskilled.text"));
                        break;
                    case 0:
                        targetNumber.addModifier(skillRating, resources.getString("skillUltraGreen.text"));
                        break;
                    case 1:
                        targetNumber.addModifier(skillRating, resources.getString("skillGreen.text"));
                        break;
                    case 2:
                        targetNumber.addModifier(skillRating, resources.getString("skillRegular.text"));
                        break;
                    case 3:
                        targetNumber.addModifier(skillRating, resources.getString("skillVeteran.text"));
                        break;
                    case 4:
                        targetNumber.addModifier(skillRating, resources.getString("skillElite.text"));
                        break;
                    default:
                        LogManager.getLogger().error("RetirementDefectionTracker: Unable to parse skillRating. Returning {}", skillRating);
                }
            }

            // Unit Rating modifier
            if (campaign.getCampaignOptions().isUseUnitRatingModifiers()) {
                int unitRatingModifier = getUnitRatingModifier(campaign);
                targetNumber.addModifier(unitRatingModifier, resources.getString("unitRating.text"));
            }

            // Mission completion status modifiers
            if ((contract != null) && (campaign.getCampaignOptions().isUseMissionStatusModifiers())) {
                if (contract.getStatus().isSuccess()) {
                    targetNumber.addModifier(-1, resources.getString("missionSuccess.text"));
                } else if (contract.getStatus().isFailed()) {
                    targetNumber.addModifier(1, resources.getString("missionFailure.text"));
                } else if (contract.getStatus().isBreach()) {
                    targetNumber.addModifier(2, resources.getString("missionBreach.text"));
                }
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

            // Shares Modifiers
            if (campaign.getCampaignOptions().isUseShareSystem()) {
                /* If this retirement roll is not being made at the end
                 * of a contract (e.g. >12 months since last roll), the
                 * share percentage should still apply. In the case of multiple
                 * active contracts, pick the one with the best percentage.
                 */
                AtBContract c = contract;
                if (c == null) {
                    for (AtBContract atBContract : campaign.getActiveAtBContracts()) {
                        if ((c == null) || (c.getSharesPct() < atBContract.getSharesPct())) {
                            c = atBContract;
                        }
                    }
                }
                if (c != null) {
                    targetNumber.addModifier(- (c.getSharesPct() / 10), resources.getString("shares.text"));
                }
            }

            // Administrative Strain Modifiers
            if (campaign.getCampaignOptions().isUseAdministrativeStrain()) {
                int nonCombatantStrainModifier = getNonCombatantStrainModifier(campaign);
                int combatantStrainModifier = getCombatantStrainModifier(campaign);

                if ((nonCombatantStrainModifier > 0) && (person.getUnit() == null)) {
                    targetNumber.addModifier(nonCombatantStrainModifier, resources.getString("administrativeStrain.text"));
                }

                if ((combatantStrainModifier > 0) && (person.getUnit() != null)) {
                    targetNumber.addModifier(combatantStrainModifier, resources.getString("administrativeStrain.text"));
                }
            }

            // Fatigue Modifiers
            if (campaign.getCampaignOptions().isTrackUnitFatigue()) {
                targetNumber.addModifier(campaign.getFatigueLevel() / 10, resources.getString("fatigue.text"));
            }

            targets.put(person.getId(), targetNumber);
        }
        return targets;
    }

    /**
     * This method calculates the combatant strain modifier based on the active personnel assigned to units.
     *
     * @param campaign the campaign for which to calculate the strain modifier
     * @return the strain modifier
     */
    private static int getCombatantStrainModifier(Campaign campaign) {
        int combatants = 0;
        int proto = 0;

        for (Person person : campaign.getActivePersonnel()) {
            if (person.getPrimaryRole().isCivilian() || !person.getPrisonerStatus().isFree()) {
                continue;
            }

            // personnel without a unit are treated as non-combatants
            if (person.getUnit() != null) {
                // we treat multi-crewed units as one, for Administrative Strain,
                // as otherwise users would be penalized for their use
                if (person.getUnit().isCommander(person)) {
                    if (person.getUnit().getEntity().isProtoMek()) {
                        proto++;
                    } else {
                        combatants++;
                    }
                }
            }
        }

        combatants += proto / 5;

        int maximumStrain = campaign.getCampaignOptions().getCombatantStrain();

        int skillLevel = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_COMMAND, SkillType.S_ADMIN)
                .getSkill(SkillType.S_ADMIN)
                .getLevel();

        return (maximumStrain * skillLevel) / combatants;
    }

    /**
     * This method calculates the non-combatant strain modifier based on the active personnel not assigned to units.
     *
     * @param campaign the campaign for which to calculate the strain modifier
     * @return the strain modifier
     */
    private int getNonCombatantStrainModifier(Campaign campaign) {
        int nonCombatants = (int) campaign.getActivePersonnel().stream()
                .filter(person -> !person.getPrimaryRole().isCivilian() && person.getPrisonerStatus().isFree())
                .filter(person -> person.getUnit() == null).count();

        int maximumStrain = campaign.getCampaignOptions().getNonCombatantStrain();

        int skillLevel = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, SkillType.S_ADMIN)
                .getSkill(SkillType.S_ADMIN)
                .getLevel();

        return (maximumStrain * skillLevel) / nonCombatants;
    }

    /**
     * This method calculates the base target number.
     *
     * @param campaign the campaign for which the base target number is calculated
     * @return the base target number
     */
    private static Integer getBaseTargetNumber(Campaign campaign) {
        try {
            if (campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isAdministration()) {
                return campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, SkillType.S_ADMIN)
                        .getSkill(SkillType.S_ADMIN)
                        .getFinalSkillValue();
            } else if (campaign.getCampaignOptions().getTurnoverTargetNumberMethod().isNegotiation()) {
                return campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, SkillType.S_NEG)
                        .getSkill(SkillType.S_NEG)
                        .getFinalSkillValue();
            } else {
                return campaign.getCampaignOptions().getTurnoverFixedTargetNumber();
            }
        // this means there isn't someone in the campaign with the relevant skill or role
        } catch (Exception e) {
            return 13;
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
     * Makes rolls for Employee Turnover based on previously calculated target rolls,
     * and tracks all retirees in the unresolvedPersonnel hash in case the dialog
     * is closed before payments are resolved, to avoid re-rolling the results.
     *
     * @param mission Nullable mission value
     * @param targets The hash previously generated by calculateTargetNumbers.
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
