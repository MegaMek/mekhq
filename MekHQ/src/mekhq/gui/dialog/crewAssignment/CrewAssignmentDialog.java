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
package mekhq.gui.dialog.crewAssignment;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.function.Function;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Problem;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTech;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTempCrew;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TechStageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TempCrewResult;
import mekhq.campaign.unit.crewAssignment.AutoFill;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService.CommitReport;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.*;
import mekhq.gui.baseComponents.hud.HudFilterList.Item;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.*;
import mekhq.gui.model.LocationFilterItem;

/**
 * The crew assignment dialog: assign people and techs to one location's units, in a heads-up display in the style of
 * the interstellar map.
 *
 * <p>The Assign page shows the location's people on the left and a card per unit on the right, each filtered from the
 * side rail and its own search field. The player ticks people, then presses the button on the seat or tech slot of the
 * unit they should go to, so who goes where is always the player's choice; a mark beside anyone aboard takes them out
 * again, and a full seat offers to replace whoever holds it. The Techs page is the same layout for maintenance alone:
 * the techs, with the time each has left, beside the units and who maintains them. Auto-fill, temporary crew and
 * spreading the selected people over the units, previewed first, work on every unit shown at once. Nothing changes
 * until the player commits:
 * assignments are staged in an {@link AssignmentPlan}, shown on the Review page, can be undone a step at a time, and
 * are applied by {@link CrewAssignmentService}.</p>
 *
 * <p>The dialog is modal, so the campaign cannot change under a staged plan.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CrewAssignmentDialog extends JDialog implements CrewUnitBoard.Actions {
    private static final MMLogger LOGGER = MMLogger.create(CrewAssignmentDialog.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.CrewAssignmentDialog";

    /** Wide enough for most formation names and their counts; longer names are shortened, with a tooltip. */
    private static final int RAIL_WIDTH = 300;
    private static final int PERSON_ROW_HEIGHT = 46;
    private static final int REVIEW_ROW_HEIGHT = 46;

    private final Campaign campaign;
    private final List<LocationFilterItem> locations = new ArrayList<>();
    private LocationFilterItem location;
    private CrewAssignmentData data;
    private final CrewAssignmentState state = new CrewAssignmentState();
    private String message = "";
    private boolean isWarning;
    private boolean closeRequested;
    private int lastPersonIndex = -1;
    /** The page with a list and cards the player was last on, which is where a link on the Review page goes back to. */
    private CrewAssignmentPage lastWorkPage = CrewAssignmentPage.ASSIGN;
    /** The units that pass the filters, in the order their cards are listed; the bulk actions work on these. */
    private List<UnitEntry> shownUnits = List.of();

    private final HudModeSelector<CrewAssignmentPage> modeSelector;
    private final JLabel subtitle = new JLabel();
    private final HudStatTile unassignedTile;
    private final HudStatTile openSeatsTile;
    private final HudStatTile unmannedTile;
    private final HudStatTile pendingTile;

    private final CardLayout railCards = new CardLayout();
    private final JPanel railHolder = new JPanel(railCards);
    /** A combo box rather than a list, so a campaign with many bases does not fill the rail with them. */
    private final JComboBox<LocationFilterItem> locationPicker = new JComboBox<>();
    private final HudFilterList<String> typeFilter = new HudFilterList<>(value -> {
        state.unitType = value;
        refresh();
    });
    private final HudFilterList<String> formationFilter = new HudFilterList<>(value -> {
        state.formation = value;
        refresh();
    });
    private final HudFilterList<Assignment> assignmentFilter = new HudFilterList<>(value -> {
        state.assignment = value;
        refresh();
    });
    private final HudFilterList<RoleFilter> roleFilter = new HudFilterList<>(value -> {
        state.role = value;
        refresh();
    });
    private final HudFilterList<Condition> conditionFilter = new HudFilterList<>(value -> {
        state.condition = value;
        refresh();
    });
    private final HudFilterList<FamiliarityBand> familiarityFilter = new HudFilterList<>(value -> {
        state.familiarity = value;
        refresh();
    });
    /** Names the chassis the familiarity filter measures against. */
    private final JLabel familiarityBasis = new JLabel();

    private final CardLayout pageCards = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageCards);
    private final CrewListPage<PersonEntry> peoplePage;
    private final CrewListPage<CrewRowRenderers.ReviewItem> reviewPage;
    private final HudVerdictBanner problemsBanner = new HudVerdictBanner();
    private final CrewUnitBoard board = new CrewUnitBoard(this);

    // Guides: one per page, sliding over the window from the right
    private final HudCheckBox autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "guide.autoOpen"));
    private final HudGuideDrawer guideDrawer;
    private final HudButton guideButton = new HudButton("", false, true);
    /** Hides or shows the filter rail, for more room for the list and the cards. */
    private final HudButton filtersButton = new HudButton("", false, true);
    /** The page whose guide was last offered, so moving to another page offers that page's guide once. */
    private CrewAssignmentPage guideOfferedFor;

    private final JLabel dockSummary = new JLabel();
    private final JLabel dockMessage = new JLabel();
    private final HudButton undoButton = new HudButton(upper("dock.undo"), false);
    private final HudConfirmButton cancelButton = new HudConfirmButton(upper("dock.cancel"),
          upper("dock.cancel.confirm"), false);
    private final HudButton commitButton = new HudButton(upper("dock.commit"), true);
    private final HudButton closeButton = new HudButton(upper("dock.close"), false);

    // region Opening

    /**
     * Opens the dialog for the location of the given units or people. People are opened already selected, ready to be
     * assigned; units are opened as the only cards listed, until the player clears that filter.
     *
     * @param frame    the main window
     * @param campaign the campaign
     * @param units    the units to list, possibly none, for every unit that needs crew
     * @param people   people to select, possibly none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showFor(JFrame frame, Campaign campaign, Collection<Unit> units, Collection<Person> people) {
        CrewAssignmentDialog dialog = new CrewAssignmentDialog(frame, campaign, units, people);
        dialog.setVisible(true);
    }

    private CrewAssignmentDialog(JFrame frame, Campaign campaign, Collection<Unit> units, Collection<Person> people) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "window.title"), true);
        this.campaign = campaign;

        locations.add(LocationFilterItem.MAIN_FORCE);
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            locations.add(LocationFilterItem.forBase(base));
        }
        ILocation anchor = !units.isEmpty() ? units.iterator().next() :
                                 !people.isEmpty() ? people.iterator().next() : null;
        location = locationOf(anchor);
        data = new CrewAssignmentData(campaign, location);
        buildLocationPicker();

        modeSelector = new HudModeSelector<>(CrewAssignmentPage.class, CrewAssignmentPage::getLabel, this::showPage);
        unassignedTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.unassigned"),
              getTextAt(RESOURCE_BUNDLE, "tile.unassigned.sub"));
        openSeatsTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.openSeats"),
              getTextAt(RESOURCE_BUNDLE, "tile.openSeats.sub"));
        unmannedTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.unmanned"),
              getTextAt(RESOURCE_BUNDLE, "tile.unmanned.sub"));
        pendingTile = new HudStatTile(getTextAt(RESOURCE_BUNDLE, "tile.pending"),
              getTextAt(RESOURCE_BUNDLE, "tile.pending.sub"));

        AssignmentPlan plan = data.plan();
        peoplePage = new CrewListPage<>(getTextAt(RESOURCE_BUNDLE, "personnel.search"), text -> {
            state.peopleQuery = text;
            refresh();
        }, new CrewRowRenderers.PersonRow(plan, state.selectedPeople::contains, this::isTechsPage),
              PERSON_ROW_HEIGHT,
              this::onPersonRow);
        peoplePage.setSelectAll(this::selectShownPeople);
        // A click on a Review row does nothing by itself; its links open the unit or person it names
        reviewPage = new CrewListPage<>(null, text -> {}, new CrewRowRenderers.ReviewRow(plan), REVIEW_ROW_HEIGHT,
              (item, index, isRange) -> {});
        peoplePage.setLinkHandler((entry, link) -> openLink(link));
        reviewPage.setLinkHandler((change, link) -> openLink(link));

        CrewAssignmentGuidePreferences guidePreferences = CrewAssignmentGuidePreferences.getInstance();
        autoOpenGuides.setSelected(guidePreferences.isGuideAutoOpen());
        autoOpenGuides.addActionListener(event -> guidePreferences.setGuideAutoOpen(autoOpenGuides.isSelected()));
        guideDrawer = new HudGuideDrawer(List.of(getTextAt(RESOURCE_BUNDLE, "guide.tab.steps"),
              getTextAt(RESOURCE_BUNDLE, "guide.tab.example"), getTextAt(RESOURCE_BUNDLE, "guide.tab.terms")),
              autoOpenGuides, getTextAt(RESOURCE_BUNDLE, "guide.close"), this::closeGuide);
        guideButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "guide.button.toolTipText"));
        guideButton.addActionListener(event -> openGuide(state.page));
        filtersButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "filters.button.toolTipText"));
        filtersButton.addActionListener(event -> {
            guidePreferences.setFiltersHidden(railHolder.isVisible());
            refresh();
        });

        preselect(units, people);
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
        setMinimumSize(scaleForGUI(1280, 700));
        getContentPane().setPreferredSize(scaleForGUI(1520, 860));
        pack();
        setLocationRelativeTo(frame);
        setPreferences();
    }

    private void buildLocationPicker() {
        for (LocationFilterItem item : locations) {
            locationPicker.addItem(item);
        }
        locationPicker.setSelectedItem(location);
        Hud.styleComboBox(locationPicker);
        locationPicker.getAccessibleContext().setAccessibleName(getTextAt(RESOURCE_BUNDLE, "rail.location"));
        locationPicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, locationPicker.getPreferredSize().height));
        locationPicker.addActionListener(event -> {
            if (locationPicker.getSelectedItem() instanceof LocationFilterItem item) {
                chooseLocation(item);
            }
        });
    }

    /** Finds the location filter entry for the base something is at, or the main force. */
    private LocationFilterItem locationOf(ILocation anchor) {
        AbstractBase base = (anchor == null) ? null : LocationUtils.findEffectiveBase(anchor);
        if (base != null) {
            for (LocationFilterItem item : locations) {
                if (base.equals(item.getBase())) {
                    return item;
                }
            }
        }
        return LocationFilterItem.MAIN_FORCE;
    }

    private void preselect(Collection<Unit> units, Collection<Person> people) {
        for (Person person : people) {
            if (data.entryFor(person) != null) {
                state.selectedPeople.add(person);
            }
        }
        for (Unit unit : units) {
            if (data.entryFor(unit) != null) {
                state.pinnedUnits.add(unit);
            }
        }
        if (!state.pinnedUnits.isEmpty()) {
            // Opened for these units: list only them, however well they are crewed, and show who could fill the
            // first open seat when there is just the one
            state.unitStatus = UnitStatus.ALL;
            Unit first = state.pinnedUnits.iterator().next();
            state.openUnit = first;
            if (state.pinnedUnits.size() == 1) {
                for (CrewSeat seat : data.plan().seatsOf(first)) {
                    if (data.plan().openSeats(first, seat.type()) > 0) {
                        state.qualifiedFor = new SeatFilter(first, seat.type());
                        break;
                    }
                }
            }
        } else if (state.selectedPeople.size() == 1) {
            // Opened for one person: list only the units with a seat they could take
            state.seatsForPerson = state.selectedPeople.iterator().next();
        }
    }

    // endregion Opening

    // region Layout

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(GROUND);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildRail(), BorderLayout.WEST);

        pageHolder.setOpaque(false);
        JPanel work = new JPanel(new BorderLayout());
        work.setOpaque(false);
        work.add(peoplePage, BorderLayout.CENTER);
        work.add(board, BorderLayout.EAST);
        pageHolder.add(work, CrewAssignmentPage.ASSIGN.name());
        JPanel review = new JPanel(new BorderLayout());
        review.setOpaque(false);
        problemsBanner.setVisible(false);
        review.add(problemsBanner, BorderLayout.NORTH);
        review.add(reviewPage, BorderLayout.CENTER);
        pageHolder.add(review, CrewAssignmentPage.REVIEW.name());
        body.add(pageHolder, BorderLayout.CENTER);

        content.add(buildCommandBar(), BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(buildDock(), BorderLayout.SOUTH);
        setContentPane(content);
        // The guide drawer and its scrim float over the window; Escape closes the guide
        guideDrawer.install(getRootPane());
    }

    private JComponent buildCommandBar() {
        JPanel identity = column();
        JLabel eyebrow = Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "header.eyebrow"));
        JLabel title = new JLabel(upper("header.title"));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.3f, 0.16f));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        identity.add(leftAligned(eyebrow));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(title));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(leftAligned(subtitle));

        JPanel topRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(24), 0));
        topRow.add(identity, BorderLayout.CENTER);
        topRow.add(Hud.tileRow(unassignedTile, openSeatsTile, unmannedTile, pendingTile), BorderLayout.EAST);

        JPanel bar = new JPanel(new BorderLayout(0, scaleForGUI(12)));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        bar.add(topRow, BorderLayout.CENTER);
        JPanel modesRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        modesRow.add(modeSelector, BorderLayout.CENTER);
        // Far left, above the filter rail it hides
        modesRow.add(filtersButton, BorderLayout.WEST);
        modesRow.add(guideButton, BorderLayout.EAST);
        bar.add(modesRow, BorderLayout.SOUTH);
        return bar;
    }

    private JComponent buildRail() {
        // One rail for the Assign page: where, then who is listed, then which units have a card
        JPanel rail = column();
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.location"))));
        rail.add(leftAligned(locationPicker));
        rail.add(Box.createVerticalStrut(scaleForGUI(12)));
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.assignment"))));
        rail.add(leftAligned(assignmentFilter));
        rail.add(Box.createVerticalStrut(scaleForGUI(12)));
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.role"))));
        rail.add(leftAligned(roleFilter));
        rail.add(Box.createVerticalStrut(scaleForGUI(12)));
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.condition"))));
        rail.add(leftAligned(conditionFilter));
        if (data.familiarityMode().isEnabled()) {
            familiarityBasis.setForeground(TEXT_MUTED);
            familiarityBasis.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
            rail.add(Box.createVerticalStrut(scaleForGUI(12)));
            rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.familiarity"))));
            rail.add(leftAligned(familiarityBasis));
            rail.add(Box.createVerticalStrut(scaleForGUI(4)));
            rail.add(leftAligned(familiarityFilter));
        }
        rail.add(Box.createVerticalStrut(scaleForGUI(12)));
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.unitType"))));
        rail.add(leftAligned(typeFilter));
        rail.add(Box.createVerticalStrut(scaleForGUI(12)));
        rail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.formation"))));
        rail.add(leftAligned(formationFilter));

        railHolder.setOpaque(false);
        railHolder.add(railScroll(rail), CrewAssignmentPage.ASSIGN.name());
        railHolder.add(railScroll(column()), CrewAssignmentPage.REVIEW.name());
        railHolder.setPreferredSize(scaleForGUI(RAIL_WIDTH, 0));
        railHolder.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        return railHolder;
    }

    private static JScrollPane railScroll(JPanel rail) {
        rail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(12),
              scaleForGUI(12)));
        // The holder follows the rail's width, so long formation names are shortened rather than pushing the counts
        // out of sight, and keeps the filters at their natural height at the top
        JScrollablePanel holder = new JScrollablePanel();
        holder.setLayout(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(GROUND);
        holder.add(rail, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, GROUND, false);
        return scroll;
    }

    private JComponent buildDock() {
        dockSummary.setForeground(TEXT);
        dockSummary.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        dockMessage.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        JPanel text = Hud.transparentPanel(null);
        text.setLayout(new BoxLayout(text, BoxLayout.X_AXIS));
        text.add(dockSummary);
        text.add(Box.createHorizontalStrut(scaleForGUI(12)));
        text.add(dockMessage);

        undoButton.addActionListener(event -> {
            if (data.plan().undo()) {
                setMessage(getTextAt(RESOURCE_BUNDLE, "message.undone"), false);
            }
            refresh();
        });
        cancelButton.addActionListener(event -> {
            int count = data.plan().size();
            data.plan().clear();
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.discarded", count), false);
            refresh();
        });
        commitButton.addActionListener(event -> commit());
        closeButton.addActionListener(event -> requestClose());

        JPanel buttons = Hud.transparentPanel(null);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        for (JComponent button : List.of(undoButton, cancelButton, commitButton, closeButton)) {
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

    // endregion Layout

    // region Refreshing

    /** Redraws everything from the state and the plan. */
    private void refresh() {
        AssignmentPlan plan = data.plan();
        modeSelector.setSelected(state.page);
        if (state.page != guideOfferedFor) {
            // Whatever moved the player to another page (a tab, a link, a "Find" action), that page's guide is offered
            closeGuide();
            guideOfferedFor = state.page;
            maybeOpenGuide(state.page);
        }
        guideButton.setText("?  " + getFormattedTextAt(RESOURCE_BUNDLE, "guide.button", state.page.getLabel())
                                             .toUpperCase(Locale.ROOT));
        // The Assign and Techs pages are one layout, and one rail, showing different things
        String card = state.page.isWorkPage() ? CrewAssignmentPage.ASSIGN.name() : state.page.name();
        pageCards.show(pageHolder, card);
        railCards.show(railHolder, card);
        refreshFiltersButton();
        subtitle.setText(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", data.locationName(),
              campaign.getLocalDate()));

        switch (state.page) {
            case ASSIGN, TECHS -> {
                lastWorkPage = state.page;
                refreshPeople(plan);
                refreshUnits(plan);
            }
            case REVIEW -> refreshReviewPage(plan);
        }
        refreshTiles(plan);
        refreshDock(plan);
    }

    private void refreshUnits(AssignmentPlan plan) {
        List<UnitEntry> shown = new ArrayList<>();
        Map<String, Integer> byType = new TreeMap<>();
        Map<String, Integer> byFormation = new TreeMap<>();
        int statusMatches = 0;
        for (UnitEntry entry : data.units()) {
            if (state.matchesStatus(entry, plan)) {
                statusMatches++;
                byType.merge(entry.typeName(), 1, Integer::sum);
                if (!entry.formationName().isEmpty()) {
                    byFormation.merge(entry.formationName(), 1, Integer::sum);
                }
            }
            if (state.shows(entry, plan)) {
                shown.add(entry);
            }
        }
        shownUnits = List.copyOf(shown);

        typeFilter.setItems(withAll("rail.allTypes", statusMatches, byType));
        typeFilter.setSelected(state.unitType);
        formationFilter.setItems(withAll("rail.allFormations", statusMatches, byFormation));
        formationFilter.setSelected(state.formation);

        List<JComponent> chips = new ArrayList<>();
        if (isTechsPage()) {
            for (TechStatus status : TechStatus.values()) {
                HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "TechStatus." + status.name()), () -> {
                    state.techStatus = status;
                    refresh();
                });
                chip.setActive(state.techStatus == status);
                chips.add(chip);
            }
        } else {
            for (UnitStatus status : UnitStatus.values()) {
                HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "UnitStatus." + status.name()), () -> {
                    state.unitStatus = status;
                    refresh();
                });
                chip.setActive(state.unitStatus == status);
                chips.add(chip);
            }
        }
        if (!state.pinnedUnits.isEmpty()) {
            HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "units.pinned",
                  state.pinnedUnits.size()), () -> {
                state.pinnedUnits.clear();
                refresh();
            });
            chip.setActive(true);
            chips.add(chip);
        }
        // With one person selected, the cards can be narrowed to where that person could go
        Person only = (state.selectedPeople.size() == 1) ? state.selectedPeople.iterator().next() : null;
        if ((only != null) && (state.seatsForPerson == null) && (state.unitsForTech == null)) {
            if (!isTechsPage()) {
                chips.add(new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "units.seatsFor.offer",
                      only.getFullTitle()), () -> {
                    state.seatsForPerson = only;
                    refresh();
                }));
            }
            if (only.isTech()) {
                chips.add(new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "units.forTech.offer",
                      only.getFullTitle()), () -> {
                    state.unitsForTech = only;
                    refresh();
                }));
            }
        }
        if ((state.seatsForPerson != null) && !isTechsPage()) {
            HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "units.seatsFor",
                  state.seatsForPerson.getFullTitle()), () -> {
                state.seatsForPerson = null;
                refresh();
            });
            chip.setActive(true);
            chips.add(chip);
        }
        if (state.unitsForTech != null) {
            HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "units.forTech",
                  state.unitsForTech.getFullTitle()), () -> {
                state.unitsForTech = null;
                refresh();
            });
            chip.setActive(true);
            chips.add(chip);
        }
        board.refresh(state, data, shown, chips);
    }

    private void refreshPeople(AssignmentPlan plan) {
        List<PersonEntry> shown = new ArrayList<>();
        Map<PersonnelRole, Integer> byRole = new TreeMap<>();
        Map<RoleGroup, Integer> byRoleGroup = new EnumMap<>(RoleGroup.class);
        int unassigned = 0;
        int listed = 0;
        int roleTotal = 0;
        Map<Condition, Integer> byCondition = new EnumMap<>(Condition.class);
        Map<FamiliarityBand, Integer> byFamiliarity = new EnumMap<>(FamiliarityBand.class);
        Familiarity familiarityMode = data.familiarityMode();
        String familiarityChassis = state.familiarityChassis();
        List<PersonEntry> people = data.people();
        for (PersonEntry entry : people) {
            Person person = entry.person();
            if (!state.lists(person)) {
                // The Techs page lists only techs
                continue;
            }
            listed++;
            // Counted the way the list filters, so the rail's counts match the rows shown
            boolean isAssigned = state.countsAsAssigned(person, plan);
            if (!isAssigned) {
                unassigned++;
            }
            if ((state.assignment == Assignment.EVERYONE)
                      || ((state.assignment == Assignment.ASSIGNED) == isAssigned)) {
                roleTotal++;
                for (PersonnelRole role : List.of(person.getPrimaryRole(), person.getSecondaryRole())) {
                    if (CrewAssignmentState.isFilterableRole(role)) {
                        byRole.merge(role, 1, Integer::sum);
                    }
                }
                for (RoleGroup group : RoleGroup.values()) {
                    if (CrewAssignmentState.matches(person, group)) {
                        byRoleGroup.merge(group, 1, Integer::sum);
                    }
                }
                for (Condition condition : Condition.values()) {
                    if (CrewAssignmentState.matches(entry, condition)) {
                        byCondition.merge(condition, 1, Integer::sum);
                    }
                }
                if (familiarityMode.isEnabled()) {
                    int score = CrewAssignmentState.familiarityScore(person, familiarityChassis);
                    for (FamiliarityBand band : FamiliarityBand.values()) {
                        if (CrewAssignmentState.matches(score, band)) {
                            byFamiliarity.merge(band, 1, Integer::sum);
                        }
                    }
                }
            }
            if (state.shows(entry, plan)) {
                shown.add(entry);
            }
        }
        if (isTechsPage()) {
            // The list arrives best first; the other orders keep that order among equals
            switch (state.techSort) {
                case BEST -> {
                }
                case TIME_FREE -> shown.sort(Comparator.comparingInt((PersonEntry entry) ->
                      plan.techMinutesAvailable(entry.person()) - plan.plannedTechMinutes(entry.person())).reversed());
                case FEWEST_UNITS -> shown.sort(Comparator.comparingInt((PersonEntry entry) ->
                      plan.plannedTechUnits(entry.person()).size()));
            }
        }
        peoplePage.setRows(shown);
        peoplePage.setCount(getFormattedTextAt(RESOURCE_BUNDLE, isTechsPage() ? "personnel.count.techs" :
                                                                      "personnel.count", shown.size(),
              state.selectedPeople.size()));
        List<JComponent> links = new ArrayList<>();
        // Taking the selected people out of their units and off the units they maintain
        boolean anyPlaced = state.selectedPeople.stream()
                                  .anyMatch(person -> plan.canUnassign(person)
                                                            || !plan.plannedTechUnits(person).isEmpty());
        if (anyPlaced) {
            JLabel unassign = Hud.link(getTextAt(RESOURCE_BUNDLE, "people.unassign"), this::unassignSelectedPeople);
            unassign.setToolTipText(getTextAt(RESOURCE_BUNDLE, "people.unassign.toolTipText"));
            links.add(unassign);
        }
        links.add(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "people.selectAll", shown.size()),
              this::selectShownPeople));
        if (!state.selectedPeople.isEmpty()) {
            links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "people.clear"), this::clearSelection));
        }
        peoplePage.setCountLinks(links);

        assignmentFilter.setItems(List.of(
              new Item<>(Assignment.UNASSIGNED, getTextAt(RESOURCE_BUNDLE, "Assignment.UNASSIGNED"), unassigned),
              new Item<>(Assignment.ASSIGNED, getTextAt(RESOURCE_BUNDLE, "Assignment.ASSIGNED"),
                    listed - unassigned),
              new Item<>(Assignment.EVERYONE, getTextAt(RESOURCE_BUNDLE, "Assignment.EVERYONE"), listed)));
        assignmentFilter.setSelected(state.assignment);
        List<Item<RoleFilter>> roles = new ArrayList<>();
        roles.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, "rail.allRoles"), roleTotal));
        // The groups are always offered, so the player can see there are none rather than wonder where they went
        for (RoleGroup group : RoleGroup.values()) {
            roles.add(new Item<>(group, getTextAt(RESOURCE_BUNDLE, "RoleGroup." + group.name()),
                  byRoleGroup.getOrDefault(group, 0)));
        }
        byRole.forEach((role, count) -> roles.add(new Item<>(new SingleRole(role), role.getLabel(false), count)));
        roleFilter.setItems(roles);
        roleFilter.setSelected(state.role);
        List<Item<Condition>> conditions = new ArrayList<>();
        for (Condition condition : Condition.values()) {
            conditions.add(new Item<>(condition, getTextAt(RESOURCE_BUNDLE, "Condition." + condition.name()),
                  byCondition.getOrDefault(condition, 0)));
        }
        conditionFilter.setItems(conditions);
        conditionFilter.setSelected(state.condition);
        if (familiarityMode.isEnabled()) {
            refreshFamiliarityFilter(familiarityMode, familiarityChassis, byFamiliarity);
        }

        List<JComponent> chips = new ArrayList<>();
        if (isTechsPage()) {
            for (TechSort sort : TechSort.values()) {
                HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "TechSort." + sort.name()), () -> {
                    state.techSort = sort;
                    refresh();
                });
                chip.setActive(state.techSort == sort);
                chips.add(chip);
            }
        }
        if ((state.qualifiedFor != null) && !isTechsPage()) {
            SeatFilter filter = state.qualifiedFor;
            HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "personnel.qualifiedFor",
                  CrewRowRenderers.seatLabel(filter.unit(), filter.seat()), filter.unit().getName()), () -> {
                state.qualifiedFor = null;
                refresh();
            });
            chip.setActive(true);
            chips.add(chip);
        }
        if (state.techFor != null) {
            HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "personnel.techFor",
                  state.techFor.getName()), () -> {
                state.techFor = null;
                refresh();
            });
            chip.setActive(true);
            chips.add(chip);
        }
        peoplePage.setChips(chips);
    }

    /**
     * Shows or hides the filter rail as the player last chose. While it is hidden, the button counts the page's rail
     * filters still narrowing the list, so a hidden filter cannot quietly leave someone out.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void refreshFiltersButton() {
        boolean isHidden = CrewAssignmentGuidePreferences.getInstance().isFiltersHidden();
        if (railHolder.isVisible() == isHidden) {
            railHolder.setVisible(!isHidden);
            getContentPane().revalidate();
            getContentPane().repaint();
        }
        String text;
        if (!isHidden) {
            text = getTextAt(RESOURCE_BUNDLE, "filters.hide");
        } else {
            int active = state.activeRailFilters(state.page);
            text = (active == 0) ? getTextAt(RESOURCE_BUNDLE, "filters.show") :
                         getFormattedTextAt(RESOURCE_BUNDLE, "filters.show.active", active);
        }
        filtersButton.setText(text.toUpperCase(Locale.ROOT));
    }

    /**
     * Fills the familiarity filter, offering only the bands the mode's cap can reach, and names the chassis it
     * measures against.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void refreshFamiliarityFilter(Familiarity familiarityMode, @Nullable String familiarityChassis,
          Map<FamiliarityBand, Integer> byFamiliarity) {
        familiarityBasis.setText((familiarityChassis == null) ?
                                       getTextAt(RESOURCE_BUNDLE, "rail.familiarity.best") :
                                       getFormattedTextAt(RESOURCE_BUNDLE, "rail.familiarity.chassis",
                                             familiarityChassis));
        List<Item<FamiliarityBand>> bands = new ArrayList<>();
        for (FamiliarityBand band : FamiliarityBand.values()) {
            if (band.getThreshold() <= familiarityMode.getFamiliarityCap()) {
                bands.add(new Item<>(band, getTextAt(RESOURCE_BUNDLE, "FamiliarityBand." + band.name()),
                      byFamiliarity.getOrDefault(band, 0)));
            }
        }
        familiarityFilter.setItems(bands);
        familiarityFilter.setSelected(state.familiarity);
    }

    private void refreshReviewPage(AssignmentPlan plan) {
        // Crew moves first, then techs, then temporary crew: the order they read best in, not the order they apply in
        List<CrewRowRenderers.ReviewItem> items = new ArrayList<>();
        plan.changes().forEach(change -> items.add(new CrewRowRenderers.CrewItem(change)));
        plan.techChanges().forEach(change -> items.add(new CrewRowRenderers.TechItem(change)));
        plan.tempCrewChanges().forEach(change -> items.add(new CrewRowRenderers.TempCrewItem(change)));
        reviewPage.setRows(items);
        long reassignments = plan.changes().stream().filter(StagedAssignment::isReassignment).count();
        reviewPage.setCount(items.isEmpty() ? getTextAt(RESOURCE_BUNDLE, "review.empty") :
                                  getFormattedTextAt(RESOURCE_BUNDLE, "review.count", items.size(),
                                        reassignments));
        int problems = plan.validate().size() + plan.validateTechs().size() + plan.validateTempCrew().size();
        problemsBanner.setVisible(problems > 0);
        if (problems > 0) {
            problemsBanner.setVerdict(getTextAt(RESOURCE_BUNDLE, "review.problems.verdict"),
                  getFormattedTextAt(RESOURCE_BUNDLE, "review.problems.reason", problems),
                  getTextAt(RESOURCE_BUNDLE, "review.problems.badge"), DANGER);
        }
    }

    private <T> List<Item<T>> withAll(String allKey, int total, Map<T, Integer> counts) {
        List<Item<T>> items = new ArrayList<>();
        items.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, allKey), total));
        counts.forEach((value, count) -> items.add(new Item<>(value, String.valueOf(value), count)));
        return items;
    }

    private void refreshTiles(AssignmentPlan plan) {
        int unassigned = 0;
        for (PersonEntry entry : data.people()) {
            // The tile counts combat personnel only, not the techs and other staff the page also lists
            Person person = entry.person();
            boolean isCombat = person.getPrimaryRole().isCombat() || person.getSecondaryRole().isCombat();
            if (isCombat && (plan.plannedUnitOf(person) == null)) {
                unassigned++;
            }
        }
        int open = 0;
        int unmanned = 0;
        for (UnitEntry entry : data.units()) {
            // Places temporary crew hold are crewed, so they are not counted as open
            CrewCoverage.UnitCoverage coverage = CrewCoverage.of(entry.unit(), plan);
            open += coverage.uncovered();
            if (coverage.crewing() == Crewing.EMPTY) {
                unmanned++;
            }
        }
        unassignedTile.setValue(String.format(Locale.ROOT, "%,d", unassigned), TEXT);
        openSeatsTile.setValue(String.format(Locale.ROOT, "%,d", open), AMBER);
        unmannedTile.setValue(String.format(Locale.ROOT, "%,d", unmanned), DANGER);
        pendingTile.setValue(String.format(Locale.ROOT, "%,d", plan.size()), ACCENT_BRIGHT);
    }

    private void refreshDock(AssignmentPlan plan) {
        List<StagedAssignment> changes = plan.changes();
        int pending = plan.size();
        if (pending == 0) {
            dockSummary.setText(getTextAt(RESOURCE_BUNDLE, "dock.none"));
        } else {
            Set<Unit> units = new HashSet<>();
            for (StagedAssignment change : changes) {
                units.add((change.toUnit() != null) ? change.toUnit() : change.fromUnit());
            }
            plan.techChanges().forEach(change -> units.add(change.unit()));
            plan.tempCrewChanges().forEach(change -> units.add(change.unit()));
            units.remove(null);
            long reassignments = changes.stream().filter(StagedAssignment::isReassignment).count();
            dockSummary.setText(getFormattedTextAt(RESOURCE_BUNDLE, "dock.summary", pending, units.size(),
                  reassignments));
        }
        dockMessage.setText(message);
        dockMessage.setForeground(isWarning ? AMBER : TEXT_FAINT);
        modeSelector.setModeLabel(CrewAssignmentPage.REVIEW, (pending == 0) ?
                                                               CrewAssignmentPage.REVIEW.getLabel() :
                                                               getFormattedTextAt(RESOURCE_BUNDLE,
                                                                     "CrewAssignmentPage.REVIEW.count", pending));
        undoButton.setArmed(plan.canUndo());
        cancelButton.setArmed(pending > 0);
        commitButton.setArmed(pending > 0);
    }

    private void setMessage(String text, boolean isWarning) {
        this.message = text;
        this.isWarning = isWarning;
        closeRequested = false;
    }

    // endregion Refreshing

    // region Rows and links

    private void onPersonRow(PersonEntry entry, int index, boolean isRange) {
        List<PersonEntry> rows = peoplePage.rows();
        toggle(rows, entry, index, isRange, lastPersonIndex, PersonEntry::person, state.selectedPeople);
        if (!isRange) {
            lastPersonIndex = index;
        }
        refresh();
    }

    /** Adds everyone the list shows to the selection. */
    private void selectShownPeople() {
        for (PersonEntry entry : peoplePage.rows()) {
            state.selectedPeople.add(entry.person());
        }
        refresh();
    }

    /** A click toggles one row; a shift-click selects every row from the last one clicked. */
    private static <R, V> void toggle(List<R> rows, R row, int index, boolean isRange, int lastIndex,
          Function<R, V> valueOf, Collection<V> selection) {
        if (isRange && (lastIndex >= 0) && (lastIndex < rows.size())) {
            for (int i = Math.min(index, lastIndex); i <= Math.max(index, lastIndex); i++) {
                selection.add(valueOf.apply(rows.get(i)));
            }
        } else {
            V value = valueOf.apply(row);
            if (!selection.remove(value)) {
                selection.add(value);
            }
        }
    }

    private void openLink(Object link) {
        if (link instanceof Unit unit) {
            openUnit(unit);
        } else if (link instanceof Person person) {
            openPerson(person);
        } else if (link instanceof CrewRowRenderers.RemoveLink removeLink) {
            AssignmentPlan plan = data.plan();
            switch (removeLink.item()) {
                case CrewRowRenderers.CrewItem item -> {
                    Person person = item.change().person();
                    if (plan.remove(person)) {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removed", person.getFullTitle()),
                              false);
                    } else {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removeRefused",
                              person.getFullTitle()), true);
                    }
                }
                case CrewRowRenderers.TechItem item -> {
                    Unit unit = item.change().unit();
                    if (plan.removeTechChange(unit)) {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removed", unit.getName()), false);
                    } else {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removeTechRefused",
                              unit.getName()), true);
                    }
                }
                case CrewRowRenderers.TempCrewItem item -> {
                    Unit unit = item.change().unit();
                    if (plan.removeTempCrewChange(unit, data.tempCrewPool())) {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removed", unit.getName()), false);
                    } else {
                        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removeTempCrewRefused",
                              unit.getName()), true);
                    }
                }
            }
            refresh();
        }
    }

    // endregion Rows and links

    // region Board actions

    private void showPage(CrewAssignmentPage page) {
        enterPage(page);
        refresh();
    }

    private boolean isTechsPage() {
        return state.page == CrewAssignmentPage.TECHS;
    }

    /**
     * Moves to a page. Going between the Assign and Techs pages starts the filters that mean something different on
     * each of them over, so a role or a "who fits" filter from one page cannot quietly empty the other; the people
     * ticked stay ticked. Coming back from the Review page changes nothing.
     */
    private void enterPage(CrewAssignmentPage page) {
        if (page.isWorkPage() && (page != lastWorkPage)) {
            state.assignment = CrewAssignmentState.defaultAssignment(page);
            state.role = null;
            state.qualifiedFor = null;
            state.techFor = null;
            state.seatsForPerson = null;
            state.unitsForTech = null;
            state.targetTech = false;
            state.isSpreadPreview = false;
            lastPersonIndex = -1;
            lastWorkPage = page;
        }
        state.page = page;
    }

    @Override
    public void searchUnits(String text) {
        if (!text.equals(state.unitQuery)) {
            state.unitQuery = text;
            refresh();
        }
    }

    @Override
    public void assignToSeat(Unit unit, SeatType seat) {
        AssignmentPlan plan = data.plan();
        // In the order they were selected, so the first selected are the first seated; one step of history
        List<Person> candidates = new ArrayList<>();
        for (Person person : state.selectedPeople) {
            if (!unit.equals(plan.plannedUnitOf(person))) {
                candidates.add(person);
            }
        }
        plan.fillSeats(candidates, List.of(unit), seat);
        // Asked of the plan rather than read from what it staged: going back to a seat held now cancels a change
        List<Person> placed = new ArrayList<>();
        for (Person person : candidates) {
            if (unit.equals(plan.plannedUnitOf(person))) {
                placed.add(person);
            }
        }

        String seatName = CrewRowRenderers.seatLabel(unit, seat);
        if (placed.isEmpty()) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.seatRefused", seatName, unit.getName()), true);
        } else {
            // Those placed are done; anyone left stays selected, ready for the next seat or unit
            placed.forEach(state.selectedPeople::remove);
            if (placed.contains(state.seatsForPerson)) {
                state.seatsForPerson = null;
            }
            int left = state.selectedPeople.size();
            setMessage((left == 0) ?
                             getFormattedTextAt(RESOURCE_BUNDLE, "message.seatStaged", placed.size(), seatName,
                                   unit.getName()) :
                             getFormattedTextAt(RESOURCE_BUNDLE, "message.seatStagedSome", placed.size(), seatName,
                                   unit.getName(), left), false);
        }
        refresh();
    }

    @Override
    public void assignTech(Unit unit) {
        AssignmentPlan plan = data.plan();
        Person current = plan.plannedTechOf(unit);
        // The first selected tech who can take the unit on; they stay selected, as a tech can take several units
        for (Person person : List.copyOf(state.selectedPeople)) {
            if (person.isTech() && !person.equals(current) && plan.stageTech(person, unit)) {
                setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.techStaged", person.getFullTitle(),
                      unit.getName()), false);
                refresh();
                return;
            }
        }
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.techRefused", unit.getName()), true);
        refresh();
    }

    @Override
    public void removeFromUnit(Person person) {
        AssignmentPlan plan = data.plan();
        Unit unit = plan.plannedUnitOf(person);
        if ((unit != null) && plan.unassign(List.of(person)).leftOver().isEmpty()) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removedFrom", person.getFullTitle(),
                  unit.getName()), false);
        } else {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.cannotRemove", person.getFullTitle()), true);
        }
        refresh();
    }

    @Override
    public void removeTech(Unit unit) {
        AssignmentPlan plan = data.plan();
        plan.unassignTechs(List.of(unit));
        boolean removed = plan.plannedTechOf(unit) == null;
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, removed ? "message.techRemoved" : "message.notHere",
              unit.getName()), !removed);
        refresh();
    }

    @Override
    public void removeCrew(Unit unit) {
        int staged = data.plan().clearUnits(List.of(unit));
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.unassigned", staged), staged == 0);
        refresh();
    }

    @Override
    public void autoFill() {
        List<Person> candidates = new ArrayList<>();
        for (PersonEntry entry : data.people()) {
            if (CrewAssignmentData.isAutoFillCandidate(entry.person())) {
                candidates.add(entry.person());
            }
        }
        if (state.targetTech) {
            TechStageResult result = AutoFill.fillTechs(data.plan(), shownUnitList(), candidates);
            reportStaged(result.staged().size(), 0);
            if (!result.leftOver().isEmpty()) {
                setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.techsLeftOver", result.staged().size(),
                      result.leftOver().size()), true);
            }
        } else if (state.targetSeat != null) {
            StageResult result = AutoFill.fill(data.plan(), shownUnitList(), state.targetSeat, candidates,
                  data.ranking());
            reportStaged(result.staged().size(), result.leftOver().size());
        }
        refresh();
    }

    private void reportStaged(int staged, int leftOver) {
        if (staged == 0) {
            setMessage(getTextAt(RESOURCE_BUNDLE, "message.nothingStaged"), true);
        } else if (leftOver > 0) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.stagedLeftOver", staged, leftOver), true);
        } else {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.staged", staged), false);
        }
    }

    @Override
    public void selectOnly(List<Person> people) {
        state.selectedPeople.clear();
        state.selectedPeople.addAll(people);
        lastPersonIndex = -1;
        refresh();
    }

    @Override
    public void replaceOccupant(Person newcomer, Person occupant) {
        AssignmentPlan plan = data.plan();
        Unit unit = plan.plannedUnitOf(occupant);
        if ((unit != null) && plan.stageReplacement(newcomer, occupant)) {
            state.selectedPeople.remove(newcomer);
            if (newcomer.equals(state.seatsForPerson)) {
                state.seatsForPerson = null;
            }
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.replaced", occupant.getFullTitle(),
                  newcomer.getFullTitle(), unit.getName()), false);
        } else {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.replaceRefused", occupant.getFullTitle()), true);
        }
        refresh();
    }

    @Override
    public void toggleSpreadPreview() {
        state.isSpreadPreview = !state.isSpreadPreview;
        refresh();
    }

    @Override
    public void spreadSelected() {
        AssignmentPlan plan = data.plan();
        // In the order they were selected, over the units in the order their cards are listed: what the preview showed
        List<Person> people = List.copyOf(state.selectedPeople);
        if (isTechsPage()) {
            TechStageResult result = plan.stageTechs(people, shownUnitList());
            if (result.staged().isEmpty()) {
                setMessage(getTextAt(RESOURCE_BUNDLE, "message.spreadNone"), true);
            } else {
                // A tech given a unit is done with; one given none stays selected
                for (StagedTech change : result.staged()) {
                    state.selectedPeople.remove(change.toTech());
                }
                if (result.leftOver().isEmpty()) {
                    reportStaged(result.staged().size(), 0);
                } else {
                    setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.techsLeftOver",
                          result.staged().size(), result.leftOver().size()), true);
                }
            }
        } else {
            StageResult result = plan.stage(people, shownUnitList());
            if (result.leftOver().size() == people.size()) {
                setMessage(getTextAt(RESOURCE_BUNDLE, "message.spreadNone"), true);
            } else {
                state.selectedPeople.retainAll(result.leftOver());
                state.seatsForPerson = null;
                reportStaged(people.size() - result.leftOver().size(), result.leftOver().size());
            }
        }
        state.isSpreadPreview = false;
        refresh();
    }

    @Override
    public void clearSelection() {
        state.isSpreadPreview = false;
        state.selectedPeople.clear();
        lastPersonIndex = -1;
        refresh();
    }

    @Override
    public void chooseTargetSeat(SeatType seat) {
        state.targetSeat = seat;
        state.targetTech = false;
        refresh();
    }

    @Override
    public void chooseTargetTech() {
        state.targetTech = true;
        refresh();
    }

    /** Takes the selected people out of their units and off every unit they maintain. */
    private void unassignSelectedPeople() {
        int staged = data.plan().unassignEverything(sortedSelectedPeople());
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.unassigned", staged), staged == 0);
        refresh();
    }

    @Override
    public void fillTempCrewOn(Unit unit) {
        fillTempCrew(List.of(unit));
    }

    @Override
    public void fillTempCrewOnShown() {
        fillTempCrew(shownUnitList());
    }

    private void fillTempCrew(List<Unit> units) {
        TempCrewResult result = data.plan().fillTempCrew(units, data.tempCrewPool());
        int filled = result.staged().size();
        int added = result.staged().stream().mapToInt(StagedTempCrew::change).sum();
        if (filled == 0) {
            setMessage(getTextAt(RESOURCE_BUNDLE, "message.tempCrewNone"), true);
        } else if (!result.leftOver().isEmpty() && (units.size() == 1)) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewFilledSome", added, filled,
                  result.leftOver().size()), true);
        } else {
            // Across every unit shown, the units with nothing to fill are expected, not a warning
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewFilled", added, filled), false);
        }
        refresh();
    }

    @Override
    public void removeTempCrewOn(Unit unit) {
        removeTempCrew(List.of(unit));
    }

    @Override
    public void removeTempCrewOnShown() {
        removeTempCrew(shownUnitList());
    }

    private void removeTempCrew(List<Unit> units) {
        TempCrewResult result = data.plan().removeTempCrew(units);
        int removed = -result.staged().stream().mapToInt(StagedTempCrew::change).sum();
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewRemoved", removed,
              result.staged().size()), result.staged().isEmpty());
        refresh();
    }

    @Override
    public void findTechFor(Unit unit) {
        // The list narrows to the techs who could take the unit on; the player ticks one and presses its button
        state.techFor = unit;
        state.qualifiedFor = null;
        state.assignment = Assignment.EVERYONE;
        state.role = null;
        state.peopleQuery = "";
        peoplePage.setSearchText("");
        refresh();
    }

    @Override
    public void findPeopleFor(Unit unit, SeatType seat) {
        // The list narrows to the free people who could take the seat; the player ticks them and presses its button
        state.qualifiedFor = new SeatFilter(unit, seat);
        state.techFor = null;
        state.assignment = Assignment.UNASSIGNED;
        state.role = null;
        state.peopleQuery = "";
        peoplePage.setSearchText("");
        refresh();
    }

    @Override
    public void toggleUnit(Unit unit) {
        state.openUnit = unit.equals(state.openUnit) ? null : unit;
        refresh();
    }

    /** Shows a unit's card, open, clearing the unit filters if they hide it. */
    private void openUnit(Unit unit) {
        UnitEntry entry = data.entryFor(unit);
        if (entry == null) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.notHere", unit.getName()), true);
            refresh();
            return;
        }
        // Stay on the Techs page for a unit a tech can maintain; anything else only has a card on the Assign page
        enterPage(((lastWorkPage == CrewAssignmentPage.TECHS) && SeatEligibility.takesTech(unit)) ?
                        CrewAssignmentPage.TECHS : CrewAssignmentPage.ASSIGN);
        if (!state.shows(entry, data.plan())) {
            state.unitStatus = UnitStatus.ALL;
            state.techStatus = TechStatus.ALL;
            state.unitType = null;
            state.formation = null;
            state.seatsForPerson = null;
            state.unitsForTech = null;
            state.pinnedUnits.clear();
            state.unitQuery = "";
            board.setSearchText("");
        }
        state.openUnit = unit;
        refresh();
        board.reveal(unit);
    }

    @Override
    public void openPerson(Person person) {
        PersonEntry entry = data.entryFor(person);
        if (entry == null) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.notHere", person.getFullTitle()), true);
            refresh();
            return;
        }
        // Stay on the Techs page for a tech; anyone else is only listed on the Assign page
        enterPage(((lastWorkPage == CrewAssignmentPage.TECHS) && person.isTech()) ?
                        CrewAssignmentPage.TECHS : CrewAssignmentPage.ASSIGN);
        if (!state.shows(entry, data.plan())) {
            state.assignment = Assignment.EVERYONE;
            state.role = null;
            state.condition = Condition.ANY;
            state.familiarity = FamiliarityBand.ANY;
            state.qualifiedFor = null;
            state.techFor = null;
            state.peopleQuery = "";
            peoplePage.setSearchText("");
        }
        refresh();
        peoplePage.reveal(entry);
    }

    /** @return the units whose cards are listed, in order */
    private List<Unit> shownUnitList() {
        List<Unit> units = new ArrayList<>(shownUnits.size());
        for (UnitEntry entry : shownUnits) {
            units.add(entry.unit());
        }
        return units;
    }

    /** @return the selected people, best first */
    private List<Person> sortedSelectedPeople() {
        List<Person> sorted = new ArrayList<>(state.selectedPeople);
        sorted.sort(data.ranking());
        return sorted;
    }

    // endregion Board actions

    // region Guides

    /** Opens a page's guide the first time the player visits that page, unless they have turned that off. */
    private void maybeOpenGuide(CrewAssignmentPage page) {
        if (!isShowing()) {
            return;
        }
        CrewAssignmentGuidePreferences preferences = CrewAssignmentGuidePreferences.getInstance();
        if (preferences.isGuideAutoOpen() && !preferences.isGuideSeen(page)) {
            openGuide(page);
        }
    }

    private void openGuide(CrewAssignmentPage page) {
        CrewAssignmentGuidePreferences.getInstance().setGuideSeen(page, true);
        guideDrawer.open(CrewAssignmentGuides.build(page, this));
    }

    private void closeGuide() {
        guideDrawer.close();
    }

    // endregion Guides

    // region Location, commit and close

    private void chooseLocation(LocationFilterItem item) {
        if (Objects.equals(item, location)) {
            return;
        }
        if (!data.plan().isEmpty()) {
            setMessage(getTextAt(RESOURCE_BUNDLE, "message.locationLocked"), true);
            // Put the picker back; the change it fires is for the current location, which is ignored above
            locationPicker.setSelectedItem(location);
            refresh();
            return;
        }
        location = item;
        state.pinnedUnits.clear();
        reload();
        setMessage("", false);
        refresh();
    }

    /** Reads the location again, after a commit or a change of location; selections and filters start over. */
    private void reload() {
        data = new CrewAssignmentData(campaign, location);
        state.selectedPeople.clear();
        state.openUnit = null;
        state.seatsForPerson = null;
        state.unitsForTech = null;
        state.qualifiedFor = null;
        state.techFor = null;
        state.targetTech = false;
        state.isSpreadPreview = false;
        state.unitType = null;
        state.formation = null;
        state.role = null;
        state.condition = Condition.ANY;
        state.familiarity = FamiliarityBand.ANY;
        lastPersonIndex = -1;
        shownUnits = List.of();
        board.resetCards();
        // The row renderers hold the plan they draw from, so they are given the new one
        peoplePage.setRenderer(new CrewRowRenderers.PersonRow(data.plan(), state.selectedPeople::contains,
              this::isTechsPage));
        reviewPage.setRenderer(new CrewRowRenderers.ReviewRow(data.plan()));
    }

    private void commit() {
        AssignmentPlan plan = data.plan();
        if (plan.isEmpty()) {
            return;
        }
        CommitReport report = CrewAssignmentService.commit(plan, data.isUseTransfers(), data.today());
        if (report.skippedCount() == 0) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.committed", report.appliedCount()), false);
        } else {
            for (Problem problem : report.skipped()) {
                Unit target = (problem.change().toUnit() != null) ? problem.change().toUnit() :
                                    problem.change().fromUnit();
                LOGGER.info("Skipped crew change for {} on {}: {}", problem.change().person().getFullName(),
                      (target == null) ? "-" : target.getName(), problem.reason());
            }
            report.techsSkipped().forEach(problem -> LOGGER.info("Skipped tech change on {}: {}",
                  problem.change().unit().getName(), problem.reason()));
            report.tempCrewSkipped().forEach(problem -> LOGGER.info("Skipped temp crew change on {}: {}",
                  problem.change().unit().getName(), problem.reason()));
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.committedSkipped", report.appliedCount(),
                  report.skippedCount()), true);
        }
        String kept = message;
        boolean keptWarning = isWarning;
        reload();
        setMessage(kept, keptWarning);
        refresh();
    }

    /** Closes, unless changes are pending: then the first request warns, and a second one discards them. */
    private void requestClose() {
        if (data.plan().isEmpty() || closeRequested) {
            dispose();
            return;
        }
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.closePending", data.plan().size()), true);
        // Set after the message, which clears it: any other action in between means the next close warns again
        closeRequested = true;
        refresh();
    }

    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CrewAssignmentDialog.class);
            setName("CrewAssignmentDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }

    // endregion Location, commit and close

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static String upper(String key) {
        return getTextAt(RESOURCE_BUNDLE, key).toUpperCase(Locale.ROOT);
    }
}
