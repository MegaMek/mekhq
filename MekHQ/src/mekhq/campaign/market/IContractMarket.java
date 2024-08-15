package mekhq.campaign.market;

import mekhq.campaign.Campaign;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public interface IContractMarket {
    int CLAUSE_COMMAND = 0;
    int CLAUSE_SALVAGE = 1;
    int CLAUSE_SUPPORT = 2;
    int CLAUSE_TRANSPORT = 3;
    int CLAUSE_NUM = 4;

    public void generateContractOffers(Campaign campaign);

    public void generateContractOffers(Campaign campaign, boolean newCampaign);

    public int getRerollsUsed(Contract c, int clause);

    public AtBContract addAtBContract(Campaign c);

    public void removeContract(Contract c);

    public List<Contract> getContracts();

    public void rerollClause(AtBContract c, int clause, Campaign campaign);

    public void addFollowup(Campaign campaign, AtBContract contract);

    public void writeToXML(final PrintWriter pw, int indent);
}
