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
package mekhq.gui.dialog.trainingConsole;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.training.TrainingContext;
import mekhq.campaign.personnel.training.TrainingPlan;
import mekhq.campaign.personnel.training.TrainingPlan.StageResult;
import mekhq.campaign.personnel.training.TrainingPurchase;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.campaign.personnel.training.TrainingService;
import mekhq.campaign.personnel.training.TrainingService.CommitReport;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudConfirmButton;
import mekhq.gui.baseComponents.hud.HudFilterList;
import mekhq.gui.baseComponents.hud.HudFilterList.Item;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudModeSelector;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.AbilityGroup;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.AttributeGroup;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.SkillGroup;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.Sort;
import mekhq.gui.dialog.trainingConsole.TrainingRoster.PersonEntry;

/**
 * The Training Console: one place to spend XP for any number of people at once, replacing the Spend XP menu, Mass
 * Training and Quick Train.
 *
 * <p>The player picks people from a filtered list of everyone who can train, then picks what to buy for them: skill
 * levels, special abilities, attributes, traits, or a rule that levels many skills at once. Every purchase shows, for
 * the whole selection, how many can make it now, what it costs in XP and training fees, and why the rest can't.
 * Nothing changes until the player commits: purchases are staged in a {@link TrainingPlan}, priced in order so XP is
 * never counted twice, shown on the Review page, can be undone a step at a time, and are applied by
 * {@link TrainingService}.</p>
 *
 * <p>Training is all or nothing where fees are concerned: a purchase is only staged if the campaign can pay the fees
 * of everything staged with it.</p>
 *
 * <p>The dialog is modal, so the campaign cannot change under a staged plan.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class TrainingConsole extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(TrainingConsole.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.TrainingConsole";

    private static final int RAIL_WIDTH = 260;
    /** Characters per line for the rail's check boxes, which wrap rather than run past its edge. */
    private static final int RAIL_WRAP = 30;
    private static final int PANEL_WIDTH = 470;
    /** Counts grouped by thousands; only used on the Swing thread, so one instance is shared. */
    private static final NumberFormat COUNT_FORMAT = NumberFormat.getIntegerInstance();

    private final Campaign campaign;
    private TrainingRoster roster;
    private TrainingPlan plan;
    private final TrainingConsoleState state = new TrainingConsoleState();
    private List<PersonEntry> shown = List.of();
    private int lastRowIndex = -1;
    /** The selection before a "Toggle them" link narrowed it, so a second click can bring it back. */
    private @Nullable Set<Person> selectionBeforeToggle;
    private String message = "";
    private boolean isWarning;
    private boolean closeRequested;
    /** Changes whenever the selection does, so what was worked out for the old selection is thrown away. */
    private int selectionRevision;
    /** Changes whenever the people shown do. */
    private int shownRevision;
    /** What the pages worked out from the plan and the selection, kept until either changes. */
    private final Map<String, Object> cache = new HashMap<>();
    private @Nullable TrainingPlan cachedPlan;
    private int cachedPlanRevision;
    private int cachedSelectionRevision;

    private final HudModeSelector<TrainingConsolePage> modeSelector;
    private final JLabel subtitle = new JLabel();
    private final HudStatTile unspentTile;
    private final HudStatTile selectedTile;
    private final HudStatTile stagedTile;
    private final HudStatTile feesTile;

    // Rail
    private final JComponent rail;
    private final HudButton railToggle = new HudButton("", false, true);
    private final HudFilterList<PersonnelRole> roleFilter = new HudFilterList<>(value -> {
        state.role = value;
        refreshList();
    });
    private final HudFilterList<String> formationFilter = new HudFilterList<>(value -> {
        state.formation = value;
        refreshList();
    });
    private final HudFilterList<SkillLevel> levelFilter = new HudFilterList<>(value -> {
        state.level = value;
        refreshList();
    });
    private final HudCheckBox includePrisoners = new HudCheckBox(wordWrap(getTextAt(RESOURCE_BUNDLE, "rail.prisoners"),
          RAIL_WRAP));
    private final HudCheckBox onlyStaged = new HudCheckBox(wordWrap(getTextAt(RESOURCE_BUNDLE, "rail.onlyStaged"),
          RAIL_WRAP));
    private final HudCheckBox hideExcluded = new HudCheckBox(wordWrap(getTextAt(RESOURCE_BUNDLE, "rail.hideExcluded"),
          RAIL_WRAP));

    // Center
    private final CardLayout centerCards = new CardLayout();
    private final JPanel center = new JPanel(centerCards);
    private final PeopleList peopleList;
    private final JPanel basket = ConsoleWidgets.column();
    /** Holds only the current page's tools, so the area is as tall as they are rather than the tallest page's. */
    private final JPanel tools = new JPanel(new BorderLayout());
    private final Map<TrainingConsolePage, JComponent> toolsByPage = new EnumMap<>(TrainingConsolePage.class);
    private final JComponent noTools = Hud.transparentPanel(null);
    private final JPanel skillChips = ConsoleWidgets.row(6);
    private final JPanel aptitudeChips = ConsoleWidgets.row(6);
    private final JPanel abilityChips = ConsoleWidgets.row(6);
    private final JPanel attributeChips = ConsoleWidgets.row(6);
    private final JPanel catalog = ConsoleWidgets.column();
    private final JScrollPane catalogScroll;
    private final JPanel review = ConsoleWidgets.column();

    // Guides
    private final HudCheckBox autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "guide.autoOpen"));
    private final HudGuideDrawer guideDrawer;
    private final HudButton guideButton = new HudButton("", false, true);
    private TrainingConsolePage guideOfferedFor;

    // Dock
    private final JLabel dockSummary = new JLabel();
    private final JLabel dockMessage = new JLabel();
    private final HudButton undoButton = new HudButton(upper("dock.undo"), false);
    private final HudConfirmButton discardButton = new HudConfirmButton(upper("dock.discard"),
          upper("dock.discard.confirm"), false);
    private final HudButton commitButton = new HudButton(upper("dock.commit"), true);
    private final HudButton closeButton = new HudButton(upper("dock.close"), false);

    // region Opening

    /**
     * Opens the console with the given people already selected.
     *
     * @param frame    the main window
     * @param campaign the campaign
     * @param people   people to select, possibly none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showFor(JFrame frame, Campaign campaign, Collection<Person> people) {
        TrainingConsole console = new TrainingConsole(frame, campaign, people);
        console.setVisible(true);
    }

    private TrainingConsole(JFrame frame, Campaign campaign, Collection<Person> people) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "window.title"), true);
        this.campaign = campaign;
        roster = new TrainingRoster(campaign);
        plan = newPlan();

        modeSelector = new HudModeSelector<>(TrainingConsolePage.class, TrainingConsolePage::getLabel, page -> {
            state.page = page;
            refresh();
        });
        unspentTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.unspent"), "");
        selectedTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.selected"), "");
        stagedTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.staged"), "");
        feesTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.fees"), "");

        peopleList = new PeopleList(text -> {
            state.query = text;
            refreshList();
        }, this::onRow, this::selectShown, this::clearSelection, this::sortBy, state.selected::contains,
              person -> plan.purchaseCountOf(person), person -> plan.availableXpOf(person));

        for (Person person : people) {
            PersonEntry entry = roster.entryFor(person);
            if (entry != null) {
                state.selected.add(person);
                if (entry.isPrisoner()) {
                    state.isIncludePrisoners = true;
                }
            }
        }

        TrainingConsolePreferences preferences = TrainingConsolePreferences.getInstance();
        autoOpenGuides.setSelected(preferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> preferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "guide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "guide.tab.example"), getTextAt(RESOURCE_BUNDLE, "guide.tab.terms")),
              autoOpenGuides, getTextAt(RESOURCE_BUNDLE, "guide.close"), this::closeGuide);
        guideButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "guide.button.toolTipText"));
        guideButton.addActionListener(event -> openGuide(state.page));

        rail = buildRail();
        rail.setVisible(preferences.isRailVisible());
        catalogScroll = scroll(catalog, GROUND);
        buildUI();
        refresh();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                maybeOpenGuide(state.page);
            }

            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });
        setMinimumSize(scaleForGUI(1200, 720));
        getContentPane().setPreferredSize(scaleForGUI(1440, 880));
        pack();
        setLocationRelativeTo(frame);
        setPreferences();
    }

    private TrainingPlan newPlan() {
        return new TrainingPlan(new TrainingContext(campaign), campaign.getPlayerForce().getFinances().getBalance());
    }

    // endregion Opening

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        JPanel work = new JPanel(new BorderLayout());
        work.setOpaque(false);
        work.add(peopleList, BorderLayout.CENTER);
        work.add(buildPurchasePanel(), BorderLayout.EAST);
        center.setOpaque(false);
        center.add(work, "work");
        center.add(scroll(review, GROUND), TrainingConsolePage.REVIEW.name());
        review.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(18), scaleForGUI(18),
              scaleForGUI(18)));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(rail, BorderLayout.WEST);
        body.add(center, BorderLayout.CENTER);

        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(buildDock(), BorderLayout.SOUTH);
        setContentPane(content);
        guideDrawer.install(getRootPane());
    }

    private JComponent buildCommandBar() {
        JPanel identity = ConsoleWidgets.column();
        JLabel title = new JLabel(upper("header.title"));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.3f, 0.16f));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "header.eyebrow"))));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(Hud.tileRow(unspentTile, selectedTile, stagedTile, feesTile), BorderLayout.EAST);

        railToggle.setToolTipText(getTextAt(RESOURCE_BUNDLE, "rail.toggle.toolTipText"));
        railToggle.addActionListener(event -> {
            boolean visible = !rail.isVisible();
            rail.setVisible(visible);
            TrainingConsolePreferences.getInstance().setRailVisible(visible);
            refreshRailToggle();
            revalidate();
            repaint();
        });
        refreshRailToggle();

        JPanel modesRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        modesRow.add(railToggle, BorderLayout.WEST);
        modesRow.add(modeSelector, BorderLayout.CENTER);
        modesRow.add(guideButton, BorderLayout.EAST);

        JPanel bar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        bar.add(topRow, BorderLayout.CENTER);
        bar.add(modesRow, BorderLayout.SOUTH);
        return bar;
    }

    private void refreshRailToggle() {
        railToggle.setText(upper(rail.isVisible() ? "rail.hide" : "rail.show"));
    }

    private JComponent buildRail() {
        JPanel column = ConsoleWidgets.column();
        column.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.role"))));
        column.add(leftAligned(roleFilter));
        column.add(Box.createVerticalStrut(scaleForGUI(12)));
        column.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.level"))));
        column.add(leftAligned(levelFilter));
        column.add(Box.createVerticalStrut(scaleForGUI(12)));
        column.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.formation"))));
        column.add(leftAligned(formationFilter));
        column.add(Box.createVerticalStrut(scaleForGUI(12)));
        column.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.include"))));
        for (HudCheckBox box : List.of(includePrisoners, onlyStaged, hideExcluded)) {
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(box);
        }
        includePrisoners.addActionListener(event -> {
            state.isIncludePrisoners = includePrisoners.isSelected();
            refreshList();
        });
        onlyStaged.addActionListener(event -> {
            state.isOnlyStaged = onlyStaged.isSelected();
            refreshList();
        });
        hideExcluded.addActionListener(event -> {
            state.isHideExcluded = hideExcluded.isSelected();
            refreshList();
        });
        hideExcluded.setToolTipText(getTextAt(RESOURCE_BUNDLE, "rail.hideExcluded.toolTipText"));
        column.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(12),
              scaleForGUI(12)));

        JScrollPane scroll = scroll(column, GROUND);
        scroll.setPreferredSize(scaleForGUI(RAIL_WIDTH, 0));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        return scroll;
    }

    private JComponent buildPurchasePanel() {
        basket.setOpaque(true);
        basket.setBackground(SURFACE);
        basket.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10), scaleForGUI(12))));

        tools.setOpaque(false);
        toolsByPage.put(TrainingConsolePage.SKILLS, searchTools(getTextAt(RESOURCE_BUNDLE, "skills.search"), text -> {
            state.skillQuery = text;
            refreshPanel();
        }, skillChips));
        toolsByPage.put(TrainingConsolePage.APTITUDES, searchTools(getTextAt(RESOURCE_BUNDLE, "aptitudes.search"),
              text -> {
                  state.aptitudeQuery = text;
                  refreshPanel();
              }, aptitudeChips));
        toolsByPage.put(TrainingConsolePage.ABILITIES, searchTools(getTextAt(RESOURCE_BUNDLE, "abilities.search"),
              text -> {
                  state.abilityQuery = text;
                  refreshPanel();
              }, abilityChips));
        toolsByPage.put(TrainingConsolePage.ATTRIBUTES, searchTools(getTextAt(RESOURCE_BUNDLE,
              "attributes.search"), text -> {
            state.attributeQuery = text;
            refreshPanel();
        }, attributeChips));

        JPanel north = ConsoleWidgets.column();
        basket.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(basket);
        north.add(tools);

        catalog.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(16),
              scaleForGUI(10)));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(GROUND);
        panel.setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));
        panel.add(north, BorderLayout.NORTH);
        panel.add(catalogScroll, BorderLayout.CENTER);
        panel.setPreferredSize(scaleForGUI(PANEL_WIDTH, 0));
        return panel;
    }

    private static JComponent searchTools(String label, Consumer<String> onSearch, JPanel chips) {
        JTextField field = new JTextField();
        Hud.styleField(field);
        field.getAccessibleContext().setAccessibleName(label);
        ConsoleWidgets.onTextSettled(field, onSearch);
        JPanel searchRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        searchRow.add(Hud.eyebrow(label), BorderLayout.WEST);
        searchRow.add(field, BorderLayout.CENTER);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tools = ConsoleWidgets.column();
        tools.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        tools.add(searchRow);
        tools.add(Box.createVerticalStrut(scaleForGUI(6)));
        tools.add(chips);
        return tools;
    }

    private JComponent buildDock() {
        dockSummary.setForeground(TEXT);
        dockSummary.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        dockMessage.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        JPanel text = ConsoleWidgets.column();
        text.add(leftAligned(dockSummary));
        text.add(leftAligned(dockMessage));

        undoButton.addActionListener(event -> {
            if (plan.undo()) {
                setMessage(getTextAt(RESOURCE_BUNDLE, "message.undone"), false);
            }
        });
        discardButton.addActionListener(event -> {
            int count = plan.purchaseCount();
            plan.clear();
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.discarded", count), false);
        });
        commitButton.addActionListener(event -> commit());
        closeButton.addActionListener(event -> requestClose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        for (JComponent button : List.of(undoButton, discardButton, commitButton, closeButton)) {
            buttons.add(Box.createHorizontalStrut(scaleForGUI(10)));
            buttons.add(button);
        }

        JPanel dock = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        dock.setOpaque(true);
        dock.setBackground(SURFACE_DEEP);
        dock.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));
        dock.add(text, BorderLayout.CENTER);
        dock.add(buttons, BorderLayout.EAST);
        return dock;
    }

    private static JScrollPane scroll(JComponent content, Color background) {
        // The holder follows the viewport's width, so long rows wrap instead of running off, and keeps the content
        // at its natural height at the top
        JScrollablePanel holder = new JScrollablePanel();
        holder.setLayout(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(background);
        holder.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, background, false);
        return scroll;
    }

    // endregion Layout

    // region Refreshing

    /** Redraws everything from the state and the plan. */
    void refresh() {
        modeSelector.setSelected(state.page);
        if (state.page != guideOfferedFor) {
            closeGuide();
            guideOfferedFor = state.page;
            maybeOpenGuide(state.page);
        }
        guideButton.setText("?  " + getFormattedTextAt(RESOURCE_BUNDLE, "guide.button", state.page.getLabel())
                                             .toUpperCase(Locale.ROOT));
        centerCards.show(center, (state.page == TrainingConsolePage.REVIEW) ? TrainingConsolePage.REVIEW.name() :
                                       "work");
        subtitle.setText(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", formatCount(roster.entries().size()),
              campaign.getLocalDate(), plan.funds().toAmountAndSymbolString()));
        refreshShownPeople();
        refreshPanel();
    }

    /**
     * Redraws the people list after a search, filter or sort. The catalog only depends on who is shown when nobody is
     * selected on the Auto-train page, which then trains everyone shown.
     */
    private void refreshList() {
        refreshShownPeople();
        if ((state.page == TrainingConsolePage.AUTO_TRAIN) && state.selected.isEmpty()) {
            refreshPanel();
        }
    }

    private void refreshShownPeople() {
        List<PersonEntry> base = new ArrayList<>();
        Map<PersonnelRole, Integer> byRole = new TreeMap<>(Comparator.comparing(
              (PersonnelRole role) -> role.getLabel(false)));
        Map<String, Integer> byFormation = new TreeMap<>();
        Map<SkillLevel, Integer> byLevel = new EnumMap<>(SkillLevel.class);
        String query = state.query.trim().toLowerCase(Locale.ROOT);
        for (PersonEntry entry : roster.entries()) {
            Person person = entry.person();
            if ((entry.isPrisoner() && !state.isIncludePrisoners)
                      || (state.isHideExcluded && person.isQuickTrainIgnore())
                      || (state.isOnlyStaged && (plan.purchaseCountOf(person) == 0))
                      || (!query.isEmpty() && !entry.search().contains(query))) {
                continue;
            }
            byRole.merge(entry.role(), 1, Integer::sum);
            if (!entry.formationName().isEmpty()) {
                byFormation.merge(entry.formationName(), 1, Integer::sum);
            }
            byLevel.merge(entry.skillLevel(), 1, Integer::sum);
            base.add(entry);
        }
        List<PersonEntry> filtered = new ArrayList<>();
        for (PersonEntry entry : base) {
            if (((state.role == null) || (entry.role() == state.role))
                      && ((state.formation == null) || entry.formationName().equals(state.formation))
                      && ((state.level == null) || (entry.skillLevel() == state.level))) {
                filtered.add(entry);
            }
        }
        Comparator<PersonEntry> comparator = switch (state.sort) {
            // Not selected sorts after selected, so ascending puts the selection on top
            case SELECTED -> Comparator.comparing((PersonEntry entry) -> !state.selected.contains(entry.person()));
            case NAME -> Comparator.comparing(PersonEntry::name);
            case ROLE -> Comparator.comparing(PersonEntry::roleName);
            case FORMATION -> Comparator.comparing(PersonEntry::formationName);
            case EXPERIENCE -> Comparator.comparing(PersonEntry::skillLevel);
            case XP -> Comparator.comparingInt((PersonEntry entry) -> plan.availableXpOf(entry.person()));
        };
        if (!state.isAscending) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator.thenComparing(PersonEntry::name));
        if (!filtered.equals(shown)) {
            shownRevision++;
        }
        shown = filtered;
        peopleList.setRows(shown);
        peopleList.setSortShown(state.sort, state.isAscending);
        int selectedShown = 0;
        for (PersonEntry entry : shown) {
            if (state.selected.contains(entry.person())) {
                selectedShown++;
            }
        }
        peopleList.setCount(shown.size(), state.selected.size(), state.selected.size() - selectedShown);
        peopleList.redraw();

        List<Item<PersonnelRole>> roles = new ArrayList<>();
        roles.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, "rail.allRoles"), base.size()));
        boolean isClan = campaign.getPlayerForce().isClanForce();
        byRole.forEach((role, count) -> roles.add(new Item<>(role, role.getLabel(isClan), count)));
        roleFilter.setItems(roles);
        roleFilter.setSelected(state.role);

        List<Item<SkillLevel>> levels = new ArrayList<>();
        levels.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, "rail.allLevels"), base.size()));
        byLevel.forEach((level, count) -> levels.add(new Item<>(level, level.toString(), count)));
        levelFilter.setItems(levels);
        levelFilter.setSelected(state.level);

        List<Item<String>> formations = new ArrayList<>();
        formations.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, "rail.allFormations"), base.size()));
        byFormation.forEach((formation, count) -> formations.add(new Item<>(formation, formation, count)));
        formationFilter.setItems(formations);
        formationFilter.setSelected(state.formation);

        includePrisoners.setSelected(state.isIncludePrisoners);
        onlyStaged.setSelected(state.isOnlyStaged);
        hideExcluded.setSelected(state.isHideExcluded);
    }

    /** Redraws the purchase panel, the review page, the tiles and the dock, after a purchase or a setting changed. */
    void refreshPanel() {
        List<Person> selection = selection();
        refreshBasket(selection);
        refreshChips();

        int scroll = catalogScroll.getVerticalScrollBar().getValue();
        catalog.removeAll();
        List<JComponent> parts = switch (state.page) {
            case SKILLS -> selection.isEmpty() ? List.of() : SkillsPage.build(this, selection);
            case APTITUDES -> selection.isEmpty() ? List.of() : AptitudesPage.build(this, selection);
            case ABILITIES -> selection.isEmpty() ? List.of() : AbilitiesPage.build(this, selection);
            case ATTRIBUTES -> selection.isEmpty() ? List.of() : AttributesPage.build(this, selection);
            case AUTO_TRAIN -> AutoTrainPage.build(this, selection, shownPeople());
            case REVIEW -> List.of();
        };
        if (parts.isEmpty() && (state.page != TrainingConsolePage.REVIEW)) {
            parts = List.of(Hud.notice(getTextAt(RESOURCE_BUNDLE, "panel.emptySelection")));
        }
        for (JComponent part : parts) {
            part.setAlignmentX(Component.LEFT_ALIGNMENT);
            catalog.add(part);
            catalog.add(Box.createVerticalStrut(scaleForGUI(6)));
        }
        catalog.revalidate();
        catalog.repaint();
        SwingUtilities.invokeLater(() -> catalogScroll.getVerticalScrollBar().setValue(scroll));

        if (state.page == TrainingConsolePage.REVIEW) {
            review.removeAll();
            for (JComponent part : ReviewPage.build(this)) {
                part.setAlignmentX(Component.LEFT_ALIGNMENT);
                review.add(part);
                review.add(Box.createVerticalStrut(scaleForGUI(8)));
            }
            review.revalidate();
            review.repaint();
        }

        refreshTiles(selection);
        refreshDock();
        peopleList.redraw();
    }

    private void refreshBasket(List<Person> selection) {
        basket.removeAll();
        JPanel top = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        top.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "basket.title")), BorderLayout.WEST);
        if (!selection.isEmpty()) {
            top.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "people.clear"), this::clearSelection), BorderLayout.EAST);
        }
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        basket.add(top);

        int free = 0;
        int none = 0;
        Map<String, Integer> byRole = new TreeMap<>();
        for (Person person : selection) {
            int available = plan.availableXpOf(person);
            free += Math.max(0, available);
            if (available <= 0) {
                none++;
            }
            PersonEntry entry = roster.entryFor(person);
            if (entry != null) {
                byRole.merge(entry.roleName(), 1, Integer::sum);
            }
        }
        String roles = byRole.entrySet().stream()
                             .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                             .limit(2)
                             .map(entry -> getFormattedTextAt(RESOURCE_BUNDLE, "basket.role", entry.getValue(),
                                   entry.getKey()))
                             .reduce((first, second) -> first + ", " + second).orElse("");
        if (byRole.size() > 2) {
            roles = getFormattedTextAt(RESOURCE_BUNDLE, "basket.moreRoles", roles, byRole.size() - 2);
        }
        JLabel count = ConsoleWidgets.bold(selection.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "basket.none") :
                                                 getFormattedTextAt(RESOURCE_BUNDLE, "basket.count",
                                                       selection.size()), 1.05f, ACCENT_BRIGHT);
        basket.add(leftAligned(count));
        if (!roles.isEmpty()) {
            basket.add(leftAligned(ConsoleWidgets.label(roles, 0.82f, TEXT_MUTED)));
        }
        if (!selection.isEmpty()) {
            basket.add(leftAligned(ConsoleWidgets.label((none > 0) ?
                                                              getFormattedTextAt(RESOURCE_BUNDLE,
                                                                    "basket.freeNone", free, none) :
                                                              getFormattedTextAt(RESOURCE_BUNDLE, "basket.free", free),
                  0.82f, TEXT_MUTED)));
        }
        basket.revalidate();
        basket.repaint();

        JComponent pageTools = toolsByPage.getOrDefault(state.page, noTools);
        if ((tools.getComponentCount() == 0) || (tools.getComponent(0) != pageTools)) {
            // Swapped rather than rebuilt, so a search field keeps its text and the keyboard when the page returns
            tools.removeAll();
            tools.add(pageTools, BorderLayout.CENTER);
            tools.revalidate();
            tools.repaint();
        }
    }

    private void refreshChips() {
        skillChips.removeAll();
        for (SkillGroup group : SkillGroup.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "SkillGroup." + group.name()), () -> {
                state.skillGroup = group;
                refreshPanel();
            });
            chip.setActive(state.skillGroup == group);
            skillChips.add(chip);
        }
        skillChips.revalidate();
        skillChips.repaint();

        aptitudeChips.removeAll();
        for (SkillGroup group : SkillGroup.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "SkillGroup." + group.name()), () -> {
                state.aptitudeGroup = group;
                refreshPanel();
            });
            chip.setActive(state.aptitudeGroup == group);
            aptitudeChips.add(chip);
        }
        aptitudeChips.revalidate();
        aptitudeChips.repaint();

        abilityChips.removeAll();
        for (AbilityGroup group : AbilityGroup.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "AbilityGroup." + group.name()), () -> {
                state.abilityGroup = group;
                refreshPanel();
            });
            chip.setActive(state.abilityGroup == group);
            abilityChips.add(chip);
        }
        abilityChips.revalidate();
        abilityChips.repaint();

        attributeChips.removeAll();
        for (AttributeGroup group : AttributeGroup.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "AttributeGroup." + group.name()), () -> {
                state.attributeGroup = group;
                refreshPanel();
            });
            chip.setActive(state.attributeGroup == group);
            attributeChips.add(chip);
        }
        attributeChips.revalidate();
        attributeChips.repaint();
    }

    private void refreshTiles(List<Person> selection) {
        int[] totals = cached("tiles.unspent", () -> {
            int xp = 0;
            int count = 0;
            for (PersonEntry entry : roster.entries()) {
                if (!entry.isPrisoner()) {
                    xp += Math.max(0, plan.availableXpOf(entry.person()));
                    count++;
                }
            }
            return new int[] { xp, count };
        });
        int unspent = totals[0];
        int people = totals[1];
        unspentTile.setValue(formatCount(unspent), TEXT);
        unspentTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "tile.unspent.sub", formatCount(people)));

        int selectedFree = 0;
        for (Person person : selection) {
            selectedFree += Math.max(0, plan.availableXpOf(person));
        }
        selectedTile.setValue(formatCount(selection.size()), ACCENT_BRIGHT);
        selectedTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "tile.selected.sub", formatCount(selectedFree)));

        stagedTile.setValue(getFormattedTextAt(RESOURCE_BUNDLE, "xp.amount", plan.totalXp()), AMBER);
        stagedTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "tile.staged.sub", plan.purchaseCount(),
              plan.changedPeopleCount()));

        TrainingContext context = plan.context();
        if (!context.isUseSkillTrainingCosts() && !context.isUseSpaTrainingCosts()) {
            feesTile.setValue(getTextAt(RESOURCE_BUNDLE, "tile.fees.off"), TEXT_FAINT);
            feesTile.setSub(getTextAt(RESOURCE_BUNDLE, "tile.fees.off.sub"));
            feesTile.hideMeter();
        } else {
            Money funds = plan.funds();
            Money fees = plan.totalFees();
            double share = funds.isPositive() ?
                                 fees.getAmount().doubleValue() / funds.getAmount().doubleValue() : 1.0;
            feesTile.setValue(fees.toAmountAndSymbolString(), (share > 0.8) ? DANGER : AMBER);
            feesTile.setSub(getFormattedTextAt(RESOURCE_BUNDLE, "tile.fees.sub",
                  plan.fundsLeft().toAmountAndSymbolString()));
            feesTile.setMeter(share, (share > 0.8) ? DANGER : AMBER, null);
        }
    }

    private void refreshDock() {
        if (plan.isEmpty()) {
            dockSummary.setText(getTextAt(RESOURCE_BUNDLE, "dock.none"));
        } else if (plan.totalFees().isZero()) {
            dockSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "dock.summary", plan.purchaseCount(),
                  plan.changedPeopleCount(), formatCount(plan.totalXp())));
        } else {
            dockSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "dock.summary.fees", plan.purchaseCount(),
                  plan.changedPeopleCount(), formatCount(plan.totalXp()),
                  plan.totalFees().toAmountAndSymbolString()));
        }
        dockMessage.setText(message);
        dockMessage.setForeground(isWarning ? AMBER : TEXT_FAINT);
        int steps = plan.steps().size();
        modeSelector.setModeLabel(TrainingConsolePage.REVIEW, (steps == 0) ?
                                                                    TrainingConsolePage.REVIEW.getLabel() :
                                                                    getFormattedTextAt(RESOURCE_BUNDLE,
                                                                          "TrainingConsolePage.REVIEW.count",
                                                                          steps));
        undoButton.setArmed(!plan.isEmpty());
        discardButton.setArmed(!plan.isEmpty());
        commitButton.setArmed(!plan.isEmpty());
    }

    // endregion Refreshing

    // region Selection and rows

    /** @return the selected people, in the order the roster lists them */
    List<Person> selection() {
        List<Person> selection = new ArrayList<>();
        for (PersonEntry entry : roster.entries()) {
            if (state.selected.contains(entry.person())) {
                selection.add(entry.person());
            }
        }
        return selection;
    }

    private List<Person> shownPeople() {
        return shown.stream().map(PersonEntry::person).toList();
    }

    /** Records that the selection changed, so what was worked out for the old one is worked out again. */
    private void selectionChanged() {
        selectionRevision++;
    }

    private void onRow(PersonEntry entry, int index, boolean isRange) {
        List<PersonEntry> rows = peopleList.rows();
        if (isRange && (lastRowIndex >= 0) && (lastRowIndex < rows.size())) {
            for (int row = Math.min(index, lastRowIndex); row <= Math.max(index, lastRowIndex); row++) {
                state.selected.add(rows.get(row).person());
            }
        } else {
            Person person = entry.person();
            if (!state.selected.remove(person)) {
                state.selected.add(person);
            }
            lastRowIndex = index;
        }
        refreshAfterSelection();
    }

    private void selectShown() {
        shown.forEach(entry -> state.selected.add(entry.person()));
        refreshAfterSelection();
    }

    private void clearSelection() {
        state.selected.clear();
        lastRowIndex = -1;
        refreshAfterSelection();
    }

    /**
     * Toggles the selection to a group, for the "Toggle them" links such as the 12 short on XP: the first click
     * narrows the selection to the group, and a second click on the narrowed selection brings back what was selected
     * before.
     *
     * @param people the group
     */
    void toggle(Collection<Person> people) {
        Set<Person> group = new LinkedHashSet<>(people);
        lastRowIndex = -1;
        selectionChanged();
        if ((selectionBeforeToggle != null) && state.selected.equals(group)) {
            state.selected.clear();
            state.selected.addAll(selectionBeforeToggle);
            selectionBeforeToggle = null;
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.toggleRestored", state.selected.size()), false);
            return;
        }
        selectionBeforeToggle = new LinkedHashSet<>(state.selected);
        state.selected.clear();
        state.selected.addAll(group);
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.toggled", group.size()), false);
    }

    private void refreshAfterSelection() {
        selectionChanged();
        selectionBeforeToggle = null;
        int selectedShown = 0;
        Set<Person> shownSet = new HashSet<>(shownPeople());
        for (Person person : state.selected) {
            if (shownSet.contains(person)) {
                selectedShown++;
            }
        }
        peopleList.setCount(shown.size(), state.selected.size(), state.selected.size() - selectedShown);
        if (state.isOnlyStaged) {
            refresh();
        } else {
            refreshPanel();
        }
    }

    private void sortBy(Sort sort) {
        // XP sorts most first, as the people with XP to spend are usually the ones wanted; the checkbox column
        // sorts the selection to the top
        state.isAscending = (state.sort == sort) ? !state.isAscending : (sort != Sort.XP);
        state.sort = sort;
        refreshList();
    }

    // endregion Selection and rows

    // region Staging, commit and close

    /**
     * Stages a batch of purchases as one step and reports what happened.
     *
     * @param label     what the player bought, for the review list
     * @param purchases the purchases, in order
     */
    void stage(String label, List<? extends TrainingPurchase> purchases) {
        if (purchases.isEmpty()) {
            setMessage(getTextAt(RESOURCE_BUNDLE, "message.nothingToStage"), true);
            return;
        }
        StageResult result = plan.stage(label, purchases);
        if (result.step() == null) {
            if (!result.shortfall().isZero()) {
                setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.cannotAfford", label,
                      result.shortfall().toAmountAndSymbolString()), true);
            } else {
                setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.nothingStaged", refusalSummary(result)),
                      true);
            }
            return;
        }
        int people = result.step().peopleCount();
        String staged = getFormattedTextAt(RESOURCE_BUNDLE, "message.staged", label, people,
              formatCount(result.step().xp()));
        if (result.refusedCount() > 0) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.stagedSome", staged, refusalSummary(result)),
                  true);
        } else {
            setMessage(staged, false);
        }
    }

    private static String refusalSummary(StageResult result) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<TrainingRefusal, Integer> entry : result.refused().entrySet()) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "message.refusal", entry.getValue(),
                  entry.getKey().getLabel()));
        }
        return String.join(", ", parts);
    }

    /**
     * Removes a staged step and prices the steps after it again.
     *
     * @param index the step's position
     */
    void removeStep(int index) {
        String label = plan.steps().get(index).label();
        int dropped = plan.removeStep(index);
        state.openStep = null;
        setMessage((dropped == 0) ? getFormattedTextAt(RESOURCE_BUNDLE, "message.removed", label) :
                         getFormattedTextAt(RESOURCE_BUNDLE, "message.removedDropped", label, dropped),
              dropped > 0);
    }

    private void commit() {
        if (plan.isEmpty()) {
            return;
        }
        CommitReport report = TrainingService.commit(campaign, plan);
        if (report.isRefused()) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.commitRefused",
                  plan.totalFees().toAmountAndSymbolString()), true);
            return;
        }
        String text = (report.skipped() == 0) ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "message.committed", report.applied(),
                                  formatCount(report.xpSpent()), report.feesPaid().toAmountAndSymbolString()) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "message.committedSkipped", report.applied(),
                                  report.skipped());
        // Read everyone again: levels, abilities and funds have all changed
        roster = new TrainingRoster(campaign);
        plan = newPlan();
        state.selected.removeIf(person -> roster.entryFor(person) == null);
        selectionChanged();
        state.openStep = null;
        setMessage(text, report.skipped() > 0);
    }

    /** Closes, unless purchases are staged: then the first request warns, and a second one discards them. */
    private void requestClose() {
        if (plan.isEmpty() || closeRequested) {
            dispose();
            return;
        }
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.closePending", plan.purchaseCount()), true);
        // Set after the message, which clears it: any other action in between means the next close warns again
        closeRequested = true;
    }

    /**
     * Shows a message in the dock and redraws.
     *
     * @param text      the message
     * @param isWarning {@code true} to show it as a warning
     */
    void setMessage(String text, boolean isWarning) {
        this.message = text;
        this.isWarning = isWarning;
        closeRequested = false;
        refresh();
    }

    // endregion Staging, commit and close

    // region Guides

    private void maybeOpenGuide(TrainingConsolePage page) {
        if (!isShowing()) {
            return;
        }
        TrainingConsolePreferences preferences = TrainingConsolePreferences.getInstance();
        if (preferences.isGuideAutoOpen() && !preferences.isGuideSeen(page)) {
            openGuide(page);
        }
    }

    private void openGuide(TrainingConsolePage page) {
        TrainingConsolePreferences.getInstance().setGuideSeen(page, true);
        guideDrawer.open(TrainingConsoleGuides.build(page));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    // endregion Guides

    // region Accessors for the pages

    Campaign campaign() {
        return campaign;
    }

    TrainingPlan plan() {
        return plan;
    }

    TrainingConsoleState state() {
        return state;
    }

    /** @return a number that changes whenever the people shown in the list do */
    int shownRevision() {
        return shownRevision;
    }

    /**
     * Something a page works out from the plan and the selection, such as a card's tally, kept until either changes,
     * so opening a card or typing in a search doesn't price everything again.
     *
     * @param key     what it is; it must name everything else the value depends on, such as a chosen level
     * @param compute works it out
     *
     * @return the value
     */
    @SuppressWarnings("unchecked")
    <T> T cached(String key, Supplier<T> compute) {
        if ((cachedPlan != plan) || (cachedPlanRevision != plan.revision())
                  || (cachedSelectionRevision != selectionRevision)) {
            cache.clear();
            cachedPlan = plan;
            cachedPlanRevision = plan.revision();
            cachedSelectionRevision = selectionRevision;
        }
        Object value = cache.get(key);
        if (value == null) {
            value = compute.get();
            cache.put(key, value);
        }
        return (T) value;
    }

    // endregion Accessors for the pages

    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(TrainingConsole.class);
            setName("TrainingConsole");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    /** @return a count grouped by thousands, such as "3,412" */
    static String formatCount(int count) {
        return COUNT_FORMAT.format(count);
    }

    private static String upper(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }
}
