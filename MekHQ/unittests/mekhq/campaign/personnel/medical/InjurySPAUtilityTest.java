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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InjurySPAUtilityTest {
    private static final int FATIGUE_RATE = 2;

    private static Person mockPerson(boolean hasGlassJaw, boolean hasToughness) {
        PersonnelOptions options = mock(PersonnelOptions.class);
        when(options.booleanOption(FLAW_GLASS_JAW)).thenReturn(hasGlassJaw);
        when(options.booleanOption(ATOW_TOUGHNESS)).thenReturn(hasToughness);

        Person person = mock(Person.class);
        when(person.getOptions()).thenReturn(options);
        return person;
    }

    @ParameterizedTest(name = "Fatigue {0}, Injury Fatigue {1} -> {2}")
    @CsvSource({ "true, true, true", "true, false, false", "false, true, false", "false, false, false" })
    void injuryFatigueRequiresBothOptions(boolean useFatigue, boolean useInjuryFatigue, boolean expected) {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(useFatigue);
        when(campaignOptions.get(CampaignOption.USE_INJURY_FATIGUE)).thenReturn(useInjuryFatigue);

        assertEquals(expected, InjurySPAUtility.isUseInjuryFatigue(campaignOptions));
    }

    @ParameterizedTest(name = "Glass Jaw {0}, Toughness {1}: {2} injuries -> {3}")
    @CsvSource({
          "false, false, 4, 4",
          // Glass Jaw doubles injuries
          "true, false, 4, 8",
          // Toughness reduces injuries to 75%, rounded up
          "false, true, 4, 3",
          "false, true, 3, 3",
          // Both cancel out
          "true, true, 4, 4"
    })
    void injuriesAdjustedButFatigueUsesBaseInjuries(boolean hasGlassJaw, boolean hasToughness, int injuries,
          int expectedInjuries) {
        Person person = mockPerson(hasGlassJaw, hasToughness);

        int adjustedInjuries = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(person, true, FATIGUE_RATE, injuries);

        assertEquals(expectedInjuries, adjustedInjuries);
        // Injury fatigue uses the unadjusted injuries and skips the fatigue multiplier
        verify(person).changeFatigue(injuries * FATIGUE_RATE, false);
        verify(person, never()).changeFatigue(anyInt());
    }

    @Test
    void noFatigueWhenInjuryFatigueDisabled() {
        Person person = mockPerson(true, false);

        int adjustedInjuries = InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(person, false, FATIGUE_RATE, 2);

        assertEquals(4, adjustedInjuries);
        verify(person, never()).changeFatigue(anyInt(), anyBoolean());
        verify(person, never()).changeFatigue(anyInt());
    }

    @Test
    void isUseInjuryFatigueDoesNotNeedFatigueOptionWhenInjuryFatigueIsOff() {
        // Injury Fatigue is checked first, so callers that only stub it still work
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.USE_INJURY_FATIGUE)).thenReturn(false);

        assertFalse(InjurySPAUtility.isUseInjuryFatigue(campaignOptions));
    }
}
