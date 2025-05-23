package mekhq.gui.dialog.reportDialogs.FactionStanding;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.swing.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.logging.MMLogger;
import mekhq.gui.dialog.GlossaryDialog;
import mekhq.gui.utilities.RoundedLineBorder;

public class SimulatedMissionConfirmationDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SimulateMissionDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(400);
    public final static String GLOSSARY_COMMAND_STRING = "GLOSSARY";

    private ImageIcon campaignIcon;
    private boolean updateWasSuccess;

    public SimulatedMissionConfirmationDialog(ImageIcon campaignIcon, boolean updateWasSuccess) {
        this.campaignIcon = campaignIcon;
        this.updateWasSuccess = updateWasSuccess;

        populateDialog();
        initializeDialog();
    }

    private void initializeDialog() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        pack();
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    private void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, 0, PADDING, 0);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for campaign icon
        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(0, PADDING, 0, 0));
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 1;
        mainPanel.add(pnlLeft, constraints);
        gridx++;

        // Center box for the message
        JPanel pnlCenter = populateCenterPanel();
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 2;
        constraints.weighty = 2;
        mainPanel.add(pnlCenter, constraints);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel pnlCampaign = new JPanel();
        pnlCampaign.setLayout(new BoxLayout(pnlCampaign, BoxLayout.Y_AXIS));
        pnlCampaign.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCampaign.setMaximumSize(new Dimension(IMAGE_WIDTH, scaleForGUI(MAX_VALUE)));

        campaignIcon = scaleImageIcon(campaignIcon, IMAGE_WIDTH, true);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(campaignIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlCampaign.add(imageLabel);

        return pnlCampaign;
    }

    private JPanel populateCenterPanel() {
        JPanel pnlParent = new JPanel();
        pnlParent.setLayout(new BoxLayout(pnlParent, BoxLayout.Y_AXIS));

        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlInstructions = populateInstructionsPanel();
        pnlParent.add(pnlInstructions);

        JPanel pnlButton = populateButtonPanel();
        pnlButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlParent.add(pnlButton);

        return pnlParent;
    }

    private JPanel populateInstructionsPanel() {
        JPanel pnlInstructions = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String instructionsKey = updateWasSuccess ?
                                       "simulateContractDialog.confirmation.success" :
                                       "simulateContractDialog.confirmation.failure";
        String instructions = getTextAt(RESOURCE_BUNDLE, instructionsKey);

        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, instructions));
        setFontScaling(editorPane, false, 1.1);

        pnlInstructions.add(editorPane);

        return pnlInstructions;
    }

    private JPanel populateButtonPanel() {
        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        String label = getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.button.confirm");
        JButton btnConfirm = new JButton(label);
        btnConfirm.setBorder(RoundedLineBorder.createRoundedLineBorder());
        btnConfirm.addActionListener(evt -> dispose());

        pnlButton.add(btnConfirm);

        return pnlButton;
    }

    private void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            handleImmersiveHyperlinkClick(evt.getDescription());
        }
    }

    private void handleImmersiveHyperlinkClick(String reference) {
        String[] splitReference = reference.split(":");

        String commandKey = splitReference[0];
        String entryKey = splitReference[1];

        if (commandKey.equalsIgnoreCase(GLOSSARY_COMMAND_STRING)) {
            new GlossaryDialog(this, entryKey);
        } else {
            LOGGER.warn("Invalid hyperlink command: {}", commandKey);
        }
    }
}
