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
package mekhq.gui.dialog.markets.personnelMarket;

import static mekhq.campaign.personnel.enums.GenderDescriptors.MALE_FEMALE_OTHER;
import static mekhq.campaign.personnel.skills.SkillUtilities.getColoredExperienceLevelName;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.swing.table.AbstractTableModel;

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.sorter.IntegerStringSorter;
import mekhq.gui.sorter.LevelSorter;

/**
 * Table model for displaying a list of {@link Person} applicants in the personnel market dialog.
 *
 * <p>Supports customized columns, localized headers, and type-aware sorting for each column.</p>
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Provides display and sorting logic for applicant full name, profession, experience, age, and gender.</li>
 *     <li>Integrates with campaign context for dynamic values (e.g., experience, age).</li>
 *     <li>Supplies localized column headers via {@link ApplicantTableColumns#getLabel()}.</li>
 *     <li>Supports multiple comparator types for intuitive column sorting.</li>
 * </ul>
 *
 * <p>Column summary:</p>
 * <ul>
 *     <li><b>FULL_NAME: Personal name, sorted naturally.</b></li>
 *     <li><b>PROFESSION: Primary role string, sorted naturally.</b></li>
 *     <li><b>EXPERIENCE: Formatted and colored experience level (HTML), sorted by custom level order.</b></li>
 *     <li><b>AGE: Age as string, sorted with integer-aware sorting.</b></li>
 *     <li><b>GENDER: Capitalized gender descriptor, sorted naturally.</b></li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>Instantiate with a campaign and a list of people.</li>
 *     <li>Used by {@link PersonnelTablePanel} to back the applicant table.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonTableModel extends AbstractTableModel {
    private final Campaign campaign;
    private final List<Person> people;

    /**
     * Creates a new {@code PersonTableModel} for the provided campaign and list of people.
     *
     * @param campaign the campaign context
     * @param people   the people to display
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonTableModel(Campaign campaign, List<Person> people) {
        this.campaign = campaign;
        this.people = people;
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
        NewPersonnelMarket market = campaign.getNewPersonnelMarket();
        Set<UUID> rarePersonnel = market.getRarePersonnel();

        ApplicantTableColumns column = ApplicantTableColumns.values()[columnIndex];
        return switch (column) {
            case FULL_NAME -> {
                String name = person.getFullName();
                if (rarePersonnel.contains(person.getId())) {
                    name += " ★";
                }

                yield name;
            }
            case PROFESSION -> {
                PersonnelRole profession = person.getPrimaryRole();

                if (market.getRareProfessions().contains(profession)) {
                    yield "<html><b>" + profession.toString() + "</b></html>";
                } else {
                    yield profession.toString();
                }
            }
            case EXPERIENCE -> {
                int experienceLevel = person.getExperienceLevel(campaign, false);
                yield "<html>" + getColoredExperienceLevelName(experienceLevel) + "</html>";
            }
            case AGE -> Integer.toString(person.getAge(campaign.getLocalDate()));
            case GENDER -> MALE_FEMALE_OTHER.getDescriptorCapitalized(person.getGender());
        };
    }

    /**
     * Returns a comparator suitable for sorting the specified column.
     *
     * @param columnIndex the column to sort
     *
     * @return a {@link Comparator} for column values
     *
     * @author Illiani
     * @since 0.50.06
     */
    public Comparator<?> getComparator(int columnIndex) {
        ApplicantTableColumns column = ApplicantTableColumns.values()[columnIndex];
        return switch (column) {
            case AGE -> new IntegerStringSorter();
            case EXPERIENCE -> new LevelSorter();
            default -> new NaturalOrderComparator();
        };
    }

    /**
     * Gets the {@link Person} instance for the specified row, or null if out of bounds.
     *
     * @param row the table row
     *
     * @return the associated {@link Person} object, or {@code null}
     *
     * @author Illiani
     * @since 0.50.06
     */
    public Person getPerson(int row) {
        if (row >= 0 && row < people.size()) {
            return people.get(row);
        }
        return null;
    }
}
