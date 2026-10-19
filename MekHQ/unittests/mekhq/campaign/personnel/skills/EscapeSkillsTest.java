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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.turnoverAndRetention.Fatigue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the injuries a prisoner suffers after a notable escape failure in {@link EscapeSkills}.
 */
class EscapeSkillsTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 5);

    private Campaign campaign;
    private Person prisoner;
    private PersonnelOptions prisonerOptions;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaignOptions.get(CampaignOption.USE_FATIGUE)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.USE_INJURY_FATIGUE)).thenReturn(true);
        when(campaignOptions.get(CampaignOption.FATIGUE_RATE)).thenReturn(1);
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(false);

        prisoner = mock(Person.class);
        prisonerOptions = mock(PersonnelOptions.class);
        when(prisoner.getOptions()).thenReturn(prisonerOptions);
        when(prisoner.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        doAnswer(invocation -> {
            events.add("injuries");
            return null;
        }).when(prisoner).setHits(anyInt());
    }

    /**
     * Runs a disastrous escape failure, rolling {@code roll} on 2d6 (the prisoner takes half that many injuries).
     */
    private void processDisastrousFailure(int roll) throws Exception {
        Method processNotableFailure = EscapeSkills.class.getDeclaredMethod("processNotableFailure", Campaign.class,
              Person.class, boolean.class);
        processNotableFailure.setAccessible(true);

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class);
              MockedStatic<Fatigue> mockedFatigue = mockStatic(Fatigue.class)) {
            mockedCompute.when(() -> Compute.d6(2)).thenReturn(roll);
            mockedFatigue.when(() -> Fatigue.processFatigueActions(campaign, prisoner)).thenAnswer(invocation -> {
                events.add("fatigue");
                return null;
            });

            processNotableFailure.invoke(null, campaign, prisoner, true);
        }
    }

    @Test
    void fatigueIsProcessedAfterInjuries() throws Exception {
        processDisastrousFailure(8);

        // If fatigue were processed first, the leave check would see an uninjured prisoner
        assertEquals(List.of("injuries", "fatigue"), events);
        verify(prisoner).setHits(4);
    }

    @Test
    void toughnessReducesInjuries() throws Exception {
        when(prisonerOptions.booleanOption(ATOW_TOUGHNESS)).thenReturn(true);

        processDisastrousFailure(8);

        // 4 injuries, reduced by a quarter (rounded up) for Toughness
        verify(prisoner).setHits(3);
    }

    @Test
    void killedPrisonerSkipsFatigue() throws Exception {
        when(prisoner.getHits()).thenReturn(4);
        when(prisoner.getStatus()).thenReturn(PersonnelStatus.HOMICIDE);

        processDisastrousFailure(4);

        verify(prisoner).changeStatus(campaign, TODAY, PersonnelStatus.HOMICIDE);
        assertEquals(List.of("injuries"), events);
    }
}
