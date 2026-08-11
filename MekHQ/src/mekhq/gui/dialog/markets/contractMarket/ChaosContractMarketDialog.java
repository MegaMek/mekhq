package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.mission.newContract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MAXIMUM_STEP_VALUE;
import static mekhq.campaign.mission.newContract.contractData.ChaosContractStepsTable.CHAOS_CONTRACT_MINIMUM_STEP_VALUE;
import static mekhq.campaign.mission.newContract.contractGeneration.AbstractContractGeneration.createContract;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.newContract.AbstractContract;
import mekhq.campaign.mission.newContract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.newContract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.newContract.contractGeneration.ContractTermsData;
import mekhq.campaign.mission.newContract.contractGeneration.targetFinder.ChaosContractPayDetermination;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;

public class ChaosContractMarketDialog {
    private JLabel lblBreakdown;
    private final JLabel lblPayRateNegotiations = new JLabel("Pay Rate Negotiations: ");
    private final JSpinner spnPayRateNegotiations = new JSpinner();
    private final JLabel lblSupportNegotiations = new JLabel("Support Negotiations: ");
    private final JSpinner spnSupportNegotiations = new JSpinner();
    private final JLabel lblTransportNegotiations = new JLabel("Transport Negotiations: ");
    private final JSpinner spnTransportNegotiations = new JSpinner();
    private final JLabel lblSalvageRightsNegotiations = new JLabel("Salvage Rights Negotiations: ");
    private final JSpinner spnSalvageRightsNegotiations = new JSpinner();
    private final JLabel lblCommandRightsNegotiations = new JLabel("Command Rights Negotiations: ");
    private final JSpinner spnCommandRightsNegotiations = new JSpinner();

    public ChaosContractMarketDialog(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        LocalDate currentDate = campaign.getLocalDate();
        PlayerForce playerForce = campaign.getPlayerForce();
        Detachment detachment = playerForce.getForceDetachment();
        FactionStandings factionStandings = playerForce.getFactionStandings();
        boolean isClanForce = playerForce.isClanForce();
        while (true) {
            AbstractContract contract = createContract(campaign,
                  campaignOptions,
                  currentDate,
                  detachment,
                  0,
                  ContractSearchType.MERCENARY,
                  factionStandings,
                  false,
                  false);

            if (contract == null) {
                new ImmersiveDialogNotification(null, "ERROR", true);
            } else {
                ImmersiveDialogCore dialog = new ImmersiveDialogCore(campaign,
                      getSeniorAdminPerson(playerForce.getHumanResources(), campaignOptions, isClanForce, currentDate),
                      contract.getEmployerNegotiator(),
                      "",
                      getButtons(),
                      null,
                      null,
                      false,
                      getSpinnerPanel(campaign, currentDate, detachment.getCurrentLocation(), contract),
                      null,
                      true);
                if (dialog.getDialogChoice() == 1) {
                    break;
                }
            }
        }
    }

    private JPanel getSpinnerPanel(Campaign campaign, LocalDate currentDate, AbstractLocation currentLocation,
          AbstractContract contract) {
        final int PADDING = scaleForGUI(10);

        lblBreakdown = new JLabel(buildTextFromContract(contract));

        int currentValuePayRate = contract.getBasePayRateStep().stepValue();
        spnPayRateNegotiations.setModel(new SpinnerNumberModel(currentValuePayRate,
              CHAOS_CONTRACT_MINIMUM_STEP_VALUE,
              CHAOS_CONTRACT_MAXIMUM_STEP_VALUE,
              1));
        spnPayRateNegotiations.addChangeListener(e -> {
            int newStepValue = (int) spnPayRateNegotiations.getValue();
            ChaosContractStepsTable step = ChaosContractStepsTable.fromStepValue(newStepValue);
            ContractTermsData existingTermsData = contract.getContractTerms();
            ContractTermsData updatedTermsData = new ContractTermsData(existingTermsData, step, null, null, null, null);
            contract.setContractTerms(updatedTermsData);

            Money newMoney = ChaosContractPayDetermination.getMonthlyPay(contract);
            contract.updateMonthlyPay(newMoney);

            lblBreakdown.setText(buildTextFromContract(contract));
        });

        int currentValueSupport = contract.getSupportStep().stepValue();
        spnSupportNegotiations.setModel(new SpinnerNumberModel(currentValueSupport,
              CHAOS_CONTRACT_MINIMUM_STEP_VALUE,
              CHAOS_CONTRACT_MAXIMUM_STEP_VALUE,
              1));
        spnSupportNegotiations.addChangeListener(e -> {
            int newStepValue = (int) spnSupportNegotiations.getValue();
            ChaosContractStepsTable step = ChaosContractStepsTable.fromStepValue(newStepValue);
            ContractTermsData existingTermsData = contract.getContractTerms();
            ContractTermsData updatedTermsData = new ContractTermsData(existingTermsData, null, step, null, null, null);
            contract.setContractTerms(updatedTermsData);

            lblBreakdown.setText(buildTextFromContract(contract));
        });

        int currentValueTransport = contract.getTransportStep().stepValue();
        spnTransportNegotiations.setModel(new SpinnerNumberModel(currentValueTransport,
              CHAOS_CONTRACT_MINIMUM_STEP_VALUE,
              CHAOS_CONTRACT_MAXIMUM_STEP_VALUE,
              1));
        spnTransportNegotiations.addChangeListener(e -> {
            int newStepValue = (int) spnTransportNegotiations.getValue();
            ChaosContractStepsTable step = ChaosContractStepsTable.fromStepValue(newStepValue);
            ContractTermsData existingTermsData = contract.getContractTerms();
            ContractTermsData updatedTermsData = new ContractTermsData(existingTermsData, null, null, step, null, null);
            contract.setContractTerms(updatedTermsData);

            Money newMoney = ChaosContractPayDetermination.getTransportPay(campaign, currentDate, contract,
                  currentLocation);
            contract.updateTransportPay(newMoney);

            lblBreakdown.setText(buildTextFromContract(contract));
        });

        int currentValueSalvageRights = contract.getSalvageRightsStep().stepValue();
        spnSalvageRightsNegotiations.setModel(new SpinnerNumberModel(currentValueSalvageRights,
              CHAOS_CONTRACT_MINIMUM_STEP_VALUE,
              CHAOS_CONTRACT_MAXIMUM_STEP_VALUE,
              1));
        spnSalvageRightsNegotiations.addChangeListener(e -> {
            int newStepValue = (int) spnSalvageRightsNegotiations.getValue();
            ChaosContractStepsTable step = ChaosContractStepsTable.fromStepValue(newStepValue);
            ContractTermsData existingTermsData = contract.getContractTerms();
            ContractTermsData updatedTermsData = new ContractTermsData(existingTermsData, null, null, null, step, null);
            contract.setContractTerms(updatedTermsData);

            lblBreakdown.setText(buildTextFromContract(contract));
        });

        int currentValueCommandRights = contract.getCommandRightsStep().stepValue();
        spnCommandRightsNegotiations.setModel(new SpinnerNumberModel(currentValueCommandRights,
              CHAOS_CONTRACT_MINIMUM_STEP_VALUE,
              CHAOS_CONTRACT_MAXIMUM_STEP_VALUE,
              1));
        spnCommandRightsNegotiations.addChangeListener(e -> {
            int newStepValue = (int) spnCommandRightsNegotiations.getValue();
            ChaosContractStepsTable step = ChaosContractStepsTable.fromStepValue(newStepValue);
            ContractTermsData existingTermsData = contract.getContractTerms();
            ContractTermsData updatedTermsData = new ContractTermsData(existingTermsData, null, null, null, null, step);
            contract.setContractTerms(updatedTermsData);

            lblBreakdown.setText(buildTextFromContract(contract));
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints layout = new GridBagConstraints();

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2; // The label takes up two spaces horizontally
        layout.anchor = GridBagConstraints.WEST;
        layout.insets = new Insets(PADDING, 0, PADDING, 0);
        panel.add(lblBreakdown, layout);

        layout.gridwidth = 1;
        layout.gridy = 1;
        panel.add(lblPayRateNegotiations, layout);

        layout.gridx = 1;
        panel.add(spnPayRateNegotiations, layout);

        layout.gridx = 0;
        layout.gridy = 2;
        panel.add(lblSupportNegotiations, layout);

        layout.gridx = 1;
        panel.add(spnSupportNegotiations, layout);

        layout.gridx = 0;
        layout.gridy = 3;
        panel.add(lblTransportNegotiations, layout);

        layout.gridx = 1;
        panel.add(spnTransportNegotiations, layout);

        layout.gridx = 0;
        layout.gridy = 4;
        panel.add(lblSalvageRightsNegotiations, layout);

        layout.gridx = 1;
        panel.add(spnSalvageRightsNegotiations, layout);

        layout.gridx = 0;
        layout.gridy = 5;
        panel.add(lblCommandRightsNegotiations, layout);

        layout.gridx = 1;
        panel.add(spnCommandRightsNegotiations, layout);

        return panel;
    }

    private List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons() {
        List<ImmersiveDialogCore.ButtonLabelTooltipPair> buttons = new ArrayList<>();

        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair("REGENERATE", null));
        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair("LEAVE", null));

        return buttons;
    }

    private static Person getSeniorAdminPerson(ForceHumanResources humanResources, CampaignOptions campaignOptions,
          boolean isClanForce, LocalDate currentDate) {
        return humanResources.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND,
              campaignOptions,
              isClanForce,
              currentDate);
    }

    private String getContractText(AbstractContract contract) {
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
