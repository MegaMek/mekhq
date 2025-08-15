/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A test class for validating the functionality of the {@code PrisonerCaptureStyle} enumeration.
 *
 * <p>This class contains unit tests to ensure that each {@code PrisonerCaptureStyle} has valid
 * resource keys for its labels and tooltips. The tests verify that no invalid labels or tooltips are present, as
 * determined by the {@code isResourceKeyValid} method.</p>
 */
public class PrisonerCaptureStyleTest {
    @Test
    public void testGetLabel_notInvalid() {
        for (PrisonerCaptureStyle status : PrisonerCaptureStyle.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetTitleExtension_notInvalid() {
        for (PrisonerCaptureStyle status : PrisonerCaptureStyle.values()) {
            String titleExtension = status.getTooltip();
            assertTrue(isResourceKeyValid(titleExtension));
        }
    }
}
