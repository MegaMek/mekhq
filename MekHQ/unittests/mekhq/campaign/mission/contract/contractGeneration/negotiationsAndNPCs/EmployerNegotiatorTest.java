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
package mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link EmployerNegotiator#getEmployerSkillLevel}, which sets how skilled a contract's employer negotiator is.
 *
 * <p>The tier follows the employer: a major power, Clan, ComStar, or the Word of Blake sends a seasoned negotiator
 * when it deals as a system owner, while smaller employers send progressively greener ones.</p>
 */
class EmployerNegotiatorTest {
    private static Faction faction(boolean isMajorPower, boolean isClan, boolean isComStarOrWoB) {
        Faction faction = mock(Faction.class);
        when(faction.isMajorOrSuperPower()).thenReturn(isMajorPower);
        when(faction.isClan()).thenReturn(isClan);
        when(faction.isComStarOrWoB()).thenReturn(isComStarOrWoB);
        return faction;
    }

    @ParameterizedTest
    @CsvSource({
          "ANY_SYSTEM_OWNER, true, false, false, VETERAN",
          "LOCAL_SYSTEM_OWNER, false, true, false, VETERAN",
          "ANY_SYSTEM_OWNER, false, false, true, ELITE",
          "LOCAL_SYSTEM_OWNER, false, false, false, REGULAR",
          "NOBLE, true, false, false, REGULAR",
          "MERCENARY_SUBCONTRACT, false, false, false, REGULAR",
          "CORPORATION, false, false, true, REGULAR",
          "UNDERWORLD_CONTACT, false, false, false, REGULAR",
          "CIVILIAN_ORGANIZATION_BUSINESS, false, false, false, GREEN",
          "CIVILIAN_ORGANIZATION_MILITIA, false, false, false, GREEN",
          "LOCAL_PLANETARY_GOVERNMENT, true, false, false, GREEN",
          "ANY_PLANETARY_GOVERNMENT, false, false, false, GREEN",
          "CIVILIAN_ORGANIZATION_REBELS, false, false, false, ULTRA_GREEN"
    })
    void negotiatorTierFollowsTheEmployer(ChaosEmployerType employerType, boolean isMajorPower, boolean isClan,
          boolean isComStarOrWoB, SkillLevel expected) {
        assertEquals(expected,
              EmployerNegotiator.getEmployerSkillLevel(faction(isMajorPower, isClan, isComStarOrWoB), employerType));
    }
}
