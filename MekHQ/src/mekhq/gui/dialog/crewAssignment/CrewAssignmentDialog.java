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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import javax.swing.*;

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
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Problem;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTempCrew;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TechStageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TempCrewResult;
import mekhq.campaign.unit.crewAssignment.AutoFill;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService;
import mekhq.campaign.unit.crewAssignment.CrewAssignmentService.CommitReport;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.*;
import mekhq.gui.baseComponents.hud.HudFilterList.Item;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Assignment;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.Crewing;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.SeatFilter;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.UnitStatus;
import mekhq.gui.model.LocationFilterItem;

/**
 * The crew assignment dialog: assign many people to many units at once, in a heads-up display in the style of the
 * interstellar map.
 *
 * <p>The Units and Personnel pages each list one location's units or people, filtered from a side rail and a search
 * field. Selections carry across pages, so a player can pick platoons on one page and soldiers on the other, then
 * assign them in one go, or let auto-fill put the best free people into every open seat. Names link between the pages.
 * Nothing changes until the player commits: assignments are staged in an {@link AssignmentPlan}, shown on the Review
 * page, can be undone a step at a time, and are applied by {@link CrewAssignmentService}.</p>
 *
 * <p>The dialog is modal, so the campaign cannot change under a staged plan.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class CrewAssignmentDialog extends JDialog implements CrewInspector.Actions {
    private static final MMLogger LOGGER = MMLogger.create(CrewAssignmentDialog.class);
    static final String RESOURCE_BUNDLE = "mekhq.resources.CrewAssignmentDialog";

    /** Wide enough for most formation names and their counts; longer names are shortened, with a tooltip. */
    private static final int RAIL_WIDTH = 300;
    private static final int UNIT_ROW_HEIGHT = 52;
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
    private int lastUnitIndex = -1;
    private int lastPersonIndex = -1;

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
    private final HudFilterList<PersonnelRole> roleFilter = new HudFilterList<>(value -> {
        state.role = value;
        refresh();
    });

    private final CardLayout pageCards = new CardLayout();
    private final JPanel pageHolder = new JPanel(pageCards);
    private final CrewListPage<UnitEntry> unitsPage;
    private final CrewListPage<PersonEntry> peoplePage;
    private final CrewListPage<CrewRowRenderers.ReviewItem> reviewPage;
    private final HudVerdictBanner problemsBanner = new HudVerdictBanner();
    private final CrewInspector inspector = new CrewInspector(this);

    // Guides: one per page, sliding over the window from the right
    private final HudCheckBox autoOpenGuides = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "guide.autoOpen"));
    private final HudGuideDrawer guideDrawer;
    private final HudButton guideButton = new HudButton("", false, true);
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
     * Opens the dialog for the location of the given units or people, with them already selected. People open it on
     * the Units page, showing where they could serve; units open it on the Personnel page, showing who could crew
     * them.
     *
     * @param frame    the main window
     * @param campaign the campaign
     * @param units    units to select, possibly none
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
        unitsPage = new CrewListPage<>(getTextAt(RESOURCE_BUNDLE, "units.search"), text -> {
            state.unitQuery = text;
            refresh();
        }, new CrewRowRenderers.UnitRow(plan, state.selectedUnits::contains), UNIT_ROW_HEIGHT, this::onUnitRow);
        peoplePage = new CrewListPage<>(getTextAt(RESOURCE_BUNDLE, "personnel.search"), text -> {
            state.peopleQuery = text;
            refresh();
        }, new CrewRowRenderers.PersonRow(plan, state.selectedPeople::contains), PERSON_ROW_HEIGHT,
              this::onPersonRow);
        reviewPage = new CrewListPage<>(null, text -> {}, new CrewRowRenderers.ReviewRow(plan), REVIEW_ROW_HEIGHT,
              (item, index, isRange) -> {
                  switch (item) {
                      case CrewRowRenderers.CrewItem crewItem -> state.focus(crewItem.change().person());
                      case CrewRowRenderers.TechItem techItem -> state.focus(techItem.change().unit());
                      case CrewRowRenderers.TempCrewItem tempItem -> state.focus(tempItem.change().unit());
                  }
                  refresh();
              });
        unitsPage.setLinkHandler((entry, link) -> openLink(link));
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
        setMinimumSize(scaleForGUI(1200, 700));
        getContentPane().setPreferredSize(scaleForGUI(1400, 840));
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
        for (Unit unit : units) {
            if (data.entryFor(unit) != null) {
                state.selectedUnits.add(unit);
            }
        }
        for (Person person : people) {
            if (data.entryFor(person) != null) {
                state.selectedPeople.add(person);
            }
        }
        if (!state.selectedPeople.isEmpty() && state.selectedUnits.isEmpty()) {
            Person first = state.selectedPeople.iterator().next();
            state.focus(first);
            if (state.selectedPeople.size() == 1) {
                state.seatsForPerson = first;
            }
            state.page = CrewAssignmentPage.UNITS;
        } else if (!state.selectedUnits.isEmpty()) {
            Unit first = state.selectedUnits.iterator().next();
            state.focus(first);
            if (state.selectedUnits.size() == 1) {
                for (CrewSeat seat : data.plan().seatsOf(first)) {
                    if (data.plan().openSeats(first, seat.type()) > 0) {
                        state.qualifiedFor = new SeatFilter(first, seat.type());
                        state.targetSeat = seat.type();
                        break;
                    }
                }
            }
            state.page = CrewAssignmentPage.PERSONNEL;
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
        pageHolder.add(unitsPage, CrewAssignmentPage.UNITS.name());
        pageHolder.add(peoplePage, CrewAssignmentPage.PERSONNEL.name());
        JPanel review = new JPanel(new BorderLayout());
        review.setOpaque(false);
        problemsBanner.setVisible(false);
        review.add(problemsBanner, BorderLayout.NORTH);
        review.add(reviewPage, BorderLayout.CENTER);
        pageHolder.add(review, CrewAssignmentPage.REVIEW.name());
        body.add(pageHolder, BorderLayout.CENTER);
        body.add(inspector, BorderLayout.EAST);

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
        modesRow.add(guideButton, BorderLayout.EAST);
        bar.add(modesRow, BorderLayout.SOUTH);
        return bar;
    }

    private JComponent buildRail() {
        JPanel unitRail = column();
        unitRail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.location"))));
        unitRail.add(leftAligned(locationPicker));
        unitRail.add(Box.createVerticalStrut(scaleForGUI(12)));
        unitRail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.unitType"))));
        unitRail.add(leftAligned(typeFilter));
        unitRail.add(Box.createVerticalStrut(scaleForGUI(12)));
        unitRail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.formation"))));
        unitRail.add(leftAligned(formationFilter));

        JPanel peopleRail = column();
        peopleRail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.assignment"))));
        peopleRail.add(leftAligned(assignmentFilter));
        peopleRail.add(Box.createVerticalStrut(scaleForGUI(12)));
        peopleRail.add(leftAligned(Hud.sectionHeading(getTextAt(RESOURCE_BUNDLE, "rail.role"))));
        peopleRail.add(leftAligned(roleFilter));

        railHolder.setOpaque(false);
        railHolder.add(railScroll(unitRail), CrewAssignmentPage.UNITS.name());
        railHolder.add(railScroll(peopleRail), CrewAssignmentPage.PERSONNEL.name());
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
        pageCards.show(pageHolder, state.page.name());
        railCards.show(railHolder, state.page.name());
        subtitle.setText(getFormattedTextAt(RESOURCE_BUNDLE, "header.subtitle", data.locationName(),
              campaign.getLocalDate()));

        switch (state.page) {
            case UNITS -> refreshUnitsPage(plan);
            case PERSONNEL -> refreshPersonnelPage(plan);
            case REVIEW -> refreshReviewPage(plan);
        }
        inspector.refresh(state, data);
        refreshTiles(plan);
        refreshDock(plan);
    }

    private void refreshUnitsPage(AssignmentPlan plan) {
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
        unitsPage.setRows(shown);
        unitsPage.setCount(getFormattedTextAt(RESOURCE_BUNDLE, "units.count", shown.size(),
              state.selectedUnits.size()));

        typeFilter.setItems(withAll("rail.allTypes", statusMatches, byType));
        typeFilter.setSelected(state.unitType);
        formationFilter.setItems(withAll("rail.allFormations", statusMatches, byFormation));
        formationFilter.setSelected(state.formation);

        List<JComponent> chips = new ArrayList<>();
        for (UnitStatus status : UnitStatus.values()) {
            HudChip chip = new HudChip(getTextAt(RESOURCE_BUNDLE, "UnitStatus." + status.name()), () -> {
                state.unitStatus = status;
                refresh();
            });
            chip.setActive(state.unitStatus == status);
            chips.add(chip);
        }
        if (state.seatsForPerson != null) {
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
        unitsPage.setChips(chips);
    }

    private void refreshPersonnelPage(AssignmentPlan plan) {
        List<PersonEntry> shown = new ArrayList<>();
        Map<PersonnelRole, Integer> byRole = new TreeMap<>();
        int unassigned = 0;
        int roleTotal = 0;
        List<PersonEntry> people = data.people();
        for (PersonEntry entry : people) {
            Person person = entry.person();
            boolean isAssigned = plan.plannedUnitOf(person) != null;
            if (!isAssigned) {
                unassigned++;
            }
            if ((state.assignment == Assignment.EVERYONE) || ((state.assignment == Assignment.ASSIGNED) == isAssigned)) {
                roleTotal++;
                for (PersonnelRole role : List.of(person.getPrimaryRole(), person.getSecondaryRole())) {
                    if (role.isCombat()) {
                        byRole.merge(role, 1, Integer::sum);
                    }
                }
            }
            if (state.shows(entry, plan)) {
                shown.add(entry);
            }
        }
        peoplePage.setRows(shown);
        peoplePage.setCount(getFormattedTextAt(RESOURCE_BUNDLE, "personnel.count", shown.size(),
              state.selectedPeople.size()));

        assignmentFilter.setItems(List.of(
              new Item<>(Assignment.UNASSIGNED, getTextAt(RESOURCE_BUNDLE, "Assignment.UNASSIGNED"), unassigned),
              new Item<>(Assignment.ASSIGNED, getTextAt(RESOURCE_BUNDLE, "Assignment.ASSIGNED"),
                    people.size() - unassigned),
              new Item<>(Assignment.EVERYONE, getTextAt(RESOURCE_BUNDLE, "Assignment.EVERYONE"), people.size())));
        assignmentFilter.setSelected(state.assignment);
        List<Item<PersonnelRole>> roles = new ArrayList<>();
        roles.add(new Item<>(null, getTextAt(RESOURCE_BUNDLE, "rail.allRoles"), roleTotal));
        byRole.forEach((role, count) -> roles.add(new Item<>(role, role.getLabel(false), count)));
        roleFilter.setItems(roles);
        roleFilter.setSelected(state.role);

        List<JComponent> chips = new ArrayList<>();
        if (state.qualifiedFor != null) {
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
            if (plan.plannedUnitOf(entry.person()) == null) {
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

    private void onUnitRow(UnitEntry entry, int index, boolean isRange) {
        List<UnitEntry> rows = unitsPage.rows();
        toggle(rows, entry, index, isRange, lastUnitIndex, UnitEntry::unit, state.selectedUnits);
        if (!isRange) {
            lastUnitIndex = index;
        }
        state.focus(entry.unit());
        refresh();
    }

    private void onPersonRow(PersonEntry entry, int index, boolean isRange) {
        List<PersonEntry> rows = peoplePage.rows();
        toggle(rows, entry, index, isRange, lastPersonIndex, PersonEntry::person, state.selectedPeople);
        if (!isRange) {
            lastPersonIndex = index;
        }
        state.focus(entry.person());
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
                    plan.removeTempCrewChange(item.change().unit());
                    setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.removed",
                          item.change().unit().getName()), false);
                }
            }
            refresh();
        }
    }

    // endregion Rows and links

    // region Inspector actions

    @Override
    public void showPage(CrewAssignmentPage page) {
        state.page = page;
        refresh();
    }

    @Override
    public void assignSelected() {
        AssignmentPlan plan = data.plan();
        List<Unit> units = orderedSelectedUnits();
        // Crew first; a tech no seat suits may still maintain a unit that has no tech
        StageResult crew = plan.stage(sortedSelectedPeople(), units);
        List<Person> spareTechs = crew.leftOver().stream().filter(Person::isTech).toList();
        TechStageResult techs = plan.stageTechs(spareTechs, units);
        Set<Person> techsPlaced = new HashSet<>();
        techs.staged().forEach(change -> techsPlaced.add(change.toTech()));

        int staged = crew.staged().size() + techs.staged().size();
        int leftOver = (int) crew.leftOver().stream().filter(person -> !techsPlaced.contains(person)).count();
        reportStaged(staged, leftOver);
        // Those placed are done; anyone left over stays selected, ready to try with other units
        state.selectedPeople.retainAll(crew.leftOver());
        state.selectedPeople.removeAll(techsPlaced);
        refresh();
    }

    @Override
    public void autoFill() {
        List<Person> candidates = data.people().stream().map(PersonEntry::person).toList();
        if (state.targetTech) {
            TechStageResult result = AutoFill.fillTechs(data.plan(), orderedSelectedUnits(), candidates,
                  data.ranking());
            reportStaged(result.staged().size(), 0);
            if (!result.leftOver().isEmpty()) {
                setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.techsLeftOver", result.staged().size(),
                      result.leftOver().size()), true);
            }
        } else if (state.targetSeat != null) {
            StageResult result = AutoFill.fill(data.plan(), orderedSelectedUnits(), state.targetSeat, candidates,
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
    public void clearSelection() {
        state.selectedUnits.clear();
        state.selectedPeople.clear();
        lastUnitIndex = -1;
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

    @Override
    public void unassignSelectedPeople() {
        int staged = data.plan().unassignEverything(sortedSelectedPeople());
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.unassigned", staged), staged == 0);
        refresh();
    }

    @Override
    public void clearSelectedUnits() {
        int staged = data.plan().clearUnits(orderedSelectedUnits());
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.unassigned", staged), staged == 0);
        refresh();
    }

    @Override
    public void removeSelectedTechs() {
        int staged = data.plan().unassignTechs(orderedSelectedUnits());
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.unassigned", staged), staged == 0);
        refresh();
    }

    @Override
    public void fillTempCrew() {
        TempCrewResult result = data.plan().fillTempCrew(orderedSelectedUnits(), data.tempCrewPool());
        int filled = result.staged().size();
        int added = result.staged().stream().mapToInt(StagedTempCrew::change).sum();
        if (filled == 0) {
            setMessage(getTextAt(RESOURCE_BUNDLE, "message.tempCrewNone"), true);
        } else if (!result.leftOver().isEmpty()) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewFilledSome", added, filled,
                  result.leftOver().size()), true);
        } else {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewFilled", added, filled), false);
        }
        refresh();
    }

    @Override
    public void removeTempCrew() {
        TempCrewResult result = data.plan().removeTempCrew(orderedSelectedUnits());
        int removed = -result.staged().stream().mapToInt(StagedTempCrew::change).sum();
        setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.tempCrewRemoved", removed,
              result.staged().size()), result.staged().isEmpty());
        refresh();
    }

    @Override
    public void findTechFor(Unit unit) {
        state.selectedUnits.add(unit);
        state.targetTech = true;
        state.techFor = unit;
        state.qualifiedFor = null;
        state.assignment = Assignment.EVERYONE;
        state.role = null;
        state.peopleQuery = "";
        peoplePage.setSearchText("");
        state.page = CrewAssignmentPage.PERSONNEL;
        refresh();
    }

    @Override
    public void findUnitsForTech(Person tech) {
        state.selectedPeople.add(tech);
        state.unitsForTech = tech;
        state.seatsForPerson = null;
        state.unitStatus = UnitStatus.ALL;
        state.unitType = null;
        state.formation = null;
        state.unitQuery = "";
        unitsPage.setSearchText("");
        state.page = CrewAssignmentPage.UNITS;
        refresh();
    }

    @Override
    public void openUnit(Unit unit) {
        UnitEntry entry = data.entryFor(unit);
        if (entry == null) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.notHere", unit.getName()), true);
            refresh();
            return;
        }
        if (!state.shows(entry, data.plan())) {
            state.unitStatus = UnitStatus.ALL;
            state.unitType = null;
            state.formation = null;
            state.seatsForPerson = null;
            state.unitsForTech = null;
            state.unitQuery = "";
            unitsPage.setSearchText("");
        }
        state.focus(unit);
        state.page = CrewAssignmentPage.UNITS;
        refresh();
        unitsPage.reveal(entry);
    }

    @Override
    public void openPerson(Person person) {
        PersonEntry entry = data.entryFor(person);
        if (entry == null) {
            setMessage(getFormattedTextAt(RESOURCE_BUNDLE, "message.notHere", person.getFullTitle()), true);
            refresh();
            return;
        }
        if (!state.shows(entry, data.plan())) {
            state.assignment = Assignment.EVERYONE;
            state.role = null;
            state.qualifiedFor = null;
            state.techFor = null;
            state.peopleQuery = "";
            peoplePage.setSearchText("");
        }
        state.focus(person);
        state.page = CrewAssignmentPage.PERSONNEL;
        refresh();
        peoplePage.reveal(entry);
    }

    @Override
    public void findPeopleFor(Unit unit, SeatType seat) {
        state.selectedUnits.add(unit);
        state.targetSeat = seat;
        state.qualifiedFor = new SeatFilter(unit, seat);
        state.techFor = null;
        state.targetTech = false;
        state.assignment = Assignment.UNASSIGNED;
        state.role = null;
        state.peopleQuery = "";
        peoplePage.setSearchText("");
        state.page = CrewAssignmentPage.PERSONNEL;
        refresh();
    }

    @Override
    public void findUnitsFor(Person person) {
        state.selectedPeople.add(person);
        state.seatsForPerson = person;
        state.unitsForTech = null;
        state.unitStatus = UnitStatus.NEEDS_CREW;
        state.unitType = null;
        state.formation = null;
        state.unitQuery = "";
        unitsPage.setSearchText("");
        state.page = CrewAssignmentPage.UNITS;
        refresh();
    }

    /** @return the selected units in the order the Units page lists them */
    private List<Unit> orderedSelectedUnits() {
        List<Unit> ordered = new ArrayList<>();
        for (UnitEntry entry : data.units()) {
            if (state.selectedUnits.contains(entry.unit())) {
                ordered.add(entry.unit());
            }
        }
        return ordered;
    }

    /** @return the selected people, best first */
    private List<Person> sortedSelectedPeople() {
        List<Person> sorted = new ArrayList<>(state.selectedPeople);
        sorted.sort(data.ranking());
        return sorted;
    }

    // endregion Inspector actions

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
        reload();
        setMessage("", false);
        refresh();
    }

    /** Reads the location again, after a commit or a change of location; selections and filters start over. */
    private void reload() {
        data = new CrewAssignmentData(campaign, location);
        state.selectedUnits.clear();
        state.selectedPeople.clear();
        state.focusedUnit = null;
        state.focusedPerson = null;
        state.seatsForPerson = null;
        state.unitsForTech = null;
        state.qualifiedFor = null;
        state.techFor = null;
        state.targetTech = false;
        state.unitType = null;
        state.formation = null;
        state.role = null;
        lastUnitIndex = -1;
        lastPersonIndex = -1;
        // The row renderers hold the plan they draw from, so they are given the new one
        unitsPage.setRenderer(new CrewRowRenderers.UnitRow(data.plan(), state.selectedUnits::contains));
        peoplePage.setRenderer(new CrewRowRenderers.PersonRow(data.plan(), state.selectedPeople::contains));
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
