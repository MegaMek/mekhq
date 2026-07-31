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
package mekhq.gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Covers the enhanced imaging badge shown against a unit in the force tree.
 *
 * <p>The claim worth holding is that the badge separates an implant that is working from one that is
 * not: the implant does nothing without an EI interface in the unit, and a badge that treated the two
 * alike would report a benefit the warrior does not have.</p>
 */
class EnhancedImagingBadgeTest {

    private static Person warrior(boolean implanted) {
        Person person = mock(Person.class);
        PersonnelOptions options = new PersonnelOptions();
        options.getOption(OptionsConstants.MD_EI_IMPLANT).setValue(implanted);
        when(person.getOptions()).thenReturn(options);
        return person;
    }

    /**
     * @param implanted    how many of the crew carry the implant
     * @param crewSize     how many crew the unit has
     * @param hasInterface whether the unit has an EI interface for the implant to work through
     */
    private static Unit unitWith(int implanted, int crewSize, boolean hasInterface) {
        List<Person> crew = new ArrayList<>();
        for (int seat = 0; seat < crewSize; seat++) {
            crew.add(warrior(seat < implanted));
        }
        Entity entity = mock(Entity.class);
        when(entity.hasEiCockpit()).thenReturn(hasInterface);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getActiveCrew()).thenReturn(crew);
        return unit;
    }

    @Test
    void anImplantedCrewInAUnitWithAnInterfaceIsBadged() {
        String badge = EnhancedImagingBadge.forUnit(unitWith(5, 5, true));

        assertTrue(badge.contains("[EI]"), "an implanted crew must be badged, got: " + badge);
        assertFalse(badge.contains("no interface"), "the implant is working, so nothing is amiss");
    }

    /**
     * The distinction the badge exists to draw. A warrior implanted in a machine with no EI interface
     * gains nothing from it, and the tree has to say so rather than imply a benefit.
     */
    @Test
    void anImplantWithNoInterfaceToWorkThroughIsMarkedInert() {
        String badge = EnhancedImagingBadge.forUnit(unitWith(5, 5, false));

        assertTrue(badge.contains("no interface"),
              "an implant with nothing to work through must say so, got: " + badge);
    }

    /** A partly implanted squad is worth counting; a wholly implanted one reads better without. */
    @Test
    void aPartlyImplantedCrewIsCounted() {
        assertTrue(EnhancedImagingBadge.forUnit(unitWith(3, 5, true)).contains("[EI 3/5]"));
        assertTrue(EnhancedImagingBadge.forUnit(unitWith(5, 5, true)).contains("[EI]"),
              "a wholly implanted crew needs no count");
    }

    @Test
    void aCrewWithNoImplantsIsNotBadged() {
        assertEquals("", EnhancedImagingBadge.forUnit(unitWith(0, 5, true)),
              "an EI interface with nobody implanted to use it is not an EI unit");
    }

    @Test
    void aNullUnitIsNotBadged() {
        assertEquals("", EnhancedImagingBadge.forUnit(null));
    }

    /** A unit whose entity has not been loaded must not bring the tree down. */
    @Test
    void aUnitWithNoEntityIsNotBadged() {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(null);

        assertEquals("", EnhancedImagingBadge.forUnit(unit));
    }
}
