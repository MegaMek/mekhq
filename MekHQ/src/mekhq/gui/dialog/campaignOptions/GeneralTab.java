package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;

import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.dialog.campaignOptions.CampaignOptionsUtilities.*;

/**
 * Represents a tab that allows the user to configure general settings for a campaign.
 * Extends the {@link AbstractMHQTabbedPane} class.
 */
public class GeneralTab {
    private final JFrame frame;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;

    private JLabel lblName;
    private JTextField txtName;
    private JButton btnNameGenerator;
    private JLabel lblFaction;
    private MMComboBox<FactionDisplay> comboFaction;
    private JLabel lblReputation;
    private MMComboBox<UnitRatingMethod> unitRatingMethodCombo;
    private JLabel lblManualUnitRatingModifier;
    private JSpinner manualUnitRatingModifier;
    private JLabel lblDate;
    private JButton btnDate;
    private LocalDate date;
    private JLabel lblCamo;
    private JButton btnCamo;
    private Camouflage camouflage;
    private JLabel lblIcon;
    private JButton btnIcon;
    private StandardForceIcon unitIcon;

    public GeneralTab(Campaign campaign, JFrame frame) {
        // region Variable Declarations
        this.frame = frame;
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.date = campaign.getLocalDate();
        this.camouflage = campaign.getCamouflage();
        this.unitIcon = campaign.getUnitIcon();

        initialize();
    }

    /**
     * Creates the general tab for a campaign.
     *
     * @return the created general tab as an AbstractMHQScrollablePanel
     */
    public AbstractMHQScrollablePanel createGeneralTab() {
        // Header
        JPanel headerPanel = createGeneralHeader();

        // Campaign name
        lblName = new CampaignOptionsLabel("Name");
        txtName = new CampaignOptionsTextField("Name");

        // Generate new random campaign name
        btnNameGenerator = new CampaignOptionsButton("NameGenerator");
        btnNameGenerator.addActionListener(e -> txtName.setText(BackgroundsController
                .randomMercenaryCompanyNameGenerator(campaign.getFlaggedCommander())));

        // Campaign faction
        lblFaction = new CampaignOptionsLabel("Faction");
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), campaign.getLocalDate()));

        // Reputation
        lblReputation = new CampaignOptionsLabel("Reputation");
        lblManualUnitRatingModifier = new CampaignOptionsLabel("ManualUnitRatingModifier");
        manualUnitRatingModifier = new CampaignOptionsSpinner("ManualUnitRatingModifier",
            0, -200, 200, 1);

        // Date
        lblDate = new CampaignOptionsLabel("Date");
        btnDate = new CampaignOptionsButton("Date");
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.addActionListener(this::btnDateActionPerformed);

        // Camouflage
        lblCamo = new CampaignOptionsLabel("Camo");
        btnCamo.setName("btnCamo");
        btnCamo.addActionListener(this::btnCamoActionPerformed);

        // Unit icon
        lblIcon = new CampaignOptionsLabel("Icon");
        btnIcon.setName("btnIcon");
        btnIcon.addActionListener(this::btnIconActionPerformed);

        // Initialize the parent panel
        AbstractMHQScrollablePanel generalPanel = new DefaultMHQScrollablePanel(frame,
            "generalPanel", new GridBagLayout());

        // Layout the Panel
        JPanel panel = new JPanel();
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 5;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblDate, layout);
        panel.add(btnDate, layout);

        layout.gridy++;
        panel.add(lblName, layout);

        layout.gridwidth = 2;
        layout.weightx = 1;
        panel.add(txtName, layout);

        layout.gridwidth = 1;
        layout.weightx = 0;
        layout.fill = GridBagConstraints.NONE;
        panel.add(btnNameGenerator, layout);
        layout.fill = GridBagConstraints.HORIZONTAL;

        layout.gridy++;
        panel.add(lblFaction, layout);
        layout.gridwidth = 2;
        panel.add(comboFaction, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblReputation, layout);
        layout.gridwidth = 2;
        panel.add(unitRatingMethodCombo, layout);

        layout.gridwidth = 1;
        layout.gridy++;
        panel.add(lblManualUnitRatingModifier, layout);
        panel.add(manualUnitRatingModifier, layout);

        layout.gridy++;
        layout.gridwidth = 5;
        layout.gridx = GridBagConstraints.RELATIVE;

        JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconsPanel.setBorder(BorderFactory.createEtchedBorder());

        iconsPanel.add(lblIcon);
        iconsPanel.add(btnIcon);
        iconsPanel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(50)));
        iconsPanel.add(lblCamo);
        iconsPanel.add(btnCamo);

        panel.add(iconsPanel, layout);
        layout.gridy++;
        panel.add(createFurtherReadingPanel(), layout);
        generalPanel.add(panel);

        return generalPanel;
    }

    /**
     * Creates a {@link JPanel} with a header containing an image, title, and body text.
     *
     * @return the created {@link JPanel} with the header
     */
    private static JPanel createGeneralHeader() {
        ImageIcon imageIcon = new ImageIcon("data/images/misc/MekHQ.png");
        JLabel imageLabel = new JLabel(imageIcon);

        final JLabel lblHeader = new JLabel(resources.getString("lblGeneral.text"), SwingConstants.CENTER);
        setFontScaling(lblHeader, true, 2);
        lblHeader.setName("lblGeneral");

        JLabel lblBody = new JLabel(String.format("<html>%s</html>",
            resources.getString("lblGeneralBody.text")), SwingConstants.CENTER);
        lblBody.setName("lblGeneralHeaderBody");

        final JPanel panel = new CampaignOptionsStandardPanel("pnlGeneralHeaderPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(lblHeader)
                .addComponent(imageLabel)
                .addComponent(lblBody)
                .addGap(UIUtil.scaleForGUI(20)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addComponent(lblHeader)
                .addComponent(imageLabel)
                .addComponent(lblBody));

        return panel;
    }

    /**
     * Initialize the components of the {@link GeneralTab} class.
     */
    private void initialize() {
        lblName = new JLabel();
        txtName = new JTextField();

        btnNameGenerator = new JButton();

        lblFaction = new JLabel();
        comboFaction = new MMComboBox<>("comboFaction", buildFactionDisplayOptions());

        lblReputation = new JLabel();
        unitRatingMethodCombo = new MMComboBox<>("unitRatingMethodCombo", UnitRatingMethod.values());

        lblManualUnitRatingModifier = new JLabel();
        manualUnitRatingModifier = new JSpinner();

        lblDate = new JLabel();
        btnDate = new JButton();

        lblCamo = new JLabel();
        btnCamo = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };

        lblIcon = new JLabel();
        btnIcon = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };
    }

    /**
     * Builds a {@link DefaultComboBoxModel} of {@link FactionDisplay} options.
     */
    private DefaultComboBoxModel<FactionDisplay> buildFactionDisplayOptions() {
        DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();

        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(
            Factions.getInstance().getChoosableFactions(), campaign.getLocalDate()));

        return factionModel;
    }

    /**
     * This method is called when the "btnDate" button is clicked.
     * It shows a date chooser dialog and sets the selected date if the user chooses a date.
     *
     * @param actionEvent The action event that triggered the method.
     */
    private void btnDateActionPerformed(ActionEvent actionEvent) {
        // show the date chooser
        DateChooser dateChooser = new DateChooser(frame, date);
        // user can either choose a date or cancel by closing
        if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
            setDate(dateChooser.getDate());
        }
    }

    /**
     * Sets the date for the {@link RepairAndMaintenanceTab}.
     * This method is called when the "btnDate" button is clicked.
     * It shows a date chooser dialog and sets the selected date if the user chooses a date.
     *
     * @param date The selected date. This parameter may contain a null value.
     */
    private void setDate(final @Nullable LocalDate date) {
        if (date == null) {
            return;
        }

        this.date = date;
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.revalidate();
        btnDate.repaint();

        final FactionDisplay factionDisplay = comboFaction.getSelectedItem();
        comboFaction.removeAllItems();
        ((DefaultComboBoxModel<FactionDisplay>) comboFaction.getModel()).addAll(FactionDisplay
            .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction.setSelectedItem(factionDisplay);
    }

    /**
     * This method is called when the "btnCamo" button is clicked.
     * It opens a {@link CamoChooserDialog} and sets the selected camouflage if the user confirms the
     * dialog.
     *
     * @param actionEvent The action event that triggered this method.
     */
    private void btnCamoActionPerformed(ActionEvent actionEvent) {
        CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame, camouflage);
        if (camoChooserDialog.showDialog().isConfirmed()) {
            camouflage = camoChooserDialog.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }

    /**
     * Event handler for the button action of btnIcon.
     * Opens a {@link UnitIconDialog} for the user to select a unit icon.
     * If selection is confirmed, the button's icon is updated to the selected one.
     *
     * @param actionEvent The {@link ActionEvent} object generated when button btnIcon is clicked.
     */
    private void btnIconActionPerformed(ActionEvent actionEvent) {
        final UnitIconDialog unitIconDialog = new UnitIconDialog(frame, unitIcon);
        if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
            unitIcon = unitIconDialog.getSelectedItem();
            btnIcon.setIcon(unitIcon.getImageIcon(UIUtil.scaleForGUI(75)));
        }
    }

    /**
     * Creates a {@link JPanel} named FurtherReadingPanel.
     * The panel contains other panels: headerPanelBMM, headerPanelTotalWarfare, and
     * headerPanelCampaignOperations, each defining a specific campaign option.
     * The panels are added to FurtherReadingPanel in top-to-bottom order.
     *
     * @return the constructed {@link JPanel}.
     */
    private JPanel createFurtherReadingPanel() {
        // Contents
        JPanel headerPanelBMM = new CampaignOptionsHeaderPanel("BMMPanel", "",
            true);

        JPanel headerPanelTotalWarfare = new CampaignOptionsHeaderPanel("TotalWarfarePanel",
            "", true);

        JPanel headerPanelCampaignOperations = new CampaignOptionsHeaderPanel("CampaignOperationsPanel",
            "", true);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("FurtherReadingPanel",
            true, "FurtherReadingPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);
        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(headerPanelBMM, layout);

        layout.gridy++;
        panel.add(headerPanelTotalWarfare, layout);

        layout.gridy++;
        panel.add(headerPanelCampaignOperations, layout);

        return panel;
    }

    void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null, null);
    }

    void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
                                       @Nullable LocalDate presetDate, @Nullable Faction presetFaction) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        txtName.setText(campaign.getName());

        comboFaction.setSelectedItem(campaign.getFaction());
        if (presetFaction != null) {
            comboFaction.setSelectedItem(new FactionDisplay(presetFaction, date));
        }

        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());

        date = campaign.getLocalDate();
        if (presetDate != null) {
            date = presetDate;
        }
        // Button labels are not updated when we repaint, so we have to specifically call it here
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));

        camouflage = campaign.getCamouflage();
        unitIcon = campaign.getUnitIcon();
    }

    void applyCampaignOptionsToCampaign() {
        campaign.setName(txtName.getText());

        FactionDisplay newFaction = comboFaction.getSelectedItem();
        if (newFaction != null) {
            campaign.setFaction(comboFaction.getSelectedItem().getFaction());
        }
        campaignOptions.setUnitRatingMethod(unitRatingMethodCombo.getSelectedItem());
        campaignOptions.setManualUnitRatingModifier((int) manualUnitRatingModifier.getValue());
        campaign.setLocalDate(date);
        campaign.setCamouflage(camouflage);
        campaign.setUnitIcon(unitIcon);
    }
}
