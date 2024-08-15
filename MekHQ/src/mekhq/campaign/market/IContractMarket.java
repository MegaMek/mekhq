package mekhq.campaign.market;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

import java.io.PrintWriter;
import java.util.List;

public interface IContractMarket {
    int CLAUSE_COMMAND = 0;
    int CLAUSE_SALVAGE = 1;
    int CLAUSE_SUPPORT = 2;
    int CLAUSE_TRANSPORT = 3;
    int CLAUSE_NUM = 4;

    void generateContractOffers(Campaign campaign);

    void generateContractOffers(Campaign campaign, boolean newCampaign);

    int getRerollsUsed(Contract c, int clause);

    AtBContract addAtBContract(Campaign c);

    void removeContract(Contract c);

    List<Contract> getContracts();

    void rerollClause(AtBContract c, int clause, Campaign campaign);

    void addFollowup(Campaign campaign, AtBContract contract);

    void writeToXML(final PrintWriter pw, int indent);
}
