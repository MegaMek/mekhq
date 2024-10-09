/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import megamek.common.Compute;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Faction.Tag;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.enums.HiringHallLevel;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contract Market as described in Campaign Operations, 4th printing.
 */
public class CamOpsContractMarket extends AbstractContractMarket {
    private static final MMLogger logger = MMLogger.create(CamOpsContractMarket.class);
    private static int BASE_NEGOTIATION_TARGET = 8;
    private static int EMPLOYER_NEGOTIATION_SKILL_LEVEL = 5;

    public CamOpsContractMarket() {
        super(ContractMarketMethod.CAM_OPS);
    }

    @Override
    public AtBContract addAtBContract(Campaign campaign) {
        HiringHallModifiers hiringHallModifiers = getHiringHallModifiers(campaign);
        ReputationController reputation = campaign.getReputation();
        Optional<AtBContract> c = generateContract(campaign, reputation, hiringHallModifiers);
        if (c.isPresent()) {
            AtBContract atbContract = c.get();
            contracts.add(atbContract);
            return atbContract;
        }
        return null;
    }

    @Override
    public void generateContractOffers(Campaign campaign, boolean newCampaign) {
        if (!(campaign.getLocalDate().getDayOfMonth() == 1) && !newCampaign) {
            return;
        }
        new ArrayList<>(contracts).forEach(this::removeContract);
        // TODO: Allow subcontracts?
        //for (AtBContract contract : campaign.getActiveAtBContracts()) {
            //checkForSubcontracts(campaign, contract, unitRatingMod);
        //}
        // TODO: CamopsMarket: allow players to choose negotiators and send them out, removing them
        // from other tasks they're doing. For now just use the highest negotiation skill on the force.
        int ratingMod = campaign.getReputation().getReputationModifier();
        HiringHallModifiers hiringHallModifiers = getHiringHallModifiers(campaign);
        int negotiationSkill = findNegotiationSkill(campaign);
        int numOffers = getNumberOfOffers(
            rollNegotiation(negotiationSkill, ratingMod + hiringHallModifiers.offersMod) - BASE_NEGOTIATION_TARGET);

        for (int i = 0; i < numOffers; i++) {
            addAtBContract(campaign);
        }
        updateReport(campaign);
    }

    @Override
    public double calculatePaymentMultiplier(Campaign campaign, AtBContract contract) {
        Faction employer = contract.getEmployerFaction();
        int reputationFactor = campaign.getReputation().getReputationFactor();
        ContractTerms terms = new ContractTerms(contract.getContractType(), employer,
            reputationFactor, campaign.getLocalDate());
        return terms.getEmploymentMultiplier() * terms.getOperationsTempoMultiplier() * reputationFactor;
    }

    @Override
    public void checkForFollowup(Campaign campaign, AtBContract contract) {

    }

    private HiringHallModifiers getHiringHallModifiers(Campaign campaign) {
        HiringHallModifiers modifiers;
        if (campaign.getFaction().isMercenary()) {
            modifiers = new HiringHallModifiers(campaign.getSystemHiringHallLevel());
        } else if (campaign.getFaction().isGovernment()) {
            modifiers = new HiringHallModifiers(HiringHallLevel.GREAT);
        } else {
            modifiers = new HiringHallModifiers(HiringHallLevel.NONE);
        }
        return modifiers;
    }

    private int findNegotiationSkill(Campaign campaign) {
        // TODO: have pirates use investigation skill instead when it is implemented per CamOps
        Person negotiator = campaign.findBestAtSkill(SkillType.S_NEG);
        if (negotiator == null) {
            return 0;
        }
        return negotiator.getSkillLevel(SkillType.S_NEG);
    }

    private int rollNegotiation(int skill, int modifiers) {
        return Compute.d6(2) + skill + modifiers;
    }

    private int rollOpposedNegotiation(int skill, int modifiers) {
        return Compute.d6(2) + skill + modifiers - Compute.d6(2) + EMPLOYER_NEGOTIATION_SKILL_LEVEL;
    }

    private int getNumberOfOffers(int margin) {
        if (margin < 1) {
            return 0;
        } else if (margin < 3) {
            return 1;
        } else if (margin < 6) {
            return 2;
        } else if (margin < 9) {
            return 3;
        } else if (margin < 11) {
            return 4;
        } else if (margin < 13) {
            return 5;
        } else {
            return 6;
        }
    }

    private Optional<AtBContract> generateContract(Campaign campaign, ReputationController reputation,
                                                   HiringHallModifiers hiringHallModifiers) {
        AtBContract contract = new AtBContract("UnnamedContract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);
        // Step 1: Determine Employer
        Faction employer = determineEmployer(campaign, reputation.getReputationModifier(), hiringHallModifiers);
        contract.setEmployerCode(employer.getShortName(), campaign.getLocalDate());
        if (employer.isMercenary()) {
            contract.setMercSubcontract(true);
        }
        // Step 2: Determine the mission type
        contract.setContractType(determineMission(campaign, employer, reputation.getReputationModifier()));
        ContractTerms contractTerms = new ContractTerms(contract.getContractType(),
            employer, reputation.getReputationFactor(), campaign.getLocalDate());
        setEnemyCode(contract);
        setAttacker(contract);
        // Step 3: Set the system location
        try {
            setSystemId(contract);
        } catch (NoContractLocationFoundException ex) {
            return Optional.empty();
        }
        // Step 4: Populate some information about enemies and allies
        setAllyRating(contract, campaign.getGameYear());
        setEnemyRating(contract, campaign.getGameYear());
        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(SkillLevel.GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }
        // Step 5: Determine the contract length (Not CamOps RAW)
        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        // Step 6: Determine the initial contract clauses
        setContractClauses(contract, contractTerms);
        // Step 7: Determine the number of required lances (Not CamOps RAW)
        contract.setRequiredLances(calculateRequiredLances(campaign, contract));
        // Step 8: Calculate the payment
        contract.setMultiplier(calculatePaymentMultiplier(campaign, contract));
        // Step 9: Determine parts availability
        // TODO: Rewrite this to be CamOps-compliant
        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());
        // Step 10: Finish up contract initialization
        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);
        contract.setName(String.format("%s - %s - %s %s",
            contract.getStartDate().format(DateTimeFormatter.ofPattern("yyyy")
                .withLocale(MekHQ.getMHQOptions().getDateLocale())), contract.getEmployer(),
            contract.getSystem().getName(contract.getStartDate()), contract.getContractType()));

        return Optional.of(contract);
    }

    private Faction determineEmployer(Campaign campaign, int ratingMod, HiringHallModifiers hiringHallModifiers) {
        Collection<Tag> employerTags;
        int roll = Compute.d6(2) + ratingMod + hiringHallModifiers.employersMod;
        if (roll < 6) {
            // Roll again on the independent employers column
            roll = Compute.d6(2) + ratingMod + hiringHallModifiers.employersMod;
            employerTags = getEmployerTags(campaign, roll, true);
        } else {
            employerTags = getEmployerTags(campaign, roll, false);
        }
        return getRandomEmployer(campaign, employerTags);
    }

    private Faction getRandomEmployer(Campaign campaign, Collection<Tag> employerTags) {
        Collection<Faction> factions = Factions.getInstance().getActiveFactions(campaign.getLocalDate());
        List<Faction> filtered = new ArrayList<>();
        for (Faction faction : factions) {
            // Clans only hire units within their own clan
            if (faction.isClan() && !faction.equals(campaign.getFaction())) {
                continue;
            }
            for (Tag employerTag : employerTags) {
                if (!faction.is(employerTag)) {
                    // The SMALL tag has to be converted to independent for now, since for some reason
                    // independent is coded as a string.
                    if (employerTag == Tag.SMALL && faction.isIndependent()) {
                        continue;
                    }
                    break;
                }
                filtered.add(faction);
            }
        }
        Random rand  = new Random();
        return filtered.get(rand.nextInt(filtered.size()));
    }

    private Collection<Tag> getEmployerTags(Campaign campaign, int roll, boolean independent) {
        Collection<Tag> tags = new ArrayList<>();
        if (independent) {
            tags.add(Tag.SMALL);
            if (roll < 4) {
                tags.add(Tag.NOBLE);
            } else if (roll < 6) {
                tags.add(Tag.PLANETARY_GOVERNMENT);
            } else if (roll == 6) {
                tags.add(Tag.MERC);
            } else if (roll < 9) {
                tags.add(Tag.PERIPHERY);
                tags.add(Tag.MAJOR);
            } else if (roll < 11) {
                tags.add(Tag.PERIPHERY);
                tags.add(Tag.MINOR);
            } else {
                tags.add(Tag.CORPORATION);
            }
        } else {
            if (roll < 6) {
                tags.add(Tag.SMALL);
            } else if (roll < 8) {
                tags.add(Tag.MINOR);
            } else if (roll < 11) {
                tags.add(Tag.MAJOR);
            } else {
                if (Factions.getInstance()
                    .getActiveFactions(campaign.getLocalDate())
                    .stream()
                    .anyMatch(Faction::isSuperPower)) {
                        tags.add(Tag.SUPER);
                } else {
                    tags.add(Tag.MAJOR);
                }
            }
        }
        return tags;
    }

    private AtBContractType determineMission(Campaign campaign, Faction employer, int ratingMod) {
        if (campaign.getFaction().isPirate()) {
            return MissionSelector.getPirateMission(Compute.d6(2), 0);
        }
        int margin = rollNegotiation(findNegotiationSkill(campaign),
            ratingMod + getHiringHallModifiers(campaign).missionsMod) - BASE_NEGOTIATION_TARGET;
        boolean isClan = campaign.getFaction().isClan();
        if (employer.isInnerSphere() || employer.isClan()) {
            return MissionSelector.getInnerSphereClanMission(Compute.d6(2), margin, isClan);
        } else if (employer.isIndependent() || employer.isPlanetaryGovt()) {
            return MissionSelector.getIndependentMission(Compute.d6(2), margin, isClan);
        } else if (employer.isCorporation()) {
            return MissionSelector.getCorporationMission(Compute.d6(2), margin, isClan);
        } else {
            logger.warn("No matching employer on Missions table; defaulting to IS/Clan");
            return MissionSelector.getInnerSphereClanMission(Compute.d6(2), margin, isClan);
        }
    }

    private void setContractClauses(AtBContract contract, ContractTerms terms) {
        setCommandRights(contract, terms);
        setSalvageRights(contract, terms);
        setSupportRights(contract, terms);
        setTransportRights(contract, terms);
    }

    private void setCommandRights(AtBContract contract, ContractTerms terms) {
        int roll = Compute.d6(2);
        contract.setCommandRights(terms.getCommandRights(roll));
    }

    private void setSalvageRights(AtBContract contract, ContractTerms terms) {
        int roll = Compute.d6(2);
        if (terms.isSalvageExchange(roll)) {
            contract.setSalvageExchange(true);
        } else {
            contract.setSalvageExchange(false);
            contract.setSalvagePct(terms.getSalvagePercentage(roll));
        }
    }

    private void setSupportRights(AtBContract contract, ContractTerms terms) {
        int roll = Compute.d6(2);
        if (terms.isStraightSupport(roll)) {
            contract.setStraightSupport(terms.getSupportPercentage(roll));
        } else if (terms.isBattleLossComp(roll)) {
            contract.setBattleLossComp(terms.getSupportPercentage(roll));
        } else {
            contract.setStraightSupport(0);
        }
    }

    private void setTransportRights(AtBContract contract, ContractTerms terms) {
        int roll = Compute.d6(2);
        contract.setTransportComp(terms.getTransportTerms(roll));
    }

    private static class HiringHallModifiers {
        protected int offersMod;
        protected int employersMod;
        protected int missionsMod;

        protected HiringHallModifiers(HiringHallLevel level) {
            switch (level) {
                case NONE -> {
                    offersMod = -3;
                    employersMod = -2;
                    missionsMod = -2;
                }
                case QUESTIONABLE -> {
                    offersMod = 0;
                    employersMod = -2;
                    missionsMod = -2;
                }
                case MINOR -> {
                    offersMod = 1;
                    employersMod = 0;
                    missionsMod = 0;
                }
                case STANDARD -> {
                    offersMod = 2;
                    employersMod = 1;
                    missionsMod = 1;
                }
                case GREAT -> {
                    offersMod = 3;
                    employersMod = 2;
                    missionsMod = 2;
                }
            }
        }
    }
}
