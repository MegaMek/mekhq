/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.markets.personnelMarket;

import static java.lang.Math.min;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.finances.enums.TransactionType.RECRUITMENT;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.compactMoney;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudFilterList;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.hud.HudWrappingLabel;
import mekhq.gui.dialog.AdvanceDaysDialog;
import mekhq.gui.dialog.glossary.GlossaryConsole;
import mekhq.gui.view.PersonViewPanel;

/**
 * The personnel market, drawn as a heads-up display in the style of the Oracle Console.
 *
 * <p>A command bar holds the market's identity and live tiles for the applicants on offer, this month's recruitment
 * rolls and the command's funds. If recruiting is blocked or reduced here, a banner beneath it says why. The body has
 * three columns: a rail of role filters with a search field, the {@link ApplicantBoard applicant board}, and the
 * highlighted applicant's {@link ApplicantDossier dossier}. The footer holds the Golden Hello offer and the Recruit
 * buttons, which act on every applicant picked on the board. A guide slides over the window, opened by its button or
 * by itself the first time the market is opened.</p>
 *
 * <p>The dialog keeps the market's state - the role filter, the search, the board's order and the selection - and
 * rebuilds the board from it on {@link #refresh}. It is modal: constructing it builds the window and shows it, and the
 * constructor returns once the window has closed.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelMarketDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(PersonnelMarketDialog.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.PersonnelMarket";

    /** How many tips there are, numbered from zero, for the one shown in the footer. */
    private static final int TIP_COUNT = 11;
    private static final int RAIL_WIDTH = 220;
    /** The board's share of the width beside the rail; the dossier has the rest. */
    private static final double BOARD_SHARE = 0.58;
    private static final Dimension MINIMUM_SIZE = scaleForGUI(1120, 640);
    private static final Dimension DEFAULT_SIZE = scaleForGUI(1150, 800);
    private static final Dimension FULL_RECORD_SIZE = scaleForGUI(760, 820);
    private static final String GUIDE_PREFIX = "guide.personnelMarket.";

    /**
     * What the board shows about an applicant, worked out once when the market opens. The experience level and hiring
     * cost are slow to compute, and the board needs them every time a card is drawn or the board is sorted.
     *
     * @param experienceLevel the applicant's experience level, such as {@code SkillType.EXP_VETERAN}
     * @param hiringCost      what the applicant costs to hire
     * @param abilities       how many special abilities they have
     * @param flaws           how many flaws they have
     */
    record ApplicantFacts(int experienceLevel, Money hiringCost, int abilities, int flaws) {}

    private final NewPersonnelMarket market;
    private final JFrame parent;
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final List<Person> currentApplicants;
    private final Map<Person, ApplicantFacts> facts = new HashMap<>();

    private ApplicantFilter filter;
    private ApplicantSort sort = ApplicantSort.EXPERIENCE;
    private boolean isSortReversed;
    /** {@code true} to show only applicants with at least one special ability. */
    private boolean isAbilitiesOnly;
    /** {@code true} to show only applicants with at least one flaw. */
    private boolean isFlawsOnly;
    /** The experience levels to show; empty shows every level. */
    private final Set<Integer> experienceLevels = new TreeSet<>();

    private final JTextField searchField = new JTextField();
    private final HudFilterList<ApplicantFilter> groupList = new HudFilterList<>(this::setFilter);
    private final HudFilterList<ApplicantFilter> professionList = new HudFilterList<>(this::setFilter);
    /** The professions' heading and rows, hidden when every applicant is a civilian. */
    private final JPanel professionRows = Hud.column();
    private final HudCheckBox abilitiesCheckBox = railCheckBox();
    private final HudCheckBox flawsCheckBox = railCheckBox();
    private final JPanel experienceRows = Hud.column();
    private final Map<Integer, HudCheckBox> experienceCheckBoxes = new HashMap<>();
    private final HudCheckBox goldenHelloCheckbox = new HudCheckBox("");
    private final JPanel dossierHolder = new JPanel(new BorderLayout());
    private final JLabel selectionLabel = Hud.hint("");
    private ApplicantBoard board;
    private HudStatTile applicantsTile;
    private HudStatTile fundsTile;
    private HudButton recruitButton;
    private HudButton gmRecruitButton;
    private HudGuideDrawer guideDrawer;
    private HudCheckBox autoOpenGuide;
    private @Nullable Person shownInDossier;
    private boolean isDossierShown;
    /** {@code true} while the filters are being cleared, so clearing the search does not refresh the board early. */
    private boolean isClearingFilters;

    /**
     * Builds the dialog and shows it modally; this returns once the dialog has closed.
     *
     * @param market the personnel market logic backing this dialog
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketDialog(NewPersonnelMarket market) {
        super(market.getCampaign().getGUI().getFrame(), true);
        this.market = market;
        this.campaign = market.getCampaign();
        this.campaignOptions = campaign.getCampaignOptions();
        this.parent = campaign.getGUI().getFrame();
        this.currentApplicants = market.getCurrentApplicants();

        this.filter = ApplicantFilter.fromKey(market.getLastSelectedFilter());

        for (Person applicant : currentApplicants) {
            facts.put(applicant, workOutFacts(applicant));
        }

        initializeComponents();
    }

    // region Layout

    /**
     * Initializes and arranges all GUI components for the dialog, then shows it. Called once, by the constructor.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void initializeComponents() {
        setTitle(dialogTitle());

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        JPanel north = Hud.transparentPanel(new BorderLayout());
        north.add(buildCommandBar(), BorderLayout.NORTH);
        JComponent banner = buildAvailabilityBanner();
        if (banner != null) {
            north.add(banner, BorderLayout.SOUTH);
        }
        content.add(north, BorderLayout.NORTH);

        board = new ApplicantBoard(this);
        dossierHolder.setOpaque(true);
        dossierHolder.setBackground(GROUND);
        dossierHolder.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));

        JPanel columns = Hud.transparentPanel(new GridBagLayout());
        addColumn(columns, board.getComponent(), BOARD_SHARE);
        addColumn(columns, dossierHolder, 1.0 - BOARD_SHARE);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(GROUND);
        body.add(buildRail(), BorderLayout.WEST);
        body.add(columns, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);

        buildGuide();
        installKeyBindings(content);
        refresh(List.of(), 0);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide();
            }

            @Override
            public void windowClosing(WindowEvent event) {
                closeAction();
            }
        });

        setMinimumSize(MINIMUM_SIZE);
        pack();
        setSize(DEFAULT_SIZE); // Default opening size; saved preferences (below) override it on later opens
        setLocationRelativeTo(parent);
        setPreferences(); // Must be before setVisible
        setVisible(true); // Should always be last
    }

    /**
     * Adds a full-height column that takes a share of the width. Its own preferred width is ignored, so the columns
     * never shift as their content changes, such as when another applicant's dossier is shown.
     *
     * @param columns the panel to add the column to, laid out by a {@link GridBagLayout}
     * @param column  the column
     * @param share   the share of the panel's width it takes
     */
    private static void addColumn(JPanel columns, JComponent column, double share) {
        column.setPreferredSize(new Dimension(0, 0));
        column.setMinimumSize(new Dimension(0, 0));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = columns.getComponentCount();
        constraints.gridy = 0;
        constraints.weightx = share;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        columns.add(column, constraints);
    }

    /** The market's identity and live tiles above the guide button, in the style of the Oracle Console. */
    private JPanel buildCommandBar() {
        JPanel bar = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));

        JPanel identity = Hud.column();
        JLabel eyebrow = new JLabel(getTitle().toUpperCase(Locale.ROOT));
        eyebrow.setForeground(TEXT_FAINT);
        eyebrow.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel title = new JLabel(text("header.personnelMarket.title").toUpperCase(Locale.ROOT));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        JLabel subtitle = new JLabel(describeSubtitle());
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        applicantsTile = new HudStatTile(text("tile.personnelMarket.applicants"), "");
        applicantsTile.setToolTipText(Hud.wrapToolTip(text("tile.personnelMarket.applicants.tooltip")));
        HudStatTile rollsTile = buildRollsTile();
        fundsTile = new HudStatTile(text("tile.personnelMarket.funds"), "");
        fundsTile.setToolTipText(Hud.wrapToolTip(text("tile.personnelMarket.funds.tooltip")));
        JPanel tiles = Hud.tileRow(applicantsTile, rollsTile, fundsTile);

        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(tiles, BorderLayout.EAST);

        HudButton guideButton = new HudButton("?  " + text("button.personnelMarket.guide").toUpperCase(Locale.ROOT),
              false, true);
        guideButton.addActionListener(event -> openGuide());
        HudButton documentation = new HudButton(text("button.personnelMarket.documentation").toUpperCase(Locale.ROOT),
              false, true);
        documentation.setToolTipText(text("button.personnelMarket.documentation.tooltip"));
        documentation.addActionListener(event -> GlossaryConsole.showEntry(this, GlossaryEntry.PERSONNEL_MARKET));
        JPanel helpButtons = Hud.transparentPanel(null);
        helpButtons.setLayout(new BoxLayout(helpButtons, BoxLayout.X_AXIS));
        helpButtons.add(documentation);
        helpButtons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        helpButtons.add(guideButton);

        JPanel helpRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(12), 0));
        helpRow.add(Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "header.personnelMarket.rules",
              market.getAssociatedPersonnelMarketStyle().toString())), BorderLayout.CENTER);
        helpRow.add(helpButtons, BorderLayout.EAST);

        bar.add(topRow, BorderLayout.CENTER);
        bar.add(helpRow, BorderLayout.SOUTH);
        return bar;
    }

    /**
     * The tile for this month's recruitment rolls: how many times the market rolled for an applicant, against the most
     * it could have rolled here. It never changes while the market is open, so it is filled in once.
     */
    private HudStatTile buildRollsTile() {
        int maximum = Math.max(1, market.getMaximumRecruitmentRolls());
        int rolls = market.getRecruitmentRolls();

        HudStatTile tile = new HudStatTile(text("tile.personnelMarket.rolls"),
              getFormattedTextAt(RESOURCE_BUNDLE, "tile.personnelMarket.rolls.sub", maximum));
        tile.setValue(Integer.toString(rolls), AMBER);
        tile.setMeter(min(rolls, maximum) / (double) maximum, AMBER, null);
        tile.setToolTipText(Hud.wrapToolTip(text("tile.personnelMarket.rolls.tooltip")));
        return tile;
    }

    /**
     * A banner beneath the command bar saying why recruiting is blocked here (in red) or reduced (in amber), or
     * {@code null} when neither applies.
     */
    private @Nullable JComponent buildAvailabilityBanner() {
        String blocked = market.getAvailabilityMessage();
        if (!blocked.isBlank()) {
            return banner(DANGER, blocked);
        }

        String amberSpan = "<span style='color:" + hex(AMBER) + "'>";
        boolean isUseChaosReputation = campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION);
        if (campaign.getPlayerForce().getReputationRating(isUseChaosReputation)
                  < market.getUnitReputationRecruitmentCutoff()) {
            return banner(AMBER, getFormattedTextAt(RESOURCE_BUNDLE, "hint.personnelMarket.reputation", amberSpan,
                  "</span>"));
        }

        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        if (currentSystem != null && currentSystem.getPopulation(campaign.getLocalDate())
                                           < market.getLowPopulationRecruitmentDivider()) {
            return banner(AMBER, getFormattedTextAt(RESOURCE_BUNDLE, "hint.personnelMarket.population", amberSpan,
                  "</span>"));
        }
        return null;
    }

    private static JComponent banner(Color tone, String html) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setOpaque(true);
        banner.setBackground(SURFACE);
        banner.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(3), 0, 0, tone),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        HudWrappingLabel text = new HudWrappingLabel(html);
        text.setForeground(TEXT);
        text.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        banner.add(text, BorderLayout.CENTER);

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(GROUND);
        holder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), 0, scaleForGUI(14)));
        holder.add(banner, BorderLayout.CENTER);
        return holder;
    }

    /**
     * The role filter rail: a search field above the broad role groups and the professions on the board, each with its
     * count, and the checkboxes that narrow the board further.
     */
    private JPanel buildRail() {
        JPanel rail = new JPanel(new BorderLayout());
        rail.setOpaque(true);
        rail.setBackground(SURFACE_DEEP);
        rail.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        rail.setPreferredSize(new Dimension(scaleForGUI(RAIL_WIDTH), 0));

        Hud.styleField(searchField);
        searchField.putClientProperty("JTextField.placeholderText", text("rail.personnelMarket.search"));
        searchField.setToolTipText(text("rail.personnelMarket.search.tooltip"));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchChanged();
            }
        });
        // Enter or Down moves from the search to the board, so the player can search and pick without the mouse.
        searchField.addActionListener(event -> board.focus());
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "toBoard");
        searchField.getActionMap().put("toBoard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                board.focus();
            }
        });

        JPanel head = Hud.column();
        head.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(8),
              scaleForGUI(12)));
        head.add(leftAligned(searchField));
        head.add(Box.createVerticalStrut(scaleForGUI(14)));
        head.add(leftAligned(Hud.eyebrow(text("rail.personnelMarket.roles"))));
        rail.add(head, BorderLayout.NORTH);

        // Below the roles, checkboxes narrow the board further: to applicants with abilities or flaws, and to chosen
        // experience levels.
        JPanel rows = Hud.column();
        rows.add(leftAligned(groupList));
        professionRows.add(Box.createVerticalStrut(scaleForGUI(14)));
        professionRows.add(leftAligned(railHeading(text("rail.personnelMarket.professions"))));
        professionRows.add(leftAligned(professionList));
        rows.add(leftAligned(professionRows));
        rows.add(Box.createVerticalStrut(scaleForGUI(14)));
        rows.add(leftAligned(railHeading(text("rail.personnelMarket.traits"))));
        abilitiesCheckBox.addActionListener(event -> {
            isAbilitiesOnly = abilitiesCheckBox.isSelected();
            refreshKeepingSelection();
        });
        rows.add(leftAligned(abilitiesCheckBox));
        flawsCheckBox.addActionListener(event -> {
            isFlawsOnly = flawsCheckBox.isSelected();
            refreshKeepingSelection();
        });
        rows.add(leftAligned(flawsCheckBox));
        rows.add(Box.createVerticalStrut(scaleForGUI(14)));
        rows.add(leftAligned(railHeading(text("rail.personnelMarket.experience"))));
        rows.add(leftAligned(experienceRows));
        rows.add(Box.createVerticalStrut(scaleForGUI(10)));
        JScrollPane scroll = Hud.scroll(rows, SURFACE_DEEP);
        rail.add(scroll, BorderLayout.CENTER);

        JPanel foot = Hud.transparentPanel(new BorderLayout());
        foot.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(10),
              scaleForGUI(12)));
        foot.add(Hud.link(text("rail.personnelMarket.clear"), this::clearFilters), BorderLayout.WEST);
        rail.add(foot, BorderLayout.SOUTH);
        return rail;
    }

    /** A caption for a group of rail filters, inset to line up with the rows beneath it. */
    private static JComponent railHeading(String text) {
        JLabel heading = Hud.eyebrow(text);
        heading.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(12), scaleForGUI(4), scaleForGUI(12)));
        return heading;
    }

    /** A checkbox row in the rail; {@link #label} gives it its text. */
    private static HudCheckBox railCheckBox() {
        HudCheckBox checkBox = new HudCheckBox("");
        checkBox.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(12), scaleForGUI(3),
              scaleForGUI(12)));
        return checkBox;
    }

    /** Labels a checkbox row in the rail with its filter's name and count. */
    private static void label(HudCheckBox checkBox, String name, int count) {
        checkBox.setText(getFormattedTextAt(RESOURCE_BUNDLE, "rail.personnelMarket.count", name, count));
    }

    /** The Golden Hello offer and a tip above the Recruit buttons. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), scaleForGUI(8)));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(16), scaleForGUI(10), scaleForGUI(16))));

        // Only the MekHQ market offers a Golden Hello, so the option is only shown there.
        JPanel optionsRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(16), 0));
        if (market.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            goldenHelloCheckbox.setText(text("checkbox.personnelMarket.goldenHello"));
            goldenHelloCheckbox.setSelected(market.isOfferingGoldenHello());
            goldenHelloCheckbox.setToolTipText(Hud.wrapToolTip(
                  text("checkbox.personnelMarket.goldenHello.tooltip")));
            JPanel options = Hud.transparentPanel(null);
            options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
            options.add(keyLabel(text("footer.personnelMarket.options").toUpperCase(Locale.ROOT)));
            options.add(Box.createHorizontalStrut(scaleForGUI(12)));
            options.add(goldenHelloCheckbox);
            optionsRow.add(options, BorderLayout.WEST);
        }
        HudWrappingLabel tip = new HudWrappingLabel("<html><span style='color:" + hex(TEXT_FAINT) + "'>"
                                                          + Hud.escape(text("footer.personnelMarket.tip")
                                                                                            .toUpperCase(Locale.ROOT))
                                                          + "</span>&nbsp;&nbsp;"
                                                          + Hud.escape(tipText()) + "</html>");
        tip.setForeground(TEXT_MUTED);
        tip.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        optionsRow.add(tip, BorderLayout.CENTER);

        HudButton advanceDays = new HudButton(text("button.personnelMarket.advanceDays").toUpperCase(Locale.ROOT),
              false);
        advanceDays.setToolTipText(Hud.wrapToolTip(
              text("button.personnelMarket.advanceDays.tooltip")));
        advanceDays.addActionListener(event -> advanceDays());
        gmRecruitButton = new HudButton(text("button.personnelMarket.hire.gm").toUpperCase(Locale.ROOT), false);
        gmRecruitButton.setToolTipText(Hud.wrapToolTip(text(campaign.isGM()
                                                                                 ? "button.personnelMarket.hire.gm.tooltip"
                                                                                 : "button.personnelMarket.gmOnly.tooltip")));
        gmRecruitButton.addActionListener(event -> {
            if (campaign.isGM() && !board.selected().isEmpty()) {
                hireActionListener(true);
            }
        });
        recruitButton = new HudButton(text("button.personnelMarket.hire.normal").toUpperCase(Locale.ROOT), true);
        recruitButton.addActionListener(event -> {
            if (!board.selected().isEmpty()) {
                hireActionListener(false);
            }
        });
        HudButton close = new HudButton(text("button.personnelMarket.close").toUpperCase(Locale.ROOT), false);
        close.addActionListener(event -> closeAction());

        JPanel actions = Hud.transparentPanel(null);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(advanceDays);
        actions.add(Box.createHorizontalStrut(scaleForGUI(16)));
        actions.add(selectionLabel);
        actions.add(Box.createHorizontalStrut(scaleForGUI(10)));
        actions.add(gmRecruitButton);
        actions.add(Box.createHorizontalStrut(scaleForGUI(8)));
        actions.add(recruitButton);
        actions.add(Box.createHorizontalStrut(scaleForGUI(8)));
        actions.add(close);
        JPanel actionsRow = Hud.transparentPanel(new BorderLayout());
        actionsRow.add(actions, BorderLayout.EAST);

        footer.add(optionsRow, BorderLayout.NORTH);
        footer.add(actionsRow, BorderLayout.SOUTH);
        return footer;
    }

    private void installKeyBindings(JComponent root) {
        // Ctrl+F (Cmd+F on a Mac) jumps to the search, as it does in most applications.
        KeyStroke find = KeyStroke.getKeyStroke(KeyEvent.VK_F,
              Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(find, "search");
        root.getActionMap().put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!guideDrawer.isVisible()) {
                    searchField.requestFocusInWindow();
                    searchField.selectAll();
                }
            }
        });
    }

    // endregion Layout

    // region State and refresh

    /**
     * Brings every part of the window up to date: the filter rail and its counts, the board, the tiles and the footer.
     * Used when the applicants or the filters' state change; see {@link #refreshBoard} for everything else.
     *
     * @param keep          the applicants to keep selected, if still shown
     * @param fallbackIndex the board position to select if none of {@code keep} is shown
     */
    private void refresh(Collection<Person> keep, int fallbackIndex) {
        refreshFilters();
        refreshBoard(keep, fallbackIndex);
    }

    /**
     * Brings the board, the rail's counts, the tiles and the footer up to date after the player filters, searches or
     * sorts. The rail's controls are left in place, keeping the keyboard focus on the one the player just used.
     *
     * @param keep          the applicants to keep selected, if still shown
     * @param fallbackIndex the board position to select if none of {@code keep} is shown
     */
    private void refreshBoard(Collection<Person> keep, int fallbackIndex) {
        groupList.setSelected(filter);
        professionList.setSelected(filter);

        ApplicantMatches matches = matches(currentApplicants);
        refreshCounts(matches);

        List<Person> shown = matches.shown();
        shown.sort(sort.comparator(this::facts, campaign.getLocalDate(), isSortReversed));

        board.show(shown, currentApplicants.size(), keep, fallbackIndex, sort, isSortReversed, emptyBoardText());

        int rare = 0;
        for (Person applicant : currentApplicants) {
            if (isRare(applicant)) {
                rare++;
            }
        }
        applicantsTile.setValue(Integer.toString(currentApplicants.size()), ACCENT_BRIGHT);
        applicantsTile.setSub(rare == 0 ? text("tile.personnelMarket.applicants.noneRare")
                                    : getFormattedTextAt(RESOURCE_BUNDLE, "tile.personnelMarket.applicants.sub", rare));

        selectionChanged();
    }

    private void refreshKeepingSelection() {
        refreshBoard(board.selected(), board.firstSelectedIndex());
    }

    /**
     * @param applicants the applicants to filter
     *
     * @return what the filters make of them: the role, the search, the ability and flaw checkboxes, and the chosen
     *       experience levels
     */
    private ApplicantMatches matches(List<Person> applicants) {
        String query = searchField.getText().strip().toLowerCase(Locale.ROOT);
        List<Person> found = new ArrayList<>();
        for (Person applicant : applicants) {
            if (query.isEmpty() || searchText(applicant).contains(query)) {
                found.add(applicant);
            }
        }
        return new ApplicantMatches(found, this::facts, filter, isAbilitiesOnly, isFlawsOnly, experienceLevels);
    }

    /** Shows beside each filter how many applicants it would find with the player's other choices as they are. */
    private void refreshCounts(ApplicantMatches matches) {
        groupList.setCounts(matches::countOf);
        professionList.setCounts(matches::countOf);
        label(abilitiesCheckBox, text("rail.personnelMarket.abilities"), matches.withAbilities());
        label(flawsCheckBox, text("rail.personnelMarket.flaws"), matches.withFlaws());
        for (Map.Entry<Integer, HudCheckBox> entry : experienceCheckBoxes.entrySet()) {
            int level = entry.getKey();
            label(entry.getValue(), SkillType.getExperienceLevelName(level), matches.withLevel(level));
        }
    }

    /**
     * Refills the rail's filters: the roles, the ability and flaw checkboxes, then one checkbox per experience level
     * among the applicants. Every filter some applicant matches keeps its row whatever else is chosen, so the rows never
     * jump about as the player filters; {@link #refreshCounts} fills in their counts.
     */
    private void refreshFilters() {
        refreshRoleFilters();

        abilitiesCheckBox.setSelected(isAbilitiesOnly);
        flawsCheckBox.setSelected(isFlawsOnly);

        // Highest level first, as the board's experience order runs. A ticked level stays on the rail even when no
        // applicant has it, so the player can see what is narrowing the board.
        Set<Integer> levels = new TreeSet<>(Comparator.reverseOrder());
        levels.addAll(experienceLevels);
        for (Person applicant : currentApplicants) {
            levels.add(facts(applicant).experienceLevel());
        }
        experienceRows.removeAll();
        experienceCheckBoxes.clear();
        for (int level : levels) {
            HudCheckBox checkBox = railCheckBox();
            checkBox.setSelected(experienceLevels.contains(level));
            checkBox.addActionListener(event -> {
                if (checkBox.isSelected()) {
                    experienceLevels.add(level);
                } else {
                    experienceLevels.remove(level);
                }
                refreshKeepingSelection();
            });
            experienceCheckBoxes.put(level, checkBox);
            experienceRows.add(leftAligned(checkBox));
        }
        experienceRows.revalidate();
        experienceRows.repaint();
    }

    /**
     * Refills the role lists: the broad groups, which are always there, then the professions the applicants have, which
     * follow the applicants as they are recruited and added.
     */
    private void refreshRoleFilters() {
        List<PersonnelRole> professions = new ArrayList<>();
        for (Person applicant : currentApplicants) {
            professions.add(applicant.getPrimaryRole());
        }
        List<ApplicantFilter> professionFilters = ApplicantFilter.professions(professions, filter);
        groupList.setItems(filterItems(ApplicantFilter.groups()));
        groupList.setSelected(filter);
        professionList.setItems(filterItems(professionFilters));
        professionList.setSelected(filter);
        professionRows.setVisible(!professionFilters.isEmpty());
    }

    /** One row per filter, without its count until {@link #refreshCounts} fills it in. */
    private static List<HudFilterList.Item<ApplicantFilter>> filterItems(List<ApplicantFilter> roleFilters) {
        List<HudFilterList.Item<ApplicantFilter>> items = new ArrayList<>();
        for (ApplicantFilter roleFilter : roleFilters) {
            items.add(new HudFilterList.Item<>(roleFilter, roleFilter.label(), -1));
        }
        return items;
    }

    /**
     * Brings the dossier, the funds tile and the footer up to date with the board's selection. The board calls this
     * whenever the player changes the selection.
     */
    void selectionChanged() {
        List<Person> selected = board.selected();
        Person lead = board.lead();
        if (!isDossierShown || lead != shownInDossier) {
            showDossier(lead);
        }

        Money total = Money.zero();
        for (Person person : selected) {
            total = total.plus(facts(person).hiringCost());
        }
        Money balance = balance();
        boolean isUnaffordable = balance.isLessThan(total);

        fundsTile.setValue(compactMoney(balance), balance.isNegative() ? DANGER : TEXT);
        fundsTile.setSub(selected.isEmpty() ? text("tile.personnelMarket.funds.none")
                               : getFormattedTextAt(RESOURCE_BUNDLE, "tile.personnelMarket.funds.sub",
                                     compactMoney(total)));

        if (selected.isEmpty()) {
            selectionLabel.setText(text("footer.personnelMarket.selected.none"));
            selectionLabel.setForeground(TEXT_MUTED);
            recruitButton.setText(text("button.personnelMarket.hire.normal").toUpperCase(Locale.ROOT));
        } else {
            selectionLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "footer.personnelMarket.selected",
                  selected.size(), total.toAmountAndSymbolString()));
            selectionLabel.setForeground(isUnaffordable ? DANGER : TEXT_MUTED);
            recruitButton.setText((selected.size() == 1
                                         ? getFormattedTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.one",
                                               selected.getFirst().getFirstName())
                                         : getFormattedTextAt(RESOURCE_BUNDLE, "button.personnelMarket.hire.many",
                                               selected.size())).toUpperCase(Locale.ROOT));
        }
        recruitButton.setArmed(!selected.isEmpty());
        recruitButton.setToolTipText(isUnaffordable
                                           ? Hud.wrapToolTip(getFormattedTextAt(RESOURCE_BUNDLE,
                                                 "button.personnelMarket.hire.unaffordable",
                                                 total.toAmountAndSymbolString(),
                                                 balance.toAmountAndSymbolString()))
                                           : null);
        gmRecruitButton.setArmed(campaign.isGM() && !selected.isEmpty());
    }

    private void showDossier(@Nullable Person person) {
        shownInDossier = person;
        isDossierShown = true;
        dossierHolder.removeAll();
        if (person != null) {
            dossierHolder.add(ApplicantDossier.build(this, person), BorderLayout.CENTER);
        } else if (currentApplicants.isEmpty()) {
            dossierHolder.add(ApplicantDossier.buildEmptyState("dossier.personnelMarket.empty.title",
                  "dossier.personnelMarket.empty.body"), BorderLayout.NORTH);
        } else {
            dossierHolder.add(ApplicantDossier.buildEmptyState("dossier.personnelMarket.noSelection.title",
                  "dossier.personnelMarket.noSelection.body"), BorderLayout.NORTH);
        }
        dossierHolder.revalidate();
        dossierHolder.repaint();
    }

    private String emptyBoardText() {
        if (currentApplicants.isEmpty()) {
            return text("board.personnelMarket.empty");
        }
        return text("board.personnelMarket.noMatch");
    }

    /** What the search matches: the applicant's name, professions and origin. */
    private String searchText(Person person) {
        StringBuilder text = new StringBuilder(person.getFullTitle()).append(' ').append(ApplicantDossier.roles(person));
        Faction origin = person.getOriginFaction();
        if (origin != null) {
            text.append(' ').append(origin.getFullName(campaign.getLocalDate().getYear()));
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    private void setFilter(ApplicantFilter newFilter) {
        filter = newFilter;
        market.setLastSelectedFilter(newFilter.key());
        refreshKeepingSelection();
    }

    /**
     * Sorts the board. Choosing the order already in use reverses it, as clicking a column heading would.
     *
     * @param newSort the order the player chose
     */
    void setSort(ApplicantSort newSort) {
        if (newSort == sort) {
            isSortReversed = !isSortReversed;
        } else {
            sort = newSort;
            isSortReversed = false;
        }
        refreshKeepingSelection();
    }

    /** The search changed: refresh the board, unless the filters are being cleared and will refresh it once. */
    private void searchChanged() {
        if (!isClearingFilters) {
            refreshKeepingSelection();
        }
    }

    /** Clears every filter and the search, then rebuilds the rail so its checkboxes show as cleared. */
    private void clearFilters() {
        resetFilters();
        refresh(board.selected(), board.firstSelectedIndex());
    }

    /** Clears every filter and the search without refreshing anything; the caller refreshes once afterwards. */
    private void resetFilters() {
        filter = ApplicantFilter.ALL;
        isAbilitiesOnly = false;
        isFlawsOnly = false;
        experienceLevels.clear();
        market.setLastSelectedFilter(filter.key());
        isClearingFilters = true;
        try {
            searchField.setText("");
        } finally {
            isClearingFilters = false;
        }
    }

    private ApplicantFacts workOutFacts(Person person) {
        int experienceLevel = person.getExperienceLevel(campaignOptions, campaign.getPlayerForce().isClanForce(),
              campaign.getLocalDate(), false, true);
        // Counted from the dossier's list, so the card's counts always match the dossier's badges.
        List<ApplicantDossier.Ability> traits = ApplicantDossier.abilities(person);
        int flaws = 0;
        for (ApplicantDossier.Ability trait : traits) {
            if (trait.isFlaw()) {
                flaws++;
            }
        }
        int abilities = traits.size() - flaws;
        return new ApplicantFacts(experienceLevel, market.getHiringCost(person), abilities, flaws);
    }

    // endregion State and refresh

    // region Accessors for the board and dossier

    Campaign campaign() {
        return campaign;
    }

    NewPersonnelMarket market() {
        return market;
    }

    /** @return what the board shows about an applicant, worked out the first time it is asked for */
    ApplicantFacts facts(Person person) {
        return facts.computeIfAbsent(person, this::workOutFacts);
    }

    /** @return {@code true} if the applicant has a rare profession */
    boolean isRare(Person person) {
        return market.getRarePersonnel().contains(person.getId());
    }

    /** @return the command's current funds */
    Money balance() {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    boolean isGM() {
        return campaign.isGM();
    }

    /**
     * @return {@code true} if applicants carry a reputation of their own: Chaos reputation is in use and is not tracked
     *       for the campaign as a whole
     */
    boolean isReputationShown() {
        return campaignOptions.get(CampaignOption.USE_CHAOS_REPUTATION)
                     && !campaignOptions.get(CampaignOption.CAMPAIGN_LEVEL_CHAOS_REPUTATION);
    }

    /**
     * Opens an applicant's full personnel record, the same view the Personnel tab shows, in a window over the market.
     *
     * @param person the applicant
     */
    void showFullRecord(Person person) {
        JDialog record = new JDialog(this, person.getFullTitle(), true);
        PersonViewPanel view = new PersonViewPanel(person, campaign, campaign.getGUI());
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        record.setContentPane(scroll);
        record.setSize(FULL_RECORD_SIZE);
        record.setLocationRelativeTo(this);
        record.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        record.getRootPane().registerKeyboardAction(event -> record.dispose(),
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        record.setVisible(true);
    }

    // endregion Accessors for the board and dossier

    // region Actions

    /**
     * Applies the Golden Hello choice and the remaining applicants to the {@link NewPersonnelMarket}, then closes the
     * dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void closeAction() {
        if (market.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            market.setOfferingGoldenHello(goldenHelloCheckbox.isSelected());
        }
        market.setCurrentApplicants(currentApplicants);
        dispose();
    }

    /**
     * Recruits every applicant picked on the board.
     *
     * <p>A normal hire charges each applicant's hiring cost, and only goes ahead if the command can pay for everyone
     * picked, so the player is never left with part of a group they chose. A GM hire is free.</p>
     *
     * @param isGMHire whether the hire action is performed as a GM Hire
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void hireActionListener(boolean isGMHire) {
        List<Person> selectedApplicants = new ArrayList<>(board.selected());
        if (selectedApplicants.isEmpty()) {
            return;
        }
        int position = board.firstSelectedIndex();

        Finances finances = campaign.getPlayerForce().getFinances();
        if (!isGMHire) {
            Money totalCost = Money.zero();
            for (Person applicant : selectedApplicants) {
                totalCost = totalCost.plus(market.getHiringCost(applicant));
            }

            Money balance = finances.getBalance();
            if (balance.isLessThan(totalCost)) {
                JOptionPane.showMessageDialog(this,
                      getFormattedTextAt(RESOURCE_BUNDLE, "hire.personnelMarket.insufficientFunds.text",
                            totalCost.toAmountAndSymbolString(), balance.toAmountAndSymbolString()),
                      text("hire.personnelMarket.insufficientFunds.title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        List<Person> recruitedPersons = new ArrayList<>();
        for (Person applicant : selectedApplicants) {
            Money cost = isGMHire ? Money.zero() : market.getHiringCost(applicant);
            // A free hire (no Pay for Recruitment, no Golden Hello) leaves no 0 C-bill line in the ledger
            if (!cost.isZero()) {
                boolean isPaid = finances.debit(RECRUITMENT,
                      campaign.getLocalDate(),
                      cost,
                      getFormattedTextAt(RESOURCE_BUNDLE, "finances.personnelMarket.hire", applicant.getFullTitle()));

                // The balance was checked above, so this only happens if something else spent the money. Never
                // recruit someone whose hiring cost wasn't paid.
                if (!isPaid) {
                    LOGGER.warn("Could not pay the hiring cost of {} for {}; stopping recruitment",
                          cost.toAmountAndSymbolString(), applicant.getFullTitle());
                    break;
                }
            }
            // The market has already charged its hiring cost, so recruitment isn't charged again
            campaign.getPlayerForce().getHumanResources().recruitPrepaidPerson(campaign, applicant, isGMHire);
            market.removeRarePerson(applicant);
            recruitedPersons.add(applicant);
        }

        // Remove the recruits from the board, and move the selection on to whoever now stands where they did
        currentApplicants.removeAll(recruitedPersons);
        for (Person recruit : recruitedPersons) {
            facts.remove(recruit);
            board.forget(recruit);
        }
        refresh(List.of(), position);
    }

    /**
     * Handles the process of adding a fresh applicant to the applicant pool. A GM tool.
     *
     * <p>This method attempts to create a single applicant for the personnel market. If no applicant is available,
     * it reports an error message to the campaign log. Otherwise, the applicant is added to the board and selected; if
     * the filters would hide them, the filters are cleared so the new applicant is in view.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    void addApplicant() {
        Person applicant = market.getSingleApplicant();
        if (applicant == null) {
            campaign.addReport(GENERAL, text("button.personnelMarket.add.gm.error"));
            return;
        }

        LOGGER.info(applicant.getFullTitle());
        currentApplicants.add(applicant);
        facts.put(applicant, workOutFacts(applicant));

        if (matches(List.of(applicant)).shown().isEmpty()) {
            // Clear the filters so the new applicant is in view; the refresh below then selects them.
            resetFilters();
        }
        refresh(List.of(applicant), 0);
    }

    /**
     * Closes the market and opens the Advance Days dialog; once the days have passed, the market opens again with
     * whatever applicants the new days brought.
     */
    private void advanceDays() {
        closeAction();
        AdvanceDaysDialog advanceDaysDialog = campaign.getGUI().openAdvanceDaysDialog();
        advanceDaysDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                new PersonnelMarketDialog(market); // Open a new instance (to ensure the market is refreshed)
            }
        });
    }

    // endregion Actions

    // region Guide

    private void buildGuide() {
        autoOpenGuide = new HudCheckBox(text(GUIDE_PREFIX + "autoOpen"));
        autoOpenGuide.setSelected(PersonnelMarketGuidePreferences.getInstance().isGuideAutoOpen());
        autoOpenGuide.addActionListener(event -> PersonnelMarketGuidePreferences.getInstance()
                                                       .setGuideAutoOpen(autoOpenGuide.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(text(GUIDE_PREFIX + "tab.steps"),
              text(GUIDE_PREFIX + "tab.example"), text(GUIDE_PREFIX + "tab.terms")), autoOpenGuide,
              text(GUIDE_PREFIX + "close"), this::closeGuide);
        // The guide drawer and its scrim float over the window; Escape closes the guide.
        guideDrawer.install(getRootPane());
    }

    private void openGuide() {
        PersonnelMarketGuidePreferences.getInstance().setGuideSeen(true);
        autoOpenGuide.setSelected(PersonnelMarketGuidePreferences.getInstance().isGuideAutoOpen());
        guideDrawer.open(guide());
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    /** Opens the guide by itself the first time the player opens the market. */
    private void maybeOpenGuide() {
        if (PersonnelMarketGuidePreferences.getInstance().shouldAutoOpen()) {
            openGuide();
        }
    }

    /**
     * Builds the guide from the resource bundle. Its steps, example lines and terms are numbered keys
     * ({@code guide.personnelMarket.step.1.title}, {@code .step.2.title}, ...), read until the next number is missing,
     * so text can be added or reworded without code changes.
     */
    private static Guide guide() {
        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText("step." + number + ".title")); number++) {
            steps.add(new Step(guideText("step." + number + ".title"), guideText("step." + number + ".text")));
        }
        List<ExampleLine> example = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText("example." + number + ".who")); number++) {
            example.add(new ExampleLine(guideText("example." + number + ".who"),
                  guideText("example." + number + ".text")));
        }
        List<Term> terms = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(guideText("term." + number + ".name")); number++) {
            terms.add(new Term(guideText("term." + number + ".name"), guideText("term." + number + ".text")));
        }
        return new Guide(guideText("eyebrow"), guideText("title"), steps, null, guideText("exampleIntro"), example,
              terms, null);
    }

    private static String guideText(String key) {
        return getTextAt(RESOURCE_BUNDLE, GUIDE_PREFIX + key);
    }

    // endregion Guide

    // region Text

    /**
     * The window's title, which the command bar also shows as its eyebrow: the recruiting service the campaign's
     * faction would use.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private String dialogTitle() {
        Faction campaignFaction = campaign.getPlayerForce().getFaction();
        if (campaignFaction.isClan()) {
            return text("title.personnelMarket.clan");
        } else if (campaignFaction.isComStarOrWoB()) {
            Person commander = campaign.getPlayerForce().getHumanResources()
                                     .getCommander(campaignOptions,
                                           campaign.getPlayerForce().isClanForce(),
                                           campaign.getLocalDate());
            String address = commander != null ? commander.getTitleAndSurname() : campaign.getCommanderAddress(false);
            return getFormattedTextAt(RESOURCE_BUNDLE, "title.personnelMarket.comStarOrWoB",
                  address.toUpperCase(Locale.ROOT));
        } else if (campaignFaction.isMercenary()) {
            return text("title.personnelMarket.mercenary");
        }
        return text("title.personnelMarket.normal");
    }

    /** The date, the command's name and where it is, beneath the market's title. */
    private String describeSubtitle() {
        String date = MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate());
        String force = campaign.getPlayerForce().getName();
        PlanetarySystem system = campaign.getCurrentSystem();
        return system == null
                     ? getFormattedTextAt(RESOURCE_BUNDLE, "header.personnelMarket.subtitle.noSystem", date, force)
                     : getFormattedTextAt(RESOURCE_BUNDLE, "header.personnelMarket.subtitle", date, force,
                           system.getName(campaign.getLocalDate()));
    }

    /** @return a tip for the footer: one of several at random in the MekHQ market, the general tip otherwise */
    private String tipText() {
        int tip = market.getAssociatedPersonnelMarketStyle() == MEKHQ ? randomInt(TIP_COUNT) : 0;
        return text("hint.personnelMarket." + tip);
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    // endregion Text

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(PersonnelMarketDialog.class);
            setName("PersonnelMarketDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
