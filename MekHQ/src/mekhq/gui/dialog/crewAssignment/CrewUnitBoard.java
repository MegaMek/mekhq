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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.WrapLayout;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StageResult;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTech;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TechStageResult;
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
 * <p>Units are never selected. Each seat and tech slot has its own button, which assigns the people selected in the
 * list to that slot on that unit, so the player always chooses who goes where. Where some of them cannot take a slot,
 * the card says why. Everyone aboard has a mark that takes them out again, and a full seat offers to replace whoever
 * holds it. Above the cards are the unit search and filters, and the actions that work on every unit shown at once:
 * auto-fill, temporary crew, and spreading the selected people over the units, previewed before it is staged.</p>
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
    /** How many of the selected people the top of the panel names before it says "and N more". */
    private static final int BASKET_NAMES = 2;
    /** How many placements the spread preview lists before it says "and N more". */
    private static final int SPREAD_LINES = 8;

    /** What the board asks the dialog to do. */
    interface Actions {
        void searchUnits(String text);

        /** Assigns the selected people, first selected first, to one seat on one unit. */
        void assignToSeat(Unit unit, SeatType seat);

        /** Makes the first selected tech who can take the unit on its tech, replacing any it has. */
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

        /** Shows, or hides, where spreading the selected people over the units shown would put them. */
        void toggleSpreadPreview();

        /** Stages what the spread preview shows. */
        void spreadSelected();

        /** Makes the selection exactly these people, for example those a slot turned away. */
        void selectOnly(List<Person> people);

        /** Puts one person in the seat another holds, taking that other person out of the unit. */
        void replaceOccupant(Person newcomer, Person occupant);

        void chooseTargetSeat(SeatType seat);

        void chooseTargetTech();

        /** Opens a unit's card, or closes it if it is the open one. */
        void toggleUnit(Unit unit);

        void openPerson(Person person);

        void clearSelection();
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

    // What the board last drew, kept so "Show more" and reveal can build more cards without the dialog
    private CrewAssignmentState state;
    private CrewAssignmentData data;
    private List<UnitEntry> shown = List.of();

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
     * @param filterChips the filter chips to show under the search field
     */
    void refresh(CrewAssignmentState state, CrewAssignmentData data, List<UnitEntry> shown,
          List<? extends JComponent> filterChips) {
        this.state = state;
        this.data = data;
        this.shown = List.copyOf(shown);

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
        List<Person> people = List.copyOf(state.selectedPeople);

        JPanel top = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        top.add(Hud.eyebrow(text("board.assigning")), BorderLayout.WEST);
        if (!people.isEmpty()) {
            top.add(Hud.link(text("people.clear"), actions::clearSelection), BorderLayout.EAST);
        }
        basket.add(leftAligned(top));

        JLabel title = new JLabel(people.isEmpty() ? text("board.none") :
                                        getFormattedTextAt(RESOURCE_BUNDLE, "board.count", people.size()));
        title.setForeground(ACCENT_BRIGHT);
        title.setFont(hudFont(Font.BOLD, 1.05f, 0.02f));
        basket.add(leftAligned(title));

        String hint;
        if (people.isEmpty()) {
            hint = text(isTechsPage() ? "board.none.hint.techs" : "board.none.hint");
        } else {
            List<String> names = new ArrayList<>();
            for (Person person : people.subList(0, Math.min(BASKET_NAMES, people.size()))) {
                names.add(person.getFullTitle());
            }
            hint = String.join(", ", names);
            if (people.size() > BASKET_NAMES) {
                hint = getFormattedTextAt(RESOURCE_BUNDLE, "board.moreNames", hint, people.size() - BASKET_NAMES);
            }
        }
        basket.add(leftAligned(Hud.hint(hint)));
    }

    private void buildBulk() {
        AssignmentPlan plan = data.plan();
        boolean techsOnly = isTechsPage();
        Map<SeatType, Integer> openBySeat = new EnumMap<>(SeatType.class);
        // A unit that has each seat, to name it by: a Driver seat is a Pilot seat on anything but a ground vehicle
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
        int openTotal = openBySeat.values().stream().mapToInt(Integer::intValue).sum();
        count.setText(techsOnly ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "board.units.count.techs", shown.size(), needTech) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "board.units.count", shown.size(), openTotal,
                                  needTech));

        // What auto-fill fills: the chosen seat type, or the missing techs, kept to something the units shown need
        if (techsOnly) {
            state.targetTech = needTech > 0;
        } else {
            if ((state.targetSeat == null) || !openBySeat.containsKey(state.targetSeat)) {
                state.targetSeat = openBySeat.isEmpty() ? null : openBySeat.keySet().iterator().next();
            }
            if ((needTech == 0) || ((state.targetSeat == null) && !state.targetTech)) {
                state.targetTech = (needTech > 0) && (state.targetSeat == null);
            }
        }

        bulk.removeAll();
        bulk.setVisible(!shown.isEmpty());
        if (shown.isEmpty()) {
            return;
        }
        bulk.add(leftAligned(Hud.eyebrow(getFormattedTextAt(RESOURCE_BUNDLE, "board.bulk", shown.size()))));

        JPanel row = wrap(4);
        if (!techsOnly) {
            for (Map.Entry<SeatType, Integer> entry : openBySeat.entrySet()) {
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "board.seatChip",
                      CrewRowRenderers.seatLabel(sampleOf.get(entry.getKey()), entry.getKey()), entry.getValue()),
                      () -> actions.chooseTargetSeat(entry.getKey()));
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
        row.add(bulkButton(autoFillText, "board.autoFill.toolTipText", wanted > 0, actions::autoFill));
        if (!techsOnly) {
            row.add(bulkButton(text("board.fillTempCrew"), "board.fillTempCrew.toolTipText", canFillTemp,
                  actions::fillTempCrewOnShown));
            row.add(bulkButton(text("board.removeTempCrew"), "board.removeTempCrew.shown.toolTipText", hasTemp,
                  actions::removeTempCrewOnShown));
        }
        int selectedCount = state.selectedPeople.size();
        row.add(bulkButton((selectedCount == 0) ? text("board.spread") :
                                 getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.count", selectedCount),
              techsOnly ? "board.spread.techs.toolTipText" : "board.spread.toolTipText", selectedCount > 0,
              actions::toggleSpreadPreview));
        bulk.add(leftAligned(row));

        if (state.isSpreadPreview && (selectedCount > 0)) {
            bulk.add(Box.createVerticalStrut(scaleForGUI(6)));
            bulk.add(leftAligned(spreadPreview(plan, techsOnly)));
        }
    }

    private static HudButton bulkButton(String label, String toolTipKey, boolean isArmed, Runnable onClick) {
        HudButton button = new HudButton(label.toUpperCase(Locale.ROOT), false, true);
        button.setArmed(isArmed);
        button.setToolTipText(text(toolTipKey));
        button.addActionListener(event -> onClick.run());
        return button;
    }

    /**
     * Where spreading the selected people over the units shown would put each of them, worked out from the plan
     * without changing it, with a button to stage exactly that.
     */
    private JComponent spreadPreview(AssignmentPlan plan, boolean techsOnly) {
        List<Person> people = List.copyOf(state.selectedPeople);
        List<Unit> units = new ArrayList<>(shown.size());
        for (UnitEntry entry : shown) {
            units.add(entry.unit());
        }

        List<String> lines = new ArrayList<>();
        String leftOver = null;
        if (techsOnly) {
            TechStageResult result = plan.previewStageTechs(people, units);
            for (StagedTech change : result.staged()) {
                Person tech = change.toTech();
                lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.techLine",
                      (tech == null) ? "" : tech.getFullTitle(), change.unit().getName()));
            }
            if (!result.leftOver().isEmpty()) {
                leftOver = getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.unitsLeft", result.leftOver().size());
            }
        } else {
            StageResult result = plan.previewStage(people, units);
            for (StagedAssignment change : result.staged()) {
                Unit unit = change.toUnit();
                if ((unit != null) && (change.toSeat() != null)) {
                    lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.line",
                          change.person().getFullTitle(), unit.getName(),
                          CrewRowRenderers.seatLabel(unit, change.toSeat())));
                }
            }
            if (!result.leftOver().isEmpty()) {
                leftOver = getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.peopleLeft", result.leftOver().size());
            }
        }

        JPanel box = column();
        box.setOpaque(true);
        box.setBackground(SURFACE);
        box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8), scaleForGUI(10))));
        box.add(leftAligned(Hud.eyebrow(text(techsOnly ? "board.spread.title.techs" : "board.spread.title"))));
        box.add(Box.createVerticalStrut(scaleForGUI(4)));
        if (lines.isEmpty()) {
            box.add(leftAligned(colored(text(techsOnly ? "board.spread.none.techs" : "board.spread.none"), AMBER)));
        }
        for (String line : lines.subList(0, Math.min(SPREAD_LINES, lines.size()))) {
            box.add(leftAligned(Hud.hint(line)));
        }
        if (lines.size() > SPREAD_LINES) {
            box.add(leftAligned(Hud.hint(getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.more",
                  lines.size() - SPREAD_LINES))));
        }
        if (leftOver != null) {
            box.add(leftAligned(colored(leftOver, AMBER)));
        }
        box.add(Box.createVerticalStrut(scaleForGUI(6)));

        JPanel buttons = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        HudButton stage = new HudButton(getFormattedTextAt(RESOURCE_BUNDLE, "board.spread.stage", lines.size())
                                              .toUpperCase(Locale.ROOT), true, true);
        stage.setArmed(!lines.isEmpty());
        stage.addActionListener(event -> actions.spreadSelected());
        HudButton cancel = new HudButton(text("board.spread.cancel").toUpperCase(Locale.ROOT), false, true);
        cancel.addActionListener(event -> actions.toggleSpreadPreview());
        buttons.add(stage);
        buttons.add(Box.createHorizontalStrut(scaleForGUI(8)));
        buttons.add(cancel);
        box.add(leftAligned(buttons));
        return box;
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
        List<Person> selected = List.copyOf(state.selectedPeople);
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
          List<Person> selected) {
        Unit unit = entry.unit();
        boolean isOpen = unit.equals(state.openUnit);
        boolean techsOnly = isTechsPage();
        CrewCoverage.UnitCoverage coverage = CrewCoverage.of(unit, plan);

        JPanel card = column();
        card.setOpaque(true);
        Color background = isOpen ? SURFACE : SURFACE_DEEP;
        card.setBackground(background);
        // A hairline under every card, and an accent bar on the left of the open one
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, isOpen ? ACCENT : background),
                    BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8),
                          scaleForGUI(12)))));

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
        header.add(colored(entry.damageName(), CrewRowRenderers.damageColor(entry.damageState())),
              BorderLayout.EAST);
        card.add(leftAligned(header));
        card.add(leftAligned(Hud.hint(entry.formationName().isEmpty() ? entry.typeName() :
                                            entry.typeName() + "  ·  " + entry.formationName())));
        card.add(Box.createVerticalStrut(scaleForGUI(6)));

        if (SeatEligibility.takesTech(unit)) {
            addTechLines(card, unit, plan, selected, isOpen);
            card.add(Box.createVerticalStrut(scaleForGUI(4)));
        }
        if (techsOnly) {
            return card;
        }
        for (CrewCoverage.SeatCoverage seatCoverage : coverage.seats()) {
            addSeatLines(card, unit, plan, seatCoverage, selected, isOpen);
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
     * Adds the unit's tech once the plan is applied, a mark to remove them, and a button to assign a selected tech.
     * An open card also says why any other selected tech cannot take the unit.
     */
    private void addTechLines(JPanel card, Unit unit, AssignmentPlan plan, List<Person> selected, boolean isOpen) {
        Person tech = plan.plannedTechOf(unit);
        JPanel left = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, scaleForGUI(1)));
        left.add(Hud.eyebrow(text("board.tech")));
        left.add(Box.createHorizontalStrut(scaleForGUI(10)));
        if (tech == null) {
            left.add(colored(text("board.noTech"), AMBER));
        } else {
            boolean isStaged = plan.techChangeFor(unit) != null;
            left.add(Hud.link(isStaged ? getFormattedTextAt(RESOURCE_BUNDLE, "board.staged", tech.getFullTitle()) :
                                    tech.getFullTitle(), () -> actions.openPerson(tech)));
            left.add(Box.createHorizontalStrut(scaleForGUI(6)));
            left.add(removeMark(getFormattedTextAt(RESOURCE_BUNDLE, "board.removeTech.toolTipText",
                  tech.getFullTitle()), () -> actions.removeTech(unit)));
        }

        // The first selected tech who can take the unit on is the one the button assigns; people who are not techs
        // at all are passed over without comment, as they were selected for a seat
        Person candidate = null;
        Map<Reason, List<Person>> cannot = new EnumMap<>(Reason.class);
        for (Person person : selected) {
            if (!person.isTech()) {
                continue;
            }
            Reason reason = plan.whyNotTech(person, unit);
            if (reason == null) {
                if (candidate == null) {
                    candidate = person;
                }
            } else if (reason != Reason.ALREADY_ABOARD) {
                cannot.computeIfAbsent(reason, key -> new ArrayList<>()).add(person);
            }
        }
        JPanel right = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        if (candidate != null) {
            right.add(assignButton(getFormattedTextAt(RESOURCE_BUNDLE, (tech == null) ? "board.assignTech" :
                                                                             "board.replaceTech",
                  candidate.getFullName(), plan.techMinutesNeeded(candidate, unit)), () -> actions.assignTech(unit)));
        } else if (!cannot.isEmpty() && !isOpen) {
            right.add(Hud.hint(shortReason(cannot)));
        }

        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.add(left, BorderLayout.CENTER);
        line.add(right, BorderLayout.EAST);
        card.add(leftAligned(line));
        if (isOpen) {
            addReasonLines(card, cannot);
        }
    }

    /**
     * Adds a seat's name and how full it is, with a button to assign the selected people who can take it. An open
     * card also says why the rest cannot.
     */
    private void addSeatLines(JPanel card, Unit unit, AssignmentPlan plan, CrewCoverage.SeatCoverage seatCoverage,
          List<Person> selected, boolean isOpen) {
        CrewSeat seat = seatCoverage.seat();
        SeatType type = seat.type();
        String seatName = CrewRowRenderers.seatLabel(unit, type);
        int open = plan.openSeats(unit, type);

        // People are assigned in the order they were selected, so the first who can take the seat gets it
        int eligible = 0;
        Person first = null;
        Map<Reason, List<Person>> cannot = new EnumMap<>(Reason.class);
        for (Person person : selected) {
            Reason reason = plan.whyNotSeat(person, unit, type);
            if (reason == null) {
                eligible++;
                if (first == null) {
                    first = person;
                }
            } else if (reason != Reason.ALREADY_ABOARD) {
                cannot.computeIfAbsent(reason, key -> new ArrayList<>()).add(person);
            }
        }

        JPanel right = Hud.transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        int placing = Math.min(eligible, Math.max(0, open));
        if ((placing == 1) && (first != null)) {
            right.add(assignButton(getFormattedTextAt(RESOURCE_BUNDLE, "board.assign.one", first.getFullName()),
                  () -> actions.assignToSeat(unit, type)));
        } else if (placing > 1) {
            right.add(assignButton(getFormattedTextAt(RESOURCE_BUNDLE, "board.assign.many", placing),
                  () -> actions.assignToSeat(unit, type)));
        } else if (!cannot.isEmpty() && !isOpen && (open > 0)) {
            // A closed card only explains a seat that has room; a full seat needs no explanation on every card
            right.add(Hud.hint(shortReason(cannot)));
        }

        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.add(Hud.eyebrow(seatName + "  " + seatCoverage.real() + "/" + seat.capacity()), BorderLayout.WEST);
        line.add(right, BorderLayout.EAST);
        card.add(leftAligned(line));
        if (isOpen) {
            addReasonLines(card, cannot);
        }
    }

    /** @return the reason that turns away the most of the selected people, as a short line for a closed card */
    private static String shortReason(Map<Reason, List<Person>> cannot) {
        Reason most = null;
        int size = 0;
        for (Map.Entry<Reason, List<Person>> entry : cannot.entrySet()) {
            if (entry.getValue().size() > size) {
                most = entry.getKey();
                size = entry.getValue().size();
            }
        }
        return (most == null) ? "" : CrewRowRenderers.reasonText(most);
    }

    /** Adds one line per reason: how many of the selected people it turns away, and a link that selects only them. */
    private void addReasonLines(JPanel card, Map<Reason, List<Person>> cannot) {
        for (Map.Entry<Reason, List<Person>> entry : cannot.entrySet()) {
            List<Person> people = List.copyOf(entry.getValue());
            JPanel line = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            line.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "board.cannot.line", people.size(),
                  CrewRowRenderers.reasonText(entry.getKey())), AMBER));
            line.add(Box.createHorizontalStrut(scaleForGUI(10)));
            JLabel select = Hud.link(text("board.cannot.select"), () -> actions.selectOnly(people));
            select.setToolTipText(text("board.cannot.select.toolTipText"));
            line.add(select);
            card.add(leftAligned(line));
        }
    }

    /**
     * Who will hold a seat once the plan is applied: those staying, then those arriving, then its temporary crew and
     * the places still open. A closed card names only the first few people. While the seat is full, each person in it
     * can be replaced by the first selected person who could take the seat.
     *
     * @return the line, or {@code null} if there is nothing to say about the seat
     */
    private JComponent crewLine(Unit unit, AssignmentPlan plan, CrewCoverage.SeatCoverage seatCoverage,
          List<StagedAssignment> arrivals, List<Person> selected, boolean isOpen) {
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

        Person replacer = null;
        if (plan.openSeats(unit, seat.type()) <= 0) {
            for (Person person : selected) {
                if (plan.couldTake(person, unit, seat.type())) {
                    replacer = person;
                    break;
                }
            }
        }

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
        JLabel mark = Hud.link("✕", onChoose);
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
