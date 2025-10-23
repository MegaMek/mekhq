package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.rentals.ContractRentalType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

public class ContractStartRentalDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FacilityRentals";

    private static final int DIALOG_CANCEL_OPTION = 0;
    private static final int DIALOG_CONFIRM_OPTION = 1;

    private static JLabel lblRentalCost;

    public boolean wasConfirmed() {
        return this.getDialogChoice() == DIALOG_CONFIRM_OPTION;
    }

    public ContractStartRentalDialog(Campaign campaign, ContractRentalType hospitalBeds, int rentalCost) {
        super(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.LOGISTICS),
              null,
              getCenterMessage(campaign.getCommanderAddress()),
              getButtons(),
              getOutOfCharacterMessage(),
              ImmersiveDialogWidth.SMALL.getWidth(),
              false,
              getSupplementalPanel(),
              null,
              true);
    }

    private static String getCenterMessage(String commanderAddress) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE", commanderAddress);
    }

    private static List<ButtonLabelTooltipPair> getButtons() {
        return List.of(
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "CANCEL"), null),
              new ButtonLabelTooltipPair(getTextAt(RESOURCE_BUNDLE, "CONFIRM"), null)
        );
    }

    private static String getOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE");
    }

    private static JPanel getSupplementalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        // Add label for ComboBox
        JLabel lblRentalCount = new JLabel(getTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        JSpinner spnRentalCount = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnRentalCount.addChangeListener(e -> {
            int count = (Integer) spnRentalCount.getValue();
            lblRentalCost.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        });
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        lblRentalCost = new JLabel(getTextAt(RESOURCE_BUNDLE, "PLACEHOLDER_MESSAGE"));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(lblRentalCount, constraints);

        return panel;
    }
}
