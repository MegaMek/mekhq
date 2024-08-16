package mekhq.campaign.market.contractMarket;

import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.PlanetarySystem;

public class CamOpsContractMarket extends AbstractContractMarket {
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
        int repFactor = campaign.getReputationFactor();
        // TODO: Allow subcontracts?
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
            //checkForSubcontracts(campaign, contract, unitRatingMod);
        }
        // TODO: CamopsMarket: allow players to choose negotiators and send them out, removing them
        // from other tasks they're doing. For now just use the highest negotiation skill on the force.
        Person negotiator = campaign.findBestAtSkill(SkillType.S_NEG);
    }

    @Override
    public void addFollowup(Campaign campaign, AtBContract contract) {

    }

    @Override
    protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {

    }

    private int getHiringHallMod(PlanetarySystem system) {
        return 0;
    }
}
