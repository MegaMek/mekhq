/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.personnelMarket;

import static mekhq.campaign.market.personnelMarket.enums.ApplicantTableColumns.AGE;
import static mekhq.campaign.personnel.enums.GenderDescriptors.MALE_FEMALE_OTHER;

import java.util.List;
import javax.swing.table.AbstractTableModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.market.personnelMarket.enums.ApplicantTableColumns;
import mekhq.campaign.personnel.Person;

public class PersonTableModel extends AbstractTableModel {
    private final List<Person> people;
    private final Campaign campaign;

    public PersonTableModel(List<Person> people, Campaign campaign) {
        this.people = people;
        this.campaign = campaign;
    }

    @Override
    public int getRowCount() {
        return people.size();
    }

    @Override
    public int getColumnCount() {
        return ApplicantTableColumns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return ApplicantTableColumns.values()[column].getLabel();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Person person = people.get(rowIndex);
        ApplicantTableColumns column = ApplicantTableColumns.values()[columnIndex];
        return switch (column) {
            case FULL_NAME -> person.getFullName();
            case PROFESSION -> person.getPrimaryRole().toString();
            case EXPERIENCE -> person.getSkillLevel(campaign, false).toString();
            case AGE -> person.getAge(campaign.getLocalDate());
            case GENDER -> MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        ApplicantTableColumns col = ApplicantTableColumns.values()[columnIndex];
        if (col == AGE) {
            return Integer.class;
        } else {
            return String.class;
        }
    }

    public Person getPerson(int row) {
        if (row >= 0 && row < people.size()) {
            return people.get(row);
        }
        return null;
    }
}
