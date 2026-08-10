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
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("contractId: " + contract.getContractId()).append("<br>");
        sb.append("contractName:" + contract.getContractName()).append("<br>");
        sb.append("description:" + contract.getDescription()).append("<br>");
        sb.append("employerData:" + contract.getEmployerData()).append("<br>");
        sb.append("enemyData:" + contract.getEnemyData()).append("<br>");
        sb.append("contractTerms:" + contract.getContractTerms()).append("<br>");
        sb.append("objectiveData:" + contract.getObjectiveData()).append("<br>");
        sb.append("contractFinanceData:" + contract.getContractFinanceData()).append("<br>");
        sb.append("missionStatus:" + contract.getMissionStatus()).append("<br>");
        sb.append("scheduleData:" + contract.getScheduleData()).append("<br>");
        sb.append("systemsTargetData:" + contract.getSystemsTargetData()).append("<br>");
        sb.append("rentedFacilitiesData:" + contract.getRentedFacilitiesData()).append("<br>");
        sb.append("moraleData:" + contract.getMoraleData()).append("<br>");
        sb.append("stratConCampaignState:" + contract.getStratConCampaignState()).append("<br>");
        sb.append("scale:" + contract.getScale()).append("<br>");
        sb.append("trackCount:" + contract.getTrackCount()).append("<br>");
        sb.append("scenarios:" + contract.getScenarios()).append("<br>");
        sb.append("cachedJumpPath:" + contract.getCachedJumpPathDirect()).append("<br>");
        sb.append("cachedContractDifficulty:" + contract.getCachedContractDifficulty()).append("<br>");
        sb.append("</html>");

        return sb.toString().replace(",", "<br>");
    }
}
