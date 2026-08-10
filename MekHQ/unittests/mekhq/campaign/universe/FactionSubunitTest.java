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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import javax.swing.DefaultComboBoxModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

import megamek.common.universe.Faction2;
import megamek.common.universe.HonorRating;
import mekhq.campaign.personnel.Person;
import mekhq.gui.displayWrappers.FactionDisplay;
import mekhq.gui.utilities.OriginFactionPickerHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Verifies that subordinate formations - the individual regiments declared inside a command's file - are recognised as
 * such and left out of the lists that offer the player a choice of faction.
 */
class FactionSubunitTest {

    /**
     * Builds a Faction from a stubbed Faction2. Every collection the constructor walks is stubbed explicitly rather
     * than left to Mockito's defaults, because it dereferences several of them directly.
     */
    private static Faction factionFrom(String key, String name, boolean isSubunit) {
        Faction2 faction2 = mock(Faction2.class);
        when(faction2.getKey()).thenReturn(key);
        when(faction2.getName()).thenReturn(name);
        when(faction2.isSubunit()).thenReturn(isSubunit);
        when(faction2.getTags()).thenReturn(new HashSet<>());
        when(faction2.getFallBackFactions()).thenReturn(new LinkedHashSet<>());
        when(faction2.getYearsActive()).thenReturn(new ArrayList<>());
        when(faction2.getNameChanges()).thenReturn(new TreeMap<>());
        when(faction2.getCapitalChanges()).thenReturn(new TreeMap<>());
        when(faction2.getPreInvasionHonorRating()).thenReturn(HonorRating.NONE);
        when(faction2.getPostInvasionHonorRating()).thenReturn(HonorRating.NONE);
        return new Faction(faction2);
    }

    @Test
    void aCommandIsNotASubunit() {
        Faction command = factionFrom("FS.DaviBrig", "Davion Brigade of Guards", false);

        assertFalse(command.isSubunit());
    }

    @Test
    void aRegimentDeclaredInsideACommandIsASubunit() {
        Faction regiment = factionFrom("FS.DaviBrig.Heav", "Davion Heavy Guards", true);

        assertTrue(regiment.isSubunit());
    }

    @Test
    void aFactionBuiltWithoutFaction2IsNotASubunit() {
        // Factions built the old way carry no Faction2, and must not blow up on the question.
        Faction legacyFaction = new Faction("FS", "Federated Suns");

        assertFalse(legacyFaction.isSubunit());
    }

    @Test
    void anExistingSubunitOriginIsKeptSoEditingDoesNotLoseIt() {
        // buildModel documents that a person's current origin is always offered, so that editing a
        // character whose origin would otherwise be filtered out does not silently drop it. The
        // subunit exclusion has to respect that, or hand-edited data loses the assignment.
        Faction subunitOrigin = factionFrom("FS.DaviBrig.Heav", "Davion Heavy Guards", true);
        Person person = mock(Person.class);
        when(person.getOriginFaction()).thenReturn(subunitOrigin);
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(3000, 1, 1));

        try (MockedStatic<Factions> factionsStatic = mockStatic(Factions.class)) {
            Factions factions = mock(Factions.class);
            when(factions.getFactions()).thenReturn(List.of(subunitOrigin));
            factionsStatic.when(Factions::getInstance).thenReturn(factions);

            DefaultComboBoxModel<Faction> model =
                  OriginFactionPickerHelper.buildModel(person, 3025, null, true);

            assertEquals(1, model.getSize(), "The person's existing origin must still be offered");
            assertEquals(subunitOrigin, model.getElementAt(0));
        }
    }

    @Test
    void factionChoosersLeaveSubunitsOut() {
        Faction command = factionFrom("FS.DaviBrig", "Davion Brigade of Guards", false);
        Faction regiment = factionFrom("FS.DaviBrig.Heav", "Davion Heavy Guards", true);

        List<FactionDisplay> offered = FactionDisplay.getSortedValidFactionDisplays(
              List.of(command, regiment), LocalDate.of(3025, 1, 1));

        assertEquals(1, offered.size(), "Only the command should be offered, not its regiment");
        assertEquals(command, offered.get(0).getFaction());
    }
}
