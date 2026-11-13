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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.WrapLayout;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;

/**
 * The right-hand panel of the {@link CrewAssignmentDialog}'s Assign and Techs pages: one card per unit, beside the list
 * of people. On the Assign page a card lists the unit's tech and every seat; on the Techs page, only its tech.
 *
 * <p>One person and one unit can be selected at a time. Selecting a unit assigns nothing: it highlights the card,
 * which then shows its whole crew and extra actions, and sets the unit the Familiarity filter measures against. Each
 * seat and tech slot has its own button, which assigns the person selected in the list to that slot on that unit, so
 * the player always chooses who goes where. Where they cannot take a slot, the card says why. Everyone aboard has a
 * mark that takes them out again, and a full seat offers to replace whoever holds it. Above the cards are the unit
 * search and filters, and the actions that work on every unit shown at once: auto-fill and temporary crew.</p>
 *
 * <p>Only the first cards are built, with a link to build more, so a location with hundreds of units stays quick.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewUnitBoard extends JPanel {
    private static final String RESOURCE_BUNDLE = CrewAssignmentDialog.RESOURCE_BUNDLE;
    /** Wide enough for a seat's name and count, and an Assign button naming one person. */
    private static final int PREFERRED_WIDTH = 560;
    private static final int MINIMUM_WIDTH = 480;
    /** How many cards are built at first, and how many more each "Show more" adds. */
    private static final int CARD_STEP = 40;
    /** How many of a seat's crew a closed card names before it says "+N more". */
    private static final int CLOSED_CARD_NAMES = 3;

    /** What the board asks the dialog to do. */
    interface Actions {
        void searchUnits(String text);

        /** Assigns the selected person to one seat on one unit. */
        void assignToSeat(Unit unit, SeatType seat);

        /** Makes the selected tech the unit's tech, replacing any it has. */
        void assignTech(Unit unit);

        void removeFromUnit(Person person);

        void removeTech(Unit unit);

        void removeCrew(Unit unit);

        void fillTempCrewOn(Unit unit);

        void removeTempCrewOn(Unit unit);

        /** Fills the chosen seat type, or the missing techs, on every unit shown. */
        void autoFill();

        void fillTempCrewOnShown();

        void removeTempCrewOnShown();

        /** Puts one person in the seat another holds, taking that other person out of the unit. */
        void replaceOccupant(Person newcomer, Person occupant);

        void chooseTargetSeat(SeatType seat);

        void chooseTargetTech();

        /** Selects a unit, or deselects it if it is the selected one. */
        void toggleUnit(Unit unit);

        void openPerson(Person person);

        /** Opens the person's full record, as the Personnel tab shows it, in a window of its own. */
        void viewPerson(Person person);

        /** Opens the unit's full record, as the Hangar tab shows it, in a window of its own. */
        void viewUnit(Unit unit);

        void clearSelection();
    }

    /**
     * What the units shown still need, and so what the bulk actions can do: the open places by seat type, how many
     * units lack a tech, and whether temporary crew can be added or removed.
     *
     * @param openBySeat  the open places on the units shown, by seat type, in seat order
     * @param sampleOf    a unit that has each of those seats, to name it by: a Driver seat is a Pilot seat on anything
     *                    but a ground vehicle
     * @param needTech    how many of the units shown take a tech and have none
     * @param canFillTemp {@code true} if a unit shown has real crew aboard to lead temporary crew
     * @param hasTemp     {@code true} if a unit shown has temporary crew
     */
    record Work(Map<SeatType, Integer> openBySeat, Map<SeatType, Unit> sampleOf, int needTech, boolean canFillTemp,
          boolean hasTemp) {
        static Work of(List<UnitEntry> shown, AssignmentPlan plan, boolean techsOnly) {
            Map<SeatType, Integer> openBySeat = new EnumMap<>(SeatType.class);
            Map<SeatType, Unit> sampleOf = new EnumMap<>(SeatType.class);
            int needTech = 0;
            boolean canFillTemp = false;
            boolean hasTemp = false;
            for (UnitEntry entry : shown) {
                Unit unit = entry.unit();
                if (!techsOnly) {
                    for (CrewSeat seat : plan.seatsOf(unit)) {
                        int open = plan.openSeats(unit, seat.type());
                        if (open > 0) {
                            openBySeat.merge(seat.type(), open, Integer::sum);
                            sampleOf.putIfAbsent(seat.type(), unit);
                        }
                    }
                    canFillTemp |= plan.plannedRealCrew(unit) > 0;
                    hasTemp |= plan.plannedTotalTempCrew(unit) > 0;
                }
                if (SeatEligibility.takesTech(unit) && (plan.plannedTechOf(unit) == null)) {
                    needTech++;
                }
            }
            return new Work(openBySeat, sampleOf, needTech, canFillTemp, hasTemp);
        }
    }

    private final Actions actions;
    private final JPanel basket = column();
    private final JTextField search = new JTextField();
    private final JPanel chips = wrap(6);
    private final JLabel count = new JLabel();
    private final JPanel bulk = column();
    private final JPanel cards = column();
    private final JScrollPane scroll;
    private final Map<Unit, JComponent> cardOf = new HashMap<>();
    private int cardLimit = CARD_STEP;
    /** The filters the cards were last listed under; a change starts the cards again from the first ones. */
    private List<Object> filterSignature = List.of();

    // What the board last drew, kept so "Show more" and reveal can build more cards without the dialog
    private CrewAssignmentState state;
    private CrewAssignmentData data;
    private List<UnitEntry> shown = List.of();
    private Work work = new Work(Map.of(), Map.of(), 0, false, false);

    CrewUnitBoard(Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        setOpaque(true);
        setBackground(GROUND);
        setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));

        basket.setOpaque(true);
        basket.setBackground(SURFACE);
        basket.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(10), scaleForGUI(12))));

        Hud.styleField(search);
        search.getAccessibleContext().setAccessibleName(text("units.search"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                actions.searchUnits(search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                actions.searchUnits(search.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                actions.searchUnits(search.getText());
            }
        });
        JPanel searchRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        searchRow.add(Hud.eyebrow(text("units.search")), BorderLayout.WEST);
        searchRow.add(search, BorderLayout.CENTER);

        count.setForeground(TEXT_FAINT);
        count.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));

        JPanel tools = column();
        tools.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        tools.add(leftAligned(searchRow));
        tools.add(Box.createVerticalStrut(scaleForGUI(8)));
        tools.add(leftAligned(chips));
        tools.add(Box.createVerticalStrut(scaleForGUI(4)));
        tools.add(leftAligned(count));
        tools.add(Box.createVerticalStrut(scaleForGUI(6)));
        tools.add(leftAligned(bulk));

        JPanel north = column();
        north.add(leftAligned(basket));
        north.add(leftAligned(tools));

        // The holder follows the viewport's width, so long rows wrap instead of running off the panel, and keeps the
        // cards at their natural height at the top
        JScrollablePanel holder = new JScrollablePanel();
        holder.setLayout(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(SURFACE_DEEP);
        holder.add(cards, BorderLayout.NORTH);
        scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, SURFACE_DEEP, false);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));

        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(scaleForGUI(PREFERRED_WIDTH, 0));
        setMinimumSize(scaleForGUI(MINIMUM_WIDTH, 0));
    }

    /**
     * Rebuilds the panel from the dialog's state.
     *
     * @param state       the dialog's state
     * @param data        the location's units, people and plan
     * @param shown       the units that pass the filters, in the order to list them
     * @param work        what the units shown still need
     * @param filterChips the filter chips to show under the search field
     */
    void refresh(CrewAssignmentState state, CrewAssignmentData data, List<UnitEntry> shown, Work work,
          List<? extends JComponent> filterChips) {
        this.state = state;
        this.data = data;
        this.shown = List.copyOf(shown);
        this.work = work;

        // Narrowing or widening the list starts the cards over; "Show more" only lasts while the list stays the same
        List<Object> signature = Arrays.asList(state.page, state.unitStatus, state.techStatus, state.unitType,
              state.formation, state.unitQuery, state.pinnedUnits.size());
        if (!signature.equals(filterSignature)) {
            filterSignature = signature;
            cardLimit = CARD_STEP;
        }

        chips.removeAll();
        filterChips.forEach(chips::add);
        buildBasket();
        buildBulk();
        buildCards();
        revalidate();
        repaint();
    }

    /** Puts text in the search field without the player typing it, for example to clear it. */
    void setSearchText(String text) {
        if (!search.getText().equals(text)) {
            search.setText(text);
        }
    }

    /** Starts again from the first cards, after the location or its units have been read again. */
    void resetCards() {
        cardLimit = CARD_STEP;
    }

    /** Scrolls to a unit's card, if the unit is shown, building as many cards as it takes to reach it. */
    void reveal(Unit unit) {
        int index = -1;
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).unit().equals(unit)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        if (index >= cardLimit) {
            cardLimit = ((index / CARD_STEP) + 1) * CARD_STEP;
            buildCards();
            revalidate();
            repaint();
        }
        // After the layout, and after the scroll position buildCards puts back
        SwingUtilities.invokeLater(() -> {
            JComponent card = cardOf.get(unit);
            if (card != null) {
                card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
            }
        });
    }

    // region Selection and bulk actions

    private boolean isTechsPage() {
        return state.page == CrewAssignmentPage.TECHS;
    }

    private void buildBasket() {
        basket.removeAll();
        Person person = state.selectedPerson;

        JPanel top = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        top.add(Hud.eyebrow(text("board.assigning")), BorderLayout.WEST);
        if (person != null) {
            JLabel view = Hud.link(text("board.viewPerson"), () -> actions.viewPerson(person));
            view.setToolTipText(text("board.viewPerson.toolTipText"));
            JPanel links = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            links.add(view);
            links.add(Box.createHorizontalStrut(scaleForGUI(14)));
            links.add(Hud.link(text("people.clear"), actions::clearSelection));
            top.add(links, BorderLayout.EAST);
        }
        basket.add(leftAligned(top));

        JLabel title = new JLabel((person == null) ? text("board.none") : person.getFullTitle());
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.05f, 0.02f));
        basket.add(leftAligned(title));
        basket.add(leftAligned(Hud.hint((person == null) ?
                                              text(isTechsPage() ? "board.none.hint.techs" : "board.none.hint") :
                                              person.getPrimaryRole().getLabel(false))));
    }

    private void buildBulk() {
        boolean techsOnly = isTechsPage();
        Map<SeatType, Integer> openBySeat = work.openBySeat();
        int needTech = work.needTech();
        count.setText(techsOnly ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "board.units.count.techs", shown.size(), needTech) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "board.units.count", shown.size(),
                                  openBySeat.values().stream().mapToInt(Integer::intValue).sum(), needTech));

        bulk.removeAll();
        bulk.setVisible(!shown.isEmpty());
        if (shown.isEmpty()) {
            return;
        }
        bulk.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE, "board.bulk", shown.size()))));

        JPanel row = wrap(4);
        if (!techsOnly) {
            for (Map.Entry<SeatType, Integer> entry : openBySeat.entrySet()) {
                String seatName = CrewRowRenderers.seatLabel(work.sampleOf().get(entry.getKey()), entry.getKey());
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "board.seatChip", seatName,
                      entry.getValue()), () -> actions.chooseTargetSeat(entry.getKey()));
                chip.setActive(!state.targetTech && (entry.getKey() == state.targetSeat));
                row.add(chip);
            }
            if (needTech > 0) {
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "board.techChip", needTech),
                      actions::chooseTargetTech);
                chip.setActive(state.targetTech);
                row.add(chip);
            }
        }

        int wanted;
        if (state.targetTech) {
            wanted = needTech;
        } else if ((state.targetSeat == null) || techsOnly) {
            wanted = 0;
        } else {
            wanted = openBySeat.getOrDefault(state.targetSeat, 0);
        }
        String autoFillText;
        if (wanted == 0) {
            autoFillText = text(techsOnly ? "board.autoFill.techs.none" : "board.autoFill");
        } else {
            autoFillText = getFormattedTextAt(RESOURCE_BUNDLE, state.targetTech ? "board.autoFill.techs" :
                                                                     "board.autoFill.count", wanted);
        }
        row.add(bulkButton(autoFillText, techsOnly ? "board.autoFill.techs.toolTipText" : "board.autoFill.toolTipText",
              wanted > 0, actions::autoFill));
        if (!techsOnly) {
            row.add(bulkButton(text("board.fillTempCrew"), "board.fillTempCrew.toolTipText", work.canFillTemp(),
                  actions::fillTempCrewOnShown));
            row.add(bulkButton(text("board.removeTempCrew"), "board.removeTempCrew.shown.toolTipText", work.hasTemp(),
                  actions::removeTempCrewOnShown));
        }
        bulk.add(leftAligned(row));
    }

    private static HudButton bulkButton(String label, String toolTipKey, boolean isArmed, Runnable onClick) {
        HudButton button = new HudButton(label.toUpperCase(Locale.ROOT), false, true);
        button.setArmed(isArmed);
        button.setToolTipText(text(toolTipKey));
        button.addActionListener(event -> onClick.run());
        return button;
    }

    // endregion Selection and bulk actions

    // region Cards

    private void buildCards() {
        int scrollValue = scroll.getVerticalScrollBar().getValue();
        cards.removeAll();
        cardOf.clear();

        if (shown.isEmpty()) {
            JLabel notice = Hud.notice(text("board.empty"));
            notice.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(24), scaleForGUI(12), scaleForGUI(24),
                  scaleForGUI(12)));
            cards.add(leftAligned(notice));
        }

        AssignmentPlan plan = data.plan();
        Map<Unit, List<StagedAssignment>> arrivals = new HashMap<>();
        for (StagedAssignment change : plan.changes()) {
            if (change.toUnit() != null) {
                arrivals.computeIfAbsent(change.toUnit(), key -> new ArrayList<>()).add(change);
            }
        }
        Person selected = state.selectedPerson;
        int built = Math.min(cardLimit, shown.size());
        for (UnitEntry entry : shown.subList(0, built)) {
            JComponent card = card(entry, plan, arrivals.getOrDefault(entry.unit(), List.of()), selected);
            cardOf.put(entry.unit(), card);
            cards.add(leftAligned(card));
        }
        if (shown.size() > built) {
            int more = Math.min(CARD_STEP, shown.size() - built);
            JLabel showMore = Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "board.showMore", more,
                  shown.size() - built), () -> {
                cardLimit += CARD_STEP;
                buildCards();
                revalidate();
                repaint();
            });
            showMore.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(14),
                  scaleForGUI(12)));
            cards.add(leftAligned(showMore));
        }
        cards.revalidate();
        cards.repaint();
        // The cards were rebuilt, not moved, so the list stays where the player had scrolled it
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scrollValue));
    }

    private JComponent card(UnitEntry entry, AssignmentPlan plan, List<StagedAssignment> arrivals,
          @Nullable Person selected) {
        Unit unit = entry.unit();
        boolean isOpen = unit.equals(state.selectedUnit);
        boolean techsOnly = isTechsPage();
        CrewCoverage.UnitCoverage coverage = CrewCoverage.of(unit, plan);

        JPanel card = column();
        card.setOpaque(true);
        Color background = isOpen ? SURFACE : SURFACE_DEEP;
        card.setBackground(background);
        // A hairline under every card, and an accent bar on the left of the selected one, as a selected person has
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, isOpen ? ACCENT : background),
                    BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8),
                          scaleForGUI(12)))));
        // A click anywhere on the card that is not a link or a button selects the unit, or deselects it
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event) && card.contains(event.getPoint())) {
                    actions.toggleUnit(unit);
                }
            }
        });

        HudRing ring = new HudRing();
        if (techsOnly) {
            // On the Techs page the ring is about maintenance: green with a tech, amber without
            ring.setColor((plan.plannedTechOf(unit) == null) ? AMBER : READY, false);
        } else {
            ring.setColor(CrewRowRenderers.crewingColor(coverage.crewing()), false);
        }
        JLabel name = Hud.link(unit.getName(), () -> actions.toggleUnit(unit));
        name.setFont(hudFont(Font.BOLD, 1.0f, 0.02f));
        name.setToolTipText(text(isOpen ? "board.card.close.toolTipText" : "board.card.open.toolTipText"));
        JPanel identity = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        identity.add(ring);
        identity.add(Box.createHorizontalStrut(scaleForGUI(8)));
        identity.add(name);
        JPanel header = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        header.add(identity, BorderLayout.CENTER);
        JLabel view = Hud.link(text("board.viewUnit"), () -> actions.viewUnit(unit));
        view.setToolTipText(text("board.viewUnit.toolTipText"));
        JPanel status = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        status.add(colored(entry.damageName(), CrewRowRenderers.damageColor(entry.damageState())));
        status.add(Box.createHorizontalStrut(scaleForGUI(12)));
        status.add(view);
        header.add(status, BorderLayout.EAST);
        card.add(leftAligned(header));
        card.add(leftAligned(Hud.hint(entry.formationName().isEmpty() ? entry.typeName() :
                                            getFormattedTextAt(RESOURCE_BUNDLE, "board.unit.detail", entry.typeName(),
                                                  entry.formationName()))));
        card.add(Box.createVerticalStrut(scaleForGUI(6)));

        if (SeatEligibility.takesTech(unit)) {
            addTechLines(card, unit, plan, selected);
            card.add(Box.createVerticalStrut(scaleForGUI(4)));
        }
        if (techsOnly) {
            return card;
        }
        for (CrewCoverage.SeatCoverage seatCoverage : coverage.seats()) {
            addSeatLine(card, unit, plan, seatCoverage, selected, isOpen);
            JComponent crew = crewLine(unit, plan, seatCoverage, arrivals, selected, isOpen);
            if (crew != null) {
                card.add(leftAligned(crew));
            }
            card.add(Box.createVerticalStrut(scaleForGUI(4)));
        }

        if (isOpen) {
            JPanel extras = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            if (!plan.plannedCrewOf(unit).isEmpty()) {
                addExtra(extras, "board.removeCrew", () -> actions.removeCrew(unit));
            }
            if ((plan.plannedRealCrew(unit) > 0) && (coverage.uncovered() > 0)) {
                addExtra(extras, "board.fillTempCrew", () -> actions.fillTempCrewOn(unit));
            }
            if (plan.plannedTotalTempCrew(unit) > 0) {
                addExtra(extras, "board.removeTempCrew", () -> actions.removeTempCrewOn(unit));
            }
            if (extras.getComponentCount() > 0) {
                card.add(Box.createVerticalStrut(scaleForGUI(2)));
                card.add(leftAligned(extras));
            }
        }
        return card;
    }

    private void addExtra(JPanel extras, String key, Runnable onChoose) {
        if (extras.getComponentCount() > 0) {
            extras.add(Box.createHorizontalStrut(scaleForGUI(16)));
        }
        JLabel link = Hud.link(text(key), onChoose);
        link.setToolTipText(text(key + ".toolTipText"));
        extras.add(link);
    }

    /**
     * Adds the unit's tech slot, laid out as a seat is: its name and how full it is, with a button to make the
     * selected person the tech or the reason they cannot be, and under it who holds the slot, or that it is open.
     */
    private void addTechLines(JPanel card, Unit unit, AssignmentPlan plan, @Nullable Person person) {
        Person tech = plan.plannedTechOf(unit);
        Reason reason = (person == null) ? null : plan.whyNotTech(person, unit);
        boolean canTake = (person != null) && (reason == null);

        JPanel right = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        if (canTake && (tech == null)) {
            HudButton assign = assignButton(getFormattedTextAt(RESOURCE_BUNDLE, "board.assign.one",
                  person.getFullName()), () -> actions.assignTech(unit));
            assign.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "board.assignTech.toolTipText",
                  plan.techMinutesNeeded(person, unit)));
            right.add(assign);
        } else if ((reason != null) && (reason != Reason.ALREADY_ABOARD) && (person.isTech() || isTechsPage())) {
            // On the Assign page, someone who is not a tech at all was selected for a seat, so nothing is said here
            right.add(Hud.hint((reason == Reason.NOT_QUALIFIED) ? text("board.tech.notQualified") :
                                     CrewRowRenderers.reasonText(reason)));
        }
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.add(Hud.eyebrow(text("board.tech") + "  " + ((tech == null) ? 0 : 1) + "/1"), BorderLayout.WEST);
        line.add(right, BorderLayout.EAST);
        card.add(leftAligned(line));

        JPanel holder = wrap(10);
        if (tech == null) {
            int minutes = ((person != null) && person.isTech()) ? plan.techMinutesNeeded(person, unit) :
                                unit.getMaintenanceTime();
            holder.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "board.tech.open", minutes), AMBER));
        } else {
            JPanel occupant = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            boolean isStaged = plan.techChangeFor(unit) != null;
            String title = tech.getFullTitle();
            occupant.add(Hud.link(isStaged ? getFormattedTextAt(RESOURCE_BUNDLE, "board.staged", title) : title,
                  () -> actions.openPerson(tech)));
            occupant.add(Box.createHorizontalStrut(scaleForGUI(5)));
            occupant.add(removeMark(getFormattedTextAt(RESOURCE_BUNDLE, "board.removeTech.toolTipText", title),
                  () -> actions.removeTech(unit)));
            if (canTake) {
                // As a full seat does: the selected person can take the place of whoever holds it
                occupant.add(Box.createHorizontalStrut(scaleForGUI(6)));
                JLabel replace = Hud.link(text("board.replace"), () -> actions.assignTech(unit));
                replace.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "board.replaceTech.toolTipText", title,
                      person.getFullTitle(), plan.techMinutesNeeded(person, unit)));
                occupant.add(replace);
            }
            holder.add(occupant);
        }
        card.add(leftAligned(holder));
    }

    /**
     * Adds a seat's name and how full it is, with a button to assign the selected person to it, or the reason they
     * cannot take it.
     */
    private void addSeatLine(JPanel card, Unit unit, AssignmentPlan plan, CrewCoverage.SeatCoverage seatCoverage,
          @Nullable Person person, boolean isOpen) {
        CrewSeat seat = seatCoverage.seat();
        SeatType type = seat.type();
        String seatName = CrewRowRenderers.seatLabel(unit, type);

        JPanel right = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        if (person != null) {
            Reason reason = plan.whyNotSeat(person, unit, type);
            if (reason == null) {
                right.add(assignButton(getFormattedTextAt(RESOURCE_BUNDLE, "board.assign.one", person.getFullName()),
                      () -> actions.assignToSeat(unit, type)));
            } else if ((reason != Reason.ALREADY_ABOARD) && ((reason != Reason.SEAT_FULL) || isOpen)) {
                // A full seat is plain from its count, so only an open card spells that out
                right.add(Hud.hint(CrewRowRenderers.reasonText(reason)));
            }
        }

        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.add(Hud.eyebrow(seatName + "  " + seatCoverage.real() + "/" + seat.capacity()), BorderLayout.WEST);
        line.add(right, BorderLayout.EAST);
        card.add(leftAligned(line));
    }

    /**
     * Who will hold a seat once the plan is applied: those staying, then those arriving, then its temporary crew and
     * the places still open. A closed card names only the first few people. While the seat is full, each person in it
     * can be replaced by the selected person, if they could take the seat.
     *
     * @return the line, or {@code null} if there is nothing to say about the seat
     */
    private JComponent crewLine(Unit unit, AssignmentPlan plan, CrewCoverage.SeatCoverage seatCoverage,
          List<StagedAssignment> arrivals, @Nullable Person selected, boolean isOpen) {
        CrewSeat seat = seatCoverage.seat();
        List<Person> staying = new ArrayList<>();
        for (Person person : seat.occupants()) {
            if (unit.equals(plan.plannedUnitOf(person))) {
                staying.add(person);
            }
        }
        List<Person> arriving = new ArrayList<>();
        for (StagedAssignment change : arrivals) {
            if (change.toSeat() == seat.type()) {
                arriving.add(change.person());
            }
        }

        Person replacer = ((selected != null) && (plan.openSeats(unit, seat.type()) <= 0)
                                 && plan.couldTake(selected, unit, seat.type())) ? selected : null;

        JPanel crew = wrap(10);
        int named = 0;
        int nameLimit = isOpen ? Integer.MAX_VALUE : CLOSED_CARD_NAMES;
        for (Person person : staying) {
            if (named++ < nameLimit) {
                crew.add(occupant(person, plan, false, replacer));
            }
        }
        for (Person person : arriving) {
            if (named++ < nameLimit) {
                crew.add(occupant(person, plan, true, replacer));
            }
        }
        if (named > nameLimit) {
            JLabel more = Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "board.moreCrew", named - nameLimit),
                  () -> actions.toggleUnit(unit));
            more.setToolTipText(text("board.card.open.toolTipText"));
            crew.add(more);
        }
        if (seatCoverage.temp() > 0) {
            crew.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "board.temp", seatCoverage.temp()), ACCENT));
        }
        if (seatCoverage.uncovered() > 0) {
            crew.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "board.open", seatCoverage.uncovered()), AMBER));
        }
        return (crew.getComponentCount() == 0) ? null : crew;
    }

    /**
     * A crew member's name, as a link to them in the list, a mark that takes them out of the unit, and, when someone
     * selected could have their seat, a link that swaps that person in.
     */
    private JComponent occupant(Person person, AssignmentPlan plan, boolean isStaged, Person replacer) {
        JPanel holder = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        String title = person.getFullTitle();
        holder.add(Hud.link(isStaged ? getFormattedTextAt(RESOURCE_BUNDLE, "board.staged", title) : title,
              () -> actions.openPerson(person)));
        if (plan.canUnassign(person)) {
            holder.add(Box.createHorizontalStrut(scaleForGUI(5)));
            holder.add(removeMark(getFormattedTextAt(RESOURCE_BUNDLE, "board.remove.toolTipText", title),
                  () -> actions.removeFromUnit(person)));
        }
        if ((replacer != null) && plan.canReplace(replacer, person)) {
            holder.add(Box.createHorizontalStrut(scaleForGUI(6)));
            JLabel replace = Hud.link(text("board.replace"), () -> actions.replaceOccupant(replacer, person));
            replace.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "board.replace.toolTipText", title,
                  replacer.getFullTitle()));
            holder.add(replace);
        }
        return holder;
    }

    // endregion Cards

    private static HudButton assignButton(String label, Runnable onClick) {
        HudButton button = new HudButton(label.toUpperCase(Locale.ROOT), true, true);
        button.addActionListener(event -> onClick.run());
        return button;
    }

    /** A small cross that removes whoever it follows; its tooltip and accessible name say who. */
    private static JLabel removeMark(String description, Runnable onChoose) {
        JLabel mark = Hud.link(getTextAt(RESOURCE_BUNDLE, "board.remove.mark"), onChoose);
        mark.setToolTipText(description);
        mark.getAccessibleContext().setAccessibleName(description);
        return mark;
    }

    private static JLabel colored(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    private static JPanel wrap(int gap) {
        return Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(gap), scaleForGUI(2)));
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }
}
