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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.DIVIDER;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
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

import megamek.client.ui.WrapLayout;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudToolTip;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.AbilityEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;

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
    }

    private final Actions actions;
    private final JPanel selection = column();
    private final JPanel detail = column();
    private final HudButton assignButton = new HudButton("", true, true);
    private final HudButton autoFillButton = new HudButton("", false, true);
    private final HudButton clearButton = new HudButton(upper("inspector.clear"), false, true);

    CrewInspector(Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        setOpaque(true);
        setBackground(GROUND);
        setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER));

        assignButton.addActionListener(event -> actions.assignSelected());
        autoFillButton.addActionListener(event -> actions.autoFill());
        clearButton.addActionListener(event -> actions.clearSelection());

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
        JScrollPane scroll = new JScrollPane(stack);
        Hud.styleScroll(scroll, GROUND, false);
        add(scroll, BorderLayout.CENTER);
        setPreferredSize(scaleForGUI(360, 0));
    }

    /** Rebuilds the panel from the dialog's state. */
    void refresh(CrewAssignmentState state, CrewAssignmentData data) {
        AssignmentPlan plan = data.plan();
        buildSelection(state, plan);
        detail.removeAll();
        if (state.focusedUnit != null) {
            buildUnitDetail(state.focusedUnit, plan);
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
        for (Unit unit : state.selectedUnits) {
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
                    getFormattedTextAt(RESOURCE_BUNDLE, "inspector.units.open", openTotal),
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

        // The seat auto-fill fills: one chip per seat type with room on the selected units
        if (!openBySeat.isEmpty()) {
            if ((state.targetSeat == null) || !openBySeat.containsKey(state.targetSeat)) {
                state.targetSeat = openBySeat.keySet().iterator().next();
            }
            JPanel chips = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4), scaleForGUI(4)));
            Unit sample = state.selectedUnits.iterator().next();
            for (Map.Entry<SeatType, Integer> entry : openBySeat.entrySet()) {
                HudChip chip = new HudChip(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.seatChip",
                      CrewRowRenderers.seatLabel(sample, entry.getKey()), entry.getValue()),
                      () -> actions.chooseTargetSeat(entry.getKey()));
                chip.setActive(entry.getKey() == state.targetSeat);
                chips.add(chip);
            }
            selection.add(Box.createVerticalStrut(scaleForGUI(8)));
            selection.add(leftAligned(chips));
        } else {
            state.targetSeat = null;
        }

        boolean canAssign = (peopleCount > 0) && (unitCount > 0);
        assignButton.setText(canAssign ?
                                   getFormattedTextAt(RESOURCE_BUNDLE, "inspector.assign.count", peopleCount,
                                         unitCount).toUpperCase(Locale.ROOT) :
                                   upper("inspector.assign"));
        assignButton.setArmed(canAssign);
        int wanted = (state.targetSeat == null) ? 0 : openBySeat.get(state.targetSeat);
        autoFillButton.setText((wanted > 0) ?
                                     getFormattedTextAt(RESOURCE_BUNDLE, "inspector.autoFill.count", wanted)
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
        row.add(Hud.link(linkText, () -> actions.showPage(page)), BorderLayout.EAST);
        return row;
    }

    // endregion Selection

    // region Details

    private void buildUnitDetail(Unit unit, AssignmentPlan plan) {
        detail.add(leftAligned(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "inspector.unit"))));
        detail.add(leftAligned(title(unit.getName())));
        detail.add(Box.createVerticalStrut(scaleForGUI(10)));

        List<StagedAssignment> changes = plan.changes();
        for (CrewSeat seat : plan.seatsOf(unit)) {
            int open = Math.max(0, plan.openSeats(unit, seat.type()));
            String seatName = CrewRowRenderers.seatLabel(unit, seat.type());

            JPanel head = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            head.add(Hud.eyebrow(seatName + "  " + (seat.capacity() - open) + "/" + seat.capacity()),
                  BorderLayout.WEST);
            if (open > 0) {
                head.add(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.findPeople", seatName),
                      () -> actions.findPeopleFor(unit, seat.type())), BorderLayout.EAST);
            }
            detail.add(leftAligned(head));

            // Who will hold the seat once the plan is applied: those staying, then those arriving
            JPanel crew = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(2)));
            for (Person person : seat.occupants()) {
                if (unit.equals(plan.plannedUnitOf(person))) {
                    crew.add(Hud.link(person.getFullTitle(), () -> actions.openPerson(person)));
                }
            }
            for (StagedAssignment change : changes) {
                if (change.toUnit().equals(unit) && (change.toSeat() == seat.type())) {
                    Person person = change.person();
                    crew.add(Hud.link(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.arriving",
                          person.getFullTitle()), () -> actions.openPerson(person)));
                }
            }
            if (open > 0) {
                crew.add(colored(getFormattedTextAt(RESOURCE_BUNDLE, "inspector.open", open), AMBER));
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
        detail.add(Box.createVerticalStrut(scaleForGUI(10)));

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
            move.add(Hud.link(change.toUnit().getName(), () -> actions.openUnit(change.toUnit())));
            move.add(Hud.hint(CrewRowRenderers.seatLabel(change.toUnit(), change.toSeat())));
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
