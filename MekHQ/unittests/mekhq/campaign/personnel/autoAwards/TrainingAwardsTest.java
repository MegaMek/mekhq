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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.education.AcademyType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests Training awards, which can be earned by education level, academy type, or academy name.
 */
class TrainingAwardsTest {
    private static final String ACADEMY_NAME = "Test Academy";

    private static int countAwards(int requiredLevel, String requiredType, String requiredName, int graduatedLevel,
          AcademyType graduatedType) {
        UUID personId = UUID.randomUUID();
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getPlayerForce().getHumanResources().getPerson(personId))
              .thenReturn(mock(Person.class, RETURNS_DEEP_STUBS));

        Award award = new Award("Graduate", "test", "", "", List.of(), List.of(), List.of(), 0, 0, false,
              requiredLevel, "Training", requiredType, requiredName, 0);

        return TrainingAwards.TrainingAwardsProcessor(campaign, personId,
              List.<Object>of(graduatedLevel, graduatedType, ACADEMY_NAME), List.of(award)).size();
    }

    @ParameterizedTest
    @CsvSource({ "2, 1, 0", "2, 2, 1", "2, 3, 1" })
    void testEducationLevelAward(int requiredLevel, int graduatedLevel, int expectedAwards) {
        // An award by education level has no academy type or name, and graduating at exactly that level counts
        assertEquals(expectedAwards, countAwards(requiredLevel, "", "", graduatedLevel, AcademyType.COLLEGE));
    }

    @ParameterizedTest
    @CsvSource({ "BASIC_TRAINING, 1", "NCO_ACADEMY, 0" })
    void testAcademyTypeAward(AcademyType graduatedType, int expectedAwards) {
        assertEquals(expectedAwards, countAwards(0, "5", "", 1, graduatedType));
    }

    @ParameterizedTest
    @CsvSource(value = { "Test Academy, 1", "Another Academy, 0" })
    void testAcademyNameAward(String requiredName, int expectedAwards) {
        assertEquals(expectedAwards, countAwards(0, "", requiredName, 1, AcademyType.COLLEGE));
    }
}
