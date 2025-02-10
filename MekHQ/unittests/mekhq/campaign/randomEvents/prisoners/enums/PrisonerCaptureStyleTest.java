/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners.enums;

import org.junit.jupiter.api.Test;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test class for validating the functionality of the {@code PrisonerCaptureStyle} enumeration.
 *
 * <p>This class contains unit tests to ensure that each {@code PrisonerCaptureStyle} has valid
 * resource keys for its labels and tooltips. The tests verify that no invalid labels or tooltips
 * are present, as determined by the {@code isResourceKeyValid} method.</p>
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
