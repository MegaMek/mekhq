package mekhq.campaign.market.contractMarket;

import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;

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

    }

    @Override
    public void addFollowup(Campaign campaign, AtBContract contract) {

    }

    @Override
    protected void setAtBContractClauses(AtBContract contract, int unitRatingMod, Campaign campaign) {

    }
}
