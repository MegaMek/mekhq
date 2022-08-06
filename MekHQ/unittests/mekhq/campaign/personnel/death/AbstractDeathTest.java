/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.death;

import mekhq.campaign.CampaignOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.Mockito.when;

/**
 * Personnel Testing Tracker:
 * 1) Death:
 *  a) AbstractDeath
 * 2) Divorce:
 *  a) AbstractDivorce
 * 3) Marriage:
 *  a) AbstractMarriage
 * 4) Procreation:
 *  a) AbstractProcreation
 * 5) Enums:
 *  a) Profession
 * Unhandled:
 * 1) Generator: All
 * 2) Ranks: All
 * 3) General: All
 *
 * Other Testing Tracker:
 * 1) GUI Enums: Most are partially tested currently
 * 2) Universe Enums: Most are unimplemented currently
 */
@Disabled // FIXME : Windchild : All Tests Missing
@ExtendWith(value = MockitoExtension.class)
public class AbstractDeathTest {
// Saved for Future Test Usage
/*
    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }
 */

    @Mock
    private CampaignOptions mockOptions;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.getEnabledRandomDeathAgeGroups()).thenReturn(new HashMap<>());
        when(mockOptions.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDeath()).thenReturn(false);
        when(mockOptions.isUseRandomDeathSuicideCause()).thenReturn(false);
    }

    //region Constructors
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testConstructorInitializesCauses() {

    }
    //endregion Constructors

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testCanDie() {

    }

    //region New Day
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessNewDay() {

    }
    //endregion New Day

    //region Cause
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetCause() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetDefaultCause() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDetermineIfInjuriesCausedTheDeath() {

    }
    //endregion Cause

    //region File I/O
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testInitializeCauses() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testInitializeCausesFromFile() {

    }
    //endregion File I/O
}
