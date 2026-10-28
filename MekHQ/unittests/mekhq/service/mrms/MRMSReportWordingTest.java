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
package mekhq.service.mrms;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The Mass Repair summary says why a unit was left alone, in words that match what happened (issue #3723).
 */
class MRMSReportWordingTest {
    private static final String MRMS_BUNDLE = "mekhq.resources.MRMS";

    @Test
    void unitsLeftAloneByTheSettingsAreExplained() {
        assertEquals("- 4 units had nothing done because no part needing work met the Mass Repair settings.",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.Summary.unitsWithNoMatchingParts", 4));
        assertEquals("Units with parts that did not meet the Mass Repair settings:",
              getTextAt(MRMS_BUNDLE, "MRMS.UnitList.noMatchingParts"));
    }

    @Test
    void summaryCountsOneUnitInTheSingular() {
        assertEquals("<font color='green'>Mass Repair/Salvage complete for 1 unit.</font>",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.Summary.report", "green", 1));
        assertEquals("- 1 unit had repairs or parts salvaged.",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.Summary.unitsWorkedOn", 1));
    }

    @Test
    void partsStillWaitingAreCountedAcrossUnits() {
        assertEquals("<font color='red'>There are still 4 parts not being worked on, across 4 units.</font>",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.StillNotWorkedOn.report", "red", 4, 4));
        assertEquals("<font color='red'>There is still 1 part not being worked on, across 1 unit.</font>",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.StillNotWorkedOn.report", "red", 1, 1));
    }

    @Test
    void stoppingBeforeAnyWorkSaysItDidNotRun() {
        assertEquals("Mass Repair/Salvage did not run. The service is disabled for Salvage.",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.CompleteTypeDisabled.report",
                    getTextAt(MRMS_BUNDLE, "Salvage")));
        assertEquals("Mass Repair/Salvage did not run. The service is disabled.",
              getTextAt(MRMS_BUNDLE, "MRMS.CompleteDisabled.report"));
        assertEquals("Mass Repair/Salvage did not run. There are insufficient Astechs to run the service. "
                           + "Please hire more before running it again.",
              getTextAt(MRMS_BUNDLE, "MRMS.InsufficientAstechs.report"));
        assertEquals("Mass Repair/Salvage", getTextAt(MRMS_BUNDLE, "MRMS.UnitResult.title"));
    }

    @Test
    void singleUnitReportCountsItsActions() {
        assertEquals("There was 1 action performed.",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.UnitActionsPerformed.report", 1));
        assertEquals("There were 3 actions performed.",
              getFormattedTextAt(MRMS_BUNDLE, "MRMS.UnitActionsPerformed.report", 3));
    }
}
