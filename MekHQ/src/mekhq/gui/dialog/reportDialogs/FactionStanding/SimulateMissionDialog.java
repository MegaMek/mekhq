package mekhq.gui.dialog.reportDialogs.FactionStanding;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.swing.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.dialog.GlossaryDialog;
import mekhq.gui.utilities.RoundedLineBorder;

public class SimulateMissionDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SimulateMissionDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(400);
    public final static String GLOSSARY_COMMAND_STRING = "GLOSSARY";
    public final static int UNTRACKED_FACTION_INDEX = 0;

    private ImageIcon campaignIcon;
    private final Faction campaignFaction;
    private final LocalDate today;
    private final int gameYear;
    private final String missionName;

    private final List<Faction> allFactions = new ArrayList<>();
    private Faction employerChoice = null;
    private MMComboBox<String> comboEmployerFaction;
    private Faction enemyChoice = null;
    private MMComboBox<String> comboEnemyFaction;

    private final List<MissionStatus> allStatuses = new ArrayList<>();
    MMComboBox<String> comboMissionStatus = null;
    private MissionStatus statusChoice = MissionStatus.ACTIVE;

    public SimulateMissionDialog(ImageIcon campaignIcon, Faction campaignFaction, LocalDate today,
          @Nullable String missionName) {
        this.campaignIcon = campaignIcon;
        this.campaignFaction = campaignFaction;
        this.today = today;
        this.gameYear = today.getYear();
        this.missionName = missionName;

        populateFactionsList();
        populateStatusList();
        populateDialog();
        initializeDialog();
    }

    public @Nullable Faction getEmployerChoice() {
        return employerChoice;
    }

    public @Nullable Faction getEnemyChoice() {
        return enemyChoice;
    }

    public @Nullable MissionStatus getStatusChoice() {
        return statusChoice;
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

    private void populateFactionsList() {
        List<Faction> activeFactions = new ArrayList<>(Factions.getInstance().getActiveFactions(today));
        activeFactions.sort(Comparator.comparing(faction -> faction.getFullName(today.getYear())));
        allFactions.addAll(activeFactions);
    }

    private void populateStatusList() {
        Collections.addAll(allStatuses, MissionStatus.values());
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

        JPanel pnlFactions = populateContractPanel();
        pnlFactions.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCenter.add(pnlInstructions);
        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(pnlFactions);
        pnlParent.add(pnlCenter);

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

        String missionText = missionName == null ?
                                   getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.instructions.noMission") :
                                   missionName;
        String instructions = getFormattedTextAt(RESOURCE_BUNDLE, "simulateContractDialog.instructions", missionText);

        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, instructions));
        setFontScaling(editorPane, false, 1.1);

        pnlInstructions.add(editorPane);

        return pnlInstructions;
    }

    private JPanel populateContractPanel() {
        JPanel pnlFactions = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblEmployer = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.employer"));
        pnlFactions.add(lblEmployer, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        comboEmployerFaction = new MMComboBox<>("comboEmployerFaction", buildFactionModel());
        if (allFactions.contains(campaignFaction)) {
            comboEmployerFaction.setSelectedItem(campaignFaction);
        }
        pnlFactions.add(comboEmployerFaction, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblEnemy = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.enemy"));
        pnlFactions.add(lblEnemy, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        comboEnemyFaction = new MMComboBox<>("comboMissionStatus", buildFactionModel());
        if (allFactions.contains(campaignFaction)) {
            comboEnemyFaction.setSelectedItem(campaignFaction);
        }
        pnlFactions.add(comboEnemyFaction, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel lblMissionStatus = new JLabel(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.label.status"));
        pnlFactions.add(lblMissionStatus, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        comboMissionStatus = new MMComboBox<>("comboMissionStatus", buildMissionStatusModel());
        if (allFactions.contains(campaignFaction)) {
            comboMissionStatus.setSelectedItem(campaignFaction);
        }
        pnlFactions.add(comboMissionStatus, gbc);

        return pnlFactions;
    }

    private DefaultComboBoxModel<String> buildFactionModel() {
        DefaultComboBoxModel<String> factionModel = new DefaultComboBoxModel<>();
        factionModel.addElement(getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.combo.untracked"));

        for (Faction faction : allFactions) {
            // Don't allow the player to pick factions we're not tracking
            if (FactionStandings.isUntrackedFaction(faction.getShortName())) {
                continue;
            }

            factionModel.addElement(faction.getFullName(gameYear));
        }

        return factionModel;
    }

    private DefaultComboBoxModel<String> buildMissionStatusModel() {
        DefaultComboBoxModel<String> missionStatusModel = new DefaultComboBoxModel<>();

        for (MissionStatus status : allStatuses) {
            missionStatusModel.addElement(status.toString());
        }

        return missionStatusModel;
    }

    private JPanel populateButtonPanel() {
        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        String label = getTextAt(RESOURCE_BUNDLE, "simulateContractDialog.button.confirm");
        JButton btnConfirm = new JButton(label);
        btnConfirm.setBorder(RoundedLineBorder.createRoundedLineBorder());
        btnConfirm.addActionListener(evt -> {
            int employerChoiceIndex = comboEmployerFaction.getSelectedIndex();
            if (employerChoiceIndex != UNTRACKED_FACTION_INDEX) { // If it's untracked leave the choice null
                employerChoice = allFactions.get(employerChoiceIndex + 1); // This accounts for the 'untracked' option
            }

            int enemyChoiceIndex = comboEnemyFaction.getSelectedIndex();
            if (enemyChoiceIndex != UNTRACKED_FACTION_INDEX) { // If it's untracked leave the choice null
                enemyChoice = allFactions.get(enemyChoiceIndex + 1); // This accounts for the 'untracked' option
            }

            int missionStatusChoiceIndex = comboMissionStatus.getSelectedIndex();
            statusChoice = allStatuses.get(missionStatusChoiceIndex);

            boolean wasUpdated = false;
            if (enemyChoice != null) {
                wasUpdated = true;
            } else if (employerChoice != null && statusChoice != MissionStatus.ACTIVE) {
                wasUpdated = true;
            }

            new SimulatedMissionConfirmationDialog(campaignIcon, wasUpdated);
            dispose();
        });

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

    public static List<String> handleFactionRegardUpdates(@Nullable final Faction employer,
          @Nullable final Faction enemy, final MissionStatus status, final LocalDate today,
          final FactionStandings factionStandings) {
        List<String> reports = new ArrayList<>();
        if (enemy != null) { // Null means the faction isn't tracked
            reports.addAll(factionStandings.processContractAccept(enemy, today));
        }

        if (employer != null) {
            reports.addAll(factionStandings.processContractCompletion(employer, today, status));
        }

        return reports;
    }
}
