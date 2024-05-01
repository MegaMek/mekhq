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
import mekhq.utilities.MHQXMLUtility;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;

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
     * and determining payouts but before the retirees have been paid,
     * we store those results to avoid making the rolls again.
     */
    private Set<Integer> rollRequired;
    private Map<Integer, HashSet<UUID>> unresolvedPersonnel;
    private Map<UUID, Payout> payouts;
    private LocalDate lastRetirementRoll;

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

        int totalShares = 0;
        for (Person p : campaign.getActivePersonnel()) {
            totalShares += p.getNumShares(campaign, campaign.getCampaignOptions().isSharesForAll());
        }

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
     *                 contract resolutions (e.g. &gt; 12 months since last roll), this can be null.
     * @param campaign  The campaign to calculate target numbers for
     * @return A map with person ids as key and calculated target roll as value.
     */
    public Map<UUID, TargetRoll> calculateTargetNumbers(final @Nullable AtBContract contract,
                                                        final Campaign campaign) {
        final Map <UUID, TargetRoll> targets = new HashMap<>();
        int combatLeadershipMod = 0;
        int supportLeadershipMod = 0;

        if (null != contract) {
            rollRequired.add(contract.getId());
        }

        if (campaign.getCampaignOptions().isUseLeadership()) {
            int combat = 0;
            int proto = 0;
            int support = 0;
            for (Person p : campaign.getActivePersonnel()) {
                if (p.getPrimaryRole().isCivilian() || !p.getPrisonerStatus().isFree()) {
                    continue;
                }

                if (p.getPrimaryRole().isSupport()) {
                    support++;
                } else if ((null == p.getUnit()) ||
                        ((null != p.getUnit()) && p.getUnit().isCommander(p))) {
                    /*
                     * The AtB rules do not state that crews count as a
                     * single person for leadership purposes, but to do otherwise
                     * would tax all but the most exceptional commanders of
                     * vehicle or infantry units.
                     */
                    if (p.getPrimaryRole().isProtoMechPilot()) {
                        proto++;
                    } else {
                        combat++;
                    }
                }
            }
            combat += proto / 5;
            int max = 12;
            if ((null != campaign.getFlaggedCommander()) &&
                    (null != campaign.getFlaggedCommander().getSkill(SkillType.S_LEADER))) {
                max += 6 * campaign.getFlaggedCommander().getSkill(SkillType.S_LEADER).getLevel();
            }

            if (combat > 2 * max) {
                combatLeadershipMod = 2;
            } else if (combat > max) {
                combatLeadershipMod = 1;
            }

            if (support > 2 * max) {
                supportLeadershipMod = 2;
            } else if (support > max) {
                supportLeadershipMod = 1;
            }
        }

        for (Person p : campaign.getActivePersonnel()) {
            if (p.getPrimaryRole().isDependent() || !p.getPrisonerStatus().isFree() || p.isDeployed() || (p.isFounder() && !campaign.getCampaignOptions().isUseRandomFounderRetirement())) {
                continue;
            }

            /* Infantry units retire or defect by platoon */
            if ((null != p.getUnit()) && p.getUnit().usesSoldiers() && !p.getUnit().isCommander(p)) {
                continue;
            }

            TargetRoll target = new TargetRoll(3, "Base");

            // Skill Rating modifier
            int skillRating = p.getExperienceLevel(campaign, false);
            String skillRatingDescription;

            switch (skillRating) {
                case -1:
                    skillRatingDescription = "Unskilled";
                    break;
                case 0:
                    skillRatingDescription = "Ultra-Green";
                    break;
                case 1:
                    skillRatingDescription = "Green";
                    break;
                case 2:
                    skillRatingDescription = "Regular";
                    break;
                case 3:
                    skillRatingDescription = "Veteran";
                    break;
                case 4:
                    skillRatingDescription = "Elite";
                    break;
                default:
                    skillRatingDescription = "Error, please see log";
                    LogManager.getLogger().error("RetirementDefectionTracker: Unable to parse skillRating. Returning " + skillRating);
            }

            target.addModifier(skillRating, skillRatingDescription);

            // Unit Rating modifier
            int unitRating = 0;

            if (campaign.getUnitRatingMod() < 1) {
                unitRating = 2;
            } else if (campaign.getUnitRatingMod() == 1) {
                unitRating = 1;
            } else if (campaign.getUnitRatingMod() > 3) {
                unitRating = -1;
            }

            target.addModifier(unitRating, "Unit Rating");

            /* Retirement rolls are made before the contract status is set */
            if ((contract != null) && (contract.getStatus().isFailed() || contract.getStatus().isBreach())) {
                target.addModifier(1, "Failed mission");
            }

            // Fatigue Modifiers
            if (campaign.getCampaignOptions().isTrackUnitFatigue() && (campaign.getFatigueLevel() >= 10)) {
                target.addModifier(campaign.getFatigueLevel() / 10, "Fatigue");
            }

            // Faction Modifiers
            if (campaign.getFaction().isPirate()) {
                target.addModifier(1, "Pirate Company");
            } else if (p.getOriginFaction().isPirate()) {
                target.addModifier(1,"Pirate");
            }

            if (p.getOriginFaction().isMercenary()) {
                target.addModifier(1, "Mercenary");
            }

            if (p.getOriginFaction().isClan()) {
                target.addModifier(-2, "Clan");
            }

            // Officer Modifiers
            if (p.getRank().isOfficer()) {
                target.addModifier(-1, "Officer");
            } else {
                for (Enumeration<IOption> i = p.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
                    IOption ability = i.nextElement();
                    if (ability.booleanValue()) {
                        if (ability.getName().equals("tactical_genius")) {
                            target.addModifier(1, "Non-officer tactical genius");
                            break;
                        }
                    }
                }
            }

            // Old Age modifier
            int age = p.getAge(campaign.getLocalDate());
            int ageMod = getAgeMod(age);

            if (ageMod > 0) {
                target.addModifier(ageMod, "Age");
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
                    target.addModifier(-((c.getSharesPct() - 20) / 10), "Shares");
                }
            }

            // Role Modifiers
            if (p.getPrimaryRole().isSoldier()) {
                target.addModifier(-1, p.getPrimaryRole().toString());
            }

            // Injury Modifiers
            int injuryMod = 0;
            for (Injury i : p.getInjuries()) {
                if (i.isPermanent()) {
                    injuryMod++;
                }
            }

            if (injuryMod > 0) {
                target.addModifier(injuryMod, "Permanent Injuries");
            }

            // Leadership Modifiers
            if ((combatLeadershipMod != 0) && p.getPrimaryRole().isCombat()) {
                target.addModifier(combatLeadershipMod, "Leadership (Combatant)");
            }

            if ((supportLeadershipMod != 0) && p.getPrimaryRole().isSupport()) {
                target.addModifier(supportLeadershipMod, "Leadership (Support)");
            }

            targets.put(p.getId(), target);
        }
        return targets;
    }

  /**
     * Makes rolls for Employee Turnover based on previously calculated target rolls,
     * and tracks all retirees in the unresolvedPersonnel hash in case the dialog
     * is closed before payments are resolved, to avoid re-rolling the results.
     *
     * @param mission Nullable mission value
     * @param targets The hash previously generated by calculateTargetNumbers.
     * @param shareValue The value of each share in the unit; if not using the share system, this is zero.
     * @param campaign
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
     * @param campaign
     * @param contract If not null, the payout must be resolved before the contract can be resolved.
     * @return true if the person is due a payout; otherwise false
     */
    public boolean removeFromCampaign(Person person, boolean killed, Campaign campaign,
                                      AtBContract contract) {
        /* Payouts to Infantry/Battle armor platoons/squads/points are
         * handled as a unit in the AtB rules, so we're just going to ignore
         * them here.
         */
        if (person.getPrimaryRole().isSoldierOrBattleArmour() || !person.getPrisonerStatus().isFree()) {
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

    /* Called by when all payouts have been resolved for the contract.
     * If contract is null, the dialog has been invoked without a
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
    public static Money getBonusCost(final Campaign campaign, Person person) {
        return person.getSalary(campaign).multipliedBy(24);
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

        public Payout(final Campaign campaign, final Person person, final Money shareValue,
                      final boolean killed, final boolean sharesForAll) {
            calculatePayout(campaign, person, killed, shareValue.isPositive());
            if (shareValue.isPositive()) {
                payoutAmount = payoutAmount.plus(shareValue.multipliedBy(person.getNumShares(campaign, sharesForAll)));
            }
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

        private void calculatePayout(final Campaign campaign, final Person person,
                                     final boolean killed, final boolean shareSystem) {
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
                if (profession.isInfantry()) {
                    if (person.getUnit() != null) {
                        payoutAmount = Money.of(50000);
                    }
                } else {
                    payoutAmount = getBonusCost(campaign, person);
                }

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
                            HashSet<UUID> pids = new HashSet<>();
                            String [] ids = wn3.getTextContent().split(",");
                            for (String s : ids) {
                                pids.add(UUID.fromString(s));
                            }
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
