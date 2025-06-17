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
package mekhq.gui.dialog.reportDialogs.FactionStanding;

import static java.lang.Math.round;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.dialog.reportDialogs.FactionStanding.manualMissionDialogs.SimulateMissionDialog.handleFactionRegardUpdates;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.Border;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.glossary.NewGlossaryEntryDialog;
import mekhq.gui.dialog.reportDialogs.FactionStanding.gmToolsDialog.GMTools;
import mekhq.gui.dialog.reportDialogs.FactionStanding.manualMissionDialogs.SimulateMissionDialog;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
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
    private final LocalDate today;
    private final int gameYear;
    private final FactionStandings factionStandings;
    private final Factions factions;
    private final boolean isGM;
    private final Faction campaignFaction;
    private final ImageIcon campaignIcon;
    private final List<Mission> missions;
    private final boolean isFactionStandingEnabled;

    private final List<String> innerSphereFactions = new ArrayList<>();
    private final List<String> clanFactions = new ArrayList<>();
    private final List<String> peripheryFactions = new ArrayList<>();
    private final List<String> deadFactions = new ArrayList<>();

    private final List<String> reports = new ArrayList<>();

    /**
     * Constructs a {@link FactionStandingReport} which generates a {@link JDialog} displaying faction standings for the
     * specified campaign and related data.
     *
     * @param frame The parent {@link JFrame} that acts as the owner of this report dialog.
     * @param factionStandings The object containing the standings of factions in the campaign.
     * @param today The current date for reference in the report.
     * @param isGM A boolean indicating whether the user is a Game Master (GM).
     * @param campaignFaction The primary faction for the campaign associated with the report.
     * @param campaignIcon An {@link ImageIcon} for the campaign (either a custom user icon or faction icon).
     * @param isFactionStandingEnabled {@code true} if the tacking of Faction Standing is enabled in campaign options; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingReport(final JFrame frame, final FactionStandings factionStandings, final LocalDate today,
                                 final boolean isGM, final Faction campaignFaction, final ImageIcon campaignIcon,
                                 final Collection<Mission> missions, final boolean isFactionStandingEnabled) {
        this.frame = frame;
        this.today = today;
        this.gameYear = today.getYear();
        this.isGM = isGM;
        this.campaignFaction = campaignFaction;
        this.campaignIcon = campaignIcon;
        this.factionStandings = factionStandings;
        factions = Factions.getInstance();
        this.missions = new ArrayList<>(missions);
        this.isFactionStandingEnabled = isFactionStandingEnabled;

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

            if (!faction.validIn(gameYear)) {
                deadFactions.add(factionCode);
            } else if (faction.isClan()) {
                clanFactions.add(factionCode);
            } else if (faction.isPeriphery()) {
                peripheryFactions.add(factionCode);
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
        String clanTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.clan");
        String peripheryTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.periphery");
        String deadTabTitle = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.tab.dead");

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setName("tabbedPane");
        tabbedPane.addTab(innerSphereTabTitle, createReportPanelForFactionGroup(innerSphereFactions));
        tabbedPane.addTab(clanTabTitle, createReportPanelForFactionGroup(clanFactions));
        tabbedPane.addTab(peripheryTabTitle, createReportPanelForFactionGroup(peripheryFactions));
        tabbedPane.addTab(deadTabTitle, createReportPanelForFactionGroup(deadFactions));
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
        JScrollPane groupScrollPane = new JScrollPaneWithSpeed(groupPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        groupScrollPane.setName("factionReportGroupScrollPane" + factions);
        groupScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());

        pnlFactionReport.add(groupScrollPane);

        return pnlFactionReport;
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
        lblStandingEffects.setMinimumSize(new Dimension(Integer.MAX_VALUE, FACTION_EFFECTS_MINIMUM_HEIGHT));
        lblStandingEffects.setPreferredSize(new Dimension(Integer.MAX_VALUE, FACTION_EFFECTS_MINIMUM_HEIGHT));

        pnlEffects.add(lblStandingEffects, BorderLayout.CENTER);
        pnlEffects.setMinimumSize(new Dimension(Integer.MAX_VALUE, FACTION_EFFECTS_MINIMUM_HEIGHT));
        pnlEffects.setPreferredSize(new Dimension(Integer.MAX_VALUE, FACTION_EFFECTS_MINIMUM_HEIGHT));

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
            GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName("FACTION_STANDING");

            if (glossaryEntry == null) {
                LOGGER.warn("Glossary entry not found: {}", "FACTION_STANDING");
                return;
            }

            new NewGlossaryEntryDialog(this, glossaryEntry);
        });
        btnDocumentation.setFocusable(false);
        pnlButtons.add(btnDocumentation);

        pnlButtons.add(Box.createHorizontalStrut(REPORT_BUTTON_SPACE_WIDTH));

        RoundedJButton btnGmTools = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "factionStandingReport.button.gmTools"));
        btnGmTools.setName("btnSimulateContract");
        btnGmTools.setFocusable(false);
        btnGmTools.setEnabled(isFactionStandingEnabled && isGM);
        btnGmTools.addActionListener(e -> {
            setVisible(false);
            GMTools gmTools = new GMTools(this, campaignIcon, campaignFaction, today, factionStandings, missions);
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
        SimulateMissionDialog dialog = new SimulateMissionDialog(frame, campaignIcon, campaignFaction, today);

        Faction employerChoice = dialog.getEmployerChoice();
        Faction enemyChoice = dialog.getEnemyChoice();
        MissionStatus statusChoice = dialog.getStatusChoice();

        reports.addAll(handleFactionRegardUpdates(employerChoice, enemyChoice, statusChoice, today, factionStandings));
    }

    /**
     * Constructs a panel describing the specified faction, including logo, description, and regard slider.
     *
     * @param factionCode the code of the faction to be displayed
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
        JPanel pnlFactionStanding = new JPanel();
        pnlFactionStanding.setName("pnlFactionStanding" + factionCode);
        pnlFactionStanding.setLayout(new BoxLayout(pnlFactionStanding, BoxLayout.Y_AXIS));
        pnlFactionStanding.setBorder(createStandingColoredRoundedTitledBorder(factionStanding.getStandingLevel()));
        pnlFactionStanding.setPreferredSize(new Dimension(FACTION_PANEL_WIDTH, FACTION_PANEL_HEIGHT));
        pnlFactionStanding.setMaximumSize(new Dimension(FACTION_PANEL_WIDTH, FACTION_PANEL_HEIGHT));
        pnlFactionStanding.addMouseListener(createEffectsPanelUpdater(getEffectsDescription(climateRegard)));

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
     * Creates a Compound Border consisting of a {@code RoundedLineBorder} colored according to the specified
     * faction standing level, combined with internal padding.
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
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            title.append("\u2605 ");
        }

        return BorderFactory.createTitledBorder(compound, title.toString());
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
     * @return a HTML string for displaying faction details, standing, and description
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getDescriptionForFaction(Faction faction, double factionRegard) {
        String factionName = faction.getFullName(gameYear);
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionRegard);
        String factionStandingLabel = factionStanding.getLabel(faction);
        String factionStandingDescription = factionStanding.getDescription(faction);

        FactionHints factionHints = FactionHints.defaultFactionHints();
        LocalDate firstOfMonth = today.withDayOfMonth(1); // Climate states update on the 1st in Faction Standing
        boolean isAtWar = factionHints.isAtWarWith(campaignFaction, faction, firstOfMonth);
        boolean isAllied = factionHints.isAlliedWith(campaignFaction, faction, firstOfMonth);
        boolean isRival = factionHints.isRivalOf(campaignFaction, faction, firstOfMonth);
        boolean isSame = campaignFaction.getShortName().equals(faction.getShortName());

        String addendum = " "; // The whitespace is important to ensure consistent GUI spacing.
        String color = "";

        if (isSame) {
            addendum = getTextAt(RESOURCE_BUNDLE, "factionStandingReport.addendum.parent");
            color = getAmazingColor();
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
     * {@link FactionStandings#getMaximumRegard()}.</p>
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
        int maximumRegard = (int) Math.ceil(FactionStandings.getMaximumRegard());
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
     * @param factionRegard the regard value of the faction
     * @return the standing effects description for the corresponding {@link FactionStandingLevel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getEffectsDescription(double factionRegard) {
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(factionRegard);
        return factionStanding.getEffectsDescription();
    }

    /**
     * Creates a mouse adapter that updates the effects panel's text when the mouse enters the associated 
     * component.
     *
     * @param replacementText the text to set in the effects panel; if null, an empty string is used
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
     * @param name the name of the component to find
     * @param type the class type of the component to find
     * @param <T> the type parameter extending {@link Component}
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
