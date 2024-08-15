package mekhq.campaign.market;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

import java.io.PrintWriter;
import java.util.List;

public class CamOpsContractMarket implements IContractMarket {

    @Override
    public void generateContractOffers(Campaign campaign) {

    }

    @Override
    public void generateContractOffers(Campaign campaign, boolean newCampaign) {

    }

    @Override
    public int getRerollsUsed(Contract c, int clause) {
        return 0;
    }

    @Override
    public AtBContract addAtBContract(Campaign c) {
        return null;
    }

    @Override
    public void removeContract(Contract c) {

    }


    @Override
    public List<Contract> getContracts() {
        return List.of();
    }

    @Override
    public void rerollClause(AtBContract c, int clause, Campaign campaign) {

    }

    @Override
    public void addFollowup(Campaign campaign, AtBContract contract) {

    }

    @Override
    public void writeToXML(PrintWriter pw, int indent) {

    }
}
