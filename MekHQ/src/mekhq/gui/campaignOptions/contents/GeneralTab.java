/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.contents;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
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
import mekhq.gui.campaignOptions.components.*;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.ResourceBundle;

import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createGroupLayout;

/**
 * Represents a tab within the campaign options UI that allows the user to configure
 * general campaign settings. This includes options for:
 * <p>
 *     <li>Configuring the campaign name</li>
 *     <li>Setting the faction and faction-related options</li>
 *     <li>Adjusting reputation and manual unit rating modifiers</li>
 *     <li>Specifying the campaign start date</li>
 *     <li>Choosing a camouflage pattern and unit icon</li>
 *
 *
 * This class extends the user interface features provided by {@link AbstractMHQTabbedPane}.
 */
public class GeneralTab {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

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

    /**
     * Constructs a new instance of the {@code GeneralTab} using the provided {@link Campaign} and {@link JFrame}.
     *
     * @param campaign The {@link Campaign} associated with this tab, which contains the core game data.
     * @param frame    The parent {@link JFrame} used to display this tab.
     */
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
     * Creates the UI components displayed in the general tab.
     * <p>
     * The general tab includes various configurable fields and panels:
     * <p>
     *     <li>An editable text field for setting the campaign name</li>
     *     <li>A dropdown for selecting the campaign's faction</li>
     *     <li>Controls for managing reputation and manual rating modifiers</li>
     *     <li>Date selection associated with the campaign</li>
     *     <li>Buttons for choosing camouflage and unit icons</li>
     *
     *
     * @return An {@link AbstractMHQScrollablePanel} containing the general tab content.
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
        comboFaction.setToolTipText(String.format("<html>%s</html>",
            resources.getString("lblFaction.tooltip")));

        // Reputation
        lblReputation = new CampaignOptionsLabel("Reputation");
        unitRatingMethodCombo.setToolTipText(String.format("<html>%s</html>",
            resources.getString("lblReputation.tooltip")));
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
        btnCamo.setIcon(camouflage.getImageIcon(UIUtil.scaleForGUI(75)));

        // Unit icon
        lblIcon = new CampaignOptionsLabel("Icon");
        btnIcon.setName("btnIcon");
        btnIcon.addActionListener(this::btnIconActionPerformed);
        btnIcon.setIcon(unitIcon.getImageIcon(UIUtil.scaleForGUI(75)));

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
        iconsPanel.setMinimumSize(UIUtil.scaleForGUI(0, 120));

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
     * Creates a header panel for the general tab, which includes:
     * <p>
     *     <li>An image representing the campaign options</li>
     *     <li>A title for the general tab</li>
     *     <li>A description of the general tab functionalities</li>
     *
     *
     * @return A {@link JPanel} containing the general tab header.
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
     * Initializes the UI components used throughout the general tab.
     * <p>
     * This method sets up the components for all editable campaign settings, including:
     * <p>
     *     <li>Labels, text fields, dropdowns, and buttons for campaign settings</li>
     *     <li>Default values fetched from the campaign instance</li>
     *
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
     * Builds a {@link DefaultComboBoxModel} containing faction options based on the current campaign data.
     * These options allow users to choose valid factions appropriate to the campaign's start date.
     *
     * @return A {@link DefaultComboBoxModel} populated with available {@link FactionDisplay} options.
     */
    private DefaultComboBoxModel<FactionDisplay> buildFactionDisplayOptions() {
        DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();

        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(
            Factions.getInstance().getChoosableFactions(), campaign.getLocalDate()));

        return factionModel;
    }

    /**
     * Handles the "Date" button action, which triggers a date selection via a {@link DateChooser} dialog.
     * If the user confirms their date choice, it updates the campaign's start date accordingly.
     *
     * @param actionEvent The {@link ActionEvent} triggered by the "Date" button.
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
     * Updates the campaign start date and refreshes the dependent UI components based on the changes.
     *
     * @param date The selected {@link LocalDate} to set as the campaign start date. Can be null.
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
     * Handles the "Camouflage" button action, which opens a {@link CamoChooserDialog}.
     * If the user confirms their camouflage selection, it updates the button icon to display the
     * chosen camouflage.
     *
     * @param actionEvent The {@link ActionEvent} triggered by the "Camouflage" button.
     */
    private void btnCamoActionPerformed(ActionEvent actionEvent) {
        CamoChooserDialog camoChooserDialog = new CamoChooserDialog(frame, camouflage);
        if (camoChooserDialog.showDialog().isConfirmed()) {
            camouflage = camoChooserDialog.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }

    /**
     * Handles the "Unit Icon" button action, which opens a {@link UnitIconDialog}.
     * If the user selects a new unit icon and confirms, this method updates the button icon to
     * reflect the selection.
     *
     * @param actionEvent The {@link ActionEvent} triggered by the "Unit Icon" button.
     */
    private void btnIconActionPerformed(ActionEvent actionEvent) {
        final UnitIconDialog unitIconDialog = new UnitIconDialog(frame, unitIcon);
        if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
            unitIcon = unitIconDialog.getSelectedItem();
            btnIcon.setIcon(unitIcon.getImageIcon(UIUtil.scaleForGUI(75)));
        }
    }

    /**
     * Creates a "Further Reading" panel that provides links or additional details to guide users
     * in understanding the campaign options.
     * <p>
     * The panel may include references to:
     * <p>
     *     <li>BattleMech Manual (BMM)</li>
     *     <li>Total Warfare rules</li>
     *     <li>Campaign Operations documentation</li>
     *
     *
     * @return A {@link JPanel} containing additional informational components.
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

    /**
     * Loads the values from the current campaign's {@link CampaignOptions} and updates the user interface components.
     * <p>
     * This is a convenience method that uses the default campaign options, default date, and default faction
     * to populate the relevant data fields in the user interface. It essentially delegates the work to the overloaded
     * method {@link #loadValuesFromCampaignOptions(CampaignOptions, LocalDate, Faction)} with all parameters set to {@code null}.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null, null);
    }

    /**
     * Loads values from the specified {@link CampaignOptions}, date, and faction into the user interface components.
     * <p>
     * This method updates the UI fields (e.g., text fields, combo boxes, and buttons) with the corresponding values
     * from the provided options or defaults to the current campaign's settings if no presets are provided.
     * <p>
     * Specific actions include:
     * <p>
     *     <li>Setting the campaign name and faction in the respective fields.</li>
     *     <li>Updating the unit rating method and manual unit rating modifier based on the campaign
     *     options.</li>
     *     <li>Synchronizing the date to the UI, accounting for a preset date if provided.</li>
     *     <li>Setting the camouflage pattern and unit icon to align with the campaign's default or
     *     custom configuration.</li>
     *     <li>Performing required UI updates (e.g., repainting date labels).</li>
     *
     *
     * @param presetCampaignOptions Optional {@link CampaignOptions} used to populate values.
     *                              If {@code null}, the current campaign options are used.
     * @param presetDate Optional {@link LocalDate} to be used as the active date.
     *                   If {@code null}, the campaign's current date is used.
     * @param presetFaction Optional {@link Faction} to be used in the faction selection field.
     *                      If {@code null}, the campaign's default faction is used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions,
                                              @Nullable LocalDate presetDate,
                                              @Nullable Faction presetFaction) {
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

    /**
     * Applies the updated campaign options from the general tab's UI components to the {@link Campaign}.
     * This method ensures that any changes made in the UI are reflected in the campaign's settings.
     *
     * @param presetCampaignOptions An optional {@link CampaignOptions} to apply instead of the campaign's current options.
     * @param isStartUp             A boolean indicating if the campaign is in a startup state.
     * @param isSaveAction          A boolean indicating if this is a save action.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions,
                                               boolean isStartUp, boolean isSaveAction) {
        // First, we apply any updates to the campaign
        if (!isSaveAction) {
            campaign.setName(txtName.getText());

            if (isStartUp) {
                campaign.getForces().setName(campaign.getName());
            }
            campaign.setLocalDate(date);

            if ((campaign.getCampaignStartDate() == null)
                || (campaign.getCampaignStartDate().isAfter(campaign.getLocalDate()))) {
                campaign.setCampaignStartDate(date);
            }
            // Ensure that the MegaMek year GameOption matches the campaign year
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());

            // Null state handled during validation
            FactionDisplay newFaction = comboFaction.getSelectedItem();
            if (newFaction != null) {
                campaign.setFaction(comboFaction.getSelectedItem().getFaction());
            }

            campaign.setCamouflage(camouflage);
            campaign.setUnitIcon(unitIcon);
        }

        // Then updates to Campaign Options
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        options.setUnitRatingMethod(unitRatingMethodCombo.getSelectedItem());
        options.setManualUnitRatingModifier((int) manualUnitRatingModifier.getValue());
    }
}
