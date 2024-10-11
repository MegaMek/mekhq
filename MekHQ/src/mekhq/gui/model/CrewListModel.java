/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import megamek.common.Aero;
import megamek.common.Tank;
import megamek.common.VTOL;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Model for a list that displays a unit's crew with their role.
 *
 * @author Neoancient
 */
public class CrewListModel extends AbstractListModel<Person> {
    enum CrewRole {
        COMMANDER (0, "Commander"),
        CONSOLE_CMDR (1, "Commander"),
        PILOT (2, "Pilot"),
        NAVIGATOR (3, "Navigator"),
        DRIVER (4, "Driver"),
        GUNNER (5, "Gunner"),
        TECH_OFFICER (6, "Tech Officer"),
        CREW (7, "Crew");

        private final int sortOrder;
        private final String displayName;

        public int getSortOrder() {
            return sortOrder;
        }

        public String getDisplayName() {
            return displayName;
        }

        CrewRole(int sortOrder, String displayName) {
            this.sortOrder = sortOrder;
            this.displayName = displayName;
        }

        public static CrewRole getCrewRole(Person p, Unit u) {
            if (u.usesSoloPilot()) {
                return PILOT;
            } else if (u.isCommander(p) && u.getEntity().getCrew().getSlotCount() == 1) {
                return COMMANDER;
            } else if (u.getEntity() instanceof Tank && u.isTechOfficer(p)) {
                return CONSOLE_CMDR;
            } else if (u.isDriver(p)) {
                if (u.getEntity() instanceof VTOL || u.getEntity() instanceof Aero) {
                    return PILOT;
                } else {
                    return DRIVER;
                }
            } else if (u.isNavigator(p)) {
                return NAVIGATOR;
            } else if (u.isGunner(p)) {
                return GUNNER;
            } else if (u.isTechOfficer(p)) {
                return TECH_OFFICER;
            } else {
                return CREW;
            }
        }

    }

    Unit unit;
    List<Person> crew;

    public void setData(final Unit u) {
        this.unit = u;
        this.crew = new ArrayList<>(u.getCrew());
        crew.sort(Comparator.comparingInt(p -> CrewRole.getCrewRole(p, u).getSortOrder()));
        fireContentsChanged(this, 0, crew.size());
    }

    @Override
    public int getSize() {
        return crew.size();
    }
    @Override
    public Person getElementAt(int index) {
        if (index < 0 || index >= crew.size()) {
            return null;
        }
        return crew.get(index);
    }

    public ListCellRenderer<Person> getRenderer() {
        return new CrewRenderer();
    }

    public class CrewRenderer extends BasicInfo implements ListCellRenderer<Person> {
        public CrewRenderer() {
            super();
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Person> list, Person value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setOpaque(true);
            Person p = getElementAt(index);
            String gunSkill = SkillType.getGunnerySkillFor(unit.getEntity());
            String driveSkill = SkillType.getDrivingSkillFor(unit.getEntity());
            String sb = "<html><font><b>" + p.getFullTitle() + "</b><br/>"
                    + CrewRole.getCrewRole(p, unit).getDisplayName()
                    + " ("
                    + (p.hasSkill(gunSkill) ? p.getSkill(gunSkill).getFinalSkillValue() : "-")
                    + "/"
                    + (p.hasSkill(driveSkill) ? p.getSkill(driveSkill).getFinalSkillValue() : "-")
                    + ")</font></html>";
            setHtmlText(sb);
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setImage(p.getPortrait().getImage(54));
            return this;
        }
    }
}
