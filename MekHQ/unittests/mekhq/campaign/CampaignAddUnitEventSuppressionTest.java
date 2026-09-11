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

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import mekhq.EventSpy;
import mekhq.campaign.events.units.UnitNewEvent;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies that {@link Campaign#setBulkGenerationInProgress(boolean)} gates the per-unit
 * {@link UnitNewEvent} fired by {@link Campaign#addNewUnit}. Force generation relies on this to add
 * many units off the Swing event dispatch thread without triggering event-driven GUI refreshes that
 * would read the half-built campaign mid-generation.
 */
class CampaignAddUnitEventSuppressionTest {

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    private static long unitNewEventCount(EventSpy eventSpy) {
        return eventSpy.getEvents().stream().filter(UnitNewEvent.class::isInstance).count();
    }

    @Test
    void addNewUnitFiresUnitNewEventUnlessSuppressed() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        try (EventSpy eventSpy = new EventSpy()) {
            campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
            assertEquals(1, unitNewEventCount(eventSpy), "a normal add fires one UnitNewEvent");

            campaign.setBulkGenerationInProgress(true);
            campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
            assertEquals(1, unitNewEventCount(eventSpy), "a suppressed add fires no UnitNewEvent");

            campaign.setBulkGenerationInProgress(false);
            campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
            assertEquals(2, unitNewEventCount(eventSpy), "restoring events fires a UnitNewEvent again");
        }
    }
}
