/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import megamek.common.units.Aero;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;

/**
 * Model for a list that displays a unit's crew with their role.
 *
 * @author Neoancient
 */
public class CrewListModel extends AbstractListModel<Person> {
    enum CrewRole {
        COMMANDER(0, "Commander"),
        CONSOLE_CMDR(1, "Commander"),
        PILOT(2, "Pilot"),
        NAVIGATOR(3, "Navigator"),
        DRIVER(4, "Driver"),
        GUNNER(5, "Gunner"),
        TECH_OFFICER(6, "Tech Officer"),
        CREW(7, "Crew");

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
        public Component getListCellRendererComponent(JList<? extends Person> list, Person value, int index,
              boolean isSelected, boolean cellHasFocus) {
            setOpaque(true);
            Person person = getElementAt(index);
            String gunSkill = SkillType.getGunnerySkillFor(unit.getEntity());
            String driveSkill = SkillType.getDrivingSkillFor(unit.getEntity());
            PersonnelOptions options = person.getOptions();
            Attributes attributes = person.getATOWAttributes();
            String sb = "<html><font><b>" +
                              person.getFullTitle() +
                              "</b><br/>" +
                              CrewRole.getCrewRole(person, unit).getDisplayName() +
                              " ("
                              // Shooting and driving don't benefit from Reputation, so no need to pass that in.
                              +
                              (person.hasSkill(gunSkill) ?
                                     person.getSkill(gunSkill).getFinalSkillValue(options, attributes, 0) :
                                     "-") +
                              '/' +
                              (person.hasSkill(driveSkill) ?
                                     person.getSkill(driveSkill).getFinalSkillValue(options, attributes, 0) :
                                     "-") +
                              ")</font></html>";
            setHtmlText(sb);
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setImage(person.getPortrait().getImage(54));
            return this;
        }
    }
}
