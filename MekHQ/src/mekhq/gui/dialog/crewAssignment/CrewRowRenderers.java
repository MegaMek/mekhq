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
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudLinkList;
import mekhq.gui.baseComponents.hud.HudRing;
import mekhq.gui.baseComponents.hud.HudToolTip;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;

/**
 * The row renderers of the crew assignment lists, and the labels they share. Links in a row carry the {@link Unit} or
 * {@link Person} they point to, or a {@link RemoveLink}, which the dialog's link handler opens.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewRowRenderers {
    /** The Review page's link that takes a change out of the plan. */
    record RemoveLink(Person person) {}

    private static final String RESOURCE_BUNDLE = CrewAssignmentDialog.RESOURCE_BUNDLE;

    private CrewRowRenderers() {
    }

    // region Shared labels

    /**
     * @param unit the unit
     * @param type the seat
     *
     * @return the seat's name as the player knows it, for example "Driver" on a tank but "Pilot" on a VTOL
     */
    static String seatLabel(Unit unit, SeatType type) {
        Entity entity = unit.getEntity();
        if ((type == SeatType.DRIVER) && !((entity instanceof Tank) && !(entity instanceof VTOL))) {
            return getTextAt(RESOURCE_BUNDLE, "SeatType.PILOT");
        }
        return getTextAt(RESOURCE_BUNDLE, "SeatType." + type.name());
    }

    static Color skillColor(SkillLevel level) {
        return switch (level) {
            case LEGENDARY, HEROIC -> AMBER;
            case ELITE -> READY;
            case VETERAN -> ACCENT_BRIGHT;
            case REGULAR -> TEXT;
            case GREEN -> TEXT_MUTED;
            default -> TEXT_FAINT;
        };
    }

    /** A unit's damage level: green when undamaged, amber when moderately damaged, red when heavily or worse. */
    static Color damageColor(int damageState) {
        return switch (damageState) {
            case Entity.DMG_NONE -> READY;
            case Entity.DMG_LIGHT -> TEXT;
            case Entity.DMG_MODERATE -> AMBER;
            default -> DANGER;
        };
    }

    /** A person's hits: muted at none, amber when hurt, red when close to death. */
    static Color hitsColor(int hits) {
        if (hits <= 0) {
            return TEXT_MUTED;
        }
        return (hits >= 4) ? DANGER : AMBER;
    }

    /** A person's fatigue: muted at none, amber otherwise. */
    static Color fatigueColor(int fatigue) {
        return (fatigue <= 0) ? TEXT_MUTED : AMBER;
    }

    static Color crewingColor(CrewAssignmentState.Crewing crewing) {
        return switch (crewing) {
            case FULL -> READY;
            case TEMP -> ACCENT;
            case PARTIAL -> AMBER;
            case EMPTY -> DANGER;
        };
    }

    static String reasonText(Reason reason) {
        return getTextAt(RESOURCE_BUNDLE, "Reason." + reason.name());
    }

    private static JLabel label(Font font, Color color) {
        JLabel label = new JLabel();
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JLabel badge() {
        JLabel badge = label(hudFont(Font.BOLD, 0.72f, 0.06f), TEXT_MUTED);
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(6), scaleForGUI(1), scaleForGUI(6))));
        return badge;
    }

    private static void setBadgeColor(JLabel badge, Color text, Color border) {
        badge.setForeground(text);
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(6), scaleForGUI(1), scaleForGUI(6))));
    }

    /** Shows a label as a link, hovered or not, or as plain text when {@code link} is {@code null}. */
    private static void setLink(JList<?> list, int index, JLabel label, String text, Object link) {
        label.setText(text);
        HudLinkList.markLink(label, link);
        if (link == null) {
            label.setForeground(TEXT_MUTED);
            label.setFont(LINE_FONT);
        } else {
            boolean hovered = HudLinkList.isLinkHovered(list, index, link);
            label.setForeground(hovered ? TEXT : ACCENT_BRIGHT);
            label.setFont(hovered ? LINK_HOVER_FONT : LINE_FONT);
        }
    }

    private static final Font NAME_FONT = hudFont(Font.BOLD, 1.0f, 0.02f);
    private static final Font ROLE_FONT = hudFont(Font.PLAIN, 0.82f, 0.04f);
    private static final Font LINE_FONT = hudFont(Font.PLAIN, 0.82f, 0.0f);
    private static final Font LINK_HOVER_FONT = LINE_FONT.deriveFont(Map.of(TextAttribute.UNDERLINE,
          TextAttribute.UNDERLINE_ON));
    private static final Font SKILL_FONT = hudFont(Font.BOLD, 0.74f, 0.1f);

    /** A row panel: the selection box and any marker on the left, two lines of text, and badges on the right. */
    private abstract static class Row extends JPanel {
        final HudCheckBox checkBox = new HudCheckBox("");
        final JLabel name = label(NAME_FONT, TEXT);
        final JLabel role = label(ROLE_FONT, TEXT_MUTED);
        /** A short status after the role: a unit's damage, or a person's hits and fatigue. */
        final JLabel status = label(ROLE_FONT, TEXT_MUTED);
        final JPanel secondLine = transparent();
        final JPanel badges = transparent();

        Row(boolean hasCheckBox) {
            super(new BorderLayout(scaleForGUI(10), 0));
            setOpaque(true);

            JPanel left = transparent();
            left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
            if (hasCheckBox) {
                checkBox.setOpaque(false);
                left.add(checkBox);
            }
            addMarker(left);

            JPanel firstLine = transparent();
            firstLine.setLayout(new BoxLayout(firstLine, BoxLayout.X_AXIS));
            firstLine.add(name);
            firstLine.add(Box.createHorizontalStrut(scaleForGUI(6)));
            firstLine.add(role);
            firstLine.add(Box.createHorizontalStrut(scaleForGUI(10)));
            firstLine.add(status);
            firstLine.add(Box.createHorizontalGlue());
            secondLine.setLayout(new BoxLayout(secondLine, BoxLayout.X_AXIS));

            JPanel text = transparent();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            firstLine.setAlignmentX(LEFT_ALIGNMENT);
            secondLine.setAlignmentX(LEFT_ALIGNMENT);
            text.add(Box.createVerticalGlue());
            text.add(firstLine);
            text.add(secondLine);
            text.add(Box.createVerticalGlue());

            badges.setLayout(new BoxLayout(badges, BoxLayout.X_AXIS));

            add(left, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(badges, BorderLayout.EAST);
        }

        void addMarker(JPanel left) {
            // rows without a marker add nothing
        }

        void paintState(boolean isSelected) {
            Color background = isSelected ? SURFACE : SURFACE_DEEP;
            setBackground(background);
            // A hairline under every row, and an accent bar on the left of a selected one
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, isSelected ? ACCENT : background),
                        BorderFactory.createEmptyBorder(0, scaleForGUI(8), 0, scaleForGUI(10)))));
            checkBox.setSelected(isSelected);
        }

        static JPanel transparent() {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            return panel;
        }
    }

    // endregion Shared labels

    // region Units

    /** A unit row: its crewing ring, name and type, formation and lead crew member, and a badge per seat. */
    static final class UnitRow extends Row implements ListCellRenderer<UnitEntry> {
        private final AssignmentPlan plan;
        private final Predicate<Unit> isSelected;
        private HudRing ring;
        private final JLabel formation = label(LINE_FONT, TEXT_MUTED);
        private final JLabel lead = label(LINE_FONT, ACCENT_BRIGHT);

        UnitRow(AssignmentPlan plan, Predicate<Unit> isSelected) {
            super(true);
            this.plan = plan;
            this.isSelected = isSelected;
            secondLine.add(formation);
            secondLine.add(lead);
        }

        @Override
        void addMarker(JPanel left) {
            ring = new HudRing();
            left.add(Box.createHorizontalStrut(scaleForGUI(6)));
            left.add(ring);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends UnitEntry> list, UnitEntry entry, int index,
              boolean selectedInList, boolean cellHasFocus) {
            Unit unit = entry.unit();
            paintState(isSelected.test(unit));
            CrewCoverage.UnitCoverage coverage = CrewCoverage.of(unit, plan);
            ring.setColor(crewingColor(coverage.crewing()), false);
            name.setText(unit.getName());
            role.setText(entry.typeName());
            status.setText(entry.damageName());
            status.setForeground(damageColor(entry.damageState()));
            formation.setText(entry.formationName().isEmpty() ? "" : entry.formationName() + "  ·  ");

            Person leader = firstCrew(unit);
            if (leader == null) {
                setLink(list, index, lead, getTextAt(RESOURCE_BUNDLE,
                      (coverage.temp() > 0) ? "row.tempCrewOnly" : "row.noCrew"), null);
            } else {
                setLink(list, index, lead,
                      getFormattedTextAt(RESOURCE_BUNDLE, "row.lead", leader.getFullTitle()), leader);
            }

            badges.removeAll();
            for (CrewCoverage.SeatCoverage seat : coverage.seats()) {
                // Real people over the seat's size, then any places temporary crew hold, such as "GUNNER 2/10 +8 TEMP"
                JLabel badge = badge();
                String text = seatLabel(unit, seat.seat().type()).toUpperCase(Locale.ROOT) + ' ' + seat.real() + '/'
                                    + seat.seat().capacity();
                if (seat.temp() > 0) {
                    text += ' ' + getFormattedTextAt(RESOURCE_BUNDLE, "row.tempCrew", seat.temp())
                                        .toUpperCase(Locale.ROOT);
                }
                badge.setText(text);
                if (seat.uncovered() > 0) {
                    setBadgeColor(badge, AMBER, AMBER.darker());
                } else if (seat.temp() > 0) {
                    setBadgeColor(badge, ACCENT, BORDER_CYAN);
                } else {
                    setBadgeColor(badge, READY, BORDER);
                }
                badges.add(Box.createHorizontalStrut(scaleForGUI(4)));
                badges.add(badge);
            }
            return this;
        }

        private static Person firstCrew(Unit unit) {
            List<Person> crew = unit.getCrew();
            return crew.isEmpty() ? null : crew.get(0);
        }
    }

    // endregion Units

    // region People

    /** A person row: their name and role, where they will serve, their abilities, and their experience level. */
    static final class PersonRow extends Row implements ListCellRenderer<PersonEntry> {
        private final AssignmentPlan plan;
        private final Predicate<Person> isSelected;
        private final JLabel where = label(LINE_FONT, TEXT_MUTED);
        /** Hits and fatigue, on the second line beside where the person serves. */
        private final JLabel condition = label(LINE_FONT, TEXT_MUTED);
        private final JLabel abilities = badge();
        private final JLabel skill = label(SKILL_FONT, TEXT);

        PersonRow(AssignmentPlan plan, Predicate<Person> isSelected) {
            super(true);
            this.plan = plan;
            this.isSelected = isSelected;
            secondLine.add(where);
            secondLine.add(Box.createHorizontalStrut(scaleForGUI(10)));
            secondLine.add(condition);
            badges.add(abilities);
            badges.add(Box.createHorizontalStrut(scaleForGUI(10)));
            skill.setHorizontalAlignment(JLabel.RIGHT);
            badges.add(skill);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PersonEntry> list, PersonEntry entry,
              int index, boolean selectedInList, boolean cellHasFocus) {
            Person person = entry.person();
            paintState(isSelected.test(person));
            name.setText(person.getFullTitle());
            role.setText(person.getPrimaryRole().getLabel(false));
            status.setText("");
            List<String> parts = new ArrayList<>();
            if (entry.hits() > 0) {
                parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "row.hits", entry.hits()));
            }
            if ((entry.fatigue() != null) && (entry.fatigue() > 0)) {
                parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "row.fatigue", entry.fatigue()));
            }
            condition.setText(String.join("  \u00b7  ", parts));
            condition.setForeground((entry.hits() > 0) ? hitsColor(entry.hits()) :
                                          fatigueColor((entry.fatigue() == null) ? 0 : entry.fatigue()));

            Unit planned = plan.plannedUnitOf(person);
            StagedAssignment change = plan.changeFor(person);
            if (planned == null) {
                setLink(list, index, where, getTextAt(RESOURCE_BUNDLE, "row.unassigned"), null);
            } else {
                String text = (change == null) ? planned.getName() :
                                    getFormattedTextAt(RESOURCE_BUNDLE, "row.staged", planned.getName());
                setLink(list, index, where, text, planned);
            }

            int count = entry.abilities().size();
            abilities.setVisible(count > 0);
            if (count > 0) {
                abilities.setText(getFormattedTextAt(RESOURCE_BUNDLE, "row.abilities", count));
                setBadgeColor(abilities, ACCENT, BORDER_CYAN);
                abilities.setToolTipText(HudToolTip.html(getTextAt(RESOURCE_BUNDLE, "row.abilities.title"),
                      entry.abilities().stream().map(CrewAssignmentData.AbilityEntry::name)
                            .collect(Collectors.joining(", "))));
            } else {
                abilities.setToolTipText(null);
            }

            skill.setText(entry.skillLevel().toString().toUpperCase(Locale.ROOT));
            skill.setForeground(skillColor(entry.skillLevel()));
            return this;
        }
    }

    // endregion People

    // region Review

    /** A staged change: who, from where to where, and anything that would stop it being applied. */
    static final class ReviewRow extends Row implements ListCellRenderer<StagedAssignment> {
        private final AssignmentPlan plan;
        private final JLabel from = label(LINE_FONT, ACCENT_BRIGHT);
        private final JLabel arrow = label(LINE_FONT, TEXT_FAINT);
        private final JLabel to = label(LINE_FONT, ACCENT_BRIGHT);
        private final JLabel seat = label(LINE_FONT, TEXT_MUTED);
        private final JLabel reassign = badge();
        private final JLabel problem = label(LINE_FONT, DANGER);
        private final JLabel remove = label(LINE_FONT, ACCENT_BRIGHT);

        ReviewRow(AssignmentPlan plan) {
            super(false);
            this.plan = plan;
            arrow.setText("  →  ");
            secondLine.add(from);
            secondLine.add(arrow);
            secondLine.add(to);
            secondLine.add(seat);
            reassign.setText(getTextAt(RESOURCE_BUNDLE, "row.reassign").toUpperCase(Locale.ROOT));
            setBadgeColor(reassign, AMBER, AMBER.darker());
            badges.add(problem);
            badges.add(Box.createHorizontalStrut(scaleForGUI(10)));
            badges.add(reassign);
            badges.add(Box.createHorizontalStrut(scaleForGUI(12)));
            badges.add(remove);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends StagedAssignment> list,
              StagedAssignment change, int index, boolean selectedInList, boolean cellHasFocus) {
            paintState(false);
            Person person = change.person();
            setLink(list, index, name, person.getFullTitle(), person);
            name.setFont(HudLinkList.isLinkHovered(list, index, person) ? NAME_FONT.deriveFont(Map.of(
                  TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)) : NAME_FONT);
            role.setText(person.getPrimaryRole().getLabel(false));
            status.setText("");

            if (change.fromUnit() == null) {
                setLink(list, index, from, getTextAt(RESOURCE_BUNDLE, "row.unassigned"), null);
            } else {
                setLink(list, index, from, change.fromUnit().getName(), change.fromUnit());
            }
            setLink(list, index, to, change.toUnit().getName(), change.toUnit());
            seat.setText("  ·  " + seatLabel(change.toUnit(), change.toSeat()));

            reassign.setVisible(change.isReassignment());
            Reason reason = plan.problemWith(change);
            problem.setText((reason == null) ? "" : reasonText(reason));
            setLink(list, index, remove, getTextAt(RESOURCE_BUNDLE, "row.remove"), new RemoveLink(person));
            return this;
        }
    }

    // endregion Review
}
