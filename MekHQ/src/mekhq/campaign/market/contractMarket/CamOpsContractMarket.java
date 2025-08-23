/*
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.market.contractMarket;

import static megamek.common.compute.Compute.d6;
import static megamek.common.enums.SkillLevel.REGULAR;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.personnel.PersonnelOptions.ADMIN_NETWORKER;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.ContractCommandRights;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Contract Market as described in Campaign Operations, 4th printing.
 */
public class CamOpsContractMarket extends AbstractContractMarket {
    private static final MMLogger logger = MMLogger.create(CamOpsContractMarket.class);
    private static final int BASE_NEGOTIATION_TARGET = 8;
    private static final int EMPLOYER_NEGOTIATION_SKILL_LEVEL = 5;

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
        boolean isGrayMonday = isGrayMonday(campaign.getLocalDate(),
              campaign.getCampaignOptions().isSimulateGrayMonday());
        boolean hasActiveContract = campaign.hasActiveContract() || campaign.hasActiveAtBContract(true);

        if (!(campaign.getLocalDate().getDayOfMonth() == 1) && !newCampaign) {
            return;
        }

        // If the player has an active contract, they will not be offered new contracts,
        // as MekHQ doesn't support multiple contracts (outside of subcontracts).
        if (hasActiveContract) {
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

        Person negotiator = campaign.getSeniorAdminPerson(COMMAND);
        int negotiatorModifier = 0;
        if (negotiator != null) {
            PersonnelOptions options = negotiator.getOptions();
            if (options.booleanOption(ADMIN_NETWORKER)) {
                negotiatorModifier++;
            }
        }

        int numOffers = getNumberOfOffers(rollNegotiation(negotiationSkill, ratingMod + hiringHallModifiers.offersMod) -
                                                BASE_NEGOTIATION_TARGET) + negotiatorModifier;

        if (isGrayMonday) {
            for (int i = 0; i < numOffers; i++) {
                if (d6() <= 2) {
                    numOffers--;
                }
            }
        }

        if (numOffers == 0) {
            return;
        }

        for (int i = 0; i < numOffers; i++) {
            addAtBContract(campaign);
        }
        updateReport(campaign);
    }

    @Override
    public double calculatePaymentMultiplier(Campaign campaign, AtBContract contract) {
        double reputationFactor = campaign.getReputation().getReputationFactor();
        ContractTerms terms = getContractTerms(campaign, contract);
        return terms.getEmploymentMultiplier() * terms.getOperationsTempoMultiplier() * reputationFactor;
    }

    @Override
    public void checkForFollowup(Campaign campaign, AtBContract contract) {

    }

    @Override
    public void rerollClause(AtBContract contract, int clause, Campaign campaign) {
        if (getRerollsUsed(contract, clause) > 0) {
            // CamOps RAW only allows 1 negotiation attempt
            return;
        }
        int negotiationSkill = findNegotiationSkill(campaign);
        int ratingMod = campaign.getReputation().getReputationModifier();

        if (campaign.getCampaignOptions().isUseFactionStandingNegotiationSafe()) {
            FactionStandings standings = campaign.getFactionStandings();
            double regard = standings.getRegardForFaction(contract.getEmployerCode(), true);
            int negotiationModifier = FactionStandingUtilities.getNegotiationModifier(regard);
            ratingMod += negotiationModifier;
        }

        int margin = rollOpposedNegotiation(negotiationSkill, ratingMod);
        int change = margin / 2;
        ContractTerms terms = getContractTerms(campaign, contract);

        switch (clause) {
            case CLAUSE_COMMAND -> setCommandRights(contract, terms, contract.getCommandRoll() + change);
            case CLAUSE_SALVAGE -> setSalvageRights(contract, terms, contract.getSalvageRoll() + change);
            case CLAUSE_SUPPORT -> setSupportRights(contract, terms, contract.getSupportRoll() + change);
            case CLAUSE_TRANSPORT -> setTransportRights(contract, terms, contract.getTransportRoll() + change);
            default -> throw new IllegalStateException("Unexpected clause when rerolling contract clause: " + clause);
        }
        clauseMods.get(contract.getId()).rerollsUsed[clause]++;
        contract.calculateContract(campaign);
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
        Person negotiator = campaign.findBestAtSkill(SkillType.S_NEGOTIATION);
        if (negotiator == null) {
            return 0;
        }
        return negotiator.getSkillLevel(S_NEGOTIATION,
              campaign.getCampaignOptions().isUseAgeEffects(),
              campaign.isClanCampaign(),
              campaign.getLocalDate());
    }

    private int rollNegotiation(int skill, int modifiers) {
        return Compute.d6(2) + skill + modifiers;
    }

    private int rollOpposedNegotiation(int skill, int modifiers) {
        return rollNegotiation(skill, modifiers) - Compute.d6(2) + EMPLOYER_NEGOTIATION_SKILL_LEVEL;
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
        contract.setEmployerCode(employer.getShortName(), campaign.getGameYear());
        if (employer.isMercenary()) {
            contract.setMercSubcontract(true);
        }
        // Step 2: Determine the mission type
        contract.setContractType(determineMission(campaign, employer, reputation.getReputationModifier()));
        ContractTerms contractTerms = getContractTerms(campaign, contract);
        setEnemyCode(contract);
        setAttacker(contract);
        // Step 3: Set the system location
        try {
            setSystemId(contract);
        } catch (NoContractLocationFoundException ex) {
            return Optional.empty();
        }
        // Step 4: Populate some information about enemies and allies
        final SkillLevel campaignSkillLevel = reputation.getAverageSkillLevel();
        final boolean useDynamicDifficulty = campaign.getCampaignOptions().isUseDynamicDifficulty();
        setAllyRating(contract, campaign.getGameYear(), useDynamicDifficulty ? campaignSkillLevel : REGULAR);
        setEnemyRating(contract, campaign.getGameYear(), useDynamicDifficulty ? campaignSkillLevel : REGULAR);
        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(SkillLevel.GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }
        // Step 5: Determine the contract length (Not CamOps RAW)
        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        // Step 6: Determine the initial contract clauses
        setContractClauses(contract, contractTerms);
        // Step 7: Determine the number of required lances (Not CamOps RAW)
        contract.setRequiredCombatTeams(ContractUtilities.calculateBaseNumberOfRequiredLances(campaign));
        contract.setRequiredCombatElements(calculateRequiredCombatElements(campaign, contract, false));
        // Step 8: Calculate the payment
        contract.setMultiplier(calculatePaymentMultiplier(campaign, contract));
        // Step 9: Determine parts availability
        // TODO: Rewrite this to be CamOps-compliant
        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());
        // Step 10: Finish up contract initialization
        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);
        contract.setName(String.format("%s - %s - %s %s",
              contract.getStartDate()
                    .format(DateTimeFormatter.ofPattern("yyyy").withLocale(MekHQ.getMHQOptions().getDateLocale())),
              contract.getEmployer(),
              contract.getSystem().getName(contract.getStartDate()),
              contract.getContractType()));

        contract.clanTechSalvageOverride();

        return Optional.of(contract);
    }

    @Override
    protected void rollCommandClause(final Contract contract, final int modifier, boolean isMercenary) {
        final int roll = d6(2) + modifier;

        if (isMercenary) {
            // Handle mercenaries
            contract.setCommandRights(determineMercenaryCommandRights(roll));
        } else {
            // Handle non-mercenaries
            contract.setCommandRights(ContractCommandRights.INTEGRATED);
        }
    }

    private Faction determineEmployer(Campaign campaign, int ratingMod, HiringHallModifiers hiringHallModifiers) {
        Collection<FactionTag> employerTags;
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

    private Faction getRandomEmployer(Campaign campaign, Collection<FactionTag> employerTags) {
        Collection<Faction> factions = Factions.getInstance().getActiveFactions(campaign.getLocalDate());
        List<Faction> filtered = new ArrayList<>();
        for (Faction faction : factions) {
            // Clans only hire units within their own clan
            if (faction.isClan() && !faction.equals(campaign.getFaction())) {
                continue;
            }
            for (FactionTag employerTag : employerTags) {
                if (!faction.is(employerTag)) {
                    // The SMALL tag has to be converted to independent for now, since for some reason
                    // independent is coded as a string.
                    if (employerTag == FactionTag.SMALL && faction.isIndependent()) {
                        continue;
                    }
                    break;
                }
                filtered.add(faction);
            }
        }
        Random rand = new Random();
        return filtered.get(rand.nextInt(filtered.size()));
    }

    private Collection<FactionTag> getEmployerTags(Campaign campaign, int roll, boolean independent) {
        Collection<FactionTag> tags = new ArrayList<>();
        if (independent) {
            tags.add(FactionTag.SMALL);
            if (roll < 4) {
                tags.add(FactionTag.NOBLE);
            } else if (roll < 6) {
                tags.add(FactionTag.PLANETARY_GOVERNMENT);
            } else if (roll == 6) {
                tags.add(FactionTag.MERC);
            } else if (roll < 9) {
                tags.add(FactionTag.PERIPHERY);
                tags.add(FactionTag.MAJOR);
            } else if (roll < 11) {
                tags.add(FactionTag.PERIPHERY);
                tags.add(FactionTag.MINOR);
            } else {
                tags.add(FactionTag.CORPORATION);
            }
        } else {
            if (roll < 6) {
                tags.add(FactionTag.SMALL);
            } else if (roll < 8) {
                tags.add(FactionTag.MINOR);
            } else if (roll < 11) {
                tags.add(FactionTag.MAJOR);
            } else {
                if (Factions.getInstance()
                          .getActiveFactions(campaign.getLocalDate())
                          .stream()
                          .anyMatch(Faction::isSuperPower)) {
                    tags.add(FactionTag.SUPER);
                } else {
                    tags.add(FactionTag.MAJOR);
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

    private ContractTerms getContractTerms(Campaign campaign, AtBContract contract) {
        return new ContractTerms(contract.getContractType(),
              contract.getEmployerFaction(),
              campaign.getReputation().getReputationFactor(),
              campaign.getLocalDate());
    }

    private void setContractClauses(AtBContract contract, ContractTerms terms) {
        clauseMods.put(contract.getId(), new ClauseMods());
        setCommandRights(contract, terms, Compute.d6(2));
        setSalvageRights(contract, terms, Compute.d6(2));
        setSupportRights(contract, terms, Compute.d6(2));
        setTransportRights(contract, terms, Compute.d6(2));
    }

    private void setCommandRights(AtBContract contract, ContractTerms terms, int roll) {
        contract.setCommandRoll(roll);
        contract.setCommandRights(terms.getCommandRights(roll));
    }

    private void setSalvageRights(AtBContract contract, ContractTerms terms, int roll) {
        contract.setSalvageRoll(roll);
        if (terms.isSalvageExchange(roll)) {
            contract.setSalvageExchange(true);
        } else {
            contract.setSalvageExchange(false);
            contract.setSalvagePct(terms.getSalvagePercentage(roll));
        }
    }

    private void setSupportRights(AtBContract contract, ContractTerms terms, int roll) {
        contract.setSupportRoll(roll);
        if (terms.isStraightSupport(roll)) {
            contract.setStraightSupport(terms.getSupportPercentage(roll));
        } else if (terms.isBattleLossComp(roll)) {
            contract.setBattleLossComp(terms.getSupportPercentage(roll));
        } else {
            contract.setStraightSupport(0);
        }
    }

    private void setTransportRights(AtBContract contract, ContractTerms terms, int roll) {
        contract.setTransportRoll(roll);
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
