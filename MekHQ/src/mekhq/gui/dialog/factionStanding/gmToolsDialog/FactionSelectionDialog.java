/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.factionStanding.gmToolsDialog;

import static java.lang.Integer.MAX_VALUE;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.universe.factionStanding.FactionStandings.getMaximumSameFactionRegard;
import static mekhq.campaign.universe.factionStanding.FactionStandings.getMinimumRegard;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;


/**
 * A dialog allowing the player or game master to select a faction and modify its faction standing (Regard).
 *
 * <p>This dialog presents a list of all active and relevant factions, enabling the user to pick one and specify a
 * new standing value for it. Upon confirmation, the dialog provides access to the selected faction and the associated
 * standing value.</p>
 *
 * <p>This form is typically used within GM tools for campaign debugging, adjustment, or scenario setup.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionSelectionDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(FactionSelectionDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private final int PADDING = scaleForGUI(10);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);
    protected static final int CENTER_WIDTH = scaleForGUI(450);

    private ImageIcon campaignIcon;
    private final LocalDate today;
    private final FactionStandings factionStandings;
    private boolean actionWasConfirmed = false;

    private final List<Faction> allFactions = new ArrayList<>();
    private Faction selectedFaction = null;
    private MMComboBox<String> comboFaction;
    private double selectedRegard;

    /**
     * Constructs a new {@link FactionSelectionDialog}.
     *
     * @param parent           The parent dialog for positioning and modality.
     * @param campaignIcon     The campaign icon to display in the dialog.
     * @param factionStandings The current {@link FactionStandings} to use for available factions and modifying regard.
     * @param today            The current in-game date for determining active factions.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionSelectionDialog(JDialog parent, ImageIcon campaignIcon, FactionStandings factionStandings,
          LocalDate today) {
        this.campaignIcon = campaignIcon;
        this.today = today;
        this.factionStandings = factionStandings;

        populateFactionsList();
        populateDialog();
        initializeDialog(parent);
    }

    /**
     * Returns whether the user confirmed the selection (clicked the Confirm button).
     *
     * @return {@code true} if the Confirm action was taken, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasActionConfirmed() {
        return actionWasConfirmed;
    }


    /**
     * Gets the faction selected by the user when the dialog was confirmed.
     *
     * @return the selected {@link Faction}, or {@code null} if no selection was made.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Faction getSelectedFaction() {
        return selectedFaction;
    }

    /**
     * Gets the value of the faction regard selected when the dialog was confirmed.
     *
     * @return the selected standing (Regard) value.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getSelectedRegard() {
        return selectedRegard;
    }

    /**
     * Populates the internal list of factions for selection, including all active factions and any non-active faction
     * present in the standings.
     *
     * <p>Factions untracked for standings are excluded. The resulting list is sorted alphabetically.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    void populateFactionsList() {
        Factions factions = Factions.getInstance();
        List<Faction> activeFactions = new ArrayList<>(factions.getActiveFactions(today));

        for (String factionCode : factionStandings.getAllFactionStandings().keySet()) {
            Faction faction = factions.getFaction(factionCode);
            if (faction == null) {
                LOGGER.warn(new NullPointerException(), "Failed to find faction with code: {}", factionCode);
                continue;
            }

            if (!activeFactions.contains(faction)) {
                activeFactions.add(faction);
            }
        }

        activeFactions.removeIf(Faction::isAggregate);
        activeFactions.sort(Comparator.comparing(faction -> faction.getFullName(today.getYear())));

        allFactions.addAll(activeFactions);
    }

    /**
     * Initializes dialog window properties, sets its modality, default close operation, position relative to its
     * parent, and makes it visible.
     *
     * @param parent the parent dialog to relate this dialog's position to
     *
     * @author Illiani
     * @since 0.50.07
     */
    void initializeDialog(JDialog parent) {
        setTitle(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
        setModal(true);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    /**
     * Lays out and constructs the main panel components of the dialog, including the campaign image,
     * message/description area, faction selection combo, and confirmation.
     *
     * @author Illiani
     * @since 0.50.07
     */
    void populateDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for campaign icon
        JPanel pnlLeft = buildLeftPanel();
        pnlLeft.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
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

    /**
     * Builds a vertical panel displaying the campaign icon.
     *
     * @return the left-side panel containing the campaign image.
     *
     * @author Illiani
     * @since 0.50.07
     */
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

    /**
     * Builds the dialog's central panel, including the instruction/description area, faction combo, value spinner for
     * regard, and the confirmation button.
     *
     * <p>Handles updating of the spinner value when the faction selection changes.</p>
     *
     * @return central panel for dialog UI.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel populateCenterPanel() {
        JPanel pnlCenter = new JPanel();
        pnlCenter.setBorder(RoundedLineBorder.createRoundedLineBorder());
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        String description = getFormattedTextAt(RESOURCE_BUNDLE,
              "gmTools.SET_SPECIFIC_REGARD.pickFaction",
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG);
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>", CENTER_WIDTH, fontStyle, description));
        setFontScaling(editorPane, false, 1.1);
        pnlCenter.add(editorPane);

        // Use a sub-panel with GridBagLayout for label-input alignment
        JPanel pnlInputs = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;

        // Faction combo
        JLabel lblFaction = new JLabel(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.pickFaction"));
        comboFaction = new MMComboBox<>("comboFaction", buildFactionModel());
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlInputs.add(lblFaction, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        pnlInputs.add(comboFaction, gbc);

        // Regard spinner
        JLabel lblNewRegard = new JLabel(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.pickRegard"));
        int selectionIndex = comboFaction.getSelectedIndex();
        Faction currentSelectedFaction = allFactions.get(selectionIndex);
        double currentRegard = factionStandings.getRegardForFaction(currentSelectedFaction.getShortName(), false);
        JSpinner spnNewRegard = new JSpinner(new SpinnerNumberModel(currentRegard,
              getMinimumRegard(),
              getMaximumSameFactionRegard(),
              0.01));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        pnlInputs.add(lblNewRegard, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        pnlInputs.add(spnNewRegard, gbc);

        // Update spinner when combo selection changes
        comboFaction.addActionListener(e -> {
            int selectedIndex = comboFaction.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < allFactions.size()) {
                Faction selFaction = allFactions.get(selectedIndex);
                double newRegard = factionStandings.getRegardForFaction(selFaction.getShortName(), false);
                spnNewRegard.setValue(newRegard);
            }
        });

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(pnlInputs);
        pnlCenter.add(Box.createVerticalStrut(PADDING));

        RoundedJButton button = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.button.confirm"));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(evt -> {
            actionWasConfirmed = true;

            int selectedIndex = comboFaction.getSelectedIndex();
            selectedFaction = allFactions.get(selectedIndex);
            selectedRegard = (double) spnNewRegard.getValue();

            dispose();
        });
        pnlCenter.add(button);

        return pnlCenter;
    }


    /**
     * Constructs the combo box model containing all eligible faction names for the current year.
     *
     * @return combo box model of faction display names.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private DefaultComboBoxModel<String> buildFactionModel() {
        int gameYear = today.getYear();

        DefaultComboBoxModel<String> factionModel = new DefaultComboBoxModel<>();

        for (Faction faction : allFactions) {
            factionModel.addElement(faction.getFullName(gameYear));
        }

        return factionModel;
    }

    /**
     * Handles actions to perform when a hyperlink event occurs in the dialog, such as glossary lookups.
     *
     * @param evt the {@link HyperlinkEvent} that was received.
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            NewGlossaryDialog.handleGlossaryHyperlinkClick(this, evt);
        }
    }
}
