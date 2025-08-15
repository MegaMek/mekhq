/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class PersonnelFilterStyleTest {
    //region Variable Declarations
    private static final PersonnelFilterStyle[] styles = PersonnelFilterStyle.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelFilterStyle.STANDARD.toolTipText"),
              PersonnelFilterStyle.STANDARD.getToolTipText());
        assertEquals(resources.getString("PersonnelFilterStyle.ALL.toolTipText"),
              PersonnelFilterStyle.ALL.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsStandard() {
        for (final PersonnelFilterStyle personnelFilterStyle : styles) {
            if (personnelFilterStyle == PersonnelFilterStyle.STANDARD) {
                assertTrue(personnelFilterStyle.isStandard());
            } else {
                assertFalse(personnelFilterStyle.isStandard());
            }
        }
    }

    @Test
    public void testIsIndividualRole() {
        for (final PersonnelFilterStyle personnelFilterStyle : styles) {
            if (personnelFilterStyle == PersonnelFilterStyle.INDIVIDUAL_ROLE) {
                assertTrue(personnelFilterStyle.isIndividualRole());
            } else {
                assertFalse(personnelFilterStyle.isIndividualRole());
            }
        }
    }

    @Test
    public void testIsAll() {
        for (final PersonnelFilterStyle personnelFilterStyle : styles) {
            if (personnelFilterStyle == PersonnelFilterStyle.ALL) {
                assertTrue(personnelFilterStyle.isAll());
            } else {
                assertFalse(personnelFilterStyle.isAll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetFilters() {
        try (MockedStatic<PersonnelFilter> personnelFilter = Mockito.mockStatic(PersonnelFilter.class)) {
            final List<PersonnelFilter> individualRolesStandardPersonnelFilters = new ArrayList<>();
            individualRolesStandardPersonnelFilters.add(PersonnelFilter.MEKWARRIOR);
            personnelFilter.when(PersonnelFilter::getIndividualRolesStandardPersonnelFilters)
                  .thenReturn(individualRolesStandardPersonnelFilters);
            assertEquals(individualRolesStandardPersonnelFilters,
                  PersonnelFilterStyle.INDIVIDUAL_ROLE.getFilters(true));

            final List<PersonnelFilter> individualRolesExpandedPersonnelFilters = new ArrayList<>();
            individualRolesExpandedPersonnelFilters.add(PersonnelFilter.FOUNDER);
            personnelFilter.when(PersonnelFilter::getIndividualRolesExpandedPersonnelFilters)
                  .thenReturn(individualRolesExpandedPersonnelFilters);
            assertEquals(individualRolesExpandedPersonnelFilters,
                  PersonnelFilterStyle.INDIVIDUAL_ROLE.getFilters(false));

            final List<PersonnelFilter> allStandardFilters = new ArrayList<>();
            allStandardFilters.add(PersonnelFilter.ACTIVE);
            personnelFilter.when(PersonnelFilter::getAllStandardFilters).thenReturn(allStandardFilters);
            assertEquals(allStandardFilters, PersonnelFilterStyle.ALL.getFilters(true));

            final List<PersonnelFilter> allIndividualRoleFilters = new ArrayList<>();
            allIndividualRoleFilters.add(PersonnelFilter.PRISONER);
            personnelFilter.when(PersonnelFilter::getAllIndividualRoleFilters).thenReturn(allIndividualRoleFilters);
            assertEquals(allIndividualRoleFilters, PersonnelFilterStyle.ALL.getFilters(false));

            final List<PersonnelFilter> standardPersonnelFilters = new ArrayList<>();
            standardPersonnelFilters.add(PersonnelFilter.VEHICLE_CREWMEMBER);
            personnelFilter.when(PersonnelFilter::getStandardPersonnelFilters).thenReturn(standardPersonnelFilters);
            assertEquals(standardPersonnelFilters, PersonnelFilterStyle.STANDARD.getFilters(true));

            final List<PersonnelFilter> expandedPersonnelFilters = new ArrayList<>();
            expandedPersonnelFilters.add(PersonnelFilter.DEAD);
            personnelFilter.when(PersonnelFilter::getExpandedPersonnelFilters).thenReturn(expandedPersonnelFilters);
            assertEquals(expandedPersonnelFilters, PersonnelFilterStyle.STANDARD.getFilters(false));
        }
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PersonnelFilterStyle.STANDARD.text"),
              PersonnelFilterStyle.STANDARD.toString());
        assertEquals(resources.getString("PersonnelFilterStyle.INDIVIDUAL_ROLE.text"),
              PersonnelFilterStyle.INDIVIDUAL_ROLE.toString());
        assertEquals(resources.getString("PersonnelFilterStyle.ALL.text"), PersonnelFilterStyle.ALL.toString());
    }
}
