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
package mekhq.gui.dialog.factionStanding;

import static java.lang.Math.round;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;
import static mekhq.gui.dialog.factionStanding.manualMissionDialogs.SimulateMissionDialog.handleFactionRegardUpdates;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.Border;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.factionStanding.gmToolsDialog.GMTools;
import mekhq.gui.dialog.factionStanding.manualMissionDialogs.SimulateMissionDialog;
import mekhq.gui.dialog.glossary.NewDocumentationEntryDialog;
import mekhq.gui.utilities.WrapLayout;

/**
 * Displays a dialog window that visualizes a report on faction standings for the current campaign year. Shows
 * individual faction panels with images, standing levels, regard sliders, and interactive details on standing effects.
 *
 * <p>This dialog provides a convenient summary as well as documentation and GM tool links related to faction
 * standings.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingReport extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingReport.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    /**
     * A label name constant for the Effects Panel in the Faction Standing Report dialog.
     *
     * <p>This value is used to reference the specific UI component and allow us to dynamically update the faction
     * standing effects display.</p>
     */
    private static final String EFFECTS_PANEL_LABEL_NAME = "lblFactionStandingEffects";

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int FACTION_PANEL_WIDTH = UIUtil.scaleForGUI(500);
    private static final int FACTION_PANEL_HEIGHT = UIUtil.scaleForGUI(400);
    private static final int FACTION_DESCRIPTION_WIDTH = FACTION_PANEL_WIDTH;
    private static final int FACTION_DESCRIPTION_HEIGHT = UIUtil.scaleForGUI(200);
    private static final int FACTION_EFFECTS_MINIMUM_HEIGHT = UIUtil.scaleForGUI(70);
    private static final int REPORT_BUTTONS_MAXIMUM_HEIGHT = UIUtil.scaleForGUI(30);
    private static final int REPORT_BUTTON_SPACE_WIDTH = UIUtil.scaleForGUI(50);
    private static final int REPORT_IMAGE_WIDTH = 100; // Scaled by scaleImageIcon call

    private final JFrame frame;
    private final Campaign campaign;
    private final LocalDate today;
    private final int gameYear;
    private final FactionStandings factionStandings;
    private final Factions factions;
    private final Faction campaignFaction;
    private final boolean isFactionStandingEnabled;
    private final CampaignOptions campaignOptions;

    private final boolean hideClanFactions;
    private final boolean hideNonClanFactions;

    private final List<String> innerSphereFactions = new ArrayList<>();
    private final List<String> innerSphereMinorFactions = new ArrayList<>();
    private final List<String> clanFactions = new ArrayList<>();
    private final List<String> peripheryFactions = new ArrayList<>();
    private final List<String> deepPeripheryFactions = new ArrayList<>();
    private final List<String> specialFactions = new ArrayList<>();
    private final List<String> deadFactions = new ArrayList<>();

    private final List<String> reports = new ArrayList<>();

    /**
     * Constructs a {@link FactionStandingReport} which generates a {@link JDialog} displaying faction standings for the
     * specified campaign and related data.
     *
     * @param frame    The parent {@link JFrame} that acts as the owner of this report dialog.
     * @param campaign The current campaign
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingReport(final JFrame frame, final Campaign campaign) {
        this.frame = frame;
        this.campaign = campaign;
        this.today = campaign.getLocalDate();
        this.gameYear = today.getYear();
        this.campaignFaction = campaign.getFaction();
        this.factionStandings = campaign.getFactionStandings();
        factions = Factions.getInstance();
        this.campaignOptions = campaign.getCampaignOptions();
        this.isFactionStandingEnabled = campaignOptions.isTrackFactionStanding();

        // We minus a day as otherwise this will return false if today is the first day of the First Wave
        boolean clanInvasionHasBegun = MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS.minusDays(1).isBefore(today);
        boolean campaignIsClan = campaignFaction.isClan();
        hideClanFactions = !clanInvasionHasBegun && !campaignIsClan;
        hideNonClanFactions = !clanInvasionHasBegun && campaignIsClan;

        sortFactions();
        createReportPanel();
        initializeDialogParameters();
    }

    /**
     * @return a list of Faction Standing change reports.
     */
    public List<String> getReports() {
        return reports;
    }

    /**
     * Initializes dialog window parameters, such as title, size, modality, and visibility.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void initializeDialogParameters() {
        setTitle(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.title"));

        // Just big enough for one faction panel
        int dialogDefaultWidth = (int) round(FACTION_PANEL_WIDTH * 1.15);
        int combinedHeight = FACTION_PANEL_HEIGHT + FACTION_EFFECTS_MINIMUM_HEIGHT + REPORT_BUTTONS_MAXIMUM_HEIGHT;
        int dialogDefaultHeight = (int) round(combinedHeight * 1.25);
        setMinimumSize(new Dimension(dialogDefaultWidth, dialogDefaultHeight));

        setResizable(true);
        setModal(true);
        setPreferences(); // Must be before setVisible
        setLocationRelativeTo(frame);

        setVisible(true); // Should always be last
    }

    /**
     * Sorts all factions into appropriate categories based on their current standing and properties.
     *
     * <p>The method collects all faction codes from both the overall standings and the climate regard, combining
     * them into a sorted list. For each faction, it determines if the faction:</p>
     *
     * <ul>
     *     <li>is no longer valid in the current game year (added to {@code deadFactions}),</li>
     *     <li>is a Clan-type faction (added to {@code clanFactions}),</li>
     *     <li>is a Periphery-type faction (added to {@code peripheryFactions}),</li>
     *     <li>or is an Inner Sphere faction (added to {@code innerSphereFactions}).</li>
     * </ul>
     *
     * <p>Logs an error if a faction code cannot be resolved to a {@code Faction}.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void sortFactions() {
        Set<String> allFactionStandingsSet = factionStandings.getAllFactionStandings().keySet();
        List<String> sortedFactionStandings = new ArrayList<>(allFactionStandingsSet);
        for (String factionCode : factionStandings.getAllClimateRegard().keySet()) {
            if (!allFactionStandingsSet.contains(factionCode)) {
                sortedFactionStandings.add(factionCode);
            }
        }
        Collections.sort(sortedFactionStandings);

        for (String factionCode : sortedFactionStandings) {
            Faction faction = factions.getFaction(factionCode);
            if (faction == null) {
                LOGGER.error(new NullPointerException(), "Failed to find faction with code: {}", factionCode);
                continue;
            }

            boolean factionIsClan = faction.isClan();
            if ((factionIsClan && hideClanFactions) || (!factionIsClan && hideNonClanFactions)) {
                continue;
            }

            if (!faction.validIn(gameYear)) {
                deadFactions.add(factionCode);
            } else if (faction.isMercenaryOrganization() || factionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE)) {
                specialFactions.add(factionCode);
            } else if (factionIsClan) {
                clanFactions.add(factionCode);
            } else if (faction.isDeepPeriphery()) {
                deepPeripheryFactions.add(factionCode);
            } else if (faction.isPeriphery()) {
                peripheryFactions.add(factionCode);
            } else if (faction.isMinorPower()) {
                innerSphereMinorFactions.add(factionCode);
            } else {
                innerSphereFactions.add(factionCode);
            }
        }
    }

    /**
     * Constructs the main report panel for the faction standing report dialog.
     *
     * <p>This method creates a tabbed pane, adding a separate tab for each faction category (Inner Sphere, Clan,
     * Periphery, and Dead) with content provided by {@code createReportPanelForFactionGroup} using the relevant faction
     * lists. Each tab's title is retrieved from a resource bundle for localization.</p>
     *
     * <p>If a tab's panel is empty, that tab is disabled to prevent selection. The method also creates and sets up
     * effects and buttons panels, applies layout configuration and font scaling, and then assembles all components in a
     * vertically stacked panel, which is set as the dialog's main content pane.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void createReportPanel() {
        // Create the tabbed pane
        String innerSphereTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.innerSphere");
        String innerSphereMinorTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.innerSphere.minor");
        String clanTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.clan");
        String peripheryTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.periphery");
        String deepPeripheryTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.deepPeriphery");
        String specialTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.special");
        String deadTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.dead");
        String disabledTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.disabled");

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setName("tabbedPane");
        if (isFactionStandingEnabled) {
            Object[][] tabs = {
                  { innerSphereTabTitle, innerSphereFactions },
                  { innerSphereMinorTabTitle, innerSphereMinorFactions },
                  { clanTabTitle, clanFactions },
                  { peripheryTabTitle, peripheryFactions },
                  { deepPeripheryTabTitle, deepPeripheryFactions },
                  { specialTabTitle, specialFactions },
                  { deadTabTitle, deadFactions }
            };

            for (Object[] tab : tabs) {
                String title = (String) tab[0];
                @SuppressWarnings("unchecked")
                List<String> factions = (List<String>) tab[1];
                if (!factions.isEmpty()) {
                    tabbedPane.addTab(title, createReportPanelForFactionGroup(factions));
                }
            }
        } else {
            tabbedPane.addTab(disabledTitle, createFactionStandingDisabledTab());
        }
        setFontScaling(tabbedPane, true, 1.5);

        // If a tab only contains an empty Container, disable it. This will occur if the relevant faction list is empty.
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            boolean isEmpty = (component instanceof Container) && ((Container) component).getComponentCount() == 0;
            tabbedPane.setEnabledAt(i, !isEmpty);
        }

        // Create effects and buttons panels
        JPanel pnlEffects = createEffectsPanel();
        pnlEffects.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JPanel pnlButtons = createButtonsPanel();
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Main report panel with vertical stacking
        JPanel pnlReport = new JPanel();
        pnlReport.setLayout(new BoxLayout(pnlReport, BoxLayout.Y_AXIS));
        pnlReport.add(tabbedPane);
        pnlReport.add(pnlEffects);
        pnlReport.add(pnlButtons);

        setContentPane(pnlReport);
    }

    /**
     * Creates the main report panel. Contains the scrollable faction standings panel, effects panel, and buttons
     * panel.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createReportPanelForFactionGroup(List<String> factions) {
        if (factions.isEmpty()) {
            return new JPanel();
        }

        JPanel pnlFactionReport = new JPanel();
        pnlFactionReport.setName("factionReportPanel" + factions);
        pnlFactionReport.setLayout(new BoxLayout(pnlFactionReport, BoxLayout.Y_AXIS));

        JPanel groupPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, PADDING, PADDING));
        groupPanel.setName("factionReportGroupPanel" + factions);
        for (String faction : factions) {
            JPanel factionPanel = createFactionPanel(faction);
            groupPanel.add(factionPanel);
        }
        JScrollPane groupScrollPane = new FastJScrollPane(groupPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupScrollPane.setName("factionReportGroupScrollPane" + factions);
        groupScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());

        pnlFactionReport.add(groupScrollPane);

        return pnlFactionReport;
    }

    private JPanel createFactionStandingDisabledTab() {
        JTextPane textPane = new JTextPane();
        textPane.setText(getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.disabled.blurb"));
        textPane.setEditable(false);
        textPane.setOpaque(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textPane, BorderLayout.CENTER);
        panel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        return panel;
    }

    /**
     * Constructs the panel that shows standing effects or explanatory text.
     *
     * @return a configured {@link JPanel} containing the standing effects text area
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createEffectsPanel() {
        JPanel pnlEffects = new JPanel(new BorderLayout());
        pnlEffects.setName("pnlFactionStandingEffects");

        JTextArea lblStandingEffects = new JTextArea();
        lblStandingEffects.setName(EFFECTS_PANEL_LABEL_NAME);
        lblStandingEffects.setEditable(false);
        lblStandingEffects.setWrapStyleWord(true);
        lblStandingEffects.setLineWrap(true);
        lblStandingEffects.setOpaque(false);
        lblStandingEffects.setBorder(null);
        lblStandingEffects.setFocusable(false);
        lblStandingEffects.setText("");

        pnlEffects.add(lblStandingEffects, BorderLayout.SOUTH);

        return pnlEffects;
    }

    /**
     * Constructs a panel with documentation and GM tools buttons.
     *
     * @return a configured {@link JPanel} containing dialog buttons
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createButtonsPanel() {
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlButtons.setName("pnlButtons");
        pnlButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, REPORT_BUTTONS_MAXIMUM_HEIGHT));

        RoundedJButton btnDocumentation = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "factionStandingReport.button.documentation"));
        btnDocumentation.setName("btnDocumentation");
        btnDocumentation.addActionListener(e -> {
            DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(
                  "FACTION_STANDINGS");

            if (documentationEntry == null) {
                LOGGER.warn("Glossary entry not found: {}", "FACTION_STANDINGS");
                return;
            }

            new NewDocumentationEntryDialog(this, documentationEntry);
        });
        btnDocumentation.setFocusable(false);
        pnlButtons.add(btnDocumentation);

        pnlButtons.add(Box.createHorizontalStrut(REPORT_BUTTON_SPACE_WIDTH));

        RoundedJButton btnGmTools = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "factionStandingReport.button.gmTools"));
        btnGmTools.setName("btnSimulateContract");
        btnGmTools.setFocusable(false);
        btnGmTools.setEnabled(isFactionStandingEnabled && campaign.isGM());
        btnGmTools.addActionListener(e -> {
            setVisible(false);
            GMTools gmTools = new GMTools(this, campaign);
            reports.addAll(gmTools.getReports());
            setVisible(true);
        });
        pnlButtons.add(btnGmTools);

        pnlButtons.add(Box.createHorizontalStrut(REPORT_BUTTON_SPACE_WIDTH));

        RoundedJButton btnSimulateContract = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "factionStandingReport.button.contract"));
        btnSimulateContract.setName("btnSimulateContract");
        btnSimulateContract.setFocusable(false);
        btnSimulateContract.setEnabled(isFactionStandingEnabled);
        btnSimulateContract.addActionListener(e -> {
            setVisible(false);
            triggerMissionSimulationDialog();
            setVisible(true);
        });
        pnlButtons.add(btnSimulateContract);

        return pnlButtons;
    }

    /**
     * Opens the Simulate Mission dialog, allowing the user to choose the employer and enemy factions, as well as the
     * mission status. After selections are made, the method updates faction standings accordingly.
     *
     * <p>This method blocks until the dialog is closed, then retrieves the selected values and applies any necessary
     * updates to faction standings.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void triggerMissionSimulationDialog() {
        SimulateMissionDialog dialog = new SimulateMissionDialog(frame, campaign.getCampaignFactionIcon(),
              campaignFaction, today);

        Faction employerChoice = dialog.getEmployerChoice();
        Faction enemyChoice = dialog.getEnemyChoice();
        MissionStatus statusChoice = dialog.getStatusChoice();
        int durationChoice = dialog.getDurationChoice();

        reports.addAll(handleFactionRegardUpdates(campaignFaction, employerChoice, enemyChoice, statusChoice, today,
              factionStandings, campaignOptions.getRegardMultiplier(), durationChoice));
    }

    /**
     * Constructs a panel describing the specified faction, including logo, description, and regard slider.
     *
     * @param factionCode the code of the faction to be displayed
     *
     * @return a {@link JPanel} representing the faction's standing information
     *
     * @author Illiani
     * @since 0.50.07
     */
    private JPanel createFactionPanel(final String factionCode) {
        final Faction faction = factions.getFaction(factionCode);
        if (faction == null) {
            LOGGER.error(new NullPointerException(),
                  "Failed to find faction with code: {} - skipping faction panel",
                  factionCode);
            JPanel lblEmptyPanelFromNullFaction = new JPanel();
            lblEmptyPanelFromNullFaction.setName("lblEmptyPanelFromNullFaction" + factionCode);
            return lblEmptyPanelFromNullFaction;
        }

        final double factionRegard = factionStandings.getRegardForFaction(factionCode, false);
        final double climateRegard = factionStandings.getRegardForFaction(factionCode, true);
        final FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(
              climateRegard);

        // Parent panel
        boolean isMercenaryOrganization = faction.isMercenaryOrganization();
        boolean isClan = !isMercenaryOrganization && faction.isClan();
        boolean isPirateOrMercenaryOrganization = isMercenaryOrganization ||
                                                        factionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE);

        JPanel pnlFactionStanding = new JPanel();
        pnlFactionStanding.setName("pnlFactionStanding" + factionCode);
        pnlFactionStanding.setLayout(new BoxLayout(pnlFactionStanding, BoxLayout.Y_AXIS));
        pnlFactionStanding.setBorder(createStandingColoredRoundedTitledBorder(factionStanding.getStandingLevel()));
        pnlFactionStanding.setPreferredSize(new Dimension(FACTION_PANEL_WIDTH, FACTION_PANEL_HEIGHT));
        pnlFactionStanding.setMaximumSize(new Dimension(FACTION_PANEL_WIDTH, FACTION_PANEL_HEIGHT));
        pnlFactionStanding.addMouseListener(createEffectsPanelUpdater(getEffectsDescription(isClan,
              isPirateOrMercenaryOrganization, climateRegard)));

        // Faction Logo
        ImageIcon icon = Factions.getFactionLogo(gameYear, factionCode);
        icon = ImageUtilities.scaleImageIcon(icon, REPORT_IMAGE_WIDTH, true);
        JLabel lblFactionImage = new JLabel(icon);
        lblFactionImage.setName("lblFactionImage" + factionCode);
        lblFactionImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblFactionImage.getPreferredSize().height));
        lblFactionImage.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        pnlFactionStanding.add(lblFactionImage);

        // Faction Descriptions
        String factionDescription = getDescriptionForFaction(faction, climateRegard);
        JLabel lblDetails = new JLabel(factionDescription);
        lblDetails.setPreferredSize(new Dimension(FACTION_DESCRIPTION_WIDTH, FACTION_DESCRIPTION_HEIGHT));
        lblDetails.setMaximumSize(new Dimension(FACTION_DESCRIPTION_WIDTH, FACTION_DESCRIPTION_HEIGHT));
        lblDetails.setName("lblFactionDetails" + factionCode);
        lblDetails.setAlignmentX(CENTER_ALIGNMENT);
        lblDetails.setHorizontalAlignment(SwingConstants.CENTER);
        pnlFactionStanding.add(lblDetails);

        // Regard Slider
        JSlider sldRegard = getRegardSlider(factionCode, factionRegard, climateRegard);
        pnlFactionStanding.add(sldRegard);

        return pnlFactionStanding;
    }

    /**
     * Creates a Compound Border consisting of a {@code RoundedLineBorder} colored according to the specified faction
     * standing level, combined with internal padding.
     *
     * <p>The color selection is determined by the faction standing level:<br>
     * <ul>
     *     <li>≤ 1: Negative color</li>
     *     <li>≤ 3: Warning color</li>
     *     <li>≥ 7: Amazing color</li>
     *     <li>≥ 5: Positive color</li>
     * </ul>
     *
     * <p>If standing is neutral, gray is used by default.</p>
     *
     * @param factionStandingLevel the numeric standing level of the faction which determines the border color
     *
     * @return a compound border with a colored rounded line border and padding
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static Border createStandingColoredRoundedTitledBorder(final int factionStandingLevel) {
        Border rounded = getRoundedBorder(factionStandingLevel);
        Border padding = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING);
        Border compound = BorderFactory.createCompoundBorder(rounded, padding);

        int stars = factionStandingLevel + 1;

        return BorderFactory.createTitledBorder(compound, "\u2605 ".repeat(Math.max(0, stars)));
    }

    private static Border getRoundedBorder(int factionStandingLevel) {
        Color color;
        if (factionStandingLevel >= 7) {
            color = MekHQ.getMHQOptions().getFontColorAmazing();
        } else if (factionStandingLevel >= 5) {
            color = MekHQ.getMHQOptions().getFontColorPositive();
        } else if (factionStandingLevel == 4) {
            color = UIUtil.uiIndependentGray();
        } else if (factionStandingLevel > 1) {
            color = MekHQ.getMHQOptions().getFontColorWarning();
        } else {
            color = MekHQ.getMHQOptions().getFontColorNegative();
        }

        return new RoundedLineBorder(color, 2, 16);
    }

    /**
     * Constructs the HTML markup string used to describe a faction's details and standing.
     *
     * @param faction       the faction object
     * @param factionRegard the regard value for this faction
     *
     * @return an HTML string for displaying faction details, standing, and description
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getDescriptionForFaction(Faction faction, double factionRegard) {
        String factionName = faction.getFullName(gameYear);
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionRegard);
        String factionStandingLabel = factionStanding.getLabel(faction);
        String factionStandingDescription = factionStanding.getDescription(faction);

        FactionHints factionHints = FactionHints.getInstance();
        LocalDate firstOfMonth = today.withDayOfMonth(1); // Climate states update on the 1st in Faction Standing
        boolean isAtWar = factionHints.isAtWarWith(campaignFaction, faction, firstOfMonth);
        boolean isAllied = factionHints.isAlliedWith(campaignFaction, faction, firstOfMonth);
        boolean isRival = factionHints.isRivalOf(campaignFaction, faction, firstOfMonth);
        boolean isSame = campaignFaction.getShortName().equals(faction.getShortName());
        boolean isOutlawed = factionStanding.isOutlawed();

        String addendum = " "; // The whitespace is important to ensure consistent GUI spacing.
        String color = "";

        if (isSame) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.parent");
            color = getAmazingColor();
        } else if (isOutlawed) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.outlawed");
            color = getNegativeColor();
        } else if (isAtWar) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.atWar");
            color = getNegativeColor();
        } else if (isAllied) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.allied");
            color = getPositiveColor();
        } else if (isRival) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.rival");
            color = getWarningColor();
        }

        return String.format("<html><div style='text-align: center;'><h1>%s</h1>" +
                                   "<h2>%s</h2>" +
                                   "<h2>%s%s%s</h2>" +
                                   "<i>%s</i></div></html>",
              factionName,
              factionStandingLabel,
              spanOpeningWithCustomColor(color),
              addendum,
              CLOSING_SPAN_TAG,
              factionStandingDescription);
    }

    /**
     * Creates and configures a {@link JSlider} to visually represent the regard values for a faction.
     *
     * <p>The slider uses integer values, so the provided double regard values are rounded.</p>
     *
     * <p>The minimum and maximum values are determined using {@link FactionStandings#getMinimumRegard()} and
     * {@link FactionStandings#getMaximumSameFactionRegard()}.</p>
     *
     * @param factionCode   the code identifying the faction, used to set the slider's name
     * @param factionRegard the current regard value for the faction; will be rounded to the nearest {@link Integer}
     * @param climateRegard the climate regard value for the faction; will be rounded to the nearest {@link Integer}
     *
     * @return a {@link JSlider} (specifically, a {@link FactionStandingSlider}) configured for the faction, disabled
     *       and styled
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static JSlider getRegardSlider(String factionCode, double factionRegard, double climateRegard) {
        int roundedFactionRegard = (int) round(factionRegard); // JSlider doesn't accept doubles, so we round.
        int roundedClimateRegard = (int) round(climateRegard); // JSlider doesn't accept doubles, so we round.
        int minimumRegard = (int) Math.floor(FactionStandings.getMinimumRegard());
        int maximumRegard = (int) Math.ceil(FactionStandings.getMaximumSameFactionRegard());
        JSlider sldRegard = new FactionStandingSlider(minimumRegard,
              maximumRegard,
              roundedFactionRegard,
              roundedClimateRegard);
        sldRegard.setName("sldFactionRegard" + factionCode);
        sldRegard.setEnabled(false);
        sldRegard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        sldRegard.setAlignmentX(JSlider.CENTER_ALIGNMENT);
        return sldRegard;
    }

    /**
     * Calculates the standing effects description string for a given faction regard value.
     *
     * @param isClan                          {@code true} if the faction is a Clan faction, otherwise {@code false}
     * @param isPirateOrMercenaryOrganization {@code true} if the faction is a pirate or mercenary organization
     * @param factionRegard                   the regard value of the faction
     *
     * @return the standing effects description for the corresponding {@link FactionStandingLevel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getEffectsDescription(boolean isClan, boolean isPirateOrMercenaryOrganization,
          double factionRegard) {
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionRegard);
        return factionStanding.getEffectsDescription(isClan, isPirateOrMercenaryOrganization, campaignOptions);
    }

    /**
     * Creates a mouse adapter that updates the effects panel's text when the mouse enters the associated component.
     *
     * @param replacementText the text to set in the effects panel; if null, an empty string is used
     *
     * @return a {@link MouseAdapter} that updates the effects panel on mouse enter
     *
     * @author Illiani
     * @since 0.50.07
     */
    public MouseAdapter createEffectsPanelUpdater(String replacementText) {
        final String effectsText = replacementText == null ? "" : replacementText;
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                JTextArea effectsArea = findComponentByName(getContentPane(),
                      EFFECTS_PANEL_LABEL_NAME,
                      JTextArea.class);
                if (effectsArea != null) {
                    LOGGER.debug("Updating effects panel with text: {}", effectsText);
                    effectsArea.setText(effectsText);
                }
            }
        };
    }

    /**
     * Recursively searches the given container and its children for a component of the specified type with the given
     * name.
     *
     * @param container the container to search within
     * @param name      the name of the component to find
     * @param type      the class type of the component to find
     * @param <T>       the type parameter extending {@link Component}
     *
     * @return the found component cast to the specified type, or null if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    private <T extends Component> T findComponentByName(Container container, String name, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName())) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findComponentByName(child, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(FactionStandingReport.class);
            this.setName("FactionStandingReport");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
