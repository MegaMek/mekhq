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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoveryTimeDataTest {
    private final RecoveryTimeData data = new RecoveryTimeData(0.25, 0.5, 0.25, 0.5, 0.5, 0.25, 60, 195);

    @Test
    void breakdownCanBeWrappedInHtml() {
        String breakdown = data.getRecoveryTimeBreakdownString(true);

        assertTrue(breakdown.startsWith("<html>"), breakdown);
        assertTrue(breakdown.endsWith("</html>"), breakdown);
    }

    @Test
    void plainBreakdownHasNoHtmlWrapper() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        assertFalse(breakdown.startsWith("<html>"), breakdown);
        assertFalse(breakdown.endsWith("</html>"), breakdown);
    }

    @Test
    void breakdownIsFullyLocalized() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        // A missing resource key comes back wrapped in '!'
        assertFalse(breakdown.contains("!RecoveryTimeData"), breakdown);
    }

    @Test
    void breakdownShowsTheTotals() {
        String breakdown = data.getRecoveryTimeBreakdownString(false);

        assertTrue(breakdown.contains("195"), breakdown);
        // 1.0 + 0.25 + 0.5 + 0.25 + 0.5 + 0.5 + 0.25
        assertTrue(breakdown.contains("3.25") || breakdown.contains("3,25"), breakdown);
    }
}
