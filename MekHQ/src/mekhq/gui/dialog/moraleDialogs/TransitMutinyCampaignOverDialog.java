package mekhq.gui.dialog.moraleDialogs;

import mekhq.campaign.Campaign;

import javax.swing.*;
import java.util.ResourceBundle;

public class TransitMutinyCampaignOverDialog extends JDialog {
    /**
     * Displays a dialog for the onset of a mutiny while in transit.
     *
     * @param resources the resource bundle containing the dialog text and options
     */
    public static void transitMutinyCampaignOverDialog(Campaign campaign, ResourceBundle resources) {
        JOptionPane pane = new JOptionPane(
                String.format(resources.getString("abstractMutinyCampaignEnd.text"), campaign.getName()),
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                new Object[]{},
                null
        );

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JDialog dialog = pane.createDialog(null, resources.getString("abstractMutingCampaignEndTitle.title"));

        dialog.setResizable(false);
        dialog.setVisible(true);
    }
}