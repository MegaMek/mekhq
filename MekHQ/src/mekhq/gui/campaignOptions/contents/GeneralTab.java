/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions.contents;

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.common.options.OptionsConstants.ALLOWED_YEAR;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createGroupLayout;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.util.DateUtilities;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.components.CampaignOptionsButton;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsTextField;
import mekhq.gui.dialog.DateChooser;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

/**
 * Represents a tab within the campaign options UI that allows the user to configure general campaign settings. This
 * includes options for:
 * <ul>
 *     <li>Configuring the campaign name</li>
 *     <li>Setting the faction and faction-related options</li>
 *     <li>Adjusting reputation and manual unit rating modifiers</li>
 *     <li>Specifying the campaign start date</li>
 *     <li>Choosing a camouflage pattern and unit icon</li>
 * </ul>
 * <p>
 * This class extends the user interface features provided by {@link AbstractMHQTabbedPane}.
 */
public class GeneralTab {
    private static final LocalDate RANDOM_DATE_EARLIEST = LocalDate.of(2775, 1, 1);
    private static final LocalDate RANDOM_DATE_LATEST = LocalDate.of(3151, 1, 1);

    private final JFrame frame;
    private final Campaign campaign;
    private final CampaignOptionsDialogMode mode;

    private JLabel lblName;
    private JTextField txtName;
    private RoundedJButton btnNameGenerator;
    private JLabel lblFaction;
    private MMComboBox<FactionDisplay> comboFaction;
    private RoundedJButton btnRandomFaction;
    private JLabel lblDate;
    private RoundedJButton btnDate;
    private RoundedJButton btnRandomDate;
    private LocalDate date;
    private JLabel lblCamo;
    private RoundedJButton btnCamo;
    private Camouflage camouflage;
    private JLabel lblIcon;
    private RoundedJButton btnIcon;
    private StandardForceIcon unitIcon;

    /**
     * Constructs a new instance of the {@code GeneralTab} using the provided {@link Campaign} and {@link JFrame}.
     *
     * @param campaign The {@link Campaign} associated with this tab, which contains the core game data.
     * @param frame    The parent {@link JFrame} used to display this tab.
     * @param mode     The {@link CampaignOptionsDialogMode} enum determining what state caused the dialog to be
     *                 triggered.
     */
    public GeneralTab(Campaign campaign, JFrame frame, CampaignOptionsDialogMode mode) {
        // region Variable Declarations
        this.frame = frame;
        this.campaign = campaign;
        this.date = campaign.getLocalDate();
        this.camouflage = campaign.getCamouflage();
        this.unitIcon = campaign.getUnitIcon();
        this.mode = mode;

        initialize();
    }

    /**
     * @return the currently selected date
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Retrieves the currently selected faction.
     *
     * <p>If no faction is selected, the method defaults to returning the "MERC" faction.</p>
     *
     * @return the {@link Faction} object representing the selected faction, or the "MERC" faction if no selection is
     *       made.
     */
    public Faction getFaction() {
        if (comboFaction.getSelectedItem() == null) {
            return Factions.getInstance().getFaction("MERC");
        } else {
            return comboFaction.getSelectedItem().getFaction();
        }
    }

    /**
     * Creates the UI components displayed in the general tab.
     * <p>
     * The general tab includes various configurable fields and panels:
     * </p>
     * <ul>
     *     <li>An editable text field for setting the campaign name</li>
     *     <li>A dropdown for selecting the campaign's faction</li>
     *     <li>Controls for managing reputation and manual rating modifiers</li>
     *     <li>Date selection associated with the campaign</li>
     *     <li>Buttons for choosing camouflage and unit icons</li>
     * </ul>
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
        btnNameGenerator.addActionListener(e -> txtName.setText(BackgroundsController.randomMercenaryCompanyNameGenerator(
              campaign.getCommander())));

        // Campaign faction
        lblFaction = new CampaignOptionsLabel("Faction");
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), campaign.getLocalDate()));
        comboFaction.setToolTipText(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblFaction.tooltip")));

        // Randomize faction
        btnRandomFaction = new CampaignOptionsButton("RandomFaction", getMetadata(MILESTONE_BEFORE_METADATA));
        btnRandomFaction.addActionListener(e -> {
            FactionDisplay randomFaction = pickRandomFaction();
            if (randomFaction != null) {
                comboFaction.setSelectedItem(randomFaction);
            }
        });

        // Date
        lblDate = new CampaignOptionsLabel("Date");
        btnDate = new CampaignOptionsButton("Date");
        btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.addActionListener(this::btnDateActionPerformed);

        // Randomize starting date
        btnRandomDate = new CampaignOptionsButton("RandomDate", getMetadata(MILESTONE_BEFORE_METADATA));
        btnRandomDate.addActionListener(e -> {
            LocalDate randomDate = DateUtilities.getRandomDateBetween(RANDOM_DATE_EARLIEST, RANDOM_DATE_LATEST);
            setDate(randomDate);
        });

        if (mode != CampaignOptionsDialogMode.STARTUP && mode != CampaignOptionsDialogMode.STARTUP_ABRIDGED) {
            lblDate.setEnabled(false);
            btnDate.setEnabled(false);
            btnRandomDate.setEnabled(false);
        }

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
              "generalPanel",
              new GridBagLayout());

        // Layout the Panel
        JPanel panel = new JPanel();
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 5;
        layout.weightx = 1.0;
        layout.fill = GridBagConstraints.HORIZONTAL;
        panel.add(headerPanel, layout);

        layout.gridwidth = 1;
        layout.weightx = 1;
        layout.gridy++;
        panel.add(lblDate, layout);
        panel.add(btnDate, layout);
        panel.add(btnRandomDate, layout);

        layout.gridy++;
        panel.add(lblName, layout);
        panel.add(txtName, layout);

        panel.add(btnNameGenerator, layout);

        layout.gridy++;
        panel.add(lblFaction, layout);
        panel.add(comboFaction, layout);
        panel.add(btnRandomFaction, layout);

        layout.gridy++;
        layout.gridwidth = 5;
        layout.gridx = GridBagConstraints.RELATIVE;

        JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconsPanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        iconsPanel.setMinimumSize(UIUtil.scaleForGUI(0, 120));

        iconsPanel.add(lblIcon);
        iconsPanel.add(btnIcon);
        iconsPanel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(50)));
        iconsPanel.add(lblCamo);
        iconsPanel.add(btnCamo);

        panel.add(iconsPanel, layout);
        layout.gridy++;
        layout.gridwidth = 5;
        panel.add(createFurtherReadingPanel(), layout);
        generalPanel.add(panel);

        return generalPanel;
    }

    /**
     * Creates a header panel for the general tab, which includes:
     * <p>
     * <li>An image representing the campaign options</li>
     * <li>A title for the general tab</li>
     * <li>A description of the general tab functionalities</li>
     * </p>
     *
     * @return A {@link JPanel} containing the general tab header.
     */
    private static JPanel createGeneralHeader() {
        ImageIcon imageIcon = new ImageIcon("data/images/misc/MekHQ.png");
        imageIcon = scaleImageIcon(imageIcon, 200, true);
        JLabel imageLabel = new JLabel(imageIcon);

        final JLabel lblHeader = new JLabel(getTextAt(getCampaignOptionsResourceBundle(), "lblGeneral.text"),
              SwingConstants.CENTER);
        lblHeader.setMinimumSize(UIUtil.scaleForGUI(600, 0));
        setFontScaling(lblHeader, true, 2);
        lblHeader.setName("lblGeneral");

        JLabel lblBody = new JLabel(String.format("<html>%s</html>",
              getTextAt(getCampaignOptionsResourceBundle(), "lblGeneralBody.text")),
              SwingConstants.CENTER);
        lblBody.setName("lblGeneralHeaderBody");

        final JPanel panel = new CampaignOptionsStandardPanel("pnlGeneralHeaderPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(lblHeader)
                                      .addComponent(imageLabel)
                                      .addComponent(lblBody)
                                      .addGap(UIUtil.scaleForGUI(20)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
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
     * <li>Labels, text fields, dropdowns, and buttons for campaign settings</li>
     * <li>Default values fetched from the campaign instance</li>
     * </p>
     */
    private void initialize() {
        lblName = new JLabel();
        txtName = new JTextField();

        btnNameGenerator = new RoundedJButton();

        lblFaction = new JLabel();
        comboFaction = new MMComboBox<>("comboFaction", buildFactionDisplayOptions());
        btnRandomFaction = new RoundedJButton();

        lblDate = new JLabel();
        btnDate = new RoundedJButton();
        btnRandomDate = new RoundedJButton();

        lblCamo = new JLabel();
        btnCamo = new RoundedJButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };

        lblIcon = new JLabel();
        btnIcon = new RoundedJButton() {
            @Override
            public Dimension getPreferredSize() {
                return UIUtil.scaleForGUI(100, 100);
            }
        };
    }

    /**
     * Builds a {@link DefaultComboBoxModel} containing faction options based on the current campaign data. These
     * options allow users to choose valid factions appropriate to the campaign's start date.
     *
     * @return A {@link DefaultComboBoxModel} populated with available {@link FactionDisplay} options.
     */
    private DefaultComboBoxModel<FactionDisplay> buildFactionDisplayOptions() {
        DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();

        factionModel.addAll(FactionDisplay.getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(),
              date));

        return factionModel;
    }

    /**
     * Picks and returns a random {@link FactionDisplay} from the available faction display options.
     *
     * <p>This method builds a {@link DefaultComboBoxModel} of faction options using
     * {@link #buildFactionDisplayOptions()}. If the model contains any options, a random index is selected and the
     * corresponding {@link FactionDisplay} is returned. If no options are available, this method returns
     * {@code null}.</p>
     *
     * @return a randomly selected {@link FactionDisplay}, or {@code null} if no factions are available
     *
     * @author Illiani
     * @since 0.50.07
     */
    private @Nullable FactionDisplay pickRandomFaction() {
        List<FactionDisplay> factionOptions = FactionDisplay.getSortedValidFactionDisplays(
              Factions.getInstance().getChoosableFactions(), date);
        return ObjectUtility.getRandomItem(factionOptions);
    }

    /**
     * Handles the "Date" button action, which triggers a date selection via a {@link DateChooser} dialog. If the user
     * confirms their date choice, it updates the campaign's start date accordingly.
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
        ((DefaultComboBoxModel<FactionDisplay>) comboFaction.getModel()).addAll(FactionDisplay.getSortedValidFactionDisplays(
              Factions.getInstance().getChoosableFactions(),
              date));
        comboFaction.setSelectedItem(factionDisplay);
    }

    /**
     * Handles the "Camouflage" button action, which opens a {@link CamoChooserDialog}. If the user confirms their
     * camouflage selection, it updates the button icon to display the chosen camouflage.
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
     * Handles the "Unit Icon" button action, which opens a {@link UnitIconDialog}. If the user selects a new unit icon
     * and confirms, this method updates the button icon to reflect the selection.
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
     * Creates a "Further Reading" panel that provides links or additional details to guide users in understanding the
     * campaign options.
     *
     * @return A {@link JPanel} containing additional informational components.
     */
    private JPanel createFurtherReadingPanel() {
        JLabel lblFurtherReading = new CampaignOptionsLabel("FurtherReading", null, true);

        final JPanel panel = new CampaignOptionsStandardPanel("FurtherReadingPanel", true, "FurtherReadingPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridx = 0;
        layout.gridy = 0;
        layout.fill = GridBagConstraints.HORIZONTAL;
        layout.weightx = 1.0;

        panel.add(lblFurtherReading, layout);

        return panel;
    }

    /**
     * Loads the values from the current campaign's {@link CampaignOptions} and updates the user interface components.
     * <p>
     * This is a convenience method that uses the default campaign options, default date, and default faction to
     * populate the relevant data fields in the user interface. It essentially delegates the work to the overloaded
     * method {@link #loadValuesFromCampaignOptions(LocalDate, Faction)} with all parameters set to {@code null}.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null, null);
    }

    /**
     * Loads values from the specified {@link CampaignOptions}, date, and faction into the user interface components.
     * <p>
     * This method updates the UI fields (e.g., text fields, combo boxes, and buttons) with the corresponding values
     * from the provided options or defaults to the current campaign's settings if no presets are provided.
     * <p>
     * Specific actions include:
     * <ul>
     *     <li>Setting the campaign name and faction in the respective fields.</li>
     *     <li>Updating the unit rating method and manual unit rating modifier based on the campaign
     *     options.</li>
     *     <li>Synchronizing the date to the UI, accounting for a preset date if provided.</li>
     *     <li>Setting the camouflage pattern and unit icon to align with the campaign's default or
     *     custom configuration.</li>
     *     <li>Performing required UI updates (e.g., repainting date labels).</li>
     * </ul>
     * <p>
     *  @param presetDate            Optional {@link LocalDate} to be used as the active date. If {@code null}, the
     *                              campaign's current date is used.
     *
     * @param presetFaction Optional {@link Faction} to be used in the faction selection field. If {@code null}, the
     *                      campaign's default faction is used.
     */
    public void loadValuesFromCampaignOptions(@Nullable LocalDate presetDate, @Nullable Faction presetFaction) {
        txtName.setText(campaign.getName());

        setDate((presetDate != null) ? presetDate : campaign.getLocalDate());

        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), date));
        if (presetFaction != null) {
            comboFaction.setSelectedItem(new FactionDisplay(presetFaction, date));
        }

        camouflage = campaign.getCamouflage();
        unitIcon = campaign.getUnitIcon();
    }

    /**
     * Applies the updated campaign options from the general tab's UI components to the {@link Campaign}. This method
     * ensures that any changes made in the UI are reflected in the campaign's settings.
     *
     * @param isStartUp    A boolean indicating if the campaign is in a startup state.
     * @param isSaveAction A boolean indicating if this is a save action.
     */
    public void applyCampaignOptionsToCampaign(boolean isStartUp, boolean isSaveAction) {
        // First, we apply any updates to the campaign
        if (!isSaveAction) {
            campaign.setName(txtName.getText());

            if (isStartUp) {
                campaign.getForces().setName(campaign.getName());
                campaign.setLocalDate(date);
            }

            if ((campaign.getCampaignStartDate() == null) ||
                      (campaign.getCampaignStartDate().isAfter(campaign.getLocalDate()))) {
                campaign.setCampaignStartDate(date);
            }
            // Ensure that the MegaMek year GameOption matches the campaign year
            campaign.getGameOptions().getOption(ALLOWED_YEAR).setValue(campaign.getGameYear());

            // Null state handled during validation
            FactionDisplay newFaction = comboFaction.getSelectedItem();
            if (newFaction != null) {
                campaign.setFaction(newFaction.getFaction());
            }

            campaign.setCamouflage(camouflage);
            campaign.setUnitIcon(unitIcon);
        }
    }
}
