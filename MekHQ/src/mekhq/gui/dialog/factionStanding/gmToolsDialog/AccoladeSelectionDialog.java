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
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.PROPAGANDA_REEL;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;

/**
 * {@link AccoladeSelectionDialog} is a modal dialog allowing a user to select a faction and an accolade level for that
 * faction, as well as set whether the accolade is permanent. This dialog is used within the GM Tools dialog for
 * managing and editing faction standings and accolades given within a campaign.
 *
 * <p>The dialog displays a selectable list of factions and available accolade levels. It can also display an
 * associated campaign icon and supports a permanence option for the selected accolade. The user confirms the selection,
 * which can then be retrieved from this dialog instance.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class AccoladeSelectionDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(AccoladeSelectionDialog.class);
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

    private final List<FactionAccoladeLevel> allAccolades = Arrays.asList(FactionAccoladeLevel.values());
    private FactionAccoladeLevel selectedAccolade = null;
    private MMComboBox<String> comboAccolade;

    private boolean isPermanent = true;

    /**
     * Constructs an {@link AccoladeSelectionDialog} with the provided parent dialog, campaign icon, current faction
     * standings, and current date.
     *
     * @param parent           the parent dialog for modality; may be {@code null}
     * @param campaignIcon     the icon representing the campaign or context; may be {@code null}
     * @param factionStandings the current standings object to provide available factions
     * @param today            the current date (typically used for display or calculation)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public AccoladeSelectionDialog(JDialog parent, ImageIcon campaignIcon, FactionStandings factionStandings,
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
     * Gets the Accolade selected by the user when the dialog was confirmed.
     *
     * @return the selected {@link FactionAccoladeLevel}, or {@code null} if no selection was made.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionAccoladeLevel getSelectedAccolade() {
        return selectedAccolade;
    }

    /**
     * Gets the status of the Permanence checkbox when the dialog was confirmed.
     *
     * @return {@code true} if the accolade should be permanent
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean getIsPermanent() {
        return isPermanent;
    }

    /**
     * Populates the list of available factions for the dialog's selector.
     *
     * <p>Only includes factions valid for accolade assignment.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void populateFactionsList() {
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

        Faction piracySuccessIndex = factions.getFaction("PSI");
        activeFactions.add(0, piracySuccessIndex);

        Faction mercenaryOrganization = Faction.getActiveMercenaryOrganization(today.getYear());
        activeFactions.add(0, mercenaryOrganization);

        allFactions.clear();
        allFactions.addAll(activeFactions);
    }

    /**
     * Initializes the dialog UI, performing layout, event wiring, and other setup operations needed before display.
     *
     * @param parent the parent dialog (for positioning and modality)
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void initializeDialog(JDialog parent) {
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
     * Populates the dialog controls and components with initial data and layout.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void populateDialog() {
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
     * Builds and returns the panel containing the dialog's left-side content, such as the campaign icon or related
     * visual elements.
     *
     * @return a fully constructed {@link JPanel} for the left panel
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
     * Builds and returns the main center panel of the dialog UI, containing selectors and attribute controls.
     *
     * @return a fully constructed {@link JPanel} for the center panel
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
              "gmTools.TRIGGER_ACCOLADE.pickFaction",
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
        comboFaction.addActionListener(e -> {
            updateAccoladeForFactionSelection();
            clampAccoladeForFactionSelected();
        });

        // Accolade combo
        JLabel lblAccolade = new JLabel(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.pickAccolade"));
        comboAccolade = new MMComboBox<>("comboAccolade", buildAccoladeModel());
        gbc.gridx = 0;
        gbc.gridy = 1;
        pnlInputs.add(lblAccolade, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        pnlInputs.add(comboAccolade, gbc);
        comboAccolade.addActionListener(e -> clampAccoladeForFactionSelected());

        updateAccoladeForFactionSelection();
        clampAccoladeForFactionSelected();

        // Permanence
        JCheckBox chkPermanence = new JCheckBox(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.pickPermanence"));
        chkPermanence.setSelected(isPermanent);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        pnlInputs.add(chkPermanence, gbc);

        pnlCenter.add(Box.createVerticalStrut(PADDING));
        pnlCenter.add(pnlInputs);
        pnlCenter.add(Box.createVerticalStrut(PADDING));

        RoundedJButton button = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "gmTools.confirmation.button.confirm"));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(evt -> {
            actionWasConfirmed = true;

            int selectedFactionIndex = comboFaction.getSelectedIndex();
            selectedFaction = allFactions.get(selectedFactionIndex);

            int selectedAccoladesIndex = comboAccolade.getSelectedIndex();
            selectedAccolade = allAccolades.get(selectedAccoladesIndex);

            isPermanent = chkPermanence.isSelected();

            dispose();
        });
        pnlCenter.add(button);

        return pnlCenter;
    }

    /**
     * Updates the available accolade selection model when the selected faction is changed.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void updateAccoladeForFactionSelection() {
        int selectedIndex = comboFaction.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < allFactions.size()) {
            Faction selectedFaction = allFactions.get(selectedIndex);
            FactionAccoladeLevel newAccolade = factionStandings.getFactionJudgments()
                                                     .getAccoladeForFaction(selectedFaction.getShortName());
            int newIndex = newAccolade == null ? FactionAccoladeLevel.NO_ACCOLADE.ordinal() : newAccolade.ordinal();
            comboAccolade.setSelectedIndex(newIndex);
        }
    }

    /**
     * Validates and clamps the accolade level based on the current faction selection.
     *
     * <p>This prevents the user from choosing an accolade not permitted for the faction.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void clampAccoladeForFactionSelected() {
        int selectedFactionIndex = comboFaction.getSelectedIndex();
        Faction selectedFaction = allFactions.get(selectedFactionIndex);

        int selectedAccoladeIndex = comboAccolade.getSelectedIndex();
        FactionAccoladeLevel selectedAccolade = allAccolades.get(selectedAccoladeIndex);

        if (selectedFaction.isMercenaryOrganization() && !selectedAccolade.isMercenarySuitable()) {
            int newSelectedIndex = allAccolades.indexOf(PROPAGANDA_REEL);
            comboAccolade.setSelectedIndex(newSelectedIndex);
        }

        if (selectedFaction.getShortName().equals("PSI") && !selectedAccolade.isPirateSuitable()) {
            int newSelectedIndex = allAccolades.indexOf(PROPAGANDA_REEL);
            comboAccolade.setSelectedIndex(newSelectedIndex);
        }
    }

    /**
     * Builds the data model for the faction selector combo box.
     *
     * @return a {@link DefaultComboBoxModel} containing faction names for UI
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
     * Builds the data model for the accolade selector combo box.
     *
     * @return a {@link DefaultComboBoxModel} containing accolade level names for UI
     *
     * @author Illiani
     * @since 0.50.07
     */
    private DefaultComboBoxModel<String> buildAccoladeModel() {
        DefaultComboBoxModel<String> accoladeModel = new DefaultComboBoxModel<>();

        for (FactionAccoladeLevel level : allAccolades) {
            accoladeModel.addElement(level.toString());
        }

        return accoladeModel;
    }

    /**
     * Handles hyperlink events triggered in the dialog, such as opening documentation or help links.
     *
     * @param evt the hyperlink event to respond to
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            NewGlossaryDialog.handleGlossaryHyperlinkClick(this, evt);
        }
    }
}
