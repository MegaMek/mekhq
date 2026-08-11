package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.campaign.mission.newContract.contractGeneration.AbstractContractGeneration.createContract;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.mission.newContract.contractGeneration.ContractSearchType;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class ChaosContractMarketDialogTest {
    public ChaosContractMarketDialogTest(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        LocalDate currentDate = campaign.getLocalDate();
        Detachment detachment = campaign.getPlayerForce().getForceDetachment();
        FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();
        while (true) {
            ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
                  null,
                  getContractText(campaign, campaignOptions, currentDate, detachment, factionStandings),
                  List.of("NEW", "EXIT"),
                  null);
            if (dialog.getDialogChoice() == 1) {
                break;
            }
        }
    }

    private String getContractText(Campaign campaign, CampaignOptions campaignOptions, LocalDate currentDate,
          Detachment detachment, FactionStandings factionStandings) {
        AbstractContract contract = createContract(campaign,
              campaignOptions,
              currentDate,
              detachment,
              0,
              ContractSearchType.MERCENARY,
              factionStandings,
              false,
              false);

        return contract == null ? "WHOOPSIE DOODLES" : buildTextFromContract(contract);
    }

    private String buildTextFromContract(AbstractContract contract) {
        StringBuilder contractReport = new StringBuilder("<html>");
        contractReport.append("contractId: ").append(contract.getContractId()).append("<br>");
        contractReport.append("contractName:").append(contract.getContractName()).append("<br>");
        contractReport.append("description:").append(contract.getDescription()).append("<br>");
        contractReport.append("employerData:").append(contract.getEmployerData()).append("<br>");
        contractReport.append("enemyData:").append(contract.getEnemyData()).append("<br>");
        contractReport.append("contractTerms:").append(contract.getContractTerms()).append("<br>");
        contractReport.append("objectiveData:").append(contract.getObjectiveData()).append("<br>");
        contractReport.append("contractFinanceData:").append(contract.getContractFinanceData()).append("<br>");
        contractReport.append("missionStatus:").append(contract.getMissionStatus()).append("<br>");
        contractReport.append("scheduleData:").append(contract.getScheduleData()).append("<br>");
        contractReport.append("systemsTargetData:").append(contract.getSystemsTargetData()).append("<br>");
        contractReport.append("rentedFacilitiesData:").append(contract.getRentedFacilitiesData()).append("<br>");
        contractReport.append("moraleData:").append(contract.getMoraleData()).append("<br>");
        contractReport.append("stratConCampaignState:").append(contract.getStratConCampaignState()).append("<br>");
        contractReport.append("scale:").append(contract.getScale()).append("<br>");
        contractReport.append("trackCount:").append(contract.getTrackCount()).append("<br>");
        contractReport.append("scenarios:").append(contract.getScenarios()).append("<br>");
        contractReport.append("cachedJumpPath:").append(contract.getCachedJumpPathDirect()).append("<br>");
        contractReport.append("cachedContractDifficulty:")
              .append(contract.getCachedContractDifficulty())
              .append("<br>");
        contractReport.append("</html>");

        return contractReport.toString().replace(",", "<br>");
    }
}
