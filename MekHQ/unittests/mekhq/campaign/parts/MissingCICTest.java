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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingCIC;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link MissingCIC#isAcceptableReplacement}, which compared costs by object identity, so a spare
 * Combat Information Center was only accepted if it shared the very same cost object: never after a campaign reload,
 * and never for a salvaged spare.
 */
class MissingCICTest {
    @Test
    void spareWithAnEqualCostIsAccepted() {
        Campaign campaign = mock(Campaign.class);
        // Two separate but equal amounts, as a campaign reload produces.
        Money placeholderCost = Money.of(1_000_000);
        Money spareCost = Money.of(1_000_000);
        assertNotSame(placeholderCost, spareCost);
        MissingCIC missingCombatInformationCenter = new MissingCIC(0, placeholderCost, campaign);
        CombatInformationCenter spare = new CombatInformationCenter(0, spareCost, campaign);

        assertTrue(missingCombatInformationCenter.isAcceptableReplacement(spare, false));
    }

    @Test
    void spareWithADifferentCostIsRefused() {
        Campaign campaign = mock(Campaign.class);
        MissingCIC missingCombatInformationCenter = new MissingCIC(0, Money.of(1_000_000), campaign);
        CombatInformationCenter spare = new CombatInformationCenter(0, Money.of(2_000_000), campaign);

        assertFalse(missingCombatInformationCenter.isAcceptableReplacement(spare, false));
    }
}
