package mekhq.campaign.market.contractMarket;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class CamOpsContractMarket extends AbstractContractMarket {
    private static int BASE_NEGOTIATION_TARGET = 8;
    private static int EMPLOYER_NEGOTIATION_SKILL_LEVEL = 5;

    public CamOpsContractMarket() {
        super(ContractMarketMethod.CAM_OPS);
    }
    @Override
    public AtBContract addAtBContract(Campaign campaign) {
        return null;
    }

    @Override
    public void generateContractOffers(Campaign campaign, boolean newCampaign) {
        if (!(campaign.getLocalDate().getDayOfMonth() == 1) && !newCampaign) {
            return;
        }
        // TODO: Allow subcontracts?
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            //checkForSubcontracts(campaign, contract, unitRatingMod);
        }
        // TODO: CamopsMarket: allow players to choose negotiators and send them out, removing them
        // from other tasks they're doing. For now just use the highest negotiation skill on the force.
        int ratingMod = getReputationModifier(campaign);
        ContractModifiers contractMods = generateContractModifiers(campaign);
        int negotiationSkill = findNegotiationSkill(campaign);
        int numOffers = getNumberOfOffers(
            rollNegotiation(negotiationSkill, ratingMod + contractMods.offersMod) - BASE_NEGOTIATION_TARGET);

        for (int i = 0; i < numOffers; i++) {
            AtBContract c = generateContract(campaign, ratingMod, negotiationSkill);
            if (c != null) {
                contracts.add(c);
            }
        }
    }

    @Override
    public void addFollowup(Campaign campaign, AtBContract contract) {

    }

    @Override
    protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {

    }

    private HiringHallLevel getHiringHallLevel(Campaign campaign) {
        AtBConfiguration atbConfig = campaign.getAtBConfig();
        return atbConfig.getHiringHallLevel(campaign.getCurrentSystem()
                .getPrimaryPlanet()
                .getName(campaign.getLocalDate()),
            campaign.getLocalDate());
    }

    private ContractModifiers generateContractModifiers(Campaign campaign) {
        if (campaign.getFaction().isMercenary()) {
            return new ContractModifiers(getHiringHallLevel(campaign));
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

    private @Nullable AtBContract generateContract(Campaign campaign, int ratingMod, int negotiationSkill) {
        AtBContract contract = new AtBContract("UnnamedContract");

        return contract;
    }

    private int getReputationModifier(Campaign campaign) {
        return campaign.getReputation().getReputationRating() / 10;
    }

    private void determineMission(Campaign campaign) {
        if (campaign.getFaction().isPirate()) {

        }
    }

    private static class ContractModifiers {
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
