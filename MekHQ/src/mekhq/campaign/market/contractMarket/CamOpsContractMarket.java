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

    private ContractModifiers contractMods = null;

    public CamOpsContractMarket() {
        super(ContractMarketMethod.CAM_OPS);
    }

    @Override
    public AtBContract addAtBContract(Campaign campaign) {
        if (contractMods == null) {
            contractMods = generateContractModifiers(campaign);
        }
        int ratingMod = campaign.getReputation().getReputationModifier();
        Optional<AtBContract> c = generateContract(campaign, ratingMod);
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
        contractMods = generateContractModifiers(campaign);
        int negotiationSkill = findNegotiationSkill(campaign);
        int numOffers = getNumberOfOffers(
            rollNegotiation(negotiationSkill, ratingMod + contractMods.offersMod) - BASE_NEGOTIATION_TARGET);

        for (int i = 0; i < numOffers; i++) {
            addAtBContract(campaign);
        }
        updateReport(campaign);
    }

    @Override
    public void addFollowup(Campaign campaign, AtBContract contract) {
        //TODO: add logic if we decide followup contracts should be allow in CamOps
    }

    @Override
    public double calculatePaymentMultiplier(Campaign campaign, AtBContract contract) {
        //TODO: add logic from camops 4th printing
        return 1.0;
    }

    private ContractModifiers generateContractModifiers(Campaign campaign) {
        if (campaign.getFaction().isMercenary()) {
            return new ContractModifiers(campaign.getCurrentSystem().getHiringHallLevel(campaign.getLocalDate()));
        } else if (campaign.getFaction().isRebelOrPirate()) {
            return new ContractModifiers(HiringHallLevel.NONE);
        } else {
            return new ContractModifiers(HiringHallLevel.GREAT);
        }
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

    private Optional<AtBContract> generateContract(Campaign campaign, int ratingMod) {
        AtBContract contract = new AtBContract("UnnamedContract");
        lastId++;
        contract.setId(lastId);
        contractIds.put(lastId, contract);
        Faction employer = determineEmployer(campaign, ratingMod);
        contract.setEmployerCode(employer.getShortName(), campaign.getLocalDate());
        if (employer.isMercenary()) {
            contract.setMercSubcontract(true);
        }
        contract.setContractType(determineMission(campaign, employer, ratingMod));
        setEnemyCode(contract);
        setIsRiotDuty(contract);
        setAttacker(contract);
        try {
            setSystemId(contract);
        } catch (NoContractLocationFoundException ex) {
            return Optional.empty();
        }
        setAllyRating(contract, campaign.getGameYear());
        setEnemyRating(contract, campaign.getGameYear());
        if (contract.getContractType().isCadreDuty()) {
            contract.setAllySkill(SkillLevel.GREEN);
            contract.setAllyQuality(IUnitRating.DRAGOON_F);
        }
        contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
        setContractClauses(contract, campaign);
        contract.setRequiredLances(calculateRequiredLances(campaign, contract));
        contract.setMultiplier(calculatePaymentMultiplier(campaign, contract));
        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());
        contract.initContractDetails(campaign);
        contract.calculateContract(campaign);
        contract.setName(String.format("%s - %s - %s %s",
            contract.getStartDate().format(DateTimeFormatter.ofPattern("yyyy")
                .withLocale(MekHQ.getMHQOptions().getDateLocale())), contract.getEmployer(),
            contract.getSystem().getName(contract.getStartDate()), contract.getContractType()));

        return Optional.of(contract);
    }

    private int getReputationScore(Campaign campaign) {
        return campaign.getReputation().getReputationRating();
    }

    private Faction determineEmployer(Campaign campaign, int ratingMod) {
        Collection<Tag> employerTags;
        int roll = Compute.d6(2) + ratingMod + contractMods.employersMod;
        if (roll < 6) {
            // Roll again on the independent employers column
            roll = Compute.d6(2) + ratingMod + contractMods.employersMod;
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
        int roll = Compute.d6(2);
        if (campaign.getFaction().isPirate()) {
            if (roll < 6) {
                return AtBContractType.RECON_RAID;
            } else {
                return AtBContractType.OBJECTIVE_RAID;
            }
        }
        return findMissionType(ratingMod, employer.isISMajorOrSuperPower());
    }

    private void setContractClauses(AtBContract contract, Campaign campaign) {
        // TODO: add logic to determine initial contract clauses from CamOps 4th printing.
    }

    private class ContractModifiers {
        protected int offersMod;
        protected int employersMod;
        protected int missionsMod;

        protected ContractModifiers(HiringHallLevel level) {
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
