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
package mekhq.campaign.personnel.autoAwards;

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

/**
 * Tests the Support Person of the Year award when the winner already holds it.
 */
class MiscAwardsTest {
    @Test
    void testRepeatSupportPersonOfTheYearIsReported() {
        UUID personId = UUID.randomUUID();
        Person person = mock(Person.class, RETURNS_DEEP_STUBS);
        when(person.getHyperlinkedFullTitle()).thenReturn("Tech Sergeant");

        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId)).thenReturn(person);

        Award award = new Award("Support Person Of The Year", "test", "", "", List.of(), List.of(), List.of(), 0, 0,
              false, 0, "Misc", null, "Support Person of the Year", 0);
        when(person.getAwardController().hasAward(award)).thenReturn(true);

        int awards = MiscAwards.MiscAwardsProcessor(campaign, null, personId, List.of(award), false, false, 0, 0,
              personId, null).size();

        assertEquals(0, awards, "a person cannot win a non-repeatable award twice");
        verify(campaign).addReport(eq(PERSONNEL), anyString());
    }
}
