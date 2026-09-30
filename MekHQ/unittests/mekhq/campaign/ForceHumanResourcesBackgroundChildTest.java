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
package mekhq.campaign;

import static mekhq.campaign.ForceHumanResources.rerollBackgroundChildEducation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for 'Dr Baby': children generated as part of a recruit's background family had their education
 * rolled against a random adult age before their date of birth was backdated.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ForceHumanResourcesBackgroundChildTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 1);

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 5, 10, 15 })
    void rerollBackgroundChildEducation_childUnderSixteen_hasOnlyEarlyChildhoodAndNoTitles(int age) {
        Campaign campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        Person child = new Person(campaign);
        child.setDateOfBirth(TODAY.minusYears(age));
        child.setEduHighestEducation(EducationLevel.DOCTORATE);
        child.setPreNominal("Dr");
        child.setPostNominal("PhD");

        rerollBackgroundChildEducation(campaign, child);

        assertEquals(EducationLevel.EARLY_CHILDHOOD, child.getEduHighestEducation());
        assertEquals("", child.getPreNominal());
        assertEquals("", child.getPostNominal());
    }
}
