/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.camOpsSalvage;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.salvage.AbstractSalvage;
import mekhq.campaign.mission.scenarios.salvage.CamOpsSalvageUtilities;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TeamAvailability;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TeamOption;
import mekhq.campaign.mission.scenarios.salvage.SalvageOperationDraft.TechOrigin;
import mekhq.campaign.mission.scenarios.salvage.SalvageTechCandidate;
import mekhq.campaign.mission.scenarios.salvage.SalvageTechFilter;
import mekhq.campaign.mission.scenarios.salvage.SalvageTechFilter.HiddenReason;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.utilities.PersonnelStateColors;

/**
 * The pre-scenario salvage planner, where the player picks the formations that recover wrecks after the battle (the
 * salvage teams) and the techs who go with them.
 *
 * <p>The planner is a two-page dialog in the style of the StratCon deployment wizard: a Teams page and a Techs page,
 * each a searchable, filterable board of cards beside an inspector with a dossier and a tray of what is staged. Both
 * pages share one plan, a {@link SalvageOperationDraft}, which reaches the scenario only when the player commits.
 * Cancelling leaves the scenario's salvage assignments as they were.</p>
 *
 * <p>The Techs page is built for campaigns with hundreds of techs: filters hide injured and pregnant techs and techs
 * already assigned to units, or show only one experience level, with a count of what each filter hides. Techs the
 * player has selected are always shown. Filter choices are remembered between scenarios.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class SalvageOperationPlanner extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SalvageOperationPlanner.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";
    private static final String KEY = "SalvageOperationPlanner.";
    private static final Dimension DEFAULT_SIZE = scaleForGUI(1100, 740);
    /** The tech-time meter reads full at twice the low-time warning, or the crew's total if that is larger. */
    private static final int TECH_TIME_METER_SCALE = SalvageOperationDraft.LOW_TECH_MINUTES * 2;

    private final transient Campaign campaign;
    private final transient SalvageOperationDraft draft;
    private boolean isCommitted;
    private Page page = Page.TEAMS;
    private boolean isRefreshing;

    // Header
    private final HudModeSelector<Page> pageSelector = new HudModeSelector<>(Page.class, Page::label, this::showPage);
    private final JLabel instructions = new JLabel();
    private final HudStatTile teamsTile = new HudStatTile(text("tile.teams"), "");
    private final HudStatTile cargoTile = new HudStatTile(text("tile.cargo"), text("tile.largestUnit.sub"));
    private final HudStatTile towTile;
    private final HudStatTile techTimeTile = new HudStatTile(text("tile.techTime"), "");
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHolder = new JPanel(pages);

    // Teams page
    private final JTextField teamSearch = new JTextField();
    private final HudCheckBox hideUnavailable = new HudCheckBox(text("filter.hideUnavailable"));
    private final HudCheckBox onlyWithTech = new HudCheckBox(text("filter.onlyWithTech"));
    private final JLabel teamCount = countLabel();
    private final DefaultListModel<Object> teamModel = new DefaultListModel<>();
    private final JList<Object> teamList = new JList<>(teamModel);
    private transient @Nullable TeamOption focusedTeam;
    private final JPanel teamDossier = Hud.transparentPanel(null);
    private final JLabel teamDossierEmpty = Hud.notice(text("teamDossier.empty"));
    private final JLabel teamTitle = dossierTitleLabel();
    private final JLabel teamSub = dossierSubLabel();
    private final DefaultTableModel unitTableModel = new DefaultTableModel(new Object[] {
          text("column.unit"), text("column.drag"), text("column.cargo"), text("column.salvage") }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final HudButton stageButton = new HudButton(upper(text("stage")), false);
    private final DefaultListModel<TeamOption> stagedModel = new DefaultListModel<>();
    private final JList<TeamOption> stagedList = new JList<>(stagedModel);
    private final JLabel stagedEmpty = Hud.hint("<html>" + text("staged.empty") + "</html>");

    // Techs page
    private final JTextField techSearch = new JTextField();
    private final JComboBox<TechSort> techSort = new JComboBox<>(TechSort.values());
    private final HudCheckBox hideInjured = new HudCheckBox(text("filter.hideInjured"));
    private final HudCheckBox hidePregnant = new HudCheckBox(text("filter.hidePregnant"));
    private final HudCheckBox hideUnitTechs = new HudCheckBox(text("filter.hideUnitTechs"));
    private final JComboBox<ExperienceFilter> experience = new JComboBox<>(ExperienceFilter.values());
    private final JLabel techCount = countLabel();
    private final DefaultListModel<Object> techModel = new DefaultListModel<>();
    private final JList<Object> techList = new JList<>(techModel);
    private final JLabel hiddenNote = new JLabel();
    private final HudChip showAllChip = new HudChip(text("showAll"), this::clearTechFilters);
    private transient @Nullable SalvageTechCandidate focusedTech;
    private final JPanel techDossier = Hud.transparentPanel(null);
    private final JLabel techDossierEmpty = Hud.notice(text("techDossier.empty"));
    private final JLabel techTitle = dossierTitleLabel();
    private final JLabel techSub = dossierSubLabel();
    private final HudStatTile techMinutesTile = new HudStatTile(text("techTile.minutes"), "");
    private final HudStatTile techFatigueTile = new HudStatTile(text("techTile.fatigue"), "");
    private final HudStatTile techEdgeTile = new HudStatTile(text("techTile.edge"), "");
    private final HudStatTile techInjuryTile;
    private final JLabel techStatus = new JLabel();
    private final HudButton selectButton = new HudButton(upper(text("select")), false);
    private final DefaultListModel<SalvageTechCandidate> crewModel = new DefaultListModel<>();
    private final JList<SalvageTechCandidate> crewList = new JList<>(crewModel);
    private final JLabel crewEmpty = Hud.hint(text("crew.empty"));

    // Buttons
    private final HudButton backNextButton = new HudButton("", false);
    private final JLabel footer = new JLabel();

    /** The planner's pages. */
    private enum Page {
        TEAMS, TECHS;

        private String label() {
            return text("page." + name());
        }
    }

    /** The orders the tech board can be sorted in. */
    private enum TechSort {
        MINUTES, SKILL, NAME;

        @Override
        public String toString() {
            return text("sort." + name());
        }
    }

    /** The experience levels the tech board can be filtered to. */
    private enum ExperienceFilter {
        ANY(null),
        ULTRA_GREEN(SkillLevel.ULTRA_GREEN),
        GREEN(SkillLevel.GREEN),
        REGULAR(SkillLevel.REGULAR),
        VETERAN(SkillLevel.VETERAN),
        ELITE(SkillLevel.ELITE),
        HEROIC(SkillLevel.HEROIC),
        LEGENDARY(SkillLevel.LEGENDARY);

        private final @Nullable SkillLevel skillLevel;

        ExperienceFilter(@Nullable SkillLevel skillLevel) {
            this.skillLevel = skillLevel;
        }

        @Override
        public String toString() {
            return (skillLevel == null) ? text("filter.experience.any") : skillLevel.toString();
        }
    }

    /** A divider row captioning a group of cards on a board. */
    private record SectionHeader(String label) {}

    /**
     * Opens the planner for a scenario's salvage operation.
     *
     * @param campaign the current campaign
     * @param draft    the plan to edit, started from the scenario's current salvage assignments
     */
    public SalvageOperationPlanner(Campaign campaign, SalvageOperationDraft draft) {
        super((Frame) null, text("title"), true);
        this.campaign = campaign;
        this.draft = draft;

        if (draft.isInSpace()) {
            towTile = new HudStatTile(text("tile.tug"), text("tile.tug.sub"));
        } else {
            towTile = new HudStatTile(text("tile.tow"), text("tile.largestUnit.sub"));
        }
        techInjuryTile = new HudStatTile(campaign.getCampaignOptions().isUseAdvancedMedical() ?
                                               text("techTile.injuries") :
                                               text("techTile.hits"), "");

        buildWindow();
        showPage(Page.TEAMS);
        setVisible(true);

        if (!isCommitted) {
            LOGGER.debug("[Salvage] Salvage plan for {} cancelled", draft.getScenario().getName());
        }
    }

    /**
     * @return {@code true} if the player committed the plan to the scenario; {@code false} if they cancelled
     */
    public boolean wasCommitted() {
        return isCommitted;
    }

    // region Layout

    private void buildWindow() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose(); // Closing the window cancels the plan
            }
        });
        getContentPane().setBackground(GROUND);
        setLayout(new BorderLayout());

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(leftAligned(buildCommandBar()));
        JPanel tileHolder = Hud.transparentPanel(new BorderLayout());
        tileHolder.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), 0, scaleForGUI(12)));
        tileHolder.add(Hud.tileRow(teamsTile, cargoTile, towTile, techTimeTile), BorderLayout.CENTER);
        north.add(leftAligned(tileHolder));
        add(north, BorderLayout.NORTH);

        pageHolder.setOpaque(false);
        pageHolder.add(buildTeamsPage(), Page.TEAMS.name());
        pageHolder.add(buildTechsPage(), Page.TECHS.name());
        add(pageHolder, BorderLayout.CENTER);

        add(buildButtons(), BorderLayout.SOUTH);

        setSize(DEFAULT_SIZE);
        setPreferredSize(DEFAULT_SIZE);
        setLocationRelativeTo(null);
        setPreferences(); // Must be before setVisible
    }

    private JComponent buildCommandBar() {
        JPanel commandBar = new JPanel();
        commandBar.setOpaque(true);
        commandBar.setBackground(GROUND);
        commandBar.setLayout(new BoxLayout(commandBar, BoxLayout.Y_AXIS));
        int pad = scaleForGUI(12);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(pad, scaleForGUI(14), pad, scaleForGUI(14))));

        JPanel titleRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(16), 0));
        JLabel title = new JLabel(upper(formatted("heading", draft.getScenario().getName())));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.25f, 0.16f));
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(Hud.hint(contextText()), BorderLayout.EAST);
        commandBar.add(leftAligned(titleRow));
        commandBar.add(Box.createVerticalStrut(scaleForGUI(8)));

        JPanel selectorRow = Hud.transparentPanel(new BorderLayout());
        selectorRow.add(pageSelector, BorderLayout.WEST);
        commandBar.add(leftAligned(selectorRow));
        commandBar.add(Box.createVerticalStrut(scaleForGUI(8)));

        instructions.setForeground(TEXT_MUTED);
        instructions.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        commandBar.add(leftAligned(instructions));
        return commandBar;
    }

    private String contextText() {
        String system = campaign.getCampaignOptions().get(CampaignOption.SALVAGE_SYSTEM).getLabel();
        String environment = draft.isInSpace() ? text("context.space") : text("context.ground");
        return system + " · " + environment;
    }

    private JComponent buildTeamsPage() {
        // Board
        JPanel board = boardPanel();
        JPanel top = Hud.transparentPanel(null);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(leftAligned(searchRow(teamSearch, null)));
        top.add(Box.createVerticalStrut(scaleForGUI(6)));
        JPanel filterBar = filterBar();
        JPanel filterRow = filterRow();
        filterRow.add(hideUnavailable);
        filterRow.add(onlyWithTech);
        filterBar.add(filterRow, BorderLayout.CENTER);
        filterBar.add(teamCount, BorderLayout.EAST);
        top.add(leftAligned(filterBar));
        board.add(top, BorderLayout.NORTH);

        hideUnavailable.setSelected(true);
        hideUnavailable.setName("hideUnavailable");
        onlyWithTech.setName("onlyWithTech");
        hideUnavailable.addItemListener(event -> refreshAll());
        onlyWithTech.addItemListener(event -> refreshAll());
        onSearchChange(teamSearch, this::refreshAll);

        teamList.setCellRenderer(new TeamBoardRenderer());
        configureBoardList(teamList, value -> {
            if (value instanceof TeamOption option) {
                focusedTeam = option;
                refreshAll();
            }
        }, this::toggleFocusedTeam);
        board.add(scroll(teamList), BorderLayout.CENTER);

        // Inspector
        JPanel inspector = inspectorPanel();
        teamDossier.setLayout(new BoxLayout(teamDossier, BoxLayout.Y_AXIS));
        teamDossier.add(leftAligned(Hud.eyebrow(text("teamDossier.heading"))));
        teamDossier.add(Box.createVerticalStrut(scaleForGUI(4)));
        teamDossier.add(leftAligned(teamTitle));
        teamDossier.add(leftAligned(teamSub));
        teamDossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        JTable unitTable = new JTable(unitTableModel);
        Hud.styleTable(unitTable);
        JScrollPane unitScroll = new JScrollPane(unitTable);
        Hud.styleScroll(unitScroll, SURFACE_DEEP, true);
        unitScroll.setPreferredSize(new Dimension(scaleForGUI(360), scaleForGUI(150)));
        teamDossier.add(leftAligned(unitScroll));
        teamDossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        stageButton.addActionListener(event -> toggleFocusedTeam());
        teamDossier.add(leftAligned(westAligned(stageButton)));

        JPanel dossierHolder = Hud.transparentPanel(new BorderLayout());
        dossierHolder.add(teamDossier, BorderLayout.NORTH);
        dossierHolder.add(teamDossierEmpty, BorderLayout.CENTER);
        inspector.add(dossierHolder, BorderLayout.NORTH);

        JPanel tray = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(6)));
        tray.add(Hud.sectionHeading(text("staged.heading")), BorderLayout.NORTH);
        stagedList.setCellRenderer(new StagedTeamRenderer());
        stagedList.setBackground(SURFACE_DEEP);
        stagedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stagedList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                TeamOption option = stagedList.getSelectedValue();
                if (option == null) {
                    return;
                }
                if (event.getClickCount() == 2) {
                    draft.unstage(option);
                }
                focusedTeam = option;
                refreshAll();
            }
        });
        tray.add(scroll(stagedList), BorderLayout.CENTER);
        tray.add(stagedEmpty, BorderLayout.SOUTH);
        inspector.add(tray, BorderLayout.CENTER);

        return split(board, inspector);
    }

    private JComponent buildTechsPage() {
        // Board
        JPanel board = boardPanel();
        JPanel top = Hud.transparentPanel(null);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        Hud.styleComboBox(techSort);
        techSort.setName("techSort");
        techSort.addActionListener(event -> refreshAll());
        top.add(leftAligned(searchRow(techSearch, techSort)));
        top.add(Box.createVerticalStrut(scaleForGUI(6)));

        // Two rows: the checkboxes, then the experience filter and the count beneath them
        JPanel filterBar = filterBar();
        JPanel rows = Hud.transparentPanel(null);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        JPanel checkBoxRow = filterRow();
        checkBoxRow.add(hideInjured);
        checkBoxRow.add(hidePregnant);
        checkBoxRow.add(hideUnitTechs);
        rows.add(leftAligned(checkBoxRow));
        JPanel divider = new JPanel();
        divider.setBackground(DIVIDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(1)));
        divider.setPreferredSize(new Dimension(scaleForGUI(10), scaleForGUI(1)));
        rows.add(Box.createVerticalStrut(scaleForGUI(5)));
        rows.add(leftAligned(divider));
        rows.add(Box.createVerticalStrut(scaleForGUI(5)));
        JPanel experienceRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        JPanel experiencePicker = filterRow();
        experiencePicker.add(Hud.eyebrow(text("filter.experience")));
        Hud.styleComboBox(experience);
        experience.setName("experience");
        experiencePicker.add(experience);
        experienceRow.add(experiencePicker, BorderLayout.WEST);
        experienceRow.add(techCount, BorderLayout.EAST);
        rows.add(leftAligned(experienceRow));
        filterBar.add(rows, BorderLayout.CENTER);
        top.add(leftAligned(filterBar));
        board.add(top, BorderLayout.NORTH);

        for (HudCheckBox checkBox : List.of(hideInjured, hidePregnant, hideUnitTechs)) {
            checkBox.addItemListener(event -> refreshAll());
        }
        hideInjured.setName("hideInjured");
        hidePregnant.setName("hidePregnant");
        hideUnitTechs.setName("hideUnitTechs");
        experience.addActionListener(event -> refreshAll());
        onSearchChange(techSearch, this::refreshAll);

        techList.setCellRenderer(new TechBoardRenderer());
        configureBoardList(techList, value -> {
            if (value instanceof SalvageTechCandidate candidate) {
                focusedTech = candidate;
                refreshAll();
            }
        }, this::toggleFocusedTech);

        JPanel listHolder = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(6)));
        listHolder.add(scroll(techList), BorderLayout.CENTER);
        JPanel hiddenRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        hiddenNote.setForeground(TEXT_FAINT);
        hiddenNote.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        hiddenRow.add(hiddenNote, BorderLayout.CENTER);
        hiddenRow.add(showAllChip, BorderLayout.EAST);
        listHolder.add(hiddenRow, BorderLayout.SOUTH);
        board.add(listHolder, BorderLayout.CENTER);

        // Inspector
        JPanel inspector = inspectorPanel();
        techDossier.setLayout(new BoxLayout(techDossier, BoxLayout.Y_AXIS));
        techDossier.add(leftAligned(Hud.eyebrow(text("techDossier.heading"))));
        techDossier.add(Box.createVerticalStrut(scaleForGUI(4)));
        techDossier.add(leftAligned(techTitle));
        techDossier.add(leftAligned(techSub));
        techDossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        List<JComponent> techTiles = new ArrayList<>(List.of(techMinutesTile, techFatigueTile));
        if (campaign.getCampaignOptions().get(CampaignOption.USE_EDGE)) {
            techTiles.add(techEdgeTile);
        }
        techTiles.add(techInjuryTile);
        techDossier.add(leftAligned(Hud.tileRow(techTiles.toArray(new JComponent[0]))));
        techDossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        techStatus.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        techStatus.setOpaque(true);
        techStatus.setBackground(SURFACE_DEEP);
        techStatus.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,
              scaleForGUI(1)), BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5),
              scaleForGUI(8))));
        techDossier.add(leftAligned(westAligned(techStatus)));
        techDossier.add(Box.createVerticalStrut(scaleForGUI(8)));
        selectButton.addActionListener(event -> toggleFocusedTech());
        techDossier.add(leftAligned(westAligned(selectButton)));

        JPanel dossierHolder = Hud.transparentPanel(new BorderLayout());
        dossierHolder.add(techDossier, BorderLayout.NORTH);
        dossierHolder.add(techDossierEmpty, BorderLayout.CENTER);
        inspector.add(dossierHolder, BorderLayout.NORTH);

        JPanel tray = Hud.transparentPanel(new BorderLayout(0, scaleForGUI(6)));
        tray.add(Hud.sectionHeading(text("crew.heading")), BorderLayout.NORTH);
        crewList.setCellRenderer(new CrewRenderer());
        crewList.setBackground(SURFACE_DEEP);
        crewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        crewList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                SalvageTechCandidate candidate = crewList.getSelectedValue();
                if (candidate == null) {
                    return;
                }
                if (event.getClickCount() == 2) {
                    draft.setTechSelected(candidate.tech().getId(), false);
                }
                focusedTech = candidate;
                refreshAll();
            }
        });
        tray.add(scroll(crewList), BorderLayout.CENTER);
        tray.add(crewEmpty, BorderLayout.SOUTH);
        inspector.add(tray, BorderLayout.CENTER);

        return split(board, inspector);
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        buttons.setOpaque(true);
        buttons.setBackground(GROUND);
        buttons.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        footer.setForeground(TEXT_FAINT);
        footer.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
        buttons.add(footer, BorderLayout.WEST);

        JPanel actions = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(8), 0));
        HudButton cancelButton = new HudButton(upper(text("button.cancel")), false);
        cancelButton.addActionListener(event -> dispose());
        actions.add(cancelButton);
        backNextButton.addActionListener(event -> showPage((page == Page.TEAMS) ? Page.TECHS : Page.TEAMS));
        actions.add(backNextButton);
        HudButton commitButton = new HudButton(upper(text("button.commit")), true);
        commitButton.addActionListener(event -> commit());
        actions.add(commitButton);
        buttons.add(actions, BorderLayout.EAST);
        return buttons;
    }

    // endregion Layout

    // region Layout helpers

    private static JPanel boardPanel() {
        JPanel board = new JPanel(new BorderLayout(0, scaleForGUI(6)));
        board.setOpaque(true);
        board.setBackground(GROUND);
        int pad = scaleForGUI(12);
        board.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, scaleForGUI(6)));
        return board;
    }

    private static JPanel inspectorPanel() {
        JPanel inspector = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        inspector.setOpaque(true);
        inspector.setBackground(GROUND);
        int pad = scaleForGUI(12);
        inspector.setBorder(BorderFactory.createEmptyBorder(pad, scaleForGUI(6), pad, pad));
        return inspector;
    }

    private static JSplitPane split(JComponent board, JComponent inspector) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, board, inspector);
        splitPane.setResizeWeight(0.58);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setBackground(GROUND);
        splitPane.setDividerSize(scaleForGUI(6));
        return splitPane;
    }

    private static JComponent searchRow(JTextField field, @Nullable JComboBox<?> sort) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        row.add(Hud.eyebrow(text("search")), BorderLayout.WEST);
        Hud.styleField(field);
        row.add(field, BorderLayout.CENTER);
        if (sort != null) {
            JPanel sortPanel = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            sortPanel.add(Hud.eyebrow(text("sort")), BorderLayout.WEST);
            sortPanel.add(sort, BorderLayout.CENTER);
            row.add(sortPanel, BorderLayout.EAST);
        }
        return row;
    }

    private static JPanel filterBar() {
        JPanel filterBar = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        filterBar.setOpaque(true);
        filterBar.setBackground(SURFACE_DEEP);
        filterBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(9), scaleForGUI(6), scaleForGUI(9))));
        return filterBar;
    }

    private static JPanel filterRow() {
        return Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(12), 0));
    }

    private static JLabel countLabel() {
        JLabel label = new JLabel();
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
        return label;
    }

    private static JLabel dossierTitleLabel() {
        JLabel label = new JLabel();
        label.setForeground(TEXT);
        label.setFont(hudFont(Font.BOLD, 1.15f, 0.04f));
        return label;
    }

    private static JLabel dossierSubLabel() {
        JLabel label = new JLabel();
        label.setForeground(TEXT_MUTED);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    private static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        Hud.styleScroll(scroll, SURFACE_DEEP, true);
        return scroll;
    }

    private static JComponent westAligned(JComponent component) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(component, BorderLayout.WEST);
        return holder;
    }

    /**
     * Sets up a board: single selection, focusing the selected card, and toggling it on double-click or Space/Enter.
     *
     * <p>Section headers can't be selected: selecting one moves the selection on to the nearest card in the direction
     * the player was moving, so the focused card is always the one highlighted.</p>
     */
    private void configureBoardList(JList<Object> list, Consumer<Object> onFocus, Runnable onToggle) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        int[] lastCardIndex = { -1 };
        list.addListSelectionListener(event -> {
            if (isRefreshing || event.getValueIsAdjusting()) {
                return;
            }
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            if (list.getSelectedValue() instanceof SectionHeader) {
                int step = (selectedIndex < lastCardIndex[0]) ? -1 : 1;
                int cardIndex = findCardIndex(list, selectedIndex, step);
                if (cardIndex < 0) {
                    cardIndex = findCardIndex(list, selectedIndex, -step);
                }
                if (cardIndex < 0) {
                    list.clearSelection();
                } else {
                    list.setSelectedIndex(cardIndex);
                    list.ensureIndexIsVisible(cardIndex);
                }
                return;
            }
            lastCardIndex[0] = selectedIndex;
            onFocus.accept(list.getSelectedValue());
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if ((event.getClickCount() == 2) && isCardSelected(list)) {
                    onToggle.run();
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                boolean isToggleKey = (event.getKeyCode() == KeyEvent.VK_SPACE) ||
                                            (event.getKeyCode() == KeyEvent.VK_ENTER);
                if (isToggleKey && isCardSelected(list)) {
                    onToggle.run();
                }
            }
        });
    }

    private static boolean isCardSelected(JList<Object> list) {
        Object selectedValue = list.getSelectedValue();
        return (selectedValue != null) && !(selectedValue instanceof SectionHeader);
    }

    /** Finds the nearest card from an index onward in one direction, or -1 if there is none. */
    private static int findCardIndex(JList<Object> list, int fromIndex, int step) {
        for (int index = fromIndex + step; (index >= 0) && (index < list.getModel().getSize()); index += step) {
            if (!(list.getModel().getElementAt(index) instanceof SectionHeader)) {
                return index;
            }
        }
        return -1;
    }

    private static void onSearchChange(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                onChange.run();
            }
        });
    }

    // endregion Layout helpers

    // region Refresh

    private void showPage(Page newPage) {
        page = newPage;
        pageSelector.setSelected(newPage);
        pages.show(pageHolder, newPage.name());
        refreshAll();
    }

    private void refreshAll() {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;
        try {
            refreshHeader();
            if (page == Page.TEAMS) {
                refreshTeamsBoard();
                refreshTeamInspector();
            } else {
                refreshTechsBoard();
                refreshTechInspector();
            }
            refreshButtons();
        } finally {
            isRefreshing = false;
        }
    }

    private void refreshHeader() {
        pageSelector.setModeLabel(Page.TEAMS, formatted("page.count", Page.TEAMS.label(),
              draft.getStagedTeams().size()));
        pageSelector.setModeLabel(Page.TECHS, formatted("page.count", Page.TECHS.label(),
              draft.getSelectedTechs().size()));

        instructions.setText("<html>" + instructionsText() + "</html>");

        int teams = draft.getStagedTeams().size();
        teamsTile.setValue(String.valueOf(teams), teams > 0 ? ACCENT_BRIGHT : TEXT_FAINT);
        teamsTile.setSub(formatted("tile.teams.sub", draft.getSalvageUnitCount()));
        cargoTile.setValue(formatted("tile.tonnage", draft.getBestCargoCapacity()), TEXT);
        if (draft.isInSpace()) {
            boolean hasTug = draft.hasNavalTug();
            towTile.setValue(hasTug ? text("tile.tug.yes") : text("tile.tug.no"), hasTug ? READY : TEXT_FAINT);
        } else {
            towTile.setValue(formatted("tile.tonnage", draft.getBestTowCapacity()), TEXT);
        }

        int minutes = draft.getSelectedTechMinutes();
        int techs = draft.getSelectedTechs().size();
        boolean isLow = draft.isLowOnTechTime();
        Color color = isLow ? CAUTION : READY;
        techTimeTile.setValue(formatted("tile.techTime.value", minutes), color);
        techTimeTile.setSub(isLow ?
                                  formatted("tile.techTime.low.sub", techs, SalvageOperationDraft.LOW_TECH_MINUTES) :
                                  formatted("tile.techTime.sub", techs));
        int scale = Math.max(TECH_TIME_METER_SCALE, minutes);
        techTimeTile.setMeter((double) minutes / scale, color,
              (double) SalvageOperationDraft.LOW_TECH_MINUTES / scale);
    }

    private String instructionsText() {
        StringBuilder text = new StringBuilder();
        if (page == Page.TEAMS) {
            text.append(draft.getSalvageRules().isSalvageFormationCombatAllowed() ?
                              text("instructions.teams.combatAllowed") :
                              text("instructions.teams.combatForbidden"));
        } else {
            text.append(text("instructions.techs"));
            if (campaign.getCampaignOptions().get(CampaignOption.IS_USE_RISKY_SALVAGE)) {
                text.append(text("instructions.risky"));
            }
        }
        ScenarioTemplate.BattlefieldControlType fieldControl = draft.getBattlefieldControlType();
        String controlText = (fieldControl == null) ?
                                   text("fieldControlUnknown") :
                                   getText("ResolveDialog.control." + fieldControl.name());
        text.append(formatted("instructions.fieldControl", controlText));
        return text.toString();
    }

    private void refreshTeamsBoard() {
        String query = teamSearch.getText().trim().toLowerCase(Locale.ROOT);
        List<TeamOption> options = draft.getTeamOptions();
        List<TeamOption> shown = new ArrayList<>();
        for (TeamOption option : options) {
            if (isTeamShown(option, query)) {
                shown.add(option);
            }
        }

        teamModel.clear();
        addTeamSection(shown, true, text("section.salvageFormations"));
        addTeamSection(shown, false, text("section.combatTeams"));
        teamCount.setText(formatted("count.teams", shown.size(), options.size()));

        if (focusedTeam != null) {
            teamList.setSelectedValue(focusedTeam, true);
        }

        stagedModel.clear();
        for (TeamOption option : draft.getStagedTeams()) {
            stagedModel.addElement(option);
        }
        stagedEmpty.setVisible(stagedModel.isEmpty());
    }

    private boolean isTeamShown(TeamOption option, String query) {
        // Staged teams are always shown, so filters never hide the player's picks
        if (draft.isStaged(option)) {
            return true;
        }
        if (hideUnavailable.isSelected() && !option.availability().isStageable()) {
            return false;
        }
        if (onlyWithTech.isSelected() && (option.data().tech() == null)) {
            return false;
        }
        return query.isEmpty() || teamSearchText(option).contains(query);
    }

    private String teamSearchText(TeamOption option) {
        StringBuilder searchText = new StringBuilder(option.formation().getFullName());
        Person tech = option.data().tech();
        if (tech != null) {
            searchText.append(' ').append(tech.getFullName());
        }
        for (Unit unit : option.formation().getAllUnitsAsUnits(campaign.getPlayerForce().getHangar(), false)) {
            searchText.append(' ').append(unit.getName());
        }
        return searchText.toString().toLowerCase(Locale.ROOT);
    }

    private void addTeamSection(List<TeamOption> shown, boolean isSalvageSection, String label) {
        boolean hasHeader = false;
        for (TeamOption option : shown) {
            if (option.isSalvageFormation() == isSalvageSection) {
                if (!hasHeader) {
                    teamModel.addElement(new SectionHeader(label));
                    hasHeader = true;
                }
                teamModel.addElement(option);
            }
        }
    }

    private void refreshTeamInspector() {
        TeamOption option = focusedTeam;
        teamDossier.setVisible(option != null);
        teamDossierEmpty.setVisible(option == null);
        if (option == null) {
            return;
        }

        teamTitle.setText(option.formation().getFullName());
        Person tech = option.data().tech();
        String techName = (tech == null) ? text("teamDossier.noTech") : tech.getFullTitle();
        teamSub.setText(formatted("teamDossier.sub", option.formation().getFormationType().getDisplayName(),
              techName));

        unitTableModel.setRowCount(0);
        AbstractSalvage rules = draft.getSalvageRules();
        for (Unit unit : option.formation().getAllUnitsAsUnits(campaign.getPlayerForce().getHangar(), false)) {
            double tow = draft.isInSpace() ? 0.0 : CamOpsSalvageUtilities.getTowCapacity(unit);
            double cargo = (unit.getEntity() == null) ? 0.0 : unit.getCargoCapacityForSalvage();
            unitTableModel.addRow(new Object[] { unit.getName(), tonnage(tow), tonnage(cargo),
                                                 salvageAbility(rules, unit) });
        }

        boolean isStaged = draft.isStaged(option);
        stageButton.setText(upper(isStaged ? text("unstage") : text("stage")));
        stageButton.setArmed(isStaged || option.availability().isStageable());
    }

    private String salvageAbility(AbstractSalvage rules, Unit unit) {
        if (rules.isAvailableForSalvage(unit, draft.isInSpace())) {
            return text("unit.canSalvage");
        }
        if (!rules.canSalvage(unit, draft.isInSpace())) {
            return text("unit.cannotSalvage");
        }
        if (unit.isSalvage() || !unit.isRepairable()) {
            return text("unit.unrepairable");
        }
        return text("unit.immobilized");
    }

    private static String tonnage(double tons) {
        return (tons <= 0.0) ? text("unit.none") : formatted("unit.tonnage", tons);
    }

    private SalvageTechFilter currentTechFilter() {
        ExperienceFilter experienceFilter = (ExperienceFilter) experience.getSelectedItem();
        SkillLevel skillLevel = (experienceFilter == null) ? null : experienceFilter.skillLevel;
        return new SalvageTechFilter(hideInjured.isSelected(), hidePregnant.isSelected(), hideUnitTechs.isSelected(),
              skillLevel, techSearch.getText());
    }

    private void refreshTechsBoard() {
        SalvageTechFilter filter = currentTechFilter();
        List<SalvageTechCandidate> candidates = draft.getTechCandidates();
        List<SalvageTechCandidate> shown = new ArrayList<>();
        List<SalvageTechCandidate> hiddenCandidates = new ArrayList<>();
        for (SalvageTechCandidate candidate : candidates) {
            // Selected techs are always shown, so filters never hide the player's picks
            if (draft.isTechSelected(candidate.tech().getId()) || filter.shows(candidate)) {
                shown.add(candidate);
            } else {
                hiddenCandidates.add(candidate);
            }
        }
        shown.sort(techComparator());

        techModel.clear();
        addTechSection(shown, true, text("section.teamTechs"));
        addTechSection(shown, false, text("section.supervisors"));
        techCount.setText(formatted("count.techs", shown.size(), candidates.size()));

        Map<HiddenReason, Integer> hiddenCounts = filter.countHidden(hiddenCandidates);
        if (hiddenCandidates.isEmpty()) {
            hiddenNote.setText(" ");
            showAllChip.setVisible(false);
        } else {
            SkillLevel skillLevel = filter.skillLevel();
            String skillLevelName = (skillLevel == null) ? "" : skillLevel.toString();
            List<String> reasons = new ArrayList<>();
            for (Map.Entry<HiddenReason, Integer> entry : hiddenCounts.entrySet()) {
                // Only the experience reason names a level; the others ignore the extra argument
                reasons.add(formatted("hidden." + entry.getKey().name(), entry.getValue(), skillLevelName));
            }
            hiddenNote.setText(formatted("hidden", hiddenCandidates.size(),
                  String.join(text("hidden.separator"), reasons)));
            showAllChip.setVisible(true);
        }

        if (focusedTech != null) {
            techList.setSelectedValue(focusedTech, true);
        }

        crewModel.clear();
        for (SalvageTechCandidate candidate : draft.getSelectedTechs()) {
            crewModel.addElement(candidate);
        }
        crewEmpty.setVisible(crewModel.isEmpty());
    }

    private Comparator<SalvageTechCandidate> techComparator() {
        Comparator<SalvageTechCandidate> byName = Comparator.comparing(candidate -> candidate.tech().getFullName());
        TechSort sort = (TechSort) techSort.getSelectedItem();
        if (sort == TechSort.SKILL) {
            return Comparator.comparing(SalvageTechCandidate::skillLevel).reversed().thenComparing(byName);
        }
        if (sort == TechSort.NAME) {
            return byName;
        }
        return Comparator.comparingInt(SalvageTechCandidate::minutesLeft).reversed().thenComparing(byName);
    }

    private void addTechSection(List<SalvageTechCandidate> shown, boolean isTeamSection, String label) {
        boolean hasHeader = false;
        for (SalvageTechCandidate candidate : shown) {
            boolean isFromTeam = draft.getTechOrigin(candidate.tech().getId()) != TechOrigin.SUPERVISOR;
            if (isFromTeam == isTeamSection) {
                if (!hasHeader) {
                    techModel.addElement(new SectionHeader(label));
                    hasHeader = true;
                }
                techModel.addElement(candidate);
            }
        }
    }

    private void refreshTechInspector() {
        SalvageTechCandidate candidate = focusedTech;
        techDossier.setVisible(candidate != null);
        techDossierEmpty.setVisible(candidate == null);
        if (candidate == null) {
            return;
        }

        Person tech = candidate.tech();
        techTitle.setText(tech.getFullTitle());
        techSub.setText(formatted("techDossier.sub", tech.getPrimaryRole().getLabel(isClanCampaign()),
              candidate.skillLevel().toString()));

        techMinutesTile.setValue(String.valueOf(candidate.minutesLeft()), TEXT);
        int fatigue = Fatigue.getEffectiveFatigue(tech, campaign);
        techFatigueTile.setValue(String.valueOf(fatigue), isFatigued(tech) ? CAUTION : TEXT);
        techEdgeTile.setValue(String.valueOf(tech.getCurrentEdge()), TEXT);
        int injuries = campaign.getCampaignOptions().isUseAdvancedMedical() ?
                             tech.getTotalInjurySeverity() :
                             tech.getHits();
        techInjuryTile.setValue(String.valueOf(injuries), candidate.isInjured() ? DANGER : TEXT);

        List<String> reasonKeys = new ArrayList<>();
        PersonnelStateColors.getStateColors(campaign, tech, reasonKeys);
        techStatus.setText(reasonKeys.isEmpty() ?
                                 text("status.ready") :
                                 "<html>" + PersonnelStateColors.getColorReasonsText(reasonKeys) + "</html>");
        techStatus.setForeground(statusColor(reasonKeys));

        boolean isSelected = draft.isTechSelected(tech.getId());
        selectButton.setText(upper(isSelected ? text("deselect") : text("select")));
    }

    private void refreshButtons() {
        backNextButton.setText(upper((page == Page.TEAMS) ? text("button.next") : text("button.back")));
        footer.setText((page == Page.TEAMS) ? text("footer.teams") : text("footer.techs"));
    }

    // endregion Refresh

    // region Actions

    private void toggleFocusedTeam() {
        TeamOption option = focusedTeam;
        if (option == null) {
            return;
        }
        if (draft.isStaged(option)) {
            draft.unstage(option);
        } else {
            draft.stage(option);
        }
        refreshAll();
    }

    private void toggleFocusedTech() {
        SalvageTechCandidate candidate = focusedTech;
        if (candidate == null) {
            return;
        }
        draft.setTechSelected(candidate.tech().getId(), !draft.isTechSelected(candidate.tech().getId()));
        refreshAll();
    }

    private void clearTechFilters() {
        isRefreshing = true;
        try {
            hideInjured.setSelected(false);
            hidePregnant.setSelected(false);
            hideUnitTechs.setSelected(false);
            experience.setSelectedItem(ExperienceFilter.ANY);
            techSearch.setText("");
        } finally {
            isRefreshing = false;
        }
        refreshAll();
    }

    private void commit() {
        if (draft.isLowOnTechTime() && !isLowTechTimeAccepted()) {
            return;
        }
        draft.commit();
        isCommitted = true;
        if (LOGGER.isDebugEnabled()) {
            List<String> teamNames = new ArrayList<>();
            for (TeamOption option : draft.getStagedTeams()) {
                teamNames.add(option.formation().getFullName());
            }
            LOGGER.debug("[Salvage] Salvage plan for {} committed: teams {}, {} techs, {} minutes",
                  draft.getScenario().getName(), teamNames, draft.getSelectedTechs().size(),
                  draft.getSelectedTechMinutes());
        }
        dispose();
    }

    /**
     * Warns the player that the selected techs have little or no work time left for salvage operations.
     *
     * @return {@code true} if the player chose to continue anyway
     */
    private boolean isLowTechTimeAccepted() {
        int minutes = draft.getSelectedTechMinutes();
        String message = (minutes <= 0) ?
                               text("lowMinutes.none") :
                               formatted("lowMinutes.some", minutes, SalvageOperationDraft.LOW_TECH_MINUTES);
        Person speaker = campaign.getPlayerForce()
              .getHumanResources()
              .getSeniorTechPerson(campaign.getCampaignOptions(), campaign.getPlayerForce().isClanForce(),
                    campaign.getLocalDate());
        ImmersiveDialogSimple warningDialog = new ImmersiveDialogSimple(campaign, speaker, null, message,
              List.of(getText("Cancel.text"), getText("Confirm.text")), null, null, false);
        return warningDialog.getDialogChoice() != 0; // 0 = Cancel
    }

    // endregion Actions

    // region Presentation helpers

    private boolean isClanCampaign() {
        return campaign.getPlayerForce().isClanForce();
    }

    private boolean isFatigued(Person tech) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_FATIGUE) &&
                     (Fatigue.getEffectiveFatigue(tech, campaign) >= PersonnelStateColors.FATIGUE_HIGHLIGHT_THRESHOLD);
    }

    /**
     * Colors the tech's status from the same reasons it lists, so every listed reason is flagged: injuries as danger,
     * anything else as a caution.
     */
    private static Color statusColor(List<String> reasonKeys) {
        if (reasonKeys.isEmpty()) {
            return READY;
        }
        return reasonKeys.contains(PersonnelStateColors.INJURED_REASON_KEY) ? DANGER : CAUTION;
    }

    private Color techRingColor(SalvageTechCandidate candidate) {
        if (candidate.isInjured()) {
            return DANGER;
        }
        if (candidate.isPregnant() || isFatigued(candidate.tech())) {
            return CAUTION;
        }
        return READY;
    }

    private static Color teamRingColor(TeamAvailability availability) {
        return switch (availability) {
            case AVAILABLE -> READY;
            case FIGHTING_HERE -> AMBER;
            case DEPLOYED, NO_SALVAGE_UNITS -> TEXT_FAINT;
        };
    }

    private String teamSub(TeamOption option) {
        String sub;
        if (draft.isInSpace()) {
            sub = formatted("team.sub.space", option.data().salvageCapableUnits(),
                  option.data().maximumCargoCapacity());
            if (option.data().hasTug()) {
                sub += text("team.sub.tug");
            }
        } else {
            sub = formatted("team.sub", option.data().salvageCapableUnits(), option.data().maximumCargoCapacity(),
                  option.data().maximumTowCapacity());
        }
        Person tech = option.data().tech();
        if (tech != null) {
            sub += formatted("team.sub.tech", tech.getFullTitle());
        }
        return sub;
    }

    private String techCardSub(SalvageTechCandidate candidate) {
        Person tech = candidate.tech();
        StringBuilder sub = new StringBuilder(formatted("tech.sub", candidate.skillLevel().toString(),
              candidate.minutesLeft()));
        if (campaign.getCampaignOptions().get(CampaignOption.USE_EDGE)) {
            sub.append(formatted("tech.sub.edge", tech.getCurrentEdge()));
        }
        List<String> unitNames = new ArrayList<>();
        for (Unit unit : tech.getTechUnits()) {
            unitNames.add(unit.getName());
        }
        if (!unitNames.isEmpty()) {
            sub.append(formatted("tech.sub.units", String.join(", ", unitNames)));
        }
        return sub.toString();
    }

    private static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }

    private static String formatted(String key, Object... arguments) {
        return getFormattedTextAt(RESOURCE_BUNDLE, KEY + key, arguments);
    }

    /** Keeps the window's size and position, and the filter choices, in MekHQ's preferences. */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SalvageOperationPlanner.class);
            setName("SalvageOperationPlanner");
            preferences.manage(new JWindowPreference(this));
            preferences.manage(new JToggleButtonPreference(hideUnavailable));
            preferences.manage(new JToggleButtonPreference(onlyWithTech));
            preferences.manage(new JToggleButtonPreference(hideInjured));
            preferences.manage(new JToggleButtonPreference(hidePregnant));
            preferences.manage(new JToggleButtonPreference(hideUnitTechs));
            preferences.manage(new JComboBoxPreference(experience));
            preferences.manage(new JComboBoxPreference(techSort));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    // endregion Presentation helpers

    // region Renderers

    /** Renders the Teams board: section headers, and a card per formation. */
    private final class TeamBoardRenderer implements ListCellRenderer<Object> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            if (value instanceof SectionHeader header) {
                return card.sectionHeader(header.label());
            }
            TeamOption option = (TeamOption) value;
            List<Tag> tags = new ArrayList<>();
            boolean isStaged = draft.isStaged(option);
            if (isStaged) {
                tags.add(new Tag(text("tag.staged"), READY));
            }
            if (option.availability() != TeamAvailability.AVAILABLE) {
                tags.add(new Tag(text("availability." + option.availability().name()),
                      option.availability() == TeamAvailability.FIGHTING_HERE ? AMBER : TEXT_FAINT));
            }
            String role = option.isSalvageFormation() ? null : option.formation().getCombatRoleInMemory().toString();
            return card.show(teamRingColor(option.availability()), isStaged, option.formation().getFullName(), role,
                  tags, teamSub(option), String.valueOf(option.data().salvageCapableUnits()), text("team.figure.unit"),
                  isSelected);
        }
    }

    /** Renders the staged-team tray. */
    private final class StagedTeamRenderer implements ListCellRenderer<TeamOption> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends TeamOption> list, TeamOption option, int index,
              boolean isSelected, boolean cellHasFocus) {
            return card.show(READY, true, option.formation().getFullName(), null, List.of(), teamSub(option),
                  null, null, isSelected);
        }
    }

    /** Renders the Techs board: section headers, and a card per tech. */
    private final class TechBoardRenderer implements ListCellRenderer<Object> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            if (value instanceof SectionHeader header) {
                return card.sectionHeader(header.label());
            }
            SalvageTechCandidate candidate = (SalvageTechCandidate) value;
            Person tech = candidate.tech();
            boolean isGoing = draft.isTechSelected(tech.getId());

            List<Tag> tags = new ArrayList<>();
            if (isGoing) {
                tags.add(new Tag(text("tag.selected"), READY));
            }
            TechOrigin origin = draft.getTechOrigin(tech.getId());
            if (origin != TechOrigin.SUPERVISOR) {
                tags.add(new Tag(text("tag." + origin.name()), ACCENT));
            }
            if (candidate.isInjured()) {
                tags.add(new Tag(text("tag.injured"), DANGER));
            } else if (isFatigued(tech)) {
                tags.add(new Tag(text("tag.fatigued"), AMBER));
            }
            if (candidate.isPregnant()) {
                tags.add(new Tag(text("tag.pregnant"), AMBER));
            }

            return card.show(techRingColor(candidate), isGoing, tech.getFullTitle(),
                  tech.getPrimaryRole().getLabel(isClanCampaign()), tags, techCardSub(candidate),
                  String.valueOf(candidate.minutesLeft()), text("tech.figure.unit"), isSelected);
        }
    }

    /** Renders the salvage crew tray. */
    private final class CrewRenderer implements ListCellRenderer<SalvageTechCandidate> {
        private final HudCard card = new HudCard();

        @Override
        public Component getListCellRendererComponent(JList<? extends SalvageTechCandidate> list,
              SalvageTechCandidate candidate, int index, boolean isSelected, boolean cellHasFocus) {
            TechOrigin origin = draft.getTechOrigin(candidate.tech().getId());
            List<Tag> tags = List.of(new Tag(text("tag." + origin.name()), origin == TechOrigin.SUPERVISOR ?
                                                                                  TEXT_FAINT :
                                                                                  ACCENT));
            return card.show(techRingColor(candidate), true, candidate.tech().getFullTitle(), null, tags,
                  formatted("crew.sub", candidate.minutesLeft()), null, null, isSelected);
        }
    }

    // endregion Renderers
}
