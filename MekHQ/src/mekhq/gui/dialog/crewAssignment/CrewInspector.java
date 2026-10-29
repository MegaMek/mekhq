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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.ScrollPaneConstants;

import megamek.client.ui.WrapLayout;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudToolTip;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.AbilityEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;

/**
 * The right-hand panel of the {@link CrewAssignmentDialog}: what is selected on each page, the actions that assign
 * the selected people, and the details of the row the player last clicked. Every link here is a real, focusable link,
 * so anything a list row links to can also be reached from the keyboard.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewInspector extends JPanel {
    private static final String RESOURCE_BUNDLE = CrewAssignmentDialog.RESOURCE_BUNDLE;
    /** Wide enough for a selection summary, its link, and a row of three action buttons. */
    private static final int PREFERRED_WIDTH = 440;
    private static final int MINIMUM_WIDTH = 400;

    /** What the inspector asks the dialog to do. */
    interface Actions {
        void showPage(CrewAssignmentPage page);

        void assignSelected();

        void autoFill();

        void clearSelection();

        void chooseTargetSeat(SeatType seat);

        void openUnit(Unit unit);

        void openPerson(Person person);

        void findPeopleFor(Unit unit, SeatType seat);

        void findUnitsFor(Person person);

        void chooseTargetTech();

        void unassignSelectedPeople();

        void clearSelectedUnits();

        void removeSelectedTechs();

        void fillTempCrew();

        void removeTempCrew();

        void findTechFor(Unit unit);

        void findUnitsForTech(Person tech);
    }

    private final Actions actions;
    private final JPanel selection = column();
    private final JPanel detail = column();
    private final HudButton assignButton = new HudButton("", true, true);
    private final HudButton autoFillButton = new HudButton("", false, true);
    private final HudButton clearButton = new HudButton(upper("inspector.clear"), false, true);
    private final HudButton unassignButton = new HudButton(upper("inspector.unassign"), false, true);
    private final HudButton removeCrewButton = new HudButton(upper("inspector.removeCrew"), false, true);
    private final HudButton removeTechButton = new HudButton(upper("inspector.removeTech"), false, true);
    private final HudButton fillTempButton = new HudButton(upper("inspector.fillTempCrew"), false, true);
    private final HudButton removeTempButton = new HudButton(upper("inspector.removeTempCrew"), false, true);

    CrewInspector(Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        setOpaque(true);
        setBackground(GROUND);
        setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));

        assignButton.addActionListener(event -> actions.assignSelected());
        autoFillButton.addActionListener(event -> actions.autoFill());
        clearButton.addActionListener(event -> actions.clearSelection());
        unassignButton.addActionListener(event -> actions.unassignSelectedPeople());
        removeCrewButton.addActionListener(event -> actions.clearSelectedUnits());
        removeTechButton.addActionListener(event -> actions.removeSelectedTechs());
        fillTempButton.addActionListener(event -> actions.fillTempCrew());
        removeTempButton.addActionListener(event -> actions.removeTempCrew());
        unassignButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "inspector.unassign.toolTipText"));
        removeCrewButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "inspector.removeCrew.toolTipText"));
        removeTechButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "inspector.removeTech.toolTipText"));
        fillTempButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "inspector.fillTempCrew.toolTipText"));
        removeTempButton.setToolTipText(getTextAt(RESOURCE_BUNDLE, "inspector.removeTempCrew.toolTipText"));

        selection.setOpaque(true);
        selection.setBackground(SURFACE);
        selection.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(12), scaleForGUI(12))));
        detail.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(12),
              scaleForGUI(12)));

        JPanel stack = column();
        stack.add(leftAligned(selection));
        stack.add(leftAligned(detail));
        // The holder follows the viewport's width, so long rows wrap instead of running off the panel, and keeps the
        // column at its natural height at the top, so the sections are never stretched apart
        JScrollablePanel holder = new JScrollablePanel();
        holder.setLayout(new BorderLayout());
        holder.setOpaque(true);
        holder.setBackground(GROUND);
        holder.add(stack, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, GROUND, false);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(scaleForGUI(PREFERRED_WIDTH, 0));
        setMinimumSize(scaleForGUI(MINIMUM_WIDTH, 0));
    }

    /** Rebuilds the panel from the dialog's state. */
    void refresh(CrewAssignmentState state, CrewAssignmentData data) {
        AssignmentPlan plan = data.plan();
        buildSelection(state, plan);
        detail.removeAll();
        if (state.focusedUnit != null) {
            UnitEntry unitEntry = data.entryFor(state.focusedUnit);
            if (unitEntry != null) {
                buildUnitDetail(unitEntry, plan);
            }
        } else if (state.focusedPerson != null) {
            PersonEntry entry = data.entryFor(state.focusedPerson);
            if (entry != null) {
                buildPersonDetail(entry, plan);
            }
        } else {
            detail.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "inspector.detail.none"))));
        }
        revalidate();
        repaint();
    }

    // region Selection

    private void buildSelection(CrewAssignmentState state, AssignmentPlan plan) {
        selection.removeAll();
        selection.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.selection"))));

        Map<SeatType, Integer> openBySeat = new EnumMap<>(SeatType.class);
        int heldByTemp = 0;
        for (Unit unit : state.selectedUnits) {
            heldByTemp += CrewCoverage.of(unit, plan).temp();
            for (CrewSeat seat : plan.seatsOf(unit)) {
                int open = plan.openSeats(unit, seat.type());
                if (open > 0) {
                    openBySeat.merge(seat.type(), open, Integer::sum);
                }
            }
        }
        int unitCount = state.selectedUnits.size();
        int openTotal = openBySeat.values().stream().mapToInt(Integer::intValue).sum();
        selection.add(leftAligned(basket(
              (unitCount == 0) ? getTextAt(RESOURCE_BUNDLE, "inspector.units.none") :
                    getFormattedTextAt(RESOURCE_BUNDLE, "inspector.units.count", unitCount),
              (unitCount == 0) ? getTextAt(RESOURCE_BUNDLE, "inspector.units.none.hint") :
                    (heldByTemp == 0) ? getFormattedTextAt(RESOURCE_BUNDLE, "inspector.units.open", openTotal) :
                          getFormattedTextAt(RESOURCE_BUNDLE, "inspector.units.openWithTemp", openTotal,
                                heldByTemp),
              getTextAt(RESOURCE_BUNDLE, "inspector.units.link"), CrewAssignmentPage.UNITS)));

        int peopleCount = state.selectedPeople.size();
        long alreadyCrewing = state.selectedPeople.stream().filter(person -> person.getUnit() != null).count();
        String peopleHint = (peopleCount == 0) ? getTextAt(RESOURCE_BUNDLE, "inspector.people.none.hint") :
                                  (alreadyCrewing == 0) ? "" :
                                        getFormattedTextAt(RESOURCE_BUNDLE, "inspector.people.crewing",
                                              alreadyCrewing);
        selection.add(leftAligned(basket(
              (peopleCount == 0) ? getTextAt(RESOURCE_BUNDLE, "inspector.people.none") :
                    getFormattedTextAt(RESOURCE_BUNDLE, "inspector.people.count", peopleCount),
              peopleHint, getTextAt(RESOURCE_BUNDLE, "inspector.people.link"), CrewAssignmentPage.PERSONNEL)));

        // What auto-fill fills: one chip per seat type with room on the selected units, and one for missing techs
        int needTech = 0;
        boolean hasTempCrew = false;
        for (Unit unit : state.selectedUnits) {
            if (SeatEligibility.takesTech(unit) && (plan.plannedTechOf(unit) == null)) {
                needTech++;
            }
            hasTempCrew |= plan.plannedTotalTempCrew(unit) > 0;
        }
        if ((state.targetSeat == null) || !openBySeat.containsKey(state.targetSeat)) {
            state.targetSeat = openBySeat.isEmpty() ? null : openBySeat.keySet().iterator().next();
        }
        if ((needTech == 0) || ((state.targetSeat == null) && !state.targetTech)) {
            state.targetTech = (needTech > 0) && (state.targetSeat == null);
        }
        if (!openBySeat.isEmpty() || (needTech > 0)) {
            JPanel chips = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(4)));
            Unit sample = state.selectedUnits.iterator().next();
            for (Map.Entry<SeatType, Integer> entry : openBySeat.entrySet()) {
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.seatChip",
                      CrewRowRenderers.seatLabel(sample, entry.getKey()), entry.getValue()),
                      () -> actions.chooseTargetSeat(entry.getKey()));
                chip.setActive(!state.targetTech && (entry.getKey() == state.targetSeat));
                chips.add(chip);
            }
            if (needTech > 0) {
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.techChip", needTech),
                      actions::chooseTargetTech);
                chip.setActive(state.targetTech);
                chips.add(chip);
            }
            selection.add(Box.createVerticalStrut(scaleForGUI(8)));
            selection.add(leftAligned(chips));
        }

        boolean canAssign = (peopleCount > 0) && (unitCount > 0);
        assignButton.setText(canAssign ?
                                   getFormattedTextAt(RESOURCE_BUNDLE, "inspector.assign.count", peopleCount,
                                         unitCount).toUpperCase(Locale.ROOT) :
                                   upper("inspector.assign"));
        assignButton.setArmed(canAssign);
        int wanted = state.targetTech ? needTech :
                           (state.targetSeat == null) ? 0 : openBySeat.get(state.targetSeat);
        autoFillButton.setText((wanted > 0) ?
                                     getFormattedTextAt(RESOURCE_BUNDLE, state.targetTech ?
                                                                               "inspector.autoFill.techs" :
                                                                               "inspector.autoFill.count", wanted)
                                           .toUpperCase(Locale.ROOT) :
                                     upper("inspector.autoFill"));
        autoFillButton.setArmed(wanted > 0);
        clearButton.setArmed((peopleCount > 0) || (unitCount > 0));

        JPanel buttons = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(4)));
        buttons.add(assignButton);
        buttons.add(autoFillButton);
        buttons.add(clearButton);
        selection.add(Box.createVerticalStrut(scaleForGUI(6)));
        selection.add(leftAligned(buttons));

        // Taking people out, and the units' crew and temporary crew, once something is selected
        boolean anyPersonPlaced = state.selectedPeople.stream()
                                        .anyMatch(person -> plan.canUnassign(person)
                                                                  || !plan.plannedTechUnits(person).isEmpty());
        boolean anyCrewed = state.selectedUnits.stream().anyMatch(unit -> !plan.plannedCrewOf(unit).isEmpty());
        boolean anyTeched = state.selectedUnits.stream().anyMatch(unit -> plan.plannedTechOf(unit) != null);
        unassignButton.setArmed(anyPersonPlaced);
        removeCrewButton.setArmed(anyCrewed);
        removeTechButton.setArmed(anyTeched);
        fillTempButton.setArmed(state.selectedUnits.stream().anyMatch(unit -> plan.plannedRealCrew(unit) > 0));
        removeTempButton.setArmed(hasTempCrew);

        JPanel moreButtons = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(4)));
        if (peopleCount > 0) {
            moreButtons.add(unassignButton);
        }
        if (unitCount > 0) {
            moreButtons.add(removeCrewButton);
            moreButtons.add(removeTechButton);
            moreButtons.add(fillTempButton);
            moreButtons.add(removeTempButton);
        }
        if (moreButtons.getComponentCount() > 0) {
            selection.add(leftAligned(moreButtons));
        }
    }

    private JComponent basket(String title, String hint, String linkText, CrewAssignmentPage page) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), 0, scaleForGUI(8), 0)));
        JPanel text = column();
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        text.add(leftAligned(titleLabel));
        if (!hint.isEmpty()) {
            text.add(leftAligned(Hud.hint(hint)));
        }
        row.add(text, BorderLayout.CENTER);
        // Pinned to the top, so the link sits beside the title rather than floating between the two lines
        JPanel linkHolder = Hud.transparentPanel(new BorderLayout());
        linkHolder.add(Hud.link(linkText, () -> actions.showPage(page)), BorderLayout.NORTH);
        row.add(linkHolder, BorderLayout.EAST);
        return row;
    }

    // endregion Selection

    // region Details

    private void buildUnitDetail(UnitEntry entry, AssignmentPlan plan) {
        Unit unit = entry.unit();
        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.unit"))));
        detail.add(leftAligned(title(unit.getName())));
        detail.add(leftAligned(Hud.hint(entry.formationName().isEmpty() ? entry.typeName() :
                                              entry.typeName() + "  ·  " + entry.formationName())));
        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(statLine(getTextAt(RESOURCE_BUNDLE, "inspector.unit.condition"), entry.damageName(),
              CrewRowRenderers.damageColor(entry.damageState()))));
        if (SeatEligibility.takesTech(unit)) {
            detail.add(leftAligned(techLine(unit, plan)));
        }
        detail.add(Box.createVerticalStrut(scaleForGUI(10)));

        List<StagedAssignment> changes = plan.changes();
        CrewCoverage.UnitCoverage coverage = CrewCoverage.of(unit, plan);
        if (coverage.pooled() > 0) {
            detail.add(leftAligned(statLine(getTextAt(RESOURCE_BUNDLE, "inspector.unit.tempPool"),
                  String.valueOf(coverage.pooled()), ACCENT)));
            detail.add(Box.createVerticalStrut(scaleForGUI(10)));
        }
        for (CrewCoverage.SeatCoverage seatCoverage : coverage.seats()) {
            CrewSeat seat = seatCoverage.seat();
            String seatName = CrewRowRenderers.seatLabel(unit, seat.type());

            JPanel head = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            head.add(Hud.eyebrow(seatName + "  " + seatCoverage.real() + "/" + seat.capacity()),
                  BorderLayout.WEST);
            // Offered even when the seat is full, to find someone to swap in
            head.add(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.findPeople", seatName),
                  () -> actions.findPeopleFor(unit, seat.type())), BorderLayout.EAST);
            detail.add(leftAligned(head));

            // Who will hold the seat once the plan is applied: those staying, then those arriving
            JPanel crew = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(2)));
            for (Person person : seat.occupants()) {
                if (unit.equals(plan.plannedUnitOf(person))) {
                    crew.add(Hud.link(person.getFullTitle(), () -> actions.openPerson(person)));
                }
            }
            for (StagedAssignment change : changes) {
                if (unit.equals(change.toUnit()) && (change.toSeat() == seat.type())) {
                    Person person = change.person();
                    crew.add(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.arriving",
                          person.getFullTitle()), () -> actions.openPerson(person)));
                }
            }
            if (seatCoverage.temp() > 0) {
                crew.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.temp", seatCoverage.temp()),
                      ACCENT));
            }
            if (seatCoverage.uncovered() > 0) {
                crew.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.open", seatCoverage.uncovered()),
                      AMBER));
            }
            detail.add(leftAligned(crew));
            detail.add(Box.createVerticalStrut(scaleForGUI(10)));
        }
    }

    private void buildPersonDetail(PersonEntry entry, AssignmentPlan plan) {
        Person person = entry.person();
        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.person"))));
        detail.add(leftAligned(title(person.getFullTitle())));
        detail.add(leftAligned(Hud.hint(person.getPrimaryRole().getLabel(false) + "  ·  " + entry.skillLevel())));
        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(statLine(getTextAt(RESOURCE_BUNDLE, "inspector.person.hits"),
              String.valueOf(entry.hits()), CrewRowRenderers.hitsColor(entry.hits()))));
        if (entry.fatigue() != null) {
            detail.add(leftAligned(statLine(getTextAt(RESOURCE_BUNDLE, "inspector.person.fatigue"),
                  String.valueOf(entry.fatigue()), CrewRowRenderers.fatigueColor(entry.fatigue()))));
        }
        detail.add(Box.createVerticalStrut(scaleForGUI(10)));

        if (person.isTech()) {
            buildTechDetail(person, plan);
            detail.add(Box.createVerticalStrut(scaleForGUI(10)));
        }

        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.person.unit"))));
        Unit unit = person.getUnit();
        detail.add(leftAligned((unit == null) ?
                                     Hud.hint(getTextAt(RESOURCE_BUNDLE, "row.unassigned")) :
                                     Hud.link(unit.getName(), () -> actions.openUnit(unit))));

        StagedAssignment change = plan.changeFor(person);
        if (change != null) {
            detail.add(Box.createVerticalStrut(scaleForGUI(8)));
            detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.person.pending"))));
            JPanel move = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
            move.add(Hud.hint("→"));
            Unit toUnit = change.toUnit();
            if (toUnit == null) {
                move.add(Hud.hint(getTextAt(RESOURCE_BUNDLE, "row.unassigned")));
            } else {
                move.add(Hud.link(toUnit.getName(), () -> actions.openUnit(toUnit)));
                move.add(Hud.hint(CrewRowRenderers.seatLabel(toUnit, change.toSeat())));
            }
            detail.add(leftAligned(move));
        }

        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.person.abilities"))));
        if (entry.abilities().isEmpty()) {
            detail.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "inspector.person.abilities.none"))));
        } else {
            JPanel chips = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(4)));
            for (AbilityEntry ability : entry.abilities()) {
                chips.add(abilityChip(ability));
            }
            detail.add(leftAligned(chips));
        }

        detail.add(Box.createVerticalStrut(scaleForGUI(10)));
        detail.add(leftAligned(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.findUnits",
              person.getGivenName()), () -> actions.findUnitsFor(person))));
    }

    /** A chip naming a special ability, whose HUD tooltip explains it. */
    private static JComponent abilityChip(AbilityEntry ability) {
        HudChip chip = new HudChip(ability.name(), () -> {}) {
            @Override
            public JToolTip createToolTip() {
                return new HudToolTip(this);
            }
        };
        chip.setActive(true);
        chip.setToolTipText(HudToolTip.html(ability.name(), ability.description()));
        chip.getAccessibleContext().setAccessibleDescription(ability.description());
        return chip;
    }

    // endregion Details

    /** The unit's tech once the plan is applied, as a link, with a link to find one. */
    private JComponent techLine(Unit unit, AssignmentPlan plan) {
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        JPanel left = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, scaleForGUI(1)));
        left.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.unit.tech")));
        left.add(Box.createHorizontalStrut(scaleForGUI(10)));
        Person tech = plan.plannedTechOf(unit);
        if (tech == null) {
            left.add(colored(getTextAt(RESOURCE_BUNDLE, "inspector.unit.noTech"), AMBER));
        } else {
            boolean isStaged = plan.techChangeFor(unit) != null;
            left.add(Hud.link(isStaged ? getFormattedTextAt(RESOURCE_BUNDLE, "inspector.arriving",
                  tech.getFullTitle()) : tech.getFullTitle(), () -> actions.openPerson(tech)));
        }
        line.add(left, BorderLayout.CENTER);
        line.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "inspector.findTech"), () -> actions.findTechFor(unit)),
              BorderLayout.EAST);
        return line;
    }

    /** A tech's units and time once the plan is applied. */
    private void buildTechDetail(Person tech, AssignmentPlan plan) {
        int used = plan.plannedTechMinutes(tech);
        int available = plan.techMinutesAvailable(tech);
        detail.add(leftAligned(statLine(getTextAt(RESOURCE_BUNDLE, "inspector.person.techTime"),
              getFormattedTextAt(RESOURCE_BUNDLE, "inspector.person.techTime.value", used, available),
              (used > available) ? DANGER : TEXT)));
        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.person.maintains"))));
        List<Unit> units = plan.plannedTechUnits(tech);
        if (units.isEmpty()) {
            detail.add(leftAligned(Hud.hint(getTextAt(RESOURCE_BUNDLE, "inspector.person.maintains.none"))));
        } else {
            JPanel links = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(2)));
            for (Unit unit : units) {
                links.add(Hud.link(unit.getName(), () -> actions.openUnit(unit)));
            }
            detail.add(leftAligned(links));
        }
        detail.add(Box.createVerticalStrut(scaleForGUI(8)));
        detail.add(leftAligned(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.findUnitsForTech",
              tech.getGivenName()), () -> actions.findUnitsForTech(tech))));
    }

    /** A small caption with its value beside it, such as "HITS  2". */
    private static JComponent statLine(String caption, String value, Color color) {
        JPanel line = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, scaleForGUI(1)));
        line.add(Hud.eyebrow(caption));
        line.add(Box.createHorizontalStrut(scaleForGUI(10)));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(color);
        valueLabel.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        line.add(valueLabel);
        return line;
    }

    private static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT_BRIGHT);
        label.setFont(hudFont(Font.BOLD, 1.1f, 0.04f));
        return label;
    }

    private static JLabel colored(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

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
